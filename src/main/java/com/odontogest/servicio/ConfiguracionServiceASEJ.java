package com.odontogest.servicio;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.odontogest.config.DirectorioDatosASEJ;
import com.odontogest.dominio.comun.ValidacionesASEJ;
import com.odontogest.excepcion.NegocioExceptionASEJ;
import com.odontogest.integracion.seguridad.CifradorSecretosASEJ;
import com.odontogest.modelo.ConfiguracionASEJ;
import com.odontogest.modelo.TipoConfiguracionASEJ;
import com.odontogest.persistencia.repositorio.ConfiguracionRepositoryASEJ;
import com.odontogest.seguridad.PermisoASEJ;
import com.odontogest.seguridad.SesionUsuarioASEJ;
import com.odontogest.servicio.dto.ConfiguracionDtoASEJ;
import com.odontogest.servicio.dto.DatosClinicaASEJ;
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

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ConfiguracionServiceASEJ {

    private static final Set<String> LIBRESASEJ = Set.of("ui.tema");
    private static final Map<String, int[]> RANGOSASEJ = Map.of(
            "agenda.duracion_cita_min", new int[]{5, 240},
            "agenda.dias_busqueda_hueco", new int[]{1, 365},
            "recordatorio.horas_antes", new int[]{1, 168},
            "email.smtp.puerto", new int[]{1, 65535},
            "api.timeout_seg", new int[]{1, 60},
            "respaldo.mantener", new int[]{1, 100});
    private static final Set<Integer> INTERVALOS_AGENDAASEJ = Set.of(5, 10, 15, 20, 30);
    private static final long TAMANIO_MAXIMO_LOGOASEJ = 2L * 1024 * 1024;

    private final Clock relojASEJ;
    private final ConfiguracionRepositoryASEJ configuracionRepositoryASEJ;
    private final AuditoriaServiceASEJ auditoriaASEJ;
    private final CifradorSecretosASEJ cifradorASEJ;
    private final SesionUsuarioASEJ sesionUsuarioASEJ;
    private final Map<String, String> cacheASEJ = new ConcurrentHashMap<>();

    public String textoASEJ(String claveASEJ) {
        return cacheASEJ.computeIfAbsent(claveASEJ, this::leerASEJ);
    }

    public int enteroASEJ(String claveASEJ) {
        String valorASEJ = textoASEJ(claveASEJ);
        try {
            return Integer.parseInt(valorASEJ.trim());
        } catch (NumberFormatException eASEJ) {
            throw new NegocioExceptionASEJ("La configuración '" + claveASEJ + "' no es un número entero: " + valorASEJ);
        }
    }

    public boolean booleanoASEJ(String claveASEJ) {
        return Boolean.parseBoolean(textoASEJ(claveASEJ).trim());
    }

    public Optional<String> secretoASEJ(String claveASEJ) {
        String guardadoASEJ = textoASEJ(claveASEJ);
        if (guardadoASEJ.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(CifradorSecretosASEJ.esCifradoASEJ(guardadoASEJ) ? cifradorASEJ.descifrarASEJ(guardadoASEJ) : guardadoASEJ);
    }

    public JsonObject jsonASEJ(String claveASEJ) {
        try {
            return JsonParser.parseString(textoASEJ(claveASEJ)).getAsJsonObject();
        } catch (JsonParseException | IllegalStateException eASEJ) {
            throw new NegocioExceptionASEJ("La configuración '" + claveASEJ + "' no es un objeto JSON válido.");
        }
    }

    public DatosClinicaASEJ datosClinicaASEJ() {
        return new DatosClinicaASEJ(textoASEJ("clinica.nombre"), textoASEJ("clinica.ruc"), textoASEJ("clinica.direccion"),
                textoASEJ("clinica.telefono"), textoASEJ("clinica.email"), textoASEJ("clinica.logo_ruta"));
    }

    public List<ConfiguracionDtoASEJ> listarASEJ() {
        return configuracionRepositoryASEJ.findAll().stream()
                .filter(ConfiguracionASEJ::isEditableASEJ)
                .sorted(Comparator.comparing(ConfiguracionASEJ::getGrupoASEJ))
                .map(cASEJ -> new ConfiguracionDtoASEJ(cASEJ.getClaveASEJ(), cASEJ.getTipoASEJ() == TipoConfiguracionASEJ.SECRETO ? null : valorASEJ(cASEJ),
                        cASEJ.getTipoASEJ(), cASEJ.getGrupoASEJ(), cASEJ.getDescripcionASEJ(),
                        cASEJ.getTipoASEJ() == TipoConfiguracionASEJ.SECRETO && !valorASEJ(cASEJ).isBlank()))
                .toList();
    }

    @Transactional
    public void guardarASEJ(Map<String, String> valoresASEJ) {
        if (!LIBRESASEJ.containsAll(valoresASEJ.keySet())) {
            sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.CONFIGURACION);
        }
        Map<String, String> normalizadosASEJ = new HashMap<>();
        valoresASEJ.forEach((claveASEJ, valorASEJ) -> normalizadosASEJ.put(claveASEJ, valorASEJ == null ? "" : valorASEJ.trim()));
        LocalDateTime ahoraASEJ = LocalDateTime.now(relojASEJ);
        normalizadosASEJ.forEach((claveASEJ, valorASEJ) -> {
            ConfiguracionASEJ cASEJ = configuracionRepositoryASEJ.findById(claveASEJ)
                    .orElseThrow(() -> new NegocioExceptionASEJ("No existe la configuración '" + claveASEJ + "'."));
            if (!cASEJ.isEditableASEJ()) {
                throw new NegocioExceptionASEJ("La configuración '" + claveASEJ + "' no es editable.");
            }
            validarASEJ(cASEJ, valorASEJ, normalizadosASEJ);
            cASEJ.setValorASEJ(cASEJ.getTipoASEJ() == TipoConfiguracionASEJ.SECRETO && !valorASEJ.isEmpty() ? cifradorASEJ.cifrarASEJ(valorASEJ) : valorASEJ);
            cASEJ.setActualizadoEnASEJ(ahoraASEJ);
        });
        cacheASEJ.clear();
        logASEJ.info("Configuración guardada: {}", normalizadosASEJ.keySet());
        auditoriaASEJ.registrarASEJ("CONFIGURACION_GUARDAR", "configuracion", null, Map.of("claves", normalizadosASEJ.keySet()));
    }

    @Transactional
    public void guardarSistemaASEJ(String claveASEJ, String valorASEJ) {
        ConfiguracionASEJ cASEJ = configuracionRepositoryASEJ.findById(claveASEJ)
                .orElseThrow(() -> new NegocioExceptionASEJ("No existe la configuración '" + claveASEJ + "'."));
        cASEJ.setValorASEJ(valorASEJ == null ? "" : valorASEJ);
        cASEJ.setActualizadoEnASEJ(LocalDateTime.now(relojASEJ));
        cacheASEJ.remove(claveASEJ);
    }

    @Transactional
    public String guardarLogoASEJ(Path origenASEJ) {
        sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.CONFIGURACION);
        String nombreASEJ = origenASEJ.getFileName().toString().toLowerCase(Locale.ROOT);
        String extASEJ = nombreASEJ.endsWith(".png") ? "png" : nombreASEJ.endsWith(".jpg") || nombreASEJ.endsWith(".jpeg") ? "jpg" : null;
        if (extASEJ == null) {
            throw new NegocioExceptionASEJ("El logo debe ser una imagen PNG o JPG.");
        }
        try {
            if (Files.size(origenASEJ) > TAMANIO_MAXIMO_LOGOASEJ) {
                throw new NegocioExceptionASEJ("El logo no debe pasar de 2 MB.");
            }
            Path destinoASEJ = DirectorioDatosASEJ.homeASEJ().resolve("logo." + extASEJ);
            Files.copy(origenASEJ, destinoASEJ, StandardCopyOption.REPLACE_EXISTING);
            guardarASEJ(Map.of("clinica.logo_ruta", destinoASEJ.toAbsolutePath().toString()));
            return destinoASEJ.toAbsolutePath().toString();
        } catch (IOException eASEJ) {
            throw new NegocioExceptionASEJ("No se pudo copiar el logo: " + eASEJ.getMessage());
        }
    }

    private void validarASEJ(ConfiguracionASEJ cASEJ, String valorASEJ, Map<String, String> loteASEJ) {
        String claveASEJ = cASEJ.getClaveASEJ();
        String nombreASEJ = cASEJ.getDescripcionASEJ() == null ? claveASEJ : cASEJ.getDescripcionASEJ();
        switch (cASEJ.getTipoASEJ()) {
            case ENTERO -> {
                int nASEJ;
                try {
                    nASEJ = Integer.parseInt(valorASEJ);
                } catch (NumberFormatException eASEJ) {
                    throw new NegocioExceptionASEJ("«" + nombreASEJ + "» debe ser un número entero.");
                }
                int[] rangoASEJ = RANGOSASEJ.get(claveASEJ);
                if (rangoASEJ != null && (nASEJ < rangoASEJ[0] || nASEJ > rangoASEJ[1])) {
                    throw new NegocioExceptionASEJ("«" + nombreASEJ + "» debe estar entre " + rangoASEJ[0] + " y " + rangoASEJ[1] + ".");
                }
                if (claveASEJ.equals("agenda.intervalo_min") && !INTERVALOS_AGENDAASEJ.contains(nASEJ)) {
                    throw new NegocioExceptionASEJ("El intervalo de la agenda debe ser 5, 10, 15, 20 o 30 minutos.");
                }
                if (claveASEJ.equals("agenda.duracion_cita_min")) {
                    int intervaloASEJ = Integer.parseInt(loteASEJ.getOrDefault("agenda.intervalo_min", textoASEJ("agenda.intervalo_min")));
                    if (nASEJ % intervaloASEJ != 0) {
                        throw new NegocioExceptionASEJ("La duración por defecto debe ser múltiplo del intervalo (" + intervaloASEJ + " min).");
                    }
                }
            }
            case BOOLEANO -> {
                if (!valorASEJ.equals("true") && !valorASEJ.equals("false")) {
                    throw new NegocioExceptionASEJ("«" + nombreASEJ + "» debe ser verdadero o falso.");
                }
            }
            case JSON -> {
                try {
                    JsonParser.parseString(valorASEJ).getAsJsonObject();
                } catch (JsonParseException | IllegalStateException eASEJ) {
                    throw new NegocioExceptionASEJ("«" + nombreASEJ + "» debe ser un objeto JSON, p. ej. {\"nombres\":\"nombres\"}.");
                }
            }
            case TEXTO -> validarTextoASEJ(claveASEJ, nombreASEJ, valorASEJ);
            case SECRETO -> {
                if (valorASEJ.length() > 500) {
                    throw new NegocioExceptionASEJ("«" + nombreASEJ + "» es demasiado largo.");
                }
            }
        }
    }

    private static void validarTextoASEJ(String claveASEJ, String nombreASEJ, String valorASEJ) {
        if (valorASEJ.length() > 500) {
            throw new NegocioExceptionASEJ("«" + nombreASEJ + "» admite como máximo 500 caracteres.");
        }
        if ((claveASEJ.equals("clinica.email") || claveASEJ.equals("email.remitente")) && !valorASEJ.isEmpty()
                && !ValidacionesASEJ.esEmailASEJ(valorASEJ)) {
            throw new NegocioExceptionASEJ("«" + nombreASEJ + "» no es un correo válido.");
        }
        if (claveASEJ.equals("clinica.nombre") && valorASEJ.isEmpty()) {
            throw new NegocioExceptionASEJ("Ingrese el nombre de la clínica: aparece en todos los documentos.");
        }
        if (claveASEJ.equals("api.dni.url_plantilla") && !valorASEJ.isEmpty()
                && (!(valorASEJ.startsWith("https://") || valorASEJ.startsWith("http://")) || !valorASEJ.contains("{numero}"))) {
            throw new NegocioExceptionASEJ("La URL de la API de DNI debe empezar con https:// y contener {numero}.");
        }
        if (claveASEJ.equals("ui.tema") && !valorASEJ.equals("CLARO") && !valorASEJ.equals("OSCURO")) {
            throw new NegocioExceptionASEJ("El tema debe ser CLARO u OSCURO.");
        }
    }

    private static String valorASEJ(ConfiguracionASEJ cASEJ) {
        return cASEJ.getValorASEJ() == null ? "" : cASEJ.getValorASEJ();
    }

    private String leerASEJ(String claveASEJ) {
        return configuracionRepositoryASEJ.findById(claveASEJ)
                .map(ConfiguracionServiceASEJ::valorASEJ)
                .orElseThrow(() -> new NegocioExceptionASEJ("No existe la configuración '" + claveASEJ + "'."));
    }
}
