package com.odontogest.dominio.odontograma;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PiezaDentalTestASEJ {

    @ParameterizedTest
    @ValueSource(ints = {11, 18, 21, 28, 31, 38, 41, 48, 51, 55, 61, 65, 71, 75, 81, 85})
    void aceptaPiezasValidasASEJ(int fdiASEJ) {
        assertThat(PiezaDentalASEJ.esValidaASEJ(fdiASEJ)).isTrue();
        assertThat(PiezaDentalASEJ.deASEJ(fdiASEJ).fdiASEJ()).isEqualTo(fdiASEJ);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 10, 19, 20, 49, 50, 56, 66, 76, 86, 90, 91, 9, 100, -11})
    void rechazaPiezasInvalidasASEJ(int fdiASEJ) {
        assertThat(PiezaDentalASEJ.esValidaASEJ(fdiASEJ)).isFalse();
        assertThatThrownBy(() -> PiezaDentalASEJ.deASEJ(fdiASEJ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(String.valueOf(fdiASEJ));
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
    void metadatosDeLaPiezaASEJ(int fdiASEJ, TipoPiezaASEJ tipoASEJ, TipoRadicularASEJ radicularASEJ, ArcadaASEJ arcadaASEJ,
                            boolean ladoDerechoASEJ, boolean deciduaASEJ) {
        PiezaDentalASEJ pASEJ = PiezaDentalASEJ.deASEJ(fdiASEJ);
        assertThat(pASEJ.tipoASEJ()).isEqualTo(tipoASEJ);
        assertThat(pASEJ.tipoRadicularASEJ()).isEqualTo(radicularASEJ);
        assertThat(pASEJ.arcadaASEJ()).isEqualTo(arcadaASEJ);
        assertThat(pASEJ.ladoDerechoPacienteASEJ()).isEqualTo(ladoDerechoASEJ);
        assertThat(pASEJ.esDeciduaASEJ()).isEqualTo(deciduaASEJ);
        assertThat(pASEJ.cuadranteASEJ().numeroASEJ).isEqualTo(fdiASEJ / 10);
        assertThat(pASEJ.posicionASEJ()).isEqualTo(fdiASEJ % 10);
    }

    @Test
    void anterioresYTercerasMolaresASEJ() {
        assertThat(PiezaDentalASEJ.deASEJ(13).esAnteriorASEJ()).isTrue();
        assertThat(PiezaDentalASEJ.deASEJ(14).esAnteriorASEJ()).isFalse();
        assertThat(PiezaDentalASEJ.deASEJ(52).esAnteriorASEJ()).isTrue();
        assertThat(PiezaDentalASEJ.deASEJ(18).esTerceraMolarASEJ()).isTrue();
        assertThat(PiezaDentalASEJ.deASEJ(17).esTerceraMolarASEJ()).isFalse();
    }

    @Test
    void ordenDeDibujoDePermanentesYDeciduasASEJ() {
        List<Integer> permanentesASEJ = PiezaDentalASEJ.permanentesASEJ().stream().map(PiezaDentalASEJ::fdiASEJ).toList();
        assertThat(permanentesASEJ).hasSize(32).startsWith(18, 17, 16, 15, 14, 13, 12, 11, 21, 22)
                .endsWith(41, 31, 32, 33, 34, 35, 36, 37, 38);
        assertThat(permanentesASEJ.subList(16, 24)).containsExactly(48, 47, 46, 45, 44, 43, 42, 41);

        List<Integer> deciduasASEJ = PiezaDentalASEJ.deciduasASEJ().stream().map(PiezaDentalASEJ::fdiASEJ).toList();
        assertThat(deciduasASEJ).containsExactly(55, 54, 53, 52, 51, 61, 62, 63, 64, 65,
                85, 84, 83, 82, 81, 71, 72, 73, 74, 75);
    }

    @Test
    void filasPorDenticionASEJ() {
        assertThat(PiezaDentalASEJ.filasASEJ(DenticionASEJ.PERMANENTE)).hasSize(2);
        assertThat(PiezaDentalASEJ.filasASEJ(DenticionASEJ.DECIDUA)).hasSize(2);
        List<List<PiezaDentalASEJ>> mixtaASEJ = PiezaDentalASEJ.filasASEJ(DenticionASEJ.MIXTA);
        assertThat(mixtaASEJ).extracting(List::size).containsExactly(16, 10, 10, 16);
        assertThat(mixtaASEJ.get(1).get(0).fdiASEJ()).isEqualTo(55);
        assertThat(mixtaASEJ.get(2).get(0).fdiASEJ()).isEqualTo(85);
        assertThat(new HashSet<>(mixtaASEJ.stream().flatMap(List::stream).toList())).hasSize(52);
    }

    @Test
    void ordenGlobalYComparacionASEJ() {
        assertThat(PiezaDentalASEJ.deASEJ(18).ordenDibujoASEJ()).isZero();
        assertThat(PiezaDentalASEJ.deASEJ(55).ordenDibujoASEJ()).isEqualTo(16);
        assertThat(PiezaDentalASEJ.deASEJ(85).ordenDibujoASEJ()).isEqualTo(26);
        assertThat(PiezaDentalASEJ.deASEJ(38).ordenDibujoASEJ()).isEqualTo(51);
        assertThat(PiezaDentalASEJ.deASEJ(11)).isLessThan(PiezaDentalASEJ.deASEJ(21));
        assertThat(PiezaDentalASEJ.deASEJ(48)).isGreaterThan(PiezaDentalASEJ.deASEJ(28));
    }

    @Test
    void contiguidadEnLaFilaDeDibujoASEJ() {
        assertThat(PiezaDentalASEJ.deASEJ(11).esContiguaAASEJ(PiezaDentalASEJ.deASEJ(21))).isTrue();
        assertThat(PiezaDentalASEJ.deASEJ(11).esContiguaAASEJ(PiezaDentalASEJ.deASEJ(12))).isTrue();
        assertThat(PiezaDentalASEJ.deASEJ(31).esContiguaAASEJ(PiezaDentalASEJ.deASEJ(41))).isTrue();
        assertThat(PiezaDentalASEJ.deASEJ(11).esContiguaAASEJ(PiezaDentalASEJ.deASEJ(13))).isFalse();
        assertThat(PiezaDentalASEJ.deASEJ(11).esContiguaAASEJ(PiezaDentalASEJ.deASEJ(41))).isFalse();
        assertThat(PiezaDentalASEJ.deASEJ(51).esContiguaAASEJ(PiezaDentalASEJ.deASEJ(11))).isFalse();
        assertThat(PiezaDentalASEJ.deASEJ(18).esContiguaAASEJ(PiezaDentalASEJ.deASEJ(28))).isFalse();
        assertThat(PiezaDentalASEJ.deASEJ(14).mismaArcadaASEJ(PiezaDentalASEJ.deASEJ(26))).isTrue();
        assertThat(PiezaDentalASEJ.deASEJ(14).mismaArcadaASEJ(PiezaDentalASEJ.deASEJ(46))).isFalse();
    }

    @Test
    void denticionAdmitePiezasASEJ() {
        assertThat(DenticionASEJ.PERMANENTE.admiteASEJ(PiezaDentalASEJ.deASEJ(36))).isTrue();
        assertThat(DenticionASEJ.PERMANENTE.admiteASEJ(PiezaDentalASEJ.deASEJ(75))).isFalse();
        assertThat(DenticionASEJ.DECIDUA.admiteASEJ(PiezaDentalASEJ.deASEJ(75))).isTrue();
        assertThat(DenticionASEJ.DECIDUA.admiteASEJ(PiezaDentalASEJ.deASEJ(36))).isFalse();
        assertThat(DenticionASEJ.MIXTA.admiteASEJ(PiezaDentalASEJ.deASEJ(36))).isTrue();
        assertThat(DenticionASEJ.MIXTA.admiteASEJ(PiezaDentalASEJ.deASEJ(75))).isTrue();
    }

    @Test
    void cuadranteInvalidoASEJ() {
        assertThatThrownBy(() -> CuadranteASEJ.deASEJ(9)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CuadranteASEJ.deASEJ(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rotuloDeSuperficiesSegunLaPiezaASEJ() {
        PiezaDentalASEJ incisivoSuperiorASEJ = PiezaDentalASEJ.deASEJ(11);
        PiezaDentalASEJ molarInferiorASEJ = PiezaDentalASEJ.deASEJ(36);
        assertThat(SuperficieASEJ.O.etiquetaASEJ(incisivoSuperiorASEJ)).isEqualTo("I");
        assertThat(SuperficieASEJ.O.etiquetaASEJ(molarInferiorASEJ)).isEqualTo("O");
        assertThat(SuperficieASEJ.L.etiquetaASEJ(incisivoSuperiorASEJ)).isEqualTo("P");
        assertThat(SuperficieASEJ.L.etiquetaASEJ(molarInferiorASEJ)).isEqualTo("L");
        assertThat(SuperficieASEJ.M.etiquetaASEJ(molarInferiorASEJ)).isEqualTo("M");
        assertThat(SuperficieASEJ.O.nombreASEJ(incisivoSuperiorASEJ)).isEqualTo("Incisal");
        assertThat(SuperficieASEJ.O.nombreASEJ(molarInferiorASEJ)).isEqualTo("Oclusal");
        assertThat(SuperficieASEJ.L.nombreASEJ(incisivoSuperiorASEJ)).isEqualTo("Palatino");
        assertThat(SuperficieASEJ.L.nombreASEJ(molarInferiorASEJ)).isEqualTo("Lingual");
        assertThat(SuperficieASEJ.D.nombreASEJ(molarInferiorASEJ)).isEqualTo("Distal");
        assertThat(SuperficieASEJ.V.nombreASEJ(molarInferiorASEJ)).isEqualTo("Vestibular");
        assertThat(SuperficieASEJ.M.nombreASEJ(molarInferiorASEJ)).isEqualTo("Mesial");
    }
}
