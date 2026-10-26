package com.odontogest.ui.soporte;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;

public enum TemaASEJ {
    CLARO, OSCURO;

    private static volatile TemaASEJ actualASEJ = CLARO;

    public void aplicarASEJ() {
        Application.setUserAgentStylesheet(this == OSCURO
                ? new PrimerDark().getUserAgentStylesheet()
                : new PrimerLight().getUserAgentStylesheet());
        actualASEJ = this;
    }

    public TemaASEJ alternarASEJ() {
        return this == CLARO ? OSCURO : CLARO;
    }

    public static TemaASEJ actualASEJ() {
        return actualASEJ;
    }

    public static TemaASEJ desdeASEJ(String valorASEJ) {
        return "OSCURO".equalsIgnoreCase(valorASEJ == null ? "" : valorASEJ.trim()) ? OSCURO : CLARO;
    }
}
