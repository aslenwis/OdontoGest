/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.agenda;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Intervalo de tiempo semiabierto {@code [inicio, fin)}: una cita que termina a las 10:00 no se solapa con
 * otra que empieza a las 10:00.
 */
public record Intervalo(LocalDateTime inicio, LocalDateTime fin) {

    public Intervalo {
        Objects.requireNonNull(inicio, "inicio");
        Objects.requireNonNull(fin, "fin");
        if (!fin.isAfter(inicio)) {
            throw new IllegalArgumentException("El fin debe ser posterior al inicio: " + inicio + " / " + fin);
        }
    }

    public static Intervalo de(LocalDateTime inicio, Duration duracion) {
        return new Intervalo(inicio, inicio.plus(duracion));
    }

    /** {@code a.inicio < b.fin ∧ b.inicio < a.fin}. */
    public boolean solapa(Intervalo otro) {
        return inicio.isBefore(otro.fin) && otro.inicio.isBefore(fin);
    }

    public boolean contiene(Intervalo otro) {
        return !otro.inicio.isBefore(inicio) && !otro.fin.isAfter(fin);
    }

    public Duration duracion() {
        return Duration.between(inicio, fin);
    }
}
