package com.odontogest.servicio.dto;

import com.odontogest.dominio.odontograma.PiezaDentalASEJ;
import com.odontogest.dominio.odontograma.SuperficieASEJ;
import com.odontogest.dominio.tratamiento.FormaCobroASEJ;
import com.odontogest.modelo.EstadoPlanItemASEJ;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

public record PlanItemDtoASEJ(long idASEJ, long planIdASEJ, long procedimientoIdASEJ, String procedimientoCodigoASEJ, String procedimientoNombreASEJ,
                          FormaCobroASEJ formaCobroASEJ, Long hallazgoIdASEJ, Integer piezaASEJ, Set<SuperficieASEJ> superficiesASEJ, int faseASEJ,
                          int cantidadASEJ, BigDecimal precioUnitASEJ, BigDecimal descuentoASEJ, BigDecimal totalASEJ,
                          EstadoPlanItemASEJ estadoASEJ, Long atencionIdASEJ, LocalDateTime realizadoEnASEJ) {

    public PlanItemDtoASEJ {
        superficiesASEJ = Set.copyOf(superficiesASEJ);
    }

    public static String nombreFaseASEJ(int faseASEJ) {
        return switch (faseASEJ) {
            case 1 -> "Fase 1 · Urgente";
            case 2 -> "Fase 2 · Operatoria";
            case 3 -> "Fase 3 · Rehabilitación";
            case 4 -> "Fase 4 · Mantenimiento";
            default -> "Fase " + faseASEJ;
        };
    }

    public String superficiesTextoASEJ() {
        if (superficiesASEJ.isEmpty() || piezaASEJ == null) {
            return "";
        }
        PiezaDentalASEJ pASEJ = PiezaDentalASEJ.deASEJ(piezaASEJ);
        return superficiesASEJ.stream().sorted().map(sASEJ -> sASEJ.etiquetaASEJ(pASEJ)).collect(Collectors.joining());
    }

    public String descripcionASEJ() {
        StringBuilder sbASEJ = new StringBuilder(procedimientoNombreASEJ);
        if (piezaASEJ != null) {
            sbASEJ.append(" · pieza ").append(piezaASEJ);
            String supASEJ = superficiesTextoASEJ();
            if (!supASEJ.isEmpty()) {
                sbASEJ.append(' ').append(supASEJ);
            }
        }
        if (cantidadASEJ > 1 && formaCobroASEJ != FormaCobroASEJ.SUPERFICIE) {
            sbASEJ.append(" ×").append(cantidadASEJ);
        }
        return sbASEJ.toString();
    }

    public boolean realizadoASEJ() {
        return estadoASEJ == EstadoPlanItemASEJ.REALIZADO;
    }
}
