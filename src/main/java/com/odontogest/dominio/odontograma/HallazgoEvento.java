/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.odontograma;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Objects;

/**
 * Hallazgo registrado en un odontograma: un evento inmutable. El estado de la boca se obtiene
 * proyectando la secuencia de eventos ({@link ProyectorOdontograma}).
 *
 * @param piezaFin pieza final; solo para {@link Representacion#ENTRE_PIEZAS} y {@link Representacion#RANGO_PIEZAS}
 * @param estado   {@code null} si el tipo no usa estado
 * @param anulado  tiene una anulación registrada; sigue en el historial pero no cuenta en el estado
 */
public record HallazgoEvento(
        long id, long odontogramaId, TipoHallazgoDef tipo,
        PiezaDental pieza, PiezaDental piezaFin,
        EnumSet<Superficie> superficies,
        EstadoHallazgo estado,
        String sigla, String nota,
        LocalDateTime registradoEn, long registradoPor,
        boolean anulado) {

    public HallazgoEvento {
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(pieza, "pieza");
        Objects.requireNonNull(registradoEn, "registradoEn");
        superficies = superficies == null ? EnumSet.noneOf(Superficie.class) : EnumSet.copyOf(superficies);
        switch (tipo.representacion()) {
            case SUPERFICIE -> {
                if (superficies.isEmpty()) {
                    throw new IllegalArgumentException("El hallazgo " + tipo.codigo() + " requiere al menos una superficie");
                }
            }
            case ENTRE_PIEZAS, RANGO_PIEZAS -> Objects.requireNonNull(piezaFin,
                    "El hallazgo " + tipo.codigo() + " requiere pieza final");
            case PIEZA -> {
                // sin validación adicional
            }
        }
    }

    /** Copia defensiva: el record es inmutable. */
    @Override
    public EnumSet<Superficie> superficies() {
        return EnumSet.copyOf(superficies);
    }

    public ColorHallazgo color() {
        return switch (tipo.reglaColor()) {
            case SIEMPRE_ROJO -> ColorHallazgo.ROJO;
            case SIEMPRE_AZUL -> ColorHallazgo.AZUL;
            case POR_ESTADO -> estado == EstadoHallazgo.MALO ? ColorHallazgo.ROJO : ColorHallazgo.AZUL;
        };
    }

    public boolean esDeSuperficie() {
        return tipo.representacion() == Representacion.SUPERFICIE;
    }

    public boolean esDeRango() {
        return tipo.representacion() == Representacion.ENTRE_PIEZAS
                || tipo.representacion() == Representacion.RANGO_PIEZAS;
    }
}
