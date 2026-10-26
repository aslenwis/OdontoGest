package com.odontogest.servicio;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.odontogest.config.DirectorioDatosASEJ;
import com.odontogest.config.RestauracionPendienteASEJ;
import com.odontogest.excepcion.NegocioExceptionASEJ;
import com.odontogest.persistencia.conversor.LocalDateTimeConverterASEJ;
import com.odontogest.seguridad.PermisoASEJ;
import com.odontogest.seguridad.SesionUsuarioASEJ;
import com.odontogest.servicio.dto.RespaldoDtoASEJ;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class RespaldoServiceASEJ {

    public static final int VERSION_ESQUEMAASEJ = 1;
    public static final String PREFIJO_MANUALASEJ = "odontogest-respaldo-";
    public static final String PREFIJO_AUTOMATICOASEJ = "odontogest-auto-";
    static final String MANIFIESTOASEJ = "manifest.json";
    private static final DateTimeFormatter SELLOASEJ = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final byte[] CABECERA_SQLITEASEJ = "SQLite format 3\0".getBytes(StandardCharsets.US_ASCII);
    private static final List<String> TABLAS_REQUERIDASASEJ = List.of("configuracion", "usuario", "paciente", "odontograma");

    private final Clock relojASEJ;
    private final JdbcTemplate jdbcTemplateASEJ;
    private final ConfiguracionServiceASEJ configuracionServiceASEJ;
    private final AuditoriaServiceASEJ auditoriaASEJ;
    private final SesionUsuarioASEJ sesionUsuarioASEJ;
    private final PlatformTransactionManager transaccionesASEJ;
    private final ObjectProvider<BuildProperties> buildPropertiesASEJ;
    private final Gson gsonASEJ = new Gson();

    public RespaldoDtoASEJ crearAhoraASEJ() {
        sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.RESPALDO);
        Path zipASEJ = crearASEJ(nuevoArchivoASEJ(PREFIJO_MANUALASEJ));
        auditarASEJ("RESPALDO_CREAR", Map.of("archivo", zipASEJ.getFileName().toString()));
        return dtoASEJ(zipASEJ);
    }

    public Path crearASEJ(Path destinoZipASEJ) {
        Path homeASEJ = DirectorioDatosASEJ.homeASEJ();
        Path temporalASEJ = null;
        try {
            temporalASEJ = Files.createTempDirectory("odontogest-respaldo-");
            Path copiaASEJ = temporalASEJ.resolve(RestauracionPendienteASEJ.BASEASEJ);
            jdbcTemplateASEJ.execute("VACUUM INTO '" + copiaASEJ.toAbsolutePath().toString().replace("'", "''") + "'");
            Path parcialASEJ = destinoZipASEJ.resolveSibling(destinoZipASEJ.getFileName() + ".parcial");
            try (OutputStream salidaASEJ = Files.newOutputStream(parcialASEJ); ZipOutputStream zipASEJ = new ZipOutputStream(salidaASEJ)) {
                agregarTextoASEJ(zipASEJ, MANIFIESTOASEJ, manifiestoASEJ());
                agregarArchivoASEJ(zipASEJ, copiaASEJ, RestauracionPendienteASEJ.BASEASEJ);
                Path claveASEJ = homeASEJ.resolve(RestauracionPendienteASEJ.CLAVEASEJ);
                if (Files.isRegularFile(claveASEJ)) {
                    agregarArchivoASEJ(zipASEJ, claveASEJ, RestauracionPendienteASEJ.CLAVEASEJ);
                }
                Path adjuntosASEJ = homeASEJ.resolve(DirectorioDatosASEJ.ADJUNTOSASEJ);
                if (Files.isDirectory(adjuntosASEJ)) {
                    try (Stream<Path> archivosASEJ = Files.walk(adjuntosASEJ)) {
                        for (Path aASEJ : archivosASEJ.filter(Files::isRegularFile).sorted().toList()) {
                            agregarArchivoASEJ(zipASEJ, aASEJ, DirectorioDatosASEJ.ADJUNTOSASEJ + "/"
                                    + adjuntosASEJ.relativize(aASEJ).toString().replace('\\', '/'));
                        }
                    }
                }
            }
            Files.move(parcialASEJ, destinoZipASEJ, StandardCopyOption.REPLACE_EXISTING);
            logASEJ.info("Respaldo creado: {} ({} bytes)", destinoZipASEJ, Files.size(destinoZipASEJ));
            return destinoZipASEJ;
        } catch (IOException eASEJ) {
            throw new UncheckedIOException("No se pudo crear el respaldo " + destinoZipASEJ, eASEJ);
        } finally {
            if (temporalASEJ != null) {
                try {
                    borrarASEJ(temporalASEJ);
                } catch (IOException eASEJ) {
                    logASEJ.warn("No se pudo borrar la carpeta temporal {}", temporalASEJ, eASEJ);
                }
            }
        }
    }

    public void respaldoAutomaticoAlCerrarASEJ() {
        try {
            if (hayRestauracionPendienteASEJ() || !configuracionServiceASEJ.booleanoASEJ("respaldo.automatico_al_cerrar")) {
                return;
            }
            crearASEJ(nuevoArchivoASEJ(PREFIJO_AUTOMATICOASEJ));
            podarAutomaticosASEJ(configuracionServiceASEJ.enteroASEJ("respaldo.mantener"));
        } catch (RuntimeException eASEJ) {
            logASEJ.error("Falló el respaldo automático al cerrar", eASEJ);
        }
    }

    void podarAutomaticosASEJ(int mantenerASEJ) {
        List<Path> automaticosASEJ = archivosASEJ().stream()
                .filter(pASEJ -> pASEJ.getFileName().toString().startsWith(PREFIJO_AUTOMATICOASEJ))
                .toList();
        for (Path viejoASEJ : automaticosASEJ.subList(Math.min(mantenerASEJ, automaticosASEJ.size()), automaticosASEJ.size())) {
            try {
                Files.delete(viejoASEJ);
                logASEJ.info("Respaldo automático antiguo eliminado: {}", viejoASEJ.getFileName());
            } catch (IOException eASEJ) {
                logASEJ.warn("No se pudo eliminar el respaldo {}", viejoASEJ, eASEJ);
            }
        }
    }

    public List<RespaldoDtoASEJ> listarASEJ() {
        sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.RESPALDO);
        return archivosASEJ().stream().map(RespaldoServiceASEJ::dtoASEJ).toList();
    }

    public String carpetaRespaldosASEJ() {
        return DirectorioDatosASEJ.homeASEJ().resolve(DirectorioDatosASEJ.RESPALDOSASEJ).toAbsolutePath().toString();
    }

    private static List<Path> archivosASEJ() {
        Path carpetaASEJ = DirectorioDatosASEJ.homeASEJ().resolve(DirectorioDatosASEJ.RESPALDOSASEJ);
        if (!Files.isDirectory(carpetaASEJ)) {
            return List.of();
        }
        try (Stream<Path> sASEJ = Files.list(carpetaASEJ)) {
            return sASEJ.filter(pASEJ -> {
                        String nASEJ = pASEJ.getFileName().toString();
                        return nASEJ.endsWith(".zip") && (nASEJ.startsWith(PREFIJO_MANUALASEJ) || nASEJ.startsWith(PREFIJO_AUTOMATICOASEJ));
                    })
                    .sorted(Comparator.comparing(RespaldoServiceASEJ::modificadoASEJ).reversed()
                            .thenComparing(Comparator.comparing(Path::getFileName).reversed()))
                    .toList();
        } catch (IOException eASEJ) {
            throw new UncheckedIOException("No se pudo leer la carpeta de respaldos", eASEJ);
        }
    }

    public void programarRestauracionASEJ(Path zipASEJ) {
        sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.RESPALDO);
        Path homeASEJ = DirectorioDatosASEJ.homeASEJ();
        Path carpetaASEJ = homeASEJ.resolve(RestauracionPendienteASEJ.CARPETAASEJ);
        try {
            borrarASEJ(carpetaASEJ);
            Files.deleteIfExists(homeASEJ.resolve(RestauracionPendienteASEJ.MARCAASEJ));
            JsonObject manifiestoASEJ = extraerASEJ(zipASEJ, carpetaASEJ);
            validarBaseASEJ(carpetaASEJ.resolve(RestauracionPendienteASEJ.BASEASEJ));
            Files.writeString(homeASEJ.resolve(RestauracionPendienteASEJ.MARCAASEJ), zipASEJ.toAbsolutePath().toString());
            auditarASEJ("RESPALDO_PROGRAMAR_RESTAURACION", Map.of("archivo", zipASEJ.getFileName().toString(),
                    "fechaRespaldo", textoASEJ(manifiestoASEJ, "fecha")));
            logASEJ.warn("Restauración programada desde {}; se aplicará al reiniciar", zipASEJ);
        } catch (IOException eASEJ) {
            limpiarSinFallarASEJ(carpetaASEJ);
            throw new NegocioExceptionASEJ("No se pudo preparar la restauración: " + eASEJ.getMessage());
        } catch (RuntimeException eASEJ) {
            limpiarSinFallarASEJ(carpetaASEJ);
            throw eASEJ;
        }
    }

    public boolean hayRestauracionPendienteASEJ() {
        return Files.exists(DirectorioDatosASEJ.homeASEJ().resolve(RestauracionPendienteASEJ.MARCAASEJ));
    }

    public void cancelarRestauracionASEJ() {
        sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.RESPALDO);
        Path homeASEJ = DirectorioDatosASEJ.homeASEJ();
        try {
            Files.deleteIfExists(homeASEJ.resolve(RestauracionPendienteASEJ.MARCAASEJ));
            borrarASEJ(homeASEJ.resolve(RestauracionPendienteASEJ.CARPETAASEJ));
        } catch (IOException eASEJ) {
            throw new UncheckedIOException("No se pudo cancelar la restauración", eASEJ);
        }
        auditarASEJ("RESPALDO_CANCELAR_RESTAURACION", null);
    }

    private static JsonObject extraerASEJ(Path zipASEJ, Path carpetaASEJ) throws IOException {
        if (!Files.isRegularFile(zipASEJ)) {
            throw new NegocioExceptionASEJ("No existe el archivo " + zipASEJ.getFileName() + ".");
        }
        Path destinoASEJ = Files.createDirectories(carpetaASEJ).toAbsolutePath().normalize();
        JsonObject manifiestoASEJ;
        try (ZipFile archivoASEJ = new ZipFile(zipASEJ.toFile())) {
            ZipEntry entradaManifiestoASEJ = archivoASEJ.getEntry(MANIFIESTOASEJ);
            if (entradaManifiestoASEJ == null || archivoASEJ.getEntry(RestauracionPendienteASEJ.BASEASEJ) == null) {
                throw new NegocioExceptionASEJ("El archivo no es un respaldo de OdontoGest (falta " + MANIFIESTOASEJ
                        + " o la base de datos).");
            }
            manifiestoASEJ = leerManifiestoASEJ(archivoASEJ, entradaManifiestoASEJ);
            Enumeration<? extends ZipEntry> entradasASEJ = archivoASEJ.entries();
            while (entradasASEJ.hasMoreElements()) {
                ZipEntry eASEJ = entradasASEJ.nextElement();
                Path salidaASEJ = destinoASEJ.resolve(eASEJ.getName()).normalize();
                if (!salidaASEJ.startsWith(destinoASEJ)) {
                    throw new NegocioExceptionASEJ("El respaldo contiene una ruta no permitida: " + eASEJ.getName());
                }
                if (eASEJ.isDirectory()) {
                    Files.createDirectories(salidaASEJ);
                    continue;
                }
                Files.createDirectories(salidaASEJ.getParent());
                try (InputStream entradaASEJ = archivoASEJ.getInputStream(eASEJ)) {
                    Files.copy(entradaASEJ, salidaASEJ, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (ZipException eASEJ) {
            throw new NegocioExceptionASEJ("El archivo no es un ZIP válido o está dañado.");
        }
        Files.deleteIfExists(destinoASEJ.resolve(MANIFIESTOASEJ));
        return manifiestoASEJ;
    }

    private static JsonObject leerManifiestoASEJ(ZipFile archivoASEJ, ZipEntry entradaASEJ) throws IOException {
        JsonObject mASEJ;
        try (InputStream inASEJ = archivoASEJ.getInputStream(entradaASEJ)) {
            mASEJ = JsonParser.parseString(new String(inASEJ.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (JsonParseException | IllegalStateException eASEJ) {
            throw new NegocioExceptionASEJ("El manifiesto del respaldo está dañado.");
        }
        if (!"OdontoGest".equals(textoASEJ(mASEJ, "aplicacion"))) {
            throw new NegocioExceptionASEJ("El archivo no es un respaldo de OdontoGest.");
        }
        int esquemaASEJ = mASEJ.has("esquema") ? mASEJ.get("esquema").getAsInt() : 0;
        if (esquemaASEJ < 1 || esquemaASEJ > VERSION_ESQUEMAASEJ) {
            throw new NegocioExceptionASEJ("El respaldo es de una versión más reciente de OdontoGest (esquema " + esquemaASEJ
                    + "); actualice la aplicación antes de restaurarlo.");
        }
        return mASEJ;
    }

    static void validarBaseASEJ(Path baseASEJ) throws IOException {
        byte[] cabeceraASEJ = new byte[CABECERA_SQLITEASEJ.length];
        try (InputStream inASEJ = Files.newInputStream(baseASEJ)) {
            if (inASEJ.readNBytes(cabeceraASEJ, 0, cabeceraASEJ.length) != cabeceraASEJ.length || !Arrays.equals(cabeceraASEJ, CABECERA_SQLITEASEJ)) {
                throw new NegocioExceptionASEJ("La base de datos del respaldo no es un archivo SQLite válido.");
            }
        }
        try (Connection cASEJ = DriverManager.getConnection("jdbc:sqlite:" + baseASEJ.toAbsolutePath().toString().replace('\\', '/'));
             Statement stASEJ = cASEJ.createStatement()) {
            try (ResultSet rsASEJ = stASEJ.executeQuery("PRAGMA integrity_check")) {
                String resultadoASEJ = rsASEJ.next() ? rsASEJ.getString(1) : "";
                if (!"ok".equalsIgnoreCase(resultadoASEJ)) {
                    throw new NegocioExceptionASEJ("La base de datos del respaldo está dañada: " + resultadoASEJ);
                }
            }
            for (String tablaASEJ : TABLAS_REQUERIDASASEJ) {
                try (ResultSet rsASEJ = stASEJ.executeQuery(
                        "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = '" + tablaASEJ + "'")) {
                    if (!rsASEJ.next() || rsASEJ.getInt(1) != 1) {
                        throw new NegocioExceptionASEJ("La base de datos del respaldo no tiene la tabla «" + tablaASEJ + "».");
                    }
                }
            }
        } catch (SQLException eASEJ) {
            throw new NegocioExceptionASEJ("No se pudo abrir la base de datos del respaldo: " + eASEJ.getMessage());
        }
    }

    private String manifiestoASEJ() {
        Map<String, Object> mASEJ = new LinkedHashMap<>();
        mASEJ.put("aplicacion", "OdontoGest");
        BuildProperties buildASEJ = buildPropertiesASEJ.getIfAvailable();
        mASEJ.put("version", buildASEJ == null ? "desarrollo" : buildASEJ.getVersion());
        mASEJ.put("fecha", LocalDateTimeConverterASEJ.aTextoASEJ(LocalDateTime.now(relojASEJ)));
        mASEJ.put("esquema", VERSION_ESQUEMAASEJ);
        return gsonASEJ.toJson(mASEJ);
    }

    private Path nuevoArchivoASEJ(String prefijoASEJ) {
        Path carpetaASEJ = DirectorioDatosASEJ.homeASEJ().resolve(DirectorioDatosASEJ.RESPALDOSASEJ);
        try {
            Files.createDirectories(carpetaASEJ);
        } catch (IOException eASEJ) {
            throw new UncheckedIOException(eASEJ);
        }
        String baseASEJ = prefijoASEJ + SELLOASEJ.format(LocalDateTime.now(relojASEJ));
        Path archivoASEJ = carpetaASEJ.resolve(baseASEJ + ".zip");
        for (int iASEJ = 2; Files.exists(archivoASEJ); iASEJ++) {
            archivoASEJ = carpetaASEJ.resolve(baseASEJ + "-" + iASEJ + ".zip");
        }
        return archivoASEJ;
    }

    private void auditarASEJ(String accionASEJ, Object detalleASEJ) {
        new TransactionTemplate(transaccionesASEJ).executeWithoutResult(sASEJ -> auditoriaASEJ.registrarASEJ(accionASEJ, "respaldo", null, detalleASEJ));
    }

    private static void agregarTextoASEJ(ZipOutputStream zipASEJ, String nombreASEJ, String textoASEJ) throws IOException {
        zipASEJ.putNextEntry(new ZipEntry(nombreASEJ));
        zipASEJ.write(textoASEJ.getBytes(StandardCharsets.UTF_8));
        zipASEJ.closeEntry();
    }

    private static void agregarArchivoASEJ(ZipOutputStream zipASEJ, Path archivoASEJ, String nombreASEJ) throws IOException {
        zipASEJ.putNextEntry(new ZipEntry(nombreASEJ));
        Files.copy(archivoASEJ, zipASEJ);
        zipASEJ.closeEntry();
    }

    private static RespaldoDtoASEJ dtoASEJ(Path pASEJ) {
        try {
            return new RespaldoDtoASEJ(pASEJ.getFileName().toString(), pASEJ.toAbsolutePath().toString(), modificadoASEJ(pASEJ),
                    Files.size(pASEJ), pASEJ.getFileName().toString().startsWith(PREFIJO_AUTOMATICOASEJ));
        } catch (IOException eASEJ) {
            throw new UncheckedIOException(eASEJ);
        }
    }

    private static LocalDateTime modificadoASEJ(Path pASEJ) {
        try {
            return LocalDateTime.ofInstant(Files.getLastModifiedTime(pASEJ).toInstant(), ZoneId.systemDefault())
                    .truncatedTo(ChronoUnit.SECONDS);
        } catch (IOException eASEJ) {
            return LocalDateTime.MIN;
        }
    }

    private static String textoASEJ(JsonObject oASEJ, String claveASEJ) {
        return oASEJ.has(claveASEJ) && oASEJ.get(claveASEJ).isJsonPrimitive() ? oASEJ.get(claveASEJ).getAsString() : "";
    }

    private static void borrarASEJ(Path carpetaASEJ) throws IOException {
        if (!Files.exists(carpetaASEJ)) {
            return;
        }
        try (Stream<Path> sASEJ = Files.walk(carpetaASEJ)) {
            for (Path pASEJ : sASEJ.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(pASEJ);
            }
        }
    }

    private static void limpiarSinFallarASEJ(Path carpetaASEJ) {
        try {
            borrarASEJ(carpetaASEJ);
        } catch (IOException eASEJ) {
            logASEJ.warn("No se pudo limpiar {}", carpetaASEJ, eASEJ);
        }
    }
}
