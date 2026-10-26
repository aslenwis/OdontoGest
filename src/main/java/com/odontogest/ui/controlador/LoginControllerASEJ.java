package com.odontogest.ui.controlador;

import com.odontogest.excepcion.NegocioExceptionASEJ;
import com.odontogest.servicio.AutenticacionServiceASEJ;
import com.odontogest.ui.soporte.CargadorVistasASEJ;
import com.odontogest.ui.soporte.TareaUiASEJ;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoginControllerASEJ {

    private final AutenticacionServiceASEJ autenticacionServiceASEJ;
    private final CargadorVistasASEJ cargadorVistasASEJ;
    private final TareaUiASEJ tareaUiASEJ;

    @Value("${app.seeder.enabled:false}")
    private boolean modoDemoASEJ;

    @FXML private TextField txtUsuarioASEJ;
    @FXML private PasswordField txtPasswordASEJ;
    @FXML private Label lblErrorASEJ;
    @FXML private Label lblDemoASEJ;
    @FXML private Button btnIngresarASEJ;

    @FXML
    public void initialize() {
        ocultarErrorASEJ();
        lblDemoASEJ.setVisible(modoDemoASEJ);
        lblDemoASEJ.setManaged(modoDemoASEJ);
        txtUsuarioASEJ.textProperty().addListener((obsASEJ, aASEJ, bASEJ) -> ocultarErrorASEJ());
        txtPasswordASEJ.textProperty().addListener((obsASEJ, aASEJ, bASEJ) -> ocultarErrorASEJ());
    }

    @FXML
    private void onIngresarASEJ() {
        String usuarioASEJ = txtUsuarioASEJ.getText();
        String passwordASEJ = txtPasswordASEJ.getText();
        ocultarErrorASEJ();
        tareaUiASEJ.ejecutarASEJ(btnIngresarASEJ,
                () -> autenticacionServiceASEJ.iniciarSesionASEJ(usuarioASEJ, passwordASEJ),
                sesionASEJ -> cargadorVistasASEJ.mostrarPrincipalASEJ(),
                errorASEJ -> {
                    if (errorASEJ instanceof NegocioExceptionASEJ) {
                        mostrarErrorASEJ(errorASEJ.getMessage());
                        txtPasswordASEJ.clear();
                        txtPasswordASEJ.requestFocus();
                    } else {
                        tareaUiASEJ.manejarErrorASEJ(errorASEJ);
                    }
                });
    }

    @FXML
    private void onAcercaDeASEJ() {
        cargadorVistasASEJ.mostrarModalASEJ("acerca-de-view", "Acerca de OdontoGest");
    }

    private void mostrarErrorASEJ(String mensajeASEJ) {
        lblErrorASEJ.setText(mensajeASEJ);
        lblErrorASEJ.setVisible(true);
        lblErrorASEJ.setManaged(true);
    }

    private void ocultarErrorASEJ() {
        lblErrorASEJ.setText("");
        lblErrorASEJ.setVisible(false);
        lblErrorASEJ.setManaged(false);
    }
}
