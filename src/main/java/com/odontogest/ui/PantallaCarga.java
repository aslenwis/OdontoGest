/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui;

import javafx.application.Preloader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Pantalla de carga (preloader de JavaFX): se muestra mientras {@code JavaFxApplication.init()} levanta Spring y,
 * en el primer arranque, siembra los datos de demostración. {@code Launcher} la registra con la propiedad
 * {@code javafx.preloader}; se cierra sola antes de mostrar el inicio de sesión.
 */
public class PantallaCarga extends Preloader {

    private Stage ventana;

    @Override
    public void start(Stage stage) {
        ventana = stage;
        FontIcon icono = new FontIcon(Feather.ACTIVITY);
        icono.setIconSize(40);
        icono.setStyle("-fx-icon-color: #2563EB;");
        Label nombre = new Label("OdontoGest");
        nombre.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #1F2937;");
        Label mensaje = new Label("Iniciando… La primera vez se preparan los datos de demostración.");
        mensaje.setStyle("-fx-text-fill: #6B7280;");
        mensaje.setWrapText(true);
        ProgressBar barra = new ProgressBar(ProgressBar.INDETERMINATE_PROGRESS);
        barra.setPrefWidth(300);
        VBox raiz = new VBox(12, icono, nombre, barra, mensaje);
        raiz.setAlignment(Pos.CENTER);
        raiz.setPadding(new Insets(28));
        raiz.setStyle("-fx-background-color: white; -fx-border-color: #D1D5DB; -fx-border-width: 1;");
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setTitle("OdontoGest");
        stage.setScene(new Scene(raiz, 420, 230));
        stage.centerOnScreen();
        stage.show();
    }

    @Override
    public void handleStateChangeNotification(StateChangeNotification evento) {
        if (evento.getType() == StateChangeNotification.Type.BEFORE_START && ventana != null) {
            ventana.hide();
        }
    }
}
