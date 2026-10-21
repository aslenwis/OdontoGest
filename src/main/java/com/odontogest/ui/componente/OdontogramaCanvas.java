/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.componente;

import com.odontogest.dominio.odontograma.Denticion;
import com.odontogest.dominio.odontograma.EstadoOdontograma;
import com.odontogest.dominio.odontograma.Superficie;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

import java.util.Optional;

/**
 * Lienzo interactivo del odontograma. Recalcula la geometría al cambiar de tamaño y redibuja como máximo una vez
 * por pulso de JavaFX (bandera {@code sucio} + {@link AnimationTimer}), así el hover se mantiene fluido.
 * <p>
 * No conoce servicios: informa movimientos y clics a un {@link Oyente} (el ViewModel decide qué hacer).
 */
public class OdontogramaCanvas extends Region {

    /** Pieza y superficie bajo el mouse ({@code null} si no hay). */
    public interface Oyente {
        void movido(Integer fdi, Superficie superficie);

        void clic(Integer fdi, Superficie superficie);
    }

    private final Canvas canvas = new Canvas();
    private final RenderizadorOdontograma renderizador = new RenderizadorOdontograma();
    private Denticion denticion = Denticion.PERMANENTE;
    private EstadoOdontograma estado = EstadoOdontograma.vacio();
    private PaletaOdontograma paleta = PaletaOdontograma.clara();
    private RenderizadorOdontograma.Superposicion superposicion = RenderizadorOdontograma.Superposicion.NINGUNA;
    private LayoutOdontograma layout;
    private Oyente oyente;
    private boolean sucio = true;

    public OdontogramaCanvas() {
        getChildren().add(canvas);
        getStyleClass().add("odontograma-canvas");
        setMinSize(400, 300);
        canvas.addEventHandler(MouseEvent.MOUSE_MOVED, e -> notificar(e, false));
        canvas.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            if (oyente != null) {
                oyente.movido(null, null);
            }
        });
        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                notificar(e, true);
            }
        });
        new AnimationTimer() {
            @Override
            public void handle(long ahora) {
                if (sucio) {
                    sucio = false;
                    dibujar();
                }
            }
        }.start();
    }

    public void setOyente(Oyente oyente) {
        this.oyente = oyente;
    }

    public void setDenticion(Denticion denticion) {
        if (denticion != null && denticion != this.denticion) {
            this.denticion = denticion;
            layout = null;
            marcarSucio();
        }
    }

    public void setEstado(EstadoOdontograma estado) {
        this.estado = estado == null ? EstadoOdontograma.vacio() : estado;
        marcarSucio();
    }

    public void setPaleta(PaletaOdontograma paleta) {
        this.paleta = paleta;
        marcarSucio();
    }

    public void setSuperposicion(RenderizadorOdontograma.Superposicion superposicion) {
        this.superposicion = superposicion;
        marcarSucio();
    }

    public void marcarSucio() {
        sucio = true;
    }

    @Override
    protected void layoutChildren() {
        double w = snapSizeX(getWidth());
        double h = snapSizeY(getHeight());
        if (canvas.getWidth() != w || canvas.getHeight() != h) {
            canvas.setWidth(w);
            canvas.setHeight(h);
            layout = null;
            marcarSucio();
        }
    }

    private LayoutOdontograma geometria() {
        if (layout == null) {
            layout = LayoutOdontograma.calcular(canvas.getWidth(), canvas.getHeight(), denticion);
        }
        return layout;
    }

    private void dibujar() {
        if (canvas.getWidth() <= 0 || canvas.getHeight() <= 0) {
            return;
        }
        renderizador.dibujar(canvas.getGraphicsContext2D(), canvas.getWidth(), canvas.getHeight(), geometria(), estado,
                paleta, superposicion);
    }

    private void notificar(MouseEvent e, boolean esClic) {
        if (oyente == null) {
            return;
        }
        Optional<CeldaPieza> celda = geometria().celdaEn(e.getX(), e.getY());
        Integer fdi = celda.map(c -> c.pieza().fdi()).orElse(null);
        Superficie superficie = celda.flatMap(c -> DetectorSuperficie.detectar(e.getX(), e.getY(), c.corona(), c.pieza()))
                .orElse(null);
        if (esClic) {
            oyente.clic(fdi, superficie);
        } else {
            oyente.movido(fdi, superficie);
        }
    }
}
