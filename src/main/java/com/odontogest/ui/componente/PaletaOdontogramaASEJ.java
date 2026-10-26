package com.odontogest.ui.componente;

import com.odontogest.dominio.odontograma.ColorHallazgoASEJ;
import javafx.scene.paint.Color;

public record PaletaOdontogramaASEJ(Color azulASEJ, Color rojoASEJ, Color trazoASEJ, Color fondoASEJ, Color textoASEJ, Color acentoASEJ,
                                Color diferenciaASEJ, Color sinFondoASEJ) {

    public static PaletaOdontogramaASEJ claraASEJ() {
        return new PaletaOdontogramaASEJ(Color.web("#1D4ED8"), Color.web("#DC2626"), Color.web("#1F2937"),
                Color.WHITE, Color.web("#111827"), Color.web("#0969DA"), Color.web("#F59E0B"), Color.WHITE);
    }

    public static PaletaOdontogramaASEJ oscuraASEJ() {
        return new PaletaOdontogramaASEJ(Color.web("#60A5FA"), Color.web("#F87171"), Color.web("#C9D1D9"),
                Color.web("#0D1117"), Color.web("#E6EDF3"), Color.web("#58A6FF"), Color.web("#FBBF24"), Color.web("#161B22"));
    }

    public Color colorASEJ(ColorHallazgoASEJ cASEJ) {
        return cASEJ == ColorHallazgoASEJ.ROJO ? rojoASEJ : azulASEJ;
    }

    public Color conTransparenciaASEJ(Color cASEJ, double opacidadASEJ) {
        return new Color(cASEJ.getRed(), cASEJ.getGreen(), cASEJ.getBlue(), opacidadASEJ);
    }
}
