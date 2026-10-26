package com.odontogest.servicio.dto;

import java.util.List;

public record PaginaASEJ<T>(List<T> contenidoASEJ, int numeroASEJ, int tamanioASEJ, long totalASEJ) {

    public PaginaASEJ {
        contenidoASEJ = List.copyOf(contenidoASEJ);
    }

    public int totalPaginasASEJ() {
        return tamanioASEJ <= 0 ? 1 : (int) Math.max(1, (totalASEJ + tamanioASEJ - 1) / tamanioASEJ);
    }
}
