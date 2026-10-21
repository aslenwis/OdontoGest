/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio;

import com.odontogest.PruebaIntegracion;
import com.odontogest.excepcion.NegocioException;
import com.odontogest.excepcion.PermisoDenegadoException;
import com.odontogest.modelo.EstadoCita;
import com.odontogest.modelo.Sexo;
import com.odontogest.modelo.TipoDocumento;
import com.odontogest.servicio.dto.AgendaDto;
import com.odontogest.servicio.dto.CitaDto;
import com.odontogest.servicio.dto.CitaForm;
import com.odontogest.servicio.dto.OdontologoDto;
import com.odontogest.servicio.dto.PacienteForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Agenda (Fase 6): sin solapamientos del odontólogo ni del paciente, dentro del horario, mover con validación,
 * siguiente hueco, estados y flujo cita → atención. Horario demo: L–V 09:00–13:00 y 15:00–19:00, S 09:00–13:00.
 */
class CitaServiceTest extends PruebaIntegracion {

    @Autowired private PacienteService pacienteService;
    @Autowired private OdontologoService odontologoService;
    @Autowired private CitaService citaService;
    @Autowired private AtencionService atencionService;
    @Autowired private OdontogramaService odontogramaService;
    @Autowired private JdbcTemplate jdbc;

    private long od1;
    private long od2;
    private long paciente1;
    private long paciente2;
    private LocalDate lunes;
    private int agendadas;
    private static final AtomicInteger SEMANAS = new AtomicInteger();

    @BeforeEach
    void preparar() {
        od1 = iniciarSesion("odontologo").odontologoId();
        List<OdontologoDto> odontologos = odontologoService.listar(false);
        od2 = odontologos.stream().filter(o -> o.id() != od1).findFirst().orElseThrow().id();
        paciente1 = paciente("Uno");
        paciente2 = paciente("Dos");
        // Cada prueba usa una semana distinta (la base de pruebas es nueva en cada ejecución).
        lunes = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).plusWeeks(10L + 3L * SEMANAS.getAndIncrement());
        agendadas = 0;
    }

    private long paciente(String nombre) {
        return pacienteService.guardar(new PacienteForm(null, TipoDocumento.DNI, dniAleatorio(), nombre, "Agenda" + sufijo(),
                null, LocalDate.now().minusYears(30), Sexo.F, null, null, null, null, null, null, null, null, null, true));
    }

    private CitaDto cita(long paciente, long odontologo, LocalDateTime inicio, int minutos) {
        agendadas++;
        return citaService.programar(new CitaForm(null, paciente, odontologo, inicio, minutos, "Control " + agendadas, null, null));
    }

    private LocalDateTime lunesA(int hora, int minuto) {
        return lunes.atTime(hora, minuto);
    }

    // ------------------------------------------------------------------ solapamientos

    @Test
    void noSeCruzanCitasDelOdontologoNiDelPaciente() {
        CitaDto a = cita(paciente1, od1, lunesA(9, 0), 30);
        assertThat(a.estado()).isEqualTo(EstadoCita.PROGRAMADA);
        assertThat(a.fin()).isEqualTo(lunesA(9, 30));

        assertThatThrownBy(() -> cita(paciente2, od1, lunesA(9, 15), 30))
                .isInstanceOf(NegocioException.class).hasMessageContaining("ya tiene una cita que se cruza");
        assertThat(cita(paciente2, od1, lunesA(9, 30), 30).inicio()).as("[inicio, fin): 09:30 no choca con la que termina a las 09:30")
                .isEqualTo(lunesA(9, 30));
        assertThatThrownBy(() -> cita(paciente1, od2, lunesA(9, 15), 30))
                .as("el mismo paciente con otro odontólogo").hasMessageContaining("paciente ya tiene otra cita");

        citaService.cambiarEstado(a.id(), EstadoCita.CANCELADA);
        assertThat(cita(paciente1, od2, lunesA(9, 0), 15).estado()).as("la cancelada libera el horario")
                .isEqualTo(EstadoCita.PROGRAMADA);
    }

    @Test
    void dentroDelHorarioYEnLaGrilla() {
        assertThatThrownBy(() -> cita(paciente1, od1, lunesA(13, 0), 30)).hasMessageContaining("fuera del horario");
        assertThatThrownBy(() -> cita(paciente1, od1, lunesA(12, 45), 30)).as("cruza el fin del bloque de la mañana")
                .hasMessageContaining("fuera del horario");
        assertThatThrownBy(() -> cita(paciente1, od1, lunes.plusDays(6).atTime(10, 0), 30)).as("domingo")
                .hasMessageContaining("fuera del horario");
        assertThatThrownBy(() -> cita(paciente1, od1, lunesA(9, 10), 30)).hasMessageContaining("grilla de 15 minutos");
        assertThatThrownBy(() -> cita(paciente1, od1, lunesA(9, 0), 20)).hasMessageContaining("múltiplo de 15");
        assertThatThrownBy(() -> cita(paciente1, od1, lunesA(9, 0), 255)).hasMessageContaining("entre 15 y 240");
        assertThatThrownBy(() -> cita(paciente1, od1, LocalDate.now().minusWeeks(1).with(DayOfWeek.MONDAY).atTime(9, 0), 30))
                .hasMessageContaining("en el pasado");
        assertThat(cita(paciente1, od1, lunesA(15, 0), 240).fin()).isEqualTo(lunesA(19, 0));
    }

    @Test
    void moverConValidacionYEditarSinCambiarHorario() {
        CitaDto a = cita(paciente1, od1, lunesA(9, 0), 30);
        CitaDto b = cita(paciente2, od1, lunesA(10, 0), 45);

        CitaDto movida = citaService.mover(a.id(), lunesA(11, 0));
        assertThat(movida.inicio()).isEqualTo(lunesA(11, 0));
        assertThat(movida.fin()).as("conserva la duración").isEqualTo(lunesA(11, 30));
        assertThatThrownBy(() -> citaService.mover(a.id(), lunesA(10, 30))).hasMessageContaining("se cruza");
        assertThatThrownBy(() -> citaService.mover(a.id(), lunesA(12, 45))).hasMessageContaining("fuera del horario");
        assertThat(citaService.obtener(a.id()).inicio()).as("un movimiento rechazado no cambia nada").isEqualTo(lunesA(11, 0));
        assertThat(citaService.mover(b.id(), lunesA(10, 15)).inicio()).as("puede solaparse consigo misma")
                .isEqualTo(lunesA(10, 15));

        CitaDto editada = citaService.programar(new CitaForm(a.id(), paciente1, od1, lunesA(11, 0), 30, "Control de resina",
                "Traer radiografía", null));
        assertThat(editada.motivo()).isEqualTo("Control de resina");
        assertThat(editada.notas()).isEqualTo("Traer radiografía");
    }

    // ------------------------------------------------------------------ siguiente hueco

    @Test
    void siguienteHuecoRespetaHorarioYCitas() {
        cita(paciente2, od1, lunesA(9, 0), 60);
        cita(paciente2, od1, lunesA(10, 30), 30);
        assertThat(citaService.siguienteHueco(od1, 30, lunesA(8, 0), null)).contains(lunesA(10, 0));
        assertThat(citaService.siguienteHueco(od1, 45, lunesA(8, 0), null)).as("10:00–10:30 no alcanza")
                .contains(lunesA(11, 0));

        cita(paciente1, od2, lunesA(11, 0), 30);
        assertThat(citaService.siguienteHueco(od1, 45, lunesA(8, 0), paciente1))
                .as("también evita las otras citas del paciente").contains(lunesA(11, 30));
        assertThat(citaService.siguienteHueco(od1, 30, lunesA(12, 50), null)).as("redondea a la grilla y salta al bloque de la tarde")
                .contains(lunesA(15, 0));
        assertThat(citaService.siguienteHueco(od1, 240, lunes.plusDays(5).atTime(9, 0), null))
                .as("el sábado 09:00–13:00 alcanza justo para 4 horas").contains(lunes.plusDays(5).atTime(9, 0));
        assertThat(citaService.siguienteHueco(od1, 240, lunes.plusDays(5).atTime(9, 15), null))
                .as("desde las 09:15 del sábado ya no alcanza: pasa al lunes siguiente")
                .contains(lunes.plusWeeks(1).atTime(9, 0));
    }

    // ------------------------------------------------------------------ estados y agenda

    @Test
    void estadosDeLaCita() {
        CitaDto a = cita(paciente1, od1, lunesA(9, 0), 30);
        assertThat(citaService.cambiarEstado(a.id(), EstadoCita.CONFIRMADA).estado()).isEqualTo(EstadoCita.CONFIRMADA);
        assertThatThrownBy(() -> citaService.cambiarEstado(a.id(), EstadoCita.NO_ASISTIO)).hasMessageContaining("aún no empieza");
        assertThatThrownBy(() -> citaService.cambiarEstado(a.id(), EstadoCita.ATENDIDA)).hasMessageContaining("al cerrar la atención");
        citaService.cambiarEstado(a.id(), EstadoCita.CANCELADA);
        assertThatThrownBy(() -> citaService.cambiarEstado(a.id(), EstadoCita.CONFIRMADA)).hasMessageContaining("cancelada no puede");
        assertThatThrownBy(() -> citaService.mover(a.id(), lunesA(10, 0))).hasMessageContaining("ya no se puede modificar");
    }

    @Test
    void laAgendaMuestraLaSemanaSinLasCanceladas() {
        CitaDto a = cita(paciente1, od1, lunesA(9, 0), 30);
        cita(paciente2, od1, lunes.plusDays(2).atTime(16, 0), 60);
        cita(paciente2, od1, lunes.plusWeeks(1).atTime(9, 0), 30);
        citaService.cambiarEstado(a.id(), EstadoCita.CANCELADA);

        AgendaDto semana = citaService.agenda(od1, lunes, 7);
        assertThat(semana.citas()).singleElement().satisfies(c -> {
            assertThat(c.inicio()).isEqualTo(lunes.plusDays(2).atTime(16, 0));
            assertThat(c.duracionMin()).isEqualTo(60);
            assertThat(c.colorAgenda()).startsWith("#");
        });
        assertThat(semana.odontologo().horarios()).isNotEmpty();
        assertThat(semana.intervaloMin()).isEqualTo(15);
    }

    // ------------------------------------------------------------------ cita → atención

    @Test
    void atenderDesdeLaCitaYCerrarLaDejaAtendida() {
        CitaDto futura = cita(paciente1, od1, lunesA(9, 0), 30);
        assertThatThrownBy(() -> atencionService.abrirDesdeCita(futura.id())).hasMessageContaining("Solo se atienden las citas del día");

        // La cita de hoy se crea en una fecha futura y se lleva a hoy por SQL (el servicio no programa en el pasado).
        CitaDto hoy = cita(paciente2, od1, lunesA(10, 0), 30);
        LocalDateTime inicioHoy = LocalDate.now().atTime(LocalTime.of(8, 0));
        jdbc.update("UPDATE cita SET inicio = ?, fin = ? WHERE id = ?", inicioHoy + ":00", inicioHoy.plusMinutes(30) + ":00",
                hoy.id());

        iniciarSesion("recepcion");
        assertThatThrownBy(() -> atencionService.abrirDesdeCita(hoy.id())).isInstanceOf(PermisoDenegadoException.class);
        iniciarSesion("odontologo");
        long atencion = atencionService.abrirDesdeCita(hoy.id());
        assertThat(atencionService.obtener(atencion).motivoConsulta()).isEqualTo(hoy.motivo());
        assertThat(citaService.obtener(hoy.id()).atencionId()).isEqualTo(atencion);
        assertThatThrownBy(() -> atencionService.abrirDesdeCita(hoy.id())).hasMessageContaining("ya tiene una atención");
        assertThatThrownBy(() -> citaService.cambiarEstado(hoy.id(), EstadoCita.CANCELADA)).hasMessageContaining("atención abierta");

        long odontograma = odontogramaService.obtenerVista(paciente2, null).odontogramaId();
        assertThatThrownBy(() -> atencionService.cerrar(atencion)).as("un odontograma inicial vacío debe firmarse")
                .hasMessageContaining("Cierre y firme");
        odontogramaService.cerrar(odontograma);
        atencionService.cerrar(atencion);
        assertThat(citaService.obtener(hoy.id()).estado()).isEqualTo(EstadoCita.ATENDIDA);
    }

    @Test
    void recepcionProgramaCitas() {
        iniciarSesion("recepcion");
        assertThat(cita(paciente1, od2, lunesA(9, 0), 30).odontologoId()).isEqualTo(od2);
    }
}
