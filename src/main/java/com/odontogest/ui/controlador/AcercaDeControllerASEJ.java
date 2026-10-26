package com.odontogest.ui.controlador;

import atlantafx.base.theme.Styles;
import com.odontogest.servicio.SistemaServiceASEJ;
import com.odontogest.servicio.dto.InformacionSistemaDtoASEJ;
import com.odontogest.servicio.dto.IntegridadBaseDtoASEJ;
import com.odontogest.ui.soporte.ExportadorASEJ;
import com.odontogest.ui.soporte.FormulariosASEJ;
import com.odontogest.ui.soporte.TareaUiASEJ;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
@RequiredArgsConstructor
public class AcercaDeControllerASEJ {

    private final SistemaServiceASEJ sistemaServiceASEJ;
    private final TareaUiASEJ tareaUiASEJ;
    private final ExportadorASEJ exportadorASEJ;

    @FXML private Label lblNombreASEJ;
    @FXML private Label lblDescripcionASEJ;
    @FXML private Label lblVersionASEJ;
    @FXML private VBox boxAutoresASEJ;
    @FXML private Label lblCursoASEJ;
    @FXML private Label lblUniversidadASEJ;
    @FXML private Label lblAnioASEJ;
    @FXML private Label lblJavaASEJ;
    @FXML private Label lblAvisoASEJ;
    @FXML private Hyperlink lnkDatosASEJ;
    @FXML private Label lblBaseASEJ;
    @FXML private Hyperlink lnkLogsASEJ;
    @FXML private Button btnVerificarASEJ;
    @FXML private Label lblIntegridadASEJ;
    @FXML private Button btnCerrarASEJ;

    @FXML
    public void initialize() {
        lblNombreASEJ.setText(SistemaServiceASEJ.NOMBREASEJ);
        lblDescripcionASEJ.setText(SistemaServiceASEJ.DESCRIPCIONASEJ);
        tareaUiASEJ.ejecutarASEJ(sistemaServiceASEJ::informacionASEJ, this::mostrarASEJ);
    }

    private void mostrarASEJ(InformacionSistemaDtoASEJ infoASEJ) {
        lblVersionASEJ.setText(infoASEJ.versionASEJ() + (infoASEJ.compiladoASEJ() == null ? ""
                : "  ·  compilada el " + FormulariosASEJ.fechaHoraASEJ(infoASEJ.compiladoASEJ())));
        boxAutoresASEJ.getChildren().setAll(infoASEJ.autoresASEJ().stream().map(Label::new).toList());
        lblCursoASEJ.setText(infoASEJ.cursoASEJ());
        lblUniversidadASEJ.setText(infoASEJ.universidadASEJ());
        lblAnioASEJ.setText(String.valueOf(infoASEJ.anioASEJ()));
        lblJavaASEJ.setText(infoASEJ.javaVersionASEJ() + "  ·  JavaFX " + System.getProperty("javafx.version", "—"));
        lblAvisoASEJ.setText(infoASEJ.avisoASEJ());
        lnkDatosASEJ.setText(infoASEJ.carpetaDatosASEJ());
        lblBaseASEJ.setText(infoASEJ.baseDatosASEJ());
        lnkLogsASEJ.setText(infoASEJ.carpetaLogsASEJ());
        ajustarVentanaASEJ();
    }

    private void ajustarVentanaASEJ() {
        if (btnCerrarASEJ.getScene() != null && btnCerrarASEJ.getScene().getWindow() instanceof Stage ventanaASEJ) {
            ventanaASEJ.sizeToScene();
        }
    }

    @FXML
    private void onVerificarASEJ() {
        lblIntegridadASEJ.setText("Verificando…");
        lblIntegridadASEJ.getStyleClass().removeAll(Styles.SUCCESS, Styles.DANGER);
        lblIntegridadASEJ.setVisible(true);
        lblIntegridadASEJ.setManaged(true);
        ajustarVentanaASEJ();
        tareaUiASEJ.ejecutarASEJ(btnVerificarASEJ, sistemaServiceASEJ::verificarIntegridadASEJ, this::mostrarIntegridadASEJ,
                errorASEJ -> {
                    lblIntegridadASEJ.setText("");
                    tareaUiASEJ.manejarErrorASEJ(errorASEJ);
                });
    }

    private void mostrarIntegridadASEJ(IntegridadBaseDtoASEJ rASEJ) {
        String tamanioASEJ = rASEJ.tamanioBytesASEJ() < 0 ? "" : rASEJ.tamanioBytesASEJ() < 1024 * 1024
                ? String.format(" (%d KB)", rASEJ.tamanioBytesASEJ() / 1024)
                : String.format(" (%.1f MB)", rASEJ.tamanioBytesASEJ() / (1024.0 * 1024));
        if (rASEJ.correctaASEJ()) {
            lblIntegridadASEJ.setText("✔ La base de datos está íntegra" + tamanioASEJ + ": sin errores de estructura ni "
                    + "referencias rotas.");
            lblIntegridadASEJ.getStyleClass().add(Styles.SUCCESS);
            ajustarVentanaASEJ();
            return;
        }
        StringBuilder sbASEJ = new StringBuilder("✖ Se encontraron problemas");
        if (!rASEJ.problemasASEJ().isEmpty()) {
            sbASEJ.append(": ").append(String.join("; ", rASEJ.problemasASEJ().subList(0, Math.min(3, rASEJ.problemasASEJ().size()))));
        }
        if (rASEJ.clavesForaneasASEJ() > 0) {
            sbASEJ.append(". ").append(rASEJ.clavesForaneasASEJ()).append(" referencia(s) a registros inexistentes");
        }
        sbASEJ.append(". Restaure el último respaldo (Configuración → Respaldo) y revise el log.");
        lblIntegridadASEJ.setText(sbASEJ.toString());
        lblIntegridadASEJ.getStyleClass().add(Styles.DANGER);
        ajustarVentanaASEJ();
    }

    @FXML
    private void onAbrirDatosASEJ() {
        exportadorASEJ.abrirASEJ(new File(lnkDatosASEJ.getText()));
    }

    @FXML
    private void onAbrirLogsASEJ() {
        exportadorASEJ.abrirASEJ(new File(lnkLogsASEJ.getText()));
    }

    @FXML
    private void onCerrarASEJ() {
        ((Stage) btnCerrarASEJ.getScene().getWindow()).close();
    }
}
