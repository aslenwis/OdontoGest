package com.odontogest.ui.vistamodelo;

import com.odontogest.dominio.odontograma.EstadoHallazgoASEJ;
import com.odontogest.dominio.odontograma.PiezaDentalASEJ;
import com.odontogest.dominio.odontograma.ReglaColorASEJ;
import com.odontogest.dominio.odontograma.RepresentacionASEJ;
import com.odontogest.dominio.odontograma.SuperficieASEJ;
import com.odontogest.servicio.dto.DiferenciaVistaASEJ;
import com.odontogest.servicio.dto.HallazgoDtoASEJ;
import com.odontogest.servicio.dto.HallazgoTipoDtoASEJ;
import com.odontogest.servicio.dto.NuevoHallazgoASEJ;
import com.odontogest.servicio.dto.OdontogramaVistaASEJ;
import com.odontogest.ui.componente.RenderizadorOdontogramaASEJ;
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

@Component
public class OdontogramaViewModelASEJ {

    public final ObjectProperty<OdontogramaVistaASEJ> vistaASEJ = new SimpleObjectProperty<>(this, "vista");
    public final ObjectProperty<HallazgoTipoDtoASEJ> herramientaASEJ = new SimpleObjectProperty<>(this, "herramienta");
    public final ObjectProperty<EstadoHallazgoASEJ> estadoActivoASEJ = new SimpleObjectProperty<>(this, "estadoActivo", EstadoHallazgoASEJ.BUENO);
    public final ObjectProperty<Integer> piezaSeleccionadaASEJ = new SimpleObjectProperty<>(this, "piezaSeleccionada");
    public final ObjectProperty<Integer> piezaHoverASEJ = new SimpleObjectProperty<>(this, "piezaHover");
    public final ObjectProperty<SuperficieASEJ> superficieHoverASEJ = new SimpleObjectProperty<>(this, "superficieHover");
    public final ObjectProperty<Integer> piezaPendienteASEJ = new SimpleObjectProperty<>(this, "piezaPendiente");
    public final ObservableSet<SuperficieASEJ> superficiesPendientesASEJ = FXCollections.observableSet(new LinkedHashSet<>());
    public final ObjectProperty<Integer> inicioRangoASEJ = new SimpleObjectProperty<>(this, "inicioRango");
    public final BooleanProperty soloLecturaASEJ = new SimpleBooleanProperty(this, "soloLectura", true);
    public final BooleanProperty modoCompararASEJ = new SimpleBooleanProperty(this, "modoComparar");
    public final ObjectProperty<DiferenciaVistaASEJ> diferenciaASEJ = new SimpleObjectProperty<>(this, "diferencia");
    public final ObjectProperty<LocalDateTime> fechaVistaASEJ = new SimpleObjectProperty<>(this, "fechaVista");
    public final ObservableList<HallazgoDtoASEJ> hallazgosPiezaASEJ = FXCollections.observableArrayList();

    private Consumer<NuevoHallazgoASEJ> registradorASEJ = nASEJ -> { };
    private Consumer<String> avisadorASEJ = mASEJ -> { };
    private Function<HallazgoTipoDtoASEJ, Optional<String>> selectorSiglaASEJ = tASEJ -> Optional.of(tASEJ.siglasOpcionesASEJ().get(0));

    public OdontogramaViewModelASEJ() {
        vistaASEJ.addListener((obsASEJ, aASEJ, bASEJ) -> {
            limpiarPendientesASEJ();
            actualizarInspectorASEJ();
        });
        piezaSeleccionadaASEJ.addListener((obsASEJ, aASEJ, bASEJ) -> actualizarInspectorASEJ());
        herramientaASEJ.addListener((obsASEJ, aASEJ, bASEJ) -> limpiarPendientesASEJ());
        soloLecturaASEJ.addListener((obsASEJ, aASEJ, bASEJ) -> {
            if (bASEJ) {
                limpiarPendientesASEJ();
            }
        });
    }

    public void setRegistradorASEJ(Consumer<NuevoHallazgoASEJ> registradorASEJ) {
        this.registradorASEJ = registradorASEJ;
    }

    public void setAvisadorASEJ(Consumer<String> avisadorASEJ) {
        this.avisadorASEJ = avisadorASEJ;
    }

    public void setSelectorSiglaASEJ(Function<HallazgoTipoDtoASEJ, Optional<String>> selectorSiglaASEJ) {
        this.selectorSiglaASEJ = selectorSiglaASEJ;
    }

    public void movidoASEJ(Integer fdiASEJ, SuperficieASEJ superficieASEJ) {
        piezaHoverASEJ.set(fdiASEJ);
        HallazgoTipoDtoASEJ tASEJ = herramientaASEJ.get();
        boolean resaltarSuperficieASEJ = !soloLecturaASEJ.get() && tASEJ != null && tASEJ.representacionASEJ() == RepresentacionASEJ.SUPERFICIE;
        superficieHoverASEJ.set(resaltarSuperficieASEJ ? superficieASEJ : null);
    }

    public void clicASEJ(Integer fdiASEJ, SuperficieASEJ superficieASEJ) {
        if (fdiASEJ == null) {
            return;
        }
        HallazgoTipoDtoASEJ tASEJ = herramientaASEJ.get();
        if (tASEJ == null || soloLecturaASEJ.get()) {
            piezaSeleccionadaASEJ.set(fdiASEJ);
            return;
        }
        switch (tASEJ.representacionASEJ()) {
            case SUPERFICIE -> alternarSuperficieASEJ(fdiASEJ, superficieASEJ);
            case PIEZA -> registrarASEJ(tASEJ, fdiASEJ, null, Set.of());
            case ENTRE_PIEZAS, RANGO_PIEZAS -> {
                Integer inicioASEJ = inicioRangoASEJ.get();
                if (inicioASEJ == null) {
                    inicioRangoASEJ.set(fdiASEJ);
                    avisadorASEJ.accept("Pieza " + fdiASEJ + " marcada: haga clic en la otra pieza del hallazgo.");
                } else if (inicioASEJ.equals(fdiASEJ)) {
                    inicioRangoASEJ.set(null);
                } else {
                    inicioRangoASEJ.set(null);
                    registrarASEJ(tASEJ, inicioASEJ, fdiASEJ, Set.of());
                }
            }
        }
    }

    private void alternarSuperficieASEJ(int fdiASEJ, SuperficieASEJ superficieASEJ) {
        if (superficieASEJ == null) {
            avisadorASEJ.accept("Haga clic sobre una superficie de la corona.");
            return;
        }
        Integer pendienteASEJ = piezaPendienteASEJ.get();
        if (pendienteASEJ != null && pendienteASEJ != fdiASEJ && !superficiesPendientesASEJ.isEmpty()) {
            avisadorASEJ.accept("Se descartó la selección de la pieza " + pendienteASEJ + ": las superficies deben ser de una misma pieza.");
            superficiesPendientesASEJ.clear();
        }
        piezaPendienteASEJ.set(fdiASEJ);
        piezaSeleccionadaASEJ.set(fdiASEJ);
        if (!superficiesPendientesASEJ.remove(superficieASEJ)) {
            superficiesPendientesASEJ.add(superficieASEJ);
        }
        if (superficiesPendientesASEJ.isEmpty()) {
            piezaPendienteASEJ.set(null);
        }
    }

    public boolean registrarPendienteASEJ() {
        HallazgoTipoDtoASEJ tASEJ = herramientaASEJ.get();
        if (tASEJ == null || piezaPendienteASEJ.get() == null || superficiesPendientesASEJ.isEmpty() || soloLecturaASEJ.get()) {
            return false;
        }
        registrarASEJ(tASEJ, piezaPendienteASEJ.get(), null, EnumSet.copyOf(superficiesPendientesASEJ));
        return true;
    }

    public void cancelarASEJ() {
        boolean habiaPendienteASEJ = !superficiesPendientesASEJ.isEmpty() || inicioRangoASEJ.get() != null;
        limpiarPendientesASEJ();
        if (!habiaPendienteASEJ) {
            herramientaASEJ.set(null);
        }
    }

    public boolean hayPendienteASEJ() {
        return !superficiesPendientesASEJ.isEmpty();
    }

    private void registrarASEJ(HallazgoTipoDtoASEJ tASEJ, int fdiASEJ, Integer finASEJ, Set<SuperficieASEJ> superficiesASEJ) {
        OdontogramaVistaASEJ vASEJ = vistaASEJ.get();
        if (vASEJ == null || vASEJ.odontogramaIdASEJ() == null) {
            return;
        }
        String siglaASEJ = null;
        if (!tASEJ.siglasOpcionesASEJ().isEmpty()) {
            Optional<String> elegidaASEJ = selectorSiglaASEJ.apply(tASEJ);
            if (elegidaASEJ.isEmpty()) {
                return;
            }
            siglaASEJ = elegidaASEJ.get();
        }
        EstadoHallazgoASEJ estadoASEJ = tASEJ.reglaColorASEJ() == ReglaColorASEJ.POR_ESTADO ? estadoActivoASEJ.get() : null;
        piezaSeleccionadaASEJ.set(fdiASEJ);
        registradorASEJ.accept(new NuevoHallazgoASEJ(vASEJ.odontogramaIdASEJ(), tASEJ.idASEJ(), fdiASEJ, finASEJ, superficiesASEJ, estadoASEJ, siglaASEJ, null));
    }

    private void limpiarPendientesASEJ() {
        superficiesPendientesASEJ.clear();
        piezaPendienteASEJ.set(null);
        inicioRangoASEJ.set(null);
    }

    public RenderizadorOdontogramaASEJ.SuperposicionASEJ superposicionASEJ() {
        DiferenciaVistaASEJ dASEJ = diferenciaASEJ.get();
        Set<Integer> cambiosASEJ = modoCompararASEJ.get() && dASEJ != null ? dASEJ.diferenciaASEJ().piezasAfectadasASEJ() : Set.of();
        return new RenderizadorOdontogramaASEJ.SuperposicionASEJ(piezaHoverASEJ.get(), superficieHoverASEJ.get(), piezaSeleccionadaASEJ.get(),
                piezaPendienteASEJ.get(), Set.copyOf(superficiesPendientesASEJ), inicioRangoASEJ.get(), cambiosASEJ);
    }

    public static boolean afectaASEJ(HallazgoDtoASEJ hASEJ, int fdiASEJ) {
        if (hASEJ.piezaASEJ() == fdiASEJ) {
            return true;
        }
        return hASEJ.piezaFinASEJ() != null && PiezaDentalASEJ.esValidaASEJ(fdiASEJ)
                && PiezaDentalASEJ.rangoASEJ(PiezaDentalASEJ.deASEJ(hASEJ.piezaASEJ()), PiezaDentalASEJ.deASEJ(hASEJ.piezaFinASEJ())).contains(PiezaDentalASEJ.deASEJ(fdiASEJ));
    }

    private void actualizarInspectorASEJ() {
        OdontogramaVistaASEJ vASEJ = vistaASEJ.get();
        Integer fdiASEJ = piezaSeleccionadaASEJ.get();
        if (vASEJ == null || fdiASEJ == null) {
            hallazgosPiezaASEJ.clear();
            return;
        }
        List<HallazgoDtoASEJ> listaASEJ = vASEJ.historialASEJ().stream()
                .filter(hASEJ -> afectaASEJ(hASEJ, fdiASEJ))
                .sorted(Comparator.comparingInt((HallazgoDtoASEJ hASEJ) -> hASEJ.vigenteASEJ() ? 0 : hASEJ.anuladoASEJ() ? 2 : 1)
                        .thenComparing(HallazgoDtoASEJ::registradoEnASEJ, Comparator.reverseOrder()))
                .toList();
        hallazgosPiezaASEJ.setAll(listaASEJ);
    }
}
