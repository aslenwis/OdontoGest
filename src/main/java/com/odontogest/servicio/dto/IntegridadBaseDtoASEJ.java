package com.odontogest.servicio.dto;

import java.util.List;

public record IntegridadBaseDtoASEJ(List<String> problemasASEJ, int clavesForaneasASEJ, long tamanioBytesASEJ) {

    public IntegridadBaseDtoASEJ {
        problemasASEJ = List.copyOf(problemasASEJ);
    }

    public boolean correctaASEJ() {
        return problemasASEJ.isEmpty() && clavesForaneasASEJ == 0;
    }
}
