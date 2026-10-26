package com.odontogest.dominio.odontograma;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.odontogest.dominio.odontograma.CatalogoPruebaASEJ.*;
import static org.assertj.core.api.Assertions.assertThat;

class DiferenciaOdontogramaTestASEJ {

    private final ProyectorOdontogramaASEJ proyectorASEJ = new ProyectorOdontogramaASEJ();

    @Test
    void detectaAgregadasEliminadasYModificadasASEJ() {
        HallazgoEventoASEJ caries36ASEJ = supASEJ(CARIESASEJ, 36, 0, SuperficieASEJ.O);
        HallazgoEventoASEJ caries11ASEJ = supASEJ(CARIESASEJ, 11, 1, SuperficieASEJ.V);
        HallazgoEventoASEJ restauracion36ASEJ = supASEJ(RESTAURACIONASEJ, 36, 100, EstadoHallazgoASEJ.BUENO, SuperficieASEJ.O);
        HallazgoEventoASEJ ausente46ASEJ = piezaASEJ(PIEZA_AUSENTEASEJ, 46, 101);
        HallazgoEventoASEJ anulacionDeCaries11ASEJ = anuladoASEJ(caries11ASEJ);
        HallazgoEventoASEJ puenteASEJ = rangoASEJ(PROTESIS_FIJAASEJ, 24, 26, 102, EstadoHallazgoASEJ.BUENO);

        List<HallazgoEventoASEJ> eventosASEJ = List.of(caries36ASEJ, caries11ASEJ, restauracion36ASEJ, ausente46ASEJ, puenteASEJ);
        EstadoOdontogramaASEJ inicialASEJ = proyectorASEJ.proyectarASEJ(eventosASEJ, T0ASEJ.plusMinutes(10));
        EstadoOdontogramaASEJ actualASEJ = proyectorASEJ.proyectarASEJ(
                List.of(caries36ASEJ, anulacionDeCaries11ASEJ, restauracion36ASEJ, ausente46ASEJ, puenteASEJ), null);

        DiferenciaOdontogramaASEJ dASEJ = DiferenciaOdontogramaASEJ.entreASEJ(inicialASEJ, actualASEJ);

        assertThat(dASEJ.modificadasASEJ()).containsExactly(new ClaveCapaASEJ(36, 36, SuperficieASEJ.O, "SUPERFICIE"));
        assertThat(dASEJ.agregadasASEJ()).containsExactlyInAnyOrder(
                new ClaveCapaASEJ(46, 46, null, "ESTADO_PIEZA"), new ClaveCapaASEJ(24, 26, null, "PROTESIS_FIJA"));
        assertThat(dASEJ.eliminadasASEJ()).containsExactly(new ClaveCapaASEJ(11, 11, SuperficieASEJ.V, "SUPERFICIE"));
        assertThat(dASEJ.piezasAfectadasASEJ()).containsExactly(11, 24, 26, 36, 46);
        assertThat(dASEJ.sinCambiosASEJ()).isFalse();
    }

    @Test
    void mismoEstadoNoTieneCambiosASEJ() {
        EstadoOdontogramaASEJ eASEJ = estadoASEJ(supASEJ(CARIESASEJ, 36, 0, SuperficieASEJ.O));
        DiferenciaOdontogramaASEJ dASEJ = DiferenciaOdontogramaASEJ.entreASEJ(eASEJ, eASEJ);
        assertThat(dASEJ.sinCambiosASEJ()).isTrue();
        assertThat(dASEJ.piezasAfectadasASEJ()).isEmpty();
    }
}
