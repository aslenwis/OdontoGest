package com.odontogest.servicio;

import com.odontogest.PruebaIntegracionASEJ;
import com.odontogest.excepcion.NegocioExceptionASEJ;
import com.odontogest.excepcion.PermisoDenegadoExceptionASEJ;
import com.odontogest.integracion.seguridad.CifradorSecretosASEJ;
import com.odontogest.servicio.dto.ConfiguracionDtoASEJ;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfiguracionServiceTestASEJ extends PruebaIntegracionASEJ {

    @Autowired private ConfiguracionServiceASEJ configuracionServiceASEJ;
    @Autowired private JdbcTemplate jdbcASEJ;

    @BeforeEach
    void sesionASEJ() {
        iniciarSesionASEJ("admin");
    }

    private String guardadoASEJ(String claveASEJ) {
        return jdbcASEJ.queryForObject("SELECT valor FROM configuracion WHERE clave = ?", String.class, claveASEJ);
    }

    @Test
    void leeValoresTipadosASEJ() {
        assertThat(configuracionServiceASEJ.textoASEJ("clinica.nombre")).isEqualTo("Consultorio Odontológico Demo");
        assertThat(configuracionServiceASEJ.enteroASEJ("agenda.intervalo_min")).isEqualTo(15);
        assertThat(configuracionServiceASEJ.booleanoASEJ("email.smtp.starttls")).isTrue();
        assertThat(configuracionServiceASEJ.jsonASEJ("api.dni.mapeo_json").get("nombres").getAsString()).isEqualTo("nombres");
    }

    @Test
    void guardarInvalidaLaCacheASEJ() {
        String originalASEJ = configuracionServiceASEJ.textoASEJ("clinica.telefono");
        try {
            configuracionServiceASEJ.guardarASEJ(Map.of("clinica.telefono", "  999 888 777 "));
            assertThat(configuracionServiceASEJ.textoASEJ("clinica.telefono")).isEqualTo("999 888 777");
        } finally {
            configuracionServiceASEJ.guardarASEJ(Map.of("clinica.telefono", originalASEJ));
        }
    }

    @Test
    void rechazaClavesInexistentesONoEditablesASEJ() {
        assertThatThrownBy(() -> configuracionServiceASEJ.textoASEJ("no.existe")).isInstanceOf(NegocioExceptionASEJ.class);
        assertThatThrownBy(() -> configuracionServiceASEJ.guardarASEJ(Map.of("esquema.version", "2")))
                .isInstanceOf(NegocioExceptionASEJ.class).hasMessageContaining("no es editable");
        assertThat(configuracionServiceASEJ.enteroASEJ("esquema.version")).isEqualTo(1);
        assertThat(configuracionServiceASEJ.listarASEJ()).noneMatch(cASEJ -> cASEJ.claveASEJ().equals("esquema.version"));
    }

    @Test
    void losSecretosSeGuardanCifradosYNoSalenALaInterfazASEJ() {
        try {
            configuracionServiceASEJ.guardarASEJ(Map.of("api.dni.token", "token-super-secreto-123"));
            assertThat(guardadoASEJ("api.dni.token")).startsWith(CifradorSecretosASEJ.PREFIJOASEJ).doesNotContain("secreto");
            assertThat(configuracionServiceASEJ.secretoASEJ("api.dni.token")).contains("token-super-secreto-123");
            ConfiguracionDtoASEJ dtoASEJ = configuracionServiceASEJ.listarASEJ().stream().filter(cASEJ -> cASEJ.claveASEJ().equals("api.dni.token"))
                    .findFirst().orElseThrow();
            assertThat(dtoASEJ.valorASEJ()).isNull();
            assertThat(dtoASEJ.secretoConfiguradoASEJ()).isTrue();

            String antesASEJ = guardadoASEJ("api.dni.token");
            configuracionServiceASEJ.guardarASEJ(Map.of("api.dni.token", "token-super-secreto-123"));
            assertThat(guardadoASEJ("api.dni.token")).as("IV aleatorio: el mismo valor se cifra distinto").isNotEqualTo(antesASEJ);
        } finally {
            configuracionServiceASEJ.guardarASEJ(Map.of("api.dni.token", ""));
        }
        assertThat(configuracionServiceASEJ.secretoASEJ("api.dni.token")).as("vacío borra el secreto").isEmpty();
    }

    @Test
    void validaSegunTipoYRangoASEJ() {
        assertThatThrownBy(() -> configuracionServiceASEJ.guardarASEJ(Map.of("email.smtp.puerto", "abc"))).hasMessageContaining("número entero");
        assertThatThrownBy(() -> configuracionServiceASEJ.guardarASEJ(Map.of("email.smtp.puerto", "70000"))).hasMessageContaining("entre 1 y 65535");
        assertThatThrownBy(() -> configuracionServiceASEJ.guardarASEJ(Map.of("agenda.intervalo_min", "7"))).hasMessageContaining("5, 10, 15, 20 o 30");
        assertThatThrownBy(() -> configuracionServiceASEJ.guardarASEJ(Map.of("agenda.duracion_cita_min", "25"))).hasMessageContaining("múltiplo");
        assertThatThrownBy(() -> configuracionServiceASEJ.guardarASEJ(Map.of("agenda.intervalo_min", "10", "agenda.duracion_cita_min", "25")))
                .as("la duración se valida contra el intervalo nuevo del mismo lote").hasMessageContaining("múltiplo");
        assertThatThrownBy(() -> configuracionServiceASEJ.guardarASEJ(Map.of("recordatorio.habilitado", "si"))).hasMessageContaining("verdadero o falso");
        assertThatThrownBy(() -> configuracionServiceASEJ.guardarASEJ(Map.of("api.dni.mapeo_json", "[1,2]"))).hasMessageContaining("objeto JSON");
        assertThatThrownBy(() -> configuracionServiceASEJ.guardarASEJ(Map.of("api.dni.url_plantilla", "https://api.pe/dni")))
                .hasMessageContaining("{numero}");
        assertThatThrownBy(() -> configuracionServiceASEJ.guardarASEJ(Map.of("email.remitente", "no-es-correo"))).hasMessageContaining("correo válido");
        assertThatThrownBy(() -> configuracionServiceASEJ.guardarASEJ(Map.of("clinica.nombre", " "))).hasMessageContaining("nombre de la clínica");
        assertThatThrownBy(() -> configuracionServiceASEJ.guardarASEJ(Map.of("clinica.telefono", "1", "email.smtp.puerto", "0")))
                .as("todo o nada").hasMessageContaining("entre 1 y 65535");
        assertThat(configuracionServiceASEJ.enteroASEJ("email.smtp.puerto")).isEqualTo(587);
    }

    @Test
    void elTemaLoCambiaCualquieraElRestoSoloElAdministradorASEJ() {
        iniciarSesionASEJ("recepcion");
        configuracionServiceASEJ.guardarASEJ(Map.of("ui.tema", "OSCURO"));
        assertThat(configuracionServiceASEJ.textoASEJ("ui.tema")).isEqualTo("OSCURO");
        configuracionServiceASEJ.guardarASEJ(Map.of("ui.tema", "CLARO"));
        assertThatThrownBy(() -> configuracionServiceASEJ.guardarASEJ(Map.of("clinica.telefono", "123")))
                .isInstanceOf(PermisoDenegadoExceptionASEJ.class);
        assertThatThrownBy(() -> configuracionServiceASEJ.guardarASEJ(Map.of("ui.tema", "AZUL"))).hasMessageContaining("CLARO u OSCURO");
    }
}
