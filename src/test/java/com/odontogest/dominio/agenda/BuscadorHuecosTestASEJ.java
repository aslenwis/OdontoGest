package com.odontogest.dominio.agenda;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BuscadorHuecosTestASEJ {

    private static final LocalDate LUNESASEJ = LocalDate.of(2026, 3, 2);
    private static final Duration MEDIA_HORAASEJ = Duration.ofMinutes(30);
    private static final Duration PASOASEJ = Duration.ofMinutes(15);

    private static final List<BloqueHorarioASEJ> HORARIOASEJ = horarioDemoASEJ();

    private final BuscadorHuecosASEJ buscadorASEJ = new BuscadorHuecosASEJ();

    private static List<BloqueHorarioASEJ> horarioDemoASEJ() {
        List<BloqueHorarioASEJ> hASEJ = new ArrayList<>();
        for (DayOfWeek dASEJ : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)) {
            hASEJ.add(new BloqueHorarioASEJ(dASEJ, LocalTime.of(15, 0), LocalTime.of(19, 0)));
            hASEJ.add(new BloqueHorarioASEJ(dASEJ, LocalTime.of(9, 0), LocalTime.of(13, 0)));
        }
        hASEJ.add(new BloqueHorarioASEJ(DayOfWeek.SATURDAY, LocalTime.of(9, 0), LocalTime.of(13, 0)));
        return List.copyOf(hASEJ);
    }

    private static LocalDateTime tASEJ(LocalDate diaASEJ, int horaASEJ, int minutoASEJ) {
        return diaASEJ.atTime(horaASEJ, minutoASEJ);
    }

    private static IntervaloASEJ citaASEJ(LocalDate diaASEJ, int h1ASEJ, int m1ASEJ, int h2ASEJ, int m2ASEJ) {
        return new IntervaloASEJ(tASEJ(diaASEJ, h1ASEJ, m1ASEJ), tASEJ(diaASEJ, h2ASEJ, m2ASEJ));
    }

    private Optional<IntervaloASEJ> buscarASEJ(LocalDateTime desdeASEJ, Duration duracionASEJ, List<IntervaloASEJ> ocupadosASEJ) {
        return buscadorASEJ.siguienteHuecoASEJ(desdeASEJ, duracionASEJ, PASOASEJ, HORARIOASEJ, ocupadosASEJ, 30);
    }

    @Test
    void agendaVaciaDevuelveElInicioDelBloqueASEJ() {
        assertThat(buscarASEJ(tASEJ(LUNESASEJ, 7, 0), MEDIA_HORAASEJ, List.of()))
                .contains(citaASEJ(LUNESASEJ, 9, 0, 9, 30));
    }

    @Test
    void redondeaElInicioAlPasoASEJ() {
        assertThat(buscarASEJ(tASEJ(LUNESASEJ, 10, 7), MEDIA_HORAASEJ, List.of()))
                .contains(citaASEJ(LUNESASEJ, 10, 15, 10, 45));
        assertThat(buscarASEJ(tASEJ(LUNESASEJ, 10, 15), MEDIA_HORAASEJ, List.of()))
                .as("ya alineado").contains(citaASEJ(LUNESASEJ, 10, 15, 10, 45));
    }

    @Test
    void encuentraElHuecoEntreDosCitasASEJ() {
        List<IntervaloASEJ> ocupadosASEJ = List.of(citaASEJ(LUNESASEJ, 9, 0, 10, 0), citaASEJ(LUNESASEJ, 10, 30, 12, 0));
        assertThat(buscarASEJ(tASEJ(LUNESASEJ, 8, 0), MEDIA_HORAASEJ, ocupadosASEJ))
                .as("hueco exacto de 30 min").contains(citaASEJ(LUNESASEJ, 10, 0, 10, 30));
        assertThat(buscarASEJ(tASEJ(LUNESASEJ, 8, 0), Duration.ofMinutes(45), ocupadosASEJ))
                .as("el hueco de 45 min solo cabe después de las 12:00").contains(citaASEJ(LUNESASEJ, 12, 0, 12, 45));
    }

    @Test
    void huecoAlFinalDelBloqueASEJ() {
        List<IntervaloASEJ> ocupadosASEJ = List.of(citaASEJ(LUNESASEJ, 9, 0, 12, 30));
        assertThat(buscarASEJ(tASEJ(LUNESASEJ, 8, 0), MEDIA_HORAASEJ, ocupadosASEJ)).contains(citaASEJ(LUNESASEJ, 12, 30, 13, 0));
    }

    @Test
    void siNoCabeEnLaMananaPasaAlBloqueDeLaTardeASEJ() {
        List<IntervaloASEJ> ocupadosASEJ = List.of(citaASEJ(LUNESASEJ, 9, 0, 12, 45));
        assertThat(buscarASEJ(tASEJ(LUNESASEJ, 8, 0), MEDIA_HORAASEJ, ocupadosASEJ)).contains(citaASEJ(LUNESASEJ, 15, 0, 15, 30));
    }

    @Test
    void citaQueTerminaFueraDelPasoRedondeaElCursorASEJ() {
        List<IntervaloASEJ> ocupadosASEJ = List.of(citaASEJ(LUNESASEJ, 9, 0, 9, 50));
        assertThat(buscarASEJ(tASEJ(LUNESASEJ, 8, 0), MEDIA_HORAASEJ, ocupadosASEJ)).contains(citaASEJ(LUNESASEJ, 10, 0, 10, 30));
    }

    @Test
    void citasQueSeSolapanEntreSiSeRespetanASEJ() {
        List<IntervaloASEJ> ocupadosASEJ = List.of(citaASEJ(LUNESASEJ, 9, 0, 11, 0), citaASEJ(LUNESASEJ, 9, 30, 10, 0));
        assertThat(buscarASEJ(tASEJ(LUNESASEJ, 8, 0), MEDIA_HORAASEJ, ocupadosASEJ)).contains(citaASEJ(LUNESASEJ, 11, 0, 11, 30));
    }

    @Test
    void desdeLaTardeDelSabadoSaltaAlLunesASEJ() {
        LocalDate sabadoASEJ = LUNESASEJ.plusDays(5);
        assertThat(buscarASEJ(tASEJ(sabadoASEJ, 14, 0), MEDIA_HORAASEJ, List.of()))
                .contains(citaASEJ(LUNESASEJ.plusWeeks(1), 9, 0, 9, 30));
    }

    @Test
    void sinHuecoEnElRangoDeDiasASEJ() {
        assertThat(buscadorASEJ.siguienteHuecoASEJ(tASEJ(LUNESASEJ, 8, 0), Duration.ofHours(5), PASOASEJ, HORARIOASEJ, List.of(), 30))
                .as("ningún bloque dura 5 horas").isEmpty();
        assertThat(buscadorASEJ.siguienteHuecoASEJ(tASEJ(LUNESASEJ, 8, 0), MEDIA_HORAASEJ, PASOASEJ, List.of(), List.of(), 30)).isEmpty();
    }

    @Test
    void validaParametrosASEJ() {
        assertThatThrownBy(() -> buscadorASEJ.siguienteHuecoASEJ(tASEJ(LUNESASEJ, 8, 0), Duration.ZERO, PASOASEJ, HORARIOASEJ, List.of(), 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> buscadorASEJ.siguienteHuecoASEJ(tASEJ(LUNESASEJ, 8, 0), MEDIA_HORAASEJ, Duration.ofMinutes(-1), HORARIOASEJ, List.of(), 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void solapamientoSemiabiertoASEJ() {
        IntervaloASEJ aASEJ = citaASEJ(LUNESASEJ, 9, 0, 10, 0);
        assertThat(aASEJ.solapaASEJ(citaASEJ(LUNESASEJ, 10, 0, 10, 30))).as("contiguas no se solapan").isFalse();
        assertThat(aASEJ.solapaASEJ(citaASEJ(LUNESASEJ, 9, 59, 10, 30))).isTrue();
        assertThat(aASEJ.solapaASEJ(citaASEJ(LUNESASEJ, 9, 15, 9, 30))).isTrue();
        assertThat(aASEJ.solapaASEJ(citaASEJ(LUNESASEJ, 8, 0, 9, 0))).isFalse();
        assertThat(aASEJ.duracionASEJ()).isEqualTo(Duration.ofHours(1));
        assertThatThrownBy(() -> citaASEJ(LUNESASEJ, 9, 0, 9, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void citaDentroDelHorarioASEJ() {
        assertThat(BloqueHorarioASEJ.dentroDelHorarioASEJ(citaASEJ(LUNESASEJ, 12, 30, 13, 0), HORARIOASEJ)).isTrue();
        assertThat(BloqueHorarioASEJ.dentroDelHorarioASEJ(citaASEJ(LUNESASEJ, 12, 45, 13, 15), HORARIOASEJ)).isFalse();
        assertThat(BloqueHorarioASEJ.dentroDelHorarioASEJ(citaASEJ(LUNESASEJ.plusDays(6), 10, 0, 10, 30), HORARIOASEJ))
                .as("domingo").isFalse();
        assertThat(BloqueHorarioASEJ.dentroDelHorarioASEJ(
                new IntervaloASEJ(tASEJ(LUNESASEJ, 12, 0), tASEJ(LUNESASEJ.plusDays(1), 9, 30)), HORARIOASEJ)).isFalse();
        assertThatThrownBy(() -> new BloqueHorarioASEJ(DayOfWeek.MONDAY, LocalTime.NOON, LocalTime.NOON))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HORARIOASEJ.get(0).enASEJ(LUNESASEJ.plusDays(1))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void redondeoAlPasoASEJ() {
        assertThat(BuscadorHuecosASEJ.redondearArribaASEJ(tASEJ(LUNESASEJ, 9, 1), PASOASEJ)).isEqualTo(tASEJ(LUNESASEJ, 9, 15));
        assertThat(BuscadorHuecosASEJ.redondearArribaASEJ(tASEJ(LUNESASEJ, 9, 0), PASOASEJ)).isEqualTo(tASEJ(LUNESASEJ, 9, 0));
        assertThat(BuscadorHuecosASEJ.redondearArribaASEJ(tASEJ(LUNESASEJ, 23, 50), PASOASEJ)).isEqualTo(tASEJ(LUNESASEJ.plusDays(1), 0, 0));
    }
}
