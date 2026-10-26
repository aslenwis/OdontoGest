package com.odontogest.ui.soporte;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class CargadorVistasASEJ {

    public static final String TITULOASEJ = "OdontoGest";
    private static final String CSSASEJ = "/css/app.css";
    private static final String ICONOASEJ = "/img/icono.png";

    public record VistaASEJ(Parent raizASEJ, Object controladorASEJ) {
    }

    private final ApplicationContext contextASEJ;
    private final Map<String, VistaASEJ> cacheASEJ = new HashMap<>();
    private Stage stageASEJ;

    public VistaASEJ cargarASEJ(String nombreASEJ) {
        return cacheASEJ.computeIfAbsent(nombreASEJ, this::cargarNuevaASEJ);
    }

    public VistaASEJ cargarNuevaASEJ(String nombreASEJ) {
        URL recursoASEJ = Objects.requireNonNull(getClass().getResource("/view/" + nombreASEJ + ".fxml"),
                "No existe la vista " + nombreASEJ);
        FXMLLoader loaderASEJ = new FXMLLoader(recursoASEJ);
        loaderASEJ.setControllerFactory(contextASEJ::getBean);
        try {
            Parent raizASEJ = loaderASEJ.load();
            return new VistaASEJ(raizASEJ, loaderASEJ.getController());
        } catch (IOException eASEJ) {
            throw new UncheckedIOException("No se pudo cargar la vista " + nombreASEJ, eASEJ);
        }
    }

    public void limpiarCacheASEJ() {
        cacheASEJ.clear();
    }

    public void mostrarLoginASEJ(Stage ventanaASEJ) {
        this.stageASEJ = ventanaASEJ;
        limpiarCacheASEJ();
        Scene escenaASEJ = crearEscenaASEJ(cargarNuevaASEJ("login-view").raizASEJ(), 960, 620);
        stageASEJ.setMaximized(false);
        stageASEJ.setScene(escenaASEJ);
        stageASEJ.setTitle(TITULOASEJ + " - Iniciar sesión");
        stageASEJ.setMinWidth(720);
        stageASEJ.setMinHeight(520);
        ponerIconoASEJ();
        stageASEJ.centerOnScreen();
        stageASEJ.show();
    }

    public void mostrarPrincipalASEJ() {
        Scene escenaASEJ = crearEscenaASEJ(cargarASEJ("main-view").raizASEJ(), 1280, 720);
        stageASEJ.setScene(escenaASEJ);
        stageASEJ.setTitle(TITULOASEJ);
        stageASEJ.setMinWidth(1024);
        stageASEJ.setMinHeight(640);
        stageASEJ.setMaximized(true);
        stageASEJ.show();
    }

    public Stage mostrarModalASEJ(String nombreASEJ, String tituloASEJ) {
        Stage dialogoASEJ = new Stage();
        if (stageASEJ != null) {
            dialogoASEJ.initOwner(stageASEJ);
            dialogoASEJ.getIcons().setAll(stageASEJ.getIcons());
        }
        dialogoASEJ.initModality(Modality.WINDOW_MODAL);
        dialogoASEJ.setTitle(tituloASEJ);
        Scene escenaASEJ = new Scene(cargarNuevaASEJ(nombreASEJ).raizASEJ());
        escenaASEJ.getStylesheets().add(Objects.requireNonNull(getClass().getResource(CSSASEJ)).toExternalForm());
        dialogoASEJ.setScene(escenaASEJ);
        dialogoASEJ.setResizable(false);
        dialogoASEJ.show();
        return dialogoASEJ;
    }

    public Stage ventanaASEJ() {
        return stageASEJ;
    }

    private Scene crearEscenaASEJ(Parent raizASEJ, double anchoASEJ, double altoASEJ) {
        Scene escenaASEJ = new Scene(raizASEJ, anchoASEJ, altoASEJ);
        escenaASEJ.getStylesheets().add(Objects.requireNonNull(getClass().getResource(CSSASEJ)).toExternalForm());
        return escenaASEJ;
    }

    private void ponerIconoASEJ() {
        if (!stageASEJ.getIcons().isEmpty()) {
            return;
        }
        try (InputStream iconoASEJ = getClass().getResourceAsStream(ICONOASEJ)) {
            if (iconoASEJ != null) {
                stageASEJ.getIcons().add(new Image(iconoASEJ));
            }
        } catch (IOException ignoradaASEJ) {
        }
    }
}
