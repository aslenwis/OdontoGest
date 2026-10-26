package com.odontogest.servicio.dto;

import java.time.LocalDate;
import java.util.List;

public record AgendaDtoASEJ(LocalDate desdeASEJ, int diasASEJ, OdontologoDtoASEJ odontologoASEJ, List<CitaDtoASEJ> citasASEJ, int intervaloMinASEJ) {

    public AgendaDtoASEJ {
        citasASEJ = List.copyOf(citasASEJ);
    }
}
