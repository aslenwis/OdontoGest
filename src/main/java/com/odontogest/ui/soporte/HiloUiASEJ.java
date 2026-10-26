package com.odontogest.ui.soporte;

import javafx.application.Platform;

public final class HiloUiASEJ {

    private HiloUiASEJ() {
    }

    public static void ejecutarASEJ(Runnable trabajoASEJ) {
        if (Platform.isFxApplicationThread()) {
            trabajoASEJ.run();
            return;
        }
        try {
            Platform.runLater(trabajoASEJ);
        } catch (IllegalStateException sinJavaFxASEJ) {
        }
    }
}
