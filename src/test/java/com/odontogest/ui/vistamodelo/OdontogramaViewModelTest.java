/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.vistamodelo;

import com.odontogest.dominio.odontograma.ColorHallazgo;
import com.odontogest.dominio.odontograma.ConteoIndice;
import com.odontogest.dominio.odontograma.Denticion;
import com.odontogest.dominio.odontograma.EstadoHallazgo;
import com.odontogest.dominio.odontograma.EstadoOdontograma;
import com.odontogest.dominio.odontograma.NivelSeveridadOms;
import com.odontogest.dominio.odontograma.ReglaColor;
import com.odontogest.dominio.odontograma.Representacion;
import com.odontogest.dominio.odontograma.ResultadoIndice;
import com.odontogest.dominio.odontograma.SimboloHallazgo;
import com.odontogest.dominio.odontograma.Superficie;
import com.odontogest.modelo.EstadoOdontogramaRegistro;
import com.odontogest.modelo.TipoOdontograma;
import com.odontogest.servicio.dto.HallazgoDto;
import com.odontogest.servicio.dto.HallazgoTipoDto;
import com.odontogest.servicio.dto.IndicesVista;
import com.odontogest.servicio.dto.NuevoHallazgo;
import com.odontogest.servicio.dto.OdontogramaVista;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Máquina de estados del clic del odontograma (PLAN §5.1.5), sin levantar JavaFX.
 */
class OdontogramaViewModelTest {

    private static final ResultadoIndice CERO = new ResultadoIndice(0, 0, 0, 0, 28, NivelSeveridadOms.MUY_BAJO);
    private static final HallazgoTipoDto CARIES = tipo(1, Representacion.SUPERFICIE, ReglaColor.SIEMPRE_ROJO, List.of("MB", "CD"));
    private static final HallazgoTipoDto CORONA = tipo(2, Representacion.PIEZA, ReglaColor.POR_ESTADO, List.of("CM", "CMC"));
    private static final HallazgoTipoDto FRACTURA = tipo(3, Representacion.PIEZA, ReglaColor.SIEMPRE_ROJO, List.of());
    private static final HallazgoTipoDto PUENTE = tipo(4, Representacion.RANGO_PIEZAS, ReglaColor.POR_ESTADO, List.of());

    private final OdontogramaViewModel vm = new OdontogramaViewModel();
    private final List<NuevoHallazgo> registrados = new ArrayList<>();
    private final List<String> avisos = new ArrayList<>();
    private Optional<String> respuestaSigla = Optional.of("CD");

    private static HallazgoTipoDto tipo(long id, Representacion r, ReglaColor color, List<String> siglas) {
        return new HallazgoTipoDto(id, "T" + id, "Tipo " + id, null, siglas, r, SimboloHallazgo.RELLENO, color, "CAPA",
                false, ConteoIndice.NINGUNO, ConteoIndice.NINGUNO, true, 0, false);
    }

    private static OdontogramaVista vista(boolean borrador, List<HallazgoDto> historial) {
        return new OdontogramaVista(1, 50L, 9L, TipoOdontograma.INICIAL,
                borrador ? EstadoOdontogramaRegistro.BORRADOR : EstadoOdontogramaRegistro.CERRADO, Denticion.PERMANENTE,
                null, null, LocalDateTime.now(), null, null, true, null, EstadoOdontograma.vacio(), historial,
                new IndicesVista(CERO, CERO, true));
    }

    private static HallazgoDto hallazgo(long id, int pieza, Integer fin, boolean vigente, boolean anulado) {
        return new HallazgoDto(id, 50, 1, "T1", "Tipo", Representacion.PIEZA, pieza, fin, Set.of(), null, null, null,
                LocalDateTime.now().minusMinutes(id), "Dr.", ColorHallazgo.ROJO, anulado, null, null, vigente, true);
    }

    @BeforeEach
    void preparar() {
        vm.setRegistrador(registrados::add);
        vm.setAvisador(avisos::add);
        vm.setSelectorSigla(t -> respuestaSigla);
        vm.vista.set(vista(true, List.of()));
        vm.soloLectura.set(false);
    }

    @Test
    void sinHerramientaElClicSeleccionaLaPieza() {
        vm.clic(36, Superficie.O);
        assertThat(vm.piezaSeleccionada.get()).isEqualTo(36);
        assertThat(registrados).isEmpty();
    }

    @Test
    void superficiesDeUnaPiezaSeRegistranJuntasConEnter() {
        vm.herramienta.set(CARIES);
        vm.clic(36, Superficie.O);
        vm.clic(36, Superficie.M);
        vm.clic(36, Superficie.D);
        vm.clic(36, Superficie.D);                    // alterna: se quita
        assertThat(vm.superficiesPendientes).containsExactly(Superficie.O, Superficie.M);
        assertThat(vm.registrarPendiente()).isTrue();

        NuevoHallazgo n = registrados.get(0);
        assertThat(n.piezaFdi()).isEqualTo(36);
        assertThat(n.superficies()).containsExactlyInAnyOrder(Superficie.O, Superficie.M);
        assertThat(n.sigla()).isEqualTo("CD");
        assertThat(n.estado()).as("caries no usa estado").isNull();
        assertThat(n.odontogramaId()).isEqualTo(50);
    }

    @Test
    void cambiarDePiezaDescartaLaSeleccionConAviso() {
        vm.herramienta.set(CARIES);
        vm.clic(36, Superficie.O);
        vm.clic(46, Superficie.V);
        assertThat(vm.piezaPendiente.get()).isEqualTo(46);
        assertThat(vm.superficiesPendientes).containsExactly(Superficie.V);
        assertThat(avisos).anyMatch(a -> a.contains("pieza 36"));
        vm.clic(46, null);
        assertThat(avisos).anyMatch(a -> a.contains("superficie"));
    }

    @Test
    void piezaConSiglaYEstado() {
        vm.herramienta.set(CORONA);
        vm.estadoActivo.set(EstadoHallazgo.MALO);
        respuestaSigla = Optional.of("CMC");
        vm.clic(11, null);
        assertThat(registrados).singleElement().satisfies(n -> {
            assertThat(n.estado()).isEqualTo(EstadoHallazgo.MALO);
            assertThat(n.sigla()).isEqualTo("CMC");
            assertThat(n.superficies()).isEmpty();
        });

        respuestaSigla = Optional.empty();
        vm.clic(12, null);
        assertThat(registrados).as("cancelar la sigla cancela el registro").hasSize(1);
        vm.herramienta.set(FRACTURA);
        vm.clic(12, null);
        assertThat(registrados.get(1).sigla()).isNull();
    }

    @Test
    void rangoConDosClics() {
        vm.herramienta.set(PUENTE);
        vm.clic(14, null);
        assertThat(vm.inicioRango.get()).isEqualTo(14);
        vm.clic(14, null);
        assertThat(vm.inicioRango.get()).as("segundo clic en la misma pieza cancela").isNull();
        vm.clic(14, null);
        vm.clic(16, null);
        assertThat(registrados).singleElement().satisfies(n -> {
            assertThat(n.piezaFdi()).isEqualTo(14);
            assertThat(n.piezaFdiFin()).isEqualTo(16);
            assertThat(n.estado()).isEqualTo(EstadoHallazgo.BUENO);
        });
        assertThat(vm.inicioRango.get()).isNull();
    }

    @Test
    void escapeLimpiaYLuegoVuelveAlModoSeleccion() {
        vm.herramienta.set(CARIES);
        vm.clic(36, Superficie.O);
        vm.cancelar();
        assertThat(vm.superficiesPendientes).isEmpty();
        assertThat(vm.herramienta.get()).isEqualTo(CARIES);
        vm.cancelar();
        assertThat(vm.herramienta.get()).isNull();
    }

    @Test
    void enSoloLecturaNoSeRegistraNada() {
        vm.soloLectura.set(true);
        vm.herramienta.set(FRACTURA);
        vm.clic(21, null);
        assertThat(registrados).isEmpty();
        assertThat(vm.piezaSeleccionada.get()).isEqualTo(21);
        vm.movido(21, Superficie.O);
        assertThat(vm.superficieHover.get()).isNull();
    }

    @Test
    void elInspectorOrdenaVigentesReemplazadosYAnulados() {
        vm.vista.set(vista(false, List.of(
                hallazgo(1, 36, null, false, true),
                hallazgo(2, 36, null, true, false),
                hallazgo(3, 36, null, false, false),
                hallazgo(4, 14, 16, true, false),
                hallazgo(5, 11, null, true, false))));
        vm.piezaSeleccionada.set(36);
        assertThat(vm.hallazgosPieza).extracting(HallazgoDto::id).containsExactly(2L, 3L, 1L);
        vm.piezaSeleccionada.set(15);
        assertThat(vm.hallazgosPieza).as("el rango 14–16 cubre la 15").extracting(HallazgoDto::id).containsExactly(4L);
    }

    @Test
    void laSuperposicionReflejaElEstadoDeInteraccion() {
        vm.herramienta.set(CARIES);
        vm.movido(36, Superficie.M);
        vm.clic(36, Superficie.O);
        var s = vm.superposicion();
        assertThat(s.piezaHover()).isEqualTo(36);
        assertThat(s.superficieHover()).isEqualTo(Superficie.M);
        assertThat(s.pendientes()).containsExactly(Superficie.O);
        assertThat(s.diferencias()).isEmpty();
    }
}
