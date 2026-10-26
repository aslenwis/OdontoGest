package com.odontogest.ui.controlador;

import atlantafx.base.controls.CustomTextField;
import atlantafx.base.theme.Styles;
import com.odontogest.dominio.finanzas.EstadoCuotaASEJ;
import com.odontogest.evento.NavegarAASEJ;
import com.odontogest.evento.PagoRegistradoASEJ;
import com.odontogest.modelo.MedioPagoASEJ;
import com.odontogest.servicio.PacienteServiceASEJ;
import com.odontogest.servicio.PagoServiceASEJ;
import com.odontogest.servicio.dto.AplicacionDtoASEJ;
import com.odontogest.servicio.dto.CuotaPacienteDtoASEJ;
import com.odontogest.servicio.dto.PacienteResumenASEJ;
import com.odontogest.servicio.dto.PagoDtoASEJ;
import com.odontogest.servicio.dto.PagoFormASEJ;
import com.odontogest.servicio.dto.ResumenCajaDtoASEJ;
import com.odontogest.ui.componente.AutocompletarASEJ;
import com.odontogest.ui.soporte.AccionesPagoASEJ;
import com.odontogest.ui.soporte.FormulariosASEJ;
import com.odontogest.ui.soporte.HiloUiASEJ;
import com.odontogest.ui.soporte.NotificadorASEJ;
import com.odontogest.ui.soporte.RefrescableASEJ;
import com.odontogest.ui.soporte.TareaUiASEJ;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.odontogest.ui.soporte.FormulariosASEJ.ESTADO_ALERTAASEJ;
import static com.odontogest.ui.soporte.FormulariosASEJ.ESTADO_OKASEJ;
import static com.odontogest.ui.soporte.FormulariosASEJ.ESTADO_PELIGROASEJ;

@Component
@RequiredArgsConstructor
public class CajaControllerASEJ implements RefrescableASEJ {

    private final PagoServiceASEJ pagoServiceASEJ;
    private final PacienteServiceASEJ pacienteServiceASEJ;
    private final AccionesPagoASEJ accionesPagoASEJ;
    private final TareaUiASEJ tareaUiASEJ;
    private final NotificadorASEJ notificadorASEJ;
    private final ApplicationEventPublisher eventosASEJ;

    @FXML private CustomTextField txtPacienteASEJ;
    @FXML private Label lblPacienteASEJ;
    @FXML private Label lblSaldoASEJ;
    @FXML private TableView<CuotaPacienteDtoASEJ> tblPendientesASEJ;
    @FXML private TextField txtMontoASEJ;
    @FXML private Button btnTodoASEJ;
    @FXML private Button btnVencidoASEJ;
    @FXML private ChoiceBox<MedioPagoASEJ> chMedioASEJ;
    @FXML private TextField txtReferenciaASEJ;
    @FXML private Label lblPreviaASEJ;
    @FXML private TableView<AplicacionDtoASEJ> tblPreviaASEJ;
    @FXML private Button btnCobrarASEJ;
    @FXML private DatePicker dpFechaASEJ;
    @FXML private TableView<PagoDtoASEJ> tblDiaASEJ;
    @FXML private Button btnReciboASEJ;
    @FXML private Button btnAnularASEJ;
    @FXML private Button btnFichaASEJ;
    @FXML private FlowPane boxTotalesASEJ;
    @FXML private Label lblTotalDiaASEJ;

    private AutocompletarASEJ<PacienteResumenASEJ> autocompletarASEJ;
    private PauseTransition esperaPreviaASEJ;
    private PacienteResumenASEJ pacienteASEJ;
    private List<CuotaPacienteDtoASEJ> pendientesASEJ = List.of();

    @FXML
    public void initialize() {
        autocompletarASEJ = new AutocompletarASEJ<>(txtPacienteASEJ,
                (filtroASEJ, entregarASEJ) -> tareaUiASEJ.ejecutarASEJ(() -> pacienteServiceASEJ.buscarASEJ(filtroASEJ, false, 0).contenidoASEJ(), entregarASEJ),
                pASEJ -> pASEJ.nombreCompletoASEJ() + "  ·  " + pASEJ.numeroHistoriaASEJ() + "  ·  " + pASEJ.documentoASEJ(),
                this::elegirPacienteASEJ);
        configurarTablasASEJ();
        chMedioASEJ.getItems().setAll(MedioPagoASEJ.values());
        chMedioASEJ.setConverter(FormulariosASEJ.conversorASEJ(MedioPagoASEJ::etiquetaASEJ));
        chMedioASEJ.setValue(MedioPagoASEJ.EFECTIVO);
        chMedioASEJ.valueProperty().addListener((obsASEJ, aASEJ, bASEJ) -> txtReferenciaASEJ.setPromptText(bASEJ != null && bASEJ.pideReferenciaASEJ()
                ? "N° de operación (recomendado)" : "Opcional"));
        FormulariosASEJ.configurarFechaASEJ(dpFechaASEJ);
        dpFechaASEJ.setValue(LocalDate.now());
        dpFechaASEJ.valueProperty().addListener((obsASEJ, aASEJ, bASEJ) -> cargarDiaASEJ());
        esperaPreviaASEJ = new PauseTransition(Duration.millis(250));
        esperaPreviaASEJ.setOnFinished(eASEJ -> actualizarPreviaASEJ());
        txtMontoASEJ.textProperty().addListener((obsASEJ, aASEJ, bASEJ) -> esperaPreviaASEJ.playFromStart());
        tblDiaASEJ.getSelectionModel().selectedItemProperty().addListener((obsASEJ, aASEJ, bASEJ) -> actualizarBotonesDiaASEJ());
        mostrarPacienteASEJ(null, List.of());
    }

    @Override
    public void refrescarASEJ() {
        cargarDiaASEJ();
        if (pacienteASEJ != null) {
            cargarPendientesASEJ();
        }
    }

    public void seleccionarPacienteASEJ(long pacienteIdASEJ) {
        tareaUiASEJ.ejecutarASEJ(() -> {
            var dASEJ = pacienteServiceASEJ.obtenerASEJ(pacienteIdASEJ);
            return new PacienteResumenASEJ(dASEJ.idASEJ(), dASEJ.numeroHistoriaASEJ(), dASEJ.documentoASEJ(), dASEJ.nombreCompletoASEJ(), dASEJ.edadASEJ(),
                    dASEJ.telefonoASEJ(), dASEJ.activoASEJ(), dASEJ.demoASEJ());
        }, pASEJ -> {
            autocompletarASEJ.fijarASEJ(pASEJ);
            elegirPacienteASEJ(pASEJ);
        });
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void alRegistrarPagoASEJ(PagoRegistradoASEJ eASEJ) {
        HiloUiASEJ.ejecutarASEJ(() -> {
            if (tblDiaASEJ != null && tblDiaASEJ.getScene() != null) {
                refrescarASEJ();
            }
        });
    }

    private void elegirPacienteASEJ(PacienteResumenASEJ pASEJ) {
        pacienteASEJ = pASEJ;
        txtMontoASEJ.clear();
        txtReferenciaASEJ.clear();
        cargarPendientesASEJ();
    }

    private void cargarPendientesASEJ() {
        PacienteResumenASEJ pASEJ = pacienteASEJ;
        tareaUiASEJ.ejecutarASEJ(() -> pagoServiceASEJ.cuotasPendientesASEJ(pASEJ.idASEJ()), cuotasASEJ -> {
            if (pASEJ == pacienteASEJ) {
                mostrarPacienteASEJ(pASEJ, cuotasASEJ);
                actualizarPreviaASEJ();
            }
        });
    }

    private void mostrarPacienteASEJ(PacienteResumenASEJ pASEJ, List<CuotaPacienteDtoASEJ> cuotasASEJ) {
        pendientesASEJ = cuotasASEJ;
        tblPendientesASEJ.getItems().setAll(cuotasASEJ);
        lblPacienteASEJ.setText(pASEJ == null ? "Elija un paciente" : pASEJ.nombreCompletoASEJ() + " · " + pASEJ.numeroHistoriaASEJ());
        BigDecimal saldoASEJ = cuotasASEJ.stream().map(CuotaPacienteDtoASEJ::saldoASEJ).reduce(BigDecimal.ZERO, BigDecimal::add);
        lblSaldoASEJ.setText(pASEJ == null ? "" : "Saldo: " + FormulariosASEJ.solesASEJ(saldoASEJ));
        tblPendientesASEJ.setPlaceholder(new Label(pASEJ == null ? "Busque un paciente para ver sus cuotas."
                : "El paciente no tiene cuotas pendientes (los planes se cobran desde que son aceptados)."));
        boolean hayASEJ = pASEJ != null && !cuotasASEJ.isEmpty();
        for (var nASEJ : List.of(txtMontoASEJ, btnTodoASEJ, chMedioASEJ, txtReferenciaASEJ, btnCobrarASEJ)) {
            nASEJ.setDisable(!hayASEJ);
        }
        btnVencidoASEJ.setDisable(!hayASEJ || cuotasASEJ.stream().noneMatch(cASEJ -> cASEJ.estadoASEJ() == EstadoCuotaASEJ.VENCIDA));
    }

    private void actualizarPreviaASEJ() {
        PacienteResumenASEJ pASEJ = pacienteASEJ;
        tblPreviaASEJ.getItems().clear();
        lblPreviaASEJ.getStyleClass().remove(Styles.DANGER);
        if (!lblPreviaASEJ.getStyleClass().contains(Styles.TEXT_MUTED)) {
            lblPreviaASEJ.getStyleClass().add(Styles.TEXT_MUTED);
        }
        if (pASEJ == null || pendientesASEJ.isEmpty()) {
            lblPreviaASEJ.setText("");
            return;
        }
        BigDecimal montoASEJ;
        try {
            montoASEJ = FormulariosASEJ.montoASEJ(txtMontoASEJ.getText(), "monto");
        } catch (RuntimeException eASEJ) {
            errorASEJ(eASEJ.getMessage());
            return;
        }
        if (montoASEJ == null) {
            lblPreviaASEJ.setText("Escriba el monto para ver a qué cuotas se aplica.");
            return;
        }
        tareaUiASEJ.ejecutarASEJ(() -> pagoServiceASEJ.vistaPreviaASEJ(pASEJ.idASEJ(), montoASEJ), aplicacionesASEJ -> {
            if (pASEJ != pacienteASEJ) {
                return;
            }
            tblPreviaASEJ.getItems().setAll(aplicacionesASEJ);
            BigDecimal saldoASEJ = pendientesASEJ.stream().map(CuotaPacienteDtoASEJ::saldoASEJ).reduce(BigDecimal.ZERO, BigDecimal::add);
            lblPreviaASEJ.setText("Se aplica a " + aplicacionesASEJ.size() + " cuota(s). Saldo después del pago: "
                    + FormulariosASEJ.solesASEJ(saldoASEJ.subtract(montoASEJ)) + ".");
        }, exASEJ -> errorASEJ(exASEJ.getMessage()));
    }

    private void errorASEJ(String mensajeASEJ) {
        lblPreviaASEJ.setText(mensajeASEJ);
        lblPreviaASEJ.getStyleClass().remove(Styles.TEXT_MUTED);
        if (!lblPreviaASEJ.getStyleClass().contains(Styles.DANGER)) {
            lblPreviaASEJ.getStyleClass().add(Styles.DANGER);
        }
    }

    @FXML
    private void onTodoASEJ() {
        txtMontoASEJ.setText(pendientesASEJ.stream().map(CuotaPacienteDtoASEJ::saldoASEJ).reduce(BigDecimal.ZERO, BigDecimal::add)
                .toPlainString());
    }

    @FXML
    private void onVencidoASEJ() {
        txtMontoASEJ.setText(pendientesASEJ.stream().filter(cASEJ -> cASEJ.estadoASEJ() == EstadoCuotaASEJ.VENCIDA).map(CuotaPacienteDtoASEJ::saldoASEJ)
                .reduce(BigDecimal.ZERO, BigDecimal::add).toPlainString());
    }

    @FXML
    private void onCobrarASEJ() {
        PacienteResumenASEJ pASEJ = pacienteASEJ;
        if (pASEJ == null) {
            return;
        }
        PagoFormASEJ formASEJ = new PagoFormASEJ(pASEJ.idASEJ(), FormulariosASEJ.montoASEJ(txtMontoASEJ.getText(), "monto"), chMedioASEJ.getValue(),
                txtReferenciaASEJ.getText());
        if (formASEJ.montoASEJ() != null && !notificadorASEJ.confirmarASEJ("Cobrar", "¿Registrar el cobro de " + FormulariosASEJ.solesASEJ(formASEJ.montoASEJ())
                + " (" + (formASEJ.medioASEJ() == null ? "—" : formASEJ.medioASEJ().etiquetaASEJ()) + ") a " + pASEJ.nombreCompletoASEJ() + "?")) {
            return;
        }
        tareaUiASEJ.ejecutarASEJ(btnCobrarASEJ, () -> pagoServiceASEJ.registrarASEJ(formASEJ), pagoASEJ -> {
            txtMontoASEJ.clear();
            txtReferenciaASEJ.clear();
            refrescarASEJ();
            exitoASEJ(pagoASEJ);
        });
    }

    private void exitoASEJ(PagoDtoASEJ pagoASEJ) {
        Label numeroASEJ = new Label("Recibo " + pagoASEJ.numeroReciboASEJ());
        numeroASEJ.getStyleClass().add(Styles.TITLE_3);
        GridPane gASEJ = FormulariosASEJ.grillaASEJ();
        FormulariosASEJ.filaASEJ(gASEJ, "Paciente", new Label(pagoASEJ.pacienteNombreASEJ()));
        FormulariosASEJ.filaASEJ(gASEJ, "Monto", new Label(FormulariosASEJ.solesASEJ(pagoASEJ.montoASEJ()) + " · " + pagoASEJ.medioASEJ().etiquetaASEJ()));
        Label aplicadoASEJ = new Label(AccionesPagoASEJ.aplicadoAASEJ(pagoASEJ));
        aplicadoASEJ.setWrapText(true);
        FormulariosASEJ.filaASEJ(gASEJ, "Aplicado a", aplicadoASEJ);
        Button cerrarASEJ = FormulariosASEJ.botonASEJ("Cerrar", null);
        cerrarASEJ.setOnAction(eASEJ -> notificadorASEJ.cerrarPanelASEJ());
        Button imprimirASEJ = FormulariosASEJ.botonASEJ("Imprimir recibo", Feather.PRINTER, Styles.ACCENT);
        imprimirASEJ.setOnAction(eASEJ -> {
            notificadorASEJ.cerrarPanelASEJ();
            accionesPagoASEJ.imprimirReciboASEJ(btnReciboASEJ, pagoASEJ);
        });
        notificadorASEJ.abrirPanelASEJ(FormulariosASEJ.panelLateralASEJ("Pago registrado", new VBox(14, numeroASEJ, gASEJ), cerrarASEJ, imprimirASEJ));
    }

    private void cargarDiaASEJ() {
        LocalDate fechaASEJ = dpFechaASEJ.getValue() == null ? LocalDate.now() : dpFechaASEJ.getValue();
        tareaUiASEJ.ejecutarASEJ(() -> pagoServiceASEJ.cajaDelDiaASEJ(fechaASEJ), this::mostrarDiaASEJ);
    }

    private void mostrarDiaASEJ(ResumenCajaDtoASEJ rASEJ) {
        tblDiaASEJ.getItems().setAll(rASEJ.pagosASEJ());
        tblDiaASEJ.setPlaceholder(new Label("Sin cobros el " + FormulariosASEJ.fechaASEJ(rASEJ.fechaASEJ()) + "."));
        boxTotalesASEJ.getChildren().clear();
        for (MedioPagoASEJ mASEJ : MedioPagoASEJ.values()) {
            Label lASEJ = new Label(mASEJ.etiquetaASEJ() + ": " + FormulariosASEJ.solesASEJ(rASEJ.porMedioASEJ().get(mASEJ)));
            if (rASEJ.porMedioASEJ().get(mASEJ).signum() == 0) {
                lASEJ.getStyleClass().add(Styles.TEXT_MUTED);
            }
            boxTotalesASEJ.getChildren().add(lASEJ);
        }
        lblTotalDiaASEJ.setText("Total: " + FormulariosASEJ.solesASEJ(rASEJ.totalASEJ()));
        actualizarBotonesDiaASEJ();
    }

    private void actualizarBotonesDiaASEJ() {
        PagoDtoASEJ pASEJ = tblDiaASEJ.getSelectionModel().getSelectedItem();
        btnReciboASEJ.setDisable(pASEJ == null);
        btnFichaASEJ.setDisable(pASEJ == null);
        btnAnularASEJ.setDisable(!accionesPagoASEJ.puedeAnularASEJ(pASEJ));
    }

    @FXML
    private void onHoyASEJ() {
        dpFechaASEJ.setValue(LocalDate.now());
    }

    @FXML
    private void onReciboASEJ() {
        accionesPagoASEJ.imprimirReciboASEJ(btnReciboASEJ, tblDiaASEJ.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void onAnularASEJ() {
        accionesPagoASEJ.anularASEJ(tblDiaASEJ.getSelectionModel().getSelectedItem(), dASEJ -> refrescarASEJ());
    }

    @FXML
    private void onFichaASEJ() {
        PagoDtoASEJ pASEJ = tblDiaASEJ.getSelectionModel().getSelectedItem();
        if (pASEJ != null) {
            eventosASEJ.publishEvent(new NavegarAASEJ(MainControllerASEJ.VISTA_FICHAASEJ, Map.of("pacienteId", pASEJ.pacienteIdASEJ(), "pestana", "pagos")));
        }
    }

    private void configurarTablasASEJ() {
        TableColumn<CuotaPacienteDtoASEJ, String> estadoASEJ = FormulariosASEJ.columnaASEJ("Estado", cASEJ -> etiquetaASEJ(cASEJ.estadoASEJ()), 90);
        estadoASEJ.setCellFactory(cASEJ -> celdaEstadoASEJ());
        FormulariosASEJ.columnasASEJ(tblPendientesASEJ,
                FormulariosASEJ.columnaASEJ("Cuota", CuotaPacienteDtoASEJ::etiquetaASEJ, 150),
                FormulariosASEJ.columnaASEJ("Vence", cASEJ -> FormulariosASEJ.fechaASEJ(cASEJ.vencimientoASEJ()), 90),
                FormulariosASEJ.columnaASEJ("Monto", cASEJ -> FormulariosASEJ.solesASEJ(cASEJ.montoASEJ()), 90),
                FormulariosASEJ.columnaASEJ("Saldo", cASEJ -> FormulariosASEJ.solesASEJ(cASEJ.saldoASEJ()), 90),
                estadoASEJ);

        FormulariosASEJ.columnasASEJ(tblPreviaASEJ,
                FormulariosASEJ.columnaASEJ("Cuota", AplicacionDtoASEJ::etiquetaASEJ, 150),
                FormulariosASEJ.columnaASEJ("Vence", aASEJ -> FormulariosASEJ.fechaASEJ(aASEJ.vencimientoASEJ()), 90),
                FormulariosASEJ.columnaASEJ("Saldo antes", aASEJ -> FormulariosASEJ.solesASEJ(aASEJ.saldoAntesASEJ()), 95),
                FormulariosASEJ.columnaASEJ("Se aplica", aASEJ -> FormulariosASEJ.solesASEJ(aASEJ.montoASEJ()), 95),
                FormulariosASEJ.columnaASEJ("Saldo después", aASEJ -> FormulariosASEJ.solesASEJ(aASEJ.saldoDespuesASEJ()), 100));
        tblPreviaASEJ.setPlaceholder(new Label("—"));

        TableColumn<PagoDtoASEJ, String> horaASEJ = FormulariosASEJ.columnaASEJ("Hora", pASEJ -> FormulariosASEJ.HORAASEJ.format(pASEJ.fechaASEJ()), 60);
        horaASEJ.setMinWidth(60);
        TableColumn<PagoDtoASEJ, String> colReciboASEJ = FormulariosASEJ.columnaASEJ("Recibo", PagoDtoASEJ::numeroReciboASEJ, 105);
        colReciboASEJ.setMinWidth(100);
        FormulariosASEJ.columnasASEJ(tblDiaASEJ,
                colReciboASEJ,
                horaASEJ,
                FormulariosASEJ.columnaASEJ("Paciente", PagoDtoASEJ::pacienteNombreASEJ, 170),
                FormulariosASEJ.columnaASEJ("Medio", pASEJ -> pASEJ.medioASEJ().etiquetaASEJ(), 90),
                FormulariosASEJ.columnaASEJ("Monto", pASEJ -> FormulariosASEJ.solesASEJ(pASEJ.montoASEJ()), 90),
                AccionesPagoASEJ.columnaEstadoASEJ());
        tblDiaASEJ.setRowFactory(tASEJ -> {
            TableRow<PagoDtoASEJ> filaASEJ = new TableRow<>();
            MenuItem reciboASEJ = new MenuItem("Recibo PDF");
            reciboASEJ.setOnAction(eASEJ -> accionesPagoASEJ.imprimirReciboASEJ(btnReciboASEJ, filaASEJ.getItem()));
            MenuItem anularASEJ = new MenuItem("Anular…");
            anularASEJ.setOnAction(eASEJ -> accionesPagoASEJ.anularASEJ(filaASEJ.getItem(), dASEJ -> refrescarASEJ()));
            ContextMenu menuASEJ = new ContextMenu(reciboASEJ, anularASEJ);
            menuASEJ.setOnShowing(eASEJ -> anularASEJ.setDisable(!accionesPagoASEJ.puedeAnularASEJ(filaASEJ.getItem())));
            filaASEJ.contextMenuProperty().bind(javafx.beans.binding.Bindings.when(filaASEJ.emptyProperty())
                    .then((ContextMenu) null).otherwise(menuASEJ));
            return filaASEJ;
        });
    }

    static TableCell<CuotaPacienteDtoASEJ, String> celdaEstadoASEJ() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String textoASEJ, boolean vacioASEJ) {
                super.updateItem(textoASEJ, vacioASEJ);
                getStyleClass().removeAll(ESTADO_OKASEJ, ESTADO_ALERTAASEJ, ESTADO_PELIGROASEJ);
                setText(vacioASEJ ? null : textoASEJ);
                CuotaPacienteDtoASEJ cASEJ = vacioASEJ || getTableRow() == null ? null : getTableRow().getItem();
                if (cASEJ != null) {
                    switch (cASEJ.estadoASEJ()) {
                        case PAGADA -> getStyleClass().add(ESTADO_OKASEJ);
                        case VENCIDA -> getStyleClass().add(ESTADO_PELIGROASEJ);
                        case PARCIAL -> getStyleClass().add(ESTADO_ALERTAASEJ);
                        default -> {
                        }
                    }
                }
            }
        };
    }

    static String etiquetaASEJ(EstadoCuotaASEJ eASEJ) {
        return switch (eASEJ) {
            case PENDIENTE -> "Pendiente";
            case PARCIAL -> "Parcial";
            case VENCIDA -> "Vencida";
            case PAGADA -> "Pagada";
        };
    }
}
