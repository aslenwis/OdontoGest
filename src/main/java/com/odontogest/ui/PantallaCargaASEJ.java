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

public class PantallaCargaASEJ extends Preloader {

    private Stage ventanaASEJ;

    @Override
    public void start(Stage stageASEJ) {
        ventanaASEJ = stageASEJ;
        FontIcon iconoASEJ = new FontIcon(Feather.ACTIVITY);
        iconoASEJ.setIconSize(40);
        iconoASEJ.setStyle("-fx-icon-color: #2563EB;");
        Label nombreASEJ = new Label("OdontoGest");
        nombreASEJ.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #1F2937;");
        Label mensajeASEJ = new Label("Iniciando… La primera vez se preparan los datos de demostración.");
        mensajeASEJ.setStyle("-fx-text-fill: #6B7280;");
        mensajeASEJ.setWrapText(true);
        ProgressBar barraASEJ = new ProgressBar(ProgressBar.INDETERMINATE_PROGRESS);
        barraASEJ.setPrefWidth(300);
        VBox raizASEJ = new VBox(12, iconoASEJ, nombreASEJ, barraASEJ, mensajeASEJ);
        raizASEJ.setAlignment(Pos.CENTER);
        raizASEJ.setPadding(new Insets(28));
        raizASEJ.setStyle("-fx-background-color: white; -fx-border-color: #D1D5DB; -fx-border-width: 1;");
        stageASEJ.initStyle(StageStyle.UNDECORATED);
        stageASEJ.setTitle("OdontoGest");
        stageASEJ.setScene(new Scene(raizASEJ, 420, 230));
        stageASEJ.centerOnScreen();
        stageASEJ.show();
    }

    @Override
    public void handleStateChangeNotification(StateChangeNotification eventoASEJ) {
        if (eventoASEJ.getType() == StateChangeNotification.Type.BEFORE_START && ventanaASEJ != null) {
            ventanaASEJ.hide();
        }
    }
}
