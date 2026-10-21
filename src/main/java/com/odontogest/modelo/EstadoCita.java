/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.modelo;

import java.util.Set;

/**
 * Estados de una cita (RF-AGE-02):
 * <pre>
 * PROGRAMADA → CONFIRMADA, CANCELADA, NO_ASISTIO, ATENDIDA
 * CONFIRMADA → PROGRAMADA, CANCELADA, NO_ASISTIO, ATENDIDA
 * ATENDIDA, CANCELADA y NO_ASISTIO son finales.
 * </pre>
 * ATENDIDA la fija el sistema al cerrar la atención abierta desde la cita.
 */
public enum EstadoCita {
    PROGRAMADA("Programada"),
    CONFIRMADA("Confirmada"),
    ATENDIDA("Atendida"),
    CANCELADA("Cancelada"),
    NO_ASISTIO("No asistió");

    private final String etiqueta;

    EstadoCita(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String etiqueta() {
        return etiqueta;
    }

    /** La cita sigue pendiente: ocupa el horario y se puede mover, confirmar o cancelar. */
    public boolean activa() {
        return this == PROGRAMADA || this == CONFIRMADA;
    }

    /** Estados que ocupan el horario (para solapamientos y huecos). */
    public static Set<EstadoCita> ocupan() {
        return Set.of(PROGRAMADA, CONFIRMADA, ATENDIDA);
    }

    public boolean puedePasarA(EstadoCita destino) {
        return switch (this) {
            case PROGRAMADA -> destino != PROGRAMADA;
            case CONFIRMADA -> destino != CONFIRMADA;
            case ATENDIDA, CANCELADA, NO_ASISTIO -> false;
        };
    }
}
