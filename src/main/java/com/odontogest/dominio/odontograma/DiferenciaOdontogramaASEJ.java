package com.odontogest.dominio.odontograma;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public record DiferenciaOdontogramaASEJ(Set<ClaveCapaASEJ> agregadasASEJ, Set<ClaveCapaASEJ> eliminadasASEJ, Set<ClaveCapaASEJ> modificadasASEJ) {

    public DiferenciaOdontogramaASEJ {
        agregadasASEJ = Set.copyOf(agregadasASEJ);
        eliminadasASEJ = Set.copyOf(eliminadasASEJ);
        modificadasASEJ = Set.copyOf(modificadasASEJ);
    }

    public static DiferenciaOdontogramaASEJ entreASEJ(EstadoOdontogramaASEJ baseASEJ, EstadoOdontogramaASEJ objetivoASEJ) {
        Map<ClaveCapaASEJ, HallazgoEventoASEJ> bASEJ = baseASEJ.vigentesASEJ();
        Map<ClaveCapaASEJ, HallazgoEventoASEJ> oASEJ = objetivoASEJ.vigentesASEJ();
        Set<ClaveCapaASEJ> agregadasASEJ = new LinkedHashSet<>();
        Set<ClaveCapaASEJ> modificadasASEJ = new LinkedHashSet<>();
        oASEJ.forEach((claveASEJ, hallazgoASEJ) -> {
            HallazgoEventoASEJ anteriorASEJ = bASEJ.get(claveASEJ);
            if (anteriorASEJ == null) {
                agregadasASEJ.add(claveASEJ);
            } else if (anteriorASEJ.idASEJ() != hallazgoASEJ.idASEJ()) {
                modificadasASEJ.add(claveASEJ);
            }
        });
        Set<ClaveCapaASEJ> eliminadasASEJ = new LinkedHashSet<>(bASEJ.keySet());
        eliminadasASEJ.removeAll(oASEJ.keySet());
        return new DiferenciaOdontogramaASEJ(agregadasASEJ, eliminadasASEJ, modificadasASEJ);
    }

    public boolean sinCambiosASEJ() {
        return agregadasASEJ.isEmpty() && eliminadasASEJ.isEmpty() && modificadasASEJ.isEmpty();
    }

    public Set<Integer> piezasAfectadasASEJ() {
        Set<Integer> piezasASEJ = new TreeSet<>();
        for (Set<ClaveCapaASEJ> grupoASEJ : List.of(agregadasASEJ, eliminadasASEJ, modificadasASEJ)) {
            for (ClaveCapaASEJ cASEJ : grupoASEJ) {
                piezasASEJ.add(cASEJ.piezaInicioASEJ());
                piezasASEJ.add(cASEJ.piezaFinASEJ());
            }
        }
        return piezasASEJ;
    }
}
