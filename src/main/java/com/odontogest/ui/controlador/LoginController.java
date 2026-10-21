/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.controlador;

import com.odontogest.excepcion.NegocioException;
import com.odontogest.servicio.AutenticacionService;
import com.odontogest.ui.soporte.CargadorVistas;
import com.odontogest.ui.soporte.TareaUi;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Pantalla de inicio de sesión (RF-SEG-01).
 */
@Component
@RequiredArgsConstructor
public class LoginController {

    private final AutenticacionService autenticacionService;
    private final CargadorVistas cargadorVistas;
    private final TareaUi tareaUi;

    @Value("${app.seeder.enabled:false}")
    private boolean modoDemo;

    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblError;
    @FXML private Label lblDemo;
    @FXML private Button btnIngresar;

    @FXML
    public void initialize() {
        ocultarError();
        lblDemo.setVisible(modoDemo);
        lblDemo.setManaged(modoDemo);
        txtUsuario.textProperty().addListener((obs, a, b) -> ocultarError());
        txtPassword.textProperty().addListener((obs, a, b) -> ocultarError());
    }

    @FXML
    private void onIngresar() {
        String usuario = txtUsuario.getText();
        String password = txtPassword.getText();
        ocultarError();
        tareaUi.ejecutar(btnIngresar,
                () -> autenticacionService.iniciarSesion(usuario, password),
                sesion -> cargadorVistas.mostrarPrincipal(),
                error -> {
                    if (error instanceof NegocioException) {
                        mostrarError(error.getMessage());
                        txtPassword.clear();
                        txtPassword.requestFocus();
                    } else {
                        tareaUi.manejarError(error);
                    }
                });
    }

    @FXML
    private void onAcercaDe() {
        cargadorVistas.mostrarModal("acerca-de-view", "Acerca de OdontoGest");
    }

    private void mostrarError(String mensaje) {
        lblError.setText(mensaje);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void ocultarError() {
        lblError.setText("");
        lblError.setVisible(false);
        lblError.setManaged(false);
    }
}
