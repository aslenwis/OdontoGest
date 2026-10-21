/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.odontogest.PruebaIntegracion;
import com.odontogest.config.RestauracionPendiente;
import com.odontogest.excepcion.NegocioException;
import com.odontogest.excepcion.PermisoDenegadoException;
import com.odontogest.integracion.seguridad.CifradorSecretos;
import com.odontogest.modelo.Sexo;
import com.odontogest.modelo.TipoDocumento;
import com.odontogest.servicio.dto.PacienteForm;
import com.odontogest.servicio.dto.RespaldoDto;
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

/**
 * Respaldo ZIP y restauración programada (PLAN §5.4) sobre la carpeta de datos de las pruebas.
 */
class RespaldoServiceTest extends PruebaIntegracion {

    @Autowired private RespaldoService respaldoService;
    @Autowired private PacienteService pacienteService;
    @Autowired private CifradorSecretos cifrador;

    @TempDir
    Path temporal;

    @AfterEach
    void sinRestauracionPendiente() throws IOException {
        Files.deleteIfExists(HOME_PRUEBAS.resolve(RestauracionPendiente.MARCA));
        borrar(HOME_PRUEBAS.resolve(RestauracionPendiente.CARPETA));
    }

    private static void borrar(Path carpeta) throws IOException {
        if (Files.exists(carpeta)) {
            try (Stream<Path> s = Files.walk(carpeta)) {
                for (Path p : s.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(p);
                }
            }
        }
    }

    private static long contar(Path base, String sql) throws SQLException {
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + base.toString().replace('\\', '/'));
             ResultSet rs = c.createStatement().executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private Path zipManual(String... entradas) throws IOException {
        Path zip = temporal.resolve("manual-" + System.nanoTime() + ".zip");
        try (OutputStream out = Files.newOutputStream(zip); ZipOutputStream z = new ZipOutputStream(out)) {
            for (int i = 0; i < entradas.length; i += 2) {
                z.putNextEntry(new ZipEntry(entradas[i]));
                z.write(entradas[i + 1].getBytes(StandardCharsets.UTF_8));
                z.closeEntry();
            }
        }
        return zip;
    }

    private static final String MANIFIESTO = "{\"aplicacion\":\"OdontoGest\",\"esquema\":1}";

    @Test
    void crearAhora_zipConManifiestoBaseYAdjuntos() throws Exception {
        Files.createDirectories(HOME_PRUEBAS.resolve("adjuntos/99"));
        Files.writeString(HOME_PRUEBAS.resolve("adjuntos/99/nota.txt"), "adjunto de prueba");
        cifrador.cifrar("crea clave.key si aún no existe");
        iniciarSesion("admin");

        RespaldoDto r = respaldoService.crearAhora();

        assertThat(r.nombre()).startsWith(RespaldoService.PREFIJO_MANUAL).endsWith(".zip");
        assertThat(r.automatico()).isFalse();
        assertThat(r.tamanioBytes()).isPositive();
        assertThat(respaldoService.listar()).extracting(RespaldoDto::nombre).contains(r.nombre());
        Path zip = Path.of(r.ruta());
        assertThat(zip.getParent()).isEqualTo(HOME_PRUEBAS.resolve("respaldos"));
        try (ZipFile z = new ZipFile(zip.toFile())) {
            assertThat(z.getEntry("adjuntos/99/nota.txt")).isNotNull();
            assertThat(z.getEntry("clave.key")).as("sin la clave no se descifran los secretos").isNotNull();
            try (InputStream in = z.getInputStream(z.getEntry("manifest.json"))) {
                JsonObject m = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
                assertThat(m.get("aplicacion").getAsString()).isEqualTo("OdontoGest");
                assertThat(m.get("esquema").getAsInt()).isEqualTo(RespaldoService.VERSION_ESQUEMA);
                assertThat(m.get("fecha").getAsString()).startsWith(LocalDate.now().toString());
            }
            Path base = temporal.resolve("extraida.db");
            try (InputStream in = z.getInputStream(z.getEntry("odontogest.db"))) {
                Files.copy(in, base);
            }
            assertThat(contar(base, "SELECT COUNT(*) FROM usuario")).isGreaterThanOrEqualTo(3);
        }
        try (Stream<Path> s = Files.list(zip.getParent())) {
            assertThat(s.map(p -> p.getFileName().toString())).noneMatch(n -> n.endsWith(".parcial"));
        }
    }

    @Test
    void soloElAdministradorRespalda() {
        iniciarSesion("recepcion");
        assertThatThrownBy(() -> respaldoService.crearAhora()).isInstanceOf(PermisoDenegadoException.class);
        assertThatThrownBy(() -> respaldoService.listar()).isInstanceOf(PermisoDenegadoException.class);
        assertThatThrownBy(() -> respaldoService.programarRestauracion(temporal.resolve("x.zip")))
                .isInstanceOf(PermisoDenegadoException.class);
    }

    /**
     * Ciclo completo: se respalda, se registra un paciente después, se programa la restauración y se aplica como al
     * reiniciar (sobre una copia de la carpeta de datos): el paciente posterior al respaldo ya no está.
     */
    @Test
    void restauracionProgramada_seAplicaAlReiniciar() throws Exception {
        iniciarSesion("admin");
        RespaldoDto r = respaldoService.crearAhora();
        iniciarSesion("recepcion");
        long posterior = pacienteService.guardar(new PacienteForm(null, TipoDocumento.DNI, dniAleatorio(), "Paciente",
                "Posterior" + sufijo(), null, LocalDate.now().minusYears(30), Sexo.F, null, null, null, null, null, null,
                null, null, null, true));

        iniciarSesion("admin");
        respaldoService.programarRestauracion(Path.of(r.ruta()));
        assertThat(respaldoService.hayRestauracionPendiente()).isTrue();
        Path carpeta = HOME_PRUEBAS.resolve(RestauracionPendiente.CARPETA);
        assertThat(carpeta.resolve("odontogest.db")).exists();
        assertThat(carpeta.resolve("manifest.json")).doesNotExist();

        // "Reinicio" sobre otra carpeta de datos: la base en uso por las pruebas no se puede reemplazar.
        Path otroHome = Files.createDirectories(temporal.resolve("home"));
        Files.writeString(otroHome.resolve("odontogest.db"), "base que se reemplaza");
        copiar(carpeta, otroHome.resolve(RestauracionPendiente.CARPETA));
        Files.copy(HOME_PRUEBAS.resolve(RestauracionPendiente.MARCA), otroHome.resolve(RestauracionPendiente.MARCA));
        assertThat(RestauracionPendiente.aplicarSiExiste(otroHome)).isTrue();

        Path restaurada = otroHome.resolve("odontogest.db");
        assertThat(contar(restaurada, "SELECT COUNT(*) FROM paciente WHERE id = " + posterior)).isZero();
        assertThat(contar(restaurada, "SELECT COUNT(*) FROM usuario WHERE username = 'admin'")).isEqualTo(1);
        assertThat(otroHome.resolve("clave.key")).exists();

        respaldoService.cancelarRestauracion();
        assertThat(respaldoService.hayRestauracionPendiente()).isFalse();
        assertThat(carpeta).doesNotExist();
    }

    @Test
    void archivosQueNoSonRespaldos_seRechazan() throws IOException {
        iniciarSesion("admin");
        Path texto = Files.writeString(temporal.resolve("no-es-zip.zip"), "hola");
        assertThatThrownBy(() -> respaldoService.programarRestauracion(texto))
                .isInstanceOf(NegocioException.class).hasMessageContaining("ZIP");
        assertThatThrownBy(() -> respaldoService.programarRestauracion(zipManual("odontogest.db", "x")))
                .isInstanceOf(NegocioException.class).hasMessageContaining("manifest.json");
        assertThatThrownBy(() -> respaldoService.programarRestauracion(zipManual(
                "manifest.json", "{\"aplicacion\":\"Otra\",\"esquema\":1}", "odontogest.db", "x")))
                .isInstanceOf(NegocioException.class).hasMessageContaining("no es un respaldo de OdontoGest");
        assertThatThrownBy(() -> respaldoService.programarRestauracion(zipManual(
                "manifest.json", "{\"aplicacion\":\"OdontoGest\",\"esquema\":99}", "odontogest.db", "x")))
                .isInstanceOf(NegocioException.class).hasMessageContaining("más reciente");
        assertThatThrownBy(() -> respaldoService.programarRestauracion(zipManual(
                "manifest.json", MANIFIESTO, "odontogest.db", "no soy sqlite")))
                .isInstanceOf(NegocioException.class).hasMessageContaining("SQLite");
        assertThatThrownBy(() -> respaldoService.programarRestauracion(zipManual(
                "manifest.json", MANIFIESTO, "odontogest.db", "x", "../fuera.txt", "x")))
                .isInstanceOf(NegocioException.class).hasMessageContaining("ruta no permitida");
        assertThat(HOME_PRUEBAS.resolveSibling("fuera.txt")).doesNotExist();
        assertThat(respaldoService.hayRestauracionPendiente()).isFalse();
        assertThat(HOME_PRUEBAS.resolve(RestauracionPendiente.CARPETA)).doesNotExist();
    }

    @Test
    void baseSinLasTablasDeOdontoGest_seRechaza() throws Exception {
        iniciarSesion("admin");
        Path ajena = temporal.resolve("ajena.db");
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + ajena.toString().replace('\\', '/'))) {
            c.createStatement().execute("CREATE TABLE cliente (id INTEGER PRIMARY KEY)");
        }
        Path zip = temporal.resolve("ajena.zip");
        try (OutputStream out = Files.newOutputStream(zip); ZipOutputStream z = new ZipOutputStream(out)) {
            z.putNextEntry(new ZipEntry("manifest.json"));
            z.write(MANIFIESTO.getBytes(StandardCharsets.UTF_8));
            z.closeEntry();
            z.putNextEntry(new ZipEntry("odontogest.db"));
            Files.copy(ajena, z);
            z.closeEntry();
        }
        assertThatThrownBy(() -> respaldoService.programarRestauracion(zip))
                .isInstanceOf(NegocioException.class).hasMessageContaining("configuracion");
    }

    @Test
    void automaticoAlCerrar_conservaSoloLosUltimos() throws IOException {
        Path carpeta = HOME_PRUEBAS.resolve("respaldos");
        Instant base = Instant.now().minus(30, ChronoUnit.DAYS);
        for (int i = 1; i <= 4; i++) {
            Path viejo = Files.writeString(carpeta.resolve(RespaldoService.PREFIJO_AUTOMATICO + "2000010" + i + "-000000.zip"), "x");
            Files.setLastModifiedTime(viejo, FileTime.from(base.plus(i, ChronoUnit.HOURS)));
        }
        Path manualAntiguo = Files.writeString(carpeta.resolve(RespaldoService.PREFIJO_MANUAL + "20000101-000000.zip"), "x");
        Files.setLastModifiedTime(manualAntiguo, FileTime.from(base));
        respaldoService.respaldoAutomaticoAlCerrar();
        assertThat(automaticos(carpeta)).as("los 4 de prueba + el nuevo, bajo el límite de 10").hasSize(5);

        respaldoService.podarAutomaticos(2);
        List<String> quedan = automaticos(carpeta);
        assertThat(quedan).hasSize(2);
        assertThat(quedan).anyMatch(n -> n.endsWith("20000104-000000.zip"));
        assertThat(quedan).noneMatch(n -> n.endsWith("20000101-000000.zip"));
        assertThat(manualAntiguo).as("los respaldos manuales no se podan").exists();
        Files.delete(manualAntiguo);
    }

    @Test
    void automaticoAlCerrar_noSeHaceConUnaRestauracionPendiente() throws IOException {
        Files.writeString(HOME_PRUEBAS.resolve(RestauracionPendiente.MARCA), "x");
        long antes = automaticos(HOME_PRUEBAS.resolve("respaldos")).size();
        respaldoService.respaldoAutomaticoAlCerrar();
        assertThat(automaticos(HOME_PRUEBAS.resolve("respaldos"))).hasSize((int) antes);
    }

    private static List<String> automaticos(Path carpeta) throws IOException {
        try (Stream<Path> s = Files.list(carpeta)) {
            return s.map(p -> p.getFileName().toString())
                    .filter(n -> n.startsWith(RespaldoService.PREFIJO_AUTOMATICO)).sorted().toList();
        }
    }

    private static void copiar(Path origen, Path destino) throws IOException {
        try (Stream<Path> s = Files.walk(origen)) {
            for (Path p : s.toList()) {
                Path d = destino.resolve(origen.relativize(p).toString());
                if (Files.isDirectory(p)) {
                    Files.createDirectories(d);
                } else {
                    Files.copy(p, d, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}
