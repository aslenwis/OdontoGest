package com.odontogest.ui.controlador;

import atlantafx.base.theme.Styles;
import com.odontogest.seguridad.PermisoASEJ;
import com.odontogest.seguridad.SesionUsuarioASEJ;
import com.odontogest.servicio.ConsentimientoServiceASEJ;
import com.odontogest.servicio.PacienteServiceASEJ;
import com.odontogest.servicio.dto.PacienteDetalleASEJ;
import com.odontogest.ui.soporte.CargadorVistasASEJ;
import com.odontogest.ui.soporte.ExportadorASEJ;
import com.odontogest.ui.soporte.FormulariosASEJ;
import com.odontogest.ui.soporte.NotificadorASEJ;
import com.odontogest.ui.soporte.PacienteActivoASEJ;
import com.odontogest.ui.soporte.TareaUiASEJ;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DatosPacienteControllerASEJ {

    private final PacienteActivoASEJ pacienteActivoASEJ;
    private final PacienteServiceASEJ pacienteServiceASEJ;
    private final ConsentimientoServiceASEJ consentimientoServiceASEJ;
    private final CargadorVistasASEJ cargadorVistasASEJ;
    private final ExportadorASEJ exportadorASEJ;
    private final NotificadorASEJ notificadorASEJ;
    private final TareaUiASEJ tareaUiASEJ;
    private final SesionUsuarioASEJ sesionUsuarioASEJ;

    @FXML private StackPane contenedorFormularioASEJ;
    @FXML private HBox barraAccionesASEJ;
    @FXML private Label lblEstadoConsentimientoASEJ;
    @FXML private Button btnPdfConsentimientoASEJ;
    @FXML private Button btnRegistrarFirmaASEJ;
    @FXML private Button btnEstadoASEJ;

    private PacienteFormControllerASEJ formularioASEJ;
    private ChangeListener<PacienteDetalleASEJ> oyenteASEJ;

    @FXML
    public void initialize() {
        CargadorVistasASEJ.VistaASEJ vistaASEJ = cargadorVistasASEJ.cargarNuevaASEJ("paciente-form-view");
        formularioASEJ = (PacienteFormControllerASEJ) vistaASEJ.controladorASEJ();
        formularioASEJ.configurarASEJ(null, idASEJ -> {
            notificadorASEJ.exitoASEJ("Datos del paciente guardados.");
            pacienteActivoASEJ.recargarASEJ();
        }, null);
        contenedorFormularioASEJ.getChildren().setAll(vistaASEJ.raizASEJ());
        HBox pieASEJ = formularioASEJ.extraerPieASEJ();
        HBox.setHgrow(pieASEJ, Priority.ALWAYS);
        pieASEJ.setMaxWidth(640);
        barraAccionesASEJ.getChildren().setAll(pieASEJ);

        boolean puedeEditarASEJ = sesionUsuarioASEJ.tieneASEJ(PermisoASEJ.PACIENTE_EDITAR);
        btnRegistrarFirmaASEJ.setDisable(!puedeEditarASEJ);
        btnEstadoASEJ.setDisable(!puedeEditarASEJ);

        if (oyenteASEJ != null) {
            pacienteActivoASEJ.pacientePropertyASEJ().removeListener(oyenteASEJ);
        }
        oyenteASEJ = (obsASEJ, anteriorASEJ, actualASEJ) -> mostrarASEJ(actualASEJ);
        pacienteActivoASEJ.pacientePropertyASEJ().addListener(oyenteASEJ);
        mostrarASEJ(pacienteActivoASEJ.getASEJ());
    }

    private void mostrarASEJ(PacienteDetalleASEJ pASEJ) {
        formularioASEJ.cargarASEJ(pASEJ);
        if (pASEJ == null) {
            lblEstadoConsentimientoASEJ.setText("");
            return;
        }
        lblEstadoConsentimientoASEJ.setText(pASEJ.consentimientoDatosEnASEJ() == null
                ? "Pendiente: aún no se registró la firma."
                : "Firmado el " + FormulariosASEJ.fechaHoraASEJ(pASEJ.consentimientoDatosEnASEJ()) + ".");
        lblEstadoConsentimientoASEJ.getStyleClass().removeAll(Styles.SUCCESS, Styles.WARNING);
        lblEstadoConsentimientoASEJ.getStyleClass().add(pASEJ.consentimientoDatosEnASEJ() == null ? Styles.WARNING : Styles.SUCCESS);
        btnEstadoASEJ.setText(pASEJ.activoASEJ() ? "Desactivar paciente" : "Reactivar paciente");
        btnEstadoASEJ.setGraphic(new FontIcon(pASEJ.activoASEJ() ? Feather.USER_X : Feather.USER_CHECK));
    }

    @FXML
    private void onPdfConsentimientoASEJ() {
        PacienteDetalleASEJ pASEJ = pacienteActivoASEJ.getASEJ();
        if (pASEJ != null) {
            exportadorASEJ.guardarPdfASEJ(btnPdfConsentimientoASEJ, "consentimiento-datos-" + pASEJ.numeroHistoriaASEJ() + ".pdf",
                    () -> consentimientoServiceASEJ.pdfDatosPersonalesASEJ(pASEJ.idASEJ()));
        }
    }

    @FXML
    private void onRegistrarFirmaASEJ() {
        PacienteDetalleASEJ pASEJ = pacienteActivoASEJ.getASEJ();
        if (pASEJ == null) {
            return;
        }
        TextField txtFirmanteASEJ = new TextField(pASEJ.esMenorDeEdadASEJ() && pASEJ.apoderadoNombreASEJ() != null
                ? pASEJ.apoderadoNombreASEJ() : pASEJ.nombreCompletoASEJ());
        GridPane gASEJ = FormulariosASEJ.grillaASEJ();
        FormulariosASEJ.filaASEJ(gASEJ, "Firmado por", txtFirmanteASEJ);
        Label ayudaASEJ = new Label(pASEJ.esMenorDeEdadASEJ()
                ? "El paciente es menor de edad: firma su apoderado."
                : "Registre la firma después de que el paciente firme el documento impreso.");
        ayudaASEJ.setWrapText(true);
        ayudaASEJ.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);

        Button cancelarASEJ = FormulariosASEJ.botonASEJ("Cancelar", null);
        cancelarASEJ.setCancelButton(true);
        cancelarASEJ.setOnAction(eASEJ -> notificadorASEJ.cerrarPanelASEJ());
        Button registrarASEJ = FormulariosASEJ.botonASEJ("Registrar firma", Feather.CHECK, Styles.ACCENT);
        registrarASEJ.setDefaultButton(true);
        registrarASEJ.setOnAction(eASEJ -> tareaUiASEJ.ejecutarASEJ(registrarASEJ,
                () -> consentimientoServiceASEJ.registrarDatosPersonalesASEJ(pASEJ.idASEJ(), txtFirmanteASEJ.getText()),
                cASEJ -> {
                    notificadorASEJ.cerrarPanelASEJ();
                    notificadorASEJ.exitoASEJ("Consentimiento registrado.");
                    pacienteActivoASEJ.recargarASEJ();
                }));
        notificadorASEJ.abrirPanelASEJ(FormulariosASEJ.panelLateralASEJ("Consentimiento de datos personales",
                new VBox(12, gASEJ, ayudaASEJ), cancelarASEJ, registrarASEJ));
    }

    @FXML
    private void onCambiarEstadoASEJ() {
        PacienteDetalleASEJ pASEJ = pacienteActivoASEJ.getASEJ();
        if (pASEJ == null) {
            return;
        }
        boolean activarASEJ = !pASEJ.activoASEJ();
        String preguntaASEJ = activarASEJ ? "¿Reactivar a " + pASEJ.nombreCompletoASEJ() + "?"
                : "El paciente dejará de aparecer en las búsquedas; su historia clínica se conserva. ¿Desactivar a "
                  + pASEJ.nombreCompletoASEJ() + "?";
        if (!notificadorASEJ.confirmarASEJ(activarASEJ ? "Reactivar paciente" : "Desactivar paciente", preguntaASEJ)) {
            return;
        }
        tareaUiASEJ.ejecutarASEJ(btnEstadoASEJ, () -> {
            pacienteServiceASEJ.cambiarEstadoASEJ(pASEJ.idASEJ(), activarASEJ);
            return null;
        }, rASEJ -> {
            notificadorASEJ.exitoASEJ(activarASEJ ? "Paciente reactivado." : "Paciente desactivado.");
            pacienteActivoASEJ.recargarASEJ();
        });
    }
}
