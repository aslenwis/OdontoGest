package com.odontogest.dominio.odontograma;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ProyectorOdontogramaASEJ {

    public EstadoOdontogramaASEJ proyectarASEJ(List<HallazgoEventoASEJ> eventosASEJ, LocalDateTime hastaASEJ) {
        Map<ClaveCapaASEJ, HallazgoEventoASEJ> vigentesASEJ = new LinkedHashMap<>();
        eventosASEJ.stream()
                .filter(eASEJ -> !eASEJ.anuladoASEJ())
                .filter(eASEJ -> hastaASEJ == null || !eASEJ.registradoEnASEJ().isAfter(hastaASEJ))
                .sorted(Comparator.comparing(HallazgoEventoASEJ::registradoEnASEJ).thenComparingLong(HallazgoEventoASEJ::idASEJ))
                .forEach(eASEJ -> clavesASEJ(eASEJ).forEach(kASEJ -> vigentesASEJ.put(kASEJ, eASEJ)));
        return EstadoOdontogramaASEJ.desdeASEJ(vigentesASEJ);
    }

    static List<ClaveCapaASEJ> clavesASEJ(HallazgoEventoASEJ eASEJ) {
        String capaASEJ = eASEJ.tipoASEJ().grupoExclusionASEJ();
        return switch (eASEJ.tipoASEJ().representacionASEJ()) {
            case SUPERFICIE -> eASEJ.superficiesASEJ().stream()
                    .map(sASEJ -> new ClaveCapaASEJ(eASEJ.piezaASEJ().fdiASEJ(), eASEJ.piezaASEJ().fdiASEJ(), sASEJ, capaASEJ))
                    .toList();
            case PIEZA -> List.of(new ClaveCapaASEJ(eASEJ.piezaASEJ().fdiASEJ(), eASEJ.piezaASEJ().fdiASEJ(), null, capaASEJ));
            case ENTRE_PIEZAS, RANGO_PIEZAS -> {
                int aASEJ = Math.min(eASEJ.piezaASEJ().fdiASEJ(), eASEJ.piezaFinASEJ().fdiASEJ());
                int bASEJ = Math.max(eASEJ.piezaASEJ().fdiASEJ(), eASEJ.piezaFinASEJ().fdiASEJ());
                yield List.of(new ClaveCapaASEJ(aASEJ, bASEJ, null, capaASEJ));
            }
        };
    }
}
