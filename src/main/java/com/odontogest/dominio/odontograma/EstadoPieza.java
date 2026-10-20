/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.odontograma;

import java.util.List;

/**
 * Hallazgos vigentes de una pieza (de superficie y de pieza; los de rango están en
 * {@link EstadoOdontograma#rangos()}).
 *
 * @param hallazgos          vigentes sin repetir, en orden de registro
 * @param superficiesOcultas la pieza tiene vigente un hallazgo que oculta superficies (p. ej. ausente)
 */
public record EstadoPieza(PiezaDental pieza, List<HallazgoEvento> hallazgos, boolean superficiesOcultas) {

    public EstadoPieza {
        hallazgos = List.copyOf(hallazgos);
    }

    /**
     * Hallazgos que se dibujan y cuentan en índices y sugerencias: si la pieza oculta sus superficies,
     * se omiten los hallazgos de superficie anteriores (siguen en el historial de eventos).
     */
    public List<HallazgoEvento> hallazgosVisibles() {
        if (!superficiesOcultas) {
            return hallazgos;
        }
        return hallazgos.stream().filter(h -> !h.esDeSuperficie()).toList();
    }

    /** Hallazgos visibles que cubren la superficie indicada. */
    public List<HallazgoEvento> enSuperficie(Superficie superficie) {
        return hallazgosVisibles().stream()
                .filter(h -> h.esDeSuperficie() && h.superficies().contains(superficie))
                .toList();
    }
}
