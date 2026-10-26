package com.odontogest.dominio.tratamiento;

import com.odontogest.dominio.odontograma.EstadoOdontogramaASEJ;
import com.odontogest.dominio.odontograma.HallazgoEventoASEJ;
import com.odontogest.dominio.odontograma.PiezaDentalASEJ;
import com.odontogest.dominio.odontograma.SuperficieASEJ;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class MotorSugerenciasASEJ {

    public List<ItemSugeridoASEJ> sugerirASEJ(EstadoOdontogramaASEJ estadoASEJ, List<ReglaSugerenciaDefASEJ> reglasASEJ,
                                      Map<Long, ProcedimientoDefASEJ> tarifarioASEJ) {
        List<ReglaSugerenciaDefASEJ> ordenadasASEJ = reglasASEJ.stream()
                .sorted(Comparator.comparingInt(ReglaSugerenciaDefASEJ::prioridadASEJ).thenComparingLong(ReglaSugerenciaDefASEJ::idASEJ))
                .toList();

        List<ItemSugeridoASEJ> itemsASEJ = new ArrayList<>();
        for (ReglaSugerenciaDefASEJ reglaASEJ : ordenadasASEJ) {
            if (reglaASEJ.esGeneralASEJ()) {
                itemsASEJ.add(itemASEJ(reglaASEJ, procedimientoASEJ(tarifarioASEJ, reglaASEJ), null));
            }
        }
        for (HallazgoEventoASEJ hASEJ : hallazgosConsideradosASEJ(estadoASEJ)) {
            Optional<ReglaSugerenciaDefASEJ> reglaASEJ = ordenadasASEJ.stream().filter(rASEJ -> rASEJ.aplicaAASEJ(hASEJ)).findFirst();
            reglaASEJ.ifPresent(rASEJ -> itemsASEJ.add(itemASEJ(rASEJ, procedimientoASEJ(tarifarioASEJ, rASEJ), hASEJ)));
        }

        Set<String> vistosASEJ = new HashSet<>();
        List<ItemSugeridoASEJ> unicosASEJ = new ArrayList<>();
        for (ItemSugeridoASEJ iASEJ : itemsASEJ) {
            if (vistosASEJ.add(iASEJ.procedimientoIdASEJ() + "@" + iASEJ.piezaFdiASEJ())) {
                unicosASEJ.add(iASEJ);
            }
        }
        unicosASEJ.sort(Comparator.comparingInt(ItemSugeridoASEJ::faseASEJ)
                .thenComparingInt(iASEJ -> iASEJ.piezaFdiASEJ() == null ? -1 : PiezaDentalASEJ.deASEJ(iASEJ.piezaFdiASEJ()).ordenDibujoASEJ()));
        return List.copyOf(unicosASEJ);
    }

    private static List<HallazgoEventoASEJ> hallazgosConsideradosASEJ(EstadoOdontogramaASEJ estadoASEJ) {
        Set<HallazgoEventoASEJ> hallazgosASEJ = new LinkedHashSet<>();
        for (int fdiASEJ : estadoASEJ.piezasASEJ().keySet()) {
            hallazgosASEJ.addAll(estadoASEJ.hallazgosVisiblesASEJ(fdiASEJ));
        }
        hallazgosASEJ.addAll(estadoASEJ.rangosASEJ());
        List<HallazgoEventoASEJ> listaASEJ = new ArrayList<>(hallazgosASEJ);
        listaASEJ.sort(Comparator.comparing(HallazgoEventoASEJ::registradoEnASEJ).thenComparingLong(HallazgoEventoASEJ::idASEJ));
        return listaASEJ;
    }

    private static ProcedimientoDefASEJ procedimientoASEJ(Map<Long, ProcedimientoDefASEJ> tarifarioASEJ, ReglaSugerenciaDefASEJ reglaASEJ) {
        return Objects.requireNonNull(tarifarioASEJ.get(reglaASEJ.procedimientoIdASEJ()),
                () -> "La regla " + reglaASEJ.idASEJ() + " apunta a un procedimiento inexistente: " + reglaASEJ.procedimientoIdASEJ());
    }

    private static ItemSugeridoASEJ itemASEJ(ReglaSugerenciaDefASEJ reglaASEJ, ProcedimientoDefASEJ procASEJ, HallazgoEventoASEJ hASEJ) {
        Integer piezaASEJ = hASEJ == null ? null : hASEJ.piezaASEJ().fdiASEJ();
        Set<SuperficieASEJ> superficiesASEJ = hASEJ == null ? Set.of() : hASEJ.superficiesASEJ();
        int cantidadASEJ = procASEJ.formaCobroASEJ() == FormaCobroASEJ.SUPERFICIE ? Math.max(1, superficiesASEJ.size()) : 1;
        return new ItemSugeridoASEJ(procASEJ.idASEJ(), piezaASEJ, superficiesASEJ, reglaASEJ.faseASEJ(), cantidadASEJ, procASEJ.precioCentASEJ(),
                hASEJ == null ? null : hASEJ.idASEJ());
    }
}
