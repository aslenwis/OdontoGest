package com.odontogest.dominio.odontograma;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class PromedioIndicesASEJ {

    public record GrupoASEJ(GrupoEtarioASEJ grupoASEJ, int pacientesASEJ, double cpodASEJ, double cASEJ, double pASEJ, double oASEJ, double ceodASEJ,
                        NivelSeveridadOmsASEJ nivelCpodASEJ) {
    }

    private static final class SumaASEJ {
        int pacientesASEJ;
        long cpodASEJ;
        long cASEJ;
        long pASEJ;
        long oASEJ;
        long ceodASEJ;
    }

    private final Map<GrupoEtarioASEJ, SumaASEJ> sumasASEJ = new EnumMap<>(GrupoEtarioASEJ.class);

    public void agregarASEJ(int edadASEJ, ResultadoIndiceASEJ cpodASEJ, ResultadoIndiceASEJ ceodASEJ) {
        Objects.requireNonNull(cpodASEJ, "cpod");
        Objects.requireNonNull(ceodASEJ, "ceod");
        SumaASEJ sASEJ = sumasASEJ.computeIfAbsent(GrupoEtarioASEJ.deASEJ(edadASEJ), gASEJ -> new SumaASEJ());
        sASEJ.pacientesASEJ++;
        sASEJ.cpodASEJ += cpodASEJ.totalASEJ();
        sASEJ.cASEJ += cpodASEJ.cASEJ();
        sASEJ.pASEJ += cpodASEJ.pASEJ();
        sASEJ.oASEJ += cpodASEJ.oASEJ();
        sASEJ.ceodASEJ += ceodASEJ.totalASEJ();
    }

    public int pacientesASEJ() {
        return sumasASEJ.values().stream().mapToInt(sASEJ -> sASEJ.pacientesASEJ).sum();
    }

    public List<GrupoASEJ> resultadoASEJ() {
        List<GrupoASEJ> gruposASEJ = new ArrayList<>();
        for (GrupoEtarioASEJ gASEJ : GrupoEtarioASEJ.values()) {
            SumaASEJ sASEJ = sumasASEJ.get(gASEJ);
            if (sASEJ == null) {
                gruposASEJ.add(new GrupoASEJ(gASEJ, 0, 0, 0, 0, 0, 0, null));
                continue;
            }
            double cpodASEJ = promedioASEJ(sASEJ.cpodASEJ, sASEJ.pacientesASEJ);
            gruposASEJ.add(new GrupoASEJ(gASEJ, sASEJ.pacientesASEJ, cpodASEJ, promedioASEJ(sASEJ.cASEJ, sASEJ.pacientesASEJ), promedioASEJ(sASEJ.pASEJ, sASEJ.pacientesASEJ),
                    promedioASEJ(sASEJ.oASEJ, sASEJ.pacientesASEJ), promedioASEJ(sASEJ.ceodASEJ, sASEJ.pacientesASEJ), NivelSeveridadOmsASEJ.deASEJ(cpodASEJ)));
        }
        return gruposASEJ;
    }

    private static double promedioASEJ(long sumaASEJ, int nASEJ) {
        return Math.round(sumaASEJ * 100.0 / nASEJ) / 100.0;
    }
}
