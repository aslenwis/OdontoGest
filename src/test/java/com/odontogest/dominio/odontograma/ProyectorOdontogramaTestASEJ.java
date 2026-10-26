package com.odontogest.dominio.odontograma;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

import static com.odontogest.dominio.odontograma.CatalogoPruebaASEJ.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProyectorOdontogramaTestASEJ {

    private final ProyectorOdontogramaASEJ proyectorASEJ = new ProyectorOdontogramaASEJ();

    @Test
    void sinEventosElEstadoEstaVacioASEJ() {
        EstadoOdontogramaASEJ eASEJ = proyectorASEJ.proyectarASEJ(List.of(), null);
        assertThat(eASEJ.estaVacioASEJ()).isTrue();
        assertThat(eASEJ.piezasASEJ()).isEmpty();
        assertThat(EstadoOdontogramaASEJ.vacioASEJ().clavesASEJ()).isEmpty();
    }

    @Test
    void restauracionReemplazaALaCariesEnLaMismaCapaASEJ() {
        HallazgoEventoASEJ cariesASEJ = supASEJ(CARIESASEJ, 36, 0, SuperficieASEJ.O);
        HallazgoEventoASEJ restauracionASEJ = supASEJ(RESTAURACIONASEJ, 36, 10, EstadoHallazgoASEJ.BUENO, SuperficieASEJ.O);

        EstadoOdontogramaASEJ eASEJ = estadoASEJ(cariesASEJ, restauracionASEJ);

        assertThat(eASEJ.enSuperficieASEJ(36, SuperficieASEJ.O, "SUPERFICIE")).contains(restauracionASEJ);
        assertThat(eASEJ.piezaASEJ(36).orElseThrow().hallazgosASEJ()).containsExactly(restauracionASEJ);
    }

    @Test
    void elOrdenDeLaListaNoImportaGanaElMasRecienteASEJ() {
        HallazgoEventoASEJ cariesASEJ = supASEJ(CARIESASEJ, 36, 0, SuperficieASEJ.O);
        HallazgoEventoASEJ restauracionASEJ = supASEJ(RESTAURACIONASEJ, 36, 10, EstadoHallazgoASEJ.BUENO, SuperficieASEJ.O);

        EstadoOdontogramaASEJ eASEJ = proyectorASEJ.proyectarASEJ(List.of(restauracionASEJ, cariesASEJ), null);

        assertThat(eASEJ.enSuperficieASEJ(36, SuperficieASEJ.O, "SUPERFICIE")).contains(restauracionASEJ);
    }

    @Test
    void aIgualFechaDesempataElIdASEJ() {
        HallazgoEventoASEJ aASEJ = supASEJ(CARIESASEJ, 36, 5, SuperficieASEJ.O);
        HallazgoEventoASEJ bASEJ = supASEJ(RESTAURACIONASEJ, 36, 5, EstadoHallazgoASEJ.MALO, SuperficieASEJ.O);
        assertThat(proyectorASEJ.proyectarASEJ(List.of(bASEJ, aASEJ), null).enSuperficieASEJ(36, SuperficieASEJ.O, "SUPERFICIE"))
                .contains(bASEJ);
    }

    @Test
    void sellanteConviveConRestauracionPorqueUsaOtraCapaASEJ() {
        HallazgoEventoASEJ restauracionASEJ = supASEJ(RESTAURACIONASEJ, 46, 0, EstadoHallazgoASEJ.BUENO, SuperficieASEJ.M);
        HallazgoEventoASEJ sellanteASEJ = supASEJ(SELLANTEASEJ, 46, 5, EstadoHallazgoASEJ.BUENO, SuperficieASEJ.O, SuperficieASEJ.M);

        EstadoOdontogramaASEJ eASEJ = estadoASEJ(restauracionASEJ, sellanteASEJ);

        assertThat(eASEJ.piezaASEJ(46).orElseThrow().hallazgosASEJ()).containsExactly(restauracionASEJ, sellanteASEJ);
        assertThat(eASEJ.piezaASEJ(46).orElseThrow().enSuperficieASEJ(SuperficieASEJ.M)).containsExactly(restauracionASEJ, sellanteASEJ);
    }

    @Test
    void unaRestauracionMODEsUnSoloHallazgoEnTresClavesASEJ() {
        HallazgoEventoASEJ modASEJ = supASEJ(RESTAURACIONASEJ, 26, 0, EstadoHallazgoASEJ.BUENO, SuperficieASEJ.M, SuperficieASEJ.O, SuperficieASEJ.D);

        EstadoOdontogramaASEJ eASEJ = estadoASEJ(modASEJ);

        assertThat(eASEJ.clavesASEJ()).hasSize(3);
        assertThat(eASEJ.hallazgosVigentesASEJ()).containsExactly(modASEJ);
        assertThat(eASEJ.piezaASEJ(26).orElseThrow().hallazgosASEJ()).containsExactly(modASEJ);
        assertThat(SuperficiesASEJ.aMascaraASEJ(modASEJ.superficiesASEJ())).isEqualTo(7);
    }

    @Test
    void restauracionParcialSobreCariesDejaVigenteLaParteNoRestauradaASEJ() {
        HallazgoEventoASEJ cariesASEJ = supASEJ(CARIESASEJ, 16, 0, SuperficieASEJ.O, SuperficieASEJ.M);
        HallazgoEventoASEJ restauracionASEJ = supASEJ(RESTAURACIONASEJ, 16, 10, EstadoHallazgoASEJ.BUENO, SuperficieASEJ.O);

        EstadoOdontogramaASEJ eASEJ = estadoASEJ(cariesASEJ, restauracionASEJ);

        assertThat(eASEJ.enSuperficieASEJ(16, SuperficieASEJ.O, "SUPERFICIE")).contains(restauracionASEJ);
        assertThat(eASEJ.enSuperficieASEJ(16, SuperficieASEJ.M, "SUPERFICIE")).contains(cariesASEJ);
        assertThat(eASEJ.piezaASEJ(16).orElseThrow().hallazgosASEJ()).containsExactly(cariesASEJ, restauracionASEJ);
    }

    @Test
    void losAnuladosNoCuentanASEJ() {
        HallazgoEventoASEJ cariesASEJ = supASEJ(CARIESASEJ, 36, 0, SuperficieASEJ.O);
        HallazgoEventoASEJ restauracionASEJ = anuladoASEJ(supASEJ(RESTAURACIONASEJ, 36, 10, EstadoHallazgoASEJ.BUENO, SuperficieASEJ.O));

        EstadoOdontogramaASEJ eASEJ = estadoASEJ(cariesASEJ, restauracionASEJ);

        assertThat(eASEJ.enSuperficieASEJ(36, SuperficieASEJ.O, "SUPERFICIE")).contains(cariesASEJ);
    }

    @Test
    void lineaDeTiempoReconstruyeElEstadoEnUnaFechaASEJ() {
        HallazgoEventoASEJ cariesASEJ = supASEJ(CARIESASEJ, 36, 0, SuperficieASEJ.O);
        HallazgoEventoASEJ restauracionASEJ = supASEJ(RESTAURACIONASEJ, 36, 60, EstadoHallazgoASEJ.BUENO, SuperficieASEJ.O);
        List<HallazgoEventoASEJ> eventosASEJ = List.of(cariesASEJ, restauracionASEJ);

        assertThat(proyectorASEJ.proyectarASEJ(eventosASEJ, T0ASEJ.plusMinutes(30)).enSuperficieASEJ(36, SuperficieASEJ.O, "SUPERFICIE"))
                .contains(cariesASEJ);
        assertThat(proyectorASEJ.proyectarASEJ(eventosASEJ, T0ASEJ.plusMinutes(60)).enSuperficieASEJ(36, SuperficieASEJ.O, "SUPERFICIE"))
                .as("el corte es inclusivo").contains(restauracionASEJ);
        assertThat(proyectorASEJ.proyectarASEJ(eventosASEJ, T0ASEJ.minusMinutes(1)).estaVacioASEJ()).isTrue();
    }

    @Test
    void piezaAusenteOcultaLasSuperficiesSinBorrarlasASEJ() {
        HallazgoEventoASEJ cariesASEJ = supASEJ(CARIESASEJ, 36, 0, SuperficieASEJ.O);
        HallazgoEventoASEJ coronaASEJ = piezaASEJ(CORONA_DEFINITIVAASEJ, 36, 1, EstadoHallazgoASEJ.BUENO);
        HallazgoEventoASEJ ausenteASEJ = piezaASEJ(PIEZA_AUSENTEASEJ, 36, 10);

        EstadoPiezaASEJ pASEJ = estadoASEJ(cariesASEJ, coronaASEJ, ausenteASEJ).piezaASEJ(36).orElseThrow();

        assertThat(pASEJ.superficiesOcultasASEJ()).isTrue();
        assertThat(pASEJ.hallazgosASEJ()).containsExactly(cariesASEJ, coronaASEJ, ausenteASEJ);
        assertThat(pASEJ.hallazgosVisiblesASEJ()).containsExactly(coronaASEJ, ausenteASEJ);
        assertThat(pASEJ.enSuperficieASEJ(SuperficieASEJ.O)).isEmpty();
    }

    @Test
    void implanteReemplazaAPiezaAusenteEnLaCapaEstadoPiezaASEJ() {
        HallazgoEventoASEJ ausenteASEJ = piezaASEJ(PIEZA_AUSENTEASEJ, 46, 0);
        HallazgoEventoASEJ implanteASEJ = piezaASEJ(IMPLANTEASEJ, 46, 10, EstadoHallazgoASEJ.BUENO);

        EstadoPiezaASEJ pASEJ = estadoASEJ(ausenteASEJ, implanteASEJ).piezaASEJ(46).orElseThrow();

        assertThat(pASEJ.hallazgosASEJ()).containsExactly(implanteASEJ);
        assertThat(pASEJ.superficiesOcultasASEJ()).isFalse();
    }

    @Test
    void rangosUsanUnaClaveDeMenorAMayorFdiASEJ() {
        HallazgoEventoASEJ puenteASEJ = rangoASEJ(PROTESIS_FIJAASEJ, 16, 14, 0, EstadoHallazgoASEJ.BUENO);
        HallazgoEventoASEJ diastemaASEJ = rangoASEJ(DIASTEMAASEJ, 11, 21, 1, null);

        EstadoOdontogramaASEJ eASEJ = estadoASEJ(puenteASEJ, diastemaASEJ);

        assertThat(eASEJ.clavesASEJ()).containsExactlyInAnyOrder(
                new ClaveCapaASEJ(14, 16, null, "PROTESIS_FIJA"), new ClaveCapaASEJ(11, 21, null, "DIASTEMA"));
        assertThat(eASEJ.rangosASEJ()).containsExactly(puenteASEJ, diastemaASEJ);
        assertThat(eASEJ.piezasASEJ()).isEmpty();
        assertThat(new ClaveCapaASEJ(14, 16, null, "X").esDeRangoASEJ()).isTrue();
    }

    @Test
    void nuevoPuenteSobreElMismoRangoReemplazaAlAnteriorASEJ() {
        HallazgoEventoASEJ buenoASEJ = rangoASEJ(PROTESIS_FIJAASEJ, 14, 16, 0, EstadoHallazgoASEJ.BUENO);
        HallazgoEventoASEJ maloASEJ = rangoASEJ(PROTESIS_FIJAASEJ, 16, 14, 30, EstadoHallazgoASEJ.MALO);
        assertThat(estadoASEJ(buenoASEJ, maloASEJ).rangosASEJ()).containsExactly(maloASEJ);
    }

    @Test
    void piezasEnOrdenDeDibujoASEJ() {
        EstadoOdontogramaASEJ eASEJ = estadoASEJ(supASEJ(CARIESASEJ, 38, 0, SuperficieASEJ.O), supASEJ(CARIESASEJ, 18, 1, SuperficieASEJ.O),
                supASEJ(CARIESASEJ, 21, 2, SuperficieASEJ.O), supASEJ(CARIESASEJ, 55, 3, SuperficieASEJ.O));
        assertThat(eASEJ.piezasASEJ().keySet()).containsExactly(18, 21, 55, 38);
    }

    @Test
    void colorSegunReglaDelCatalogoASEJ() {
        assertThat(supASEJ(CARIESASEJ, 36, 0, SuperficieASEJ.O).colorASEJ()).isEqualTo(ColorHallazgoASEJ.ROJO);
        assertThat(piezaASEJ(PIEZA_AUSENTEASEJ, 36, 0).colorASEJ()).isEqualTo(ColorHallazgoASEJ.AZUL);
        assertThat(supASEJ(RESTAURACIONASEJ, 36, 0, EstadoHallazgoASEJ.BUENO, SuperficieASEJ.O).colorASEJ()).isEqualTo(ColorHallazgoASEJ.AZUL);
        assertThat(supASEJ(RESTAURACIONASEJ, 36, 0, EstadoHallazgoASEJ.MALO, SuperficieASEJ.O).colorASEJ()).isEqualTo(ColorHallazgoASEJ.ROJO);
        assertThat(RESTAURACIONASEJ.usaEstadoASEJ()).isTrue();
        assertThat(CARIESASEJ.usaEstadoASEJ()).isFalse();
    }

    @Test
    void elEventoValidaSuRepresentacionYEsInmutableASEJ() {
        LocalDateTime tASEJ = T0ASEJ;
        assertThatThrownBy(() -> new HallazgoEventoASEJ(1, 1, CARIESASEJ, PiezaDentalASEJ.deASEJ(36), null,
                EnumSet.noneOf(SuperficieASEJ.class), null, null, null, tASEJ, 1, false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HallazgoEventoASEJ(1, 1, DIASTEMAASEJ, PiezaDentalASEJ.deASEJ(11), null,
                null, null, null, null, tASEJ, 1, false))
                .isInstanceOf(NullPointerException.class);

        EnumSet<SuperficieASEJ> originalASEJ = EnumSet.of(SuperficieASEJ.O);
        HallazgoEventoASEJ eASEJ = new HallazgoEventoASEJ(1, 1, CARIESASEJ, PiezaDentalASEJ.deASEJ(36), null, originalASEJ,
                null, null, null, tASEJ, 1, false);
        originalASEJ.add(SuperficieASEJ.M);
        eASEJ.superficiesASEJ().add(SuperficieASEJ.D);
        assertThat(eASEJ.superficiesASEJ()).containsExactly(SuperficieASEJ.O);
        assertThat(eASEJ.esDeSuperficieASEJ()).isTrue();
        assertThat(eASEJ.esDeRangoASEJ()).isFalse();
    }

    @Test
    void elCatalogoRechazaComponentesDeIndiceIncoherentesASEJ() {
        assertThatThrownBy(() -> new TipoHallazgoDefASEJ(1, "X", "X", null, null, RepresentacionASEJ.PIEZA, SimboloHallazgoASEJ.ASPA,
                ReglaColorASEJ.SIEMPRE_AZUL, "C", false, ConteoIndiceASEJ.E, ConteoIndiceASEJ.NINGUNO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TipoHallazgoDefASEJ(1, "X", "X", null, null, RepresentacionASEJ.PIEZA, SimboloHallazgoASEJ.ASPA,
                ReglaColorASEJ.SIEMPRE_AZUL, "C", false, ConteoIndiceASEJ.NINGUNO, ConteoIndiceASEJ.P))
                .isInstanceOf(IllegalArgumentException.class);
        TipoHallazgoDefASEJ sinConteosASEJ = new TipoHallazgoDefASEJ(1, "X", "X", null, null, RepresentacionASEJ.PIEZA, SimboloHallazgoASEJ.ASPA,
                ReglaColorASEJ.SIEMPRE_AZUL, "C", false, null, null);
        assertThat(sinConteosASEJ.conteoCpodASEJ()).isEqualTo(ConteoIndiceASEJ.NINGUNO);
        assertThat(sinConteosASEJ.siglasOpcionesASEJ()).isEmpty();
    }
}
