/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.agenda;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Bloque semanal del horario de atención de un odontólogo (fila de {@code horario_atencion}).
 */
public record BloqueHorario(DayOfWeek dia, LocalTime inicio, LocalTime fin) {

    public BloqueHorario {
        Objects.requireNonNull(dia, "dia");
        Objects.requireNonNull(inicio, "inicio");
        Objects.requireNonNull(fin, "fin");
        if (!fin.isAfter(inicio)) {
            throw new IllegalArgumentException("La hora de fin debe ser posterior a la de inicio");
        }
    }

    /** El bloque en una fecha concreta (que debe caer en {@link #dia()}). */
    public Intervalo en(LocalDate fecha) {
        if (fecha.getDayOfWeek() != dia) {
            throw new IllegalArgumentException(fecha + " no es " + dia);
        }
        return new Intervalo(fecha.atTime(inicio), fecha.atTime(fin));
    }

    /** La cita cabe completa en algún bloque del horario. */
    public static boolean dentroDelHorario(Intervalo cita, Iterable<BloqueHorario> horario) {
        LocalDate fecha = cita.inicio().toLocalDate();
        if (!cita.fin().toLocalDate().equals(fecha)) {
            return false;
        }
        for (BloqueHorario b : horario) {
            if (b.dia == fecha.getDayOfWeek() && b.en(fecha).contiene(cita)) {
                return true;
            }
        }
        return false;
    }
}
