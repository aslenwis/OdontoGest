package com.odontogest.ui.controlador;

import com.odontogest.dominio.comun.ValidacionesASEJ;
import com.odontogest.excepcion.NegocioExceptionASEJ;
import com.odontogest.modelo.SexoASEJ;
import com.odontogest.modelo.TipoDocumentoASEJ;
import com.odontogest.seguridad.PermisoASEJ;
import com.odontogest.seguridad.SesionUsuarioASEJ;
import com.odontogest.servicio.IntegracionServiceASEJ;
import com.odontogest.servicio.PacienteServiceASEJ;
import com.odontogest.servicio.dto.PacienteDetalleASEJ;
import com.odontogest.servicio.dto.PacienteFormASEJ;
import com.odontogest.servicio.dto.PersonaDniDtoASEJ;
import com.odontogest.ui.soporte.FormulariosASEJ;
import com.odontogest.ui.soporte.NotificadorASEJ;
import com.odontogest.ui.soporte.TareaUiASEJ;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.function.LongConsumer;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class PacienteFormControllerASEJ {

    private final PacienteServiceASEJ pacienteServiceASEJ;
    private final IntegracionServiceASEJ integracionServiceASEJ;
    private final TareaUiASEJ tareaUiASEJ;
    private final NotificadorASEJ notificadorASEJ;
    private final SesionUsuarioASEJ sesionUsuarioASEJ;

    @FXML private ComboBox<TipoDocumentoASEJ> cmbTipoDocumentoASEJ;
    @FXML private TextField txtNumeroDocumentoASEJ;
    @FXML private Button btnConsultarDniASEJ;
    @FXML private TextField txtNombresASEJ;
    @FXML private TextField txtApellidoPaternoASEJ;
    @FXML private TextField txtApellidoMaternoASEJ;
    @FXML private DatePicker dpFechaNacimientoASEJ;
    @FXML private Label lblEdadASEJ;
    @FXML private ComboBox<SexoASEJ> cmbSexoASEJ;
    @FXML private TextField txtOcupacionASEJ;
    @FXML private TextField txtTelefonoASEJ;
    @FXML private TextField txtEmailASEJ;
    @FXML private TextField txtDireccionASEJ;
    @FXML private TextField txtContactoNombreASEJ;
    @FXML private TextField txtContactoTelefonoASEJ;
    @FXML private CheckBox chkRecordatoriosASEJ;
    @FXML private VBox seccionApoderadoASEJ;
    @FXML private HBox pieASEJ;
    @FXML private TextField txtApoderadoNombreASEJ;
    @FXML private TextField txtApoderadoDocumentoASEJ;
    @FXML private ComboBox<String> cmbParentescoASEJ;
    @FXML private Button btnGuardarASEJ;
    @FXML private Button btnCancelarASEJ;

    private Long pacienteIdASEJ;
    private boolean dniHabilitadoASEJ;
    private LongConsumer alGuardarASEJ = idASEJ -> { };
    private Runnable alCancelarASEJ;

    @FXML
    public void initialize() {
        cmbTipoDocumentoASEJ.getItems().setAll(TipoDocumentoASEJ.values());
        cmbTipoDocumentoASEJ.setConverter(FormulariosASEJ.conversorASEJ(tASEJ -> tASEJ.etiquetaASEJ));
        cmbTipoDocumentoASEJ.setValue(TipoDocumentoASEJ.DNI);
        cmbTipoDocumentoASEJ.valueProperty().addListener((obsASEJ, aASEJ, tipoASEJ) -> {
            boolean sinDocumentoASEJ = tipoASEJ == TipoDocumentoASEJ.SIN_DOCUMENTO;
            txtNumeroDocumentoASEJ.setDisable(sinDocumentoASEJ);
            if (sinDocumentoASEJ) {
                txtNumeroDocumentoASEJ.clear();
            }
            actualizarBotonDniASEJ();
        });
        txtNumeroDocumentoASEJ.textProperty().addListener((obsASEJ, aASEJ, bASEJ) -> actualizarBotonDniASEJ());
        btnConsultarDniASEJ.setDisable(true);
        tareaUiASEJ.ejecutarASEJ(integracionServiceASEJ::dniHabilitadoASEJ, habilitadoASEJ -> {
            dniHabilitadoASEJ = habilitadoASEJ;
            btnConsultarDniASEJ.setTooltip(new Tooltip(habilitadoASEJ
                    ? "Completa nombres y apellidos desde la API de DNI (requiere internet)."
                    : "Consulta de DNI deshabilitada: actívela en Configuración → API DNI."));
            actualizarBotonDniASEJ();
        }, errorASEJ -> btnConsultarDniASEJ.setDisable(true));
        cmbSexoASEJ.getItems().setAll(SexoASEJ.values());
        cmbSexoASEJ.setConverter(FormulariosASEJ.conversorASEJ(sASEJ -> sASEJ.etiquetaASEJ));
        cmbParentescoASEJ.getItems().setAll("Madre", "Padre", "Tutor(a) legal", "Abuelo(a)", "Hermano(a)", "Otro");

        FormulariosASEJ.configurarFechaASEJ(dpFechaNacimientoASEJ);
        dpFechaNacimientoASEJ.valueProperty().addListener((obsASEJ, aASEJ, fechaASEJ) -> actualizarEdadASEJ(fechaASEJ));
        actualizarEdadASEJ(null);
        btnGuardarASEJ.setDisable(!sesionUsuarioASEJ.tieneASEJ(PermisoASEJ.PACIENTE_EDITAR));
    }

    public HBox extraerPieASEJ() {
        ((Pane) pieASEJ.getParent()).getChildren().remove(pieASEJ);
        return pieASEJ;
    }

    public void configurarASEJ(PacienteDetalleASEJ existenteASEJ, LongConsumer alGuardarASEJ, Runnable alCancelarASEJ) {
        this.alGuardarASEJ = alGuardarASEJ;
        this.alCancelarASEJ = alCancelarASEJ;
        btnCancelarASEJ.setVisible(alCancelarASEJ != null);
        btnCancelarASEJ.setManaged(alCancelarASEJ != null);
        cargarASEJ(existenteASEJ);
    }

    private void actualizarBotonDniASEJ() {
        String nASEJ = txtNumeroDocumentoASEJ.getText() == null ? "" : txtNumeroDocumentoASEJ.getText().trim();
        btnConsultarDniASEJ.setDisable(!dniHabilitadoASEJ || cmbTipoDocumentoASEJ.getValue() != TipoDocumentoASEJ.DNI
                || !nASEJ.matches("\\d{8}") || !sesionUsuarioASEJ.tieneASEJ(PermisoASEJ.PACIENTE_EDITAR));
    }

    @FXML
    private void onConsultarDniASEJ() {
        String dniASEJ = txtNumeroDocumentoASEJ.getText().trim();
        tareaUiASEJ.ejecutarASEJ(btnConsultarDniASEJ, () -> integracionServiceASEJ.consultarDniASEJ(dniASEJ), resultadoASEJ -> {
            if (resultadoASEJ.isEmpty()) {
                notificadorASEJ.infoASEJ("La API no encontró el DNI " + dniASEJ + ": ingrese los datos a mano.");
                return;
            }
            PersonaDniDtoASEJ pASEJ = resultadoASEJ.get();
            txtNombresASEJ.setText(textoASEJ(pASEJ.nombresASEJ()));
            txtApellidoPaternoASEJ.setText(textoASEJ(pASEJ.apellidoPaternoASEJ()));
            txtApellidoMaternoASEJ.setText(textoASEJ(pASEJ.apellidoMaternoASEJ()));
            notificadorASEJ.exitoASEJ("Datos completados desde la consulta de DNI.");
        });
    }

    public void cargarASEJ(PacienteDetalleASEJ pASEJ) {
        pacienteIdASEJ = pASEJ == null ? null : pASEJ.idASEJ();
        cmbTipoDocumentoASEJ.setValue(pASEJ == null ? TipoDocumentoASEJ.DNI : pASEJ.tipoDocumentoASEJ());
        txtNumeroDocumentoASEJ.setText(pASEJ == null ? "" : textoASEJ(pASEJ.numeroDocumentoASEJ()));
        txtNombresASEJ.setText(pASEJ == null ? "" : pASEJ.nombresASEJ());
        txtApellidoPaternoASEJ.setText(pASEJ == null ? "" : pASEJ.apellidoPaternoASEJ());
        txtApellidoMaternoASEJ.setText(pASEJ == null ? "" : textoASEJ(pASEJ.apellidoMaternoASEJ()));
        dpFechaNacimientoASEJ.setValue(pASEJ == null ? null : pASEJ.fechaNacimientoASEJ());
        cmbSexoASEJ.setValue(pASEJ == null ? null : pASEJ.sexoASEJ());
        txtOcupacionASEJ.setText(pASEJ == null ? "" : textoASEJ(pASEJ.ocupacionASEJ()));
        txtTelefonoASEJ.setText(pASEJ == null ? "" : textoASEJ(pASEJ.telefonoASEJ()));
        txtEmailASEJ.setText(pASEJ == null ? "" : textoASEJ(pASEJ.emailASEJ()));
        txtDireccionASEJ.setText(pASEJ == null ? "" : textoASEJ(pASEJ.direccionASEJ()));
        txtContactoNombreASEJ.setText(pASEJ == null ? "" : textoASEJ(pASEJ.contactoEmergenciaNombreASEJ()));
        txtContactoTelefonoASEJ.setText(pASEJ == null ? "" : textoASEJ(pASEJ.contactoEmergenciaTelefonoASEJ()));
        chkRecordatoriosASEJ.setSelected(pASEJ == null || pASEJ.aceptaRecordatoriosASEJ());
        txtApoderadoNombreASEJ.setText(pASEJ == null ? "" : textoASEJ(pASEJ.apoderadoNombreASEJ()));
        txtApoderadoDocumentoASEJ.setText(pASEJ == null ? "" : textoASEJ(pASEJ.apoderadoDocumentoASEJ()));
        cmbParentescoASEJ.setValue(pASEJ == null ? null : pASEJ.apoderadoParentescoASEJ());
    }

    public void enfocarASEJ() {
        Platform.runLater(txtNumeroDocumentoASEJ::requestFocus);
    }

    @FXML
    private void onGuardarASEJ() {
        LocalDate fechaNacimientoASEJ;
        try {
            fechaNacimientoASEJ = FormulariosASEJ.fechaASEJ(dpFechaNacimientoASEJ);
        } catch (NegocioExceptionASEJ eASEJ) {
            tareaUiASEJ.manejarErrorASEJ(eASEJ);
            return;
        }
        PacienteFormASEJ formASEJ = new PacienteFormASEJ(pacienteIdASEJ, cmbTipoDocumentoASEJ.getValue(), txtNumeroDocumentoASEJ.getText(),
                txtNombresASEJ.getText(), txtApellidoPaternoASEJ.getText(), txtApellidoMaternoASEJ.getText(),
                fechaNacimientoASEJ, cmbSexoASEJ.getValue(), txtTelefonoASEJ.getText(), txtEmailASEJ.getText(),
                txtDireccionASEJ.getText(), txtOcupacionASEJ.getText(), txtContactoNombreASEJ.getText(),
                txtContactoTelefonoASEJ.getText(), txtApoderadoNombreASEJ.getText(), txtApoderadoDocumentoASEJ.getText(),
                cmbParentescoASEJ.getEditor().getText(), chkRecordatoriosASEJ.isSelected());
        tareaUiASEJ.ejecutarASEJ(btnGuardarASEJ, () -> pacienteServiceASEJ.guardarASEJ(formASEJ), idASEJ -> {
            pacienteIdASEJ = idASEJ;
            alGuardarASEJ.accept(idASEJ);
        });
    }

    @FXML
    private void onCancelarASEJ() {
        if (alCancelarASEJ != null) {
            alCancelarASEJ.run();
        }
    }

    private void actualizarEdadASEJ(LocalDate fechaASEJ) {
        LocalDate hoyASEJ = LocalDate.now();
        boolean validaASEJ = fechaASEJ != null && !fechaASEJ.isAfter(hoyASEJ);
        int edadASEJ = validaASEJ ? ValidacionesASEJ.edadASEJ(fechaASEJ, hoyASEJ) : 0;
        lblEdadASEJ.setText(validaASEJ ? edadASEJ + (edadASEJ == 1 ? " año" : " años") : "");
        boolean menorASEJ = validaASEJ && edadASEJ < ValidacionesASEJ.MAYORIA_DE_EDADASEJ;
        seccionApoderadoASEJ.setVisible(menorASEJ);
        seccionApoderadoASEJ.setManaged(menorASEJ);
    }

    private static String textoASEJ(String valorASEJ) {
        return valorASEJ == null ? "" : valorASEJ;
    }
}
