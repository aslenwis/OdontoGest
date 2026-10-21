/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.componente;

import javafx.animation.PauseTransition;
import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.util.Duration;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Sugerencias bajo un {@link TextField} mientras se escribe (con espera de 300 ms). No conoce servicios: la búsqueda
 * la hace quien lo crea (en segundo plano, con {@code TareaUi}) y entrega los resultados al callback.
 * <p>
 * Enter elige la primera sugerencia; Esc las oculta.
 *
 * @param <T> tipo de los elementos sugeridos
 */
public class Autocompletar<T> {

    private static final int MINIMO_CARACTERES = 2;
    private static final int MAXIMO_SUGERENCIAS = 8;

    private final TextField campo;
    private final Function<T, String> texto;
    private final Consumer<T> alElegir;
    private final ContextMenu menu = new ContextMenu();
    private final PauseTransition espera = new PauseTransition(Duration.millis(300));
    private List<T> ultimas = List.of();
    /** Evita buscar cuando el texto lo pone el propio componente al elegir. */
    private boolean eligiendo;

    /**
     * @param buscar   recibe el texto y un callback al que entregar los resultados (en el hilo de JavaFX)
     * @param texto    cómo se muestra cada elemento
     * @param alElegir se llama al elegir una sugerencia
     */
    public Autocompletar(TextField campo, BiConsumer<String, Consumer<List<T>>> buscar, Function<T, String> texto,
                         Consumer<T> alElegir) {
        this.campo = campo;
        this.texto = texto;
        this.alElegir = alElegir;
        menu.setAutoHide(true);
        espera.setOnFinished(e -> {
            String filtro = campo.getText() == null ? "" : campo.getText().trim();
            if (filtro.length() < MINIMO_CARACTERES) {
                menu.hide();
                return;
            }
            buscar.accept(filtro, resultados -> {
                if (filtro.equals(campo.getText() == null ? "" : campo.getText().trim())) {
                    mostrar(resultados);
                }
            });
        });
        campo.textProperty().addListener((obs, a, b) -> {
            if (!eligiendo) {
                espera.playFromStart();
            }
        });
        campo.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER && !ultimas.isEmpty() && menu.isShowing()) {
                elegir(ultimas.get(0));
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                menu.hide();
            }
        });
    }

    /** Pone el texto del elemento en el campo sin volver a buscar. */
    public void fijar(T elemento) {
        eligiendo = true;
        try {
            campo.setText(elemento == null ? "" : texto.apply(elemento));
            campo.positionCaret(campo.getText().length());
        } finally {
            eligiendo = false;
        }
        menu.hide();
    }

    private void mostrar(List<T> resultados) {
        ultimas = resultados.stream().limit(MAXIMO_SUGERENCIAS).toList();
        menu.getItems().clear();
        if (ultimas.isEmpty()) {
            Label vacio = new Label("Sin coincidencias");
            vacio.getStyleClass().add("text-muted");
            CustomMenuItem item = new CustomMenuItem(vacio, true);
            item.setDisable(true);
            menu.getItems().add(item);
        }
        for (T t : ultimas) {
            Label l = new Label(texto.apply(t));
            l.setPrefWidth(Math.max(campo.getWidth() - 24, 200));
            CustomMenuItem item = new CustomMenuItem(l, true);
            item.setOnAction(e -> elegir(t));
            menu.getItems().add(item);
        }
        if (campo.getScene() != null && campo.isFocused()) {
            menu.show(campo, Side.BOTTOM, 0, 2);
        }
    }

    private void elegir(T elemento) {
        fijar(elemento);
        alElegir.accept(elemento);
    }
}
