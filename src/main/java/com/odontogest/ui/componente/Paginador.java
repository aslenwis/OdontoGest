/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.componente;

import atlantafx.base.theme.Styles;
import com.odontogest.servicio.dto.Pagina;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.function.IntConsumer;

/**
 * Barra de paginación: total de registros, página actual y botones anterior/siguiente.
 * Se usa desde FXML ({@code <Paginador fx:id="paginador"/>}).
 */
public class Paginador extends HBox {

    private final Label lblTotal = new Label();
    private final Label lblPagina = new Label();
    private final Button btnAnterior = new Button(null, new FontIcon(Feather.CHEVRON_LEFT));
    private final Button btnSiguiente = new Button(null, new FontIcon(Feather.CHEVRON_RIGHT));
    private int pagina;
    private int totalPaginas = 1;
    private IntConsumer alCambiar = p -> { };

    public Paginador() {
        super(8);
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("paginador");
        lblTotal.getStyleClass().add(Styles.TEXT_MUTED);
        btnAnterior.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
        btnSiguiente.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
        btnAnterior.setOnAction(e -> irA(pagina - 1));
        btnSiguiente.setOnAction(e -> irA(pagina + 1));
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        getChildren().addAll(lblTotal, espacio, btnAnterior, lblPagina, btnSiguiente);
        actualizarBotones();
    }

    /** Se llama con el número de página (0 = primera) cuando el usuario cambia de página. */
    public void setAlCambiar(IntConsumer alCambiar) {
        this.alCambiar = alCambiar;
    }

    public void actualizar(Pagina<?> p) {
        pagina = p.numero();
        totalPaginas = p.totalPaginas();
        lblTotal.setText(p.total() == 1 ? "1 registro" : p.total() + " registros");
        actualizarBotones();
    }

    public int pagina() {
        return pagina;
    }

    private void irA(int nueva) {
        if (nueva >= 0 && nueva < totalPaginas) {
            alCambiar.accept(nueva);
        }
    }

    private void actualizarBotones() {
        lblPagina.setText("Página " + (pagina + 1) + " de " + totalPaginas);
        btnAnterior.setDisable(pagina <= 0);
        btnSiguiente.setDisable(pagina >= totalPaginas - 1);
    }
}
