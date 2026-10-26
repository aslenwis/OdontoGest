package com.odontogest.ui.controlador;

import atlantafx.base.theme.Styles;
import com.odontogest.dominio.comun.NumeroALetrasASEJ;
import com.odontogest.dominio.finanzas.EstadoCuotaASEJ;
import com.odontogest.dominio.finanzas.FrecuenciaASEJ;
import com.odontogest.dominio.odontograma.PiezaDentalASEJ;
import com.odontogest.dominio.odontograma.SuperficieASEJ;
import com.odontogest.dominio.tratamiento.FormaCobroASEJ;
import com.odontogest.evento.PagoRegistradoASEJ;
import com.odontogest.evento.PlanCambiadoASEJ;
import com.odontogest.modelo.EstadoPlanASEJ;
import com.odontogest.modelo.RolASEJ;
import com.odontogest.seguridad.PermisoASEJ;
import com.odontogest.seguridad.SesionUsuarioASEJ;
import com.odontogest.servicio.OdontologoServiceASEJ;
import com.odontogest.servicio.PlanTratamientoServiceASEJ;
import com.odontogest.servicio.ProcedimientoServiceASEJ;
import com.odontogest.servicio.dto.CronogramaFormASEJ;
import com.odontogest.servicio.dto.CuotaDtoASEJ;
import com.odontogest.servicio.dto.OdontologoDtoASEJ;
import com.odontogest.servicio.dto.PacienteDetalleASEJ;
import com.odontogest.servicio.dto.PlanDtoASEJ;
import com.odontogest.servicio.dto.PlanItemDtoASEJ;
import com.odontogest.servicio.dto.PlanItemFormASEJ;
import com.odontogest.servicio.dto.PlanResumenASEJ;
import com.odontogest.servicio.dto.ProcedimientoDtoASEJ;
import com.odontogest.servicio.dto.UsuarioSesionASEJ;
import com.odontogest.ui.soporte.ExportadorASEJ;
import com.odontogest.ui.soporte.FormulariosASEJ.OpcionASEJ;
import com.odontogest.ui.soporte.FormulariosASEJ;
import com.odontogest.ui.soporte.HiloUiASEJ;
import com.odontogest.ui.soporte.NotificadorASEJ;
import com.odontogest.ui.soporte.PacienteActivoASEJ;
import com.odontogest.ui.soporte.TareaUiASEJ;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.converter.DefaultStringConverter;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.odontogest.ui.soporte.FormulariosASEJ.ESTADO_ALERTAASEJ;
import static com.odontogest.ui.soporte.FormulariosASEJ.ESTADO_OKASEJ;
import static com.odontogest.ui.soporte.FormulariosASEJ.ESTADO_PELIGROASEJ;

@Component
@RequiredArgsConstructor
public class PlanTratamientoControllerASEJ {

    private static final List<EstadoPlanASEJ> ORDEN_TRANSICIONESASEJ = List.of(EstadoPlanASEJ.PROPUESTO, EstadoPlanASEJ.ACEPTADO,
            EstadoPlanASEJ.EN_CURSO, EstadoPlanASEJ.FINALIZADO, EstadoPlanASEJ.BORRADOR, EstadoPlanASEJ.ANULADO);

    private final PlanTratamientoServiceASEJ planServiceASEJ;
    private final OdontologoServiceASEJ odontologoServiceASEJ;
    private final ProcedimientoServiceASEJ procedimientoServiceASEJ;
    private final PacienteActivoASEJ pacienteActivoASEJ;
    private final TareaUiASEJ tareaUiASEJ;
    private final NotificadorASEJ notificadorASEJ;
    private final SesionUsuarioASEJ sesionUsuarioASEJ;
    private final ExportadorASEJ exportadorASEJ;
    private final ApplicationEventPublisher eventosASEJ;

    @FXML private SplitPane raizASEJ;
    @FXML private ComboBox<PlanResumenASEJ> cmbPlanesASEJ;
    @FXML private Button btnNuevoASEJ;
    @FXML private Button btnSugerirASEJ;
    @FXML private Button btnAgregarASEJ;
    @FXML private Button btnQuitarASEJ;
    @FXML private Label lblEstadoASEJ;
    @FXML private HBox boxTransicionesASEJ;
    @FXML private Button btnPresupuestoASEJ;
    @FXML private Button btnConsentimientoASEJ;
    @FXML private Label lblInfoASEJ;
    @FXML private TreeTableView<FilaASEJ> tablaASEJ;
    @FXML private Label lblSubtotalASEJ;
    @FXML private TextField txtDescuentoASEJ;
    @FXML private Label lblTotalASEJ;
    @FXML private Label lblLetrasASEJ;
    @FXML private TextArea txtObservacionesASEJ;
    @FXML private Button btnGuardarCabeceraASEJ;
    @FXML private VBox panelCronogramaASEJ;
    @FXML private TextField txtInicialASEJ;
    @FXML private Spinner<Integer> spnCuotasASEJ;
    @FXML private DatePicker dpPrimerVencimientoASEJ;
    @FXML private ChoiceBox<FrecuenciaASEJ> chFrecuenciaASEJ;
    @FXML private Button btnGenerarASEJ;
    @FXML private Label lblCronogramaASEJ;
    @FXML private TableView<CuotaDtoASEJ> tblCuotasASEJ;

    private ChangeListener<PacienteDetalleASEJ> oyentePacienteASEJ;
    private PlanDtoASEJ actualASEJ;
    private boolean llenandoASEJ;
    private boolean avisandoASEJ;

    public record FilaASEJ(String faseASEJ, BigDecimal subtotalFaseASEJ, PlanItemDtoASEJ itemASEJ) {

        boolean esGrupoASEJ() {
            return itemASEJ == null;
        }
    }

    @FXML
    public void initialize() {
        configurarTablaASEJ();
        configurarCuotasASEJ();
        spnCuotasASEJ.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 60, 3));
        FormulariosASEJ.configurarFechaASEJ(dpPrimerVencimientoASEJ);
        chFrecuenciaASEJ.getItems().setAll(FrecuenciaASEJ.values());
        chFrecuenciaASEJ.setConverter(FormulariosASEJ.conversorASEJ(fASEJ -> switch (fASEJ) {
            case SEMANAL -> "Semanal";
            case QUINCENAL -> "Quincenal";
            case MENSUAL -> "Mensual";
        }));
        chFrecuenciaASEJ.setValue(FrecuenciaASEJ.MENSUAL);
        cmbPlanesASEJ.valueProperty().addListener((obsASEJ, aASEJ, bASEJ) -> {
            if (!llenandoASEJ && bASEJ != null && (actualASEJ == null || actualASEJ.idASEJ() != bASEJ.idASEJ())) {
                cargarPlanASEJ(bASEJ.idASEJ());
            }
        });
        boolean editaASEJ = sesionUsuarioASEJ.tieneASEJ(PermisoASEJ.PLAN_EDITAR);
        btnNuevoASEJ.setVisible(editaASEJ);
        btnNuevoASEJ.setManaged(editaASEJ);
        mostrarASEJ(null);

        if (oyentePacienteASEJ != null) {
            pacienteActivoASEJ.pacientePropertyASEJ().removeListener(oyentePacienteASEJ);
        }
        oyentePacienteASEJ = (obsASEJ, anteriorASEJ, nuevoASEJ) -> {
            if (nuevoASEJ == null || anteriorASEJ == null || nuevoASEJ.idASEJ() != anteriorASEJ.idASEJ()) {
                actualASEJ = null;
            }
            recargarASEJ(null);
        };
        pacienteActivoASEJ.pacientePropertyASEJ().addListener(oyentePacienteASEJ);
        recargarASEJ(null);
    }

    public void recargarASEJ(Long seleccionarASEJ) {
        PacienteDetalleASEJ pASEJ = pacienteActivoASEJ.getASEJ();
        if (pASEJ == null || cmbPlanesASEJ == null) {
            return;
        }
        Long objetivoASEJ = seleccionarASEJ != null ? seleccionarASEJ : actualASEJ == null ? null : actualASEJ.idASEJ();
        tareaUiASEJ.ejecutarASEJ(() -> {
            List<PlanResumenASEJ> planesASEJ = planServiceASEJ.listarPorPacienteASEJ(pASEJ.idASEJ());
            PlanResumenASEJ elegidoASEJ = planesASEJ.stream().filter(rASEJ -> objetivoASEJ != null && rASEJ.idASEJ() == objetivoASEJ).findFirst()
                    .orElse(planesASEJ.isEmpty() ? null : planesASEJ.get(0));
            return new CargaASEJ(planesASEJ, elegidoASEJ == null ? null : planServiceASEJ.obtenerASEJ(elegidoASEJ.idASEJ()));
        }, cASEJ -> {
            llenandoASEJ = true;
            cmbPlanesASEJ.getItems().setAll(cASEJ.planesASEJ());
            cmbPlanesASEJ.setValue(cASEJ.planASEJ() == null ? null : cASEJ.planesASEJ().stream()
                    .filter(rASEJ -> rASEJ.idASEJ() == cASEJ.planASEJ().idASEJ()).findFirst().orElse(null));
            llenandoASEJ = false;
            mostrarASEJ(cASEJ.planASEJ());
        });
    }

    private record CargaASEJ(List<PlanResumenASEJ> planesASEJ, PlanDtoASEJ planASEJ) {
    }

    private void cargarPlanASEJ(long idASEJ) {
        tareaUiASEJ.ejecutarASEJ(() -> planServiceASEJ.obtenerASEJ(idASEJ), this::mostrarASEJ);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void alRegistrarPagoASEJ(PagoRegistradoASEJ eASEJ) {
        HiloUiASEJ.ejecutarASEJ(() -> {
            PacienteDetalleASEJ pASEJ = pacienteActivoASEJ.getASEJ();
            if (cmbPlanesASEJ != null && pASEJ != null && pASEJ.idASEJ() == eASEJ.pacienteIdASEJ()) {
                recargarASEJ(null);
            }
        });
    }

    @EventListener
    public void alCambiarPlanASEJ(PlanCambiadoASEJ eASEJ) {
        if (avisandoASEJ) {
            return;
        }
        Runnable rASEJ = () -> {
            PacienteDetalleASEJ pASEJ = pacienteActivoASEJ.getASEJ();
            if (pASEJ != null && pASEJ.idASEJ() == eASEJ.pacienteIdASEJ()) {
                recargarASEJ(null);
            }
        };
        HiloUiASEJ.ejecutarASEJ(rASEJ);
    }

    private void aplicarASEJ(PlanDtoASEJ pASEJ) {
        boolean cambioEstadoASEJ = actualASEJ == null || actualASEJ.estadoASEJ() != pASEJ.estadoASEJ() || actualASEJ.totalASEJ().compareTo(pASEJ.totalASEJ()) != 0;
        mostrarASEJ(pASEJ);
        if (cambioEstadoASEJ) {
            int iASEJ = cmbPlanesASEJ.getItems().indexOf(cmbPlanesASEJ.getItems().stream().filter(rASEJ -> rASEJ.idASEJ() == pASEJ.idASEJ()).findFirst()
                    .orElse(null));
            if (iASEJ >= 0) {
                llenandoASEJ = true;
                PlanResumenASEJ nuevoASEJ = new PlanResumenASEJ(pASEJ.idASEJ(), pASEJ.fechaASEJ(), pASEJ.estadoASEJ(), pASEJ.totalASEJ(), pASEJ.odontologoNombreASEJ());
                cmbPlanesASEJ.getItems().set(iASEJ, nuevoASEJ);
                cmbPlanesASEJ.setValue(nuevoASEJ);
                llenandoASEJ = false;
            }
        }
        avisarCambioDePlanASEJ(pASEJ.pacienteIdASEJ());
    }

    private void mostrarASEJ(PlanDtoASEJ pASEJ) {
        actualASEJ = pASEJ;
        boolean hayASEJ = pASEJ != null;
        boolean editaASEJ = sesionUsuarioASEJ.tieneASEJ(PermisoASEJ.PLAN_EDITAR);
        boolean editableASEJ = hayASEJ && editaASEJ && pASEJ.editableASEJ();

        btnSugerirASEJ.setDisable(!editableASEJ);
        btnAgregarASEJ.setDisable(!editableASEJ);
        btnQuitarASEJ.setDisable(true);
        btnPresupuestoASEJ.setDisable(!hayASEJ);
        btnConsentimientoASEJ.setDisable(!hayASEJ || pASEJ.itemsASEJ().isEmpty());
        txtDescuentoASEJ.setEditable(editableASEJ);
        txtObservacionesASEJ.setEditable(hayASEJ && editaASEJ && !pASEJ.estadoASEJ().esFinalASEJ());
        btnGuardarCabeceraASEJ.setDisable(!hayASEJ || !editaASEJ || pASEJ.estadoASEJ().esFinalASEJ());

        lblEstadoASEJ.setText(hayASEJ ? pASEJ.estadoASEJ().etiquetaASEJ() : "");
        lblEstadoASEJ.getStyleClass().removeAll(Styles.SUCCESS, Styles.WARNING, Styles.DANGER, Styles.ACCENT);
        if (hayASEJ) {
            lblEstadoASEJ.getStyleClass().add(switch (pASEJ.estadoASEJ()) {
                case BORRADOR, PROPUESTO -> Styles.WARNING;
                case ACEPTADO, EN_CURSO -> Styles.ACCENT;
                case FINALIZADO -> Styles.SUCCESS;
                case ANULADO -> Styles.DANGER;
            });
        }
        construirTransicionesASEJ(pASEJ, editaASEJ);
        lblInfoASEJ.setText(infoASEJ(pASEJ));

        tablaASEJ.setRoot(arbolASEJ(pASEJ));
        tablaASEJ.setPlaceholder(new Label(hayASEJ ? "El plan aún no tiene procedimientos: use «Sugerir desde odontograma» "
                + "o «Agregar procedimiento»." : "El paciente no tiene planes de tratamiento."));

        lblSubtotalASEJ.setText(hayASEJ ? FormulariosASEJ.solesASEJ(pASEJ.subtotalASEJ()) : "—");
        txtDescuentoASEJ.setText(hayASEJ ? pASEJ.descuentoASEJ().setScale(2).toPlainString() : "");
        lblTotalASEJ.setText(hayASEJ ? FormulariosASEJ.solesASEJ(pASEJ.totalASEJ()) : "—");
        lblLetrasASEJ.setText(hayASEJ ? "Son: " + NumeroALetrasASEJ.solesASEJ(pASEJ.totalCentASEJ()) : "");
        txtObservacionesASEJ.setText(hayASEJ && pASEJ.observacionesASEJ() != null ? pASEJ.observacionesASEJ() : "");

        tblCuotasASEJ.getItems().setAll(hayASEJ ? pASEJ.cuotasASEJ() : List.of());
        boolean cronogramaASEJ = hayASEJ && editaASEJ && pASEJ.estadoASEJ().admiteCronogramaASEJ() && !pASEJ.tienePagosASEJ();
        for (var nASEJ : List.of(txtInicialASEJ, spnCuotasASEJ, dpPrimerVencimientoASEJ, chFrecuenciaASEJ, btnGenerarASEJ)) {
            nASEJ.setDisable(!cronogramaASEJ);
        }
        if (hayASEJ && dpPrimerVencimientoASEJ.getValue() == null) {
            dpPrimerVencimientoASEJ.setValue(LocalDate.now().plusMonths(1));
        }
        lblCronogramaASEJ.setText(textoCronogramaASEJ(pASEJ));
    }

    private static String infoASEJ(PlanDtoASEJ pASEJ) {
        if (pASEJ == null) {
            return "";
        }
        StringBuilder sbASEJ = new StringBuilder("Responsable: C.D. ").append(pASEJ.odontologoNombreASEJ())
                .append(" · creado el ").append(FormulariosASEJ.fechaHoraASEJ(pASEJ.fechaASEJ()));
        if (pASEJ.odontogramaIdASEJ() != null) {
            sbASEJ.append(" · sugerencias del odontograma firmado N° ").append(pASEJ.odontogramaIdASEJ());
        }
        if (pASEJ.consentimientoFirmadoPorASEJ() != null) {
            sbASEJ.append(" · consentimiento firmado por ").append(pASEJ.consentimientoFirmadoPorASEJ()).append(" el ")
                    .append(FormulariosASEJ.fechaHoraASEJ(pASEJ.consentimientoEnASEJ()));
        }
        if (!pASEJ.itemsASEJ().isEmpty() && (pASEJ.estadoASEJ().enEjecucionASEJ() || pASEJ.estadoASEJ() == EstadoPlanASEJ.FINALIZADO)) {
            sbASEJ.append(" · ").append(pASEJ.realizadosASEJ()).append(" de ").append(pASEJ.itemsASEJ().size()).append(" realizado(s)");
        }
        return sbASEJ.toString();
    }

    private static String textoCronogramaASEJ(PlanDtoASEJ pASEJ) {
        if (pASEJ == null) {
            return "";
        }
        if (pASEJ.tienePagosASEJ()) {
            return "El plan tiene pagos por " + FormulariosASEJ.solesASEJ(pASEJ.pagadoASEJ()) + ": el cronograma ya no se puede regenerar.";
        }
        if (pASEJ.estadoASEJ() == EstadoPlanASEJ.BORRADOR) {
            return "Proponga el plan para generar el cronograma (el total ya no cambiará).";
        }
        if (pASEJ.estadoASEJ().esFinalASEJ()) {
            return "";
        }
        if (pASEJ.cuotasASEJ().isEmpty()) {
            return "Sin cronograma: al aceptar el plan se crea una cuota única al contado.";
        }
        return "Generar de nuevo reemplaza las cuotas actuales. La cuota inicial vence hoy; los domingos pasan al lunes.";
    }

    private TreeItem<FilaASEJ> arbolASEJ(PlanDtoASEJ pASEJ) {
        TreeItem<FilaASEJ> raizArbolASEJ = new TreeItem<>(new FilaASEJ("", BigDecimal.ZERO, null));
        if (pASEJ == null) {
            return raizArbolASEJ;
        }
        if (pASEJ.itemsASEJ().isEmpty()) {
            return raizArbolASEJ;
        }
        Map<Integer, List<PlanItemDtoASEJ>> porFaseASEJ = pASEJ.itemsASEJ().stream()
                .collect(Collectors.groupingBy(PlanItemDtoASEJ::faseASEJ, TreeMap::new, Collectors.toList()));
        for (int faseASEJ = 1; faseASEJ <= 4; faseASEJ++) {
            List<PlanItemDtoASEJ> itemsASEJ = porFaseASEJ.getOrDefault(faseASEJ, List.of());
            BigDecimal subtotalASEJ = itemsASEJ.stream().map(PlanItemDtoASEJ::totalASEJ).reduce(BigDecimal.ZERO, BigDecimal::add);
            String tituloASEJ = PlanItemDtoASEJ.nombreFaseASEJ(faseASEJ) + (itemsASEJ.isEmpty() ? "  —  sin procedimientos" : "");
            TreeItem<FilaASEJ> grupoASEJ = new TreeItem<>(new FilaASEJ(tituloASEJ, subtotalASEJ, null));
            grupoASEJ.setExpanded(true);
            for (PlanItemDtoASEJ iASEJ : itemsASEJ) {
                grupoASEJ.getChildren().add(new TreeItem<>(new FilaASEJ(null, null, iASEJ)));
            }
            raizArbolASEJ.getChildren().add(grupoASEJ);
        }
        return raizArbolASEJ;
    }

    private void configurarTablaASEJ() {
        TreeTableColumn<FilaASEJ, String> procedimientoASEJ = columnaASEJ("Procedimiento", fASEJ -> fASEJ.esGrupoASEJ() ? fASEJ.faseASEJ()
                : fASEJ.itemASEJ().procedimientoCodigoASEJ() + " · " + fASEJ.itemASEJ().procedimientoNombreASEJ(), 300);
        TreeTableColumn<FilaASEJ, String> piezaASEJ = columnaASEJ("Pieza", fASEJ -> fASEJ.esGrupoASEJ() || fASEJ.itemASEJ().piezaASEJ() == null ? ""
                : String.valueOf(fASEJ.itemASEJ().piezaASEJ()), 60);
        TreeTableColumn<FilaASEJ, String> supASEJ = columnaASEJ("Sup.", fASEJ -> fASEJ.esGrupoASEJ() ? "" : fASEJ.itemASEJ().superficiesTextoASEJ(), 60);
        TreeTableColumn<FilaASEJ, String> cantidadASEJ = columnaASEJ("Cant.", fASEJ -> fASEJ.esGrupoASEJ() ? "" : String.valueOf(fASEJ.itemASEJ().cantidadASEJ()), 55);
        TreeTableColumn<FilaASEJ, String> precioASEJ = columnaASEJ("P. unit.", fASEJ -> fASEJ.esGrupoASEJ() ? "" : FormulariosASEJ.solesASEJ(fASEJ.itemASEJ().precioUnitASEJ()), 95);
        TreeTableColumn<FilaASEJ, String> descuentoASEJ = columnaASEJ("Descuento", fASEJ -> fASEJ.esGrupoASEJ() ? ""
                : fASEJ.itemASEJ().descuentoASEJ().setScale(2).toPlainString(), 95);
        TreeTableColumn<FilaASEJ, String> totalASEJ = columnaASEJ("Total", fASEJ -> FormulariosASEJ.solesASEJ(fASEJ.esGrupoASEJ() ? fASEJ.subtotalFaseASEJ()
                : fASEJ.itemASEJ().totalASEJ()), 100);
        TreeTableColumn<FilaASEJ, String> estadoASEJ = columnaASEJ("Estado", fASEJ -> fASEJ.esGrupoASEJ() ? "" : fASEJ.itemASEJ().estadoASEJ().etiquetaASEJ()
                + (fASEJ.itemASEJ().realizadoEnASEJ() == null ? "" : " · " + FormulariosASEJ.fechaASEJ(fASEJ.itemASEJ().realizadoEnASEJ().toLocalDate())), 140);
        for (var cASEJ : List.of(cantidadASEJ, precioASEJ, descuentoASEJ, totalASEJ)) {
            cASEJ.setStyle("-fx-alignment: CENTER-RIGHT;");
        }

        descuentoASEJ.setEditable(true);
        descuentoASEJ.setCellFactory(cASEJ -> new TextFieldTreeTableCell<>(new DefaultStringConverter()) {
            @Override
            public void startEdit() {
                TreeItem<FilaASEJ> filaASEJ = getTableRow() == null ? null : getTableRow().getTreeItem();
                if (filaASEJ != null && !filaASEJ.getValue().esGrupoASEJ() && actualASEJ != null && actualASEJ.editableASEJ()
                        && sesionUsuarioASEJ.tieneASEJ(PermisoASEJ.PLAN_EDITAR)) {
                    super.startEdit();
                }
            }
        });
        descuentoASEJ.setOnEditCommit(eASEJ -> {
            FilaASEJ fASEJ = eASEJ.getRowValue().getValue();
            try {
                BigDecimal montoASEJ = FormulariosASEJ.montoASEJ(eASEJ.getNewValue(), "descuento");
                tareaUiASEJ.ejecutarASEJ(() -> planServiceASEJ.actualizarDescuentoItemASEJ(fASEJ.itemASEJ().idASEJ(), montoASEJ), this::aplicarASEJ,
                        errorASEJ -> {
                            tareaUiASEJ.manejarErrorASEJ(errorASEJ);
                            mostrarASEJ(actualASEJ);
                        });
            } catch (RuntimeException exASEJ) {
                tareaUiASEJ.manejarErrorASEJ(exASEJ);
                mostrarASEJ(actualASEJ);
            }
        });

        tablaASEJ.getColumns().setAll(List.of(procedimientoASEJ, piezaASEJ, supASEJ, cantidadASEJ, precioASEJ, descuentoASEJ, totalASEJ, estadoASEJ));
        tablaASEJ.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tablaASEJ.setRowFactory(tASEJ -> {
            TreeTableRow<FilaASEJ> filaASEJ = new TreeTableRow<>() {
                @Override
                protected void updateItem(FilaASEJ fASEJ, boolean vacioASEJ) {
                    super.updateItem(fASEJ, vacioASEJ);
                    getStyleClass().remove("plan-fila-fase");
                    if (!vacioASEJ && fASEJ != null && fASEJ.esGrupoASEJ()) {
                        getStyleClass().add("plan-fila-fase");
                    }
                }
            };
            filaASEJ.setOnMouseClicked(eASEJ -> {
                if (eASEJ.getButton() == MouseButton.PRIMARY && eASEJ.getClickCount() == 2 && !filaASEJ.isEmpty()
                        && !filaASEJ.getItem().esGrupoASEJ() && actualASEJ != null && actualASEJ.editableASEJ()
                        && sesionUsuarioASEJ.tieneASEJ(PermisoASEJ.PLAN_EDITAR)) {
                    formularioItemASEJ(filaASEJ.getItem().itemASEJ());
                }
            });
            return filaASEJ;
        });
        tablaASEJ.getSelectionModel().selectedItemProperty().addListener((obsASEJ, aASEJ, bASEJ) -> btnQuitarASEJ.setDisable(
                bASEJ == null || bASEJ.getValue().esGrupoASEJ() || actualASEJ == null || !actualASEJ.editableASEJ()
                        || !sesionUsuarioASEJ.tieneASEJ(PermisoASEJ.PLAN_EDITAR)));
    }

    private static TreeTableColumn<FilaASEJ, String> columnaASEJ(String tituloASEJ, Function<FilaASEJ, String> valorASEJ, double anchoASEJ) {
        TreeTableColumn<FilaASEJ, String> cASEJ = new TreeTableColumn<>(tituloASEJ);
        cASEJ.setCellValueFactory(fASEJ -> new SimpleStringProperty(fASEJ.getValue().getValue() == null ? ""
                : valorASEJ.apply(fASEJ.getValue().getValue())));
        cASEJ.setPrefWidth(anchoASEJ);
        cASEJ.setSortable(false);
        return cASEJ;
    }

    private void configurarCuotasASEJ() {
        TableColumn<CuotaDtoASEJ, String> estadoASEJ = FormulariosASEJ.columnaASEJ("Estado", cASEJ -> etiquetaASEJ(cASEJ.estadoASEJ()), 100);
        estadoASEJ.setCellFactory(cASEJ -> new TableCell<>() {
            @Override
            protected void updateItem(String textoASEJ, boolean vacioASEJ) {
                super.updateItem(textoASEJ, vacioASEJ);
                getStyleClass().removeAll(ESTADO_OKASEJ, ESTADO_ALERTAASEJ, ESTADO_PELIGROASEJ);
                setText(vacioASEJ ? null : textoASEJ);
                CuotaDtoASEJ cuotaASEJ = vacioASEJ || getTableRow() == null ? null : getTableRow().getItem();
                if (cuotaASEJ != null) {
                    switch (cuotaASEJ.estadoASEJ()) {
                        case PAGADA -> getStyleClass().add(ESTADO_OKASEJ);
                        case VENCIDA -> getStyleClass().add(ESTADO_PELIGROASEJ);
                        case PARCIAL -> getStyleClass().add(ESTADO_ALERTAASEJ);
                        default -> {
                        }
                    }
                }
            }
        });
        FormulariosASEJ.columnasASEJ(tblCuotasASEJ,
                FormulariosASEJ.columnaASEJ("Cuota", CuotaDtoASEJ::etiquetaASEJ, 80),
                FormulariosASEJ.columnaASEJ("Vencimiento", cASEJ -> FormulariosASEJ.fechaASEJ(cASEJ.vencimientoASEJ()), 100),
                FormulariosASEJ.columnaASEJ("Monto", cASEJ -> FormulariosASEJ.solesASEJ(cASEJ.montoASEJ()), 100),
                FormulariosASEJ.columnaASEJ("Pagado", cASEJ -> FormulariosASEJ.solesASEJ(cASEJ.pagadoASEJ()), 100),
                FormulariosASEJ.columnaASEJ("Saldo", cASEJ -> FormulariosASEJ.solesASEJ(cASEJ.saldoASEJ()), 100),
                estadoASEJ);
        tblCuotasASEJ.setPlaceholder(new Label("Sin cronograma de cuotas."));
    }

    private static String etiquetaASEJ(EstadoCuotaASEJ eASEJ) {
        return switch (eASEJ) {
            case PENDIENTE -> "Pendiente";
            case PARCIAL -> "Parcial";
            case VENCIDA -> "Vencida";
            case PAGADA -> "Pagada";
        };
    }

    private void construirTransicionesASEJ(PlanDtoASEJ pASEJ, boolean editaASEJ) {
        boxTransicionesASEJ.getChildren().clear();
        if (pASEJ == null || !editaASEJ) {
            return;
        }
        for (EstadoPlanASEJ destinoASEJ : ORDEN_TRANSICIONESASEJ) {
            if (!pASEJ.transicionesASEJ().contains(destinoASEJ)) {
                continue;
            }
            Button bASEJ = switch (destinoASEJ) {
                case PROPUESTO -> FormulariosASEJ.botonASEJ("Proponer", Feather.SEND);
                case ACEPTADO -> FormulariosASEJ.botonASEJ("Aceptar…", Feather.CHECK_CIRCLE, Styles.SUCCESS, Styles.BUTTON_OUTLINED);
                case EN_CURSO -> FormulariosASEJ.botonASEJ("Iniciar", Feather.PLAY);
                case FINALIZADO -> FormulariosASEJ.botonASEJ("Finalizar", Feather.FLAG, Styles.SUCCESS, Styles.BUTTON_OUTLINED);
                case BORRADOR -> FormulariosASEJ.botonASEJ("Volver a borrador", Feather.EDIT_2);
                case ANULADO -> FormulariosASEJ.botonASEJ("Anular", Feather.X_CIRCLE, Styles.DANGER, Styles.BUTTON_OUTLINED);
            };
            bASEJ.setOnAction(eASEJ -> transicionASEJ(bASEJ, destinoASEJ));
            boxTransicionesASEJ.getChildren().add(bASEJ);
        }
    }

    private void transicionASEJ(Button origenASEJ, EstadoPlanASEJ destinoASEJ) {
        PlanDtoASEJ pASEJ = actualASEJ;
        if (pASEJ == null) {
            return;
        }
        if (destinoASEJ == EstadoPlanASEJ.ACEPTADO) {
            formularioAceptarASEJ(pASEJ);
            return;
        }
        String preguntaASEJ = switch (destinoASEJ) {
            case PROPUESTO -> "El plan pasará a propuesto: ya no se podrán editar procedimientos ni precios "
                    + "(puede volver a borrador mientras no tenga pagos). ¿Continuar?";
            case BORRADOR -> "El plan volverá a borrador para editarlo; se borrará el cronograma de cuotas. ¿Continuar?";
            case ANULADO -> "El plan quedará anulado y no se podrá reabrir. ¿Anular el plan?";
            case FINALIZADO -> "Todos los procedimientos están realizados. ¿Finalizar el plan?";
            default -> null;
        };
        if (preguntaASEJ != null && !notificadorASEJ.confirmarASEJ(origenASEJ.getText().replace("…", ""), preguntaASEJ)) {
            return;
        }
        tareaUiASEJ.ejecutarASEJ(origenASEJ, () -> planServiceASEJ.cambiarEstadoASEJ(pASEJ.idASEJ(), destinoASEJ), dASEJ -> {
            notificadorASEJ.exitoASEJ("Plan " + dASEJ.estadoASEJ().etiquetaASEJ().toLowerCase() + ".");
            aplicarASEJ(dASEJ);
        });
    }

    private void formularioAceptarASEJ(PlanDtoASEJ pASEJ) {
        PacienteDetalleASEJ pacienteASEJ = pacienteActivoASEJ.getASEJ();
        TextField firmanteASEJ = new TextField(pacienteASEJ == null ? "" : pacienteASEJ.esMenorDeEdadASEJ()
                && pacienteASEJ.apoderadoNombreASEJ() != null ? pacienteASEJ.apoderadoNombreASEJ() : pacienteASEJ.nombreCompletoASEJ());
        Label textoASEJ = new Label("Imprima el consentimiento informado de tratamiento, hágalo firmar por el paciente "
                + "(o su apoderado, si es menor de edad) y registre la aceptación. "
                + (pASEJ.cuotasASEJ().isEmpty() ? "Como el plan no tiene cronograma, se creará una cuota única de "
                + FormulariosASEJ.solesASEJ(pASEJ.totalASEJ()) + " con vencimiento hoy." : "El cronograma de " + pASEJ.cuotasASEJ().size()
                + " cuota(s) se mantiene."));
        textoASEJ.setWrapText(true);
        Button imprimirASEJ = FormulariosASEJ.botonASEJ("Imprimir consentimiento", Feather.PRINTER);
        imprimirASEJ.setOnAction(eASEJ -> exportadorASEJ.guardarPdfASEJ(imprimirASEJ, "consentimiento-tratamiento-plan-" + pASEJ.idASEJ() + ".pdf",
                () -> planServiceASEJ.pdfConsentimientoASEJ(pASEJ.idASEJ())));
        GridPane gASEJ = FormulariosASEJ.grillaASEJ();
        FormulariosASEJ.filaASEJ(gASEJ, "Plan", new Label(pASEJ.etiquetaASEJ()));
        FormulariosASEJ.filaASEJ(gASEJ, "Total", new Label(FormulariosASEJ.solesASEJ(pASEJ.totalASEJ())));
        FormulariosASEJ.filaASEJ(gASEJ, "Firmado por *", firmanteASEJ);

        Button cancelarASEJ = FormulariosASEJ.botonASEJ("Cancelar", null);
        cancelarASEJ.setOnAction(eASEJ -> notificadorASEJ.cerrarPanelASEJ());
        Button aceptarASEJ = FormulariosASEJ.botonASEJ("Registrar aceptación", Feather.CHECK, Styles.ACCENT);
        aceptarASEJ.setOnAction(eASEJ -> tareaUiASEJ.ejecutarASEJ(aceptarASEJ, () -> planServiceASEJ.aceptarASEJ(pASEJ.idASEJ(), firmanteASEJ.getText()), dASEJ -> {
            notificadorASEJ.cerrarPanelASEJ();
            notificadorASEJ.exitoASEJ("Plan aceptado. Los procedimientos se marcan como realizados en la pestaña Atenciones.");
            aplicarASEJ(dASEJ);
        }));
        notificadorASEJ.abrirPanelASEJ(FormulariosASEJ.panelLateralASEJ("Aceptar plan de tratamiento", new VBox(14, textoASEJ, imprimirASEJ, gASEJ),
                cancelarASEJ, aceptarASEJ));
    }

    @FXML
    private void onNuevoASEJ() {
        PacienteDetalleASEJ pASEJ = pacienteActivoASEJ.getASEJ();
        if (pASEJ == null || !sesionUsuarioASEJ.tieneASEJ(PermisoASEJ.PLAN_EDITAR)) {
            return;
        }
        tareaUiASEJ.ejecutarASEJ(() -> odontologoServiceASEJ.listarASEJ(false), odontologosASEJ -> formularioNuevoASEJ(pASEJ, odontologosASEJ));
    }

    private void formularioNuevoASEJ(PacienteDetalleASEJ pASEJ, List<OdontologoDtoASEJ> odontologosASEJ) {
        UsuarioSesionASEJ usuarioASEJ = sesionUsuarioASEJ.exigirUsuarioASEJ();
        boolean esOdontologoASEJ = RolASEJ.valueOf(usuarioASEJ.rolASEJ()) == RolASEJ.ODONTOLOGO;
        ComboBox<OdontologoDtoASEJ> odontologoASEJ = new ComboBox<>();
        odontologoASEJ.getItems().setAll(odontologosASEJ);
        odontologoASEJ.setValue(odontologosASEJ.stream()
                .filter(oASEJ -> esOdontologoASEJ && usuarioASEJ.odontologoIdASEJ() != null && oASEJ.idASEJ() == usuarioASEJ.odontologoIdASEJ())
                .findFirst().orElse(odontologosASEJ.size() == 1 ? odontologosASEJ.get(0) : null));
        odontologoASEJ.setDisable(esOdontologoASEJ);
        CheckBox sugerirASEJ = new CheckBox("Agregar los procedimientos sugeridos por el último odontograma firmado");
        sugerirASEJ.setWrapText(true);
        sugerirASEJ.setSelected(true);

        GridPane gASEJ = FormulariosASEJ.grillaASEJ();
        FormulariosASEJ.filaASEJ(gASEJ, "Paciente", new Label(pASEJ.nombreCompletoASEJ() + " · " + pASEJ.numeroHistoriaASEJ()));
        FormulariosASEJ.filaASEJ(gASEJ, "Odontólogo *", odontologoASEJ);
        Label ayudaASEJ = new Label("El plan se crea en borrador. Las sugerencias usan las reglas del catálogo "
                + "(Catálogos → Reglas de sugerencia) y los precios del tarifario.");
        ayudaASEJ.setWrapText(true);
        ayudaASEJ.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);

        Button cancelarASEJ = FormulariosASEJ.botonASEJ("Cancelar", null);
        cancelarASEJ.setOnAction(eASEJ -> notificadorASEJ.cerrarPanelASEJ());
        Button crearASEJ = FormulariosASEJ.botonASEJ("Crear plan", Feather.CHECK, Styles.ACCENT);
        crearASEJ.setOnAction(eASEJ -> {
            Long odontologoIdASEJ = odontologoASEJ.getValue() == null ? null : odontologoASEJ.getValue().idASEJ();
            tareaUiASEJ.ejecutarASEJ(crearASEJ, () -> planServiceASEJ.crearASEJ(pASEJ.idASEJ(), odontologoIdASEJ, sugerirASEJ.isSelected()), idASEJ -> {
                notificadorASEJ.cerrarPanelASEJ();
                notificadorASEJ.exitoASEJ("Plan N° " + idASEJ + " creado en borrador.");
                actualASEJ = null;
                recargarASEJ(idASEJ);
                avisarCambioDePlanASEJ(pASEJ.idASEJ());
            });
        });
        notificadorASEJ.abrirPanelASEJ(FormulariosASEJ.panelLateralASEJ("Nuevo plan de tratamiento", new VBox(12, gASEJ, sugerirASEJ, ayudaASEJ),
                cancelarASEJ, crearASEJ));
    }

    @FXML
    private void onSugerirASEJ() {
        PlanDtoASEJ pASEJ = actualASEJ;
        if (pASEJ == null) {
            return;
        }
        int antesASEJ = pASEJ.itemsASEJ().size();
        tareaUiASEJ.ejecutarASEJ(btnSugerirASEJ, () -> planServiceASEJ.sugerirASEJ(pASEJ.idASEJ()), dASEJ -> {
            notificadorASEJ.exitoASEJ((dASEJ.itemsASEJ().size() - antesASEJ) + " procedimiento(s) sugerido(s) agregado(s).");
            aplicarASEJ(dASEJ);
        });
    }

    @FXML
    private void onAgregarASEJ() {
        formularioItemASEJ(null);
    }

    @FXML
    private void onQuitarASEJ() {
        PlanDtoASEJ pASEJ = actualASEJ;
        TreeItem<FilaASEJ> selASEJ = tablaASEJ.getSelectionModel().getSelectedItem();
        if (pASEJ == null || selASEJ == null || selASEJ.getValue().esGrupoASEJ()) {
            return;
        }
        PlanItemDtoASEJ iASEJ = selASEJ.getValue().itemASEJ();
        if (!notificadorASEJ.confirmarASEJ("Quitar procedimiento", "¿Quitar «" + iASEJ.descripcionASEJ() + "» del plan?")) {
            return;
        }
        tareaUiASEJ.ejecutarASEJ(btnQuitarASEJ, () -> planServiceASEJ.eliminarItemASEJ(iASEJ.idASEJ()), this::aplicarASEJ);
    }

    @FXML
    private void onGuardarCabeceraASEJ() {
        PlanDtoASEJ pASEJ = actualASEJ;
        if (pASEJ == null) {
            return;
        }
        BigDecimal descuentoASEJ = FormulariosASEJ.montoASEJ(txtDescuentoASEJ.getText(), "descuento global");
        String obsASEJ = txtObservacionesASEJ.getText();
        tareaUiASEJ.ejecutarASEJ(btnGuardarCabeceraASEJ, () -> planServiceASEJ.actualizarCabeceraASEJ(pASEJ.idASEJ(), descuentoASEJ, obsASEJ), dASEJ -> {
            notificadorASEJ.exitoASEJ("Plan guardado.");
            aplicarASEJ(dASEJ);
        });
    }

    @FXML
    private void onGenerarASEJ() {
        PlanDtoASEJ pASEJ = actualASEJ;
        if (pASEJ == null) {
            return;
        }
        if (!pASEJ.cuotasASEJ().isEmpty() && !notificadorASEJ.confirmarASEJ("Generar cronograma",
                "Se reemplazarán las " + pASEJ.cuotasASEJ().size() + " cuota(s) actuales. ¿Continuar?")) {
            return;
        }
        spnCuotasASEJ.increment(0);
        CronogramaFormASEJ formASEJ = new CronogramaFormASEJ(pASEJ.idASEJ(), FormulariosASEJ.montoASEJ(txtInicialASEJ.getText(), "monto inicial"),
                spnCuotasASEJ.getValue(), FormulariosASEJ.fechaASEJ(dpPrimerVencimientoASEJ), chFrecuenciaASEJ.getValue());
        tareaUiASEJ.ejecutarASEJ(btnGenerarASEJ, () -> planServiceASEJ.generarCronogramaASEJ(formASEJ), dASEJ -> {
            notificadorASEJ.exitoASEJ("Cronograma generado: " + dASEJ.cuotasASEJ().size() + " cuota(s).");
            aplicarASEJ(dASEJ);
        });
    }

    @FXML
    private void onPresupuestoASEJ() {
        PlanDtoASEJ pASEJ = actualASEJ;
        if (pASEJ != null) {
            exportadorASEJ.guardarPdfASEJ(btnPresupuestoASEJ, "presupuesto-plan-" + pASEJ.idASEJ() + ".pdf", () -> planServiceASEJ.pdfPresupuestoASEJ(pASEJ.idASEJ()));
        }
    }

    @FXML
    private void onConsentimientoASEJ() {
        PlanDtoASEJ pASEJ = actualASEJ;
        if (pASEJ != null) {
            exportadorASEJ.guardarPdfASEJ(btnConsentimientoASEJ, "consentimiento-tratamiento-plan-" + pASEJ.idASEJ() + ".pdf",
                    () -> planServiceASEJ.pdfConsentimientoASEJ(pASEJ.idASEJ()));
        }
    }

    private void formularioItemASEJ(PlanItemDtoASEJ itemASEJ) {
        PlanDtoASEJ pASEJ = actualASEJ;
        if (pASEJ == null) {
            return;
        }
        tareaUiASEJ.ejecutarASEJ(() -> procedimientoServiceASEJ.listarASEJ(itemASEJ != null), procsASEJ -> formularioItemASEJ(pASEJ, itemASEJ, procsASEJ));
    }

    private void formularioItemASEJ(PlanDtoASEJ pASEJ, PlanItemDtoASEJ itemASEJ, List<ProcedimientoDtoASEJ> procedimientosASEJ) {
        ComboBox<ProcedimientoDtoASEJ> procASEJ = new ComboBox<>();
        procASEJ.getItems().setAll(procedimientosASEJ);
        procASEJ.setVisibleRowCount(14);
        TextField piezaASEJ = new TextField(itemASEJ == null || itemASEJ.piezaASEJ() == null ? "" : String.valueOf(itemASEJ.piezaASEJ()));
        piezaASEJ.setPromptText("FDI, p. ej. 36 (vacío = sin pieza)");
        Map<SuperficieASEJ, CheckBox> superficiesASEJ = new LinkedHashMap<>();
        HBox boxSupASEJ = new HBox(10);
        for (SuperficieASEJ sASEJ : SuperficieASEJ.values()) {
            CheckBox cASEJ = new CheckBox(switch (sASEJ) {
                case O -> "O/I";
                case L -> "L/P";
                default -> sASEJ.name();
            });
            cASEJ.setSelected(itemASEJ != null && itemASEJ.superficiesASEJ().contains(sASEJ));
            superficiesASEJ.put(sASEJ, cASEJ);
            boxSupASEJ.getChildren().add(cASEJ);
        }
        ComboBox<OpcionASEJ<Integer>> faseASEJ = new ComboBox<>();
        for (int iASEJ = 1; iASEJ <= 4; iASEJ++) {
            faseASEJ.getItems().add(new OpcionASEJ<>(PlanItemDtoASEJ.nombreFaseASEJ(iASEJ), iASEJ));
        }
        int faseInicialASEJ = itemASEJ == null ? 2 : itemASEJ.faseASEJ();
        faseASEJ.setValue(faseASEJ.getItems().get(faseInicialASEJ - 1));
        Spinner<Integer> cantidadASEJ = new Spinner<>(1, 99, itemASEJ == null ? 1 : itemASEJ.cantidadASEJ());
        cantidadASEJ.setEditable(true);
        TextField precioASEJ = new TextField(itemASEJ == null ? "" : itemASEJ.precioUnitASEJ().setScale(2).toPlainString());
        TextField descuentoASEJ = new TextField(itemASEJ == null ? "0.00" : itemASEJ.descuentoASEJ().setScale(2).toPlainString());
        Label formaASEJ = new Label();
        formaASEJ.setWrapText(true);
        formaASEJ.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);

        Runnable actualizarFormaASEJ = () -> {
            ProcedimientoDtoASEJ dASEJ = procASEJ.getValue();
            boolean porSuperficieASEJ = dASEJ != null && dASEJ.formaCobroASEJ() == FormaCobroASEJ.SUPERFICIE;
            cantidadASEJ.setDisable(porSuperficieASEJ);
            formaASEJ.setText(dASEJ == null ? "" : switch (dASEJ.formaCobroASEJ()) {
                case SUPERFICIE -> "Se cobra por superficie: la cantidad es el número de superficies marcadas.";
                case PIEZA -> "Se cobra por pieza.";
                case ARCADA -> "Se cobra por arcada.";
                case BOCA -> "Se cobra una vez para toda la boca.";
                case SESION -> "Se cobra por sesión: indique el número de sesiones en la cantidad.";
            } + " Precio del tarifario: " + FormulariosASEJ.solesASEJ(dASEJ.precioASEJ()) + ".");
        };
        procASEJ.valueProperty().addListener((obsASEJ, aASEJ, bASEJ) -> {
            if (bASEJ != null && (itemASEJ == null || bASEJ.idASEJ() != itemASEJ.procedimientoIdASEJ())) {
                precioASEJ.setText(bASEJ.precioASEJ().setScale(2).toPlainString());
            }
            actualizarFormaASEJ.run();
        });
        procASEJ.setValue(itemASEJ == null ? null : procedimientosASEJ.stream().filter(dASEJ -> dASEJ.idASEJ() == itemASEJ.procedimientoIdASEJ())
                .findFirst().orElse(null));
        actualizarFormaASEJ.run();

        GridPane gASEJ = FormulariosASEJ.grillaASEJ();
        FormulariosASEJ.filaASEJ(gASEJ, "Procedimiento *", procASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "", formaASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "Pieza", piezaASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "Superficies", boxSupASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "Fase *", faseASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "Cantidad", cantidadASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "Precio unitario S/", precioASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "Descuento S/", descuentoASEJ);

        Button cancelarASEJ = FormulariosASEJ.botonASEJ("Cancelar", null);
        cancelarASEJ.setOnAction(eASEJ -> notificadorASEJ.cerrarPanelASEJ());
        Button guardarASEJ = FormulariosASEJ.botonASEJ(itemASEJ == null ? "Agregar" : "Guardar", Feather.CHECK, Styles.ACCENT);
        guardarASEJ.setOnAction(eASEJ -> {
            try {
                cantidadASEJ.increment(0);
                Integer fdiASEJ = piezaASEJ.getText().isBlank() ? null : FormulariosASEJ.enteroASEJ(piezaASEJ.getText(), "pieza");
                if (fdiASEJ != null && !PiezaDentalASEJ.esValidaASEJ(fdiASEJ)) {
                    throw new com.odontogest.excepcion.NegocioExceptionASEJ("La pieza " + fdiASEJ + " no es un número FDI válido.");
                }
                Set<SuperficieASEJ> marcadasASEJ = EnumSet.noneOf(SuperficieASEJ.class);
                superficiesASEJ.forEach((sASEJ, cASEJ) -> {
                    if (cASEJ.isSelected()) {
                        marcadasASEJ.add(sASEJ);
                    }
                });
                PlanItemFormASEJ formASEJ = new PlanItemFormASEJ(itemASEJ == null ? null : itemASEJ.idASEJ(), pASEJ.idASEJ(),
                        procASEJ.getValue() == null ? null : procASEJ.getValue().idASEJ(), fdiASEJ, marcadasASEJ,
                        FormulariosASEJ.seleccionASEJ(faseASEJ), cantidadASEJ.getValue(), FormulariosASEJ.montoASEJ(precioASEJ.getText(), "precio"),
                        FormulariosASEJ.montoASEJ(descuentoASEJ.getText(), "descuento"));
                tareaUiASEJ.ejecutarASEJ(guardarASEJ, () -> planServiceASEJ.guardarItemASEJ(formASEJ), dASEJ -> {
                    notificadorASEJ.cerrarPanelASEJ();
                    aplicarASEJ(dASEJ);
                });
            } catch (RuntimeException exASEJ) {
                tareaUiASEJ.manejarErrorASEJ(exASEJ);
            }
        });
        notificadorASEJ.abrirPanelASEJ(FormulariosASEJ.panelLateralASEJ(itemASEJ == null ? "Agregar procedimiento" : "Editar procedimiento",
                gASEJ, cancelarASEJ, guardarASEJ));
        Platform.runLater(procASEJ::requestFocus);
    }

    private void avisarCambioDePlanASEJ(long pacienteIdASEJ) {
        avisandoASEJ = true;
        try {
            eventosASEJ.publishEvent(new PlanCambiadoASEJ(pacienteIdASEJ));
        } finally {
            avisandoASEJ = false;
        }
    }
}
