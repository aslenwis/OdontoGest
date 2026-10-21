/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.componente;

import com.odontogest.dominio.odontograma.ColorHallazgo;
import javafx.scene.paint.Color;

/**
 * Colores del odontograma. Los hallazgos solo usan azul y rojo (NTS 188, 5.13); el resto son colores de apoyo de
 * la interfaz (trazos, fondo, resaltado y diferencias).
 */
public record PaletaOdontograma(Color azul, Color rojo, Color trazo, Color fondo, Color texto, Color acento,
                                Color diferencia, Color sinFondo) {

    /** Tema claro; también se usa para el PDF (se imprime sobre blanco). */
    public static PaletaOdontograma clara() {
        return new PaletaOdontograma(Color.web("#1D4ED8"), Color.web("#DC2626"), Color.web("#1F2937"),
                Color.WHITE, Color.web("#111827"), Color.web("#0969DA"), Color.web("#F59E0B"), Color.WHITE);
    }

    public static PaletaOdontograma oscura() {
        return new PaletaOdontograma(Color.web("#60A5FA"), Color.web("#F87171"), Color.web("#C9D1D9"),
                Color.web("#0D1117"), Color.web("#E6EDF3"), Color.web("#58A6FF"), Color.web("#FBBF24"), Color.web("#161B22"));
    }

    public Color color(ColorHallazgo c) {
        return c == ColorHallazgo.ROJO ? rojo : azul;
    }

    public Color conTransparencia(Color c, double opacidad) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), opacidad);
    }
}
