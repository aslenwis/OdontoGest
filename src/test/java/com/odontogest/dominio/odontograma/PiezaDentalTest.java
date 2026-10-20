/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.odontograma;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Numeración FDI, metadatos de la pieza y orden de dibujo.
 */
class PiezaDentalTest {

    @ParameterizedTest
    @ValueSource(ints = {11, 18, 21, 28, 31, 38, 41, 48, 51, 55, 61, 65, 71, 75, 81, 85})
    void aceptaPiezasValidas(int fdi) {
        assertThat(PiezaDental.esValida(fdi)).isTrue();
        assertThat(PiezaDental.de(fdi).fdi()).isEqualTo(fdi);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 10, 19, 20, 49, 50, 56, 66, 76, 86, 90, 91, 9, 100, -11})
    void rechazaPiezasInvalidas(int fdi) {
        assertThat(PiezaDental.esValida(fdi)).isFalse();
        assertThatThrownBy(() -> PiezaDental.de(fdi))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(String.valueOf(fdi));
    }

    @ParameterizedTest
    @CsvSource({
            "11, INCISIVO, UNIRRADICULAR, SUPERIOR, true, false",
            "13, CANINO, UNIRRADICULAR, SUPERIOR, true, false",
            "14, PREMOLAR, BIRRADICULAR, SUPERIOR, true, false",
            "15, PREMOLAR, UNIRRADICULAR, SUPERIOR, true, false",
            "24, PREMOLAR, BIRRADICULAR, SUPERIOR, false, false",
            "34, PREMOLAR, UNIRRADICULAR, INFERIOR, false, false",
            "36, MOLAR, MULTIRRADICULAR, INFERIOR, false, false",
            "48, MOLAR, MULTIRRADICULAR, INFERIOR, true, false",
            "53, CANINO, UNIRRADICULAR, SUPERIOR, true, true",
            "54, MOLAR, MULTIRRADICULAR, SUPERIOR, true, true",
            "65, MOLAR, MULTIRRADICULAR, SUPERIOR, false, true",
            "71, INCISIVO, UNIRRADICULAR, INFERIOR, false, true",
            "85, MOLAR, MULTIRRADICULAR, INFERIOR, true, true"
    })
    void metadatosDeLaPieza(int fdi, TipoPieza tipo, TipoRadicular radicular, Arcada arcada,
                            boolean ladoDerecho, boolean decidua) {
        PiezaDental p = PiezaDental.de(fdi);
        assertThat(p.tipo()).isEqualTo(tipo);
        assertThat(p.tipoRadicular()).isEqualTo(radicular);
        assertThat(p.arcada()).isEqualTo(arcada);
        assertThat(p.ladoDerechoPaciente()).isEqualTo(ladoDerecho);
        assertThat(p.esDecidua()).isEqualTo(decidua);
        assertThat(p.cuadrante().numero).isEqualTo(fdi / 10);
        assertThat(p.posicion()).isEqualTo(fdi % 10);
    }

    @Test
    void anterioresYTercerasMolares() {
        assertThat(PiezaDental.de(13).esAnterior()).isTrue();
        assertThat(PiezaDental.de(14).esAnterior()).isFalse();
        assertThat(PiezaDental.de(52).esAnterior()).isTrue();
        assertThat(PiezaDental.de(18).esTerceraMolar()).isTrue();
        assertThat(PiezaDental.de(17).esTerceraMolar()).isFalse();
    }

    @Test
    void ordenDeDibujoDePermanentesYDeciduas() {
        List<Integer> permanentes = PiezaDental.permanentes().stream().map(PiezaDental::fdi).toList();
        assertThat(permanentes).hasSize(32).startsWith(18, 17, 16, 15, 14, 13, 12, 11, 21, 22)
                .endsWith(41, 31, 32, 33, 34, 35, 36, 37, 38);
        assertThat(permanentes.subList(16, 24)).containsExactly(48, 47, 46, 45, 44, 43, 42, 41);

        List<Integer> deciduas = PiezaDental.deciduas().stream().map(PiezaDental::fdi).toList();
        assertThat(deciduas).containsExactly(55, 54, 53, 52, 51, 61, 62, 63, 64, 65,
                85, 84, 83, 82, 81, 71, 72, 73, 74, 75);
    }

    @Test
    void filasPorDenticion() {
        assertThat(PiezaDental.filas(Denticion.PERMANENTE)).hasSize(2);
        assertThat(PiezaDental.filas(Denticion.DECIDUA)).hasSize(2);
        List<List<PiezaDental>> mixta = PiezaDental.filas(Denticion.MIXTA);
        assertThat(mixta).extracting(List::size).containsExactly(16, 10, 10, 16);
        assertThat(mixta.get(1).get(0).fdi()).isEqualTo(55);
        assertThat(mixta.get(2).get(0).fdi()).isEqualTo(85);
        assertThat(new HashSet<>(mixta.stream().flatMap(List::stream).toList())).hasSize(52);
    }

    @Test
    void ordenGlobalYComparacion() {
        assertThat(PiezaDental.de(18).ordenDibujo()).isZero();
        assertThat(PiezaDental.de(55).ordenDibujo()).isEqualTo(16);
        assertThat(PiezaDental.de(85).ordenDibujo()).isEqualTo(26);
        assertThat(PiezaDental.de(38).ordenDibujo()).isEqualTo(51);
        assertThat(PiezaDental.de(11)).isLessThan(PiezaDental.de(21));
        assertThat(PiezaDental.de(48)).isGreaterThan(PiezaDental.de(28));
    }

    @Test
    void contiguidadEnLaFilaDeDibujo() {
        assertThat(PiezaDental.de(11).esContiguaA(PiezaDental.de(21))).isTrue();
        assertThat(PiezaDental.de(11).esContiguaA(PiezaDental.de(12))).isTrue();
        assertThat(PiezaDental.de(31).esContiguaA(PiezaDental.de(41))).isTrue();
        assertThat(PiezaDental.de(11).esContiguaA(PiezaDental.de(13))).isFalse();
        assertThat(PiezaDental.de(11).esContiguaA(PiezaDental.de(41))).isFalse();
        assertThat(PiezaDental.de(51).esContiguaA(PiezaDental.de(11))).isFalse();
        assertThat(PiezaDental.de(18).esContiguaA(PiezaDental.de(28))).isFalse();
        assertThat(PiezaDental.de(14).mismaArcada(PiezaDental.de(26))).isTrue();
        assertThat(PiezaDental.de(14).mismaArcada(PiezaDental.de(46))).isFalse();
    }

    @Test
    void denticionAdmitePiezas() {
        assertThat(Denticion.PERMANENTE.admite(PiezaDental.de(36))).isTrue();
        assertThat(Denticion.PERMANENTE.admite(PiezaDental.de(75))).isFalse();
        assertThat(Denticion.DECIDUA.admite(PiezaDental.de(75))).isTrue();
        assertThat(Denticion.DECIDUA.admite(PiezaDental.de(36))).isFalse();
        assertThat(Denticion.MIXTA.admite(PiezaDental.de(36))).isTrue();
        assertThat(Denticion.MIXTA.admite(PiezaDental.de(75))).isTrue();
    }

    @Test
    void cuadranteInvalido() {
        assertThatThrownBy(() -> Cuadrante.de(9)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Cuadrante.de(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rotuloDeSuperficiesSegunLaPieza() {
        PiezaDental incisivoSuperior = PiezaDental.de(11);
        PiezaDental molarInferior = PiezaDental.de(36);
        assertThat(Superficie.O.etiqueta(incisivoSuperior)).isEqualTo("I");
        assertThat(Superficie.O.etiqueta(molarInferior)).isEqualTo("O");
        assertThat(Superficie.L.etiqueta(incisivoSuperior)).isEqualTo("P");
        assertThat(Superficie.L.etiqueta(molarInferior)).isEqualTo("L");
        assertThat(Superficie.M.etiqueta(molarInferior)).isEqualTo("M");
        assertThat(Superficie.O.nombre(incisivoSuperior)).isEqualTo("Incisal");
        assertThat(Superficie.O.nombre(molarInferior)).isEqualTo("Oclusal");
        assertThat(Superficie.L.nombre(incisivoSuperior)).isEqualTo("Palatino");
        assertThat(Superficie.L.nombre(molarInferior)).isEqualTo("Lingual");
        assertThat(Superficie.D.nombre(molarInferior)).isEqualTo("Distal");
        assertThat(Superficie.V.nombre(molarInferior)).isEqualTo("Vestibular");
        assertThat(Superficie.M.nombre(molarInferior)).isEqualTo("Mesial");
    }
}
