/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.odontograma;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.odontogest.dominio.odontograma.CatalogoPrueba.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comparación del odontograma inicial contra el actual.
 */
class DiferenciaOdontogramaTest {

    private final ProyectorOdontograma proyector = new ProyectorOdontograma();

    @Test
    void detectaAgregadasEliminadasYModificadas() {
        HallazgoEvento caries36 = sup(CARIES, 36, 0, Superficie.O);
        HallazgoEvento caries11 = sup(CARIES, 11, 1, Superficie.V);
        HallazgoEvento restauracion36 = sup(RESTAURACION, 36, 100, EstadoHallazgo.BUENO, Superficie.O);
        HallazgoEvento ausente46 = pieza(PIEZA_AUSENTE, 46, 101);
        HallazgoEvento anulacionDeCaries11 = anulado(caries11);
        HallazgoEvento puente = rango(PROTESIS_FIJA, 24, 26, 102, EstadoHallazgo.BUENO);

        List<HallazgoEvento> eventos = List.of(caries36, caries11, restauracion36, ausente46, puente);
        EstadoOdontograma inicial = proyector.proyectar(eventos, T0.plusMinutes(10));
        EstadoOdontograma actual = proyector.proyectar(
                List.of(caries36, anulacionDeCaries11, restauracion36, ausente46, puente), null);

        DiferenciaOdontograma d = DiferenciaOdontograma.entre(inicial, actual);

        assertThat(d.modificadas()).containsExactly(new ClaveCapa(36, 36, Superficie.O, "SUPERFICIE"));
        assertThat(d.agregadas()).containsExactlyInAnyOrder(
                new ClaveCapa(46, 46, null, "ESTADO_PIEZA"), new ClaveCapa(24, 26, null, "PROTESIS_FIJA"));
        assertThat(d.eliminadas()).containsExactly(new ClaveCapa(11, 11, Superficie.V, "SUPERFICIE"));
        assertThat(d.piezasAfectadas()).containsExactly(11, 24, 26, 36, 46);
        assertThat(d.sinCambios()).isFalse();
    }

    @Test
    void mismoEstadoNoTieneCambios() {
        EstadoOdontograma e = estado(sup(CARIES, 36, 0, Superficie.O));
        DiferenciaOdontograma d = DiferenciaOdontograma.entre(e, e);
        assertThat(d.sinCambios()).isTrue();
        assertThat(d.piezasAfectadas()).isEmpty();
    }
}
