package com.odontogest.servicio.dto;

import com.odontogest.dominio.odontograma.SuperficieASEJ;

import java.math.BigDecimal;
import java.util.Set;

public record PlanItemFormASEJ(Long idASEJ, long planIdASEJ, Long procedimientoIdASEJ, Integer piezaFdiASEJ, Set<SuperficieASEJ> superficiesASEJ,
                           int faseASEJ, int cantidadASEJ, BigDecimal precioUnitASEJ, BigDecimal descuentoASEJ) {

    public PlanItemFormASEJ {
        superficiesASEJ = superficiesASEJ == null ? Set.of() : Set.copyOf(superficiesASEJ);
    }
}
