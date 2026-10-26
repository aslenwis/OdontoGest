package com.odontogest.dominio.odontograma;

import java.util.ArrayList;
import java.util.List;

public record PiezaDentalASEJ(int fdiASEJ) implements Comparable<PiezaDentalASEJ> {

    private static final List<PiezaDentalASEJ> PERMANENTES_SUPERIORESASEJ = filaASEJ(1, 2, 8);
    private static final List<PiezaDentalASEJ> DECIDUAS_SUPERIORESASEJ = filaASEJ(5, 6, 5);
    private static final List<PiezaDentalASEJ> DECIDUAS_INFERIORESASEJ = filaASEJ(8, 7, 5);
    private static final List<PiezaDentalASEJ> PERMANENTES_INFERIORESASEJ = filaASEJ(4, 3, 8);

    private static final List<PiezaDentalASEJ> PERMANENTESASEJ = concatenarASEJ(PERMANENTES_SUPERIORESASEJ, PERMANENTES_INFERIORESASEJ);
    private static final List<PiezaDentalASEJ> DECIDUASASEJ = concatenarASEJ(DECIDUAS_SUPERIORESASEJ, DECIDUAS_INFERIORESASEJ);
    private static final List<PiezaDentalASEJ> TODASASEJ = concatenarASEJ(
            concatenarASEJ(PERMANENTES_SUPERIORESASEJ, DECIDUAS_SUPERIORESASEJ),
            concatenarASEJ(DECIDUAS_INFERIORESASEJ, PERMANENTES_INFERIORESASEJ));

    public PiezaDentalASEJ {
        if (!esValidaASEJ(fdiASEJ)) {
            throw new IllegalArgumentException("Pieza FDI inválida: " + fdiASEJ);
        }
    }

    public static PiezaDentalASEJ deASEJ(int fdiASEJ) {
        return new PiezaDentalASEJ(fdiASEJ);
    }

    public static boolean esValidaASEJ(int fdiASEJ) {
        int cuadranteASEJ = fdiASEJ / 10;
        int posicionASEJ = fdiASEJ % 10;
        if (cuadranteASEJ >= 1 && cuadranteASEJ <= 4) {
            return posicionASEJ >= 1 && posicionASEJ <= 8;
        }
        if (cuadranteASEJ >= 5 && cuadranteASEJ <= 8) {
            return posicionASEJ >= 1 && posicionASEJ <= 5;
        }
        return false;
    }

    public CuadranteASEJ cuadranteASEJ() {
        return CuadranteASEJ.deASEJ(fdiASEJ / 10);
    }

    public int posicionASEJ() {
        return fdiASEJ % 10;
    }

    public boolean esDeciduaASEJ() {
        return cuadranteASEJ().deciduoASEJ;
    }

    public ArcadaASEJ arcadaASEJ() {
        return cuadranteASEJ().arcadaASEJ;
    }

    public boolean ladoDerechoPacienteASEJ() {
        return cuadranteASEJ().ladoDerechoPacienteASEJ;
    }

    public boolean esAnteriorASEJ() {
        return posicionASEJ() <= 3;
    }

    public TipoPiezaASEJ tipoASEJ() {
        int pASEJ = posicionASEJ();
        if (pASEJ <= 2) {
            return TipoPiezaASEJ.INCISIVO;
        }
        if (pASEJ == 3) {
            return TipoPiezaASEJ.CANINO;
        }
        if (pASEJ <= 5) {
            return esDeciduaASEJ() ? TipoPiezaASEJ.MOLAR : TipoPiezaASEJ.PREMOLAR;
        }
        return TipoPiezaASEJ.MOLAR;
    }

    public TipoRadicularASEJ tipoRadicularASEJ() {
        if (tipoASEJ() == TipoPiezaASEJ.MOLAR) {
            return TipoRadicularASEJ.MULTIRRADICULAR;
        }
        if (fdiASEJ == 14 || fdiASEJ == 24) {
            return TipoRadicularASEJ.BIRRADICULAR;
        }
        return TipoRadicularASEJ.UNIRRADICULAR;
    }

    public boolean esTerceraMolarASEJ() {
        return posicionASEJ() == 8;
    }

    public int ordenDibujoASEJ() {
        return TODASASEJ.indexOf(this);
    }

    public boolean esContiguaAASEJ(PiezaDentalASEJ otraASEJ) {
        List<PiezaDentalASEJ> filaASEJ = filaDeASEJ(this);
        int iASEJ = filaASEJ.indexOf(this);
        int jASEJ = filaASEJ.indexOf(otraASEJ);
        return jASEJ >= 0 && Math.abs(iASEJ - jASEJ) == 1;
    }

    public boolean mismaArcadaASEJ(PiezaDentalASEJ otraASEJ) {
        return arcadaASEJ() == otraASEJ.arcadaASEJ();
    }

    public boolean mismaFilaASEJ(PiezaDentalASEJ otraASEJ) {
        return filaDeASEJ(this).contains(otraASEJ);
    }

    public static List<PiezaDentalASEJ> rangoASEJ(PiezaDentalASEJ aASEJ, PiezaDentalASEJ bASEJ) {
        List<PiezaDentalASEJ> filaASEJ = filaDeASEJ(aASEJ);
        int iASEJ = filaASEJ.indexOf(aASEJ);
        int jASEJ = filaASEJ.indexOf(bASEJ);
        if (jASEJ < 0) {
            return aASEJ.equals(bASEJ) ? List.of(aASEJ) : List.of(aASEJ, bASEJ);
        }
        return filaASEJ.subList(Math.min(iASEJ, jASEJ), Math.max(iASEJ, jASEJ) + 1);
    }

    public static List<PiezaDentalASEJ> permanentesASEJ() {
        return PERMANENTESASEJ;
    }

    public static List<PiezaDentalASEJ> deciduasASEJ() {
        return DECIDUASASEJ;
    }

    public static List<List<PiezaDentalASEJ>> filasASEJ(DenticionASEJ denticionASEJ) {
        return switch (denticionASEJ) {
            case PERMANENTE -> List.of(PERMANENTES_SUPERIORESASEJ, PERMANENTES_INFERIORESASEJ);
            case DECIDUA -> List.of(DECIDUAS_SUPERIORESASEJ, DECIDUAS_INFERIORESASEJ);
            case MIXTA -> List.of(PERMANENTES_SUPERIORESASEJ, DECIDUAS_SUPERIORESASEJ, DECIDUAS_INFERIORESASEJ,
                    PERMANENTES_INFERIORESASEJ);
        };
    }

    @Override
    public int compareTo(PiezaDentalASEJ otraASEJ) {
        return Integer.compare(ordenDibujoASEJ(), otraASEJ.ordenDibujoASEJ());
    }

    @Override
    public String toString() {
        return String.valueOf(fdiASEJ);
    }

    private static List<PiezaDentalASEJ> filaDeASEJ(PiezaDentalASEJ pASEJ) {
        return switch (pASEJ.cuadranteASEJ()) {
            case C1, C2 -> PERMANENTES_SUPERIORESASEJ;
            case C5, C6 -> DECIDUAS_SUPERIORESASEJ;
            case C8, C7 -> DECIDUAS_INFERIORESASEJ;
            case C4, C3 -> PERMANENTES_INFERIORESASEJ;
        };
    }

    private static List<PiezaDentalASEJ> filaASEJ(int cuadranteDerechoASEJ, int cuadranteIzquierdoASEJ, int piezasASEJ) {
        List<PiezaDentalASEJ> filaASEJ = new ArrayList<>();
        for (int pASEJ = piezasASEJ; pASEJ >= 1; pASEJ--) {
            filaASEJ.add(new PiezaDentalASEJ(cuadranteDerechoASEJ * 10 + pASEJ));
        }
        for (int pASEJ = 1; pASEJ <= piezasASEJ; pASEJ++) {
            filaASEJ.add(new PiezaDentalASEJ(cuadranteIzquierdoASEJ * 10 + pASEJ));
        }
        return List.copyOf(filaASEJ);
    }

    private static List<PiezaDentalASEJ> concatenarASEJ(List<PiezaDentalASEJ> aASEJ, List<PiezaDentalASEJ> bASEJ) {
        List<PiezaDentalASEJ> rASEJ = new ArrayList<>(aASEJ);
        rASEJ.addAll(bASEJ);
        return List.copyOf(rASEJ);
    }
}
