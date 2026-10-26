package com.odontogest.dominio.odontograma;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PromedioIndicesTestASEJ {

    private static ResultadoIndiceASEJ indiceASEJ(int cASEJ, int pASEJ, int oASEJ) {
        int totalASEJ = cASEJ + pASEJ + oASEJ;
        return new ResultadoIndiceASEJ(cASEJ, pASEJ, oASEJ, totalASEJ, 28, NivelSeveridadOmsASEJ.deASEJ(totalASEJ));
    }

    @ParameterizedTest
    @CsvSource({"0,INFANCIA", "5,INFANCIA", "6,ESCOLAR", "11,ESCOLAR", "12,ADOLESCENCIA", "17,ADOLESCENCIA",
            "18,JOVEN", "29,JOVEN", "30,ADULTO", "44,ADULTO", "45,ADULTO_MAYOR_JOVEN", "59,ADULTO_MAYOR_JOVEN",
            "60,ADULTO_MAYOR", "104,ADULTO_MAYOR"})
    void grupoPorEdadASEJ(int edadASEJ, GrupoEtarioASEJ esperadoASEJ) {
        assertThat(GrupoEtarioASEJ.deASEJ(edadASEJ)).isEqualTo(esperadoASEJ);
    }

    @Test
    void edadNegativa_esErrorASEJ() {
        assertThatThrownBy(() -> GrupoEtarioASEJ.deASEJ(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void indicesRepresentativosPorGrupoASEJ() {
        assertThat(GrupoEtarioASEJ.INFANCIA.aplicaCpodASEJ).isFalse();
        assertThat(GrupoEtarioASEJ.INFANCIA.aplicaCeodASEJ).isTrue();
        assertThat(GrupoEtarioASEJ.ESCOLAR.aplicaCpodASEJ).as("dentición mixta: ambos").isTrue();
        assertThat(GrupoEtarioASEJ.ESCOLAR.aplicaCeodASEJ).isTrue();
        assertThat(GrupoEtarioASEJ.ADULTO.aplicaCeodASEJ).isFalse();
    }

    @Test
    void promediosPorGrupo_redondeadosADosDecimalesASEJ() {
        PromedioIndicesASEJ pASEJ = new PromedioIndicesASEJ();
        pASEJ.agregarASEJ(35, indiceASEJ(2, 1, 3), indiceASEJ(0, 0, 0));
        pASEJ.agregarASEJ(40, indiceASEJ(1, 0, 0), indiceASEJ(0, 0, 0));
        pASEJ.agregarASEJ(42, indiceASEJ(0, 0, 0), indiceASEJ(0, 0, 0));
        pASEJ.agregarASEJ(8, indiceASEJ(1, 0, 0), indiceASEJ(3, 1, 0));
        assertThat(pASEJ.pacientesASEJ()).isEqualTo(4);

        List<PromedioIndicesASEJ.GrupoASEJ> gruposASEJ = pASEJ.resultadoASEJ();
        assertThat(gruposASEJ).hasSize(GrupoEtarioASEJ.values().length);
        PromedioIndicesASEJ.GrupoASEJ adultosASEJ = gruposASEJ.get(GrupoEtarioASEJ.ADULTO.ordinal());
        assertThat(adultosASEJ.pacientesASEJ()).isEqualTo(3);
        assertThat(adultosASEJ.cpodASEJ()).isEqualTo(2.33);
        assertThat(adultosASEJ.cASEJ()).isEqualTo(1.0);
        assertThat(adultosASEJ.pASEJ()).isEqualTo(0.33);
        assertThat(adultosASEJ.oASEJ()).isEqualTo(1.0);
        assertThat(adultosASEJ.nivelCpodASEJ()).isEqualTo(NivelSeveridadOmsASEJ.BAJO);

        PromedioIndicesASEJ.GrupoASEJ escolaresASEJ = gruposASEJ.get(GrupoEtarioASEJ.ESCOLAR.ordinal());
        assertThat(escolaresASEJ.ceodASEJ()).isEqualTo(4.0);
        assertThat(escolaresASEJ.cpodASEJ()).isEqualTo(1.0);

        PromedioIndicesASEJ.GrupoASEJ vacioASEJ = gruposASEJ.get(GrupoEtarioASEJ.JOVEN.ordinal());
        assertThat(vacioASEJ.pacientesASEJ()).isZero();
        assertThat(vacioASEJ.nivelCpodASEJ()).isNull();
    }

    @Test
    void sinIndices_esErrorASEJ() {
        PromedioIndicesASEJ pASEJ = new PromedioIndicesASEJ();
        assertThatThrownBy(() -> pASEJ.agregarASEJ(30, null, indiceASEJ(0, 0, 0))).isInstanceOf(NullPointerException.class);
    }
}
