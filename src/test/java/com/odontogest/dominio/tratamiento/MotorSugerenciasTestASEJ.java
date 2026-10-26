package com.odontogest.dominio.tratamiento;

import com.odontogest.dominio.odontograma.EstadoHallazgoASEJ;
import com.odontogest.dominio.odontograma.EstadoOdontogramaASEJ;
import com.odontogest.dominio.odontograma.HallazgoEventoASEJ;
import com.odontogest.dominio.odontograma.SuperficieASEJ;
import com.odontogest.dominio.odontograma.TipoRadicularASEJ;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.odontogest.dominio.odontograma.CatalogoPruebaASEJ.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class MotorSugerenciasTestASEJ {

    private static final ProcedimientoDefASEJ CONSASEJ = procASEJ(1, "CONS", 5000, FormaCobroASEJ.BOCA);
    private static final ProcedimientoDefASEJ PROFASEJ = procASEJ(2, "PROF", 8000, FormaCobroASEJ.BOCA);
    private static final ProcedimientoDefASEJ DESTASEJ = procASEJ(3, "DEST", 12000, FormaCobroASEJ.BOCA);
    private static final ProcedimientoDefASEJ RES_SASEJ = procASEJ(4, "RES-S", 12000, FormaCobroASEJ.PIEZA);
    private static final ProcedimientoDefASEJ RES_CASEJ = procASEJ(5, "RES-C", 18000, FormaCobroASEJ.PIEZA);
    private static final ProcedimientoDefASEJ ENDO_UASEJ = procASEJ(6, "ENDO-U", 35000, FormaCobroASEJ.PIEZA);
    private static final ProcedimientoDefASEJ ENDO_BASEJ = procASEJ(7, "ENDO-B", 45000, FormaCobroASEJ.PIEZA);
    private static final ProcedimientoDefASEJ ENDO_MASEJ = procASEJ(8, "ENDO-M", 60000, FormaCobroASEJ.PIEZA);
    private static final ProcedimientoDefASEJ EXO_RRASEJ = procASEJ(9, "EXO-RR", 12000, FormaCobroASEJ.PIEZA);
    private static final ProcedimientoDefASEJ CORO_MCASEJ = procASEJ(10, "CORO-MC", 90000, FormaCobroASEJ.PIEZA);
    private static final ProcedimientoDefASEJ SELLASEJ = procASEJ(11, "SELL", 5000, FormaCobroASEJ.SUPERFICIE);

    private static final Map<Long, ProcedimientoDefASEJ> TARIFARIOASEJ = Stream.of(
                    CONSASEJ, PROFASEJ, DESTASEJ, RES_SASEJ, RES_CASEJ, ENDO_UASEJ, ENDO_BASEJ, ENDO_MASEJ, EXO_RRASEJ, CORO_MCASEJ, SELLASEJ)
            .collect(Collectors.toMap(ProcedimientoDefASEJ::idASEJ, Function.identity()));

    private static final List<ReglaSugerenciaDefASEJ> REGLASASEJ = List.of(
            reglaASEJ(1, null, null, null, 0, 5, CONSASEJ, 1, 1),
            reglaASEJ(2, null, null, null, 0, 5, PROFASEJ, 4, 2),
            reglaASEJ(3, REMANENTE_RADICULARASEJ.idASEJ(), null, null, 0, 5, EXO_RRASEJ, 1, 10),
            reglaASEJ(4, FRACTURAASEJ.idASEJ(), null, null, 0, 5, RES_CASEJ, 1, 20),
            reglaASEJ(5, CARIESASEJ.idASEJ(), null, null, 1, 1, RES_SASEJ, 2, 30),
            reglaASEJ(6, CARIESASEJ.idASEJ(), null, null, 2, 5, RES_CASEJ, 2, 31),
            reglaASEJ(7, RESTAURACION_TEMPORALASEJ.idASEJ(), null, null, 1, 1, RES_SASEJ, 2, 32),
            reglaASEJ(8, RESTAURACION_TEMPORALASEJ.idASEJ(), null, null, 2, 5, RES_CASEJ, 2, 33),
            reglaASEJ(9, RESTAURACIONASEJ.idASEJ(), EstadoHallazgoASEJ.MALO, null, 1, 1, RES_SASEJ, 2, 34),
            reglaASEJ(10, RESTAURACIONASEJ.idASEJ(), EstadoHallazgoASEJ.MALO, null, 2, 5, RES_CASEJ, 2, 35),
            reglaASEJ(11, TRATAMIENTO_PULPARASEJ.idASEJ(), EstadoHallazgoASEJ.MALO, TipoRadicularASEJ.UNIRRADICULAR, 0, 5, ENDO_UASEJ, 1, 40),
            reglaASEJ(12, TRATAMIENTO_PULPARASEJ.idASEJ(), EstadoHallazgoASEJ.MALO, TipoRadicularASEJ.BIRRADICULAR, 0, 5, ENDO_BASEJ, 1, 41),
            reglaASEJ(13, TRATAMIENTO_PULPARASEJ.idASEJ(), EstadoHallazgoASEJ.MALO, TipoRadicularASEJ.MULTIRRADICULAR, 0, 5, ENDO_MASEJ, 1, 42),
            reglaASEJ(14, CORONA_TEMPORALASEJ.idASEJ(), null, null, 0, 5, CORO_MCASEJ, 3, 50),
            reglaASEJ(15, CORONA_DEFINITIVAASEJ.idASEJ(), EstadoHallazgoASEJ.MALO, null, 0, 5, CORO_MCASEJ, 3, 51),
            reglaASEJ(16, MOVILIDADASEJ.idASEJ(), null, null, 0, 5, DESTASEJ, 2, 60));

    private final MotorSugerenciasASEJ motorASEJ = new MotorSugerenciasASEJ();

    private static ProcedimientoDefASEJ procASEJ(long idASEJ, String codigoASEJ, long precioASEJ, FormaCobroASEJ formaASEJ) {
        return new ProcedimientoDefASEJ(idASEJ, codigoASEJ, codigoASEJ, precioASEJ, formaASEJ);
    }

    private static ReglaSugerenciaDefASEJ reglaASEJ(long idASEJ, Long tipoASEJ, EstadoHallazgoASEJ estadoASEJ, TipoRadicularASEJ radicularASEJ,
                                            int minASEJ, int maxASEJ, ProcedimientoDefASEJ pASEJ, int faseASEJ, int prioridadASEJ) {
        return new ReglaSugerenciaDefASEJ(idASEJ, tipoASEJ, estadoASEJ, radicularASEJ, minASEJ, maxASEJ, pASEJ.idASEJ(), faseASEJ, prioridadASEJ);
    }

    private List<ItemSugeridoASEJ> sugerirASEJ(HallazgoEventoASEJ... eventosASEJ) {
        return motorASEJ.sugerirASEJ(estadoASEJ(eventosASEJ), REGLASASEJ, TARIFARIOASEJ);
    }

    @Test
    void bocaSinHallazgosSoloRecibeLasReglasGeneralesASEJ() {
        List<ItemSugeridoASEJ> itemsASEJ = motorASEJ.sugerirASEJ(EstadoOdontogramaASEJ.vacioASEJ(), REGLASASEJ, TARIFARIOASEJ);
        assertThat(itemsASEJ).extracting(ItemSugeridoASEJ::procedimientoIdASEJ, ItemSugeridoASEJ::piezaFdiASEJ, ItemSugeridoASEJ::faseASEJ)
                .containsExactly(tuple(CONSASEJ.idASEJ(), null, 1), tuple(PROFASEJ.idASEJ(), null, 4));
    }

    @Test
    void cariesSegunNumeroDeSuperficiesASEJ() {
        List<ItemSugeridoASEJ> itemsASEJ = sugerirASEJ(
                supASEJ(CARIESASEJ, 36, 0, SuperficieASEJ.O),
                supASEJ(CARIESASEJ, 46, 1, SuperficieASEJ.M, SuperficieASEJ.O, SuperficieASEJ.D));
        assertThat(itemsASEJ).extracting(ItemSugeridoASEJ::procedimientoIdASEJ, ItemSugeridoASEJ::piezaFdiASEJ)
                .contains(tuple(RES_SASEJ.idASEJ(), 36), tuple(RES_CASEJ.idASEJ(), 46));
    }

    @Test
    void restauracionEnBuenEstadoNoGeneraTratamientoASEJ() {
        List<ItemSugeridoASEJ> itemsASEJ = sugerirASEJ(supASEJ(RESTAURACIONASEJ, 36, 0, EstadoHallazgoASEJ.BUENO, SuperficieASEJ.O));
        assertThat(itemsASEJ).extracting(ItemSugeridoASEJ::piezaFdiASEJ).containsOnlyNulls();
        assertThat(sugerirASEJ(supASEJ(RESTAURACIONASEJ, 36, 0, EstadoHallazgoASEJ.MALO, SuperficieASEJ.O)))
                .extracting(ItemSugeridoASEJ::procedimientoIdASEJ, ItemSugeridoASEJ::piezaFdiASEJ).contains(tuple(RES_SASEJ.idASEJ(), 36));
    }

    @Test
    void endodonciaSegunTipoRadicularDeLaPiezaASEJ() {
        List<ItemSugeridoASEJ> itemsASEJ = sugerirASEJ(
                piezaASEJ(TRATAMIENTO_PULPARASEJ, 11, 0, EstadoHallazgoASEJ.MALO),
                piezaASEJ(TRATAMIENTO_PULPARASEJ, 14, 1, EstadoHallazgoASEJ.MALO),
                piezaASEJ(TRATAMIENTO_PULPARASEJ, 36, 2, EstadoHallazgoASEJ.MALO),
                piezaASEJ(TRATAMIENTO_PULPARASEJ, 26, 3, EstadoHallazgoASEJ.BUENO));
        assertThat(itemsASEJ).extracting(ItemSugeridoASEJ::procedimientoIdASEJ, ItemSugeridoASEJ::piezaFdiASEJ)
                .contains(tuple(ENDO_UASEJ.idASEJ(), 11), tuple(ENDO_BASEJ.idASEJ(), 14), tuple(ENDO_MASEJ.idASEJ(), 36))
                .doesNotContain(tuple(ENDO_MASEJ.idASEJ(), 26));
    }

    @Test
    void desduplicaPorProcedimientoYPiezaASEJ() {
        List<ItemSugeridoASEJ> itemsASEJ = sugerirASEJ(
                supASEJ(CARIESASEJ, 36, 0, SuperficieASEJ.O),
                supASEJ(CARIESASEJ, 36, 1, SuperficieASEJ.V));
        assertThat(itemsASEJ).filteredOn(iASEJ -> Integer.valueOf(36).equals(iASEJ.piezaFdiASEJ())).hasSize(1);
    }

    @Test
    void ordenaPorFaseYLuegoPorOrdenDeDibujoASEJ() {
        List<ItemSugeridoASEJ> itemsASEJ = sugerirASEJ(
                supASEJ(CARIESASEJ, 36, 0, SuperficieASEJ.O),
                piezaASEJ(CORONA_TEMPORALASEJ, 21, 1),
                piezaASEJ(REMANENTE_RADICULARASEJ, 46, 2),
                supASEJ(CARIESASEJ, 18, 3, SuperficieASEJ.O),
                piezaASEJ(MOVILIDADASEJ, 31, 4));
        assertThat(itemsASEJ).extracting(ItemSugeridoASEJ::faseASEJ, ItemSugeridoASEJ::piezaFdiASEJ).containsExactly(
                tuple(1, null), tuple(1, 46),
                tuple(2, 18), tuple(2, 31), tuple(2, 36),
                tuple(3, 21),
                tuple(4, null));
    }

    @Test
    void reglaDeMenorPrioridadGanaAunqueVengaDespuesEnLaListaASEJ() {
        List<ReglaSugerenciaDefASEJ> reglasASEJ = List.of(
                reglaASEJ(20, CARIESASEJ.idASEJ(), null, null, 1, 5, RES_CASEJ, 2, 99),
                reglaASEJ(21, CARIESASEJ.idASEJ(), null, null, 1, 5, RES_SASEJ, 2, 5));
        List<ItemSugeridoASEJ> itemsASEJ = motorASEJ.sugerirASEJ(estadoASEJ(supASEJ(CARIESASEJ, 36, 0, SuperficieASEJ.O)), reglasASEJ, TARIFARIOASEJ);
        assertThat(itemsASEJ).extracting(ItemSugeridoASEJ::procedimientoIdASEJ).containsExactly(RES_SASEJ.idASEJ());
    }

    @Test
    void precioPorSuperficieMultiplicaLaCantidadASEJ() {
        List<ReglaSugerenciaDefASEJ> reglasASEJ = List.of(reglaASEJ(30, SELLANTEASEJ.idASEJ(), null, null, 0, 5, SELLASEJ, 4, 1));
        List<ItemSugeridoASEJ> itemsASEJ = motorASEJ.sugerirASEJ(
                estadoASEJ(supASEJ(SELLANTEASEJ, 36, 0, EstadoHallazgoASEJ.MALO, SuperficieASEJ.O, SuperficieASEJ.V)), reglasASEJ, TARIFARIOASEJ);
        ItemSugeridoASEJ itemASEJ = itemsASEJ.get(0);
        assertThat(itemASEJ.cantidadASEJ()).isEqualTo(2);
        assertThat(itemASEJ.precioUnitCentASEJ()).isEqualTo(5000);
        assertThat(itemASEJ.totalCentASEJ()).isEqualTo(10000);
        assertThat(itemASEJ.superficiesASEJ()).containsExactlyInAnyOrder(SuperficieASEJ.O, SuperficieASEJ.V);
        assertThat(itemASEJ.origenHallazgoIdASEJ()).isNotNull();
    }

    @Test
    void cariesOcultaPorPiezaAusenteNoGeneraRestauracionASEJ() {
        List<ItemSugeridoASEJ> itemsASEJ = sugerirASEJ(supASEJ(CARIESASEJ, 36, 0, SuperficieASEJ.O), piezaASEJ(PIEZA_AUSENTEASEJ, 36, 10));
        assertThat(itemsASEJ).extracting(ItemSugeridoASEJ::piezaFdiASEJ).containsOnlyNulls();
    }

    @Test
    void cariesBajoEdentuloTotalNoGeneraRestauracionASEJ() {
        List<ItemSugeridoASEJ> itemsASEJ = sugerirASEJ(supASEJ(CARIESASEJ, 16, 0, SuperficieASEJ.O), rangoASEJ(EDENTULO_TOTALASEJ, 18, 28, 10, null));
        assertThat(itemsASEJ).extracting(ItemSugeridoASEJ::piezaFdiASEJ).containsOnlyNulls();
    }

    @Test
    void reglaConProcedimientoInexistenteFallaASEJ() {
        List<ReglaSugerenciaDefASEJ> reglasASEJ = List.of(new ReglaSugerenciaDefASEJ(1, null, null, null, 0, 5, 999, 1, 1));
        assertThatThrownBy(() -> motorASEJ.sugerirASEJ(EstadoOdontogramaASEJ.vacioASEJ(), reglasASEJ, TARIFARIOASEJ))
                .isInstanceOf(NullPointerException.class).hasMessageContaining("999");
    }

    @Test
    void validacionesDeReglasYProcedimientosASEJ() {
        assertThatThrownBy(() -> new ReglaSugerenciaDefASEJ(1, 1L, null, null, 3, 1, 1, 1, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ReglaSugerenciaDefASEJ(1, 1L, null, null, 0, 5, 1, 5, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProcedimientoDefASEJ(1, "X", "X", -1, FormaCobroASEJ.PIEZA))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
