/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.soporte;

import atlantafx.base.controls.ModalPane;
import atlantafx.base.controls.Notification;
import atlantafx.base.theme.Styles;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Mensajes al usuario: notificaciones emergentes (toasts), diálogo de confirmación y panel lateral.
 * <p>
 * {@code MainController} registra sus contenedores al mostrarse. Si no hay contenedores
 * registrados (por ejemplo, en la pantalla de login) los mensajes se muestran con un {@link Alert}.
 * Todos los métodos se ejecutan en el hilo de UI.
 */
@Component
public class Notificador {

    private static final Duration DURACION_TOAST = Duration.seconds(4);

    private StackPane raiz;
    private ModalPane modal;
    private VBox zonaToasts;

    public void registrar(StackPane raiz, ModalPane modal, VBox zonaToasts) {
        this.raiz = raiz;
        this.modal = modal;
        this.zonaToasts = zonaToasts;
    }

    public void desregistrar() {
        this.raiz = null;
        this.modal = null;
        this.zonaToasts = null;
    }

    public void info(String mensaje) {
        mostrar(mensaje, Feather.INFO, Styles.ACCENT, Alert.AlertType.INFORMATION);
    }

    public void exito(String mensaje) {
        mostrar(mensaje, Feather.CHECK_CIRCLE, Styles.SUCCESS, Alert.AlertType.INFORMATION);
    }

    public void advertencia(String mensaje) {
        mostrar(mensaje, Feather.ALERT_TRIANGLE, Styles.WARNING, Alert.AlertType.WARNING);
    }

    public void error(String mensaje) {
        mostrar(mensaje, Feather.ALERT_OCTAGON, Styles.DANGER, Alert.AlertType.ERROR);
    }

    /** Diálogo modal de confirmación; devuelve {@code true} si el usuario acepta. */
    public boolean confirmar(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, mensaje, ButtonType.CANCEL, ButtonType.OK);
        alerta.setTitle(titulo);
        alerta.setHeaderText(titulo);
        asignarVentana(alerta);
        Optional<ButtonType> respuesta = alerta.showAndWait();
        return respuesta.isPresent() && respuesta.get() == ButtonType.OK;
    }

    /** Muestra un formulario en el panel lateral derecho. */
    public void abrirPanel(Node contenido) {
        if (modal == null) {
            throw new IllegalStateException("No hay ventana principal para mostrar el panel lateral");
        }
        modal.setAlignment(Pos.TOP_RIGHT);
        modal.usePredefinedTransitionFactories(Side.RIGHT);
        modal.show(contenido);
    }

    public void cerrarPanel() {
        if (modal != null) {
            modal.hide(true);
        }
    }

    private void mostrar(String mensaje, Ikon icono, String estilo, Alert.AlertType tipoAlerta) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> mostrar(mensaje, icono, estilo, tipoAlerta));
            return;
        }
        if (zonaToasts == null) {
            Alert alerta = new Alert(tipoAlerta, mensaje, ButtonType.OK);
            alerta.setTitle("OdontoGest");
            alerta.setHeaderText(null);
            asignarVentana(alerta);
            alerta.showAndWait();
            return;
        }
        VBox zona = zonaToasts;
        Notification toast = new Notification(mensaje, new FontIcon(icono));
        toast.getStyleClass().addAll(estilo, Styles.ELEVATED_1);
        toast.setPrefHeight(Region.USE_PREF_SIZE);
        toast.setMaxHeight(Region.USE_PREF_SIZE);
        toast.setOnClose(e -> zona.getChildren().remove(toast));
        zona.getChildren().add(toast);

        PauseTransition espera = new PauseTransition(DURACION_TOAST);
        espera.setOnFinished(e -> zona.getChildren().remove(toast));
        espera.play();
    }

    private void asignarVentana(Alert alerta) {
        if (raiz != null && raiz.getScene() != null) {
            alerta.initOwner(raiz.getScene().getWindow());
        }
    }
}
