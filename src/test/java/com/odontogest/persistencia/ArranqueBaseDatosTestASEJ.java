package com.odontogest.persistencia;

import com.odontogest.PruebaIntegracionASEJ;
import com.odontogest.modelo.RolASEJ;
import com.odontogest.modelo.UsuarioASEJ;
import com.odontogest.persistencia.repositorio.UsuarioRepositoryASEJ;
import com.odontogest.seeder.ConfiguracionSeederASEJ;
import com.odontogest.seeder.DatabaseSeederASEJ;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.file.Files;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ArranqueBaseDatosTestASEJ extends PruebaIntegracionASEJ {

    private static final List<String> TABLASASEJ = List.of(
            "configuracion", "correlativo", "odontologo", "horario_atencion", "usuario",
            "paciente", "anamnesis", "consentimiento", "adjunto", "atencion",
            "odontograma", "hallazgo_tipo", "hallazgo", "hallazgo_anulacion",
            "procedimiento", "regla_sugerencia", "plan_tratamiento", "plan_item",
            "cuota", "pago", "pago_aplicacion", "cita", "notificacion_log", "auditoria");

    @Autowired private JdbcTemplate jdbcASEJ;
    @Autowired private UsuarioRepositoryASEJ usuarioRepositoryASEJ;
    @Autowired private PasswordEncoder passwordEncoderASEJ;
    @Autowired private ConfiguracionSeederASEJ configuracionSeederASEJ;
    @Autowired private DatabaseSeederASEJ databaseSeederASEJ;

    @Test
    void creaLaBaseEnLaCarpetaDeDatosASEJ() {
        assertThat(HOME_PRUEBASASEJ.resolve("odontogest.db")).exists();
        assertThat(Files.isDirectory(HOME_PRUEBASASEJ.resolve("adjuntos"))).isTrue();
        assertThat(Files.isDirectory(HOME_PRUEBASASEJ.resolve("respaldos"))).isTrue();
        assertThat(Files.isDirectory(HOME_PRUEBASASEJ.resolve("logs"))).isTrue();
    }

    @Test
    void creaTodasLasTablasDelEsquemaASEJ() {
        List<String> tablasASEJ = jdbcASEJ.queryForList(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'", String.class);
        assertThat(tablasASEJ).containsAll(TABLASASEJ);
    }

    @Test
    void activaClavesForaneasYModoWalASEJ() {
        assertThat(jdbcASEJ.queryForObject("PRAGMA foreign_keys", Integer.class)).isEqualTo(1);
        assertThat(jdbcASEJ.queryForObject("PRAGMA journal_mode", String.class)).isEqualToIgnoringCase("wal");
    }

    @Test
    void cargaConfiguracionCorrelativosYAdminASEJ() {
        assertThat(jdbcASEJ.queryForObject("SELECT COUNT(*) FROM configuracion", Integer.class)).isEqualTo(30);
        assertThat(jdbcASEJ.queryForObject("SELECT valor FROM configuracion WHERE clave = 'api.dni.prefijo_token'", String.class))
                .isEqualTo("Bearer ");
        assertThat(jdbcASEJ.queryForObject("SELECT editable FROM configuracion WHERE clave = 'esquema.version'", Integer.class))
                .isZero();
        assertThat(jdbcASEJ.queryForList("SELECT serie FROM correlativo WHERE ultimo = 0", String.class))
                .containsExactlyInAnyOrder("HC", "RECIBO");

        UsuarioASEJ adminASEJ = usuarioRepositoryASEJ.findByUsernameASEJIgnoreCase("admin").orElseThrow();
        assertThat(adminASEJ.getRolASEJ()).isEqualTo(RolASEJ.ADMIN);
        assertThat(adminASEJ.getPasswordHashASEJ()).startsWith("$2a$10$");
        assertThat(passwordEncoderASEJ.matches("Admin123*", adminASEJ.getPasswordHashASEJ())).isTrue();
    }

    @Test
    void losSeedersSonIdempotentesASEJ() throws Exception {
        int configuracionesASEJ = jdbcASEJ.queryForObject("SELECT COUNT(*) FROM configuracion", Integer.class);
        long usuariosASEJ = usuarioRepositoryASEJ.count();

        configuracionSeederASEJ.run(new DefaultApplicationArguments());
        databaseSeederASEJ.run(new DefaultApplicationArguments());

        assertThat(jdbcASEJ.queryForObject("SELECT COUNT(*) FROM configuracion", Integer.class)).isEqualTo(configuracionesASEJ);
        assertThat(usuarioRepositoryASEJ.count()).isEqualTo(usuariosASEJ);
    }
}
