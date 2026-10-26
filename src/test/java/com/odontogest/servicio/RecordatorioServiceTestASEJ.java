package com.odontogest.servicio;

import com.odontogest.PruebaIntegracionASEJ;
import com.odontogest.excepcion.IntegracionExceptionASEJ;
import com.odontogest.integracion.ConectividadServiceASEJ;
import com.odontogest.integracion.correo.CorreoGatewayASEJ;
import com.odontogest.integracion.correo.ParametrosSmtpASEJ;
import com.odontogest.modelo.NotificacionLogASEJ;
import com.odontogest.modelo.SexoASEJ;
import com.odontogest.modelo.TipoDocumentoASEJ;
import com.odontogest.persistencia.repositorio.NotificacionLogRepositoryASEJ;
import com.odontogest.servicio.dto.CitaDtoASEJ;
import com.odontogest.servicio.dto.CitaFormASEJ;
import com.odontogest.servicio.dto.PacienteFormASEJ;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecordatorioServiceTestASEJ extends PruebaIntegracionASEJ {

    @MockBean private CorreoGatewayASEJ correoASEJ;
    @MockBean private ConectividadServiceASEJ conectividadASEJ;

    @Autowired private RecordatorioServiceASEJ recordatorioServiceASEJ;
    @Autowired private IntegracionServiceASEJ integracionServiceASEJ;
    @Autowired private ConfiguracionServiceASEJ configuracionServiceASEJ;
    @Autowired private PacienteServiceASEJ pacienteServiceASEJ;
    @Autowired private CitaServiceASEJ citaServiceASEJ;
    @Autowired private NotificacionLogRepositoryASEJ logRepositoryASEJ;
    @Autowired private JdbcTemplate jdbcASEJ;

    private static final AtomicInteger SEMANASASEJ = new AtomicInteger();
    private LocalDate lunesASEJ;

    @BeforeEach
    void prepararASEJ() {
        reset(correoASEJ, conectividadASEJ);
        when(conectividadASEJ.hayInternetASEJ()).thenReturn(true);
        iniciarSesionASEJ("admin");
        configuracionServiceASEJ.guardarASEJ(Map.of("recordatorio.habilitado", "true", "email.smtp.usuario", "clinica@gmail.com"));
        lunesASEJ = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).plusWeeks(600L + SEMANASASEJ.getAndIncrement());
    }

    @AfterEach
    void restaurarASEJ() {
        iniciarSesionASEJ("admin");
        configuracionServiceASEJ.guardarASEJ(Map.of("recordatorio.habilitado", "false", "email.smtp.usuario", "",
                "api.dni.habilitado", "false", "api.dni.url_plantilla", ""));
    }

    private long pacienteASEJ(String emailASEJ, boolean aceptaASEJ) {
        return pacienteServiceASEJ.guardarASEJ(new PacienteFormASEJ(null, TipoDocumentoASEJ.DNI, dniAleatorioASEJ(), "Rosa", "Correo" + sufijoASEJ(), null,
                LocalDate.now().minusYears(40), SexoASEJ.F, null, emailASEJ, null, null, null, null, null, null, null, aceptaASEJ));
    }

    private CitaDtoASEJ citaASEJ(long pacienteASEJ, int horaASEJ) {
        long odASEJ = iniciarSesionASEJ("odontologo").odontologoIdASEJ();
        CitaDtoASEJ cASEJ = citaServiceASEJ.programarASEJ(new CitaFormASEJ(null, pacienteASEJ, odASEJ, lunesASEJ.atTime(horaASEJ, 0), 30, "Control", null, null));
        iniciarSesionASEJ("admin");
        return cASEJ;
    }

    @Test
    void enviaUnaVezACadaPacienteConCorreoQueAceptaASEJ() {
        CitaDtoASEJ conCorreoASEJ = citaASEJ(pacienteASEJ("rosa@correo.pe", true), 9);
        citaASEJ(pacienteASEJ(null, true), 10);
        citaASEJ(pacienteASEJ("no@correo.pe", false), 11);
        LocalDateTime ahoraASEJ = lunesASEJ.atTime(7, 0);

        RecordatorioServiceASEJ.ResultadoASEJ rASEJ = recordatorioServiceASEJ.enviarPendientesASEJ(ahoraASEJ);
        assertThat(rASEJ.enviadosASEJ()).isEqualTo(1);
        assertThat(rASEJ.fallidosASEJ()).isZero();
        verify(correoASEJ, times(1)).enviarASEJ(any(ParametrosSmtpASEJ.class), eq("rosa@correo.pe"), contains("Recordatorio"),
                contains("a las 09:00"));
        assertThat(logRepositoryASEJ.findByCitaIdASEJOrderByFechaASEJAsc(conCorreoASEJ.idASEJ()))
                .singleElement().satisfies(lASEJ -> assertThat(lASEJ.getEstadoASEJ()).isEqualTo(NotificacionLogASEJ.EstadoASEJ.ENVIADO));
        assertThat(jdbcASEJ.queryForObject("SELECT recordatorio_enviado_en FROM cita WHERE id = ?", String.class, conCorreoASEJ.idASEJ()))
                .isNotNull();

        assertThat(recordatorioServiceASEJ.enviarPendientesASEJ(ahoraASEJ.plusMinutes(15)).enviadosASEJ()).as("no se repite").isZero();
        verify(correoASEJ, times(1)).enviarASEJ(any(), anyString(), anyString(), anyString());
    }

    @Test
    void soloDentroDeLasHorasDeAnticipacionASEJ() {
        citaASEJ(pacienteASEJ("lejos@correo.pe", true), 9);
        assertThat(recordatorioServiceASEJ.enviarPendientesASEJ(lunesASEJ.minusDays(2).atTime(9, 0)).enviadosASEJ())
                .as("faltan 48 h y el aviso es 24 h antes").isZero();
        assertThat(recordatorioServiceASEJ.enviarPendientesASEJ(lunesASEJ.minusDays(1).atTime(10, 0)).enviadosASEJ()).isEqualTo(1);
    }

    @Test
    void unFalloSeRegistraYSeReintentaASEJ() {
        CitaDtoASEJ cASEJ = citaASEJ(pacienteASEJ("falla@correo.pe", true), 9);
        doThrow(new IntegracionExceptionASEJ("no se pudo conectar con el servidor SMTP"))
                .when(correoASEJ).enviarASEJ(any(), eq("falla@correo.pe"), anyString(), anyString());
        RecordatorioServiceASEJ.ResultadoASEJ rASEJ = recordatorioServiceASEJ.enviarPendientesASEJ(lunesASEJ.atTime(7, 0));
        assertThat(rASEJ.fallidosASEJ()).isEqualTo(1);
        assertThat(logRepositoryASEJ.findByCitaIdASEJOrderByFechaASEJAsc(cASEJ.idASEJ())).singleElement().satisfies(lASEJ -> {
            assertThat(lASEJ.getEstadoASEJ()).isEqualTo(NotificacionLogASEJ.EstadoASEJ.FALLIDO);
            assertThat(lASEJ.getDetalleASEJ()).contains("SMTP");
        });

        reset(correoASEJ);
        assertThat(recordatorioServiceASEJ.enviarPendientesASEJ(lunesASEJ.atTime(7, 15)).enviadosASEJ()).as("se reintenta").isEqualTo(1);
    }

    @Test
    void noHaceNadaSiEstaDeshabilitadoSinSmtpOSinInternetASEJ() {
        citaASEJ(pacienteASEJ("x@correo.pe", true), 9);
        LocalDateTime ahoraASEJ = lunesASEJ.atTime(7, 0);
        when(conectividadASEJ.hayInternetASEJ()).thenReturn(false);
        assertThat(recordatorioServiceASEJ.enviarPendientesASEJ(ahoraASEJ).motivoOmitidoASEJ()).isEqualTo("sin internet");

        configuracionServiceASEJ.guardarASEJ(Map.of("email.smtp.usuario", ""));
        assertThat(recordatorioServiceASEJ.enviarPendientesASEJ(ahoraASEJ).motivoOmitidoASEJ()).isEqualTo("SMTP sin configurar");

        configuracionServiceASEJ.guardarASEJ(Map.of("recordatorio.habilitado", "false"));
        assertThat(recordatorioServiceASEJ.enviarPendientesASEJ(ahoraASEJ).motivoOmitidoASEJ()).isEqualTo("recordatorios deshabilitados");
        verify(correoASEJ, never()).enviarASEJ(any(), anyString(), anyString(), anyString());
    }

    @Test
    void sinInternetLasIntegracionesAvisanSinRomperNadaASEJ() {
        when(conectividadASEJ.hayInternetASEJ()).thenReturn(false);
        configuracionServiceASEJ.guardarASEJ(Map.of("api.dni.habilitado", "true", "api.dni.url_plantilla", "https://api.pe/dni/{numero}"));
        assertThat(integracionServiceASEJ.dniHabilitadoASEJ()).isTrue();
        assertThatThrownBy(() -> integracionServiceASEJ.consultarDniASEJ("12345678"))
                .isInstanceOf(IntegracionExceptionASEJ.class).hasMessageContaining("Sin conexión a internet");
        assertThatThrownBy(() -> integracionServiceASEJ.enviarCorreoPruebaASEJ("admin@correo.pe"))
                .isInstanceOf(IntegracionExceptionASEJ.class).hasMessageContaining("Sin conexión a internet");
        assertThatThrownBy(() -> integracionServiceASEJ.consultarDniASEJ("123")).hasMessageContaining("8 dígitos");

        when(conectividadASEJ.hayInternetASEJ()).thenReturn(true);
        integracionServiceASEJ.enviarCorreoPruebaASEJ("admin@correo.pe");
        verify(correoASEJ).enviarASEJ(any(ParametrosSmtpASEJ.class), eq("admin@correo.pe"), contains("Correo de prueba"), anyString());

        configuracionServiceASEJ.guardarASEJ(Map.of("api.dni.habilitado", "false"));
        assertThatThrownBy(() -> integracionServiceASEJ.consultarDniASEJ("12345678")).hasMessageContaining("no está habilitada");
    }
}
