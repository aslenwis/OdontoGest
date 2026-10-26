package com.odontogest.servicio.dto;

import com.odontogest.dominio.odontograma.ColorHallazgoASEJ;
import com.odontogest.dominio.odontograma.EstadoHallazgoASEJ;
import com.odontogest.dominio.odontograma.RepresentacionASEJ;
import com.odontogest.dominio.odontograma.SuperficieASEJ;

import java.time.LocalDateTime;
import java.util.Set;

public record HallazgoDtoASEJ(long idASEJ, long odontogramaIdASEJ, long tipoIdASEJ, String codigoTipoASEJ, String nombreTipoASEJ,
                          RepresentacionASEJ representacionASEJ, int piezaASEJ, Integer piezaFinASEJ, Set<SuperficieASEJ> superficiesASEJ,
                          EstadoHallazgoASEJ estadoASEJ, String siglaASEJ, String notaASEJ, LocalDateTime registradoEnASEJ,
                          String registradoPorASEJ, ColorHallazgoASEJ colorASEJ, boolean anuladoASEJ, String motivoAnulacionASEJ,
                          LocalDateTime anuladoEnASEJ, boolean vigenteASEJ, boolean eliminableASEJ) {

    public HallazgoDtoASEJ {
        superficiesASEJ = Set.copyOf(superficiesASEJ);
    }
}
