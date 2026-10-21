/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import com.odontogest.dominio.odontograma.EstadoHallazgo;
import com.odontogest.dominio.odontograma.Superficie;

import java.util.Set;

/**
 * Orden de registrar un hallazgo en el odontograma en borrador.
 *
 * @param piezaFdiFin solo para hallazgos entre piezas o de rango
 * @param estado      obligatorio si el tipo usa la regla de color por estado
 * @param sigla       obligatoria si el tipo tiene opciones de sigla
 */
public record NuevoHallazgo(long odontogramaId, long hallazgoTipoId, int piezaFdi, Integer piezaFdiFin,
                            Set<Superficie> superficies, EstadoHallazgo estado, String sigla, String nota) {

    public NuevoHallazgo {
        superficies = superficies == null ? Set.of() : Set.copyOf(superficies);
    }
}
