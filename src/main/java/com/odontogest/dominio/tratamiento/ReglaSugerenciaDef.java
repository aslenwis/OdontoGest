/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.tratamiento;

import com.odontogest.dominio.odontograma.EstadoHallazgo;
import com.odontogest.dominio.odontograma.HallazgoEvento;
import com.odontogest.dominio.odontograma.TipoRadicular;

/**
 * Regla configurable hallazgo → procedimiento (tabla {@code regla_sugerencia}).
 *
 * @param hallazgoTipoId {@code null} = regla general (un ítem para toda la boca)
 * @param estado         {@code null} = cualquier estado
 * @param tipoRadicular  {@code null} = cualquier pieza
 * @param prioridad      menor = se evalúa primero
 */
public record ReglaSugerenciaDef(
        long id, Long hallazgoTipoId, EstadoHallazgo estado, TipoRadicular tipoRadicular,
        int minSuperficies, int maxSuperficies, long procedimientoId, int fase, int prioridad) {

    public ReglaSugerenciaDef {
        if (minSuperficies > maxSuperficies) {
            throw new IllegalArgumentException("Rango de superficies inválido en la regla " + id);
        }
        if (fase < 1 || fase > 4) {
            throw new IllegalArgumentException("Fase fuera de rango (1–4) en la regla " + id);
        }
    }

    public boolean esGeneral() {
        return hallazgoTipoId == null;
    }

    /** La regla aplica al hallazgo: mismo tipo, estado, tipo radicular y número de superficies. */
    public boolean aplicaA(HallazgoEvento h) {
        int superficies = h.superficies().size();
        return hallazgoTipoId != null && hallazgoTipoId == h.tipo().id()
                && (estado == null || estado == h.estado())
                && (tipoRadicular == null || tipoRadicular == h.pieza().tipoRadicular())
                && minSuperficies <= superficies && superficies <= maxSuperficies;
    }
}
