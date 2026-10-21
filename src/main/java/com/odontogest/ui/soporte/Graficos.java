/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.soporte;

import javafx.collections.ListChangeListener;
import javafx.scene.chart.BarChart;

/**
 * Ayudas para los gráficos de barras del tablero y los reportes.
 */
public final class Graficos {

    /** Ancho máximo de una barra en píxeles. */
    static final double ANCHO_MAXIMO_BARRA = 64;
    /** Espacio aproximado del eje vertical y los márgenes del gráfico. */
    private static final double MARGEN = 70;

    private Graficos() {
    }

    /**
     * Evita que con pocas categorías (p. ej. un solo odontólogo) una barra ocupe todo el ancho: ajusta el espacio
     * entre categorías cuando cambian el tamaño del gráfico o sus datos.
     */
    public static void limitarAnchoBarras(BarChart<String, Number> grafico) {
        grafico.widthProperty().addListener((obs, a, b) -> ajustar(grafico));
        grafico.getData().addListener((ListChangeListener<Object>) c -> ajustar(grafico));
        ajustar(grafico);
    }

    static void ajustar(BarChart<String, Number> grafico) {
        int series = grafico.getData().size();
        int categorias = series == 0 ? 0 : grafico.getData().get(0).getData().size();
        if (categorias == 0 || grafico.getWidth() <= 0) {
            return;
        }
        double porCategoria = Math.max(0, grafico.getWidth() - MARGEN) / categorias;
        double barras = ANCHO_MAXIMO_BARRA * series + grafico.getBarGap() * (series - 1);
        grafico.setCategoryGap(Math.max(10, porCategoria - barras));
    }
}
