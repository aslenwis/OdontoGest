package com.odontogest.dominio.odontograma;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Objects;

public record HallazgoEventoASEJ(
        long idASEJ, long odontogramaIdASEJ, TipoHallazgoDefASEJ tipoASEJ,
        PiezaDentalASEJ piezaASEJ, PiezaDentalASEJ piezaFinASEJ,
        EnumSet<SuperficieASEJ> superficiesASEJ,
        EstadoHallazgoASEJ estadoASEJ,
        String siglaASEJ, String notaASEJ,
        LocalDateTime registradoEnASEJ, long registradoPorASEJ,
        boolean anuladoASEJ) {

    public HallazgoEventoASEJ {
        Objects.requireNonNull(tipoASEJ, "tipo");
        Objects.requireNonNull(piezaASEJ, "pieza");
        Objects.requireNonNull(registradoEnASEJ, "registradoEn");
        superficiesASEJ = superficiesASEJ == null ? EnumSet.noneOf(SuperficieASEJ.class) : EnumSet.copyOf(superficiesASEJ);
        switch (tipoASEJ.representacionASEJ()) {
            case SUPERFICIE -> {
                if (superficiesASEJ.isEmpty()) {
                    throw new IllegalArgumentException("El hallazgo " + tipoASEJ.codigoASEJ() + " requiere al menos una superficie");
                }
            }
            case ENTRE_PIEZAS, RANGO_PIEZAS -> Objects.requireNonNull(piezaFinASEJ,
                    "El hallazgo " + tipoASEJ.codigoASEJ() + " requiere pieza final");
            case PIEZA -> {
            }
        }
    }

    @Override
    public EnumSet<SuperficieASEJ> superficiesASEJ() {
        return EnumSet.copyOf(superficiesASEJ);
    }

    public ColorHallazgoASEJ colorASEJ() {
        return switch (tipoASEJ.reglaColorASEJ()) {
            case SIEMPRE_ROJO -> ColorHallazgoASEJ.ROJO;
            case SIEMPRE_AZUL -> ColorHallazgoASEJ.AZUL;
            case POR_ESTADO -> estadoASEJ == EstadoHallazgoASEJ.MALO ? ColorHallazgoASEJ.ROJO : ColorHallazgoASEJ.AZUL;
        };
    }

    public boolean esDeSuperficieASEJ() {
        return tipoASEJ.representacionASEJ() == RepresentacionASEJ.SUPERFICIE;
    }

    public boolean esDeRangoASEJ() {
        return tipoASEJ.representacionASEJ() == RepresentacionASEJ.ENTRE_PIEZAS
                || tipoASEJ.representacionASEJ() == RepresentacionASEJ.RANGO_PIEZAS;
    }
}
