package com.odontogest.ui.vistamodelo;

import com.odontogest.dominio.odontograma.ColorHallazgoASEJ;
import com.odontogest.dominio.odontograma.ConteoIndiceASEJ;
import com.odontogest.dominio.odontograma.DenticionASEJ;
import com.odontogest.dominio.odontograma.EstadoHallazgoASEJ;
import com.odontogest.dominio.odontograma.EstadoOdontogramaASEJ;
import com.odontogest.dominio.odontograma.NivelSeveridadOmsASEJ;
import com.odontogest.dominio.odontograma.ReglaColorASEJ;
import com.odontogest.dominio.odontograma.RepresentacionASEJ;
import com.odontogest.dominio.odontograma.ResultadoIndiceASEJ;
import com.odontogest.dominio.odontograma.SimboloHallazgoASEJ;
import com.odontogest.dominio.odontograma.SuperficieASEJ;
import com.odontogest.modelo.EstadoOdontogramaRegistroASEJ;
import com.odontogest.modelo.TipoOdontogramaASEJ;
import com.odontogest.servicio.dto.HallazgoDtoASEJ;
import com.odontogest.servicio.dto.HallazgoTipoDtoASEJ;
import com.odontogest.servicio.dto.IndicesVistaASEJ;
import com.odontogest.servicio.dto.NuevoHallazgoASEJ;
import com.odontogest.servicio.dto.OdontogramaVistaASEJ;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class OdontogramaViewModelTestASEJ {

    private static final ResultadoIndiceASEJ CEROASEJ = new ResultadoIndiceASEJ(0, 0, 0, 0, 28, NivelSeveridadOmsASEJ.MUY_BAJO);
    private static final HallazgoTipoDtoASEJ CARIESASEJ = tipoASEJ(1, RepresentacionASEJ.SUPERFICIE, ReglaColorASEJ.SIEMPRE_ROJO, List.of("MB", "CD"));
    private static final HallazgoTipoDtoASEJ CORONAASEJ = tipoASEJ(2, RepresentacionASEJ.PIEZA, ReglaColorASEJ.POR_ESTADO, List.of("CM", "CMC"));
    private static final HallazgoTipoDtoASEJ FRACTURAASEJ = tipoASEJ(3, RepresentacionASEJ.PIEZA, ReglaColorASEJ.SIEMPRE_ROJO, List.of());
    private static final HallazgoTipoDtoASEJ PUENTEASEJ = tipoASEJ(4, RepresentacionASEJ.RANGO_PIEZAS, ReglaColorASEJ.POR_ESTADO, List.of());

    private final OdontogramaViewModelASEJ vmASEJ = new OdontogramaViewModelASEJ();
    private final List<NuevoHallazgoASEJ> registradosASEJ = new ArrayList<>();
    private final List<String> avisosASEJ = new ArrayList<>();
    private Optional<String> respuestaSiglaASEJ = Optional.of("CD");

    private static HallazgoTipoDtoASEJ tipoASEJ(long idASEJ, RepresentacionASEJ rASEJ, ReglaColorASEJ colorASEJ, List<String> siglasASEJ) {
        return new HallazgoTipoDtoASEJ(idASEJ, "T" + idASEJ, "Tipo " + idASEJ, null, siglasASEJ, rASEJ, SimboloHallazgoASEJ.RELLENO, colorASEJ, "CAPA",
                false, ConteoIndiceASEJ.NINGUNO, ConteoIndiceASEJ.NINGUNO, true, 0, false);
    }

    private static OdontogramaVistaASEJ vistaASEJ(boolean borradorASEJ, List<HallazgoDtoASEJ> historialASEJ) {
        return new OdontogramaVistaASEJ(1, 50L, 9L, TipoOdontogramaASEJ.INICIAL,
                borradorASEJ ? EstadoOdontogramaRegistroASEJ.BORRADOR : EstadoOdontogramaRegistroASEJ.CERRADO, DenticionASEJ.PERMANENTE,
                null, null, LocalDateTime.now(), null, null, true, null, EstadoOdontogramaASEJ.vacioASEJ(), historialASEJ,
                new IndicesVistaASEJ(CEROASEJ, CEROASEJ, true));
    }

    private static HallazgoDtoASEJ hallazgoASEJ(long idASEJ, int piezaASEJ, Integer finASEJ, boolean vigenteASEJ, boolean anuladoASEJ) {
        return new HallazgoDtoASEJ(idASEJ, 50, 1, "T1", "Tipo", RepresentacionASEJ.PIEZA, piezaASEJ, finASEJ, Set.of(), null, null, null,
                LocalDateTime.now().minusMinutes(idASEJ), "Dr.", ColorHallazgoASEJ.ROJO, anuladoASEJ, null, null, vigenteASEJ, true);
    }

    @BeforeEach
    void prepararASEJ() {
        vmASEJ.setRegistradorASEJ(registradosASEJ::add);
        vmASEJ.setAvisadorASEJ(avisosASEJ::add);
        vmASEJ.setSelectorSiglaASEJ(tASEJ -> respuestaSiglaASEJ);
        vmASEJ.vistaASEJ.set(vistaASEJ(true, List.of()));
        vmASEJ.soloLecturaASEJ.set(false);
    }

    @Test
    void sinHerramientaElClicSeleccionaLaPiezaASEJ() {
        vmASEJ.clicASEJ(36, SuperficieASEJ.O);
        assertThat(vmASEJ.piezaSeleccionadaASEJ.get()).isEqualTo(36);
        assertThat(registradosASEJ).isEmpty();
    }

    @Test
    void superficiesDeUnaPiezaSeRegistranJuntasConEnterASEJ() {
        vmASEJ.herramientaASEJ.set(CARIESASEJ);
        vmASEJ.clicASEJ(36, SuperficieASEJ.O);
        vmASEJ.clicASEJ(36, SuperficieASEJ.M);
        vmASEJ.clicASEJ(36, SuperficieASEJ.D);
        vmASEJ.clicASEJ(36, SuperficieASEJ.D);
        assertThat(vmASEJ.superficiesPendientesASEJ).containsExactly(SuperficieASEJ.O, SuperficieASEJ.M);
        assertThat(vmASEJ.registrarPendienteASEJ()).isTrue();

        NuevoHallazgoASEJ nASEJ = registradosASEJ.get(0);
        assertThat(nASEJ.piezaFdiASEJ()).isEqualTo(36);
        assertThat(nASEJ.superficiesASEJ()).containsExactlyInAnyOrder(SuperficieASEJ.O, SuperficieASEJ.M);
        assertThat(nASEJ.siglaASEJ()).isEqualTo("CD");
        assertThat(nASEJ.estadoASEJ()).as("caries no usa estado").isNull();
        assertThat(nASEJ.odontogramaIdASEJ()).isEqualTo(50);
    }

    @Test
    void cambiarDePiezaDescartaLaSeleccionConAvisoASEJ() {
        vmASEJ.herramientaASEJ.set(CARIESASEJ);
        vmASEJ.clicASEJ(36, SuperficieASEJ.O);
        vmASEJ.clicASEJ(46, SuperficieASEJ.V);
        assertThat(vmASEJ.piezaPendienteASEJ.get()).isEqualTo(46);
        assertThat(vmASEJ.superficiesPendientesASEJ).containsExactly(SuperficieASEJ.V);
        assertThat(avisosASEJ).anyMatch(aASEJ -> aASEJ.contains("pieza 36"));
        vmASEJ.clicASEJ(46, null);
        assertThat(avisosASEJ).anyMatch(aASEJ -> aASEJ.contains("superficie"));
    }

    @Test
    void piezaConSiglaYEstadoASEJ() {
        vmASEJ.herramientaASEJ.set(CORONAASEJ);
        vmASEJ.estadoActivoASEJ.set(EstadoHallazgoASEJ.MALO);
        respuestaSiglaASEJ = Optional.of("CMC");
        vmASEJ.clicASEJ(11, null);
        assertThat(registradosASEJ).singleElement().satisfies(nASEJ -> {
            assertThat(nASEJ.estadoASEJ()).isEqualTo(EstadoHallazgoASEJ.MALO);
            assertThat(nASEJ.siglaASEJ()).isEqualTo("CMC");
            assertThat(nASEJ.superficiesASEJ()).isEmpty();
        });

        respuestaSiglaASEJ = Optional.empty();
        vmASEJ.clicASEJ(12, null);
        assertThat(registradosASEJ).as("cancelar la sigla cancela el registro").hasSize(1);
        vmASEJ.herramientaASEJ.set(FRACTURAASEJ);
        vmASEJ.clicASEJ(12, null);
        assertThat(registradosASEJ.get(1).siglaASEJ()).isNull();
    }

    @Test
    void rangoConDosClicsASEJ() {
        vmASEJ.herramientaASEJ.set(PUENTEASEJ);
        vmASEJ.clicASEJ(14, null);
        assertThat(vmASEJ.inicioRangoASEJ.get()).isEqualTo(14);
        vmASEJ.clicASEJ(14, null);
        assertThat(vmASEJ.inicioRangoASEJ.get()).as("segundo clic en la misma pieza cancela").isNull();
        vmASEJ.clicASEJ(14, null);
        vmASEJ.clicASEJ(16, null);
        assertThat(registradosASEJ).singleElement().satisfies(nASEJ -> {
            assertThat(nASEJ.piezaFdiASEJ()).isEqualTo(14);
            assertThat(nASEJ.piezaFdiFinASEJ()).isEqualTo(16);
            assertThat(nASEJ.estadoASEJ()).isEqualTo(EstadoHallazgoASEJ.BUENO);
        });
        assertThat(vmASEJ.inicioRangoASEJ.get()).isNull();
    }

    @Test
    void escapeLimpiaYLuegoVuelveAlModoSeleccionASEJ() {
        vmASEJ.herramientaASEJ.set(CARIESASEJ);
        vmASEJ.clicASEJ(36, SuperficieASEJ.O);
        vmASEJ.cancelarASEJ();
        assertThat(vmASEJ.superficiesPendientesASEJ).isEmpty();
        assertThat(vmASEJ.herramientaASEJ.get()).isEqualTo(CARIESASEJ);
        vmASEJ.cancelarASEJ();
        assertThat(vmASEJ.herramientaASEJ.get()).isNull();
    }

    @Test
    void enSoloLecturaNoSeRegistraNadaASEJ() {
        vmASEJ.soloLecturaASEJ.set(true);
        vmASEJ.herramientaASEJ.set(FRACTURAASEJ);
        vmASEJ.clicASEJ(21, null);
        assertThat(registradosASEJ).isEmpty();
        assertThat(vmASEJ.piezaSeleccionadaASEJ.get()).isEqualTo(21);
        vmASEJ.movidoASEJ(21, SuperficieASEJ.O);
        assertThat(vmASEJ.superficieHoverASEJ.get()).isNull();
    }

    @Test
    void elInspectorOrdenaVigentesReemplazadosYAnuladosASEJ() {
        vmASEJ.vistaASEJ.set(vistaASEJ(false, List.of(
                hallazgoASEJ(1, 36, null, false, true),
                hallazgoASEJ(2, 36, null, true, false),
                hallazgoASEJ(3, 36, null, false, false),
                hallazgoASEJ(4, 14, 16, true, false),
                hallazgoASEJ(5, 11, null, true, false))));
        vmASEJ.piezaSeleccionadaASEJ.set(36);
        assertThat(vmASEJ.hallazgosPiezaASEJ).extracting(HallazgoDtoASEJ::idASEJ).containsExactly(2L, 3L, 1L);
        vmASEJ.piezaSeleccionadaASEJ.set(15);
        assertThat(vmASEJ.hallazgosPiezaASEJ).as("el rango 14–16 cubre la 15").extracting(HallazgoDtoASEJ::idASEJ).containsExactly(4L);
    }

    @Test
    void laSuperposicionReflejaElEstadoDeInteraccionASEJ() {
        vmASEJ.herramientaASEJ.set(CARIESASEJ);
        vmASEJ.movidoASEJ(36, SuperficieASEJ.M);
        vmASEJ.clicASEJ(36, SuperficieASEJ.O);
        var sASEJ = vmASEJ.superposicionASEJ();
        assertThat(sASEJ.piezaHoverASEJ()).isEqualTo(36);
        assertThat(sASEJ.superficieHoverASEJ()).isEqualTo(SuperficieASEJ.M);
        assertThat(sASEJ.pendientesASEJ()).containsExactly(SuperficieASEJ.O);
        assertThat(sASEJ.diferenciasASEJ()).isEmpty();
    }
}
