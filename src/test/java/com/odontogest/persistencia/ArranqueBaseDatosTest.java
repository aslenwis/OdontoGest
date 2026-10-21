/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.persistencia;

import com.odontogest.PruebaIntegracion;
import com.odontogest.modelo.Rol;
import com.odontogest.modelo.Usuario;
import com.odontogest.persistencia.repositorio.UsuarioRepository;
import com.odontogest.seeder.ConfiguracionSeeder;
import com.odontogest.seeder.DatabaseSeeder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.file.Files;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Primer arranque (RNF-02, RNF-06): BD creada en la carpeta de datos, esquema completo,
 * PRAGMAs activos y seeders idempotentes.
 */
class ArranqueBaseDatosTest extends PruebaIntegracion {

    private static final List<String> TABLAS = List.of(
            "configuracion", "correlativo", "odontologo", "horario_atencion", "usuario",
            "paciente", "anamnesis", "consentimiento", "adjunto", "atencion",
            "odontograma", "hallazgo_tipo", "hallazgo", "hallazgo_anulacion",
            "procedimiento", "regla_sugerencia", "plan_tratamiento", "plan_item",
            "cuota", "pago", "pago_aplicacion", "cita", "notificacion_log", "auditoria");

    @Autowired private JdbcTemplate jdbc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ConfiguracionSeeder configuracionSeeder;
    @Autowired private DatabaseSeeder databaseSeeder;

    @Test
    void creaLaBaseEnLaCarpetaDeDatos() {
        assertThat(HOME_PRUEBAS.resolve("odontogest.db")).exists();
        assertThat(Files.isDirectory(HOME_PRUEBAS.resolve("adjuntos"))).isTrue();
        assertThat(Files.isDirectory(HOME_PRUEBAS.resolve("respaldos"))).isTrue();
        assertThat(Files.isDirectory(HOME_PRUEBAS.resolve("logs"))).isTrue();
    }

    @Test
    void creaTodasLasTablasDelEsquema() {
        List<String> tablas = jdbc.queryForList(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'", String.class);
        assertThat(tablas).containsAll(TABLAS);
    }

    @Test
    void activaClavesForaneasYModoWal() {
        assertThat(jdbc.queryForObject("PRAGMA foreign_keys", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("PRAGMA journal_mode", String.class)).isEqualToIgnoringCase("wal");
    }

    @Test
    void cargaConfiguracionCorrelativosYAdmin() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM configuracion", Integer.class)).isEqualTo(30);   // 29 + seeder.ultimo_hc_demo (Fase 9)
        assertThat(jdbc.queryForObject("SELECT valor FROM configuracion WHERE clave = 'api.dni.prefijo_token'", String.class))
                .isEqualTo("Bearer ");
        assertThat(jdbc.queryForObject("SELECT editable FROM configuracion WHERE clave = 'esquema.version'", Integer.class))
                .isZero();
        assertThat(jdbc.queryForList("SELECT serie FROM correlativo WHERE ultimo = 0", String.class))
                .containsExactlyInAnyOrder("HC", "RECIBO");

        Usuario admin = usuarioRepository.findByUsernameIgnoreCase("admin").orElseThrow();
        assertThat(admin.getRol()).isEqualTo(Rol.ADMIN);
        assertThat(admin.getPasswordHash()).startsWith("$2a$10$");
        assertThat(passwordEncoder.matches("Admin123*", admin.getPasswordHash())).isTrue();
    }

    @Test
    void losSeedersSonIdempotentes() throws Exception {
        int configuraciones = jdbc.queryForObject("SELECT COUNT(*) FROM configuracion", Integer.class);
        long usuarios = usuarioRepository.count();

        configuracionSeeder.run(new DefaultApplicationArguments());
        databaseSeeder.run(new DefaultApplicationArguments());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM configuracion", Integer.class)).isEqualTo(configuraciones);
        assertThat(usuarioRepository.count()).isEqualTo(usuarios);
    }
}
