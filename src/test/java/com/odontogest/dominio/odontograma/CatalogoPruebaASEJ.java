package com.odontogest.dominio.odontograma;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static com.odontogest.dominio.odontograma.ConteoIndiceASEJ.C;
import static com.odontogest.dominio.odontograma.ConteoIndiceASEJ.E;
import static com.odontogest.dominio.odontograma.ConteoIndiceASEJ.NINGUNO;
import static com.odontogest.dominio.odontograma.ConteoIndiceASEJ.O;
import static com.odontogest.dominio.odontograma.ConteoIndiceASEJ.P;

public final class CatalogoPruebaASEJ {

    public static final TipoHallazgoDefASEJ CARIESASEJ = tipoASEJ(1, "CARIES", RepresentacionASEJ.SUPERFICIE, SimboloHallazgoASEJ.RELLENO,
            ReglaColorASEJ.SIEMPRE_ROJO, "SUPERFICIE", false, C, C);
    public static final TipoHallazgoDefASEJ RESTAURACIONASEJ = new TipoHallazgoDefASEJ(2, "RESTAURACION_DEFINITIVA",
            "Restauración definitiva", null, List.of("AM", "R", "IV", "IM", "IE"), RepresentacionASEJ.SUPERFICIE,
            SimboloHallazgoASEJ.RELLENO, ReglaColorASEJ.POR_ESTADO, "SUPERFICIE", false, O, O);
    public static final TipoHallazgoDefASEJ RESTAURACION_TEMPORALASEJ = tipoASEJ(3, "RESTAURACION_TEMPORAL",
            RepresentacionASEJ.SUPERFICIE, SimboloHallazgoASEJ.CONTORNO_SUPERFICIE, ReglaColorASEJ.SIEMPRE_ROJO, "SUPERFICIE", false, O, O);
    public static final TipoHallazgoDefASEJ SELLANTEASEJ = tipoASEJ(4, "SELLANTE", RepresentacionASEJ.SUPERFICIE, SimboloHallazgoASEJ.TRAZO_FOSAS,
            ReglaColorASEJ.POR_ESTADO, "SUPERFICIE_SELLANTE", false, NINGUNO, NINGUNO);
    public static final TipoHallazgoDefASEJ PIEZA_AUSENTEASEJ = tipoASEJ(5, "PIEZA_AUSENTE", RepresentacionASEJ.PIEZA, SimboloHallazgoASEJ.ASPA,
            ReglaColorASEJ.SIEMPRE_AZUL, "ESTADO_PIEZA", true, P, E);
    public static final TipoHallazgoDefASEJ REMANENTE_RADICULARASEJ = tipoASEJ(6, "REMANENTE_RADICULAR", RepresentacionASEJ.PIEZA,
            SimboloHallazgoASEJ.SOLO_SIGLA, ReglaColorASEJ.SIEMPRE_ROJO, "ESTADO_PIEZA", false, C, E);
    public static final TipoHallazgoDefASEJ IMPLANTEASEJ = tipoASEJ(7, "IMPLANTE", RepresentacionASEJ.PIEZA, SimboloHallazgoASEJ.SOLO_SIGLA,
            ReglaColorASEJ.POR_ESTADO, "ESTADO_PIEZA", false, P, NINGUNO);
    public static final TipoHallazgoDefASEJ CORONA_DEFINITIVAASEJ = tipoASEJ(9, "CORONA_DEFINITIVA", RepresentacionASEJ.PIEZA,
            SimboloHallazgoASEJ.CUADRADO_CORONA, ReglaColorASEJ.POR_ESTADO, "CORONA", false, O, O);
    public static final TipoHallazgoDefASEJ CORONA_TEMPORALASEJ = tipoASEJ(10, "CORONA_TEMPORAL", RepresentacionASEJ.PIEZA,
            SimboloHallazgoASEJ.CUADRADO_CORONA, ReglaColorASEJ.SIEMPRE_ROJO, "CORONA", false, O, O);
    public static final TipoHallazgoDefASEJ TRATAMIENTO_PULPARASEJ = tipoASEJ(11, "TRATAMIENTO_PULPAR", RepresentacionASEJ.PIEZA,
            SimboloHallazgoASEJ.LINEA_RAIZ, ReglaColorASEJ.POR_ESTADO, "PULPAR", false, NINGUNO, NINGUNO);
    public static final TipoHallazgoDefASEJ FRACTURAASEJ = tipoASEJ(12, "FRACTURA", RepresentacionASEJ.PIEZA, SimboloHallazgoASEJ.LINEA_FRACTURA,
            ReglaColorASEJ.SIEMPRE_ROJO, "FRACTURA", false, NINGUNO, NINGUNO);
    public static final TipoHallazgoDefASEJ MOVILIDADASEJ = tipoASEJ(16, "MOVILIDAD", RepresentacionASEJ.PIEZA, SimboloHallazgoASEJ.SOLO_SIGLA,
            ReglaColorASEJ.SIEMPRE_ROJO, "MOVILIDAD", false, NINGUNO, NINGUNO);
    public static final TipoHallazgoDefASEJ DIASTEMAASEJ = tipoASEJ(17, "DIASTEMA", RepresentacionASEJ.ENTRE_PIEZAS, SimboloHallazgoASEJ.PARENTESIS,
            ReglaColorASEJ.SIEMPRE_AZUL, "DIASTEMA", false, NINGUNO, NINGUNO);
    public static final TipoHallazgoDefASEJ PROTESIS_FIJAASEJ = tipoASEJ(18, "PROTESIS_FIJA", RepresentacionASEJ.RANGO_PIEZAS, SimboloHallazgoASEJ.PUENTE,
            ReglaColorASEJ.POR_ESTADO, "PROTESIS_FIJA", false, NINGUNO, NINGUNO);
    public static final TipoHallazgoDefASEJ PIEZA_NO_ERUPCIONADAASEJ = tipoASEJ(19, "PIEZA_AUSENTE_DNE", RepresentacionASEJ.PIEZA, SimboloHallazgoASEJ.ASPA,
            ReglaColorASEJ.SIEMPRE_AZUL, "ESTADO_PIEZA", true, NINGUNO, NINGUNO);
    public static final TipoHallazgoDefASEJ EDENTULO_TOTALASEJ = tipoASEJ(40, "EDENTULO_TOTAL", RepresentacionASEJ.RANGO_PIEZAS,
            SimboloHallazgoASEJ.LINEA_CORONAS, ReglaColorASEJ.SIEMPRE_AZUL, "EDENTULO", true, P, E);

    public static final LocalDateTime T0ASEJ = LocalDateTime.of(2026, 3, 2, 9, 0);

    private static final AtomicLong IDSASEJ = new AtomicLong(1000);

    private CatalogoPruebaASEJ() {
    }

    private static TipoHallazgoDefASEJ tipoASEJ(long idASEJ, String codigoASEJ, RepresentacionASEJ rASEJ, SimboloHallazgoASEJ simboloASEJ, ReglaColorASEJ colorASEJ,
                                        String capaASEJ, boolean ocultaASEJ, ConteoIndiceASEJ cpodASEJ, ConteoIndiceASEJ ceodASEJ) {
        return new TipoHallazgoDefASEJ(idASEJ, codigoASEJ, codigoASEJ, null, List.of(), rASEJ, simboloASEJ, colorASEJ, capaASEJ, ocultaASEJ, cpodASEJ, ceodASEJ);
    }

    public static HallazgoEventoASEJ supASEJ(TipoHallazgoDefASEJ tipoASEJ, int fdiASEJ, int minutoASEJ, EstadoHallazgoASEJ estadoASEJ, SuperficieASEJ... sASEJ) {
        return new HallazgoEventoASEJ(IDSASEJ.incrementAndGet(), 1, tipoASEJ, PiezaDentalASEJ.deASEJ(fdiASEJ), null,
                EnumSet.copyOf(List.of(sASEJ)), estadoASEJ, null, null, T0ASEJ.plusMinutes(minutoASEJ), 1, false);
    }

    public static HallazgoEventoASEJ supASEJ(TipoHallazgoDefASEJ tipoASEJ, int fdiASEJ, int minutoASEJ, SuperficieASEJ... sASEJ) {
        return supASEJ(tipoASEJ, fdiASEJ, minutoASEJ, null, sASEJ);
    }

    public static HallazgoEventoASEJ piezaASEJ(TipoHallazgoDefASEJ tipoASEJ, int fdiASEJ, int minutoASEJ, EstadoHallazgoASEJ estadoASEJ) {
        return new HallazgoEventoASEJ(IDSASEJ.incrementAndGet(), 1, tipoASEJ, PiezaDentalASEJ.deASEJ(fdiASEJ), null,
                EnumSet.noneOf(SuperficieASEJ.class), estadoASEJ, null, null, T0ASEJ.plusMinutes(minutoASEJ), 1, false);
    }

    public static HallazgoEventoASEJ piezaASEJ(TipoHallazgoDefASEJ tipoASEJ, int fdiASEJ, int minutoASEJ) {
        return piezaASEJ(tipoASEJ, fdiASEJ, minutoASEJ, null);
    }

    public static HallazgoEventoASEJ rangoASEJ(TipoHallazgoDefASEJ tipoASEJ, int desdeASEJ, int hastaASEJ, int minutoASEJ, EstadoHallazgoASEJ estadoASEJ) {
        return new HallazgoEventoASEJ(IDSASEJ.incrementAndGet(), 1, tipoASEJ, PiezaDentalASEJ.deASEJ(desdeASEJ), PiezaDentalASEJ.deASEJ(hastaASEJ),
                EnumSet.noneOf(SuperficieASEJ.class), estadoASEJ, null, null, T0ASEJ.plusMinutes(minutoASEJ), 1, false);
    }

    public static HallazgoEventoASEJ anuladoASEJ(HallazgoEventoASEJ eASEJ) {
        return new HallazgoEventoASEJ(eASEJ.idASEJ(), eASEJ.odontogramaIdASEJ(), eASEJ.tipoASEJ(), eASEJ.piezaASEJ(), eASEJ.piezaFinASEJ(), eASEJ.superficiesASEJ(),
                eASEJ.estadoASEJ(), eASEJ.siglaASEJ(), eASEJ.notaASEJ(), eASEJ.registradoEnASEJ(), eASEJ.registradoPorASEJ(), true);
    }

    public static EstadoOdontogramaASEJ estadoASEJ(HallazgoEventoASEJ... eventosASEJ) {
        return new ProyectorOdontogramaASEJ().proyectarASEJ(List.of(eventosASEJ), null);
    }
}
