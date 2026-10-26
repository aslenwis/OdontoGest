package com.odontogest;

import com.odontogest.config.DirectorioDatosASEJ;
import com.odontogest.config.RestauracionPendienteASEJ;
import com.odontogest.ui.PantallaCargaASEJ;
import javafx.application.Application;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Locale;

public final class LauncherASEJ {

    private LauncherASEJ() {
    }

    public static void main(String[] argsASEJ) {
        Locale.setDefault(Locale.forLanguageTag("es-PE"));
        Path homeASEJ = DirectorioDatosASEJ.inicializarASEJ();
        try {
            if (RestauracionPendienteASEJ.aplicarSiExisteASEJ(homeASEJ)) {
                System.setProperty(RestauracionPendienteASEJ.PROPIEDAD_APLICADAASEJ, "true");
            }
        } catch (RuntimeException eASEJ) {
            LoggerFactory.getLogger(LauncherASEJ.class).error("No se pudo aplicar la restauración pendiente", eASEJ);
        }
        System.setProperty("javafx.preloader", PantallaCargaASEJ.class.getName());
        Application.launch(JavaFxApplicationASEJ.class, argsASEJ);
    }
}
