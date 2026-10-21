/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.odontogest.config.DirectorioDatos;
import com.odontogest.dominio.comun.Validaciones;
import com.odontogest.excepcion.NegocioException;
import com.odontogest.integracion.seguridad.CifradorSecretos;
import com.odontogest.modelo.Configuracion;
import com.odontogest.modelo.TipoConfiguracion;
import com.odontogest.persistencia.repositorio.ConfiguracionRepository;
import com.odontogest.seguridad.Permiso;
import com.odontogest.seguridad.SesionUsuario;
import com.odontogest.servicio.dto.ConfiguracionDto;
import com.odontogest.servicio.dto.DatosClinica;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lectura y escritura de la tabla {@code configuracion} con caché en memoria (RF-CFG-01 a RF-CFG-03).
 * <ul>
 *   <li>Los valores de tipo SECRETO se guardan cifrados ({@link CifradorSecretos}, prefijo {@code enc:v1:}) y nunca
 *       salen hacia la interfaz: {@link #listar()} solo informa si están configurados.</li>
 *   <li>{@link #guardar(Map)} valida cada valor según su tipo y algunos rangos; el tema de la interfaz lo puede
 *       cambiar cualquier usuario, el resto exige el permiso CONFIGURACION.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ConfiguracionService {

    /** Claves que cualquier usuario puede cambiar (preferencias personales de la interfaz). */
    private static final Set<String> LIBRES = Set.of("ui.tema");
    /** Rangos de las claves enteras: {mínimo, máximo}. */
    private static final Map<String, int[]> RANGOS = Map.of(
            "agenda.duracion_cita_min", new int[]{5, 240},
            "agenda.dias_busqueda_hueco", new int[]{1, 365},
            "recordatorio.horas_antes", new int[]{1, 168},
            "email.smtp.puerto", new int[]{1, 65535},
            "api.timeout_seg", new int[]{1, 60},
            "respaldo.mantener", new int[]{1, 100});
    private static final Set<Integer> INTERVALOS_AGENDA = Set.of(5, 10, 15, 20, 30);
    private static final long TAMANIO_MAXIMO_LOGO = 2L * 1024 * 1024;

    private final Clock reloj;
    private final ConfiguracionRepository configuracionRepository;
    private final AuditoriaService auditoria;
    private final CifradorSecretos cifrador;
    private final SesionUsuario sesionUsuario;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public String texto(String clave) {
        return cache.computeIfAbsent(clave, this::leer);
    }

    public int entero(String clave) {
        String valor = texto(clave);
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            throw new NegocioException("La configuración '" + clave + "' no es un número entero: " + valor);
        }
    }

    public boolean booleano(String clave) {
        return Boolean.parseBoolean(texto(clave).trim());
    }

    /** Valor descifrado de un secreto; vacío si no está configurado. */
    public Optional<String> secreto(String clave) {
        String guardado = texto(clave);
        if (guardado.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(CifradorSecretos.esCifrado(guardado) ? cifrador.descifrar(guardado) : guardado);
    }

    /** Objeto JSON de una clave de tipo JSON. */
    public JsonObject json(String clave) {
        try {
            return JsonParser.parseString(texto(clave)).getAsJsonObject();
        } catch (JsonParseException | IllegalStateException e) {
            throw new NegocioException("La configuración '" + clave + "' no es un objeto JSON válido.");
        }
    }

    /** Datos de la clínica para las cabeceras de los PDF. */
    public DatosClinica datosClinica() {
        return new DatosClinica(texto("clinica.nombre"), texto("clinica.ruc"), texto("clinica.direccion"),
                texto("clinica.telefono"), texto("clinica.email"), texto("clinica.logo_ruta"));
    }

    /** Todas las claves editables, por grupo, para la pantalla de configuración (los secretos sin su valor). */
    public List<ConfiguracionDto> listar() {
        return configuracionRepository.findAll().stream()
                .filter(Configuracion::isEditable)
                .sorted(Comparator.comparing(Configuracion::getGrupo))
                .map(c -> new ConfiguracionDto(c.getClave(), c.getTipo() == TipoConfiguracion.SECRETO ? null : valor(c),
                        c.getTipo(), c.getGrupo(), c.getDescripcion(),
                        c.getTipo() == TipoConfiguracion.SECRETO && !valor(c).isBlank()))
                .toList();
    }

    /**
     * Guarda varios valores en una sola transacción e invalida la caché. Un secreto con valor vacío se borra; para
     * conservar un secreto, no se incluye en el mapa.
     */
    @Transactional
    public void guardar(Map<String, String> valores) {
        if (!LIBRES.containsAll(valores.keySet())) {
            sesionUsuario.exigir(Permiso.CONFIGURACION);
        }
        Map<String, String> normalizados = new HashMap<>();
        valores.forEach((clave, valor) -> normalizados.put(clave, valor == null ? "" : valor.trim()));
        LocalDateTime ahora = LocalDateTime.now(reloj);
        normalizados.forEach((clave, valor) -> {
            Configuracion c = configuracionRepository.findById(clave)
                    .orElseThrow(() -> new NegocioException("No existe la configuración '" + clave + "'."));
            if (!c.isEditable()) {
                throw new NegocioException("La configuración '" + clave + "' no es editable.");
            }
            validar(c, valor, normalizados);
            c.setValor(c.getTipo() == TipoConfiguracion.SECRETO && !valor.isEmpty() ? cifrador.cifrar(valor) : valor);
            c.setActualizadoEn(ahora);
        });
        cache.clear();
        log.info("Configuración guardada: {}", normalizados.keySet());   // nunca el valor (puede ser un secreto)
        auditoria.registrar("CONFIGURACION_GUARDAR", "configuracion", null, Map.of("claves", normalizados.keySet()));
    }

    /**
     * Escribe una clave de sistema (no editable desde la interfaz), p. ej. la que deja el seeder demo. No pide
     * permiso: solo la usan procesos internos.
     */
    @Transactional
    public void guardarSistema(String clave, String valor) {
        Configuracion c = configuracionRepository.findById(clave)
                .orElseThrow(() -> new NegocioException("No existe la configuración '" + clave + "'."));
        c.setValor(valor == null ? "" : valor);
        c.setActualizadoEn(LocalDateTime.now(reloj));
        cache.remove(clave);
    }

    /**
     * Copia el logo elegido a la carpeta de datos ({@code logo.png} o {@code logo.jpg}) y lo deja configurado: así
     * los PDF lo siguen encontrando aunque se mueva o borre el archivo original.
     *
     * @return ruta guardada
     */
    @Transactional
    public String guardarLogo(Path origen) {
        sesionUsuario.exigir(Permiso.CONFIGURACION);
        String nombre = origen.getFileName().toString().toLowerCase(Locale.ROOT);
        String ext = nombre.endsWith(".png") ? "png" : nombre.endsWith(".jpg") || nombre.endsWith(".jpeg") ? "jpg" : null;
        if (ext == null) {
            throw new NegocioException("El logo debe ser una imagen PNG o JPG.");
        }
        try {
            if (Files.size(origen) > TAMANIO_MAXIMO_LOGO) {
                throw new NegocioException("El logo no debe pasar de 2 MB.");
            }
            Path destino = DirectorioDatos.home().resolve("logo." + ext);
            Files.copy(origen, destino, StandardCopyOption.REPLACE_EXISTING);
            guardar(Map.of("clinica.logo_ruta", destino.toAbsolutePath().toString()));
            return destino.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new NegocioException("No se pudo copiar el logo: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ validación

    private void validar(Configuracion c, String valor, Map<String, String> lote) {
        String clave = c.getClave();
        String nombre = c.getDescripcion() == null ? clave : c.getDescripcion();
        switch (c.getTipo()) {
            case ENTERO -> {
                int n;
                try {
                    n = Integer.parseInt(valor);
                } catch (NumberFormatException e) {
                    throw new NegocioException("«" + nombre + "» debe ser un número entero.");
                }
                int[] rango = RANGOS.get(clave);
                if (rango != null && (n < rango[0] || n > rango[1])) {
                    throw new NegocioException("«" + nombre + "» debe estar entre " + rango[0] + " y " + rango[1] + ".");
                }
                if (clave.equals("agenda.intervalo_min") && !INTERVALOS_AGENDA.contains(n)) {
                    throw new NegocioException("El intervalo de la agenda debe ser 5, 10, 15, 20 o 30 minutos.");
                }
                if (clave.equals("agenda.duracion_cita_min")) {
                    int intervalo = Integer.parseInt(lote.getOrDefault("agenda.intervalo_min", texto("agenda.intervalo_min")));
                    if (n % intervalo != 0) {
                        throw new NegocioException("La duración por defecto debe ser múltiplo del intervalo (" + intervalo + " min).");
                    }
                }
            }
            case BOOLEANO -> {
                if (!valor.equals("true") && !valor.equals("false")) {
                    throw new NegocioException("«" + nombre + "» debe ser verdadero o falso.");
                }
            }
            case JSON -> {
                try {
                    JsonParser.parseString(valor).getAsJsonObject();
                } catch (JsonParseException | IllegalStateException e) {
                    throw new NegocioException("«" + nombre + "» debe ser un objeto JSON, p. ej. {\"nombres\":\"nombres\"}.");
                }
            }
            case TEXTO -> validarTexto(clave, nombre, valor);
            case SECRETO -> {
                if (valor.length() > 500) {
                    throw new NegocioException("«" + nombre + "» es demasiado largo.");
                }
            }
        }
    }

    private static void validarTexto(String clave, String nombre, String valor) {
        if (valor.length() > 500) {
            throw new NegocioException("«" + nombre + "» admite como máximo 500 caracteres.");
        }
        if ((clave.equals("clinica.email") || clave.equals("email.remitente")) && !valor.isEmpty()
                && !Validaciones.esEmail(valor)) {
            throw new NegocioException("«" + nombre + "» no es un correo válido.");
        }
        if (clave.equals("clinica.nombre") && valor.isEmpty()) {
            throw new NegocioException("Ingrese el nombre de la clínica: aparece en todos los documentos.");
        }
        if (clave.equals("api.dni.url_plantilla") && !valor.isEmpty()
                && (!(valor.startsWith("https://") || valor.startsWith("http://")) || !valor.contains("{numero}"))) {
            throw new NegocioException("La URL de la API de DNI debe empezar con https:// y contener {numero}.");
        }
        if (clave.equals("ui.tema") && !valor.equals("CLARO") && !valor.equals("OSCURO")) {
            throw new NegocioException("El tema debe ser CLARO u OSCURO.");
        }
    }

    private static String valor(Configuracion c) {
        return c.getValor() == null ? "" : c.getValor();
    }

    private String leer(String clave) {
        return configuracionRepository.findById(clave)
                .map(ConfiguracionService::valor)
                .orElseThrow(() -> new NegocioException("No existe la configuración '" + clave + "'."));
    }
}
