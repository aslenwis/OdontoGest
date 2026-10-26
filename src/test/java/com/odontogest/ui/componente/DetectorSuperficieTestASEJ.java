package com.odontogest.ui.componente;

import com.odontogest.dominio.odontograma.PiezaDentalASEJ;
import com.odontogest.dominio.odontograma.SuperficieASEJ;
import javafx.geometry.Rectangle2D;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DetectorSuperficieTestASEJ {

    private static final Rectangle2D CORONAASEJ = new Rectangle2D(100, 50, 100, 100);

    private static Optional<SuperficieASEJ> clicASEJ(double xASEJ, double yASEJ, int fdiASEJ) {
        return DetectorSuperficieASEJ.detectarASEJ(xASEJ, yASEJ, CORONAASEJ, PiezaDentalASEJ.deASEJ(fdiASEJ));
    }

    @ParameterizedTest
    @CsvSource({"16", "21", "36", "48", "55", "61", "73", "84"})
    void centroEsOclusalASEJ(int fdiASEJ) {
        assertThat(clicASEJ(150, 100, fdiASEJ)).contains(SuperficieASEJ.O);
        assertThat(clicASEJ(170, 120, fdiASEJ)).as("borde del cuadrado central").contains(SuperficieASEJ.O);
        assertThat(clicASEJ(130, 80, fdiASEJ)).contains(SuperficieASEJ.O);
    }

    @ParameterizedTest
    @CsvSource({
            "16, V, L, D, M",
            "21, V, L, M, D",
            "36, L, V, M, D",
            "46, L, V, D, M",
            "54, V, L, D, M",
            "63, V, L, M, D",
            "75, L, V, M, D",
            "82, L, V, D, M"
    })
    void cuatroRegionesPorCuadranteASEJ(int fdiASEJ, SuperficieASEJ arribaASEJ, SuperficieASEJ abajoASEJ, SuperficieASEJ izquierdaASEJ, SuperficieASEJ derechaASEJ) {
        assertThat(clicASEJ(150, 55, fdiASEJ)).as("arriba").contains(arribaASEJ);
        assertThat(clicASEJ(150, 145, fdiASEJ)).as("abajo").contains(abajoASEJ);
        assertThat(clicASEJ(105, 100, fdiASEJ)).as("izquierda").contains(izquierdaASEJ);
        assertThat(clicASEJ(195, 100, fdiASEJ)).as("derecha").contains(derechaASEJ);
    }

    @Test
    void enLasDiagonalesExactasGanaLaRegionVerticalASEJ() {
        assertThat(clicASEJ(110, 60, 16)).as("|dx| = |dy| arriba-izquierda").contains(SuperficieASEJ.V);
        assertThat(clicASEJ(190, 140, 16)).as("abajo-derecha").contains(SuperficieASEJ.L);
        assertThat(clicASEJ(110, 140, 36)).as("abajo-izquierda en inferior").contains(SuperficieASEJ.V);
        assertThat(clicASEJ(190, 60, 36)).contains(SuperficieASEJ.L);
    }

    @Test
    void justoFueraDeLaDiagonalEsRegionHorizontalASEJ() {
        assertThat(clicASEJ(109, 60.5, 16)).contains(SuperficieASEJ.D);
        assertThat(clicASEJ(191, 60.5, 21)).contains(SuperficieASEJ.D);
    }

    @Test
    void bordesExactosDeLaCoronaCuentanComoDentroASEJ() {
        assertThat(clicASEJ(100, 100, 16)).contains(SuperficieASEJ.D);
        assertThat(clicASEJ(200, 100, 16)).contains(SuperficieASEJ.M);
        assertThat(clicASEJ(150, 50, 16)).contains(SuperficieASEJ.V);
        assertThat(clicASEJ(150, 150, 16)).contains(SuperficieASEJ.L);
        assertThat(clicASEJ(100, 50, 16)).as("esquina").contains(SuperficieASEJ.V);
    }

    @Test
    void fueraDeLaCoronaNoHaySuperficieASEJ() {
        assertThat(clicASEJ(99.9, 100, 16)).isEmpty();
        assertThat(clicASEJ(200.1, 100, 16)).isEmpty();
        assertThat(clicASEJ(150, 49.9, 16)).isEmpty();
        assertThat(clicASEJ(150, 150.1, 16)).isEmpty();
        assertThat(clicASEJ(0, 0, 16)).isEmpty();
    }
}
