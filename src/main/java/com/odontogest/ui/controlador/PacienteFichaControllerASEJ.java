package com.odontogest.ui.controlador;

import com.odontogest.evento.NavegarAASEJ;
import com.odontogest.evento.PagoRegistradoASEJ;
import com.odontogest.evento.PlanCambiadoASEJ;
import com.odontogest.evento.SolicitudNuevaAtencionASEJ;
import com.odontogest.seguridad.PermisoASEJ;
import com.odontogest.seguridad.SesionUsuarioASEJ;
import com.odontogest.servicio.PagoServiceASEJ;
import com.odontogest.servicio.dto.PacienteDetalleASEJ;
import com.odontogest.ui.soporte.FormulariosASEJ;
import com.odontogest.ui.soporte.HiloUiASEJ;
import com.odontogest.ui.soporte.PacienteActivoASEJ;
import com.odontogest.ui.soporte.TareaUiASEJ;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class PacienteFichaControllerASEJ {

    private final PacienteActivoASEJ pacienteActivoASEJ;
    private final ApplicationEventPublisher eventosASEJ;
    private final SesionUsuarioASEJ sesionUsuarioASEJ;
    private final PagoServiceASEJ pagoServiceASEJ;
    private final TareaUiASEJ tareaUiASEJ;

    @FXML private Label lblInicialesASEJ;
    @FXML private Label lblNombreASEJ;
    @FXML private Label lblDetalleASEJ;
    @FXML private Label lblAlergiasASEJ;
    @FXML private Label lblConsentimientoASEJ;
    @FXML private Label lblInactivoASEJ;
    @FXML private Label lblDemoASEJ;
    @FXML private TabPane pestanasASEJ;
    @FXML private Button btnNuevaAtencionASEJ;
    @FXML private Button btnNuevaCitaASEJ;
    @FXML private Label lblSaldoASEJ;

    private ChangeListener<PacienteDetalleASEJ> oyenteASEJ;

    @FXML
    public void initialize() {
        boolean atiendeASEJ = sesionUsuarioASEJ.tieneASEJ(PermisoASEJ.ATENCION_GESTIONAR);
        btnNuevaAtencionASEJ.setVisible(atiendeASEJ);
        btnNuevaAtencionASEJ.setManaged(atiendeASEJ);
        boolean agendaASEJ = sesionUsuarioASEJ.tieneASEJ(PermisoASEJ.AGENDA_GESTIONAR);
        btnNuevaCitaASEJ.setVisible(agendaASEJ);
        btnNuevaCitaASEJ.setManaged(agendaASEJ);
        if (oyenteASEJ != null) {
            pacienteActivoASEJ.pacientePropertyASEJ().removeListener(oyenteASEJ);
        }
        oyenteASEJ = (obsASEJ, anteriorASEJ, actualASEJ) -> mostrarASEJ(actualASEJ);
        pacienteActivoASEJ.pacientePropertyASEJ().addListener(oyenteASEJ);
        mostrarASEJ(pacienteActivoASEJ.getASEJ());
    }

    private void mostrarASEJ(PacienteDetalleASEJ pASEJ) {
        if (pASEJ == null) {
            lblInicialesASEJ.setText("");
            lblNombreASEJ.setText("Cargando…");
            lblDetalleASEJ.setText("");
            visibleASEJ(lblAlergiasASEJ, false);
            visibleASEJ(lblConsentimientoASEJ, false);
            visibleASEJ(lblInactivoASEJ, false);
            visibleASEJ(lblDemoASEJ, false);
            lblSaldoASEJ.setText("");
            return;
        }
        if (pestanasASEJ.getSelectionModel().getSelectedIndex() < 0) {
            pestanasASEJ.getSelectionModel().selectFirst();
        }
        lblInicialesASEJ.setText(inicialesASEJ(pASEJ));
        lblNombreASEJ.setText(pASEJ.nombreCompletoASEJ());
        lblDetalleASEJ.setText(pASEJ.numeroHistoriaASEJ() + "  ·  " + pASEJ.documentoASEJ() + "  ·  " + pASEJ.edadASEJ()
                + (pASEJ.edadASEJ() == 1 ? " año" : " años"));
        lblAlergiasASEJ.setText("Alergias: " + (pASEJ.tieneAlergiasASEJ() ? pASEJ.alergiasASEJ().replaceAll("\\s+", " ") : ""));
        visibleASEJ(lblAlergiasASEJ, pASEJ.tieneAlergiasASEJ());
        visibleASEJ(lblDemoASEJ, pASEJ.demoASEJ());
        lblConsentimientoASEJ.setText("Falta consentimiento de datos personales");
        visibleASEJ(lblConsentimientoASEJ, pASEJ.consentimientoDatosEnASEJ() == null);
        visibleASEJ(lblInactivoASEJ, !pASEJ.activoASEJ());
        cargarSaldoASEJ(pASEJ.idASEJ());
    }

    private void cargarSaldoASEJ(long pacienteIdASEJ) {
        tareaUiASEJ.ejecutarASEJ(() -> pagoServiceASEJ.saldoASEJ(pacienteIdASEJ), saldoASEJ -> {
            PacienteDetalleASEJ pASEJ = pacienteActivoASEJ.getASEJ();
            if (pASEJ != null && pASEJ.idASEJ() == pacienteIdASEJ) {
                lblSaldoASEJ.setText(FormulariosASEJ.solesASEJ(saldoASEJ));
                lblSaldoASEJ.getStyleClass().remove("ficha-saldo-deuda");
                if (saldoASEJ.signum() > 0) {
                    lblSaldoASEJ.getStyleClass().add("ficha-saldo-deuda");
                }
            }
        });
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void alRegistrarPagoASEJ(PagoRegistradoASEJ eASEJ) {
        HiloUiASEJ.ejecutarASEJ(() -> refrescarSaldoASEJ(eASEJ.pacienteIdASEJ()));
    }

    @EventListener
    public void alCambiarPlanASEJ(PlanCambiadoASEJ eASEJ) {
        HiloUiASEJ.ejecutarASEJ(() -> refrescarSaldoASEJ(eASEJ.pacienteIdASEJ()));
    }

    private void refrescarSaldoASEJ(long pacienteIdASEJ) {
        PacienteDetalleASEJ pASEJ = pacienteActivoASEJ.getASEJ();
        if (lblSaldoASEJ != null && pASEJ != null && pASEJ.idASEJ() == pacienteIdASEJ) {
            cargarSaldoASEJ(pacienteIdASEJ);
        }
    }

    public void seleccionarPestanaASEJ(String idASEJ) {
        if (pestanasASEJ == null || idASEJ == null) {
            return;
        }
        pestanasASEJ.getTabs().stream().filter(tASEJ -> idASEJ.equals(tASEJ.getId())).findFirst()
                .ifPresent(tASEJ -> pestanasASEJ.getSelectionModel().select(tASEJ));
    }

    @FXML
    private void onNuevaCitaASEJ() {
        PacienteDetalleASEJ pASEJ = pacienteActivoASEJ.getASEJ();
        if (pASEJ != null) {
            eventosASEJ.publishEvent(new NavegarAASEJ(MainControllerASEJ.VISTA_AGENDAASEJ, Map.of("pacienteId", pASEJ.idASEJ())));
        }
    }

    @FXML
    private void onNuevaAtencionASEJ() {
        eventosASEJ.publishEvent(new SolicitudNuevaAtencionASEJ());
    }

    @FXML
    private void onVolverASEJ() {
        eventosASEJ.publishEvent(new NavegarAASEJ(MainControllerASEJ.VISTA_PACIENTESASEJ));
    }

    private static String inicialesASEJ(PacienteDetalleASEJ pASEJ) {
        return (pASEJ.nombresASEJ().substring(0, 1) + pASEJ.apellidoPaternoASEJ().substring(0, 1)).toUpperCase();
    }

    private static void visibleASEJ(Label lASEJ, boolean visibleASEJ) {
        lASEJ.setVisible(visibleASEJ);
        lASEJ.setManaged(visibleASEJ);
    }
}
