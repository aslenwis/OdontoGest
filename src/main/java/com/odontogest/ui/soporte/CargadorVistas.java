/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.soporte;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
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

/**
 * Carga vistas FXML con controladores inyectados por Spring ({@code setControllerFactory(context::getBean)})
 * y gestiona las escenas de la ventana principal (login y vista principal).
 * <p>
 * Las vistas del área de contenido se guardan en caché: los controladores son singleton y cada vista
 * se carga una sola vez por sesión. Al volver al login la caché se vacía.
 */
@Component
@RequiredArgsConstructor
public class CargadorVistas {

    public static final String TITULO = "OdontoGest";
    private static final String CSS = "/css/app.css";
    private static final String ICONO = "/img/icono.png";

    /** Vista cargada: nodo raíz y su controlador. */
    public record Vista(Parent raiz, Object controlador) {
    }

    private final ApplicationContext context;
    private final Map<String, Vista> cache = new HashMap<>();
    private Stage stage;

    /** Carga (o devuelve de la caché) la vista {@code /view/<nombre>.fxml}. */
    public Vista cargar(String nombre) {
        return cache.computeIfAbsent(nombre, this::cargarNueva);
    }

    /** Carga la vista sin caché (formularios que se crean cada vez). */
    public Vista cargarNueva(String nombre) {
        URL recurso = Objects.requireNonNull(getClass().getResource("/view/" + nombre + ".fxml"),
                "No existe la vista " + nombre);
        FXMLLoader loader = new FXMLLoader(recurso);
        loader.setControllerFactory(context::getBean);
        try {
            Parent raiz = loader.load();
            return new Vista(raiz, loader.getController());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo cargar la vista " + nombre, e);
        }
    }

    public void limpiarCache() {
        cache.clear();
    }

    public void mostrarLogin(Stage ventana) {
        this.stage = ventana;
        limpiarCache();
        Scene escena = crearEscena(cargarNueva("login-view").raiz(), 960, 620);
        stage.setMaximized(false);
        stage.setScene(escena);
        stage.setTitle(TITULO + " - Iniciar sesión");
        stage.setMinWidth(720);
        stage.setMinHeight(520);
        ponerIcono();
        stage.centerOnScreen();
        stage.show();
    }

    public void mostrarPrincipal() {
        Scene escena = crearEscena(cargar("main-view").raiz(), 1280, 720);
        stage.setScene(escena);
        stage.setTitle(TITULO);
        stage.setMinWidth(1024);
        stage.setMinHeight(640);
        stage.setMaximized(true);
        stage.show();
    }

    public Stage ventana() {
        return stage;
    }

    private Scene crearEscena(Parent raiz, double ancho, double alto) {
        Scene escena = new Scene(raiz, ancho, alto);
        escena.getStylesheets().add(Objects.requireNonNull(getClass().getResource(CSS)).toExternalForm());
        return escena;
    }

    private void ponerIcono() {
        if (!stage.getIcons().isEmpty()) {
            return;
        }
        try (InputStream icono = getClass().getResourceAsStream(ICONO)) {
            if (icono != null) {
                stage.getIcons().add(new Image(icono));
            }
        } catch (IOException ignorada) {
            // Sin icono la ventana usa el de Java; no impide el uso.
        }
    }
}
