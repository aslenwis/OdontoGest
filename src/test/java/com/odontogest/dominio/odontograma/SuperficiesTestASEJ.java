package com.odontogest.dominio.odontograma;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SuperficiesTestASEJ {

    @Test
    void idaYVueltaParaTodasLasMascarasASEJ() {
        for (int mASEJ = 0; mASEJ <= SuperficiesASEJ.MASCARA_MAXIMAASEJ; mASEJ++) {
            assertThat(SuperficiesASEJ.aMascaraASEJ(SuperficiesASEJ.desdeMascaraASEJ(mASEJ))).isEqualTo(mASEJ);
        }
    }

    @Test
    void mesioOclusoDistalEsSieteASEJ() {
        assertThat(SuperficiesASEJ.aMascaraASEJ(EnumSet.of(SuperficieASEJ.M, SuperficieASEJ.O, SuperficieASEJ.D))).isEqualTo(7);
        assertThat(SuperficiesASEJ.desdeMascaraASEJ(24)).containsExactly(SuperficieASEJ.V, SuperficieASEJ.L);
        assertThat(SuperficiesASEJ.aMascaraASEJ(null)).isZero();
    }

    @Test
    void rechazaMascarasFueraDeRangoASEJ() {
        assertThatThrownBy(() -> SuperficiesASEJ.desdeMascaraASEJ(32)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SuperficiesASEJ.desdeMascaraASEJ(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
