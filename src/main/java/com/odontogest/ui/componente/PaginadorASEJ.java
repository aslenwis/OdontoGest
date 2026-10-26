package com.odontogest.ui.componente;

import atlantafx.base.theme.Styles;
import com.odontogest.servicio.dto.PaginaASEJ;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.function.IntConsumer;

public class PaginadorASEJ extends HBox {

    private final Label lblTotalASEJ = new Label();
    private final Label lblPaginaASEJ = new Label();
    private final Button btnAnteriorASEJ = new Button(null, new FontIcon(Feather.CHEVRON_LEFT));
    private final Button btnSiguienteASEJ = new Button(null, new FontIcon(Feather.CHEVRON_RIGHT));
    private int paginaASEJ;
    private int totalPaginasASEJ = 1;
    private IntConsumer alCambiarASEJ = pASEJ -> { };

    public PaginadorASEJ() {
        super(8);
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("paginador");
        lblTotalASEJ.getStyleClass().add(Styles.TEXT_MUTED);
        btnAnteriorASEJ.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
        btnSiguienteASEJ.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
        btnAnteriorASEJ.setOnAction(eASEJ -> irAASEJ(paginaASEJ - 1));
        btnSiguienteASEJ.setOnAction(eASEJ -> irAASEJ(paginaASEJ + 1));
        Region espacioASEJ = new Region();
        HBox.setHgrow(espacioASEJ, Priority.ALWAYS);
        getChildren().addAll(lblTotalASEJ, espacioASEJ, btnAnteriorASEJ, lblPaginaASEJ, btnSiguienteASEJ);
        actualizarBotonesASEJ();
    }

    public void setAlCambiarASEJ(IntConsumer alCambiarASEJ) {
        this.alCambiarASEJ = alCambiarASEJ;
    }

    public void actualizarASEJ(PaginaASEJ<?> pASEJ) {
        paginaASEJ = pASEJ.numeroASEJ();
        totalPaginasASEJ = pASEJ.totalPaginasASEJ();
        lblTotalASEJ.setText(pASEJ.totalASEJ() == 1 ? "1 registro" : pASEJ.totalASEJ() + " registros");
        actualizarBotonesASEJ();
    }

    public int paginaASEJ() {
        return paginaASEJ;
    }

    private void irAASEJ(int nuevaASEJ) {
        if (nuevaASEJ >= 0 && nuevaASEJ < totalPaginasASEJ) {
            alCambiarASEJ.accept(nuevaASEJ);
        }
    }

    private void actualizarBotonesASEJ() {
        lblPaginaASEJ.setText("Página " + (paginaASEJ + 1) + " de " + totalPaginasASEJ);
        btnAnteriorASEJ.setDisable(paginaASEJ <= 0);
        btnSiguienteASEJ.setDisable(paginaASEJ >= totalPaginasASEJ - 1);
    }
}
