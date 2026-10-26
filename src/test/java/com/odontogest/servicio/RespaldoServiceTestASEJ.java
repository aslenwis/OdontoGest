package com.odontogest.servicio;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.odontogest.PruebaIntegracionASEJ;
import com.odontogest.config.RestauracionPendienteASEJ;
import com.odontogest.excepcion.NegocioExceptionASEJ;
import com.odontogest.excepcion.PermisoDenegadoExceptionASEJ;
import com.odontogest.integracion.seguridad.CifradorSecretosASEJ;
import com.odontogest.modelo.SexoASEJ;
import com.odontogest.modelo.TipoDocumentoASEJ;
import com.odontogest.servicio.dto.PacienteFormASEJ;
import com.odontogest.servicio.dto.RespaldoDtoASEJ;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RespaldoServiceTestASEJ extends PruebaIntegracionASEJ {

    @Autowired private RespaldoServiceASEJ respaldoServiceASEJ;
    @Autowired private PacienteServiceASEJ pacienteServiceASEJ;
    @Autowired private CifradorSecretosASEJ cifradorASEJ;

    @TempDir
    Path temporalASEJ;

    @AfterEach
    void sinRestauracionPendienteASEJ() throws IOException {
        Files.deleteIfExists(HOME_PRUEBASASEJ.resolve(RestauracionPendienteASEJ.MARCAASEJ));
        borrarASEJ(HOME_PRUEBASASEJ.resolve(RestauracionPendienteASEJ.CARPETAASEJ));
    }

    private static void borrarASEJ(Path carpetaASEJ) throws IOException {
        if (Files.exists(carpetaASEJ)) {
            try (Stream<Path> sASEJ = Files.walk(carpetaASEJ)) {
                for (Path pASEJ : sASEJ.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(pASEJ);
                }
            }
        }
    }

    private static long contarASEJ(Path baseASEJ, String sqlASEJ) throws SQLException {
        try (Connection cASEJ = DriverManager.getConnection("jdbc:sqlite:" + baseASEJ.toString().replace('\\', '/'));
             ResultSet rsASEJ = cASEJ.createStatement().executeQuery(sqlASEJ)) {
            rsASEJ.next();
            return rsASEJ.getLong(1);
        }
    }

    private Path zipManualASEJ(String... entradasASEJ) throws IOException {
        Path zipASEJ = temporalASEJ.resolve("manual-" + System.nanoTime() + ".zip");
        try (OutputStream outASEJ = Files.newOutputStream(zipASEJ); ZipOutputStream zASEJ = new ZipOutputStream(outASEJ)) {
            for (int iASEJ = 0; iASEJ < entradasASEJ.length; iASEJ += 2) {
                zASEJ.putNextEntry(new ZipEntry(entradasASEJ[iASEJ]));
                zASEJ.write(entradasASEJ[iASEJ + 1].getBytes(StandardCharsets.UTF_8));
                zASEJ.closeEntry();
            }
        }
        return zipASEJ;
    }

    private static final String MANIFIESTOASEJ = "{\"aplicacion\":\"OdontoGest\",\"esquema\":1}";

    @Test
    void crearAhora_zipConManifiestoBaseYAdjuntosASEJ() throws Exception {
        Files.createDirectories(HOME_PRUEBASASEJ.resolve("adjuntos/99"));
        Files.writeString(HOME_PRUEBASASEJ.resolve("adjuntos/99/nota.txt"), "adjunto de prueba");
        cifradorASEJ.cifrarASEJ("crea clave.key si aún no existe");
        iniciarSesionASEJ("admin");

        RespaldoDtoASEJ rASEJ = respaldoServiceASEJ.crearAhoraASEJ();

        assertThat(rASEJ.nombreASEJ()).startsWith(RespaldoServiceASEJ.PREFIJO_MANUALASEJ).endsWith(".zip");
        assertThat(rASEJ.automaticoASEJ()).isFalse();
        assertThat(rASEJ.tamanioBytesASEJ()).isPositive();
        assertThat(respaldoServiceASEJ.listarASEJ()).extracting(RespaldoDtoASEJ::nombreASEJ).contains(rASEJ.nombreASEJ());
        Path zipASEJ = Path.of(rASEJ.rutaASEJ());
        assertThat(zipASEJ.getParent()).isEqualTo(HOME_PRUEBASASEJ.resolve("respaldos"));
        try (ZipFile zASEJ = new ZipFile(zipASEJ.toFile())) {
            assertThat(zASEJ.getEntry("adjuntos/99/nota.txt")).isNotNull();
            assertThat(zASEJ.getEntry("clave.key")).as("sin la clave no se descifran los secretos").isNotNull();
            try (InputStream inASEJ = zASEJ.getInputStream(zASEJ.getEntry("manifest.json"))) {
                JsonObject mASEJ = JsonParser.parseString(new String(inASEJ.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
                assertThat(mASEJ.get("aplicacion").getAsString()).isEqualTo("OdontoGest");
                assertThat(mASEJ.get("esquema").getAsInt()).isEqualTo(RespaldoServiceASEJ.VERSION_ESQUEMAASEJ);
                assertThat(mASEJ.get("fecha").getAsString()).startsWith(LocalDate.now().toString());
            }
            Path baseASEJ = temporalASEJ.resolve("extraida.db");
            try (InputStream inASEJ = zASEJ.getInputStream(zASEJ.getEntry("odontogest.db"))) {
                Files.copy(inASEJ, baseASEJ);
            }
            assertThat(contarASEJ(baseASEJ, "SELECT COUNT(*) FROM usuario")).isGreaterThanOrEqualTo(3);
        }
        try (Stream<Path> sASEJ = Files.list(zipASEJ.getParent())) {
            assertThat(sASEJ.map(pASEJ -> pASEJ.getFileName().toString())).noneMatch(nASEJ -> nASEJ.endsWith(".parcial"));
        }
    }

    @Test
    void soloElAdministradorRespaldaASEJ() {
        iniciarSesionASEJ("recepcion");
        assertThatThrownBy(() -> respaldoServiceASEJ.crearAhoraASEJ()).isInstanceOf(PermisoDenegadoExceptionASEJ.class);
        assertThatThrownBy(() -> respaldoServiceASEJ.listarASEJ()).isInstanceOf(PermisoDenegadoExceptionASEJ.class);
        assertThatThrownBy(() -> respaldoServiceASEJ.programarRestauracionASEJ(temporalASEJ.resolve("x.zip")))
                .isInstanceOf(PermisoDenegadoExceptionASEJ.class);
    }

    @Test
    void restauracionProgramada_seAplicaAlReiniciarASEJ() throws Exception {
        iniciarSesionASEJ("admin");
        RespaldoDtoASEJ rASEJ = respaldoServiceASEJ.crearAhoraASEJ();
        iniciarSesionASEJ("recepcion");
        long posteriorASEJ = pacienteServiceASEJ.guardarASEJ(new PacienteFormASEJ(null, TipoDocumentoASEJ.DNI, dniAleatorioASEJ(), "Paciente",
                "Posterior" + sufijoASEJ(), null, LocalDate.now().minusYears(30), SexoASEJ.F, null, null, null, null, null, null,
                null, null, null, true));

        iniciarSesionASEJ("admin");
        respaldoServiceASEJ.programarRestauracionASEJ(Path.of(rASEJ.rutaASEJ()));
        assertThat(respaldoServiceASEJ.hayRestauracionPendienteASEJ()).isTrue();
        Path carpetaASEJ = HOME_PRUEBASASEJ.resolve(RestauracionPendienteASEJ.CARPETAASEJ);
        assertThat(carpetaASEJ.resolve("odontogest.db")).exists();
        assertThat(carpetaASEJ.resolve("manifest.json")).doesNotExist();

        Path otroHomeASEJ = Files.createDirectories(temporalASEJ.resolve("home"));
        Files.writeString(otroHomeASEJ.resolve("odontogest.db"), "base que se reemplaza");
        copiarASEJ(carpetaASEJ, otroHomeASEJ.resolve(RestauracionPendienteASEJ.CARPETAASEJ));
        Files.copy(HOME_PRUEBASASEJ.resolve(RestauracionPendienteASEJ.MARCAASEJ), otroHomeASEJ.resolve(RestauracionPendienteASEJ.MARCAASEJ));
        assertThat(RestauracionPendienteASEJ.aplicarSiExisteASEJ(otroHomeASEJ)).isTrue();

        Path restauradaASEJ = otroHomeASEJ.resolve("odontogest.db");
        assertThat(contarASEJ(restauradaASEJ, "SELECT COUNT(*) FROM paciente WHERE id = " + posteriorASEJ)).isZero();
        assertThat(contarASEJ(restauradaASEJ, "SELECT COUNT(*) FROM usuario WHERE username = 'admin'")).isEqualTo(1);
        assertThat(otroHomeASEJ.resolve("clave.key")).exists();

        respaldoServiceASEJ.cancelarRestauracionASEJ();
        assertThat(respaldoServiceASEJ.hayRestauracionPendienteASEJ()).isFalse();
        assertThat(carpetaASEJ).doesNotExist();
    }

    @Test
    void archivosQueNoSonRespaldos_seRechazanASEJ() throws IOException {
        iniciarSesionASEJ("admin");
        Path textoASEJ = Files.writeString(temporalASEJ.resolve("no-es-zip.zip"), "hola");
        assertThatThrownBy(() -> respaldoServiceASEJ.programarRestauracionASEJ(textoASEJ))
                .isInstanceOf(NegocioExceptionASEJ.class).hasMessageContaining("ZIP");
        assertThatThrownBy(() -> respaldoServiceASEJ.programarRestauracionASEJ(zipManualASEJ("odontogest.db", "x")))
                .isInstanceOf(NegocioExceptionASEJ.class).hasMessageContaining("manifest.json");
        assertThatThrownBy(() -> respaldoServiceASEJ.programarRestauracionASEJ(zipManualASEJ(
                "manifest.json", "{\"aplicacion\":\"Otra\",\"esquema\":1}", "odontogest.db", "x")))
                .isInstanceOf(NegocioExceptionASEJ.class).hasMessageContaining("no es un respaldo de OdontoGest");
        assertThatThrownBy(() -> respaldoServiceASEJ.programarRestauracionASEJ(zipManualASEJ(
                "manifest.json", "{\"aplicacion\":\"OdontoGest\",\"esquema\":99}", "odontogest.db", "x")))
                .isInstanceOf(NegocioExceptionASEJ.class).hasMessageContaining("más reciente");
        assertThatThrownBy(() -> respaldoServiceASEJ.programarRestauracionASEJ(zipManualASEJ(
                "manifest.json", MANIFIESTOASEJ, "odontogest.db", "no soy sqlite")))
                .isInstanceOf(NegocioExceptionASEJ.class).hasMessageContaining("SQLite");
        assertThatThrownBy(() -> respaldoServiceASEJ.programarRestauracionASEJ(zipManualASEJ(
                "manifest.json", MANIFIESTOASEJ, "odontogest.db", "x", "../fuera.txt", "x")))
                .isInstanceOf(NegocioExceptionASEJ.class).hasMessageContaining("ruta no permitida");
        assertThat(HOME_PRUEBASASEJ.resolveSibling("fuera.txt")).doesNotExist();
        assertThat(respaldoServiceASEJ.hayRestauracionPendienteASEJ()).isFalse();
        assertThat(HOME_PRUEBASASEJ.resolve(RestauracionPendienteASEJ.CARPETAASEJ)).doesNotExist();
    }

    @Test
    void baseSinLasTablasDeOdontoGest_seRechazaASEJ() throws Exception {
        iniciarSesionASEJ("admin");
        Path ajenaASEJ = temporalASEJ.resolve("ajena.db");
        try (Connection cASEJ = DriverManager.getConnection("jdbc:sqlite:" + ajenaASEJ.toString().replace('\\', '/'))) {
            cASEJ.createStatement().execute("CREATE TABLE cliente (id INTEGER PRIMARY KEY)");
        }
        Path zipASEJ = temporalASEJ.resolve("ajena.zip");
        try (OutputStream outASEJ = Files.newOutputStream(zipASEJ); ZipOutputStream zASEJ = new ZipOutputStream(outASEJ)) {
            zASEJ.putNextEntry(new ZipEntry("manifest.json"));
            zASEJ.write(MANIFIESTOASEJ.getBytes(StandardCharsets.UTF_8));
            zASEJ.closeEntry();
            zASEJ.putNextEntry(new ZipEntry("odontogest.db"));
            Files.copy(ajenaASEJ, zASEJ);
            zASEJ.closeEntry();
        }
        assertThatThrownBy(() -> respaldoServiceASEJ.programarRestauracionASEJ(zipASEJ))
                .isInstanceOf(NegocioExceptionASEJ.class).hasMessageContaining("configuracion");
    }

    @Test
    void automaticoAlCerrar_conservaSoloLosUltimosASEJ() throws IOException {
        Path carpetaASEJ = HOME_PRUEBASASEJ.resolve("respaldos");
        Instant baseASEJ = Instant.now().minus(30, ChronoUnit.DAYS);
        for (int iASEJ = 1; iASEJ <= 4; iASEJ++) {
            Path viejoASEJ = Files.writeString(carpetaASEJ.resolve(RespaldoServiceASEJ.PREFIJO_AUTOMATICOASEJ + "2000010" + iASEJ + "-000000.zip"), "x");
            Files.setLastModifiedTime(viejoASEJ, FileTime.from(baseASEJ.plus(iASEJ, ChronoUnit.HOURS)));
        }
        Path manualAntiguoASEJ = Files.writeString(carpetaASEJ.resolve(RespaldoServiceASEJ.PREFIJO_MANUALASEJ + "20000101-000000.zip"), "x");
        Files.setLastModifiedTime(manualAntiguoASEJ, FileTime.from(baseASEJ));
        respaldoServiceASEJ.respaldoAutomaticoAlCerrarASEJ();
        assertThat(automaticosASEJ(carpetaASEJ)).as("los 4 de prueba + el nuevo, bajo el límite de 10").hasSize(5);

        respaldoServiceASEJ.podarAutomaticosASEJ(2);
        List<String> quedanASEJ = automaticosASEJ(carpetaASEJ);
        assertThat(quedanASEJ).hasSize(2);
        assertThat(quedanASEJ).anyMatch(nASEJ -> nASEJ.endsWith("20000104-000000.zip"));
        assertThat(quedanASEJ).noneMatch(nASEJ -> nASEJ.endsWith("20000101-000000.zip"));
        assertThat(manualAntiguoASEJ).as("los respaldos manuales no se podan").exists();
        Files.delete(manualAntiguoASEJ);
    }

    @Test
    void automaticoAlCerrar_noSeHaceConUnaRestauracionPendienteASEJ() throws IOException {
        Files.writeString(HOME_PRUEBASASEJ.resolve(RestauracionPendienteASEJ.MARCAASEJ), "x");
        long antesASEJ = automaticosASEJ(HOME_PRUEBASASEJ.resolve("respaldos")).size();
        respaldoServiceASEJ.respaldoAutomaticoAlCerrarASEJ();
        assertThat(automaticosASEJ(HOME_PRUEBASASEJ.resolve("respaldos"))).hasSize((int) antesASEJ);
    }

    private static List<String> automaticosASEJ(Path carpetaASEJ) throws IOException {
        try (Stream<Path> sASEJ = Files.list(carpetaASEJ)) {
            return sASEJ.map(pASEJ -> pASEJ.getFileName().toString())
                    .filter(nASEJ -> nASEJ.startsWith(RespaldoServiceASEJ.PREFIJO_AUTOMATICOASEJ)).sorted().toList();
        }
    }

    private static void copiarASEJ(Path origenASEJ, Path destinoASEJ) throws IOException {
        try (Stream<Path> sASEJ = Files.walk(origenASEJ)) {
            for (Path pASEJ : sASEJ.toList()) {
                Path dASEJ = destinoASEJ.resolve(origenASEJ.relativize(pASEJ).toString());
                if (Files.isDirectory(pASEJ)) {
                    Files.createDirectories(dASEJ);
                } else {
                    Files.copy(pASEJ, dASEJ, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}
