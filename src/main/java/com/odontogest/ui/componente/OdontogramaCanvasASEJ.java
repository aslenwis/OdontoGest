package com.odontogest.ui.componente;

import com.odontogest.dominio.odontograma.DenticionASEJ;
import com.odontogest.dominio.odontograma.EstadoOdontogramaASEJ;
import com.odontogest.dominio.odontograma.SuperficieASEJ;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

import java.util.Optional;

public class OdontogramaCanvasASEJ extends Region {

    public interface OyenteASEJ {
        void movidoASEJ(Integer fdiASEJ, SuperficieASEJ superficieASEJ);

        void clicASEJ(Integer fdiASEJ, SuperficieASEJ superficieASEJ);
    }

    private final Canvas canvasASEJ = new Canvas();
    private final RenderizadorOdontogramaASEJ renderizadorASEJ = new RenderizadorOdontogramaASEJ();
    private DenticionASEJ denticionASEJ = DenticionASEJ.PERMANENTE;
    private EstadoOdontogramaASEJ estadoASEJ = EstadoOdontogramaASEJ.vacioASEJ();
    private PaletaOdontogramaASEJ paletaASEJ = PaletaOdontogramaASEJ.claraASEJ();
    private RenderizadorOdontogramaASEJ.SuperposicionASEJ superposicionASEJ = RenderizadorOdontogramaASEJ.SuperposicionASEJ.NINGUNAASEJ;
    private LayoutOdontogramaASEJ layoutASEJ;
    private OyenteASEJ oyenteASEJ;
    private boolean sucioASEJ = true;

    public OdontogramaCanvasASEJ() {
        getChildren().add(canvasASEJ);
        getStyleClass().add("odontograma-canvas");
        setMinSize(400, 220);
        canvasASEJ.addEventHandler(MouseEvent.MOUSE_MOVED, eASEJ -> notificarASEJ(eASEJ, false));
        canvasASEJ.addEventHandler(MouseEvent.MOUSE_EXITED, eASEJ -> {
            if (oyenteASEJ != null) {
                oyenteASEJ.movidoASEJ(null, null);
            }
        });
        canvasASEJ.addEventHandler(MouseEvent.MOUSE_CLICKED, eASEJ -> {
            if (eASEJ.getButton() == MouseButton.PRIMARY) {
                notificarASEJ(eASEJ, true);
            }
        });
        new AnimationTimer() {
            @Override
            public void handle(long ahoraASEJ) {
                if (sucioASEJ) {
                    sucioASEJ = false;
                    dibujarASEJ();
                }
            }
        }.start();
    }

    public void setOyenteASEJ(OyenteASEJ oyenteASEJ) {
        this.oyenteASEJ = oyenteASEJ;
    }

    public void setDenticionASEJ(DenticionASEJ denticionASEJ) {
        if (denticionASEJ != null && denticionASEJ != this.denticionASEJ) {
            this.denticionASEJ = denticionASEJ;
            layoutASEJ = null;
            marcarSucioASEJ();
        }
    }

    public void setEstadoASEJ(EstadoOdontogramaASEJ estadoASEJ) {
        this.estadoASEJ = estadoASEJ == null ? EstadoOdontogramaASEJ.vacioASEJ() : estadoASEJ;
        marcarSucioASEJ();
    }

    public void setPaletaASEJ(PaletaOdontogramaASEJ paletaASEJ) {
        this.paletaASEJ = paletaASEJ;
        marcarSucioASEJ();
    }

    public void setSuperposicionASEJ(RenderizadorOdontogramaASEJ.SuperposicionASEJ superposicionASEJ) {
        this.superposicionASEJ = superposicionASEJ;
        marcarSucioASEJ();
    }

    public void marcarSucioASEJ() {
        sucioASEJ = true;
    }

    @Override
    protected void layoutChildren() {
        double wASEJ = snapSizeX(getWidth());
        double hASEJ = snapSizeY(getHeight());
        if (canvasASEJ.getWidth() != wASEJ || canvasASEJ.getHeight() != hASEJ) {
            canvasASEJ.setWidth(wASEJ);
            canvasASEJ.setHeight(hASEJ);
            layoutASEJ = null;
            marcarSucioASEJ();
        }
    }

    private LayoutOdontogramaASEJ geometriaASEJ() {
        if (layoutASEJ == null) {
            layoutASEJ = LayoutOdontogramaASEJ.calcularASEJ(canvasASEJ.getWidth(), canvasASEJ.getHeight(), denticionASEJ);
        }
        return layoutASEJ;
    }

    private void dibujarASEJ() {
        if (canvasASEJ.getWidth() <= 0 || canvasASEJ.getHeight() <= 0) {
            return;
        }
        renderizadorASEJ.dibujarASEJ(canvasASEJ.getGraphicsContext2D(), canvasASEJ.getWidth(), canvasASEJ.getHeight(), geometriaASEJ(), estadoASEJ,
                paletaASEJ, superposicionASEJ);
    }

    private void notificarASEJ(MouseEvent eASEJ, boolean esClicASEJ) {
        if (oyenteASEJ == null) {
            return;
        }
        Optional<CeldaPiezaASEJ> celdaASEJ = geometriaASEJ().celdaEnASEJ(eASEJ.getX(), eASEJ.getY());
        Integer fdiASEJ = celdaASEJ.map(cASEJ -> cASEJ.piezaASEJ().fdiASEJ()).orElse(null);
        SuperficieASEJ superficieASEJ = celdaASEJ.flatMap(cASEJ -> DetectorSuperficieASEJ.detectarASEJ(eASEJ.getX(), eASEJ.getY(), cASEJ.coronaASEJ(), cASEJ.piezaASEJ()))
                .orElse(null);
        if (esClicASEJ) {
            oyenteASEJ.clicASEJ(fdiASEJ, superficieASEJ);
        } else {
            oyenteASEJ.movidoASEJ(fdiASEJ, superficieASEJ);
        }
    }
}
