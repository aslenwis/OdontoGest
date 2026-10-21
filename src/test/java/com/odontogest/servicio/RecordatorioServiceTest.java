/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio;

import com.odontogest.PruebaIntegracion;
import com.odontogest.excepcion.IntegracionException;
import com.odontogest.integracion.ConectividadService;
import com.odontogest.integracion.correo.CorreoGateway;
import com.odontogest.integracion.correo.ParametrosSmtp;
import com.odontogest.modelo.NotificacionLog;
import com.odontogest.modelo.Sexo;
import com.odontogest.modelo.TipoDocumento;
import com.odontogest.persistencia.repositorio.NotificacionLogRepository;
import com.odontogest.servicio.dto.CitaDto;
import com.odontogest.servicio.dto.CitaForm;
import com.odontogest.servicio.dto.PacienteForm;
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

/**
 * Recordatorios de citas (RF-AGE-06) e integraciones sin internet (RNF-03), con el correo y la conectividad
 * simulados: ninguna prueba sale a la red.
 */
class RecordatorioServiceTest extends PruebaIntegracion {

    @MockBean private CorreoGateway correo;
    @MockBean private ConectividadService conectividad;

    @Autowired private RecordatorioService recordatorioService;
    @Autowired private IntegracionService integracionService;
    @Autowired private ConfiguracionService configuracionService;
    @Autowired private PacienteService pacienteService;
    @Autowired private CitaService citaService;
    @Autowired private NotificacionLogRepository logRepository;
    @Autowired private JdbcTemplate jdbc;

    private static final AtomicInteger SEMANAS = new AtomicInteger();
    private LocalDate lunes;

    @BeforeEach
    void preparar() {
        reset(correo, conectividad);
        when(conectividad.hayInternet()).thenReturn(true);
        iniciarSesion("admin");
        configuracionService.guardar(Map.of("recordatorio.habilitado", "true", "email.smtp.usuario", "clinica@gmail.com"));
        // Una semana distinta por prueba y lejos de las que usan otras clases (la base de pruebas es compartida).
        lunes = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).plusWeeks(600L + SEMANAS.getAndIncrement());
    }

    @AfterEach
    void restaurar() {
        iniciarSesion("admin");
        configuracionService.guardar(Map.of("recordatorio.habilitado", "false", "email.smtp.usuario", "",
                "api.dni.habilitado", "false", "api.dni.url_plantilla", ""));
    }

    private long paciente(String email, boolean acepta) {
        return pacienteService.guardar(new PacienteForm(null, TipoDocumento.DNI, dniAleatorio(), "Rosa", "Correo" + sufijo(), null,
                LocalDate.now().minusYears(40), Sexo.F, null, email, null, null, null, null, null, null, null, acepta));
    }

    private CitaDto cita(long paciente, int hora) {
        long od = iniciarSesion("odontologo").odontologoId();
        CitaDto c = citaService.programar(new CitaForm(null, paciente, od, lunes.atTime(hora, 0), 30, "Control", null, null));
        iniciarSesion("admin");
        return c;
    }

    @Test
    void enviaUnaVezACadaPacienteConCorreoQueAcepta() {
        CitaDto conCorreo = cita(paciente("rosa@correo.pe", true), 9);
        cita(paciente(null, true), 10);
        cita(paciente("no@correo.pe", false), 11);
        LocalDateTime ahora = lunes.atTime(7, 0);

        RecordatorioService.Resultado r = recordatorioService.enviarPendientes(ahora);
        assertThat(r.enviados()).isEqualTo(1);
        assertThat(r.fallidos()).isZero();
        verify(correo, times(1)).enviar(any(ParametrosSmtp.class), eq("rosa@correo.pe"), contains("Recordatorio"),
                contains("a las 09:00"));
        assertThat(logRepository.findByCitaIdOrderByFechaAsc(conCorreo.id()))
                .singleElement().satisfies(l -> assertThat(l.getEstado()).isEqualTo(NotificacionLog.Estado.ENVIADO));
        assertThat(jdbc.queryForObject("SELECT recordatorio_enviado_en FROM cita WHERE id = ?", String.class, conCorreo.id()))
                .isNotNull();

        assertThat(recordatorioService.enviarPendientes(ahora.plusMinutes(15)).enviados()).as("no se repite").isZero();
        verify(correo, times(1)).enviar(any(), anyString(), anyString(), anyString());
    }

    @Test
    void soloDentroDeLasHorasDeAnticipacion() {
        cita(paciente("lejos@correo.pe", true), 9);
        assertThat(recordatorioService.enviarPendientes(lunes.minusDays(2).atTime(9, 0)).enviados())
                .as("faltan 48 h y el aviso es 24 h antes").isZero();
        assertThat(recordatorioService.enviarPendientes(lunes.minusDays(1).atTime(10, 0)).enviados()).isEqualTo(1);
    }

    @Test
    void unFalloSeRegistraYSeReintenta() {
        CitaDto c = cita(paciente("falla@correo.pe", true), 9);
        doThrow(new IntegracionException("no se pudo conectar con el servidor SMTP"))
                .when(correo).enviar(any(), eq("falla@correo.pe"), anyString(), anyString());
        RecordatorioService.Resultado r = recordatorioService.enviarPendientes(lunes.atTime(7, 0));
        assertThat(r.fallidos()).isEqualTo(1);
        assertThat(logRepository.findByCitaIdOrderByFechaAsc(c.id())).singleElement().satisfies(l -> {
            assertThat(l.getEstado()).isEqualTo(NotificacionLog.Estado.FALLIDO);
            assertThat(l.getDetalle()).contains("SMTP");
        });

        reset(correo);
        assertThat(recordatorioService.enviarPendientes(lunes.atTime(7, 15)).enviados()).as("se reintenta").isEqualTo(1);
    }

    @Test
    void noHaceNadaSiEstaDeshabilitadoSinSmtpOSinInternet() {
        cita(paciente("x@correo.pe", true), 9);
        LocalDateTime ahora = lunes.atTime(7, 0);
        when(conectividad.hayInternet()).thenReturn(false);
        assertThat(recordatorioService.enviarPendientes(ahora).motivoOmitido()).isEqualTo("sin internet");

        configuracionService.guardar(Map.of("email.smtp.usuario", ""));
        assertThat(recordatorioService.enviarPendientes(ahora).motivoOmitido()).isEqualTo("SMTP sin configurar");

        configuracionService.guardar(Map.of("recordatorio.habilitado", "false"));
        assertThat(recordatorioService.enviarPendientes(ahora).motivoOmitido()).isEqualTo("recordatorios deshabilitados");
        verify(correo, never()).enviar(any(), anyString(), anyString(), anyString());
    }

    @Test
    void sinInternetLasIntegracionesAvisanSinRomperNada() {
        when(conectividad.hayInternet()).thenReturn(false);
        configuracionService.guardar(Map.of("api.dni.habilitado", "true", "api.dni.url_plantilla", "https://api.pe/dni/{numero}"));
        assertThat(integracionService.dniHabilitado()).isTrue();
        assertThatThrownBy(() -> integracionService.consultarDni("12345678"))
                .isInstanceOf(IntegracionException.class).hasMessageContaining("Sin conexión a internet");
        assertThatThrownBy(() -> integracionService.enviarCorreoPrueba("admin@correo.pe"))
                .isInstanceOf(IntegracionException.class).hasMessageContaining("Sin conexión a internet");
        assertThatThrownBy(() -> integracionService.consultarDni("123")).hasMessageContaining("8 dígitos");

        when(conectividad.hayInternet()).thenReturn(true);
        integracionService.enviarCorreoPrueba("admin@correo.pe");
        verify(correo).enviar(any(ParametrosSmtp.class), eq("admin@correo.pe"), contains("Correo de prueba"), anyString());

        configuracionService.guardar(Map.of("api.dni.habilitado", "false"));
        assertThatThrownBy(() -> integracionService.consultarDni("12345678")).hasMessageContaining("no está habilitada");
    }
}
