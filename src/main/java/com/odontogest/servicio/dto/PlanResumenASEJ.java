package com.odontogest.servicio.dto;

import com.odontogest.modelo.EstadoPlanASEJ;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record PlanResumenASEJ(long idASEJ, LocalDateTime fechaASEJ, EstadoPlanASEJ estadoASEJ, BigDecimal totalASEJ, String odontologoNombreASEJ) {

    private static final DateTimeFormatter FECHAASEJ = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public String etiquetaASEJ() {
        return "Plan N° " + idASEJ + " · " + FECHAASEJ.format(fechaASEJ) + " · " + estadoASEJ.etiquetaASEJ();
    }

    @Override
    public String toString() {
        return etiquetaASEJ();
    }
}
