/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.controlador;

import atlantafx.base.theme.Styles;
import com.odontogest.dominio.odontograma.Denticion;
import com.odontogest.dominio.odontograma.EstadoHallazgo;
import com.odontogest.dominio.odontograma.PiezaDental;
import com.odontogest.dominio.odontograma.ReglaColor;
import com.odontogest.dominio.odontograma.Representacion;
import com.odontogest.dominio.odontograma.ResultadoIndice;
import com.odontogest.dominio.odontograma.ResultadoVerificacion;
import com.odontogest.dominio.odontograma.Superficie;
import com.odontogest.dominio.odontograma.TipoHallazgoDef;
import com.odontogest.evento.SolicitudNuevaAtencion;
import com.odontogest.modelo.TipoOdontograma;
import com.odontogest.seguridad.Permiso;
import com.odontogest.seguridad.SesionUsuario;
import com.odontogest.servicio.AtencionService;
import com.odontogest.servicio.CatalogoHallazgoService;
import com.odontogest.servicio.IntegridadService;
import com.odontogest.servicio.OdontogramaService;
import com.odontogest.servicio.dto.AtencionDetalle;
import com.odontogest.servicio.dto.HallazgoDto;
import com.odontogest.servicio.dto.HallazgoTipoDto;
import com.odontogest.servicio.dto.NuevoHallazgo;
import com.odontogest.servicio.dto.OdontogramaResumen;
import com.odontogest.servicio.dto.OdontogramaVista;
import com.odontogest.servicio.dto.PacienteDetalle;
import com.odontogest.ui.componente.OdontogramaCanvas;
import com.odontogest.ui.componente.PaletaOdontograma;
import com.odontogest.ui.componente.RenderizadorOdontograma;
import com.odontogest.ui.soporte.Exportador;
import com.odontogest.ui.soporte.Formularios;
import com.odontogest.ui.soporte.Formularios.Opcion;
import com.odontogest.ui.soporte.Notificador;
import com.odontogest.ui.soporte.PacienteActivo;
import com.odontogest.ui.soporte.TareaUi;
import com.odontogest.ui.soporte.Tema;
import com.odontogest.ui.vistamodelo.OdontogramaViewModel;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.SetChangeListener;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Pestaña "Odontograma" de la ficha (PLAN §6.2). Muestra el estado proyectado, permite registrar, eliminar y
 * anular hallazgos, cerrar y firmar, recorrer la línea de tiempo, comparar con el inicial y exportar la ficha en
 * PDF. La lógica del clic vive en {@link OdontogramaViewModel}; aquí solo se conectan vistas y servicios.
 */
@Component
@RequiredArgsConstructor
public class OdontogramaController {

    private static final Map<Representacion, String> GRUPOS = Map.of(
            Representacion.SUPERFICIE, "Por superficie", Representacion.PIEZA, "Por pieza",
            Representacion.ENTRE_PIEZAS, "Entre dos piezas", Representacion.RANGO_PIEZAS, "Rango de piezas");

    private final OdontogramaService odontogramaService;
    private final AtencionService atencionService;
    private final IntegridadService integridadService;
    private final CatalogoHallazgoService catalogoService;
    private final PacienteActivo pacienteActivo;
    private final OdontogramaViewModel vm;
    private final TareaUi tareaUi;
    private final Notificador notificador;
    private final Exportador exportador;
    private final SesionUsuario sesionUsuario;
    private final ApplicationEventPublisher eventos;

    @FXML private BorderPane raiz;
    @FXML private ComboBox<Opcion<LocalDateTime>> cmbMomento;
    @FXML private Slider sldLinea;
    @FXML private RadioButton rbPermanente;
    @FXML private RadioButton rbDecidua;
    @FXML private RadioButton rbMixta;
    @FXML private ToggleButton tglComparar;
    @FXML private Button btnCerrar;
    @FXML private Button btnNuevaEvolucion;
    @FXML private Button btnPdf;
    @FXML private Label lblEstado;
    @FXML private Button btnSeleccion;
    @FXML private Accordion paleta;
    @FXML private RadioButton rbBueno;
    @FXML private RadioButton rbMalo;
    @FXML private Label lblPendiente;
    @FXML private Button btnRegistrar;
    @FXML private OdontogramaCanvas lienzo;
    @FXML private VBox panelVacio;
    @FXML private Button btnNuevaAtencion;
    @FXML private Label lblHover;
    @FXML private Label lblPieza;
    @FXML private Label lblPiezaDetalle;
    @FXML private ListView<HallazgoDto> lstHallazgos;
    @FXML private Button btnEliminar;
    @FXML private Button btnAnular;
    @FXML private TextArea txtEspecificaciones;
    @FXML private TextArea txtObservaciones;
    @FXML private Label lblCpod;
    @FXML private Label lblCeod;
    @FXML private Label lblIntegridad;
    @FXML private Button btnGuardarTextos;

    private final ToggleGroup grupoDenticion = new ToggleGroup();
    private final ToggleGroup grupoEstado = new ToggleGroup();
    private final List<ListView<HallazgoTipoDto>> listasPaleta = new ArrayList<>();
    private final RenderizadorOdontograma renderizador = new RenderizadorOdontograma();
    private ChangeListener<PacienteDetalle> oyentePaciente;
    /** El ViewModel es singleton: sus oyentes se registran una sola vez (usan los campos actuales del controlador). */
    private boolean vmConectado;
    /** Evita que los cambios programáticos de combos y radios disparen acciones. */
    private boolean actualizando;
    private List<OdontogramaResumen> resumenes = List.of();
    private Optional<AtencionDetalle> atencionAbierta = Optional.empty();

    /** Lo que se carga en segundo plano al abrir o recargar la pestaña. */
    private record Carga(OdontogramaVista vista, List<OdontogramaResumen> resumenes, Optional<AtencionDetalle> atencion,
                         ResultadoVerificacion integridad, List<HallazgoTipoDto> tipos) {
    }

    @FXML
    public void initialize() {
        rbPermanente.setToggleGroup(grupoDenticion);
        rbDecidua.setToggleGroup(grupoDenticion);
        rbMixta.setToggleGroup(grupoDenticion);
        rbPermanente.setUserData(Denticion.PERMANENTE);
        rbDecidua.setUserData(Denticion.DECIDUA);
        rbMixta.setUserData(Denticion.MIXTA);
        rbBueno.setToggleGroup(grupoEstado);
        rbMalo.setToggleGroup(grupoEstado);
        (vm.estadoActivo.get() == EstadoHallazgo.MALO ? rbMalo : rbBueno).setSelected(true);
        grupoEstado.selectedToggleProperty().addListener((obs, a, b) ->
                vm.estadoActivo.set(b == rbMalo ? EstadoHallazgo.MALO : EstadoHallazgo.BUENO));
        listasPaleta.clear();
        grupoDenticion.selectedToggleProperty().addListener((obs, a, b) -> {
            if (!actualizando && b != null) {
                cambiarDenticion((Denticion) b.getUserData());
            }
        });

        configurarLienzo();
        configurarLineaDeTiempo();
        configurarInspector();
        if (!vmConectado) {
            conectarViewModel();
            vmConectado = true;
        }
        tglComparar.selectedProperty().addListener((obs, a, b) -> comparar(b));
        btnNuevaAtencion.setDisable(!sesionUsuario.tiene(Permiso.ATENCION_GESTIONAR));
        raiz.addEventFilter(KeyEvent.KEY_PRESSED, this::atajos);

        if (oyentePaciente != null) {
            pacienteActivo.pacienteProperty().removeListener(oyentePaciente);
        }
        oyentePaciente = (obs, anterior, actual) -> {
            if (actual == null || anterior == null || actual.id() != anterior.id()) {
                vm.fechaVista.set(null);
                vm.piezaSeleccionada.set(null);
                vm.herramienta.set(null);
                tglComparar.setSelected(false);
            }
            recargar();
        };
        pacienteActivo.pacienteProperty().addListener(oyentePaciente);
        recargar();
    }

    // ------------------------------------------------------------------ carga

    /** Vuelve a leer el odontograma del paciente activo (también lo llama la pestaña Atenciones). */
    public void recargar() {
        PacienteDetalle p = pacienteActivo.get();
        if (p == null || lienzo == null) {
            return;
        }
        LocalDateTime hasta = vm.fechaVista.get();
        boolean primeraVez = listasPaleta.isEmpty();
        tareaUi.ejecutar(() -> new Carga(odontogramaService.obtenerVista(p.id(), hasta),
                odontogramaService.listarPorPaciente(p.id()), atencionService.abierta(p.id()),
                integridadService.verificar(p.id()), primeraVez ? catalogoService.listarTipos(false) : List.of()), this::aplicar);
    }

    private void aplicar(Carga carga) {
        if (!carga.tipos().isEmpty()) {
            construirPaleta(carga.tipos());
        }
        resumenes = carga.resumenes();
        atencionAbierta = carga.atencion();
        llenarLineaDeTiempo(carga.vista());
        mostrarIntegridad(carga.integridad());
        aplicarVista(carga.vista());
        if (tglComparar.isSelected()) {
            comparar(true);
        }
    }

    private void aplicarVista(OdontogramaVista v) {
        actualizando = true;
        try {
            vm.soloLectura.set(!v.editable() || !sesionUsuario.tiene(Permiso.ODONTOGRAMA_EDITAR));
            vm.vista.set(v);
            lienzo.setPaleta(Tema.actual() == Tema.OSCURO ? PaletaOdontograma.oscura() : PaletaOdontograma.clara());
            lienzo.setDenticion(v.denticion());
            lienzo.setEstado(v.estadoBoca());
            lienzo.setSuperposicion(vm.superposicion());

            for (var t : grupoDenticion.getToggles()) {
                ((RadioButton) t).setSelected(t.getUserData() == v.denticion());
            }
            boolean editable = !vm.soloLectura.get();
            boolean hayOdontograma = v.odontogramaId() != null;
            panelVacio.setVisible(!hayOdontograma);
            panelVacio.setManaged(!hayOdontograma);
            rbPermanente.setDisable(!editable);
            rbDecidua.setDisable(!editable);
            rbMixta.setDisable(!editable);
            paleta.setDisable(!editable);
            btnSeleccion.setDisable(!editable);
            btnCerrar.setDisable(!editable);
            btnPdf.setDisable(!hayOdontograma);
            boolean puedeEvolucionar = hayOdontograma && !v.borrador() && v.hasta() == null && atencionAbierta.isPresent()
                    && sesionUsuario.tiene(Permiso.ODONTOGRAMA_EDITAR);
            btnNuevaEvolucion.setVisible(puedeEvolucionar);
            btnNuevaEvolucion.setManaged(puedeEvolucionar);
            tglComparar.setDisable(inicialCerrado().isEmpty());

            txtEspecificaciones.setText(v.especificaciones() == null ? "" : v.especificaciones());
            txtObservaciones.setText(v.observaciones() == null ? "" : v.observaciones());
            txtEspecificaciones.setEditable(editable);
            txtObservaciones.setEditable(editable);
            btnGuardarTextos.setDisable(!editable);

            lblEstado.setText(estadoTexto(v));
            lblCpod.setText("CPO-D: " + indice(v.indices().cpod(), "C", "P", "O"));
            lblCpod.setTooltip(new Tooltip(v.indices().excluyeTercerasMolares()
                    ? "Sin terceras molares (configuración). Nivel según la referencia OMS a los 12 años."
                    : "Incluye terceras molares. Nivel según la referencia OMS a los 12 años."));
            lblCeod.setText("ceo-d: " + indice(v.indices().ceod(), "c", "e", "o"));
            actualizarIndicaciones();
            actualizarInspectorTitulo();
        } finally {
            actualizando = false;
        }
    }

    private static String estadoTexto(OdontogramaVista v) {
        if (v.odontogramaId() == null) {
            return "Sin odontograma";
        }
        String base = v.tipo().etiqueta + " · ";
        if (v.hasta() != null) {
            return base + "estado al " + Formularios.fechaHora(v.hasta()) + " (solo lectura)";
        }
        if (v.borrador()) {
            return base + "BORRADOR" + (v.atencionAbierta() ? "" : " · atención cerrada");
        }
        return base + "CERRADO el " + Formularios.fechaHora(v.cerradoEn()) + " · hash " + abreviar(v.hashCierre());
    }

    private static String indice(ResultadoIndice r, String c, String p, String o) {
        return String.format("%s=%d %s=%d %s=%d → %d (%s)", c, r.c(), p, r.p(), o, r.o(), r.total(),
                r.nivel().name().replace('_', ' ').toLowerCase());
    }

    private static String abreviar(String hash) {
        return hash == null ? "" : hash.substring(0, 6) + "…" + hash.substring(hash.length() - 4);
    }

    private void mostrarIntegridad(ResultadoVerificacion r) {
        lblIntegridad.getStyleClass().removeAll(Styles.SUCCESS, Styles.DANGER);
        if (r.integra()) {
            lblIntegridad.setText("✔ Cadena de integridad verificada");
            lblIntegridad.getStyleClass().add(Styles.SUCCESS);
        } else {
            lblIntegridad.setText("✖ Odontograma #" + r.odontogramaIdInvalido() + " alterado: " + r.motivo());
            lblIntegridad.getStyleClass().add(Styles.DANGER);
        }
    }

    // ------------------------------------------------------------------ lienzo y paleta

    private void configurarLienzo() {
        lienzo.setOyente(new OdontogramaCanvas.Oyente() {
            @Override
            public void movido(Integer fdi, Superficie superficie) {
                vm.movido(fdi, superficie);
                lblHover.setText(fdi == null ? "" : "Pieza " + fdi
                        + (superficie == null ? "" : " · " + superficie.nombre(PiezaDental.de(fdi))));
            }

            @Override
            public void clic(Integer fdi, Superficie superficie) {
                vm.clic(fdi, superficie);
            }
        });
    }

    /** Oyentes del ViewModel: se registran una sola vez; los lambdas usan los nodos actuales del controlador. */
    private void conectarViewModel() {
        vm.setRegistrador(this::registrar);
        vm.setAvisador(notificador::info);
        vm.setSelectorSigla(this::elegirSigla);
        vm.estadoActivo.addListener((obs, a, b) -> (b == EstadoHallazgo.MALO ? rbMalo : rbBueno).setSelected(true));
        vm.herramienta.addListener((obs, a, b) -> {
            actualizarIndicaciones();
            if (b == null) {
                listasPaleta.forEach(l -> l.getSelectionModel().clearSelection());
            }
        });
        vm.superficiesPendientes.addListener((SetChangeListener<Superficie>) c -> actualizarIndicaciones());
        vm.inicioRango.addListener((obs, a, b) -> actualizarIndicaciones());
        vm.hallazgosPieza.addListener((ListChangeListener<HallazgoDto>) c -> actualizarBotonesInspector());
        Runnable redibujar = () -> lienzo.setSuperposicion(vm.superposicion());
        vm.piezaHover.addListener((obs, a, b) -> redibujar.run());
        vm.superficieHover.addListener((obs, a, b) -> redibujar.run());
        vm.piezaSeleccionada.addListener((obs, a, b) -> {
            redibujar.run();
            actualizarInspectorTitulo();
        });
        vm.piezaPendiente.addListener((obs, a, b) -> redibujar.run());
        vm.superficiesPendientes.addListener((SetChangeListener<Superficie>) c -> redibujar.run());
        vm.inicioRango.addListener((obs, a, b) -> redibujar.run());
        vm.diferencia.addListener((obs, a, b) -> redibujar.run());
        vm.modoComparar.addListener((obs, a, b) -> redibujar.run());
    }

    private void construirPaleta(List<HallazgoTipoDto> tipos) {
        paleta.getPanes().clear();
        listasPaleta.clear();
        Map<Representacion, List<HallazgoTipoDto>> porGrupo = tipos.stream()
                .collect(Collectors.groupingBy(HallazgoTipoDto::representacion, () -> new EnumMap<>(Representacion.class), Collectors.toList()));
        porGrupo.forEach((representacion, lista) -> {
            ListView<HallazgoTipoDto> lv = new ListView<>();
            lv.getItems().setAll(lista);
            lv.setCellFactory(l -> new CeldaTipo());
            lv.getSelectionModel().selectedItemProperty().addListener((obs, a, b) -> {
                if (b != null) {
                    listasPaleta.stream().filter(x -> x != lv).forEach(x -> x.getSelectionModel().clearSelection());
                    vm.herramienta.set(b);
                }
            });
            listasPaleta.add(lv);
            TitledPane tp = new TitledPane(GRUPOS.get(representacion) + " (" + lista.size() + ")", lv);
            paleta.getPanes().add(tp);
        });
        if (!paleta.getPanes().isEmpty()) {
            paleta.setExpandedPane(paleta.getPanes().get(0));
        }
    }

    /** Celda de la paleta: muestra dibujada del símbolo + nombre. */
    private final class CeldaTipo extends ListCell<HallazgoTipoDto> {
        private final Canvas muestra = new Canvas(28, 28);

        @Override
        protected void updateItem(HallazgoTipoDto t, boolean vacio) {
            super.updateItem(t, vacio);
            if (vacio || t == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            renderizador.muestra(muestra.getGraphicsContext2D(), definicion(t), 28,
                    Tema.actual() == Tema.OSCURO ? PaletaOdontograma.oscura() : PaletaOdontograma.clara());
            setGraphic(muestra);
            setText(t.nombre());
        }
    }

    static TipoHallazgoDef definicion(HallazgoTipoDto t) {
        return new TipoHallazgoDef(t.id(), t.codigo(), t.nombre(), t.sigla(), t.siglasOpciones(), t.representacion(),
                t.simbolo(), t.reglaColor(), t.grupoExclusion(), t.ocultaSuperficies(), t.conteoCpod(), t.conteoCeod());
    }

    private void actualizarIndicaciones() {
        HallazgoTipoDto t = vm.herramienta.get();
        btnRegistrar.setDisable(!vm.hayPendiente());
        boolean usaEstado = t != null && t.reglaColor() == ReglaColor.POR_ESTADO;
        rbBueno.setDisable(!usaEstado);
        rbMalo.setDisable(!usaEstado);
        if (vm.soloLectura.get()) {
            lblPendiente.setText(vm.vista.get() == null || vm.vista.get().odontogramaId() == null ? ""
                    : "Solo lectura: seleccione una pieza para ver sus hallazgos.");
        } else if (t == null) {
            lblPendiente.setText("Elija un hallazgo de la paleta o haga clic en una pieza para inspeccionarla.");
        } else if (t.representacion() == Representacion.SUPERFICIE) {
            lblPendiente.setText(vm.hayPendiente()
                    ? "Pieza " + vm.piezaPendiente.get() + ": " + vm.superficiesPendientes.stream()
                    .map(s -> s.etiqueta(PiezaDental.de(vm.piezaPendiente.get()))).collect(Collectors.joining(", "))
                    + ". Pulse Enter para registrar."
                    : t.nombre() + ": haga clic en las superficies de una pieza.");
        } else if (t.representacion() == Representacion.PIEZA) {
            lblPendiente.setText(t.nombre() + ": haga clic en la pieza.");
        } else {
            lblPendiente.setText(vm.inicioRango.get() == null ? t.nombre() + ": haga clic en la primera pieza."
                    : t.nombre() + ": ahora haga clic en la otra pieza.");
        }
    }

    private Optional<String> elegirSigla(HallazgoTipoDto t) {
        ChoiceDialog<String> dialogo = new ChoiceDialog<>(t.siglasOpciones().get(0), t.siglasOpciones());
        dialogo.setTitle("Sigla del hallazgo");
        dialogo.setHeaderText(t.nombre());
        dialogo.setContentText("Sigla:");
        dialogo.initOwner(raiz.getScene().getWindow());
        return dialogo.showAndWait();
    }

    private void atajos(KeyEvent e) {
        if (e.getTarget() instanceof TextInputControl) {
            return;
        }
        if (e.getCode() == KeyCode.ENTER && vm.registrarPendiente()) {
            e.consume();
        } else if (e.getCode() == KeyCode.ESCAPE) {
            vm.cancelar();
            e.consume();
        } else if (e.getCode() == KeyCode.B && !rbBueno.isDisabled()) {
            vm.estadoActivo.set(EstadoHallazgo.BUENO);
        } else if (e.getCode() == KeyCode.M && !rbMalo.isDisabled()) {
            vm.estadoActivo.set(EstadoHallazgo.MALO);
        }
    }

    // ------------------------------------------------------------------ línea de tiempo y comparación

    private void configurarLineaDeTiempo() {
        cmbMomento.valueProperty().addListener((obs, a, b) -> {
            if (actualizando || b == null) {
                return;
            }
            actualizando = true;
            sldLinea.setValue(cmbMomento.getItems().indexOf(b));
            actualizando = false;
            vm.fechaVista.set(b.valor());
            recargar();
        });
        sldLinea.valueProperty().addListener((obs, a, b) -> {
            if (actualizando) {
                return;
            }
            int i = (int) Math.round(b.doubleValue());
            if (i >= 0 && i < cmbMomento.getItems().size() && cmbMomento.getSelectionModel().getSelectedIndex() != i) {
                cmbMomento.getSelectionModel().select(i);
            }
        });
    }

    private void llenarLineaDeTiempo(OdontogramaVista v) {
        actualizando = true;
        try {
            List<Opcion<LocalDateTime>> opciones = new ArrayList<>();
            for (OdontogramaResumen r : resumenes) {
                if (!r.borrador()) {
                    opciones.add(new Opcion<>(r.etiqueta(), r.cerradoEn()));
                }
            }
            boolean hayBorrador = resumenes.stream().anyMatch(OdontogramaResumen::borrador);
            if (!resumenes.isEmpty()) {
                opciones.add(new Opcion<>(hayBorrador ? "Actual (borrador)" : "Actual", null));
            }
            cmbMomento.getItems().setAll(opciones);
            Opcion<LocalDateTime> elegida = opciones.stream()
                    .filter(o -> v.hasta() == null ? o.valor() == null : v.hasta().equals(o.valor()))
                    .findFirst().orElse(opciones.isEmpty() ? null : opciones.get(opciones.size() - 1));
            cmbMomento.setValue(elegida);
            sldLinea.setMax(Math.max(0, opciones.size() - 1));
            sldLinea.setValue(elegida == null ? 0 : opciones.indexOf(elegida));
            sldLinea.setDisable(opciones.size() < 2);
            cmbMomento.setDisable(opciones.isEmpty());
        } finally {
            actualizando = false;
        }
    }

    private Optional<OdontogramaResumen> inicialCerrado() {
        return resumenes.stream().filter(r -> r.tipo() == TipoOdontograma.INICIAL && !r.borrador()).findFirst();
    }

    private void comparar(boolean activar) {
        vm.modoComparar.set(activar);
        PacienteDetalle p = pacienteActivo.get();
        Optional<OdontogramaResumen> inicial = inicialCerrado();
        if (!activar || p == null || inicial.isEmpty()) {
            vm.diferencia.set(null);
            return;
        }
        LocalDateTime objetivo = vm.fechaVista.get();
        tareaUi.ejecutar(() -> odontogramaService.comparar(p.id(), inicial.get().cerradoEn(), objetivo), d -> {
            vm.diferencia.set(d);
            int piezas = d.diferencia().piezasAfectadas().size();
            lblEstado.setText(piezas == 0 ? "Sin cambios respecto del inicial"
                    : piezas + (piezas == 1 ? " pieza con cambios" : " piezas con cambios") + " respecto del inicial (marco ámbar)");
        });
    }

    // ------------------------------------------------------------------ inspector

    private void configurarInspector() {
        lstHallazgos.setItems(vm.hallazgosPieza);
        lstHallazgos.setCellFactory(l -> new ListCell<>() {
            // Etiqueta con ancho máximo atado a la lista: el texto largo se ajusta en varias líneas.
            private final Label texto = new Label();
            private final Circle punto = new Circle(5);
            private final HBox fila = new HBox(8, punto, texto);

            {
                texto.setWrapText(true);
                texto.maxWidthProperty().bind(l.widthProperty().subtract(44));
                fila.setAlignment(Pos.TOP_LEFT);
                HBox.setMargin(punto, new Insets(5, 0, 0, 0));
            }

            @Override
            protected void updateItem(HallazgoDto h, boolean vacio) {
                super.updateItem(h, vacio);
                setText(null);
                texto.getStyleClass().removeAll(Styles.TEXT_MUTED);
                if (vacio || h == null) {
                    setGraphic(null);
                    return;
                }
                PaletaOdontograma colores = PaletaOdontograma.clara();
                punto.setFill(h.anulado() ? Color.GRAY : colores.color(h.color()));
                texto.setText(textoHallazgo(h));
                if (!h.vigente()) {
                    texto.getStyleClass().add(Styles.TEXT_MUTED);
                }
                setGraphic(fila);
            }
        });
        lstHallazgos.getSelectionModel().selectedItemProperty().addListener((obs, a, b) -> actualizarBotonesInspector());
        actualizarBotonesInspector();
    }

    private static String textoHallazgo(HallazgoDto h) {
        StringBuilder sb = new StringBuilder();
        if (h.anulado()) {
            sb.append("[ANULADO] ");
        } else if (!h.vigente()) {
            sb.append("[Reemplazado] ");
        }
        sb.append(h.nombreTipo());
        if (h.sigla() != null) {
            sb.append(" (").append(h.sigla()).append(')');
        }
        if (h.piezaFin() != null) {
            sb.append(" · ").append(h.pieza()).append('–').append(h.piezaFin());
        }
        if (!h.superficies().isEmpty()) {
            PiezaDental p = PiezaDental.de(h.pieza());
            sb.append(" · ").append(h.superficies().stream().sorted().map(s -> s.etiqueta(p)).collect(Collectors.joining()));
        }
        if (h.estado() != null) {
            sb.append(" · ").append(h.estado() == EstadoHallazgo.BUENO ? "bueno" : "malo");
        }
        sb.append("\n").append(Formularios.fechaHora(h.registradoEn())).append(" · ").append(h.registradoPor());
        if (h.anulado()) {
            sb.append("\nMotivo: ").append(h.motivoAnulacion());
        }
        return sb.toString();
    }

    private void actualizarInspectorTitulo() {
        Integer fdi = vm.piezaSeleccionada.get();
        if (fdi == null) {
            lblPieza.setText("Inspector");
            lblPiezaDetalle.setText("Haga clic en una pieza para ver sus hallazgos.");
            return;
        }
        PiezaDental p = PiezaDental.de(fdi);
        lblPieza.setText("Pieza " + fdi);
        lblPiezaDetalle.setText(capitalizar(p.tipo().name()) + " · " + capitalizar(p.tipoRadicular().name())
                + (p.esDecidua() ? " · decidua" : " · permanente"));
    }

    private static String capitalizar(String s) {
        return s.charAt(0) + s.substring(1).toLowerCase();
    }

    private void actualizarBotonesInspector() {
        HallazgoDto h = lstHallazgos.getSelectionModel().getSelectedItem();
        OdontogramaVista v = vm.vista.get();
        boolean permiso = sesionUsuario.tiene(Permiso.ODONTOGRAMA_EDITAR);
        boolean actual = v != null && v.hasta() == null;
        btnEliminar.setDisable(h == null || !h.eliminable() || !permiso);
        btnAnular.setDisable(h == null || h.eliminable() || h.anulado() || !permiso || !actual);
    }

    // ------------------------------------------------------------------ acciones

    private void registrar(NuevoHallazgo nuevo) {
        tareaUi.ejecutar(() -> odontogramaService.registrarHallazgo(nuevo), this::aplicarVista);
    }

    private void cambiarDenticion(Denticion denticion) {
        OdontogramaVista v = vm.vista.get();
        if (v == null || !v.editable() || v.denticion() == denticion) {
            return;
        }
        tareaUi.ejecutar(() -> odontogramaService.cambiarDenticion(v.odontogramaId(), denticion), this::aplicarVista,
                error -> {
                    tareaUi.manejarError(error);
                    aplicarVista(v);
                });
    }

    @FXML
    private void onModoSeleccion() {
        vm.herramienta.set(null);
    }

    @FXML
    private void onRegistrar() {
        vm.registrarPendiente();
    }

    @FXML
    private void onCerrar() {
        OdontogramaVista v = vm.vista.get();
        if (v == null || !v.editable() || !notificador.confirmar("Cerrar y firmar el odontograma",
                "El odontograma quedará firmado con su hash de integridad y ya no podrá modificarse; "
                        + "las correcciones se harán por anulación con motivo. ¿Continuar?")) {
            return;
        }
        tareaUi.ejecutar(btnCerrar, () -> odontogramaService.cerrar(v.odontogramaId()), cerrado -> {
            notificador.exito("Odontograma cerrado y firmado.");
            recargar();
        });
    }

    @FXML
    private void onNuevaEvolucion() {
        OdontogramaVista v = vm.vista.get();
        if (atencionAbierta.isEmpty() || v == null) {
            return;
        }
        long atencionId = atencionAbierta.get().id();
        tareaUi.ejecutar(btnNuevaEvolucion, () -> odontogramaService.abrirBorrador(atencionId, v.denticion()), id -> {
            notificador.exito("Odontograma de evolución abierto.");
            recargar();
        });
    }

    @FXML
    private void onNuevaAtencion() {
        eventos.publishEvent(new SolicitudNuevaAtencion());
    }

    @FXML
    private void onGuardarTextos() {
        OdontogramaVista v = vm.vista.get();
        if (v == null || !v.editable()) {
            return;
        }
        String esp = txtEspecificaciones.getText();
        String obs = txtObservaciones.getText();
        tareaUi.ejecutar(btnGuardarTextos, () -> odontogramaService.actualizarEspecificaciones(v.odontogramaId(), esp, obs), g -> {
            aplicarVista(g);
            notificador.exito("Especificaciones guardadas.");
        });
    }

    @FXML
    private void onEliminar() {
        HallazgoDto h = lstHallazgos.getSelectionModel().getSelectedItem();
        if (h == null || !notificador.confirmar("Eliminar hallazgo",
                "¿Eliminar \"" + h.nombreTipo() + "\" de la pieza " + h.pieza() + "? El odontograma aún está en borrador.")) {
            return;
        }
        tareaUi.ejecutar(btnEliminar, () -> odontogramaService.eliminarHallazgoBorrador(h.id()), this::aplicarVista);
    }

    @FXML
    private void onAnular() {
        HallazgoDto h = lstHallazgos.getSelectionModel().getSelectedItem();
        if (h == null) {
            return;
        }
        TextArea motivo = new TextArea();
        motivo.setPrefRowCount(3);
        motivo.setWrapText(true);
        motivo.setPromptText("Explique el error (mínimo " + OdontogramaService.LARGO_MINIMO_MOTIVO + " caracteres)");
        Label ayuda = new Label("El hallazgo no se borra: queda en el historial marcado como anulado y el hash del "
                + "odontograma firmado no cambia (NTS 188, 5.6).");
        ayuda.setWrapText(true);
        ayuda.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);
        Label titulo = new Label(h.nombreTipo() + " · pieza " + h.pieza() + " · " + Formularios.fechaHora(h.registradoEn()));
        titulo.setWrapText(true);
        Button cancelar = Formularios.boton("Cancelar", null);
        cancelar.setOnAction(e -> notificador.cerrarPanel());
        Button anular = Formularios.boton("Anular hallazgo", Feather.SLASH, Styles.DANGER);
        anular.setOnAction(e -> tareaUi.ejecutar(anular, () -> odontogramaService.anularHallazgo(h.id(), motivo.getText()), v -> {
            notificador.cerrarPanel();
            notificador.exito("Hallazgo anulado.");
            aplicarVista(v);
        }));
        notificador.abrirPanel(Formularios.panelLateral("Anular hallazgo", new VBox(10, titulo, motivo, ayuda), cancelar, anular));
    }

    @FXML
    private void onPdf() {
        OdontogramaVista v = vm.vista.get();
        PacienteDetalle p = pacienteActivo.get();
        if (v == null || p == null || v.odontogramaId() == null) {
            return;
        }
        BufferedImage imagen = SwingFXUtils.fromFXImage(renderizador.imagen(1600, 900, v.denticion(), v.estadoBoca(),
                vm.superposicion().diferencias()), null);
        LocalDateTime hasta = v.hasta();
        exportador.guardarPdf(btnPdf, "ficha-odontologica-" + p.numeroHistoria() + ".pdf", () -> {
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            ImageIO.write(imagen, "png", png);
            return odontogramaService.pdfFicha(p.id(), hasta, png.toByteArray());
        });
    }
}
