package com.odontogest.dominio.odontograma;

import org.junit.jupiter.api.Test;

import static com.odontogest.dominio.odontograma.CatalogoPruebaASEJ.*;
import static org.assertj.core.api.Assertions.assertThat;

class RangosTestASEJ {

    private final CalculadoraIndicesASEJ calculadoraASEJ = new CalculadoraIndicesASEJ();

    @Test
    void rangoDePiezasEnLaFilaDeDibujoASEJ() {
        assertThat(PiezaDentalASEJ.rangoASEJ(PiezaDentalASEJ.deASEJ(18), PiezaDentalASEJ.deASEJ(28))).hasSize(16)
                .startsWith(PiezaDentalASEJ.deASEJ(18)).endsWith(PiezaDentalASEJ.deASEJ(28));
        assertThat(PiezaDentalASEJ.rangoASEJ(PiezaDentalASEJ.deASEJ(26), PiezaDentalASEJ.deASEJ(24)))
                .extracting(PiezaDentalASEJ::fdiASEJ).containsExactly(24, 25, 26);
        assertThat(PiezaDentalASEJ.rangoASEJ(PiezaDentalASEJ.deASEJ(13), PiezaDentalASEJ.deASEJ(23)))
                .extracting(PiezaDentalASEJ::fdiASEJ).containsExactly(13, 12, 11, 21, 22, 23);
        assertThat(PiezaDentalASEJ.rangoASEJ(PiezaDentalASEJ.deASEJ(55), PiezaDentalASEJ.deASEJ(65))).hasSize(10);
        assertThat(PiezaDentalASEJ.rangoASEJ(PiezaDentalASEJ.deASEJ(16), PiezaDentalASEJ.deASEJ(55)))
                .as("filas distintas: solo los extremos").extracting(PiezaDentalASEJ::fdiASEJ).containsExactly(16, 55);
        assertThat(PiezaDentalASEJ.rangoASEJ(PiezaDentalASEJ.deASEJ(36), PiezaDentalASEJ.deASEJ(36))).extracting(PiezaDentalASEJ::fdiASEJ).containsExactly(36);
    }

    @Test
    void edentuloTotalCuentaComoPerdidaEnCadaPiezaCubiertaASEJ() {
        EstadoOdontogramaASEJ eASEJ = estadoASEJ(rangoASEJ(EDENTULO_TOTALASEJ, 18, 28, 0, null));

        ResultadoIndiceASEJ rASEJ = calculadoraASEJ.cpodASEJ(eASEJ, true);
        assertThat(rASEJ.pASEJ()).as("14 piezas superiores sin terceras molares").isEqualTo(14);
        assertThat(calculadoraASEJ.cpodASEJ(eASEJ, false).pASEJ()).isEqualTo(16);
        assertThat(eASEJ.rangosQueCubrenASEJ(14)).hasSize(1);
        assertThat(eASEJ.rangosQueCubrenASEJ(36)).isEmpty();
        assertThat(eASEJ.superficiesOcultasASEJ(14)).isTrue();
        assertThat(eASEJ.superficiesOcultasASEJ(36)).isFalse();
    }

    @Test
    void cariesPreviaBajoEdentuloNoCuentaComoCariadaASEJ() {
        EstadoOdontogramaASEJ eASEJ = estadoASEJ(supASEJ(CARIESASEJ, 16, 0, SuperficieASEJ.O), rangoASEJ(EDENTULO_TOTALASEJ, 18, 28, 60, null));

        assertThat(eASEJ.hallazgosVisiblesASEJ(16)).extracting(hASEJ -> hASEJ.tipoASEJ().codigoASEJ()).containsExactly("EDENTULO_TOTAL");
        assertThat(eASEJ.piezaASEJ(16).orElseThrow().hallazgosASEJ()).as("la caries sigue en el estado de la pieza").hasSize(1);
        ResultadoIndiceASEJ rASEJ = calculadoraASEJ.cpodASEJ(eASEJ, true);
        assertThat(rASEJ.cASEJ()).isZero();
        assertThat(rASEJ.pASEJ()).isEqualTo(14);
    }

    @Test
    void protesisFijaNoOcultaNiCuentaPeroSeVeEnLasPiezasCubiertasASEJ() {
        EstadoOdontogramaASEJ eASEJ = estadoASEJ(rangoASEJ(PROTESIS_FIJAASEJ, 14, 16, 0, EstadoHallazgoASEJ.BUENO));

        assertThat(eASEJ.hallazgosVisiblesASEJ(15)).extracting(hASEJ -> hASEJ.tipoASEJ().codigoASEJ()).containsExactly("PROTESIS_FIJA");
        assertThat(eASEJ.superficiesOcultasASEJ(15)).isFalse();
        assertThat(calculadoraASEJ.cpodASEJ(eASEJ, true).totalASEJ()).isZero();
    }

    @Test
    void piezaNoErupcionadaNoCuentaEnElCpodASEJ() {
        EstadoOdontogramaASEJ eASEJ = estadoASEJ(piezaASEJ(PIEZA_NO_ERUPCIONADAASEJ, 18, 0), piezaASEJ(PIEZA_AUSENTEASEJ, 36, 0));
        ResultadoIndiceASEJ rASEJ = calculadoraASEJ.cpodASEJ(eASEJ, false);
        assertThat(rASEJ.pASEJ()).as("solo la ausente por caries (DEX)").isEqualTo(1);
        assertThat(eASEJ.superficiesOcultasASEJ(18)).isTrue();
    }
}
