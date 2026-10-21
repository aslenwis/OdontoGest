/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.vistamodelo;

import com.odontogest.dominio.odontograma.EstadoHallazgo;
import com.odontogest.dominio.odontograma.PiezaDental;
import com.odontogest.dominio.odontograma.ReglaColor;
import com.odontogest.dominio.odontograma.Representacion;
import com.odontogest.dominio.odontograma.Superficie;
import com.odontogest.servicio.dto.DiferenciaVista;
import com.odontogest.servicio.dto.HallazgoDto;
import com.odontogest.servicio.dto.HallazgoTipoDto;
import com.odontogest.servicio.dto.NuevoHallazgo;
import com.odontogest.servicio.dto.OdontogramaVista;
import com.odontogest.ui.componente.RenderizadorOdontograma;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Estado de la pestaña Odontograma y máquina de estados del clic (PLAN §5.1.5):
 * <ul>
 *   <li>sin herramienta: el clic selecciona la pieza y el inspector lista sus hallazgos;</li>
 *   <li>herramienta de superficie: el clic alterna superficies de una misma pieza; Enter registra un solo hallazgo
 *       con todas; cambiar de pieza descarta la selección previa con aviso;</li>
 *   <li>herramienta de pieza: el clic registra (pidiendo la sigla si el tipo tiene opciones);</li>
 *   <li>entre piezas / rango: el primer clic fija el inicio, el segundo registra.</li>
 * </ul>
 * No llama servicios: entrega el {@link NuevoHallazgo} al {@code registrador} que configura el controlador.
 * Solo usa propiedades de JavaFX, por eso se prueba sin levantar la interfaz.
 */
@Component
public class OdontogramaViewModel {

    public final ObjectProperty<OdontogramaVista> vista = new SimpleObjectProperty<>(this, "vista");
    /** {@code null} = modo selección. */
    public final ObjectProperty<HallazgoTipoDto> herramienta = new SimpleObjectProperty<>(this, "herramienta");
    public final ObjectProperty<EstadoHallazgo> estadoActivo = new SimpleObjectProperty<>(this, "estadoActivo", EstadoHallazgo.BUENO);
    public final ObjectProperty<Integer> piezaSeleccionada = new SimpleObjectProperty<>(this, "piezaSeleccionada");
    public final ObjectProperty<Integer> piezaHover = new SimpleObjectProperty<>(this, "piezaHover");
    public final ObjectProperty<Superficie> superficieHover = new SimpleObjectProperty<>(this, "superficieHover");
    public final ObjectProperty<Integer> piezaPendiente = new SimpleObjectProperty<>(this, "piezaPendiente");
    public final ObservableSet<Superficie> superficiesPendientes = FXCollections.observableSet(new LinkedHashSet<>());
    public final ObjectProperty<Integer> inicioRango = new SimpleObjectProperty<>(this, "inicioRango");
    /** Odontograma cerrado, momento pasado o usuario sin permiso. */
    public final BooleanProperty soloLectura = new SimpleBooleanProperty(this, "soloLectura", true);
    public final BooleanProperty modoComparar = new SimpleBooleanProperty(this, "modoComparar");
    public final ObjectProperty<DiferenciaVista> diferencia = new SimpleObjectProperty<>(this, "diferencia");
    /** Momento de la línea de tiempo; {@code null} = actual. */
    public final ObjectProperty<LocalDateTime> fechaVista = new SimpleObjectProperty<>(this, "fechaVista");
    /** Inspector: hallazgos de la pieza seleccionada (vigentes, luego reemplazados y anulados). */
    public final ObservableList<HallazgoDto> hallazgosPieza = FXCollections.observableArrayList();

    private Consumer<NuevoHallazgo> registrador = n -> { };
    private Consumer<String> avisador = m -> { };
    private Function<HallazgoTipoDto, Optional<String>> selectorSigla = t -> Optional.of(t.siglasOpciones().get(0));

    public OdontogramaViewModel() {
        vista.addListener((obs, a, b) -> {
            limpiarPendientes();
            actualizarInspector();
        });
        piezaSeleccionada.addListener((obs, a, b) -> actualizarInspector());
        herramienta.addListener((obs, a, b) -> limpiarPendientes());
        soloLectura.addListener((obs, a, b) -> {
            if (b) {
                limpiarPendientes();
            }
        });
    }

    public void setRegistrador(Consumer<NuevoHallazgo> registrador) {
        this.registrador = registrador;
    }

    public void setAvisador(Consumer<String> avisador) {
        this.avisador = avisador;
    }

    public void setSelectorSigla(Function<HallazgoTipoDto, Optional<String>> selectorSigla) {
        this.selectorSigla = selectorSigla;
    }

    // ------------------------------------------------------------------ interacción

    public void movido(Integer fdi, Superficie superficie) {
        piezaHover.set(fdi);
        HallazgoTipoDto t = herramienta.get();
        boolean resaltarSuperficie = !soloLectura.get() && t != null && t.representacion() == Representacion.SUPERFICIE;
        superficieHover.set(resaltarSuperficie ? superficie : null);
    }

    public void clic(Integer fdi, Superficie superficie) {
        if (fdi == null) {
            return;
        }
        HallazgoTipoDto t = herramienta.get();
        if (t == null || soloLectura.get()) {
            piezaSeleccionada.set(fdi);
            return;
        }
        switch (t.representacion()) {
            case SUPERFICIE -> alternarSuperficie(fdi, superficie);
            case PIEZA -> registrar(t, fdi, null, Set.of());
            case ENTRE_PIEZAS, RANGO_PIEZAS -> {
                Integer inicio = inicioRango.get();
                if (inicio == null) {
                    inicioRango.set(fdi);
                    avisador.accept("Pieza " + fdi + " marcada: haga clic en la otra pieza del hallazgo.");
                } else if (inicio.equals(fdi)) {
                    inicioRango.set(null);
                } else {
                    inicioRango.set(null);
                    registrar(t, inicio, fdi, Set.of());
                }
            }
        }
    }

    private void alternarSuperficie(int fdi, Superficie superficie) {
        if (superficie == null) {
            avisador.accept("Haga clic sobre una superficie de la corona.");
            return;
        }
        Integer pendiente = piezaPendiente.get();
        if (pendiente != null && pendiente != fdi && !superficiesPendientes.isEmpty()) {
            avisador.accept("Se descartó la selección de la pieza " + pendiente + ": las superficies deben ser de una misma pieza.");
            superficiesPendientes.clear();
        }
        piezaPendiente.set(fdi);
        piezaSeleccionada.set(fdi);
        if (!superficiesPendientes.remove(superficie)) {
            superficiesPendientes.add(superficie);
        }
        if (superficiesPendientes.isEmpty()) {
            piezaPendiente.set(null);
        }
    }

    /** Registra las superficies pendientes (tecla Enter o botón Registrar). */
    public boolean registrarPendiente() {
        HallazgoTipoDto t = herramienta.get();
        if (t == null || piezaPendiente.get() == null || superficiesPendientes.isEmpty() || soloLectura.get()) {
            return false;
        }
        registrar(t, piezaPendiente.get(), null, EnumSet.copyOf(superficiesPendientes));
        return true;
    }

    /** Tecla Esc: descarta lo pendiente; si no había nada pendiente, vuelve al modo selección. */
    public void cancelar() {
        boolean habiaPendiente = !superficiesPendientes.isEmpty() || inicioRango.get() != null;
        limpiarPendientes();
        if (!habiaPendiente) {
            herramienta.set(null);
        }
    }

    public boolean hayPendiente() {
        return !superficiesPendientes.isEmpty();
    }

    private void registrar(HallazgoTipoDto t, int fdi, Integer fin, Set<Superficie> superficies) {
        OdontogramaVista v = vista.get();
        if (v == null || v.odontogramaId() == null) {
            return;
        }
        String sigla = null;
        if (!t.siglasOpciones().isEmpty()) {
            Optional<String> elegida = selectorSigla.apply(t);
            if (elegida.isEmpty()) {
                return;
            }
            sigla = elegida.get();
        }
        EstadoHallazgo estado = t.reglaColor() == ReglaColor.POR_ESTADO ? estadoActivo.get() : null;
        piezaSeleccionada.set(fdi);
        registrador.accept(new NuevoHallazgo(v.odontogramaId(), t.id(), fdi, fin, superficies, estado, sigla, null));
    }

    private void limpiarPendientes() {
        superficiesPendientes.clear();
        piezaPendiente.set(null);
        inicioRango.set(null);
    }

    // ------------------------------------------------------------------ derivados

    /** Lo que el lienzo dibuja encima del estado. */
    public RenderizadorOdontograma.Superposicion superposicion() {
        DiferenciaVista d = diferencia.get();
        Set<Integer> cambios = modoComparar.get() && d != null ? d.diferencia().piezasAfectadas() : Set.of();
        return new RenderizadorOdontograma.Superposicion(piezaHover.get(), superficieHover.get(), piezaSeleccionada.get(),
                piezaPendiente.get(), Set.copyOf(superficiesPendientes), inicioRango.get(), cambios);
    }

    /** ¿El hallazgo afecta a la pieza? Los de rango, a todas las piezas que cubren. */
    public static boolean afecta(HallazgoDto h, int fdi) {
        if (h.pieza() == fdi) {
            return true;
        }
        return h.piezaFin() != null && PiezaDental.esValida(fdi)
                && PiezaDental.rango(PiezaDental.de(h.pieza()), PiezaDental.de(h.piezaFin())).contains(PiezaDental.de(fdi));
    }

    private void actualizarInspector() {
        OdontogramaVista v = vista.get();
        Integer fdi = piezaSeleccionada.get();
        if (v == null || fdi == null) {
            hallazgosPieza.clear();
            return;
        }
        List<HallazgoDto> lista = v.historial().stream()
                .filter(h -> afecta(h, fdi))
                .sorted(Comparator.comparingInt((HallazgoDto h) -> h.vigente() ? 0 : h.anulado() ? 2 : 1)
                        .thenComparing(HallazgoDto::registradoEn, Comparator.reverseOrder()))
                .toList();
        hallazgosPieza.setAll(lista);
    }
}
