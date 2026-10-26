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

@Component
public class NotificadorASEJ {

    private static final Duration DURACION_TOASTASEJ = Duration.seconds(4);

    private StackPane raizASEJ;
    private ModalPane modalASEJ;
    private VBox zonaToastsASEJ;

    public void registrarASEJ(StackPane raizASEJ, ModalPane modalASEJ, VBox zonaToastsASEJ) {
        this.raizASEJ = raizASEJ;
        this.modalASEJ = modalASEJ;
        this.zonaToastsASEJ = zonaToastsASEJ;
    }

    public void desregistrarASEJ() {
        this.raizASEJ = null;
        this.modalASEJ = null;
        this.zonaToastsASEJ = null;
    }

    public void infoASEJ(String mensajeASEJ) {
        mostrarASEJ(mensajeASEJ, Feather.INFO, Styles.ACCENT, Alert.AlertType.INFORMATION);
    }

    public void exitoASEJ(String mensajeASEJ) {
        mostrarASEJ(mensajeASEJ, Feather.CHECK_CIRCLE, Styles.SUCCESS, Alert.AlertType.INFORMATION);
    }

    public void advertenciaASEJ(String mensajeASEJ) {
        mostrarASEJ(mensajeASEJ, Feather.ALERT_TRIANGLE, Styles.WARNING, Alert.AlertType.WARNING);
    }

    public void errorASEJ(String mensajeASEJ) {
        mostrarASEJ(mensajeASEJ, Feather.ALERT_OCTAGON, Styles.DANGER, Alert.AlertType.ERROR);
    }

    public boolean confirmarASEJ(String tituloASEJ, String mensajeASEJ) {
        Alert alertaASEJ = new Alert(Alert.AlertType.CONFIRMATION, mensajeASEJ, ButtonType.CANCEL, ButtonType.OK);
        alertaASEJ.setTitle(tituloASEJ);
        alertaASEJ.setHeaderText(tituloASEJ);
        asignarVentanaASEJ(alertaASEJ);
        Optional<ButtonType> respuestaASEJ = alertaASEJ.showAndWait();
        return respuestaASEJ.isPresent() && respuestaASEJ.get() == ButtonType.OK;
    }

    public void abrirPanelASEJ(Node contenidoASEJ) {
        if (modalASEJ == null) {
            throw new IllegalStateException("No hay ventana principal para mostrar el panel lateral");
        }
        modalASEJ.setAlignment(Pos.TOP_RIGHT);
        modalASEJ.usePredefinedTransitionFactories(Side.RIGHT);
        modalASEJ.show(contenidoASEJ);
    }

    public void cerrarPanelASEJ() {
        if (modalASEJ != null) {
            modalASEJ.hide(true);
        }
    }

    private void mostrarASEJ(String mensajeASEJ, Ikon iconoASEJ, String estiloASEJ, Alert.AlertType tipoAlertaASEJ) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> mostrarASEJ(mensajeASEJ, iconoASEJ, estiloASEJ, tipoAlertaASEJ));
            return;
        }
        if (zonaToastsASEJ == null) {
            Alert alertaASEJ = new Alert(tipoAlertaASEJ, mensajeASEJ, ButtonType.OK);
            alertaASEJ.setTitle("OdontoGest");
            alertaASEJ.setHeaderText(null);
            asignarVentanaASEJ(alertaASEJ);
            alertaASEJ.showAndWait();
            return;
        }
        VBox zonaASEJ = zonaToastsASEJ;
        Notification toastASEJ = new Notification(mensajeASEJ, new FontIcon(iconoASEJ));
        toastASEJ.getStyleClass().addAll(estiloASEJ, Styles.ELEVATED_1);
        toastASEJ.setPrefHeight(Region.USE_PREF_SIZE);
        toastASEJ.setMaxHeight(Region.USE_PREF_SIZE);
        toastASEJ.setOnClose(eASEJ -> zonaASEJ.getChildren().remove(toastASEJ));
        zonaASEJ.getChildren().add(toastASEJ);

        PauseTransition esperaASEJ = new PauseTransition(DURACION_TOASTASEJ);
        esperaASEJ.setOnFinished(eASEJ -> zonaASEJ.getChildren().remove(toastASEJ));
        esperaASEJ.play();
    }

    private void asignarVentanaASEJ(Alert alertaASEJ) {
        if (raizASEJ != null && raizASEJ.getScene() != null) {
            alertaASEJ.initOwner(raizASEJ.getScene().getWindow());
        }
    }
}
