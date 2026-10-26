package com.odontogest.dominio.odontograma;

import java.util.List;
import java.util.function.Function;

public final class CalculadoraIndicesASEJ {

    public ResultadoIndiceASEJ cpodASEJ(EstadoOdontogramaASEJ estadoASEJ, boolean excluirTercerasMolaresASEJ) {
        List<PiezaDentalASEJ> universoASEJ = PiezaDentalASEJ.permanentesASEJ().stream()
                .filter(pASEJ -> !(excluirTercerasMolaresASEJ && pASEJ.esTerceraMolarASEJ()))
                .toList();
        return calcularASEJ(estadoASEJ, universoASEJ, TipoHallazgoDefASEJ::conteoCpodASEJ, ConteoIndiceASEJ.P);
    }

    public ResultadoIndiceASEJ ceodASEJ(EstadoOdontogramaASEJ estadoASEJ) {
        return calcularASEJ(estadoASEJ, PiezaDentalASEJ.deciduasASEJ(), TipoHallazgoDefASEJ::conteoCeodASEJ, ConteoIndiceASEJ.E);
    }

    private ResultadoIndiceASEJ calcularASEJ(EstadoOdontogramaASEJ estadoASEJ, List<PiezaDentalASEJ> universoASEJ,
                                     Function<TipoHallazgoDefASEJ, ConteoIndiceASEJ> conteoASEJ, ConteoIndiceASEJ perdidaASEJ) {
        int cASEJ = 0;
        int pASEJ = 0;
        int oASEJ = 0;
        for (PiezaDentalASEJ piezaASEJ : universoASEJ) {
            List<HallazgoEventoASEJ> hASEJ = estadoASEJ.hallazgosVisiblesASEJ(piezaASEJ.fdiASEJ());
            if (hASEJ.stream().anyMatch(xASEJ -> conteoASEJ.apply(xASEJ.tipoASEJ()) == ConteoIndiceASEJ.C)) {
                cASEJ++;
            } else if (hASEJ.stream().anyMatch(xASEJ -> conteoASEJ.apply(xASEJ.tipoASEJ()) == ConteoIndiceASEJ.O && xASEJ.colorASEJ() == ColorHallazgoASEJ.ROJO)) {
                cASEJ++;
            } else if (hASEJ.stream().anyMatch(xASEJ -> conteoASEJ.apply(xASEJ.tipoASEJ()) == perdidaASEJ)) {
                pASEJ++;
            } else if (hASEJ.stream().anyMatch(xASEJ -> conteoASEJ.apply(xASEJ.tipoASEJ()) == ConteoIndiceASEJ.O)) {
                oASEJ++;
            }
        }
        int totalASEJ = cASEJ + pASEJ + oASEJ;
        return new ResultadoIndiceASEJ(cASEJ, pASEJ, oASEJ, totalASEJ, universoASEJ.size(), NivelSeveridadOmsASEJ.deASEJ(totalASEJ));
    }
}
