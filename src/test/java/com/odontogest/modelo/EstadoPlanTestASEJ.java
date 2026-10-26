package com.odontogest.modelo;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

class EstadoPlanTestASEJ {

    @Test
    void caminoNormalASEJ() {
        assertThat(EstadoPlanASEJ.BORRADOR.puedePasarAASEJ(EstadoPlanASEJ.PROPUESTO)).isTrue();
        assertThat(EstadoPlanASEJ.PROPUESTO.puedePasarAASEJ(EstadoPlanASEJ.ACEPTADO)).isTrue();
        assertThat(EstadoPlanASEJ.ACEPTADO.puedePasarAASEJ(EstadoPlanASEJ.EN_CURSO)).isTrue();
        assertThat(EstadoPlanASEJ.EN_CURSO.puedePasarAASEJ(EstadoPlanASEJ.FINALIZADO)).isTrue();
    }

    @Test
    void noSeSaltanEstadosASEJ() {
        assertThat(EstadoPlanASEJ.BORRADOR.puedePasarAASEJ(EstadoPlanASEJ.ACEPTADO)).isFalse();
        assertThat(EstadoPlanASEJ.PROPUESTO.puedePasarAASEJ(EstadoPlanASEJ.EN_CURSO)).isFalse();
        assertThat(EstadoPlanASEJ.ACEPTADO.puedePasarAASEJ(EstadoPlanASEJ.FINALIZADO)).isFalse();
        assertThat(EstadoPlanASEJ.ACEPTADO.puedePasarAASEJ(EstadoPlanASEJ.BORRADOR)).as("aceptado ya no vuelve a borrador").isFalse();
    }

    @Test
    void finalizadoYAnuladoSonFinalesASEJ() {
        for (EstadoPlanASEJ eASEJ : EstadoPlanASEJ.values()) {
            assertThat(EstadoPlanASEJ.FINALIZADO.puedePasarAASEJ(eASEJ)).isFalse();
            assertThat(EstadoPlanASEJ.ANULADO.puedePasarAASEJ(eASEJ)).isFalse();
        }
        assertThat(EnumSet.complementOf(EnumSet.of(EstadoPlanASEJ.FINALIZADO, EstadoPlanASEJ.ANULADO)))
                .allMatch(eASEJ -> eASEJ.puedePasarAASEJ(EstadoPlanASEJ.ANULADO));
    }

    @Test
    void capacidadesPorEstadoASEJ() {
        assertThat(EnumSet.allOf(EstadoPlanASEJ.class).stream().filter(EstadoPlanASEJ::editableASEJ)).containsExactly(EstadoPlanASEJ.BORRADOR);
        assertThat(EnumSet.allOf(EstadoPlanASEJ.class).stream().filter(EstadoPlanASEJ::admiteCronogramaASEJ))
                .containsExactly(EstadoPlanASEJ.PROPUESTO, EstadoPlanASEJ.ACEPTADO, EstadoPlanASEJ.EN_CURSO);
        assertThat(EnumSet.allOf(EstadoPlanASEJ.class).stream().filter(EstadoPlanASEJ::enEjecucionASEJ))
                .containsExactly(EstadoPlanASEJ.ACEPTADO, EstadoPlanASEJ.EN_CURSO);
    }
}
