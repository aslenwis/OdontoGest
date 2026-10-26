package com.odontogest.dominio.odontograma;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

public final class EstadoOdontogramaASEJ {

    private static final EstadoOdontogramaASEJ VACIOASEJ = new EstadoOdontogramaASEJ(Map.of(), Map.of(), List.of());

    private final Map<ClaveCapaASEJ, HallazgoEventoASEJ> vigentesASEJ;
    private final Map<Integer, EstadoPiezaASEJ> piezasASEJ;
    private final List<HallazgoEventoASEJ> rangosASEJ;

    private EstadoOdontogramaASEJ(Map<ClaveCapaASEJ, HallazgoEventoASEJ> vigentesASEJ, Map<Integer, EstadoPiezaASEJ> piezasASEJ,
                              List<HallazgoEventoASEJ> rangosASEJ) {
        this.vigentesASEJ = vigentesASEJ;
        this.piezasASEJ = piezasASEJ;
        this.rangosASEJ = rangosASEJ;
    }

    public static EstadoOdontogramaASEJ vacioASEJ() {
        return VACIOASEJ;
    }

    public static EstadoOdontogramaASEJ desdeASEJ(Map<ClaveCapaASEJ, HallazgoEventoASEJ> vigentesASEJ) {
        Map<PiezaDentalASEJ, Set<HallazgoEventoASEJ>> porPiezaASEJ = new TreeMap<>();
        Set<HallazgoEventoASEJ> rangosASEJ = new LinkedHashSet<>();
        vigentesASEJ.forEach((claveASEJ, hASEJ) -> {
            if (hASEJ.esDeRangoASEJ()) {
                rangosASEJ.add(hASEJ);
            } else {
                porPiezaASEJ.computeIfAbsent(hASEJ.piezaASEJ(), pASEJ -> new LinkedHashSet<>()).add(hASEJ);
            }
        });

        Comparator<HallazgoEventoASEJ> cronologicoASEJ = Comparator.comparing(HallazgoEventoASEJ::registradoEnASEJ)
                .thenComparingLong(HallazgoEventoASEJ::idASEJ);
        Map<Integer, EstadoPiezaASEJ> piezasASEJ = new LinkedHashMap<>();
        porPiezaASEJ.forEach((piezaASEJ, hallazgosASEJ) -> {
            List<HallazgoEventoASEJ> listaASEJ = new ArrayList<>(hallazgosASEJ);
            listaASEJ.sort(cronologicoASEJ);
            boolean ocultasASEJ = listaASEJ.stream().anyMatch(hASEJ -> hASEJ.tipoASEJ().ocultaSuperficiesASEJ());
            piezasASEJ.put(piezaASEJ.fdiASEJ(), new EstadoPiezaASEJ(piezaASEJ, listaASEJ, ocultasASEJ));
        });
        List<HallazgoEventoASEJ> listaRangosASEJ = new ArrayList<>(rangosASEJ);
        listaRangosASEJ.sort(cronologicoASEJ);

        return new EstadoOdontogramaASEJ(
                Collections.unmodifiableMap(new LinkedHashMap<>(vigentesASEJ)),
                Collections.unmodifiableMap(piezasASEJ),
                List.copyOf(listaRangosASEJ));
    }

    public Map<Integer, EstadoPiezaASEJ> piezasASEJ() {
        return piezasASEJ;
    }

    public Optional<EstadoPiezaASEJ> piezaASEJ(int fdiASEJ) {
        return Optional.ofNullable(piezasASEJ.get(fdiASEJ));
    }

    public List<HallazgoEventoASEJ> rangosASEJ() {
        return rangosASEJ;
    }

    public Set<ClaveCapaASEJ> clavesASEJ() {
        return vigentesASEJ.keySet();
    }

    public Map<ClaveCapaASEJ, HallazgoEventoASEJ> vigentesASEJ() {
        return vigentesASEJ;
    }

    public List<HallazgoEventoASEJ> rangosQueCubrenASEJ(int fdiASEJ) {
        PiezaDentalASEJ piezaASEJ = PiezaDentalASEJ.deASEJ(fdiASEJ);
        return rangosASEJ.stream()
                .filter(rASEJ -> PiezaDentalASEJ.rangoASEJ(rASEJ.piezaASEJ(), rASEJ.piezaFinASEJ()).contains(piezaASEJ))
                .toList();
    }

    public boolean superficiesOcultasASEJ(int fdiASEJ) {
        return piezaASEJ(fdiASEJ).map(EstadoPiezaASEJ::superficiesOcultasASEJ).orElse(false)
                || rangosQueCubrenASEJ(fdiASEJ).stream().anyMatch(rASEJ -> rASEJ.tipoASEJ().ocultaSuperficiesASEJ());
    }

    public List<HallazgoEventoASEJ> hallazgosVisiblesASEJ(int fdiASEJ) {
        boolean ocultasASEJ = superficiesOcultasASEJ(fdiASEJ);
        List<HallazgoEventoASEJ> visiblesASEJ = new ArrayList<>();
        piezaASEJ(fdiASEJ).ifPresent(pASEJ -> pASEJ.hallazgosASEJ().stream()
                .filter(hASEJ -> !(ocultasASEJ && hASEJ.esDeSuperficieASEJ()))
                .forEach(visiblesASEJ::add));
        visiblesASEJ.addAll(rangosQueCubrenASEJ(fdiASEJ));
        return List.copyOf(visiblesASEJ);
    }

    public Optional<HallazgoEventoASEJ> enSuperficieASEJ(int fdiASEJ, SuperficieASEJ superficieASEJ, String capaASEJ) {
        return Optional.ofNullable(vigentesASEJ.get(new ClaveCapaASEJ(fdiASEJ, fdiASEJ, superficieASEJ, capaASEJ)));
    }

    public List<HallazgoEventoASEJ> hallazgosVigentesASEJ() {
        return List.copyOf(new LinkedHashSet<>(vigentesASEJ.values()));
    }

    public boolean estaVacioASEJ() {
        return vigentesASEJ.isEmpty();
    }
}
