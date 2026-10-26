package com.odontogest.servicio;

import com.odontogest.PruebaIntegracionASEJ;
import com.odontogest.excepcion.NegocioExceptionASEJ;
import com.odontogest.excepcion.PermisoDenegadoExceptionASEJ;
import com.odontogest.modelo.EstadoCitaASEJ;
import com.odontogest.modelo.SexoASEJ;
import com.odontogest.modelo.TipoDocumentoASEJ;
import com.odontogest.servicio.dto.AgendaDtoASEJ;
import com.odontogest.servicio.dto.CitaDtoASEJ;
import com.odontogest.servicio.dto.CitaFormASEJ;
import com.odontogest.servicio.dto.OdontologoDtoASEJ;
import com.odontogest.servicio.dto.PacienteFormASEJ;
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

class CitaServiceTestASEJ extends PruebaIntegracionASEJ {

    @Autowired private PacienteServiceASEJ pacienteServiceASEJ;
    @Autowired private OdontologoServiceASEJ odontologoServiceASEJ;
    @Autowired private CitaServiceASEJ citaServiceASEJ;
    @Autowired private AtencionServiceASEJ atencionServiceASEJ;
    @Autowired private OdontogramaServiceASEJ odontogramaServiceASEJ;
    @Autowired private JdbcTemplate jdbcASEJ;

    private long od1ASEJ;
    private long od2ASEJ;
    private long paciente1ASEJ;
    private long paciente2ASEJ;
    private LocalDate lunesASEJ;
    private int agendadasASEJ;
    private static final AtomicInteger SEMANASASEJ = new AtomicInteger();

    @BeforeEach
    void prepararASEJ() {
        od1ASEJ = iniciarSesionASEJ("odontologo").odontologoIdASEJ();
        List<OdontologoDtoASEJ> odontologosASEJ = odontologoServiceASEJ.listarASEJ(false);
        od2ASEJ = odontologosASEJ.stream().filter(oASEJ -> oASEJ.idASEJ() != od1ASEJ).findFirst().orElseThrow().idASEJ();
        paciente1ASEJ = pacienteASEJ("Uno");
        paciente2ASEJ = pacienteASEJ("Dos");
        lunesASEJ = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).plusWeeks(10L + 3L * SEMANASASEJ.getAndIncrement());
        agendadasASEJ = 0;
    }

    private long pacienteASEJ(String nombreASEJ) {
        return pacienteServiceASEJ.guardarASEJ(new PacienteFormASEJ(null, TipoDocumentoASEJ.DNI, dniAleatorioASEJ(), nombreASEJ, "Agenda" + sufijoASEJ(),
                null, LocalDate.now().minusYears(30), SexoASEJ.F, null, null, null, null, null, null, null, null, null, true));
    }

    private CitaDtoASEJ citaASEJ(long pacienteASEJ, long odontologoASEJ, LocalDateTime inicioASEJ, int minutosASEJ) {
        agendadasASEJ++;
        return citaServiceASEJ.programarASEJ(new CitaFormASEJ(null, pacienteASEJ, odontologoASEJ, inicioASEJ, minutosASEJ, "Control " + agendadasASEJ, null, null));
    }

    private LocalDateTime lunesAASEJ(int horaASEJ, int minutoASEJ) {
        return lunesASEJ.atTime(horaASEJ, minutoASEJ);
    }

    @Test
    void noSeCruzanCitasDelOdontologoNiDelPacienteASEJ() {
        CitaDtoASEJ aASEJ = citaASEJ(paciente1ASEJ, od1ASEJ, lunesAASEJ(9, 0), 30);
        assertThat(aASEJ.estadoASEJ()).isEqualTo(EstadoCitaASEJ.PROGRAMADA);
        assertThat(aASEJ.finASEJ()).isEqualTo(lunesAASEJ(9, 30));

        assertThatThrownBy(() -> citaASEJ(paciente2ASEJ, od1ASEJ, lunesAASEJ(9, 15), 30))
                .isInstanceOf(NegocioExceptionASEJ.class).hasMessageContaining("ya tiene una cita que se cruza");
        assertThat(citaASEJ(paciente2ASEJ, od1ASEJ, lunesAASEJ(9, 30), 30).inicioASEJ()).as("[inicio, fin): 09:30 no choca con la que termina a las 09:30")
                .isEqualTo(lunesAASEJ(9, 30));
        assertThatThrownBy(() -> citaASEJ(paciente1ASEJ, od2ASEJ, lunesAASEJ(9, 15), 30))
                .as("el mismo paciente con otro odontólogo").hasMessageContaining("paciente ya tiene otra cita");

        citaServiceASEJ.cambiarEstadoASEJ(aASEJ.idASEJ(), EstadoCitaASEJ.CANCELADA);
        assertThat(citaASEJ(paciente1ASEJ, od2ASEJ, lunesAASEJ(9, 0), 15).estadoASEJ()).as("la cancelada libera el horario")
                .isEqualTo(EstadoCitaASEJ.PROGRAMADA);
    }

    @Test
    void dentroDelHorarioYEnLaGrillaASEJ() {
        assertThatThrownBy(() -> citaASEJ(paciente1ASEJ, od1ASEJ, lunesAASEJ(13, 0), 30)).hasMessageContaining("fuera del horario");
        assertThatThrownBy(() -> citaASEJ(paciente1ASEJ, od1ASEJ, lunesAASEJ(12, 45), 30)).as("cruza el fin del bloque de la mañana")
                .hasMessageContaining("fuera del horario");
        assertThatThrownBy(() -> citaASEJ(paciente1ASEJ, od1ASEJ, lunesASEJ.plusDays(6).atTime(10, 0), 30)).as("domingo")
                .hasMessageContaining("fuera del horario");
        assertThatThrownBy(() -> citaASEJ(paciente1ASEJ, od1ASEJ, lunesAASEJ(9, 10), 30)).hasMessageContaining("grilla de 15 minutos");
        assertThatThrownBy(() -> citaASEJ(paciente1ASEJ, od1ASEJ, lunesAASEJ(9, 0), 20)).hasMessageContaining("múltiplo de 15");
        assertThatThrownBy(() -> citaASEJ(paciente1ASEJ, od1ASEJ, lunesAASEJ(9, 0), 255)).hasMessageContaining("entre 15 y 240");
        assertThatThrownBy(() -> citaASEJ(paciente1ASEJ, od1ASEJ, LocalDate.now().minusWeeks(1).with(DayOfWeek.MONDAY).atTime(9, 0), 30))
                .hasMessageContaining("en el pasado");
        assertThat(citaASEJ(paciente1ASEJ, od1ASEJ, lunesAASEJ(15, 0), 240).finASEJ()).isEqualTo(lunesAASEJ(19, 0));
    }

    @Test
    void moverConValidacionYEditarSinCambiarHorarioASEJ() {
        CitaDtoASEJ aASEJ = citaASEJ(paciente1ASEJ, od1ASEJ, lunesAASEJ(9, 0), 30);
        CitaDtoASEJ bASEJ = citaASEJ(paciente2ASEJ, od1ASEJ, lunesAASEJ(10, 0), 45);

        CitaDtoASEJ movidaASEJ = citaServiceASEJ.moverASEJ(aASEJ.idASEJ(), lunesAASEJ(11, 0));
        assertThat(movidaASEJ.inicioASEJ()).isEqualTo(lunesAASEJ(11, 0));
        assertThat(movidaASEJ.finASEJ()).as("conserva la duración").isEqualTo(lunesAASEJ(11, 30));
        assertThatThrownBy(() -> citaServiceASEJ.moverASEJ(aASEJ.idASEJ(), lunesAASEJ(10, 30))).hasMessageContaining("se cruza");
        assertThatThrownBy(() -> citaServiceASEJ.moverASEJ(aASEJ.idASEJ(), lunesAASEJ(12, 45))).hasMessageContaining("fuera del horario");
        assertThat(citaServiceASEJ.obtenerASEJ(aASEJ.idASEJ()).inicioASEJ()).as("un movimiento rechazado no cambia nada").isEqualTo(lunesAASEJ(11, 0));
        assertThat(citaServiceASEJ.moverASEJ(bASEJ.idASEJ(), lunesAASEJ(10, 15)).inicioASEJ()).as("puede solaparse consigo misma")
                .isEqualTo(lunesAASEJ(10, 15));

        CitaDtoASEJ editadaASEJ = citaServiceASEJ.programarASEJ(new CitaFormASEJ(aASEJ.idASEJ(), paciente1ASEJ, od1ASEJ, lunesAASEJ(11, 0), 30, "Control de resina",
                "Traer radiografía", null));
        assertThat(editadaASEJ.motivoASEJ()).isEqualTo("Control de resina");
        assertThat(editadaASEJ.notasASEJ()).isEqualTo("Traer radiografía");
    }

    @Test
    void siguienteHuecoRespetaHorarioYCitasASEJ() {
        citaASEJ(paciente2ASEJ, od1ASEJ, lunesAASEJ(9, 0), 60);
        citaASEJ(paciente2ASEJ, od1ASEJ, lunesAASEJ(10, 30), 30);
        assertThat(citaServiceASEJ.siguienteHuecoASEJ(od1ASEJ, 30, lunesAASEJ(8, 0), null)).contains(lunesAASEJ(10, 0));
        assertThat(citaServiceASEJ.siguienteHuecoASEJ(od1ASEJ, 45, lunesAASEJ(8, 0), null)).as("10:00–10:30 no alcanza")
                .contains(lunesAASEJ(11, 0));

        citaASEJ(paciente1ASEJ, od2ASEJ, lunesAASEJ(11, 0), 30);
        assertThat(citaServiceASEJ.siguienteHuecoASEJ(od1ASEJ, 45, lunesAASEJ(8, 0), paciente1ASEJ))
                .as("también evita las otras citas del paciente").contains(lunesAASEJ(11, 30));
        assertThat(citaServiceASEJ.siguienteHuecoASEJ(od1ASEJ, 30, lunesAASEJ(12, 50), null)).as("redondea a la grilla y salta al bloque de la tarde")
                .contains(lunesAASEJ(15, 0));
        assertThat(citaServiceASEJ.siguienteHuecoASEJ(od1ASEJ, 240, lunesASEJ.plusDays(5).atTime(9, 0), null))
                .as("el sábado 09:00–13:00 alcanza justo para 4 horas").contains(lunesASEJ.plusDays(5).atTime(9, 0));
        assertThat(citaServiceASEJ.siguienteHuecoASEJ(od1ASEJ, 240, lunesASEJ.plusDays(5).atTime(9, 15), null))
                .as("desde las 09:15 del sábado ya no alcanza: pasa al lunes siguiente")
                .contains(lunesASEJ.plusWeeks(1).atTime(9, 0));
    }

    @Test
    void estadosDeLaCitaASEJ() {
        CitaDtoASEJ aASEJ = citaASEJ(paciente1ASEJ, od1ASEJ, lunesAASEJ(9, 0), 30);
        assertThat(citaServiceASEJ.cambiarEstadoASEJ(aASEJ.idASEJ(), EstadoCitaASEJ.CONFIRMADA).estadoASEJ()).isEqualTo(EstadoCitaASEJ.CONFIRMADA);
        assertThatThrownBy(() -> citaServiceASEJ.cambiarEstadoASEJ(aASEJ.idASEJ(), EstadoCitaASEJ.NO_ASISTIO)).hasMessageContaining("aún no empieza");
        assertThatThrownBy(() -> citaServiceASEJ.cambiarEstadoASEJ(aASEJ.idASEJ(), EstadoCitaASEJ.ATENDIDA)).hasMessageContaining("al cerrar la atención");
        citaServiceASEJ.cambiarEstadoASEJ(aASEJ.idASEJ(), EstadoCitaASEJ.CANCELADA);
        assertThatThrownBy(() -> citaServiceASEJ.cambiarEstadoASEJ(aASEJ.idASEJ(), EstadoCitaASEJ.CONFIRMADA)).hasMessageContaining("cancelada no puede");
        assertThatThrownBy(() -> citaServiceASEJ.moverASEJ(aASEJ.idASEJ(), lunesAASEJ(10, 0))).hasMessageContaining("ya no se puede modificar");
    }

    @Test
    void laAgendaMuestraLaSemanaSinLasCanceladasASEJ() {
        CitaDtoASEJ aASEJ = citaASEJ(paciente1ASEJ, od1ASEJ, lunesAASEJ(9, 0), 30);
        citaASEJ(paciente2ASEJ, od1ASEJ, lunesASEJ.plusDays(2).atTime(16, 0), 60);
        citaASEJ(paciente2ASEJ, od1ASEJ, lunesASEJ.plusWeeks(1).atTime(9, 0), 30);
        citaServiceASEJ.cambiarEstadoASEJ(aASEJ.idASEJ(), EstadoCitaASEJ.CANCELADA);

        AgendaDtoASEJ semanaASEJ = citaServiceASEJ.agendaASEJ(od1ASEJ, lunesASEJ, 7);
        assertThat(semanaASEJ.citasASEJ()).singleElement().satisfies(cASEJ -> {
            assertThat(cASEJ.inicioASEJ()).isEqualTo(lunesASEJ.plusDays(2).atTime(16, 0));
            assertThat(cASEJ.duracionMinASEJ()).isEqualTo(60);
            assertThat(cASEJ.colorAgendaASEJ()).startsWith("#");
        });
        assertThat(semanaASEJ.odontologoASEJ().horariosASEJ()).isNotEmpty();
        assertThat(semanaASEJ.intervaloMinASEJ()).isEqualTo(15);
    }

    @Test
    void atenderDesdeLaCitaYCerrarLaDejaAtendidaASEJ() {
        CitaDtoASEJ futuraASEJ = citaASEJ(paciente1ASEJ, od1ASEJ, lunesAASEJ(9, 0), 30);
        assertThatThrownBy(() -> atencionServiceASEJ.abrirDesdeCitaASEJ(futuraASEJ.idASEJ())).hasMessageContaining("Solo se atienden las citas del día");

        CitaDtoASEJ hoyASEJ = citaASEJ(paciente2ASEJ, od1ASEJ, lunesAASEJ(10, 0), 30);
        LocalDateTime inicioHoyASEJ = LocalDate.now().atTime(LocalTime.of(8, 0));
        jdbcASEJ.update("UPDATE cita SET inicio = ?, fin = ? WHERE id = ?", inicioHoyASEJ + ":00", inicioHoyASEJ.plusMinutes(30) + ":00",
                hoyASEJ.idASEJ());

        iniciarSesionASEJ("recepcion");
        assertThatThrownBy(() -> atencionServiceASEJ.abrirDesdeCitaASEJ(hoyASEJ.idASEJ())).isInstanceOf(PermisoDenegadoExceptionASEJ.class);
        iniciarSesionASEJ("odontologo");
        long atencionASEJ = atencionServiceASEJ.abrirDesdeCitaASEJ(hoyASEJ.idASEJ());
        assertThat(atencionServiceASEJ.obtenerASEJ(atencionASEJ).motivoConsultaASEJ()).isEqualTo(hoyASEJ.motivoASEJ());
        assertThat(citaServiceASEJ.obtenerASEJ(hoyASEJ.idASEJ()).atencionIdASEJ()).isEqualTo(atencionASEJ);
        assertThatThrownBy(() -> atencionServiceASEJ.abrirDesdeCitaASEJ(hoyASEJ.idASEJ())).hasMessageContaining("ya tiene una atención");
        assertThatThrownBy(() -> citaServiceASEJ.cambiarEstadoASEJ(hoyASEJ.idASEJ(), EstadoCitaASEJ.CANCELADA)).hasMessageContaining("atención abierta");

        long odontogramaASEJ = odontogramaServiceASEJ.obtenerVistaASEJ(paciente2ASEJ, null).odontogramaIdASEJ();
        assertThatThrownBy(() -> atencionServiceASEJ.cerrarASEJ(atencionASEJ)).as("un odontograma inicial vacío debe firmarse")
                .hasMessageContaining("Cierre y firme");
        odontogramaServiceASEJ.cerrarASEJ(odontogramaASEJ);
        atencionServiceASEJ.cerrarASEJ(atencionASEJ);
        assertThat(citaServiceASEJ.obtenerASEJ(hoyASEJ.idASEJ()).estadoASEJ()).isEqualTo(EstadoCitaASEJ.ATENDIDA);
    }

    @Test
    void recepcionProgramaCitasASEJ() {
        iniciarSesionASEJ("recepcion");
        assertThat(citaASEJ(paciente1ASEJ, od2ASEJ, lunesAASEJ(9, 0), 30).odontologoIdASEJ()).isEqualTo(od2ASEJ);
    }
}
