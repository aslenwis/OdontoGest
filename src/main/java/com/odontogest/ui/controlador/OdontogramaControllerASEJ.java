package com.odontogest.ui.controlador;

import atlantafx.base.controls.CustomTextField;
import atlantafx.base.theme.Styles;
import com.odontogest.dominio.odontograma.DenticionASEJ;
import com.odontogest.dominio.odontograma.EstadoHallazgoASEJ;
import com.odontogest.dominio.odontograma.PiezaDentalASEJ;
import com.odontogest.dominio.odontograma.ReglaColorASEJ;
import com.odontogest.dominio.odontograma.RepresentacionASEJ;
import com.odontogest.dominio.odontograma.ResultadoIndiceASEJ;
import com.odontogest.dominio.odontograma.ResultadoVerificacionASEJ;
import com.odontogest.dominio.odontograma.SuperficieASEJ;
import com.odontogest.dominio.odontograma.TipoHallazgoDefASEJ;
import com.odontogest.evento.SolicitudNuevaAtencionASEJ;
import com.odontogest.modelo.TipoOdontogramaASEJ;
import com.odontogest.seguridad.PermisoASEJ;
import com.odontogest.seguridad.SesionUsuarioASEJ;
import com.odontogest.servicio.AtencionServiceASEJ;
import com.odontogest.servicio.CatalogoHallazgoServiceASEJ;
import com.odontogest.servicio.IntegridadServiceASEJ;
import com.odontogest.servicio.OdontogramaServiceASEJ;
import com.odontogest.servicio.dto.AtencionDetalleASEJ;
import com.odontogest.servicio.dto.HallazgoDtoASEJ;
import com.odontogest.servicio.dto.HallazgoTipoDtoASEJ;
import com.odontogest.servicio.dto.NuevoHallazgoASEJ;
import com.odontogest.servicio.dto.OdontogramaResumenASEJ;
import com.odontogest.servicio.dto.OdontogramaVistaASEJ;
import com.odontogest.servicio.dto.PacienteDetalleASEJ;
import com.odontogest.ui.componente.OdontogramaCanvasASEJ;
import com.odontogest.ui.componente.PaletaOdontogramaASEJ;
import com.odontogest.ui.componente.RenderizadorOdontogramaASEJ;
import com.odontogest.ui.soporte.ExportadorASEJ;
import com.odontogest.ui.soporte.FormulariosASEJ;
import com.odontogest.ui.soporte.FormulariosASEJ.OpcionASEJ;
import com.odontogest.ui.soporte.NotificadorASEJ;
import com.odontogest.ui.soporte.PacienteActivoASEJ;
import com.odontogest.ui.soporte.TareaUiASEJ;
import com.odontogest.ui.soporte.TemaASEJ;
import com.odontogest.ui.vistamodelo.OdontogramaViewModelASEJ;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.SetChangeListener;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
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
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OdontogramaControllerASEJ {

    private static final Map<RepresentacionASEJ, String> GRUPOSASEJ = Map.of(
            RepresentacionASEJ.SUPERFICIE, "Por superficie", RepresentacionASEJ.PIEZA, "Por pieza",
            RepresentacionASEJ.ENTRE_PIEZAS, "Entre dos piezas", RepresentacionASEJ.RANGO_PIEZAS, "Rango de piezas");

    private final OdontogramaServiceASEJ odontogramaServiceASEJ;
    private final AtencionServiceASEJ atencionServiceASEJ;
    private final IntegridadServiceASEJ integridadServiceASEJ;
    private final CatalogoHallazgoServiceASEJ catalogoServiceASEJ;
    private final PacienteActivoASEJ pacienteActivoASEJ;
    private final OdontogramaViewModelASEJ vmASEJ;
    private final TareaUiASEJ tareaUiASEJ;
    private final NotificadorASEJ notificadorASEJ;
    private final ExportadorASEJ exportadorASEJ;
    private final SesionUsuarioASEJ sesionUsuarioASEJ;
    private final ApplicationEventPublisher eventosASEJ;

    @FXML private BorderPane raizASEJ;
    @FXML private ComboBox<OpcionASEJ<LocalDateTime>> cmbMomentoASEJ;
    @FXML private Slider sldLineaASEJ;
    @FXML private RadioButton rbPermanenteASEJ;
    @FXML private RadioButton rbDeciduaASEJ;
    @FXML private RadioButton rbMixtaASEJ;
    @FXML private ToggleButton tglCompararASEJ;
    @FXML private Button btnCerrarASEJ;
    @FXML private Button btnNuevaEvolucionASEJ;
    @FXML private Button btnPdfASEJ;
    @FXML private Label lblEstadoASEJ;
    @FXML private Button btnSeleccionASEJ;
    @FXML private CustomTextField txtBuscarHallazgoASEJ;
    @FXML private ListView<EntradaPaletaASEJ> lstPaletaASEJ;
    @FXML private RadioButton rbBuenoASEJ;
    @FXML private RadioButton rbMaloASEJ;
    @FXML private Label lblPendienteASEJ;
    @FXML private Button btnRegistrarASEJ;
    @FXML private OdontogramaCanvasASEJ lienzoASEJ;
    @FXML private VBox panelVacioASEJ;
    @FXML private Button btnNuevaAtencionASEJ;
    @FXML private Label lblHoverASEJ;
    @FXML private Label lblPiezaASEJ;
    @FXML private Label lblPiezaDetalleASEJ;
    @FXML private ListView<HallazgoDtoASEJ> lstHallazgosASEJ;
    @FXML private Button btnEliminarASEJ;
    @FXML private Button btnAnularASEJ;
    @FXML private TextArea txtEspecificacionesASEJ;
    @FXML private TextArea txtObservacionesASEJ;
    @FXML private Label lblCpodASEJ;
    @FXML private Label lblCeodASEJ;
    @FXML private Label lblIntegridadASEJ;
    @FXML private Button btnGuardarTextosASEJ;

    private final ToggleGroup grupoDenticionASEJ = new ToggleGroup();
    private final ToggleGroup grupoEstadoASEJ = new ToggleGroup();
    private List<HallazgoTipoDtoASEJ> tiposPaletaASEJ = List.of();
    private final RenderizadorOdontogramaASEJ renderizadorASEJ = new RenderizadorOdontogramaASEJ();
    private ChangeListener<PacienteDetalleASEJ> oyentePacienteASEJ;
    private boolean vmConectadoASEJ;
    private boolean actualizandoASEJ;
    private List<OdontogramaResumenASEJ> resumenesASEJ = List.of();
    private Optional<AtencionDetalleASEJ> atencionAbiertaASEJ = Optional.empty();

    private record CargaASEJ(OdontogramaVistaASEJ vistaASEJ, List<OdontogramaResumenASEJ> resumenesASEJ, Optional<AtencionDetalleASEJ> atencionASEJ,
                         ResultadoVerificacionASEJ integridadASEJ, List<HallazgoTipoDtoASEJ> tiposASEJ) {
    }

    @FXML
    public void initialize() {
        rbPermanenteASEJ.setToggleGroup(grupoDenticionASEJ);
        rbDeciduaASEJ.setToggleGroup(grupoDenticionASEJ);
        rbMixtaASEJ.setToggleGroup(grupoDenticionASEJ);
        rbPermanenteASEJ.setUserData(DenticionASEJ.PERMANENTE);
        rbDeciduaASEJ.setUserData(DenticionASEJ.DECIDUA);
        rbMixtaASEJ.setUserData(DenticionASEJ.MIXTA);
        rbBuenoASEJ.setToggleGroup(grupoEstadoASEJ);
        rbMaloASEJ.setToggleGroup(grupoEstadoASEJ);
        (vmASEJ.estadoActivoASEJ.get() == EstadoHallazgoASEJ.MALO ? rbMaloASEJ : rbBuenoASEJ).setSelected(true);
        grupoEstadoASEJ.selectedToggleProperty().addListener((obsASEJ, aASEJ, bASEJ) ->
                vmASEJ.estadoActivoASEJ.set(bASEJ == rbMaloASEJ ? EstadoHallazgoASEJ.MALO : EstadoHallazgoASEJ.BUENO));
        tiposPaletaASEJ = List.of();
        configurarPaletaASEJ();
        grupoDenticionASEJ.selectedToggleProperty().addListener((obsASEJ, aASEJ, bASEJ) -> {
            if (!actualizandoASEJ && bASEJ != null) {
                cambiarDenticionASEJ((DenticionASEJ) bASEJ.getUserData());
            }
        });

        configurarLienzoASEJ();
        configurarLineaDeTiempoASEJ();
        configurarInspectorASEJ();
        if (!vmConectadoASEJ) {
            conectarViewModelASEJ();
            vmConectadoASEJ = true;
        }
        tglCompararASEJ.selectedProperty().addListener((obsASEJ, aASEJ, bASEJ) -> compararASEJ(bASEJ));
        btnNuevaAtencionASEJ.setDisable(!sesionUsuarioASEJ.tieneASEJ(PermisoASEJ.ATENCION_GESTIONAR));
        raizASEJ.addEventFilter(KeyEvent.KEY_PRESSED, this::atajosASEJ);

        if (oyentePacienteASEJ != null) {
            pacienteActivoASEJ.pacientePropertyASEJ().removeListener(oyentePacienteASEJ);
        }
        oyentePacienteASEJ = (obsASEJ, anteriorASEJ, actualASEJ) -> {
            if (actualASEJ == null || anteriorASEJ == null || actualASEJ.idASEJ() != anteriorASEJ.idASEJ()) {
                vmASEJ.fechaVistaASEJ.set(null);
                vmASEJ.piezaSeleccionadaASEJ.set(null);
                vmASEJ.herramientaASEJ.set(null);
                tglCompararASEJ.setSelected(false);
            }
            recargarASEJ();
        };
        pacienteActivoASEJ.pacientePropertyASEJ().addListener(oyentePacienteASEJ);
        recargarASEJ();
    }

    public void recargarASEJ() {
        PacienteDetalleASEJ pASEJ = pacienteActivoASEJ.getASEJ();
        if (pASEJ == null || lienzoASEJ == null) {
            return;
        }
        LocalDateTime hastaASEJ = vmASEJ.fechaVistaASEJ.get();
        boolean primeraVezASEJ = tiposPaletaASEJ.isEmpty();
        tareaUiASEJ.ejecutarASEJ(() -> new CargaASEJ(odontogramaServiceASEJ.obtenerVistaASEJ(pASEJ.idASEJ(), hastaASEJ),
                odontogramaServiceASEJ.listarPorPacienteASEJ(pASEJ.idASEJ()), atencionServiceASEJ.abiertaASEJ(pASEJ.idASEJ()),
                integridadServiceASEJ.verificarASEJ(pASEJ.idASEJ()), primeraVezASEJ ? catalogoServiceASEJ.listarTiposASEJ(false) : List.of()), this::aplicarASEJ);
    }

    private void aplicarASEJ(CargaASEJ cargaASEJ) {
        if (!cargaASEJ.tiposASEJ().isEmpty()) {
            tiposPaletaASEJ = cargaASEJ.tiposASEJ();
            filtrarPaletaASEJ();
        }
        resumenesASEJ = cargaASEJ.resumenesASEJ();
        atencionAbiertaASEJ = cargaASEJ.atencionASEJ();
        llenarLineaDeTiempoASEJ(cargaASEJ.vistaASEJ());
        mostrarIntegridadASEJ(cargaASEJ.integridadASEJ());
        aplicarVistaASEJ(cargaASEJ.vistaASEJ());
        if (tglCompararASEJ.isSelected()) {
            compararASEJ(true);
        }
    }

    private void aplicarVistaASEJ(OdontogramaVistaASEJ vASEJ) {
        actualizandoASEJ = true;
        try {
            vmASEJ.soloLecturaASEJ.set(!vASEJ.editableASEJ() || !sesionUsuarioASEJ.tieneASEJ(PermisoASEJ.ODONTOGRAMA_EDITAR));
            vmASEJ.vistaASEJ.set(vASEJ);
            lienzoASEJ.setPaletaASEJ(TemaASEJ.actualASEJ() == TemaASEJ.OSCURO ? PaletaOdontogramaASEJ.oscuraASEJ() : PaletaOdontogramaASEJ.claraASEJ());
            lienzoASEJ.setDenticionASEJ(vASEJ.denticionASEJ());
            lienzoASEJ.setEstadoASEJ(vASEJ.estadoBocaASEJ());
            lienzoASEJ.setSuperposicionASEJ(vmASEJ.superposicionASEJ());

            for (var tASEJ : grupoDenticionASEJ.getToggles()) {
                ((RadioButton) tASEJ).setSelected(tASEJ.getUserData() == vASEJ.denticionASEJ());
            }
            boolean editableASEJ = !vmASEJ.soloLecturaASEJ.get();
            boolean hayOdontogramaASEJ = vASEJ.odontogramaIdASEJ() != null;
            panelVacioASEJ.setVisible(!hayOdontogramaASEJ);
            panelVacioASEJ.setManaged(!hayOdontogramaASEJ);
            rbPermanenteASEJ.setDisable(!editableASEJ);
            rbDeciduaASEJ.setDisable(!editableASEJ);
            rbMixtaASEJ.setDisable(!editableASEJ);
            lstPaletaASEJ.setDisable(!editableASEJ);
            txtBuscarHallazgoASEJ.setDisable(!editableASEJ);
            btnSeleccionASEJ.setDisable(!editableASEJ);
            btnCerrarASEJ.setDisable(!editableASEJ);
            btnPdfASEJ.setDisable(!hayOdontogramaASEJ);
            boolean puedeEvolucionarASEJ = hayOdontogramaASEJ && !vASEJ.borradorASEJ() && vASEJ.hastaASEJ() == null && atencionAbiertaASEJ.isPresent()
                    && sesionUsuarioASEJ.tieneASEJ(PermisoASEJ.ODONTOGRAMA_EDITAR);
            btnNuevaEvolucionASEJ.setVisible(puedeEvolucionarASEJ);
            btnNuevaEvolucionASEJ.setManaged(puedeEvolucionarASEJ);
            tglCompararASEJ.setDisable(inicialCerradoASEJ().isEmpty());

            txtEspecificacionesASEJ.setText(vASEJ.especificacionesASEJ() == null ? "" : vASEJ.especificacionesASEJ());
            txtObservacionesASEJ.setText(vASEJ.observacionesASEJ() == null ? "" : vASEJ.observacionesASEJ());
            txtEspecificacionesASEJ.setEditable(editableASEJ);
            txtObservacionesASEJ.setEditable(editableASEJ);
            btnGuardarTextosASEJ.setDisable(!editableASEJ);

            lblEstadoASEJ.setText(estadoTextoASEJ(vASEJ));
            lblEstadoASEJ.setTooltip(new Tooltip(lblEstadoASEJ.getText()));
            lblCpodASEJ.setText("CPO-D: " + indiceASEJ(vASEJ.indicesASEJ().cpodASEJ(), "C", "P", "O"));
            lblCpodASEJ.setTooltip(new Tooltip(vASEJ.indicesASEJ().excluyeTercerasMolaresASEJ()
                    ? "Sin terceras molares (configuración). Nivel según la referencia OMS a los 12 años."
                    : "Incluye terceras molares. Nivel según la referencia OMS a los 12 años."));
            lblCeodASEJ.setText("ceo-d: " + indiceASEJ(vASEJ.indicesASEJ().ceodASEJ(), "c", "e", "o"));
            actualizarIndicacionesASEJ();
            actualizarInspectorTituloASEJ();
        } finally {
            actualizandoASEJ = false;
        }
    }

    private static String estadoTextoASEJ(OdontogramaVistaASEJ vASEJ) {
        if (vASEJ.odontogramaIdASEJ() == null) {
            return "Sin odontograma";
        }
        String baseASEJ = vASEJ.tipoASEJ().etiquetaASEJ + " · ";
        if (vASEJ.hastaASEJ() != null) {
            return baseASEJ + "estado al " + FormulariosASEJ.fechaHoraASEJ(vASEJ.hastaASEJ()) + " (solo lectura)";
        }
        if (vASEJ.borradorASEJ()) {
            return baseASEJ + "BORRADOR" + (vASEJ.atencionAbiertaASEJ() ? "" : " · atención cerrada");
        }
        return baseASEJ + "CERRADO el " + FormulariosASEJ.fechaHoraASEJ(vASEJ.cerradoEnASEJ()) + " · hash " + abreviarASEJ(vASEJ.hashCierreASEJ());
    }

    private static String indiceASEJ(ResultadoIndiceASEJ rASEJ, String cASEJ, String pASEJ, String oASEJ) {
        return String.format("%s=%d %s=%d %s=%d → %d (%s)", cASEJ, rASEJ.cASEJ(), pASEJ, rASEJ.pASEJ(), oASEJ, rASEJ.oASEJ(), rASEJ.totalASEJ(),
                rASEJ.nivelASEJ().name().replace('_', ' ').toLowerCase());
    }

    private static String abreviarASEJ(String hashASEJ) {
        return hashASEJ == null ? "" : hashASEJ.substring(0, 6) + "…" + hashASEJ.substring(hashASEJ.length() - 4);
    }

    private void mostrarIntegridadASEJ(ResultadoVerificacionASEJ rASEJ) {
        lblIntegridadASEJ.getStyleClass().removeAll(Styles.SUCCESS, Styles.DANGER);
        if (rASEJ.integraASEJ()) {
            lblIntegridadASEJ.setText("✔ Cadena de integridad verificada");
            lblIntegridadASEJ.getStyleClass().add(Styles.SUCCESS);
        } else {
            lblIntegridadASEJ.setText("✖ Odontograma #" + rASEJ.odontogramaIdInvalidoASEJ() + " alterado: " + rASEJ.motivoASEJ());
            lblIntegridadASEJ.getStyleClass().add(Styles.DANGER);
        }
    }

    private void configurarLienzoASEJ() {
        lienzoASEJ.setOyenteASEJ(new OdontogramaCanvasASEJ.OyenteASEJ() {
            @Override
            public void movidoASEJ(Integer fdiASEJ, SuperficieASEJ superficieASEJ) {
                vmASEJ.movidoASEJ(fdiASEJ, superficieASEJ);
                lblHoverASEJ.setText(fdiASEJ == null ? "" : "Pieza " + fdiASEJ
                        + (superficieASEJ == null ? "" : " · " + superficieASEJ.nombreASEJ(PiezaDentalASEJ.deASEJ(fdiASEJ))));
            }

            @Override
            public void clicASEJ(Integer fdiASEJ, SuperficieASEJ superficieASEJ) {
                vmASEJ.clicASEJ(fdiASEJ, superficieASEJ);
            }
        });
    }

    private void conectarViewModelASEJ() {
        vmASEJ.setRegistradorASEJ(this::registrarASEJ);
        vmASEJ.setAvisadorASEJ(notificadorASEJ::infoASEJ);
        vmASEJ.setSelectorSiglaASEJ(this::elegirSiglaASEJ);
        vmASEJ.estadoActivoASEJ.addListener((obsASEJ, aASEJ, bASEJ) -> (bASEJ == EstadoHallazgoASEJ.MALO ? rbMaloASEJ : rbBuenoASEJ).setSelected(true));
        vmASEJ.herramientaASEJ.addListener((obsASEJ, aASEJ, bASEJ) -> {
            actualizarIndicacionesASEJ();
            if (bASEJ == null) {
                lstPaletaASEJ.getSelectionModel().clearSelection();
            }
        });
        vmASEJ.superficiesPendientesASEJ.addListener((SetChangeListener<SuperficieASEJ>) cASEJ -> actualizarIndicacionesASEJ());
        vmASEJ.inicioRangoASEJ.addListener((obsASEJ, aASEJ, bASEJ) -> actualizarIndicacionesASEJ());
        vmASEJ.hallazgosPiezaASEJ.addListener((ListChangeListener<HallazgoDtoASEJ>) cASEJ -> actualizarBotonesInspectorASEJ());
        Runnable redibujarASEJ = () -> lienzoASEJ.setSuperposicionASEJ(vmASEJ.superposicionASEJ());
        vmASEJ.piezaHoverASEJ.addListener((obsASEJ, aASEJ, bASEJ) -> redibujarASEJ.run());
        vmASEJ.superficieHoverASEJ.addListener((obsASEJ, aASEJ, bASEJ) -> redibujarASEJ.run());
        vmASEJ.piezaSeleccionadaASEJ.addListener((obsASEJ, aASEJ, bASEJ) -> {
            redibujarASEJ.run();
            actualizarInspectorTituloASEJ();
        });
        vmASEJ.piezaPendienteASEJ.addListener((obsASEJ, aASEJ, bASEJ) -> redibujarASEJ.run());
        vmASEJ.superficiesPendientesASEJ.addListener((SetChangeListener<SuperficieASEJ>) cASEJ -> redibujarASEJ.run());
        vmASEJ.inicioRangoASEJ.addListener((obsASEJ, aASEJ, bASEJ) -> redibujarASEJ.run());
        vmASEJ.diferenciaASEJ.addListener((obsASEJ, aASEJ, bASEJ) -> redibujarASEJ.run());
        vmASEJ.modoCompararASEJ.addListener((obsASEJ, aASEJ, bASEJ) -> redibujarASEJ.run());
    }

    private record EntradaPaletaASEJ(String grupoASEJ, HallazgoTipoDtoASEJ tipoASEJ) {
    }

    private void configurarPaletaASEJ() {
        lstPaletaASEJ.setCellFactory(lASEJ -> new CeldaPaletaASEJ());
        lstPaletaASEJ.getSelectionModel().selectedItemProperty().addListener((obsASEJ, aASEJ, bASEJ) -> {
            if (bASEJ != null && bASEJ.tipoASEJ() != null && bASEJ.tipoASEJ() != vmASEJ.herramientaASEJ.get()) {
                vmASEJ.herramientaASEJ.set(bASEJ.tipoASEJ());
            }
        });
        txtBuscarHallazgoASEJ.textProperty().addListener((obsASEJ, aASEJ, bASEJ) -> filtrarPaletaASEJ());
    }

    private void filtrarPaletaASEJ() {
        String filtroASEJ = txtBuscarHallazgoASEJ.getText() == null ? "" : normalizarASEJ(txtBuscarHallazgoASEJ.getText().trim());
        Map<RepresentacionASEJ, List<HallazgoTipoDtoASEJ>> porGrupoASEJ = tiposPaletaASEJ.stream()
                .filter(tASEJ -> filtroASEJ.isEmpty() || coincideASEJ(tASEJ, filtroASEJ))
                .collect(Collectors.groupingBy(HallazgoTipoDtoASEJ::representacionASEJ, () -> new EnumMap<>(RepresentacionASEJ.class),
                        Collectors.toList()));
        List<EntradaPaletaASEJ> entradasASEJ = new ArrayList<>();
        porGrupoASEJ.forEach((representacionASEJ, listaASEJ) -> {
            entradasASEJ.add(new EntradaPaletaASEJ(GRUPOSASEJ.get(representacionASEJ) + " (" + listaASEJ.size() + ")", null));
            listaASEJ.forEach(tASEJ -> entradasASEJ.add(new EntradaPaletaASEJ(null, tASEJ)));
        });
        lstPaletaASEJ.getItems().setAll(entradasASEJ);
        lstPaletaASEJ.setPlaceholder(new Label("Ningún hallazgo coincide."));
        HallazgoTipoDtoASEJ actualASEJ = vmASEJ.herramientaASEJ.get();
        entradasASEJ.stream().filter(eASEJ -> eASEJ.tipoASEJ() != null && actualASEJ != null && eASEJ.tipoASEJ().idASEJ() == actualASEJ.idASEJ()).findFirst()
                .ifPresent(eASEJ -> lstPaletaASEJ.getSelectionModel().select(eASEJ));
    }

    private static boolean coincideASEJ(HallazgoTipoDtoASEJ tASEJ, String filtroASEJ) {
        if (normalizarASEJ(tASEJ.nombreASEJ()).contains(filtroASEJ) || normalizarASEJ(tASEJ.codigoASEJ()).contains(filtroASEJ)) {
            return true;
        }
        return (tASEJ.siglaASEJ() != null && normalizarASEJ(tASEJ.siglaASEJ()).contains(filtroASEJ))
                || tASEJ.siglasOpcionesASEJ().stream().anyMatch(sASEJ -> normalizarASEJ(sASEJ).contains(filtroASEJ));
    }

    private static String normalizarASEJ(String textoASEJ) {
        return Normalizer.normalize(textoASEJ, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    private final class CeldaPaletaASEJ extends ListCell<EntradaPaletaASEJ> {
        private final Canvas muestraASEJ = new Canvas(28, 28);

        @Override
        protected void updateItem(EntradaPaletaASEJ eASEJ, boolean vacioASEJ) {
            super.updateItem(eASEJ, vacioASEJ);
            getStyleClass().remove("paleta-grupo");
            setDisable(false);
            if (vacioASEJ || eASEJ == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            if (eASEJ.tipoASEJ() == null) {
                getStyleClass().add("paleta-grupo");
                setDisable(true);
                setGraphic(null);
                setText(eASEJ.grupoASEJ());
                return;
            }
            HallazgoTipoDtoASEJ tASEJ = eASEJ.tipoASEJ();
            renderizadorASEJ.muestraASEJ(muestraASEJ.getGraphicsContext2D(), definicionASEJ(tASEJ), 28,
                    TemaASEJ.actualASEJ() == TemaASEJ.OSCURO ? PaletaOdontogramaASEJ.oscuraASEJ() : PaletaOdontogramaASEJ.claraASEJ());
            setGraphic(muestraASEJ);
            setText(tASEJ.nombreASEJ());
        }
    }

    static TipoHallazgoDefASEJ definicionASEJ(HallazgoTipoDtoASEJ tASEJ) {
        return new TipoHallazgoDefASEJ(tASEJ.idASEJ(), tASEJ.codigoASEJ(), tASEJ.nombreASEJ(), tASEJ.siglaASEJ(), tASEJ.siglasOpcionesASEJ(), tASEJ.representacionASEJ(),
                tASEJ.simboloASEJ(), tASEJ.reglaColorASEJ(), tASEJ.grupoExclusionASEJ(), tASEJ.ocultaSuperficiesASEJ(), tASEJ.conteoCpodASEJ(), tASEJ.conteoCeodASEJ());
    }

    private void actualizarIndicacionesASEJ() {
        HallazgoTipoDtoASEJ tASEJ = vmASEJ.herramientaASEJ.get();
        btnRegistrarASEJ.setDisable(!vmASEJ.hayPendienteASEJ());
        boolean usaEstadoASEJ = tASEJ != null && tASEJ.reglaColorASEJ() == ReglaColorASEJ.POR_ESTADO;
        rbBuenoASEJ.setDisable(!usaEstadoASEJ);
        rbMaloASEJ.setDisable(!usaEstadoASEJ);
        if (vmASEJ.soloLecturaASEJ.get()) {
            lblPendienteASEJ.setText(vmASEJ.vistaASEJ.get() == null || vmASEJ.vistaASEJ.get().odontogramaIdASEJ() == null ? ""
                    : "Solo lectura: seleccione una pieza para ver sus hallazgos.");
        } else if (tASEJ == null) {
            lblPendienteASEJ.setText("Elija un hallazgo de la paleta o haga clic en una pieza para inspeccionarla.");
        } else if (tASEJ.representacionASEJ() == RepresentacionASEJ.SUPERFICIE) {
            lblPendienteASEJ.setText(vmASEJ.hayPendienteASEJ()
                    ? "Pieza " + vmASEJ.piezaPendienteASEJ.get() + ": " + vmASEJ.superficiesPendientesASEJ.stream()
                    .map(sASEJ -> sASEJ.etiquetaASEJ(PiezaDentalASEJ.deASEJ(vmASEJ.piezaPendienteASEJ.get()))).collect(Collectors.joining(", "))
                    + ". Pulse Enter para registrar."
                    : tASEJ.nombreASEJ() + ": haga clic en las superficies de una pieza.");
        } else if (tASEJ.representacionASEJ() == RepresentacionASEJ.PIEZA) {
            lblPendienteASEJ.setText(tASEJ.nombreASEJ() + ": haga clic en la pieza.");
        } else {
            lblPendienteASEJ.setText(vmASEJ.inicioRangoASEJ.get() == null ? tASEJ.nombreASEJ() + ": haga clic en la primera pieza."
                    : tASEJ.nombreASEJ() + ": ahora haga clic en la otra pieza.");
        }
    }

    private Optional<String> elegirSiglaASEJ(HallazgoTipoDtoASEJ tASEJ) {
        ChoiceDialog<String> dialogoASEJ = new ChoiceDialog<>(tASEJ.siglasOpcionesASEJ().get(0), tASEJ.siglasOpcionesASEJ());
        dialogoASEJ.setTitle("Sigla del hallazgo");
        dialogoASEJ.setHeaderText(tASEJ.nombreASEJ());
        dialogoASEJ.setContentText("Sigla:");
        dialogoASEJ.initOwner(raizASEJ.getScene().getWindow());
        return dialogoASEJ.showAndWait();
    }

    private void atajosASEJ(KeyEvent eASEJ) {
        if (eASEJ.getTarget() instanceof TextInputControl) {
            return;
        }
        if (eASEJ.getCode() == KeyCode.ENTER && vmASEJ.registrarPendienteASEJ()) {
            eASEJ.consume();
        } else if (eASEJ.getCode() == KeyCode.ESCAPE) {
            vmASEJ.cancelarASEJ();
            eASEJ.consume();
        } else if (eASEJ.getCode() == KeyCode.B && !rbBuenoASEJ.isDisabled()) {
            vmASEJ.estadoActivoASEJ.set(EstadoHallazgoASEJ.BUENO);
        } else if (eASEJ.getCode() == KeyCode.M && !rbMaloASEJ.isDisabled()) {
            vmASEJ.estadoActivoASEJ.set(EstadoHallazgoASEJ.MALO);
        }
    }

    private void configurarLineaDeTiempoASEJ() {
        cmbMomentoASEJ.valueProperty().addListener((obsASEJ, aASEJ, bASEJ) -> {
            if (actualizandoASEJ || bASEJ == null) {
                return;
            }
            actualizandoASEJ = true;
            sldLineaASEJ.setValue(cmbMomentoASEJ.getItems().indexOf(bASEJ));
            actualizandoASEJ = false;
            vmASEJ.fechaVistaASEJ.set(bASEJ.valorASEJ());
            recargarASEJ();
        });
        sldLineaASEJ.valueProperty().addListener((obsASEJ, aASEJ, bASEJ) -> {
            if (actualizandoASEJ) {
                return;
            }
            int iASEJ = (int) Math.round(bASEJ.doubleValue());
            if (iASEJ >= 0 && iASEJ < cmbMomentoASEJ.getItems().size() && cmbMomentoASEJ.getSelectionModel().getSelectedIndex() != iASEJ) {
                cmbMomentoASEJ.getSelectionModel().select(iASEJ);
            }
        });
    }

    private void llenarLineaDeTiempoASEJ(OdontogramaVistaASEJ vASEJ) {
        actualizandoASEJ = true;
        try {
            List<OpcionASEJ<LocalDateTime>> opcionesASEJ = new ArrayList<>();
            for (OdontogramaResumenASEJ rASEJ : resumenesASEJ) {
                if (!rASEJ.borradorASEJ()) {
                    opcionesASEJ.add(new OpcionASEJ<>(rASEJ.etiquetaASEJ(), rASEJ.cerradoEnASEJ()));
                }
            }
            boolean hayBorradorASEJ = resumenesASEJ.stream().anyMatch(OdontogramaResumenASEJ::borradorASEJ);
            if (!resumenesASEJ.isEmpty()) {
                opcionesASEJ.add(new OpcionASEJ<>(hayBorradorASEJ ? "Actual (borrador)" : "Actual", null));
            }
            cmbMomentoASEJ.getItems().setAll(opcionesASEJ);
            OpcionASEJ<LocalDateTime> elegidaASEJ = opcionesASEJ.stream()
                    .filter(oASEJ -> vASEJ.hastaASEJ() == null ? oASEJ.valorASEJ() == null : vASEJ.hastaASEJ().equals(oASEJ.valorASEJ()))
                    .findFirst().orElse(opcionesASEJ.isEmpty() ? null : opcionesASEJ.get(opcionesASEJ.size() - 1));
            cmbMomentoASEJ.setValue(elegidaASEJ);
            sldLineaASEJ.setMax(Math.max(0, opcionesASEJ.size() - 1));
            sldLineaASEJ.setValue(elegidaASEJ == null ? 0 : opcionesASEJ.indexOf(elegidaASEJ));
            sldLineaASEJ.setDisable(opcionesASEJ.size() < 2);
            cmbMomentoASEJ.setDisable(opcionesASEJ.isEmpty());
        } finally {
            actualizandoASEJ = false;
        }
    }

    private Optional<OdontogramaResumenASEJ> inicialCerradoASEJ() {
        return resumenesASEJ.stream().filter(rASEJ -> rASEJ.tipoASEJ() == TipoOdontogramaASEJ.INICIAL && !rASEJ.borradorASEJ()).findFirst();
    }

    private void compararASEJ(boolean activarASEJ) {
        vmASEJ.modoCompararASEJ.set(activarASEJ);
        PacienteDetalleASEJ pASEJ = pacienteActivoASEJ.getASEJ();
        Optional<OdontogramaResumenASEJ> inicialASEJ = inicialCerradoASEJ();
        if (!activarASEJ || pASEJ == null || inicialASEJ.isEmpty()) {
            vmASEJ.diferenciaASEJ.set(null);
            return;
        }
        LocalDateTime objetivoASEJ = vmASEJ.fechaVistaASEJ.get();
        tareaUiASEJ.ejecutarASEJ(() -> odontogramaServiceASEJ.compararASEJ(pASEJ.idASEJ(), inicialASEJ.get().cerradoEnASEJ(), objetivoASEJ), dASEJ -> {
            vmASEJ.diferenciaASEJ.set(dASEJ);
            int piezasASEJ = dASEJ.diferenciaASEJ().piezasAfectadasASEJ().size();
            lblEstadoASEJ.setText(piezasASEJ == 0 ? "Sin cambios respecto del inicial"
                    : piezasASEJ + (piezasASEJ == 1 ? " pieza con cambios" : " piezas con cambios") + " respecto del inicial (marco ámbar)");
        });
    }

    private void configurarInspectorASEJ() {
        lstHallazgosASEJ.setItems(vmASEJ.hallazgosPiezaASEJ);
        lstHallazgosASEJ.setCellFactory(lASEJ -> new ListCell<>() {
            private final Label textoASEJ = new Label();
            private final Circle puntoASEJ = new Circle(5);
            private final HBox filaASEJ = new HBox(8, puntoASEJ, textoASEJ);

            {
                textoASEJ.setWrapText(true);
                textoASEJ.maxWidthProperty().bind(lASEJ.widthProperty().subtract(44));
                filaASEJ.setAlignment(Pos.TOP_LEFT);
                HBox.setMargin(puntoASEJ, new Insets(5, 0, 0, 0));
            }

            @Override
            protected void updateItem(HallazgoDtoASEJ hASEJ, boolean vacioASEJ) {
                super.updateItem(hASEJ, vacioASEJ);
                setText(null);
                textoASEJ.getStyleClass().removeAll(Styles.TEXT_MUTED);
                if (vacioASEJ || hASEJ == null) {
                    setGraphic(null);
                    return;
                }
                PaletaOdontogramaASEJ coloresASEJ = PaletaOdontogramaASEJ.claraASEJ();
                puntoASEJ.setFill(hASEJ.anuladoASEJ() ? Color.GRAY : coloresASEJ.colorASEJ(hASEJ.colorASEJ()));
                textoASEJ.setText(textoHallazgoASEJ(hASEJ));
                if (!hASEJ.vigenteASEJ()) {
                    textoASEJ.getStyleClass().add(Styles.TEXT_MUTED);
                }
                setGraphic(filaASEJ);
            }
        });
        lstHallazgosASEJ.getSelectionModel().selectedItemProperty().addListener((obsASEJ, aASEJ, bASEJ) -> actualizarBotonesInspectorASEJ());
        actualizarBotonesInspectorASEJ();
    }

    private static String textoHallazgoASEJ(HallazgoDtoASEJ hASEJ) {
        StringBuilder sbASEJ = new StringBuilder();
        if (hASEJ.anuladoASEJ()) {
            sbASEJ.append("[ANULADO] ");
        } else if (!hASEJ.vigenteASEJ()) {
            sbASEJ.append("[Reemplazado] ");
        }
        sbASEJ.append(hASEJ.nombreTipoASEJ());
        if (hASEJ.siglaASEJ() != null) {
            sbASEJ.append(" (").append(hASEJ.siglaASEJ()).append(')');
        }
        if (hASEJ.piezaFinASEJ() != null) {
            sbASEJ.append(" · ").append(hASEJ.piezaASEJ()).append('–').append(hASEJ.piezaFinASEJ());
        }
        if (!hASEJ.superficiesASEJ().isEmpty()) {
            PiezaDentalASEJ pASEJ = PiezaDentalASEJ.deASEJ(hASEJ.piezaASEJ());
            sbASEJ.append(" · ").append(hASEJ.superficiesASEJ().stream().sorted().map(sASEJ -> sASEJ.etiquetaASEJ(pASEJ)).collect(Collectors.joining()));
        }
        if (hASEJ.estadoASEJ() != null) {
            sbASEJ.append(" · ").append(hASEJ.estadoASEJ() == EstadoHallazgoASEJ.BUENO ? "bueno" : "malo");
        }
        sbASEJ.append("\n").append(FormulariosASEJ.fechaHoraASEJ(hASEJ.registradoEnASEJ())).append(" · ").append(hASEJ.registradoPorASEJ());
        if (hASEJ.anuladoASEJ()) {
            sbASEJ.append("\nMotivo: ").append(hASEJ.motivoAnulacionASEJ());
        }
        return sbASEJ.toString();
    }

    private void actualizarInspectorTituloASEJ() {
        Integer fdiASEJ = vmASEJ.piezaSeleccionadaASEJ.get();
        if (fdiASEJ == null) {
            lblPiezaASEJ.setText("Inspector");
            lblPiezaDetalleASEJ.setText("Haga clic en una pieza para ver sus hallazgos.");
            return;
        }
        PiezaDentalASEJ pASEJ = PiezaDentalASEJ.deASEJ(fdiASEJ);
        lblPiezaASEJ.setText("Pieza " + fdiASEJ);
        lblPiezaDetalleASEJ.setText(capitalizarASEJ(pASEJ.tipoASEJ().name()) + " · " + capitalizarASEJ(pASEJ.tipoRadicularASEJ().name())
                + (pASEJ.esDeciduaASEJ() ? " · decidua" : " · permanente"));
    }

    private static String capitalizarASEJ(String sASEJ) {
        return sASEJ.charAt(0) + sASEJ.substring(1).toLowerCase();
    }

    private void actualizarBotonesInspectorASEJ() {
        HallazgoDtoASEJ hASEJ = lstHallazgosASEJ.getSelectionModel().getSelectedItem();
        OdontogramaVistaASEJ vASEJ = vmASEJ.vistaASEJ.get();
        boolean permisoASEJ = sesionUsuarioASEJ.tieneASEJ(PermisoASEJ.ODONTOGRAMA_EDITAR);
        boolean actualASEJ = vASEJ != null && vASEJ.hastaASEJ() == null;
        btnEliminarASEJ.setDisable(hASEJ == null || !hASEJ.eliminableASEJ() || !permisoASEJ);
        btnAnularASEJ.setDisable(hASEJ == null || hASEJ.eliminableASEJ() || hASEJ.anuladoASEJ() || !permisoASEJ || !actualASEJ);
    }

    private void registrarASEJ(NuevoHallazgoASEJ nuevoASEJ) {
        tareaUiASEJ.ejecutarASEJ(() -> odontogramaServiceASEJ.registrarHallazgoASEJ(nuevoASEJ), this::aplicarVistaASEJ);
    }

    private void cambiarDenticionASEJ(DenticionASEJ denticionASEJ) {
        OdontogramaVistaASEJ vASEJ = vmASEJ.vistaASEJ.get();
        if (vASEJ == null || !vASEJ.editableASEJ() || vASEJ.denticionASEJ() == denticionASEJ) {
            return;
        }
        tareaUiASEJ.ejecutarASEJ(() -> odontogramaServiceASEJ.cambiarDenticionASEJ(vASEJ.odontogramaIdASEJ(), denticionASEJ), this::aplicarVistaASEJ,
                errorASEJ -> {
                    tareaUiASEJ.manejarErrorASEJ(errorASEJ);
                    aplicarVistaASEJ(vASEJ);
                });
    }

    @FXML
    private void onModoSeleccionASEJ() {
        vmASEJ.herramientaASEJ.set(null);
    }

    @FXML
    private void onRegistrarASEJ() {
        vmASEJ.registrarPendienteASEJ();
    }

    @FXML
    private void onCerrarASEJ() {
        OdontogramaVistaASEJ vASEJ = vmASEJ.vistaASEJ.get();
        if (vASEJ == null || !vASEJ.editableASEJ() || !notificadorASEJ.confirmarASEJ("Cerrar y firmar el odontograma",
                "El odontograma quedará firmado con su hash de integridad y ya no podrá modificarse; "
                        + "las correcciones se harán por anulación con motivo. ¿Continuar?")) {
            return;
        }
        tareaUiASEJ.ejecutarASEJ(btnCerrarASEJ, () -> odontogramaServiceASEJ.cerrarASEJ(vASEJ.odontogramaIdASEJ()), cerradoASEJ -> {
            notificadorASEJ.exitoASEJ("Odontograma cerrado y firmado.");
            recargarASEJ();
        });
    }

    @FXML
    private void onNuevaEvolucionASEJ() {
        OdontogramaVistaASEJ vASEJ = vmASEJ.vistaASEJ.get();
        if (atencionAbiertaASEJ.isEmpty() || vASEJ == null) {
            return;
        }
        long atencionIdASEJ = atencionAbiertaASEJ.get().idASEJ();
        tareaUiASEJ.ejecutarASEJ(btnNuevaEvolucionASEJ, () -> odontogramaServiceASEJ.abrirBorradorASEJ(atencionIdASEJ, vASEJ.denticionASEJ()), idASEJ -> {
            notificadorASEJ.exitoASEJ("Odontograma de evolución abierto.");
            recargarASEJ();
        });
    }

    @FXML
    private void onNuevaAtencionASEJ() {
        eventosASEJ.publishEvent(new SolicitudNuevaAtencionASEJ());
    }

    @FXML
    private void onGuardarTextosASEJ() {
        OdontogramaVistaASEJ vASEJ = vmASEJ.vistaASEJ.get();
        if (vASEJ == null || !vASEJ.editableASEJ()) {
            return;
        }
        String espASEJ = txtEspecificacionesASEJ.getText();
        String obsASEJ = txtObservacionesASEJ.getText();
        tareaUiASEJ.ejecutarASEJ(btnGuardarTextosASEJ, () -> odontogramaServiceASEJ.actualizarEspecificacionesASEJ(vASEJ.odontogramaIdASEJ(), espASEJ, obsASEJ), gASEJ -> {
            aplicarVistaASEJ(gASEJ);
            notificadorASEJ.exitoASEJ("Especificaciones guardadas.");
        });
    }

    @FXML
    private void onEliminarASEJ() {
        HallazgoDtoASEJ hASEJ = lstHallazgosASEJ.getSelectionModel().getSelectedItem();
        if (hASEJ == null || !notificadorASEJ.confirmarASEJ("Eliminar hallazgo",
                "¿Eliminar \"" + hASEJ.nombreTipoASEJ() + "\" de la pieza " + hASEJ.piezaASEJ() + "? El odontograma aún está en borrador.")) {
            return;
        }
        tareaUiASEJ.ejecutarASEJ(btnEliminarASEJ, () -> odontogramaServiceASEJ.eliminarHallazgoBorradorASEJ(hASEJ.idASEJ()), this::aplicarVistaASEJ);
    }

    @FXML
    private void onAnularASEJ() {
        HallazgoDtoASEJ hASEJ = lstHallazgosASEJ.getSelectionModel().getSelectedItem();
        if (hASEJ == null) {
            return;
        }
        TextArea motivoASEJ = new TextArea();
        motivoASEJ.setPrefRowCount(3);
        motivoASEJ.setWrapText(true);
        motivoASEJ.setPromptText("Explique el error (mínimo " + OdontogramaServiceASEJ.LARGO_MINIMO_MOTIVOASEJ + " caracteres)");
        Label ayudaASEJ = new Label("El hallazgo no se borra: queda en el historial marcado como anulado y el hash del "
                + "odontograma firmado no cambia (NTS 188, 5.6).");
        ayudaASEJ.setWrapText(true);
        ayudaASEJ.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);
        Label tituloASEJ = new Label(hASEJ.nombreTipoASEJ() + " · pieza " + hASEJ.piezaASEJ() + " · " + FormulariosASEJ.fechaHoraASEJ(hASEJ.registradoEnASEJ()));
        tituloASEJ.setWrapText(true);
        Button cancelarASEJ = FormulariosASEJ.botonASEJ("Cancelar", null);
        cancelarASEJ.setOnAction(eASEJ -> notificadorASEJ.cerrarPanelASEJ());
        Button anularASEJ = FormulariosASEJ.botonASEJ("Anular hallazgo", Feather.SLASH, Styles.DANGER);
        anularASEJ.setOnAction(eASEJ -> tareaUiASEJ.ejecutarASEJ(anularASEJ, () -> odontogramaServiceASEJ.anularHallazgoASEJ(hASEJ.idASEJ(), motivoASEJ.getText()), vASEJ -> {
            notificadorASEJ.cerrarPanelASEJ();
            notificadorASEJ.exitoASEJ("Hallazgo anulado.");
            aplicarVistaASEJ(vASEJ);
        }));
        notificadorASEJ.abrirPanelASEJ(FormulariosASEJ.panelLateralASEJ("Anular hallazgo", new VBox(10, tituloASEJ, motivoASEJ, ayudaASEJ), cancelarASEJ, anularASEJ));
    }

    @FXML
    private void onPdfASEJ() {
        OdontogramaVistaASEJ vASEJ = vmASEJ.vistaASEJ.get();
        PacienteDetalleASEJ pASEJ = pacienteActivoASEJ.getASEJ();
        if (vASEJ == null || pASEJ == null || vASEJ.odontogramaIdASEJ() == null) {
            return;
        }
        BufferedImage imagenASEJ = SwingFXUtils.fromFXImage(renderizadorASEJ.imagenASEJ(1600, 900, vASEJ.denticionASEJ(), vASEJ.estadoBocaASEJ(),
                vmASEJ.superposicionASEJ().diferenciasASEJ()), null);
        LocalDateTime hastaASEJ = vASEJ.hastaASEJ();
        exportadorASEJ.guardarPdfASEJ(btnPdfASEJ, "ficha-odontologica-" + pASEJ.numeroHistoriaASEJ() + ".pdf", () -> {
            ByteArrayOutputStream pngASEJ = new ByteArrayOutputStream();
            ImageIO.write(imagenASEJ, "png", pngASEJ);
            return odontogramaServiceASEJ.pdfFichaASEJ(pASEJ.idASEJ(), hastaASEJ, pngASEJ.toByteArray());
        });
    }
}
