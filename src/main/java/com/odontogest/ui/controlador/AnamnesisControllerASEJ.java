package com.odontogest.ui.controlador;

import com.odontogest.modelo.SexoASEJ;
import com.odontogest.seguridad.PermisoASEJ;
import com.odontogest.seguridad.SesionUsuarioASEJ;
import com.odontogest.servicio.AnamnesisServiceASEJ;
import com.odontogest.servicio.dto.AnamnesisDtoASEJ;
import com.odontogest.servicio.dto.PacienteDetalleASEJ;
import com.odontogest.ui.soporte.FormulariosASEJ;
import com.odontogest.ui.soporte.NotificadorASEJ;
import com.odontogest.ui.soporte.PacienteActivoASEJ;
import com.odontogest.ui.soporte.TareaUiASEJ;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AnamnesisControllerASEJ {

    private final AnamnesisServiceASEJ anamnesisServiceASEJ;
    private final PacienteActivoASEJ pacienteActivoASEJ;
    private final NotificadorASEJ notificadorASEJ;
    private final TareaUiASEJ tareaUiASEJ;
    private final SesionUsuarioASEJ sesionUsuarioASEJ;

    @FXML private TextArea txtAlergiasASEJ;
    @FXML private TextArea txtMedicacionASEJ;
    @FXML private TextArea txtAntecedentesASEJ;
    @FXML private TextArea txtHabitosASEJ;
    @FXML private FlowPane panelEnfermedadesASEJ;
    @FXML private CheckBox chkFumaASEJ;
    @FXML private CheckBox chkGestanteASEJ;
    @FXML private TextField txtPresionASEJ;
    @FXML private TextArea txtObservacionesASEJ;
    @FXML private Label lblActualizadoASEJ;
    @FXML private Button btnGuardarASEJ;

    private final Map<String, CheckBox> enfermedadesASEJ = new LinkedHashMap<>();
    private ChangeListener<PacienteDetalleASEJ> oyenteASEJ;

    @FXML
    public void initialize() {
        enfermedadesASEJ.clear();
        AnamnesisServiceASEJ.ENFERMEDADESASEJ.forEach((claveASEJ, etiquetaASEJ) -> {
            CheckBox cASEJ = new CheckBox(etiquetaASEJ);
            enfermedadesASEJ.put(claveASEJ, cASEJ);
            panelEnfermedadesASEJ.getChildren().add(cASEJ);
        });
        boolean puedeEditarASEJ = sesionUsuarioASEJ.tieneASEJ(PermisoASEJ.ANAMNESIS_EDITAR);
        btnGuardarASEJ.setDisable(!puedeEditarASEJ);
        if (!puedeEditarASEJ) {
            lblActualizadoASEJ.setText("Solo lectura: su rol no puede editar la anamnesis.");
        }

        if (oyenteASEJ != null) {
            pacienteActivoASEJ.pacientePropertyASEJ().removeListener(oyenteASEJ);
        }
        oyenteASEJ = (obsASEJ, anteriorASEJ, actualASEJ) -> cargarASEJ(actualASEJ);
        pacienteActivoASEJ.pacientePropertyASEJ().addListener(oyenteASEJ);
        cargarASEJ(pacienteActivoASEJ.getASEJ());
    }

    private void cargarASEJ(PacienteDetalleASEJ pASEJ) {
        if (pASEJ == null) {
            mostrarASEJ(null);
            return;
        }
        chkGestanteASEJ.setDisable(pASEJ.sexoASEJ() != SexoASEJ.F);
        tareaUiASEJ.ejecutarASEJ(() -> anamnesisServiceASEJ.obtenerASEJ(pASEJ.idASEJ()), this::mostrarASEJ);
    }

    private void mostrarASEJ(AnamnesisDtoASEJ aASEJ) {
        txtAlergiasASEJ.setText(aASEJ == null ? "" : textoASEJ(aASEJ.alergiasASEJ()));
        txtMedicacionASEJ.setText(aASEJ == null ? "" : textoASEJ(aASEJ.medicacionActualASEJ()));
        txtAntecedentesASEJ.setText(aASEJ == null ? "" : textoASEJ(aASEJ.antecedentesMedicosASEJ()));
        txtHabitosASEJ.setText(aASEJ == null ? "" : textoASEJ(aASEJ.habitosASEJ()));
        enfermedadesASEJ.forEach((claveASEJ, cASEJ) -> cASEJ.setSelected(aASEJ != null && Boolean.TRUE.equals(aASEJ.enfermedadesASEJ().get(claveASEJ))));
        chkFumaASEJ.setSelected(aASEJ != null && aASEJ.fumaASEJ());
        chkGestanteASEJ.setSelected(aASEJ != null && aASEJ.gestanteASEJ());
        txtPresionASEJ.setText(aASEJ == null ? "" : textoASEJ(aASEJ.presionArterialASEJ()));
        txtObservacionesASEJ.setText(aASEJ == null ? "" : textoASEJ(aASEJ.observacionesASEJ()));
        if (sesionUsuarioASEJ.tieneASEJ(PermisoASEJ.ANAMNESIS_EDITAR)) {
            lblActualizadoASEJ.setText(aASEJ == null || aASEJ.actualizadoEnASEJ() == null ? "Sin registrar"
                    : "Actualizada el " + FormulariosASEJ.fechaHoraASEJ(aASEJ.actualizadoEnASEJ()));
        }
    }

    @FXML
    private void onGuardarASEJ() {
        PacienteDetalleASEJ pASEJ = pacienteActivoASEJ.getASEJ();
        if (pASEJ == null) {
            return;
        }
        Map<String, Boolean> marcadasASEJ = new LinkedHashMap<>();
        enfermedadesASEJ.forEach((claveASEJ, cASEJ) -> marcadasASEJ.put(claveASEJ, cASEJ.isSelected()));
        AnamnesisDtoASEJ dtoASEJ = new AnamnesisDtoASEJ(pASEJ.idASEJ(), txtAntecedentesASEJ.getText(), txtAlergiasASEJ.getText(),
                txtMedicacionASEJ.getText(), marcadasASEJ, txtHabitosASEJ.getText(), chkFumaASEJ.isSelected(), chkGestanteASEJ.isSelected(),
                txtPresionASEJ.getText(), txtObservacionesASEJ.getText(), null);
        tareaUiASEJ.ejecutarASEJ(btnGuardarASEJ, () -> anamnesisServiceASEJ.guardarASEJ(dtoASEJ), guardadaASEJ -> {
            mostrarASEJ(guardadaASEJ);
            notificadorASEJ.exitoASEJ("Anamnesis guardada.");
            pacienteActivoASEJ.recargarASEJ();
        });
    }

    private static String textoASEJ(String valorASEJ) {
        return valorASEJ == null ? "" : valorASEJ;
    }
}
