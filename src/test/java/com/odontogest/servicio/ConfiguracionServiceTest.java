/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio;

import com.odontogest.PruebaIntegracion;
import com.odontogest.excepcion.NegocioException;
import com.odontogest.excepcion.PermisoDenegadoException;
import com.odontogest.integracion.seguridad.CifradorSecretos;
import com.odontogest.servicio.dto.ConfiguracionDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Lectura tipada, guardado validado y secretos cifrados de la configuración (Fase 7: RF-CFG-03).
 */
class ConfiguracionServiceTest extends PruebaIntegracion {

    @Autowired private ConfiguracionService configuracionService;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void sesion() {
        iniciarSesion("admin");
    }

    private String guardado(String clave) {
        return jdbc.queryForObject("SELECT valor FROM configuracion WHERE clave = ?", String.class, clave);
    }

    @Test
    void leeValoresTipados() {
        assertThat(configuracionService.texto("clinica.nombre")).isEqualTo("Consultorio Odontológico Demo");
        assertThat(configuracionService.entero("agenda.intervalo_min")).isEqualTo(15);
        assertThat(configuracionService.booleano("email.smtp.starttls")).isTrue();
        assertThat(configuracionService.json("api.dni.mapeo_json").get("nombres").getAsString()).isEqualTo("nombres");
    }

    @Test
    void guardarInvalidaLaCache() {
        String original = configuracionService.texto("clinica.telefono");
        try {
            configuracionService.guardar(Map.of("clinica.telefono", "  999 888 777 "));
            assertThat(configuracionService.texto("clinica.telefono")).isEqualTo("999 888 777");
        } finally {
            configuracionService.guardar(Map.of("clinica.telefono", original));
        }
    }

    @Test
    void rechazaClavesInexistentesONoEditables() {
        assertThatThrownBy(() -> configuracionService.texto("no.existe")).isInstanceOf(NegocioException.class);
        assertThatThrownBy(() -> configuracionService.guardar(Map.of("esquema.version", "2")))
                .isInstanceOf(NegocioException.class).hasMessageContaining("no es editable");
        assertThat(configuracionService.entero("esquema.version")).isEqualTo(1);
        assertThat(configuracionService.listar()).noneMatch(c -> c.clave().equals("esquema.version"));
    }

    @Test
    void losSecretosSeGuardanCifradosYNoSalenALaInterfaz() {
        try {
            configuracionService.guardar(Map.of("api.dni.token", "token-super-secreto-123"));
            assertThat(guardado("api.dni.token")).startsWith(CifradorSecretos.PREFIJO).doesNotContain("secreto");
            assertThat(configuracionService.secreto("api.dni.token")).contains("token-super-secreto-123");
            ConfiguracionDto dto = configuracionService.listar().stream().filter(c -> c.clave().equals("api.dni.token"))
                    .findFirst().orElseThrow();
            assertThat(dto.valor()).isNull();
            assertThat(dto.secretoConfigurado()).isTrue();

            String antes = guardado("api.dni.token");
            configuracionService.guardar(Map.of("api.dni.token", "token-super-secreto-123"));
            assertThat(guardado("api.dni.token")).as("IV aleatorio: el mismo valor se cifra distinto").isNotEqualTo(antes);
        } finally {
            configuracionService.guardar(Map.of("api.dni.token", ""));
        }
        assertThat(configuracionService.secreto("api.dni.token")).as("vacío borra el secreto").isEmpty();
    }

    @Test
    void validaSegunTipoYRango() {
        assertThatThrownBy(() -> configuracionService.guardar(Map.of("email.smtp.puerto", "abc"))).hasMessageContaining("número entero");
        assertThatThrownBy(() -> configuracionService.guardar(Map.of("email.smtp.puerto", "70000"))).hasMessageContaining("entre 1 y 65535");
        assertThatThrownBy(() -> configuracionService.guardar(Map.of("agenda.intervalo_min", "7"))).hasMessageContaining("5, 10, 15, 20 o 30");
        assertThatThrownBy(() -> configuracionService.guardar(Map.of("agenda.duracion_cita_min", "25"))).hasMessageContaining("múltiplo");
        assertThatThrownBy(() -> configuracionService.guardar(Map.of("agenda.intervalo_min", "10", "agenda.duracion_cita_min", "25")))
                .as("la duración se valida contra el intervalo nuevo del mismo lote").hasMessageContaining("múltiplo");
        assertThatThrownBy(() -> configuracionService.guardar(Map.of("recordatorio.habilitado", "si"))).hasMessageContaining("verdadero o falso");
        assertThatThrownBy(() -> configuracionService.guardar(Map.of("api.dni.mapeo_json", "[1,2]"))).hasMessageContaining("objeto JSON");
        assertThatThrownBy(() -> configuracionService.guardar(Map.of("api.dni.url_plantilla", "https://api.pe/dni")))
                .hasMessageContaining("{numero}");
        assertThatThrownBy(() -> configuracionService.guardar(Map.of("email.remitente", "no-es-correo"))).hasMessageContaining("correo válido");
        assertThatThrownBy(() -> configuracionService.guardar(Map.of("clinica.nombre", " "))).hasMessageContaining("nombre de la clínica");
        assertThatThrownBy(() -> configuracionService.guardar(Map.of("clinica.telefono", "1", "email.smtp.puerto", "0")))
                .as("todo o nada").hasMessageContaining("entre 1 y 65535");
        assertThat(configuracionService.entero("email.smtp.puerto")).isEqualTo(587);
    }

    @Test
    void elTemaLoCambiaCualquieraElRestoSoloElAdministrador() {
        iniciarSesion("recepcion");
        configuracionService.guardar(Map.of("ui.tema", "OSCURO"));
        assertThat(configuracionService.texto("ui.tema")).isEqualTo("OSCURO");
        configuracionService.guardar(Map.of("ui.tema", "CLARO"));
        assertThatThrownBy(() -> configuracionService.guardar(Map.of("clinica.telefono", "123")))
                .isInstanceOf(PermisoDenegadoException.class);
        assertThatThrownBy(() -> configuracionService.guardar(Map.of("ui.tema", "AZUL"))).hasMessageContaining("CLARO u OSCURO");
    }
}
