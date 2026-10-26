package com.odontogest.dominio.odontograma;

import java.util.List;

public record EstadoPiezaASEJ(PiezaDentalASEJ piezaASEJ, List<HallazgoEventoASEJ> hallazgosASEJ, boolean superficiesOcultasASEJ) {

    public EstadoPiezaASEJ {
        hallazgosASEJ = List.copyOf(hallazgosASEJ);
    }

    public List<HallazgoEventoASEJ> hallazgosVisiblesASEJ() {
        if (!superficiesOcultasASEJ) {
            return hallazgosASEJ;
        }
        return hallazgosASEJ.stream().filter(hASEJ -> !hASEJ.esDeSuperficieASEJ()).toList();
    }

    public List<HallazgoEventoASEJ> enSuperficieASEJ(SuperficieASEJ superficieASEJ) {
        return hallazgosVisiblesASEJ().stream()
                .filter(hASEJ -> hASEJ.esDeSuperficieASEJ() && hASEJ.superficiesASEJ().contains(superficieASEJ))
                .toList();
    }
}
