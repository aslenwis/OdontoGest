package com.odontogest.ui.controlador;

import atlantafx.base.theme.Styles;
import com.odontogest.evento.NavegarAASEJ;
import com.odontogest.evento.PagoRegistradoASEJ;
import com.odontogest.evento.PlanCambiadoASEJ;
import com.odontogest.seguridad.PermisoASEJ;
import com.odontogest.seguridad.SesionUsuarioASEJ;
import com.odontogest.servicio.PagoServiceASEJ;
import com.odontogest.servicio.dto.CuotaPacienteDtoASEJ;
import com.odontogest.servicio.dto.EstadoCuentaDtoASEJ;
import com.odontogest.servicio.dto.PacienteDetalleASEJ;
import com.odontogest.servicio.dto.PagoDtoASEJ;
import com.odontogest.ui.soporte.AccionesPagoASEJ;
import com.odontogest.ui.soporte.FormulariosASEJ;
import com.odontogest.ui.soporte.HiloUiASEJ;
import com.odontogest.ui.soporte.PacienteActivoASEJ;
import com.odontogest.ui.soporte.TareaUiASEJ;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class PagosPacienteControllerASEJ {

    private final PagoServiceASEJ pagoServiceASEJ;
    private final AccionesPagoASEJ accionesPagoASEJ;
    private final PacienteActivoASEJ pacienteActivoASEJ;
    private final TareaUiASEJ tareaUiASEJ;
    private final SesionUsuarioASEJ sesionUsuarioASEJ;
    private final ApplicationEventPublisher eventosASEJ;

    @FXML private Label lblTotalASEJ;
    @FXML private Label lblPagadoASEJ;
    @FXML private Label lblSaldoASEJ;
    @FXML private Label lblVencidoASEJ;
    @FXML private Button btnCobrarASEJ;
    @FXML private TableView<CuotaPacienteDtoASEJ> tblCuotasASEJ;
    @FXML private Button btnReciboASEJ;
    @FXML private Button btnAnularASEJ;
    @FXML private TableView<PagoDtoASEJ> tblPagosASEJ;

    private ChangeListener<PacienteDetalleASEJ> oyenteASEJ;

    @FXML
    public void initialize() {
        TableColumn<CuotaPacienteDtoASEJ, String> estadoASEJ = FormulariosASEJ.columnaASEJ("Estado", cASEJ -> CajaControllerASEJ.etiquetaASEJ(cASEJ.estadoASEJ()), 90);
        estadoASEJ.setCellFactory(cASEJ -> CajaControllerASEJ.celdaEstadoASEJ());
        FormulariosASEJ.columnasASEJ(tblCuotasASEJ,
                FormulariosASEJ.columnaASEJ("Cuota", CuotaPacienteDtoASEJ::etiquetaASEJ, 150),
                FormulariosASEJ.columnaASEJ("Vencimiento", cASEJ -> FormulariosASEJ.fechaASEJ(cASEJ.vencimientoASEJ()), 100),
                FormulariosASEJ.columnaASEJ("Monto", cASEJ -> FormulariosASEJ.solesASEJ(cASEJ.montoASEJ()), 100),
                FormulariosASEJ.columnaASEJ("Pagado", cASEJ -> FormulariosASEJ.solesASEJ(cASEJ.pagadoASEJ()), 100),
                FormulariosASEJ.columnaASEJ("Saldo", cASEJ -> FormulariosASEJ.solesASEJ(cASEJ.saldoASEJ()), 100),
                estadoASEJ);
        tblCuotasASEJ.setPlaceholder(new Label("Sin cuotas: el cronograma aparece cuando el paciente acepta un plan."));
        FormulariosASEJ.columnasASEJ(tblPagosASEJ,
                FormulariosASEJ.columnaASEJ("Recibo", PagoDtoASEJ::numeroReciboASEJ, 105),
                FormulariosASEJ.columnaASEJ("Fecha", pASEJ -> FormulariosASEJ.fechaHoraASEJ(pASEJ.fechaASEJ()), 120),
                FormulariosASEJ.columnaASEJ("Monto", pASEJ -> FormulariosASEJ.solesASEJ(pASEJ.montoASEJ()), 95),
                FormulariosASEJ.columnaASEJ("Medio", pASEJ -> pASEJ.medioASEJ().etiquetaASEJ(), 95),
                FormulariosASEJ.columnaASEJ("Referencia", pASEJ -> pASEJ.referenciaASEJ() == null ? "" : pASEJ.referenciaASEJ(), 110),
                FormulariosASEJ.columnaASEJ("Aplicado a", AccionesPagoASEJ::aplicadoAASEJ, 200),
                FormulariosASEJ.columnaASEJ("Registrado por", PagoDtoASEJ::registradoPorASEJ, 150),
                AccionesPagoASEJ.columnaEstadoASEJ());
        tblPagosASEJ.setPlaceholder(new Label("Sin pagos registrados."));
        tblPagosASEJ.getSelectionModel().selectedItemProperty().addListener((obsASEJ, aASEJ, bASEJ) -> actualizarBotonesASEJ());
        boolean cobraASEJ = sesionUsuarioASEJ.tieneASEJ(PermisoASEJ.PAGO_REGISTRAR);
        btnCobrarASEJ.setVisible(cobraASEJ);
        btnCobrarASEJ.setManaged(cobraASEJ);
        actualizarBotonesASEJ();

        if (oyenteASEJ != null) {
            pacienteActivoASEJ.pacientePropertyASEJ().removeListener(oyenteASEJ);
        }
        oyenteASEJ = (obsASEJ, aASEJ, bASEJ) -> recargarASEJ();
        pacienteActivoASEJ.pacientePropertyASEJ().addListener(oyenteASEJ);
        recargarASEJ();
    }

    public void recargarASEJ() {
        PacienteDetalleASEJ pASEJ = pacienteActivoASEJ.getASEJ();
        if (pASEJ == null || tblPagosASEJ == null) {
            return;
        }
        tareaUiASEJ.ejecutarASEJ(() -> pagoServiceASEJ.estadoCuentaASEJ(pASEJ.idASEJ()), this::mostrarASEJ);
    }

    private void mostrarASEJ(EstadoCuentaDtoASEJ eASEJ) {
        PacienteDetalleASEJ pASEJ = pacienteActivoASEJ.getASEJ();
        if (pASEJ == null || pASEJ.idASEJ() != eASEJ.pacienteIdASEJ()) {
            return;
        }
        lblTotalASEJ.setText(FormulariosASEJ.solesASEJ(eASEJ.totalASEJ()));
        lblPagadoASEJ.setText(FormulariosASEJ.solesASEJ(eASEJ.pagadoASEJ()));
        lblSaldoASEJ.setText(FormulariosASEJ.solesASEJ(eASEJ.saldoASEJ()));
        lblVencidoASEJ.setText(FormulariosASEJ.solesASEJ(eASEJ.vencidoASEJ()));
        lblVencidoASEJ.getStyleClass().remove(Styles.DANGER);
        if (eASEJ.vencidoASEJ().signum() > 0) {
            lblVencidoASEJ.getStyleClass().add(Styles.DANGER);
        }
        tblCuotasASEJ.getItems().setAll(eASEJ.cuotasASEJ());
        tblPagosASEJ.getItems().setAll(eASEJ.pagosASEJ());
        btnCobrarASEJ.setDisable(eASEJ.saldoASEJ().signum() <= 0);
        actualizarBotonesASEJ();
    }

    private void actualizarBotonesASEJ() {
        PagoDtoASEJ pASEJ = tblPagosASEJ.getSelectionModel().getSelectedItem();
        btnReciboASEJ.setDisable(pASEJ == null);
        btnAnularASEJ.setDisable(!accionesPagoASEJ.puedeAnularASEJ(pASEJ));
        btnAnularASEJ.setVisible(sesionUsuarioASEJ.tieneASEJ(PermisoASEJ.PAGO_ANULAR));
        btnAnularASEJ.setManaged(btnAnularASEJ.isVisible());
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void alRegistrarPagoASEJ(PagoRegistradoASEJ eASEJ) {
        HiloUiASEJ.ejecutarASEJ(() -> refrescarSiEsASEJ(eASEJ.pacienteIdASEJ()));
    }

    @EventListener
    public void alCambiarPlanASEJ(PlanCambiadoASEJ eASEJ) {
        HiloUiASEJ.ejecutarASEJ(() -> refrescarSiEsASEJ(eASEJ.pacienteIdASEJ()));
    }

    private void refrescarSiEsASEJ(long pacienteIdASEJ) {
        PacienteDetalleASEJ pASEJ = pacienteActivoASEJ.getASEJ();
        if (pASEJ != null && pASEJ.idASEJ() == pacienteIdASEJ) {
            recargarASEJ();
        }
    }

    @FXML
    private void onCobrarASEJ() {
        PacienteDetalleASEJ pASEJ = pacienteActivoASEJ.getASEJ();
        if (pASEJ != null) {
            eventosASEJ.publishEvent(new NavegarAASEJ(MainControllerASEJ.VISTA_CAJAASEJ, Map.of("pacienteId", pASEJ.idASEJ())));
        }
    }

    @FXML
    private void onReciboASEJ() {
        accionesPagoASEJ.imprimirReciboASEJ(btnReciboASEJ, tblPagosASEJ.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void onAnularASEJ() {
        accionesPagoASEJ.anularASEJ(tblPagosASEJ.getSelectionModel().getSelectedItem(), dASEJ -> recargarASEJ());
    }
}
