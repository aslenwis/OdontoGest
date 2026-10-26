package com.odontogest.ui.soporte;

import javafx.collections.ListChangeListener;
import javafx.scene.chart.BarChart;

public final class GraficosASEJ {

    static final double ANCHO_MAXIMO_BARRAASEJ = 64;
    private static final double MARGENASEJ = 70;

    private GraficosASEJ() {
    }

    public static void limitarAnchoBarrasASEJ(BarChart<String, Number> graficoASEJ) {
        graficoASEJ.widthProperty().addListener((obsASEJ, aASEJ, bASEJ) -> ajustarASEJ(graficoASEJ));
        graficoASEJ.getData().addListener((ListChangeListener<Object>) cASEJ -> ajustarASEJ(graficoASEJ));
        ajustarASEJ(graficoASEJ);
    }

    static void ajustarASEJ(BarChart<String, Number> graficoASEJ) {
        int seriesASEJ = graficoASEJ.getData().size();
        int categoriasASEJ = seriesASEJ == 0 ? 0 : graficoASEJ.getData().get(0).getData().size();
        if (categoriasASEJ == 0 || graficoASEJ.getWidth() <= 0) {
            return;
        }
        double porCategoriaASEJ = Math.max(0, graficoASEJ.getWidth() - MARGENASEJ) / categoriasASEJ;
        double barrasASEJ = ANCHO_MAXIMO_BARRAASEJ * seriesASEJ + graficoASEJ.getBarGap() * (seriesASEJ - 1);
        graficoASEJ.setCategoryGap(Math.max(10, porCategoriaASEJ - barrasASEJ));
    }
}
