/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.tratamiento;

import com.odontogest.dominio.odontograma.EstadoHallazgo;
import com.odontogest.dominio.odontograma.EstadoOdontograma;
import com.odontogest.dominio.odontograma.HallazgoEvento;
import com.odontogest.dominio.odontograma.Superficie;
import com.odontogest.dominio.odontograma.TipoRadicular;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.odontogest.dominio.odontograma.CatalogoPrueba.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Motor de sugerencias con el tarifario y las reglas demo de PLAN §3.5.
 */
class MotorSugerenciasTest {

    private static final ProcedimientoDef CONS = proc(1, "CONS", 5000, FormaCobro.BOCA);
    private static final ProcedimientoDef PROF = proc(2, "PROF", 8000, FormaCobro.BOCA);
    private static final ProcedimientoDef DEST = proc(3, "DEST", 12000, FormaCobro.BOCA);
    private static final ProcedimientoDef RES_S = proc(4, "RES-S", 12000, FormaCobro.PIEZA);
    private static final ProcedimientoDef RES_C = proc(5, "RES-C", 18000, FormaCobro.PIEZA);
    private static final ProcedimientoDef ENDO_U = proc(6, "ENDO-U", 35000, FormaCobro.PIEZA);
    private static final ProcedimientoDef ENDO_B = proc(7, "ENDO-B", 45000, FormaCobro.PIEZA);
    private static final ProcedimientoDef ENDO_M = proc(8, "ENDO-M", 60000, FormaCobro.PIEZA);
    private static final ProcedimientoDef EXO_RR = proc(9, "EXO-RR", 12000, FormaCobro.PIEZA);
    private static final ProcedimientoDef CORO_MC = proc(10, "CORO-MC", 90000, FormaCobro.PIEZA);
    private static final ProcedimientoDef SELL = proc(11, "SELL", 5000, FormaCobro.SUPERFICIE);

    private static final Map<Long, ProcedimientoDef> TARIFARIO = Stream.of(
                    CONS, PROF, DEST, RES_S, RES_C, ENDO_U, ENDO_B, ENDO_M, EXO_RR, CORO_MC, SELL)
            .collect(Collectors.toMap(ProcedimientoDef::id, Function.identity()));

    private static final List<ReglaSugerenciaDef> REGLAS = List.of(
            regla(1, null, null, null, 0, 5, CONS, 1, 1),
            regla(2, null, null, null, 0, 5, PROF, 4, 2),
            regla(3, REMANENTE_RADICULAR.id(), null, null, 0, 5, EXO_RR, 1, 10),
            regla(4, FRACTURA.id(), null, null, 0, 5, RES_C, 1, 20),
            regla(5, CARIES.id(), null, null, 1, 1, RES_S, 2, 30),
            regla(6, CARIES.id(), null, null, 2, 5, RES_C, 2, 31),
            regla(7, RESTAURACION_TEMPORAL.id(), null, null, 1, 1, RES_S, 2, 32),
            regla(8, RESTAURACION_TEMPORAL.id(), null, null, 2, 5, RES_C, 2, 33),
            regla(9, RESTAURACION.id(), EstadoHallazgo.MALO, null, 1, 1, RES_S, 2, 34),
            regla(10, RESTAURACION.id(), EstadoHallazgo.MALO, null, 2, 5, RES_C, 2, 35),
            regla(11, TRATAMIENTO_PULPAR.id(), EstadoHallazgo.MALO, TipoRadicular.UNIRRADICULAR, 0, 5, ENDO_U, 1, 40),
            regla(12, TRATAMIENTO_PULPAR.id(), EstadoHallazgo.MALO, TipoRadicular.BIRRADICULAR, 0, 5, ENDO_B, 1, 41),
            regla(13, TRATAMIENTO_PULPAR.id(), EstadoHallazgo.MALO, TipoRadicular.MULTIRRADICULAR, 0, 5, ENDO_M, 1, 42),
            regla(14, CORONA_TEMPORAL.id(), null, null, 0, 5, CORO_MC, 3, 50),
            regla(15, CORONA_DEFINITIVA.id(), EstadoHallazgo.MALO, null, 0, 5, CORO_MC, 3, 51),
            regla(16, MOVILIDAD.id(), null, null, 0, 5, DEST, 2, 60));

    private final MotorSugerencias motor = new MotorSugerencias();

    private static ProcedimientoDef proc(long id, String codigo, long precio, FormaCobro forma) {
        return new ProcedimientoDef(id, codigo, codigo, precio, forma);
    }

    private static ReglaSugerenciaDef regla(long id, Long tipo, EstadoHallazgo estado, TipoRadicular radicular,
                                            int min, int max, ProcedimientoDef p, int fase, int prioridad) {
        return new ReglaSugerenciaDef(id, tipo, estado, radicular, min, max, p.id(), fase, prioridad);
    }

    private List<ItemSugerido> sugerir(HallazgoEvento... eventos) {
        return motor.sugerir(estado(eventos), REGLAS, TARIFARIO);
    }

    @Test
    void bocaSinHallazgosSoloRecibeLasReglasGenerales() {
        List<ItemSugerido> items = motor.sugerir(EstadoOdontograma.vacio(), REGLAS, TARIFARIO);
        assertThat(items).extracting(ItemSugerido::procedimientoId, ItemSugerido::piezaFdi, ItemSugerido::fase)
                .containsExactly(tuple(CONS.id(), null, 1), tuple(PROF.id(), null, 4));
    }

    @Test
    void cariesSegunNumeroDeSuperficies() {
        List<ItemSugerido> items = sugerir(
                sup(CARIES, 36, 0, Superficie.O),
                sup(CARIES, 46, 1, Superficie.M, Superficie.O, Superficie.D));
        assertThat(items).extracting(ItemSugerido::procedimientoId, ItemSugerido::piezaFdi)
                .contains(tuple(RES_S.id(), 36), tuple(RES_C.id(), 46));
    }

    @Test
    void restauracionEnBuenEstadoNoGeneraTratamiento() {
        List<ItemSugerido> items = sugerir(sup(RESTAURACION, 36, 0, EstadoHallazgo.BUENO, Superficie.O));
        assertThat(items).extracting(ItemSugerido::piezaFdi).containsOnlyNulls();
        assertThat(sugerir(sup(RESTAURACION, 36, 0, EstadoHallazgo.MALO, Superficie.O)))
                .extracting(ItemSugerido::procedimientoId, ItemSugerido::piezaFdi).contains(tuple(RES_S.id(), 36));
    }

    @Test
    void endodonciaSegunTipoRadicularDeLaPieza() {
        List<ItemSugerido> items = sugerir(
                pieza(TRATAMIENTO_PULPAR, 11, 0, EstadoHallazgo.MALO),
                pieza(TRATAMIENTO_PULPAR, 14, 1, EstadoHallazgo.MALO),
                pieza(TRATAMIENTO_PULPAR, 36, 2, EstadoHallazgo.MALO),
                pieza(TRATAMIENTO_PULPAR, 26, 3, EstadoHallazgo.BUENO));
        assertThat(items).extracting(ItemSugerido::procedimientoId, ItemSugerido::piezaFdi)
                .contains(tuple(ENDO_U.id(), 11), tuple(ENDO_B.id(), 14), tuple(ENDO_M.id(), 36))
                .doesNotContain(tuple(ENDO_M.id(), 26));
    }

    @Test
    void desduplicaPorProcedimientoYPieza() {
        List<ItemSugerido> items = sugerir(
                sup(CARIES, 36, 0, Superficie.O),
                sup(CARIES, 36, 1, Superficie.V));                                  // otra caries simple, misma pieza
        assertThat(items).filteredOn(i -> Integer.valueOf(36).equals(i.piezaFdi())).hasSize(1);
    }

    @Test
    void ordenaPorFaseYLuegoPorOrdenDeDibujo() {
        List<ItemSugerido> items = sugerir(
                sup(CARIES, 36, 0, Superficie.O),
                pieza(CORONA_TEMPORAL, 21, 1),
                pieza(REMANENTE_RADICULAR, 46, 2),
                sup(CARIES, 18, 3, Superficie.O),
                pieza(MOVILIDAD, 31, 4));
        assertThat(items).extracting(ItemSugerido::fase, ItemSugerido::piezaFdi).containsExactly(
                tuple(1, null), tuple(1, 46),
                tuple(2, 18), tuple(2, 31), tuple(2, 36),
                tuple(3, 21),
                tuple(4, null));
    }

    @Test
    void reglaDeMenorPrioridadGanaAunqueVengaDespuesEnLaLista() {
        List<ReglaSugerenciaDef> reglas = List.of(
                regla(20, CARIES.id(), null, null, 1, 5, RES_C, 2, 99),
                regla(21, CARIES.id(), null, null, 1, 5, RES_S, 2, 5));
        List<ItemSugerido> items = motor.sugerir(estado(sup(CARIES, 36, 0, Superficie.O)), reglas, TARIFARIO);
        assertThat(items).extracting(ItemSugerido::procedimientoId).containsExactly(RES_S.id());
    }

    @Test
    void precioPorSuperficieMultiplicaLaCantidad() {
        List<ReglaSugerenciaDef> reglas = List.of(regla(30, SELLANTE.id(), null, null, 0, 5, SELL, 4, 1));
        List<ItemSugerido> items = motor.sugerir(
                estado(sup(SELLANTE, 36, 0, EstadoHallazgo.MALO, Superficie.O, Superficie.V)), reglas, TARIFARIO);
        ItemSugerido item = items.get(0);
        assertThat(item.cantidad()).isEqualTo(2);
        assertThat(item.precioUnitCent()).isEqualTo(5000);
        assertThat(item.totalCent()).isEqualTo(10000);
        assertThat(item.superficies()).containsExactlyInAnyOrder(Superficie.O, Superficie.V);
        assertThat(item.origenHallazgoId()).isNotNull();
    }

    @Test
    void cariesOcultaPorPiezaAusenteNoGeneraRestauracion() {
        List<ItemSugerido> items = sugerir(sup(CARIES, 36, 0, Superficie.O), pieza(PIEZA_AUSENTE, 36, 10));
        assertThat(items).extracting(ItemSugerido::piezaFdi).containsOnlyNulls();
    }

    @Test
    void reglaConProcedimientoInexistenteFalla() {
        List<ReglaSugerenciaDef> reglas = List.of(new ReglaSugerenciaDef(1, null, null, null, 0, 5, 999, 1, 1));
        assertThatThrownBy(() -> motor.sugerir(EstadoOdontograma.vacio(), reglas, TARIFARIO))
                .isInstanceOf(NullPointerException.class).hasMessageContaining("999");
    }

    @Test
    void validacionesDeReglasYProcedimientos() {
        assertThatThrownBy(() -> new ReglaSugerenciaDef(1, 1L, null, null, 3, 1, 1, 1, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ReglaSugerenciaDef(1, 1L, null, null, 0, 5, 1, 5, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProcedimientoDef(1, "X", "X", -1, FormaCobro.PIEZA))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
