/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.odontogest.config.DirectorioDatos;
import com.odontogest.config.RestauracionPendiente;
import com.odontogest.excepcion.NegocioException;
import com.odontogest.persistencia.conversor.LocalDateTimeConverter;
import com.odontogest.seguridad.Permiso;
import com.odontogest.seguridad.SesionUsuario;
import com.odontogest.servicio.dto.RespaldoDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Respaldo y restauración (PLAN §5.4, RF-CFG-04).
 * <ul>
 *   <li>{@link #crear(Path)}: {@code VACUUM INTO} copia la base de forma consistente aunque esté en modo WAL y se
 *       comprime junto con {@code adjuntos/}, {@code clave.key} (sin ella no se descifran los secretos) y un
 *       {@code manifest.json} con versión, fecha y esquema.</li>
 *   <li>{@link #programarRestauracion(Path)}: valida el ZIP, comprueba la base extraída ({@code integrity_check} y
 *       tablas principales) y deja la marca para {@link RestauracionPendiente}, que la aplica al reiniciar.</li>
 *   <li>Respaldo automático al cerrar, conservando los últimos {@code respaldo.mantener} automáticos; los manuales
 *       no se borran nunca.</li>
 * </ul>
 * Sin {@code @Transactional} a nivel de clase, a diferencia de los demás servicios: SQLite no permite
 * {@code VACUUM} dentro de una transacción. La auditoría, que exige una, se registra con {@link TransactionTemplate}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RespaldoService {

    /** Versión del esquema ({@code schema.sql} v1); un respaldo de un esquema posterior no se restaura. */
    public static final int VERSION_ESQUEMA = 1;
    public static final String PREFIJO_MANUAL = "odontogest-respaldo-";
    public static final String PREFIJO_AUTOMATICO = "odontogest-auto-";
    static final String MANIFIESTO = "manifest.json";
    private static final DateTimeFormatter SELLO = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final byte[] CABECERA_SQLITE = "SQLite format 3\0".getBytes(StandardCharsets.US_ASCII);
    private static final List<String> TABLAS_REQUERIDAS = List.of("configuracion", "usuario", "paciente", "odontograma");

    private final Clock reloj;
    private final JdbcTemplate jdbcTemplate;
    private final ConfiguracionService configuracionService;
    private final AuditoriaService auditoria;
    private final SesionUsuario sesionUsuario;
    private final PlatformTransactionManager transacciones;
    private final ObjectProvider<BuildProperties> buildProperties;
    private final Gson gson = new Gson();

    // ------------------------------------------------------------------ crear

    /** "Crear respaldo ahora": ZIP en {@code respaldos/}. */
    public RespaldoDto crearAhora() {
        sesionUsuario.exigir(Permiso.RESPALDO);
        Path zip = crear(nuevoArchivo(PREFIJO_MANUAL));
        auditar("RESPALDO_CREAR", Map.of("archivo", zip.getFileName().toString()));
        return dto(zip);
    }

    /**
     * Crea el ZIP de respaldo en {@code destinoZip} (lo reemplaza si existe).
     *
     * @return {@code destinoZip}
     */
    public Path crear(Path destinoZip) {
        Path home = DirectorioDatos.home();
        Path temporal = null;
        try {
            temporal = Files.createTempDirectory("odontogest-respaldo-");
            Path copia = temporal.resolve(RestauracionPendiente.BASE);
            jdbcTemplate.execute("VACUUM INTO '" + copia.toAbsolutePath().toString().replace("'", "''") + "'");
            Path parcial = destinoZip.resolveSibling(destinoZip.getFileName() + ".parcial");
            try (OutputStream salida = Files.newOutputStream(parcial); ZipOutputStream zip = new ZipOutputStream(salida)) {
                agregarTexto(zip, MANIFIESTO, manifiesto());
                agregarArchivo(zip, copia, RestauracionPendiente.BASE);
                Path clave = home.resolve(RestauracionPendiente.CLAVE);
                if (Files.isRegularFile(clave)) {
                    agregarArchivo(zip, clave, RestauracionPendiente.CLAVE);
                }
                Path adjuntos = home.resolve(DirectorioDatos.ADJUNTOS);
                if (Files.isDirectory(adjuntos)) {
                    try (Stream<Path> archivos = Files.walk(adjuntos)) {
                        for (Path a : archivos.filter(Files::isRegularFile).sorted().toList()) {
                            agregarArchivo(zip, a, DirectorioDatos.ADJUNTOS + "/"
                                    + adjuntos.relativize(a).toString().replace('\\', '/'));
                        }
                    }
                }
            }
            // Recién completo, el ZIP toma su nombre: nunca queda un respaldo a medias con el nombre final.
            Files.move(parcial, destinoZip, StandardCopyOption.REPLACE_EXISTING);
            log.info("Respaldo creado: {} ({} bytes)", destinoZip, Files.size(destinoZip));
            return destinoZip;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo crear el respaldo " + destinoZip, e);
        } finally {
            if (temporal != null) {
                try {
                    borrar(temporal);
                } catch (IOException e) {
                    log.warn("No se pudo borrar la carpeta temporal {}", temporal, e);
                }
            }
        }
    }

    /**
     * Respaldo automático al cerrar ({@code JavaFxApplication.stop()}): si está habilitado y no hay una restauración
     * pendiente. Nunca lanza excepciones: un error no debe impedir cerrar la aplicación.
     */
    public void respaldoAutomaticoAlCerrar() {
        try {
            if (hayRestauracionPendiente() || !configuracionService.booleano("respaldo.automatico_al_cerrar")) {
                return;
            }
            crear(nuevoArchivo(PREFIJO_AUTOMATICO));
            podarAutomaticos(configuracionService.entero("respaldo.mantener"));
        } catch (RuntimeException e) {
            log.error("Falló el respaldo automático al cerrar", e);
        }
    }

    /** Borra los respaldos automáticos más antiguos y deja {@code mantener}. */
    void podarAutomaticos(int mantener) {
        List<Path> automaticos = archivos().stream()
                .filter(p -> p.getFileName().toString().startsWith(PREFIJO_AUTOMATICO))
                .toList();   // del más reciente al más antiguo
        for (Path viejo : automaticos.subList(Math.min(mantener, automaticos.size()), automaticos.size())) {
            try {
                Files.delete(viejo);
                log.info("Respaldo automático antiguo eliminado: {}", viejo.getFileName());
            } catch (IOException e) {
                log.warn("No se pudo eliminar el respaldo {}", viejo, e);
            }
        }
    }

    // ------------------------------------------------------------------ listar

    /** Respaldos de {@code respaldos/}, del más reciente al más antiguo. */
    public List<RespaldoDto> listar() {
        sesionUsuario.exigir(Permiso.RESPALDO);
        return archivos().stream().map(RespaldoService::dto).toList();
    }

    public String carpetaRespaldos() {
        return DirectorioDatos.home().resolve(DirectorioDatos.RESPALDOS).toAbsolutePath().toString();
    }

    private static List<Path> archivos() {
        Path carpeta = DirectorioDatos.home().resolve(DirectorioDatos.RESPALDOS);
        if (!Files.isDirectory(carpeta)) {
            return List.of();
        }
        try (Stream<Path> s = Files.list(carpeta)) {
            return s.filter(p -> {
                        String n = p.getFileName().toString();
                        return n.endsWith(".zip") && (n.startsWith(PREFIJO_MANUAL) || n.startsWith(PREFIJO_AUTOMATICO));
                    })
                    .sorted(Comparator.comparing(RespaldoService::modificado).reversed()
                            .thenComparing(Comparator.comparing(Path::getFileName).reversed()))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer la carpeta de respaldos", e);
        }
    }

    // ------------------------------------------------------------------ restaurar

    /**
     * Valida el respaldo, lo extrae en {@code restauracion/} y deja la marca: la restauración se aplica al reiniciar
     * la aplicación ({@link RestauracionPendiente}).
     *
     * @throws NegocioException si el archivo no es un respaldo válido de OdontoGest
     */
    public void programarRestauracion(Path zip) {
        sesionUsuario.exigir(Permiso.RESPALDO);
        Path home = DirectorioDatos.home();
        Path carpeta = home.resolve(RestauracionPendiente.CARPETA);
        try {
            borrar(carpeta);
            Files.deleteIfExists(home.resolve(RestauracionPendiente.MARCA));
            JsonObject manifiesto = extraer(zip, carpeta);
            validarBase(carpeta.resolve(RestauracionPendiente.BASE));
            Files.writeString(home.resolve(RestauracionPendiente.MARCA), zip.toAbsolutePath().toString());
            auditar("RESPALDO_PROGRAMAR_RESTAURACION", Map.of("archivo", zip.getFileName().toString(),
                    "fechaRespaldo", texto(manifiesto, "fecha")));
            log.warn("Restauración programada desde {}; se aplicará al reiniciar", zip);
        } catch (IOException e) {
            limpiarSinFallar(carpeta);
            throw new NegocioException("No se pudo preparar la restauración: " + e.getMessage());
        } catch (RuntimeException e) {
            limpiarSinFallar(carpeta);
            throw e;
        }
    }

    public boolean hayRestauracionPendiente() {
        return Files.exists(DirectorioDatos.home().resolve(RestauracionPendiente.MARCA));
    }

    /** Deshace {@link #programarRestauracion} antes de reiniciar. */
    public void cancelarRestauracion() {
        sesionUsuario.exigir(Permiso.RESPALDO);
        Path home = DirectorioDatos.home();
        try {
            Files.deleteIfExists(home.resolve(RestauracionPendiente.MARCA));
            borrar(home.resolve(RestauracionPendiente.CARPETA));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo cancelar la restauración", e);
        }
        auditar("RESPALDO_CANCELAR_RESTAURACION", null);
    }

    /** Extrae el ZIP en {@code carpeta} y devuelve el manifiesto validado. */
    private static JsonObject extraer(Path zip, Path carpeta) throws IOException {
        if (!Files.isRegularFile(zip)) {
            throw new NegocioException("No existe el archivo " + zip.getFileName() + ".");
        }
        Path destino = Files.createDirectories(carpeta).toAbsolutePath().normalize();
        JsonObject manifiesto;
        try (ZipFile archivo = new ZipFile(zip.toFile())) {
            ZipEntry entradaManifiesto = archivo.getEntry(MANIFIESTO);
            if (entradaManifiesto == null || archivo.getEntry(RestauracionPendiente.BASE) == null) {
                throw new NegocioException("El archivo no es un respaldo de OdontoGest (falta " + MANIFIESTO
                        + " o la base de datos).");
            }
            manifiesto = leerManifiesto(archivo, entradaManifiesto);
            Enumeration<? extends ZipEntry> entradas = archivo.entries();
            while (entradas.hasMoreElements()) {
                ZipEntry e = entradas.nextElement();
                Path salida = destino.resolve(e.getName()).normalize();
                if (!salida.startsWith(destino)) {
                    throw new NegocioException("El respaldo contiene una ruta no permitida: " + e.getName());
                }
                if (e.isDirectory()) {
                    Files.createDirectories(salida);
                    continue;
                }
                Files.createDirectories(salida.getParent());
                try (InputStream entrada = archivo.getInputStream(e)) {
                    Files.copy(entrada, salida, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (ZipException e) {
            throw new NegocioException("El archivo no es un ZIP válido o está dañado.");
        }
        Files.deleteIfExists(destino.resolve(MANIFIESTO));
        return manifiesto;
    }

    private static JsonObject leerManifiesto(ZipFile archivo, ZipEntry entrada) throws IOException {
        JsonObject m;
        try (InputStream in = archivo.getInputStream(entrada)) {
            m = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (JsonParseException | IllegalStateException e) {
            throw new NegocioException("El manifiesto del respaldo está dañado.");
        }
        if (!"OdontoGest".equals(texto(m, "aplicacion"))) {
            throw new NegocioException("El archivo no es un respaldo de OdontoGest.");
        }
        int esquema = m.has("esquema") ? m.get("esquema").getAsInt() : 0;
        if (esquema < 1 || esquema > VERSION_ESQUEMA) {
            throw new NegocioException("El respaldo es de una versión más reciente de OdontoGest (esquema " + esquema
                    + "); actualice la aplicación antes de restaurarlo.");
        }
        return m;
    }

    /** La base extraída debe ser SQLite, pasar {@code integrity_check} y tener las tablas principales. */
    static void validarBase(Path base) throws IOException {
        byte[] cabecera = new byte[CABECERA_SQLITE.length];
        try (InputStream in = Files.newInputStream(base)) {
            if (in.readNBytes(cabecera, 0, cabecera.length) != cabecera.length || !Arrays.equals(cabecera, CABECERA_SQLITE)) {
                throw new NegocioException("La base de datos del respaldo no es un archivo SQLite válido.");
            }
        }
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + base.toAbsolutePath().toString().replace('\\', '/'));
             Statement st = c.createStatement()) {
            try (ResultSet rs = st.executeQuery("PRAGMA integrity_check")) {
                String resultado = rs.next() ? rs.getString(1) : "";
                if (!"ok".equalsIgnoreCase(resultado)) {
                    throw new NegocioException("La base de datos del respaldo está dañada: " + resultado);
                }
            }
            for (String tabla : TABLAS_REQUERIDAS) {
                try (ResultSet rs = st.executeQuery(
                        "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = '" + tabla + "'")) {
                    if (!rs.next() || rs.getInt(1) != 1) {
                        throw new NegocioException("La base de datos del respaldo no tiene la tabla «" + tabla + "».");
                    }
                }
            }
        } catch (SQLException e) {
            throw new NegocioException("No se pudo abrir la base de datos del respaldo: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ apoyo

    private String manifiesto() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("aplicacion", "OdontoGest");
        BuildProperties build = buildProperties.getIfAvailable();
        m.put("version", build == null ? "desarrollo" : build.getVersion());
        m.put("fecha", LocalDateTimeConverter.aTexto(LocalDateTime.now(reloj)));
        m.put("esquema", VERSION_ESQUEMA);
        return gson.toJson(m);
    }

    private Path nuevoArchivo(String prefijo) {
        Path carpeta = DirectorioDatos.home().resolve(DirectorioDatos.RESPALDOS);
        try {
            Files.createDirectories(carpeta);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        String base = prefijo + SELLO.format(LocalDateTime.now(reloj));
        Path archivo = carpeta.resolve(base + ".zip");
        for (int i = 2; Files.exists(archivo); i++) {
            archivo = carpeta.resolve(base + "-" + i + ".zip");
        }
        return archivo;
    }

    private void auditar(String accion, Object detalle) {
        new TransactionTemplate(transacciones).executeWithoutResult(s -> auditoria.registrar(accion, "respaldo", null, detalle));
    }

    private static void agregarTexto(ZipOutputStream zip, String nombre, String texto) throws IOException {
        zip.putNextEntry(new ZipEntry(nombre));
        zip.write(texto.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static void agregarArchivo(ZipOutputStream zip, Path archivo, String nombre) throws IOException {
        zip.putNextEntry(new ZipEntry(nombre));
        Files.copy(archivo, zip);
        zip.closeEntry();
    }

    private static RespaldoDto dto(Path p) {
        try {
            return new RespaldoDto(p.getFileName().toString(), p.toAbsolutePath().toString(), modificado(p),
                    Files.size(p), p.getFileName().toString().startsWith(PREFIJO_AUTOMATICO));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static LocalDateTime modificado(Path p) {
        try {
            return LocalDateTime.ofInstant(Files.getLastModifiedTime(p).toInstant(), ZoneId.systemDefault())
                    .truncatedTo(ChronoUnit.SECONDS);
        } catch (IOException e) {
            return LocalDateTime.MIN;
        }
    }

    private static String texto(JsonObject o, String clave) {
        return o.has(clave) && o.get(clave).isJsonPrimitive() ? o.get(clave).getAsString() : "";
    }

    private static void borrar(Path carpeta) throws IOException {
        if (!Files.exists(carpeta)) {
            return;
        }
        try (Stream<Path> s = Files.walk(carpeta)) {
            for (Path p : s.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(p);
            }
        }
    }

    private static void limpiarSinFallar(Path carpeta) {
        try {
            borrar(carpeta);
        } catch (IOException e) {
            log.warn("No se pudo limpiar {}", carpeta, e);
        }
    }
}
