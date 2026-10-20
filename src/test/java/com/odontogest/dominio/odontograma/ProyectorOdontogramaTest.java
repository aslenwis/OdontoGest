/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.odontograma;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

import static com.odontogest.dominio.odontograma.CatalogoPrueba.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proyección del odontograma: capas de exclusión, anulaciones, línea de tiempo y piezas ausentes.
 */
class ProyectorOdontogramaTest {

    private final ProyectorOdontograma proyector = new ProyectorOdontograma();

    @Test
    void sinEventosElEstadoEstaVacio() {
        EstadoOdontograma e = proyector.proyectar(List.of(), null);
        assertThat(e.estaVacio()).isTrue();
        assertThat(e.piezas()).isEmpty();
        assertThat(EstadoOdontograma.vacio().claves()).isEmpty();
    }

    @Test
    void restauracionReemplazaALaCariesEnLaMismaCapa() {
        HallazgoEvento caries = sup(CARIES, 36, 0, Superficie.O);
        HallazgoEvento restauracion = sup(RESTAURACION, 36, 10, EstadoHallazgo.BUENO, Superficie.O);

        EstadoOdontograma e = estado(caries, restauracion);

        assertThat(e.enSuperficie(36, Superficie.O, "SUPERFICIE")).contains(restauracion);
        assertThat(e.pieza(36).orElseThrow().hallazgos()).containsExactly(restauracion);
    }

    @Test
    void elOrdenDeLaListaNoImportaGanaElMasReciente() {
        HallazgoEvento caries = sup(CARIES, 36, 0, Superficie.O);
        HallazgoEvento restauracion = sup(RESTAURACION, 36, 10, EstadoHallazgo.BUENO, Superficie.O);

        EstadoOdontograma e = proyector.proyectar(List.of(restauracion, caries), null);

        assertThat(e.enSuperficie(36, Superficie.O, "SUPERFICIE")).contains(restauracion);
    }

    @Test
    void aIgualFechaDesempataElId() {
        HallazgoEvento a = sup(CARIES, 36, 5, Superficie.O);
        HallazgoEvento b = sup(RESTAURACION, 36, 5, EstadoHallazgo.MALO, Superficie.O);   // id mayor
        assertThat(proyector.proyectar(List.of(b, a), null).enSuperficie(36, Superficie.O, "SUPERFICIE"))
                .contains(b);
    }

    @Test
    void sellanteConviveConRestauracionPorqueUsaOtraCapa() {
        HallazgoEvento restauracion = sup(RESTAURACION, 46, 0, EstadoHallazgo.BUENO, Superficie.M);
        HallazgoEvento sellante = sup(SELLANTE, 46, 5, EstadoHallazgo.BUENO, Superficie.O, Superficie.M);

        EstadoOdontograma e = estado(restauracion, sellante);

        assertThat(e.pieza(46).orElseThrow().hallazgos()).containsExactly(restauracion, sellante);
        assertThat(e.pieza(46).orElseThrow().enSuperficie(Superficie.M)).containsExactly(restauracion, sellante);
    }

    @Test
    void unaRestauracionMODEsUnSoloHallazgoEnTresClaves() {
        HallazgoEvento mod = sup(RESTAURACION, 26, 0, EstadoHallazgo.BUENO, Superficie.M, Superficie.O, Superficie.D);

        EstadoOdontograma e = estado(mod);

        assertThat(e.claves()).hasSize(3);
        assertThat(e.hallazgosVigentes()).containsExactly(mod);
        assertThat(e.pieza(26).orElseThrow().hallazgos()).containsExactly(mod);
        assertThat(Superficies.aMascara(mod.superficies())).isEqualTo(7);
    }

    @Test
    void restauracionParcialSobreCariesDejaVigenteLaParteNoRestaurada() {
        HallazgoEvento caries = sup(CARIES, 16, 0, Superficie.O, Superficie.M);
        HallazgoEvento restauracion = sup(RESTAURACION, 16, 10, EstadoHallazgo.BUENO, Superficie.O);

        EstadoOdontograma e = estado(caries, restauracion);

        assertThat(e.enSuperficie(16, Superficie.O, "SUPERFICIE")).contains(restauracion);
        assertThat(e.enSuperficie(16, Superficie.M, "SUPERFICIE")).contains(caries);
        assertThat(e.pieza(16).orElseThrow().hallazgos()).containsExactly(caries, restauracion);
    }

    @Test
    void losAnuladosNoCuentan() {
        HallazgoEvento caries = sup(CARIES, 36, 0, Superficie.O);
        HallazgoEvento restauracion = anulado(sup(RESTAURACION, 36, 10, EstadoHallazgo.BUENO, Superficie.O));

        EstadoOdontograma e = estado(caries, restauracion);

        assertThat(e.enSuperficie(36, Superficie.O, "SUPERFICIE")).contains(caries);
    }

    @Test
    void lineaDeTiempoReconstruyeElEstadoEnUnaFecha() {
        HallazgoEvento caries = sup(CARIES, 36, 0, Superficie.O);
        HallazgoEvento restauracion = sup(RESTAURACION, 36, 60, EstadoHallazgo.BUENO, Superficie.O);
        List<HallazgoEvento> eventos = List.of(caries, restauracion);

        assertThat(proyector.proyectar(eventos, T0.plusMinutes(30)).enSuperficie(36, Superficie.O, "SUPERFICIE"))
                .contains(caries);
        assertThat(proyector.proyectar(eventos, T0.plusMinutes(60)).enSuperficie(36, Superficie.O, "SUPERFICIE"))
                .as("el corte es inclusivo").contains(restauracion);
        assertThat(proyector.proyectar(eventos, T0.minusMinutes(1)).estaVacio()).isTrue();
    }

    @Test
    void piezaAusenteOcultaLasSuperficiesSinBorrarlas() {
        HallazgoEvento caries = sup(CARIES, 36, 0, Superficie.O);
        HallazgoEvento corona = pieza(CORONA_DEFINITIVA, 36, 1, EstadoHallazgo.BUENO);
        HallazgoEvento ausente = pieza(PIEZA_AUSENTE, 36, 10);

        EstadoPieza p = estado(caries, corona, ausente).pieza(36).orElseThrow();

        assertThat(p.superficiesOcultas()).isTrue();
        assertThat(p.hallazgos()).containsExactly(caries, corona, ausente);
        assertThat(p.hallazgosVisibles()).containsExactly(corona, ausente);
        assertThat(p.enSuperficie(Superficie.O)).isEmpty();
    }

    @Test
    void implanteReemplazaAPiezaAusenteEnLaCapaEstadoPieza() {
        HallazgoEvento ausente = pieza(PIEZA_AUSENTE, 46, 0);
        HallazgoEvento implante = pieza(IMPLANTE, 46, 10, EstadoHallazgo.BUENO);

        EstadoPieza p = estado(ausente, implante).pieza(46).orElseThrow();

        assertThat(p.hallazgos()).containsExactly(implante);
        assertThat(p.superficiesOcultas()).isFalse();
    }

    @Test
    void rangosUsanUnaClaveDeMenorAMayorFdi() {
        HallazgoEvento puente = rango(PROTESIS_FIJA, 16, 14, 0, EstadoHallazgo.BUENO);
        HallazgoEvento diastema = rango(DIASTEMA, 11, 21, 1, null);

        EstadoOdontograma e = estado(puente, diastema);

        assertThat(e.claves()).containsExactlyInAnyOrder(
                new ClaveCapa(14, 16, null, "PROTESIS_FIJA"), new ClaveCapa(11, 21, null, "DIASTEMA"));
        assertThat(e.rangos()).containsExactly(puente, diastema);
        assertThat(e.piezas()).isEmpty();
        assertThat(new ClaveCapa(14, 16, null, "X").esDeRango()).isTrue();
    }

    @Test
    void nuevoPuenteSobreElMismoRangoReemplazaAlAnterior() {
        HallazgoEvento bueno = rango(PROTESIS_FIJA, 14, 16, 0, EstadoHallazgo.BUENO);
        HallazgoEvento malo = rango(PROTESIS_FIJA, 16, 14, 30, EstadoHallazgo.MALO);
        assertThat(estado(bueno, malo).rangos()).containsExactly(malo);
    }

    @Test
    void piezasEnOrdenDeDibujo() {
        EstadoOdontograma e = estado(sup(CARIES, 38, 0, Superficie.O), sup(CARIES, 18, 1, Superficie.O),
                sup(CARIES, 21, 2, Superficie.O), sup(CARIES, 55, 3, Superficie.O));
        assertThat(e.piezas().keySet()).containsExactly(18, 21, 55, 38);
    }

    @Test
    void colorSegunReglaDelCatalogo() {
        assertThat(sup(CARIES, 36, 0, Superficie.O).color()).isEqualTo(ColorHallazgo.ROJO);
        assertThat(pieza(PIEZA_AUSENTE, 36, 0).color()).isEqualTo(ColorHallazgo.AZUL);
        assertThat(sup(RESTAURACION, 36, 0, EstadoHallazgo.BUENO, Superficie.O).color()).isEqualTo(ColorHallazgo.AZUL);
        assertThat(sup(RESTAURACION, 36, 0, EstadoHallazgo.MALO, Superficie.O).color()).isEqualTo(ColorHallazgo.ROJO);
        assertThat(RESTAURACION.usaEstado()).isTrue();
        assertThat(CARIES.usaEstado()).isFalse();
    }

    @Test
    void elEventoValidaSuRepresentacionYEsInmutable() {
        LocalDateTime t = T0;
        assertThatThrownBy(() -> new HallazgoEvento(1, 1, CARIES, PiezaDental.de(36), null,
                EnumSet.noneOf(Superficie.class), null, null, null, t, 1, false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HallazgoEvento(1, 1, DIASTEMA, PiezaDental.de(11), null,
                null, null, null, null, t, 1, false))
                .isInstanceOf(NullPointerException.class);

        EnumSet<Superficie> original = EnumSet.of(Superficie.O);
        HallazgoEvento e = new HallazgoEvento(1, 1, CARIES, PiezaDental.de(36), null, original,
                null, null, null, t, 1, false);
        original.add(Superficie.M);
        e.superficies().add(Superficie.D);
        assertThat(e.superficies()).containsExactly(Superficie.O);
        assertThat(e.esDeSuperficie()).isTrue();
        assertThat(e.esDeRango()).isFalse();
    }

    @Test
    void elCatalogoRechazaComponentesDeIndiceIncoherentes() {
        assertThatThrownBy(() -> new TipoHallazgoDef(1, "X", "X", null, null, Representacion.PIEZA, "ASPA",
                ReglaColor.SIEMPRE_AZUL, "C", false, ConteoIndice.E, ConteoIndice.NINGUNO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TipoHallazgoDef(1, "X", "X", null, null, Representacion.PIEZA, "ASPA",
                ReglaColor.SIEMPRE_AZUL, "C", false, ConteoIndice.NINGUNO, ConteoIndice.P))
                .isInstanceOf(IllegalArgumentException.class);
        TipoHallazgoDef sinConteos = new TipoHallazgoDef(1, "X", "X", null, null, Representacion.PIEZA, "ASPA",
                ReglaColor.SIEMPRE_AZUL, "C", false, null, null);
        assertThat(sinConteos.conteoCpod()).isEqualTo(ConteoIndice.NINGUNO);
        assertThat(sinConteos.siglasOpciones()).isEmpty();
    }
}
