package com.odontogest.servicio.dto;

public record ItemPlanPendienteDtoASEJ(long itemIdASEJ, long planIdASEJ, String descripcionASEJ) {

    @Override
    public String toString() {
        return "Plan N° " + planIdASEJ + " · " + descripcionASEJ;
    }
}
