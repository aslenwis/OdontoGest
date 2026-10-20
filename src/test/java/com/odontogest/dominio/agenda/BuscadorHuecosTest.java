/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
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

/**
 * Siguiente hueco libre en la agenda y solapamiento de intervalos.
 */
class BuscadorHuecosTest {

    /** Lunes. */
    private static final LocalDate LUNES = LocalDate.of(2026, 3, 2);
    private static final Duration MEDIA_HORA = Duration.ofMinutes(30);
    private static final Duration PASO = Duration.ofMinutes(15);

    /** Horario demo: L–V 09:00–13:00 y 15:00–19:00, S 09:00–13:00. */
    private static final List<BloqueHorario> HORARIO = horarioDemo();

    private final BuscadorHuecos buscador = new BuscadorHuecos();

    private static List<BloqueHorario> horarioDemo() {
        List<BloqueHorario> h = new ArrayList<>();
        for (DayOfWeek d : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)) {
            h.add(new BloqueHorario(d, LocalTime.of(15, 0), LocalTime.of(19, 0)));   // desordenado a propósito
            h.add(new BloqueHorario(d, LocalTime.of(9, 0), LocalTime.of(13, 0)));
        }
        h.add(new BloqueHorario(DayOfWeek.SATURDAY, LocalTime.of(9, 0), LocalTime.of(13, 0)));
        return List.copyOf(h);
    }

    private static LocalDateTime t(LocalDate dia, int hora, int minuto) {
        return dia.atTime(hora, minuto);
    }

    private static Intervalo cita(LocalDate dia, int h1, int m1, int h2, int m2) {
        return new Intervalo(t(dia, h1, m1), t(dia, h2, m2));
    }

    private Optional<Intervalo> buscar(LocalDateTime desde, Duration duracion, List<Intervalo> ocupados) {
        return buscador.siguienteHueco(desde, duracion, PASO, HORARIO, ocupados, 30);
    }

    @Test
    void agendaVaciaDevuelveElInicioDelBloque() {
        assertThat(buscar(t(LUNES, 7, 0), MEDIA_HORA, List.of()))
                .contains(cita(LUNES, 9, 0, 9, 30));
    }

    @Test
    void redondeaElInicioAlPaso() {
        assertThat(buscar(t(LUNES, 10, 7), MEDIA_HORA, List.of()))
                .contains(cita(LUNES, 10, 15, 10, 45));
        assertThat(buscar(t(LUNES, 10, 15), MEDIA_HORA, List.of()))
                .as("ya alineado").contains(cita(LUNES, 10, 15, 10, 45));
    }

    @Test
    void encuentraElHuecoEntreDosCitas() {
        List<Intervalo> ocupados = List.of(cita(LUNES, 9, 0, 10, 0), cita(LUNES, 10, 30, 12, 0));
        assertThat(buscar(t(LUNES, 8, 0), MEDIA_HORA, ocupados))
                .as("hueco exacto de 30 min").contains(cita(LUNES, 10, 0, 10, 30));
        assertThat(buscar(t(LUNES, 8, 0), Duration.ofMinutes(45), ocupados))
                .as("el hueco de 45 min solo cabe después de las 12:00").contains(cita(LUNES, 12, 0, 12, 45));
    }

    @Test
    void huecoAlFinalDelBloque() {
        List<Intervalo> ocupados = List.of(cita(LUNES, 9, 0, 12, 30));
        assertThat(buscar(t(LUNES, 8, 0), MEDIA_HORA, ocupados)).contains(cita(LUNES, 12, 30, 13, 0));
    }

    @Test
    void siNoCabeEnLaMananaPasaAlBloqueDeLaTarde() {
        List<Intervalo> ocupados = List.of(cita(LUNES, 9, 0, 12, 45));
        assertThat(buscar(t(LUNES, 8, 0), MEDIA_HORA, ocupados)).contains(cita(LUNES, 15, 0, 15, 30));
    }

    @Test
    void citaQueTerminaFueraDelPasoRedondeaElCursor() {
        List<Intervalo> ocupados = List.of(cita(LUNES, 9, 0, 9, 50));
        assertThat(buscar(t(LUNES, 8, 0), MEDIA_HORA, ocupados)).contains(cita(LUNES, 10, 0, 10, 30));
    }

    @Test
    void citasQueSeSolapanEntreSiSeRespetan() {
        List<Intervalo> ocupados = List.of(cita(LUNES, 9, 0, 11, 0), cita(LUNES, 9, 30, 10, 0));
        assertThat(buscar(t(LUNES, 8, 0), MEDIA_HORA, ocupados)).contains(cita(LUNES, 11, 0, 11, 30));
    }

    @Test
    void desdeLaTardeDelSabadoSaltaAlLunes() {
        LocalDate sabado = LUNES.plusDays(5);
        assertThat(buscar(t(sabado, 14, 0), MEDIA_HORA, List.of()))
                .contains(cita(LUNES.plusWeeks(1), 9, 0, 9, 30));
    }

    @Test
    void sinHuecoEnElRangoDeDias() {
        assertThat(buscador.siguienteHueco(t(LUNES, 8, 0), Duration.ofHours(5), PASO, HORARIO, List.of(), 30))
                .as("ningún bloque dura 5 horas").isEmpty();
        assertThat(buscador.siguienteHueco(t(LUNES, 8, 0), MEDIA_HORA, PASO, List.of(), List.of(), 30)).isEmpty();
    }

    @Test
    void validaParametros() {
        assertThatThrownBy(() -> buscador.siguienteHueco(t(LUNES, 8, 0), Duration.ZERO, PASO, HORARIO, List.of(), 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> buscador.siguienteHueco(t(LUNES, 8, 0), MEDIA_HORA, Duration.ofMinutes(-1), HORARIO, List.of(), 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void solapamientoSemiabierto() {
        Intervalo a = cita(LUNES, 9, 0, 10, 0);
        assertThat(a.solapa(cita(LUNES, 10, 0, 10, 30))).as("contiguas no se solapan").isFalse();
        assertThat(a.solapa(cita(LUNES, 9, 59, 10, 30))).isTrue();
        assertThat(a.solapa(cita(LUNES, 9, 15, 9, 30))).isTrue();
        assertThat(a.solapa(cita(LUNES, 8, 0, 9, 0))).isFalse();
        assertThat(a.duracion()).isEqualTo(Duration.ofHours(1));
        assertThatThrownBy(() -> cita(LUNES, 9, 0, 9, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void citaDentroDelHorario() {
        assertThat(BloqueHorario.dentroDelHorario(cita(LUNES, 12, 30, 13, 0), HORARIO)).isTrue();
        assertThat(BloqueHorario.dentroDelHorario(cita(LUNES, 12, 45, 13, 15), HORARIO)).isFalse();
        assertThat(BloqueHorario.dentroDelHorario(cita(LUNES.plusDays(6), 10, 0, 10, 30), HORARIO))
                .as("domingo").isFalse();
        assertThat(BloqueHorario.dentroDelHorario(
                new Intervalo(t(LUNES, 12, 0), t(LUNES.plusDays(1), 9, 30)), HORARIO)).isFalse();
        assertThatThrownBy(() -> new BloqueHorario(DayOfWeek.MONDAY, LocalTime.NOON, LocalTime.NOON))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HORARIO.get(0).en(LUNES.plusDays(1))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void redondeoAlPaso() {
        assertThat(BuscadorHuecos.redondearArriba(t(LUNES, 9, 1), PASO)).isEqualTo(t(LUNES, 9, 15));
        assertThat(BuscadorHuecos.redondearArriba(t(LUNES, 9, 0), PASO)).isEqualTo(t(LUNES, 9, 0));
        assertThat(BuscadorHuecos.redondearArriba(t(LUNES, 23, 50), PASO)).isEqualTo(t(LUNES.plusDays(1), 0, 0));
    }
}
