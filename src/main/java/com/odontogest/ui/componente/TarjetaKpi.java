/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.componente;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Tarjeta de indicador del tablero de inicio: título con icono, valor grande y una línea de detalle. Si se le asigna
 * una acción ({@link #setAlHacerClic}), se resalta al pasar el mouse y la ejecuta con un clic.
 * <p>
 * Uso en FXML: {@code <TarjetaKpi fx:id="kpiCitas" titulo="Citas de hoy" icono="fth-calendar"/>}.
 */
public class TarjetaKpi extends VBox {

    private final Label lblTitulo = new Label();
    private final FontIcon icono = new FontIcon();
    private final Label lblValor = new Label("—");
    private final Label lblDetalle = new Label();

    public TarjetaKpi() {
        getStyleClass().add("kpi");
        lblTitulo.getStyleClass().add("kpi-titulo");
        icono.getStyleClass().add("kpi-icono");
        lblValor.getStyleClass().add("kpi-valor");
        lblDetalle.getStyleClass().add("kpi-detalle");
        lblDetalle.setWrapText(true);
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox cabecera = new HBox(8, lblTitulo, espacio, icono);
        cabecera.setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(cabecera, lblValor, lblDetalle);
    }

    public String getTitulo() {
        return lblTitulo.getText();
    }

    public void setTitulo(String titulo) {
        lblTitulo.setText(titulo);
    }

    public String getIcono() {
        return icono.getIconLiteral();
    }

    /** Literal de Ikonli, p. ej. {@code fth-calendar}. */
    public void setIcono(String literal) {
        icono.setIconLiteral(literal);
    }

    /**
     * @param peligro resalta el valor en rojo (p. ej. cuotas vencidas mayores que cero)
     */
    public void mostrar(String valor, String detalle, boolean peligro) {
        lblValor.setText(valor);
        lblDetalle.setText(detalle);
        getStyleClass().remove("kpi-peligro");
        if (peligro) {
            getStyleClass().add("kpi-peligro");
        }
    }

    public String getValor() {
        return lblValor.getText();
    }

    public void setAlHacerClic(Runnable accion) {
        getStyleClass().remove("kpi-clic");
        if (accion == null) {
            setOnMouseClicked(null);
            return;
        }
        getStyleClass().add("kpi-clic");
        setOnMouseClicked(e -> accion.run());
    }
}
