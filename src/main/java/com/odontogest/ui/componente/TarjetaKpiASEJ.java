package com.odontogest.ui.componente;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

public class TarjetaKpiASEJ extends VBox {

    private final Label lblTituloASEJ = new Label();
    private final FontIcon iconoASEJ = new FontIcon();
    private final Label lblValorASEJ = new Label("—");
    private final Label lblDetalleASEJ = new Label();

    public TarjetaKpiASEJ() {
        getStyleClass().add("kpi");
        lblTituloASEJ.getStyleClass().add("kpi-titulo");
        iconoASEJ.getStyleClass().add("kpi-icono");
        lblValorASEJ.getStyleClass().add("kpi-valor");
        lblDetalleASEJ.getStyleClass().add("kpi-detalle");
        lblDetalleASEJ.setWrapText(true);
        Region espacioASEJ = new Region();
        HBox.setHgrow(espacioASEJ, Priority.ALWAYS);
        HBox cabeceraASEJ = new HBox(8, lblTituloASEJ, espacioASEJ, iconoASEJ);
        cabeceraASEJ.setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(cabeceraASEJ, lblValorASEJ, lblDetalleASEJ);
    }

    public String getTituloASEJ() {
        return lblTituloASEJ.getText();
    }

    public void setTituloASEJ(String tituloASEJ) {
        lblTituloASEJ.setText(tituloASEJ);
    }

    public String getIconoASEJ() {
        return iconoASEJ.getIconLiteral();
    }

    public void setIconoASEJ(String literalASEJ) {
        iconoASEJ.setIconLiteral(literalASEJ);
    }

    public void mostrarASEJ(String valorASEJ, String detalleASEJ, boolean peligroASEJ) {
        lblValorASEJ.setText(valorASEJ);
        lblDetalleASEJ.setText(detalleASEJ);
        getStyleClass().remove("kpi-peligro");
        if (peligroASEJ) {
            getStyleClass().add("kpi-peligro");
        }
    }

    public String getValorASEJ() {
        return lblValorASEJ.getText();
    }

    public void setAlHacerClicASEJ(Runnable accionASEJ) {
        getStyleClass().remove("kpi-clic");
        if (accionASEJ == null) {
            setOnMouseClicked(null);
            return;
        }
        getStyleClass().add("kpi-clic");
        setOnMouseClicked(eASEJ -> accionASEJ.run());
    }
}
