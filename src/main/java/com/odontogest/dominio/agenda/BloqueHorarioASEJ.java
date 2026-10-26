package com.odontogest.dominio.agenda;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

public record BloqueHorarioASEJ(DayOfWeek diaASEJ, LocalTime inicioASEJ, LocalTime finASEJ) {

    public BloqueHorarioASEJ {
        Objects.requireNonNull(diaASEJ, "dia");
        Objects.requireNonNull(inicioASEJ, "inicio");
        Objects.requireNonNull(finASEJ, "fin");
        if (!finASEJ.isAfter(inicioASEJ)) {
            throw new IllegalArgumentException("La hora de fin debe ser posterior a la de inicio");
        }
    }

    public IntervaloASEJ enASEJ(LocalDate fechaASEJ) {
        if (fechaASEJ.getDayOfWeek() != diaASEJ) {
            throw new IllegalArgumentException(fechaASEJ + " no es " + diaASEJ);
        }
        return new IntervaloASEJ(fechaASEJ.atTime(inicioASEJ), fechaASEJ.atTime(finASEJ));
    }

    public static boolean dentroDelHorarioASEJ(IntervaloASEJ citaASEJ, Iterable<BloqueHorarioASEJ> horarioASEJ) {
        LocalDate fechaASEJ = citaASEJ.inicioASEJ().toLocalDate();
        if (!citaASEJ.finASEJ().toLocalDate().equals(fechaASEJ)) {
            return false;
        }
        for (BloqueHorarioASEJ bASEJ : horarioASEJ) {
            if (bASEJ.diaASEJ == fechaASEJ.getDayOfWeek() && bASEJ.enASEJ(fechaASEJ).contieneASEJ(citaASEJ)) {
                return true;
            }
        }
        return false;
    }
}
