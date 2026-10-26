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

public class AutocompletarASEJ<T> {

    private static final int MINIMO_CARACTERESASEJ = 2;
    private static final int MAXIMO_SUGERENCIASASEJ = 8;

    private final TextField campoASEJ;
    private final Function<T, String> textoASEJ;
    private final Consumer<T> alElegirASEJ;
    private final ContextMenu menuASEJ = new ContextMenu();
    private final PauseTransition esperaASEJ = new PauseTransition(Duration.millis(300));
    private List<T> ultimasASEJ = List.of();
    private boolean eligiendoASEJ;

    public AutocompletarASEJ(TextField campoASEJ, BiConsumer<String, Consumer<List<T>>> buscarASEJ, Function<T, String> textoASEJ,
                         Consumer<T> alElegirASEJ) {
        this.campoASEJ = campoASEJ;
        this.textoASEJ = textoASEJ;
        this.alElegirASEJ = alElegirASEJ;
        menuASEJ.setAutoHide(true);
        esperaASEJ.setOnFinished(eASEJ -> {
            String filtroASEJ = campoASEJ.getText() == null ? "" : campoASEJ.getText().trim();
            if (filtroASEJ.length() < MINIMO_CARACTERESASEJ) {
                menuASEJ.hide();
                return;
            }
            buscarASEJ.accept(filtroASEJ, resultadosASEJ -> {
                if (filtroASEJ.equals(campoASEJ.getText() == null ? "" : campoASEJ.getText().trim())) {
                    mostrarASEJ(resultadosASEJ);
                }
            });
        });
        campoASEJ.textProperty().addListener((obsASEJ, aASEJ, bASEJ) -> {
            if (!eligiendoASEJ) {
                esperaASEJ.playFromStart();
            }
        });
        campoASEJ.setOnKeyPressed(eASEJ -> {
            if (eASEJ.getCode() == KeyCode.ENTER && !ultimasASEJ.isEmpty() && menuASEJ.isShowing()) {
                elegirASEJ(ultimasASEJ.get(0));
                eASEJ.consume();
            } else if (eASEJ.getCode() == KeyCode.ESCAPE) {
                menuASEJ.hide();
            }
        });
    }

    public void fijarASEJ(T elementoASEJ) {
        eligiendoASEJ = true;
        try {
            campoASEJ.setText(elementoASEJ == null ? "" : textoASEJ.apply(elementoASEJ));
            campoASEJ.positionCaret(campoASEJ.getText().length());
        } finally {
            eligiendoASEJ = false;
        }
        menuASEJ.hide();
    }

    private void mostrarASEJ(List<T> resultadosASEJ) {
        ultimasASEJ = resultadosASEJ.stream().limit(MAXIMO_SUGERENCIASASEJ).toList();
        menuASEJ.getItems().clear();
        if (ultimasASEJ.isEmpty()) {
            Label vacioASEJ = new Label("Sin coincidencias");
            vacioASEJ.getStyleClass().add("text-muted");
            CustomMenuItem itemASEJ = new CustomMenuItem(vacioASEJ, true);
            itemASEJ.setDisable(true);
            menuASEJ.getItems().add(itemASEJ);
        }
        for (T tASEJ : ultimasASEJ) {
            Label lASEJ = new Label(textoASEJ.apply(tASEJ));
            lASEJ.setPrefWidth(Math.max(campoASEJ.getWidth() - 24, 200));
            CustomMenuItem itemASEJ = new CustomMenuItem(lASEJ, true);
            itemASEJ.setOnAction(eASEJ -> elegirASEJ(tASEJ));
            menuASEJ.getItems().add(itemASEJ);
        }
        if (campoASEJ.getScene() != null && campoASEJ.isFocused()) {
            menuASEJ.show(campoASEJ, Side.BOTTOM, 0, 2);
        }
    }

    private void elegirASEJ(T elementoASEJ) {
        fijarASEJ(elementoASEJ);
        alElegirASEJ.accept(elementoASEJ);
    }
}
