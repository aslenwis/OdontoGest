package com.odontogest.servicio.dto;

import com.odontogest.dominio.odontograma.EstadoHallazgoASEJ;
import com.odontogest.dominio.odontograma.SuperficieASEJ;

import java.util.Set;

public record NuevoHallazgoASEJ(long odontogramaIdASEJ, long hallazgoTipoIdASEJ, int piezaFdiASEJ, Integer piezaFdiFinASEJ,
                            Set<SuperficieASEJ> superficiesASEJ, EstadoHallazgoASEJ estadoASEJ, String siglaASEJ, String notaASEJ) {

    public NuevoHallazgoASEJ {
        superficiesASEJ = superficiesASEJ == null ? Set.of() : Set.copyOf(superficiesASEJ);
    }
}
