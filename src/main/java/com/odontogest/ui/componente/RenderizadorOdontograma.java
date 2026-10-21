/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.componente;

import com.odontogest.dominio.odontograma.Arcada;
import com.odontogest.dominio.odontograma.ColorHallazgo;
import com.odontogest.dominio.odontograma.Denticion;
import com.odontogest.dominio.odontograma.EstadoOdontograma;
import com.odontogest.dominio.odontograma.HallazgoEvento;
import com.odontogest.dominio.odontograma.PiezaDental;
import com.odontogest.dominio.odontograma.ReglaColor;
import com.odontogest.dominio.odontograma.Representacion;
import com.odontogest.dominio.odontograma.SimboloHallazgo;
import com.odontogest.dominio.odontograma.Superficie;
import com.odontogest.dominio.odontograma.TipoHallazgoDef;
import com.odontogest.dominio.odontograma.TipoPieza;
import com.odontogest.dominio.odontograma.TipoRadicular;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.geometry.VPos;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Dibuja el odontograma por capas sobre un {@link GraphicsContext} (PLAN §5.1.5), con la forma de cada hallazgo
 * según la NTS N° 188-MINSA/DGIESP-2022 (ver {@link SimboloHallazgo}):
 * <ol>
 *   <li>fondo, recuadros y números FDI;</li>
 *   <li>superficies (relleno, contorno, sellante, desgaste);</li>
 *   <li>contorno de corona y raíces (1, 2 o 3 según el tipo de pieza);</li>
 *   <li>símbolos de pieza;</li>
 *   <li>hallazgos entre piezas y de rango;</li>
 *   <li>siglas en el recuadro, del color del hallazgo;</li>
 *   <li>superposiciones: hover, selección pendiente, inicio de rango, diferencias y pieza seleccionada.</li>
 * </ol>
 * No guarda estado: el mismo dibujo sirve para la pantalla, la muestra de la paleta y la imagen del PDF.
 */
public final class RenderizadorOdontograma {

    /** Lo que la interacción agrega sobre el estado (todo opcional). */
    public record Superposicion(Integer piezaHover, Superficie superficieHover, Integer piezaSeleccionada,
                                Integer piezaPendiente, Set<Superficie> pendientes, Integer inicioRango,
                                Set<Integer> diferencias) {

        public static final Superposicion NINGUNA = new Superposicion(null, null, null, null, Set.of(), null, Set.of());

        public Superposicion {
            pendientes = pendientes == null ? Set.of() : Set.copyOf(pendientes);
            diferencias = diferencias == null ? Set.of() : Set.copyOf(diferencias);
        }
    }

    // ------------------------------------------------------------------ odontograma completo

    public void dibujar(GraphicsContext gc, double ancho, double alto, LayoutOdontograma layout,
                        EstadoOdontograma estado, PaletaOdontograma paleta, Superposicion sup) {
        double s = layout.lado();
        gc.save();
        gc.setFill(paleta.fondo());
        gc.fillRect(0, 0, ancho, alto);
        gc.setLineCap(StrokeLineCap.ROUND);

        for (CeldaPieza c : layout.celdas()) {
            if (sup.piezaHover() != null && sup.piezaHover() == c.pieza().fdi()) {
                gc.setFill(paleta.conTransparencia(paleta.acento(), 0.08));
                Rectangle2D l = expandir(c.limites(), 0.06 * s);
                gc.fillRoundRect(l.getMinX(), l.getMinY(), l.getWidth(), l.getHeight(), 0.2 * s, 0.2 * s);
            }
        }
        for (CeldaPieza c : layout.celdas()) {
            dibujarPieza(gc, c, estado, paleta, s);
        }
        for (HallazgoEvento r : estado.rangos()) {
            List<CeldaPieza> celdas = PiezaDental.rango(r.pieza(), r.piezaFin()).stream()
                    .filter(p -> layout.contiene(p.fdi())).map(p -> layout.celda(p.fdi()))
                    .sorted(Comparator.comparingDouble(c -> c.corona().getMinX())).toList();
            if (!celdas.isEmpty()) {
                dibujarRango(gc, r.tipo().simbolo(), paleta.color(r.color()), celdas.get(0), celdas.get(celdas.size() - 1), s);
            }
        }
        dibujarSuperposicion(gc, layout, paleta, sup, s);
        gc.restore();
    }

    /** Imagen fuera de pantalla para el PDF (en el hilo de UI). */
    public WritableImage imagen(int ancho, int alto, Denticion denticion, EstadoOdontograma estado, Set<Integer> diferencias) {
        Canvas canvas = new Canvas(ancho, alto);
        LayoutOdontograma layout = LayoutOdontograma.calcular(ancho, alto, denticion);
        dibujar(canvas.getGraphicsContext2D(), ancho, alto, layout, estado, PaletaOdontograma.clara(),
                new Superposicion(null, null, null, null, Set.of(), null, diferencias));
        SnapshotParameters parametros = new SnapshotParameters();
        parametros.setFill(Color.WHITE);
        return canvas.snapshot(parametros, null);
    }

    private void dibujarPieza(GraphicsContext gc, CeldaPieza c, EstadoOdontograma estado, PaletaOdontograma paleta, double s) {
        int fdi = c.pieza().fdi();
        double lw = grosor(s);
        Rectangle2D r = c.recuadro();
        gc.setStroke(paleta.trazo());
        gc.setLineWidth(lw);
        gc.strokeRect(r.getMinX(), r.getMinY(), r.getWidth(), r.getHeight());
        texto(gc, String.valueOf(fdi), c.etiqueta().getX(), c.etiqueta().getY(), 0.22 * s, false, paleta.texto());

        boolean ocultas = estado.superficiesOcultas(fdi);
        List<HallazgoEvento> visibles = estado.hallazgosVisibles(fdi);
        if (!ocultas) {
            for (int capa = 0; capa < 2; capa++) {
                for (HallazgoEvento h : visibles) {
                    if (!h.esDeSuperficie()) {
                        continue;
                    }
                    boolean relleno = h.tipo().simbolo() == SimboloHallazgo.RELLENO;
                    if ((capa == 0) == relleno) {
                        for (Superficie sup : h.superficies()) {
                            dibujarSuperficie(gc, h.tipo().simbolo(), paleta.color(h.color()), c.corona(), c.pieza(), sup, s);
                        }
                    }
                }
            }
        }
        contornoCorona(gc, c.corona(), paleta.trazo(), lw);
        raices(gc, c, paleta.trazo(), lw);
        for (HallazgoEvento h : visibles) {
            if (!h.esDeSuperficie() && !h.esDeRango()) {
                dibujarSimboloPieza(gc, h.tipo().simbolo(), paleta.color(h.color()), c, s);
            }
        }
        siglas(gc, c, visibles, paleta, s);
    }

    // ------------------------------------------------------------------ capas

    private static void contornoCorona(GraphicsContext gc, Rectangle2D c, Color color, double lw) {
        gc.setStroke(color);
        gc.setLineWidth(lw);
        gc.strokeRect(c.getMinX(), c.getMinY(), c.getWidth(), c.getHeight());
        double[][] centro = GeometriaCorona.poligono(c, GeometriaCorona.Region.CENTRO);
        gc.strokeRect(centro[0][0], centro[1][0], centro[0][1] - centro[0][0], centro[1][2] - centro[1][0]);
        gc.strokeLine(c.getMinX(), c.getMinY(), centro[0][0], centro[1][0]);
        gc.strokeLine(c.getMaxX(), c.getMinY(), centro[0][1], centro[1][1]);
        gc.strokeLine(c.getMaxX(), c.getMaxY(), centro[0][2], centro[1][2]);
        gc.strokeLine(c.getMinX(), c.getMaxY(), centro[0][3], centro[1][3]);
    }

    /** Raíces como triángulos: molares superiores 3, molares inferiores y primeros premolares superiores 2, el resto 1. */
    private static void raices(GraphicsContext gc, CeldaPieza c, Color color, double lw) {
        PiezaDental p = c.pieza();
        boolean superior = p.arcada() == Arcada.SUPERIOR;
        int n = p.tipo() == TipoPieza.MOLAR ? (superior ? 3 : 2) : p.tipoRadicular() == TipoRadicular.BIRRADICULAR ? 2 : 1;
        Rectangle2D r = c.raiz();
        double base = superior ? r.getMaxY() : r.getMinY();
        double apice = superior ? r.getMinY() : r.getMaxY();
        double margen = r.getWidth() * 0.08;
        double w = (r.getWidth() - 2 * margen) / n;
        gc.setStroke(color);
        gc.setLineWidth(lw);
        for (int i = 0; i < n; i++) {
            double x0 = r.getMinX() + margen + i * w;
            gc.strokePolyline(new double[]{x0, x0 + w / 2, x0 + w}, new double[]{base, apice, base}, 3);
        }
    }

    private static void dibujarSuperficie(GraphicsContext gc, SimboloHallazgo simbolo, Color color, Rectangle2D corona,
                                          PiezaDental pieza, Superficie sup, double s) {
        double[][] p = GeometriaCorona.poligono(corona, pieza, sup);
        gc.setStroke(color);
        gc.setFill(color);
        switch (simbolo) {
            case RELLENO -> gc.fillPolygon(p[0], p[1], 4);
            case CONTORNO_SUPERFICIE -> {
                gc.setLineWidth(grosor(s) * 2.6);
                gc.strokePolygon(p[0], p[1], 4);
            }
            case TRAZO_FOSAS -> {
                gc.setLineWidth(grosor(s) * 1.8);
                double[] cen = GeometriaCorona.centro(p);
                double d = 0.13 * s;
                gc.strokeLine(cen[0] - d, cen[1], cen[0] + d, cen[1]);
                gc.strokeLine(cen[0], cen[1] - d, cen[0], cen[1] + d);
            }
            case LINEA_DESGASTE -> {
                double[] b = GeometriaCorona.bordeExterior(corona, pieza, sup);
                gc.setLineWidth(grosor(s) * 3.2);
                gc.strokeLine(b[0], b[1], b[2], b[3]);
            }
            default -> gc.fillPolygon(p[0], p[1], 4);
        }
    }

    private static void dibujarSimboloPieza(GraphicsContext gc, SimboloHallazgo simbolo, Color color, CeldaPieza c, double s) {
        Rectangle2D co = c.corona();
        Rectangle2D ra = c.raiz();
        boolean superior = c.pieza().arcada() == Arcada.SUPERIOR;
        double dir = superior ? 1 : -1;                       // hacia el plano oclusal
        double bordeOclusal = superior ? co.getMaxY() : co.getMinY();
        double apice = superior ? ra.getMinY() : ra.getMaxY();
        double cx = co.getMinX() + co.getWidth() / 2;
        double lw = grosor(s);
        gc.setStroke(color);
        gc.setFill(color);
        gc.setLineWidth(lw * 2.4);
        switch (simbolo) {
            case ASPA -> {
                double y0 = Math.min(co.getMinY(), ra.getMinY());
                double y1 = Math.max(co.getMaxY(), ra.getMaxY());
                gc.strokeLine(co.getMinX(), y0, co.getMaxX(), y1);
                gc.strokeLine(co.getMaxX(), y0, co.getMinX(), y1);
            }
            case CUADRADO_CORONA -> {
                Rectangle2D e = expandir(co, 0.07 * s);
                gc.strokeRect(e.getMinX(), e.getMinY(), e.getWidth(), e.getHeight());
            }
            case LINEA_RAIZ -> gc.strokeLine(cx, superior ? co.getMinY() : co.getMaxY(), cx, apice + dir * 0.06 * s);
            case ESPIGO_MUNON -> {
                double l = 0.34 * s;
                double y = co.getMinY() + (s - l) / 2;
                gc.strokeRect(cx - l / 2, y, l, l);
                gc.strokeLine(cx, superior ? y : y + l, cx, apice + dir * 0.1 * s);
            }
            case PULPA_CORONAL -> {
                double l = 0.3 * s;
                gc.fillRect(cx - l / 2, co.getMinY() + (s - l) / 2, l, l);
            }
            case FLECHA_EXTRUSION -> flecha(gc, cx, bordeOclusal + dir * 0.06 * s, cx, bordeOclusal + dir * 0.36 * s, s);
            case FLECHA_INTRUSION -> flecha(gc, cx, bordeOclusal + dir * 0.36 * s, cx, bordeOclusal + dir * 0.06 * s, s);
            case FLECHA_GIRO -> {
                double y = bordeOclusal + dir * 0.1 * s;
                double xc = cx;
                double yc = bordeOclusal + dir * 0.36 * s;
                gc.beginPath();
                gc.moveTo(cx - 0.3 * s, y);
                gc.quadraticCurveTo(xc, yc, cx + 0.3 * s, y);
                gc.stroke();
                punta(gc, cx + 0.3 * s, y, cx + 0.3 * s - xc, y - yc, s);
            }
            case FLECHA_ZIGZAG -> {
                double desde = apice - dir * 0.05 * s;
                double hasta = bordeOclusal + dir * 0.12 * s;
                int pasos = 6;
                double[] xs = new double[pasos + 1];
                double[] ys = new double[pasos + 1];
                for (int i = 0; i <= pasos; i++) {
                    xs[i] = cx + (i == pasos ? 0 : (i % 2 == 0 ? -1 : 1) * 0.16 * s);
                    ys[i] = desde + (hasta - desde) * i / pasos;
                }
                gc.strokePolyline(xs, ys, xs.length);
                punta(gc, xs[pasos], ys[pasos], 0, dir, s);
            }
            case LINEA_FRACTURA -> gc.strokeLine(co.getMinX() + 0.12 * s, co.getMinY() - (superior ? 0.25 * s : 0),
                    co.getMaxX() - 0.12 * s, co.getMaxY() + (superior ? 0 : 0.25 * s));
            case TRIANGULO_RAIZ -> {
                double h = 0.32 * s;
                double y0 = apice - dir * 0.02 * s;
                gc.strokePolygon(new double[]{cx - 0.2 * s, cx + 0.2 * s, cx},
                        new double[]{y0 + dir * h, y0 + dir * h, y0}, 3);
            }
            case CIRCULO_NUMERO -> {
                Point2D e = c.etiqueta();
                gc.setLineWidth(lw * 1.8);
                gc.strokeOval(e.getX() - 0.3 * s, e.getY() - 0.17 * s, 0.6 * s, 0.34 * s);
            }
            default -> {
                // SOLO_SIGLA: solo el recuadro; los símbolos de superficie y rango se dibujan en otras capas
            }
        }
    }

    private static void dibujarRango(GraphicsContext gc, SimboloHallazgo simbolo, Color color, CeldaPieza izq,
                                     CeldaPieza der, double s) {
        boolean superior = izq.pieza().arcada() == Arcada.SUPERIOR;
        double haciaCorona = superior ? 1 : -1;
        double apice = superior ? izq.raiz().getMinY() + 0.12 * s : izq.raiz().getMaxY() - 0.12 * s;
        double x0 = izq.corona().getMinX();
        double x1 = der.corona().getMaxX();
        double c0 = izq.corona().getMinX() + s / 2;
        double c1 = der.corona().getMinX() + s / 2;
        double lw = grosor(s);
        gc.setStroke(color);
        gc.setFill(color);
        gc.setLineWidth(lw * 2.4);
        switch (simbolo) {
            case PUENTE -> {
                gc.strokeLine(c0, apice, c1, apice);
                gc.strokeLine(c0, apice, c0, apice + haciaCorona * 0.28 * s);
                gc.strokeLine(c1, apice, c1, apice + haciaCorona * 0.28 * s);
            }
            case LINEAS_PARALELAS -> {
                gc.strokeLine(x0, apice - 0.06 * s, x1, apice - 0.06 * s);
                gc.strokeLine(x0, apice + 0.06 * s, x1, apice + 0.06 * s);
            }
            case CUADROS_APICES -> {
                double l = 0.24 * s;
                for (double cx : new double[]{c0, c1}) {
                    gc.strokeRect(cx - l / 2, apice - l / 2, l, l);
                    gc.setLineWidth(lw * 1.4);
                    gc.strokeLine(cx - l / 2, apice, cx + l / 2, apice);
                    gc.strokeLine(cx, apice - l / 2, cx, apice + l / 2);
                    gc.setLineWidth(lw * 2.4);
                }
                gc.strokeLine(c0 + l / 2, apice, c1 - l / 2, apice);
            }
            case ZIGZAG_APICES -> {
                int pasos = Math.max(2, (int) Math.round((x1 - x0) / (0.18 * s)));
                double[] xs = new double[pasos + 1];
                double[] ys = new double[pasos + 1];
                for (int i = 0; i <= pasos; i++) {
                    xs[i] = x0 + (x1 - x0) * i / pasos;
                    ys[i] = apice + (i % 2 == 0 ? -1 : 1) * 0.07 * s;
                }
                gc.strokePolyline(xs, ys, xs.length);
            }
            case LINEA_CORONAS -> {
                double y = izq.corona().getMinY() + s / 2;
                gc.strokeLine(x0 - 0.05 * s, y, x1 + 0.05 * s, y);
            }
            case PARENTESIS -> {
                double gx = (izq.corona().getMaxX() + der.corona().getMinX()) / 2;
                double y0 = izq.corona().getMinY() + 0.1 * s;
                double y1 = izq.corona().getMaxY() - 0.1 * s;
                double ym = (y0 + y1) / 2;
                gc.beginPath();
                gc.moveTo(gx - 0.14 * s, y0);
                gc.quadraticCurveTo(gx + 0.14 * s, ym, gx - 0.14 * s, y1);
                gc.moveTo(gx + 0.14 * s, y0);
                gc.quadraticCurveTo(gx - 0.14 * s, ym, gx + 0.14 * s, y1);
                gc.stroke();
            }
            case CIRCULOS_NUMEROS -> {
                gc.setLineWidth(lw * 1.8);
                double r = Math.abs(der.etiqueta().getX() - izq.etiqueta().getX()) * 0.62;
                for (CeldaPieza c : List.of(izq, der)) {
                    gc.strokeOval(c.etiqueta().getX() - r, c.etiqueta().getY() - 0.2 * s, 2 * r, 0.4 * s);
                }
            }
            case CIRCULO_S -> {
                double gx = (izq.corona().getMaxX() + der.corona().getMinX()) / 2;
                double r = 0.17 * s;
                gc.setLineWidth(lw * 1.6);
                gc.strokeOval(gx - r, apice - r, 2 * r, 2 * r);
                texto(gc, "S", gx, apice, 0.2 * s, true, color);
            }
            case FLECHAS_CRUZADAS -> {
                double y = izq.etiqueta().getY();
                double xa = izq.etiqueta().getX();
                double xb = der.etiqueta().getX();
                double alto = 0.22 * s;
                gc.setLineWidth(lw * 1.6);
                gc.beginPath();
                gc.moveTo(xa, y - alto * 0.3);
                gc.quadraticCurveTo((xa + xb) / 2, y - alto * 1.6, xb, y + alto * 0.3);
                gc.stroke();
                punta(gc, xb, y + alto * 0.3, xb - (xa + xb) / 2, alto * 1.9, s * 0.8);
                gc.beginPath();
                gc.moveTo(xb, y - alto * 0.3);
                gc.quadraticCurveTo((xa + xb) / 2, y - alto * 1.6, xa, y + alto * 0.3);
                gc.stroke();
                punta(gc, xa, y + alto * 0.3, xa - (xa + xb) / 2, alto * 1.9, s * 0.8);
            }
            default -> {
                // otros símbolos no son de rango
            }
        }
    }

    /** Siglas de los hallazgos visibles, cada una de su color, ajustadas al ancho del recuadro. */
    private static void siglas(GraphicsContext gc, CeldaPieza c, List<HallazgoEvento> visibles, PaletaOdontograma paleta, double s) {
        List<String> textos = new ArrayList<>();
        List<Color> colores = new ArrayList<>();
        for (HallazgoEvento h : visibles) {
            String sigla = h.sigla() != null ? h.sigla() : h.tipo().sigla();
            if (sigla != null && !h.esDeRango() && !textos.contains(sigla)) {
                textos.add(sigla);
                colores.add(paleta.color(h.color()));
            }
        }
        if (textos.isEmpty()) {
            return;
        }
        Rectangle2D r = c.recuadro();
        double tam = 0.24 * s;
        double ancho = medir(String.join(" ", textos), tam, true);
        if (ancho > r.getWidth() * 0.92) {
            tam *= r.getWidth() * 0.92 / ancho;
            ancho = medir(String.join(" ", textos), tam, true);
        }
        double x = r.getMinX() + (r.getWidth() - ancho) / 2;
        double espacio = medir(" ", tam, true);
        for (int i = 0; i < textos.size(); i++) {
            double w = medir(textos.get(i), tam, true);
            texto(gc, textos.get(i), x + w / 2, r.getMinY() + r.getHeight() / 2, tam, true, colores.get(i));
            x += w + espacio;
        }
    }

    private static void dibujarSuperposicion(GraphicsContext gc, LayoutOdontograma layout, PaletaOdontograma paleta,
                                             Superposicion sup, double s) {
        double lw = grosor(s);
        if (sup.piezaPendiente() != null && layout.contiene(sup.piezaPendiente())) {
            CeldaPieza c = layout.celda(sup.piezaPendiente());
            for (Superficie superficie : sup.pendientes()) {
                double[][] p = GeometriaCorona.poligono(c.corona(), c.pieza(), superficie);
                gc.setFill(paleta.conTransparencia(paleta.acento(), 0.3));
                gc.fillPolygon(p[0], p[1], 4);
                gc.setStroke(paleta.acento());
                gc.setLineWidth(lw * 1.6);
                gc.setLineDashes(0.08 * s, 0.06 * s);
                gc.strokePolygon(p[0], p[1], 4);
                gc.setLineDashes((double[]) null);
            }
        }
        if (sup.piezaHover() != null && sup.superficieHover() != null && layout.contiene(sup.piezaHover())) {
            CeldaPieza c = layout.celda(sup.piezaHover());
            double[][] p = GeometriaCorona.poligono(c.corona(), c.pieza(), sup.superficieHover());
            gc.setFill(paleta.conTransparencia(paleta.acento(), 0.35));
            gc.fillPolygon(p[0], p[1], 4);
        }
        for (int fdi : sup.diferencias()) {
            if (layout.contiene(fdi)) {
                marco(gc, layout.celda(fdi), paleta.diferencia(), lw * 2.6, 0.1 * s, false, s);
            }
        }
        if (sup.inicioRango() != null && layout.contiene(sup.inicioRango())) {
            marco(gc, layout.celda(sup.inicioRango()), paleta.acento(), lw * 2, 0.06 * s, true, s);
        }
        if (sup.piezaSeleccionada() != null && layout.contiene(sup.piezaSeleccionada())) {
            marco(gc, layout.celda(sup.piezaSeleccionada()), paleta.acento(), lw * 2, 0.06 * s, false, s);
        }
    }

    // ------------------------------------------------------------------ muestra para la paleta

    /** Ícono del tipo de hallazgo para la paleta y el catálogo, dibujado en un cuadro de {@code lado} px. */
    public void muestra(GraphicsContext gc, TipoHallazgoDef tipo, double lado, PaletaOdontograma paleta) {
        gc.save();
        gc.clearRect(0, 0, lado, lado);
        Color color = tipo.reglaColor() == ReglaColor.SIEMPRE_ROJO ? paleta.rojo() : paleta.azul();
        double lw = Math.max(1, lado / 28);
        if (tipo.representacion() == Representacion.ENTRE_PIEZAS || tipo.representacion() == Representacion.RANGO_PIEZAS) {
            double s = lado * 0.4;
            CeldaPieza a = celdaMuestra(PiezaDental.de(12), lado * 0.06, lado, s);
            CeldaPieza b = celdaMuestra(PiezaDental.de(11), lado * 0.54, lado, s);
            for (CeldaPieza c : List.of(a, b)) {
                contornoCorona(gc, c.corona(), paleta.trazo(), lw * 0.7);
                raices(gc, c, paleta.trazo(), lw * 0.7);
            }
            dibujarRango(gc, tipo.simbolo(), color, a, b, s);
        } else {
            double s = lado * 0.5;
            CeldaPieza c = celdaMuestra(PiezaDental.de(16), (lado - s) / 2, lado, s);
            if (tipo.representacion() == Representacion.SUPERFICIE) {
                dibujarSuperficie(gc, tipo.simbolo(), color, c.corona(), c.pieza(), Superficie.O, s);
            }
            contornoCorona(gc, c.corona(), paleta.trazo(), lw * 0.7);
            raices(gc, c, paleta.trazo(), lw * 0.7);
            if (tipo.representacion() == Representacion.PIEZA) {
                dibujarSimboloPieza(gc, tipo.simbolo(), color, c, s);
            }
            String sigla = tipo.sigla() != null ? tipo.sigla() : tipo.siglasOpciones().isEmpty() ? null : tipo.siglasOpciones().get(0);
            if (tipo.simbolo() == SimboloHallazgo.SOLO_SIGLA && sigla != null) {
                gc.setFill(paleta.conTransparencia(paleta.sinFondo(), 0.85));
                gc.fillRect(c.corona().getMinX() - 2, c.corona().getMinY() + s * 0.2, s + 4, s * 0.6);
                texto(gc, sigla, c.corona().getMinX() + s / 2, c.corona().getMinY() + s / 2, s * 0.45, true, color);
            }
        }
        gc.restore();
    }

    /** Celda de una pieza dibujada en un cuadro pequeño: raíz arriba, corona abajo. */
    private static CeldaPieza celdaMuestra(PiezaDental pieza, double x, double lado, double s) {
        double yCorona = lado - s - lado * 0.08;
        Rectangle2D corona = new Rectangle2D(x, yCorona, s, s);
        Rectangle2D raiz = new Rectangle2D(x, yCorona - 0.6 * s, s, 0.6 * s);
        Rectangle2D recuadro = new Rectangle2D(x, 0, s, 0.01);
        return new CeldaPieza(pieza, corona, raiz, recuadro, new Point2D(x + s / 2, raiz.getMinY() - 0.12 * s));
    }

    // ------------------------------------------------------------------ primitivas

    private static double grosor(double s) {
        return Math.max(1, s * 0.025);
    }

    private static Rectangle2D expandir(Rectangle2D r, double d) {
        return new Rectangle2D(r.getMinX() - d, r.getMinY() - d, r.getWidth() + 2 * d, r.getHeight() + 2 * d);
    }

    private static void marco(GraphicsContext gc, CeldaPieza c, Color color, double lw, double margen, boolean discontinuo, double s) {
        Rectangle2D l = expandir(c.limites(), margen);
        gc.setStroke(color);
        gc.setLineWidth(lw);
        if (discontinuo) {
            gc.setLineDashes(0.1 * s, 0.07 * s);
        }
        gc.strokeRoundRect(l.getMinX(), l.getMinY(), l.getWidth(), l.getHeight(), 0.15 * s, 0.15 * s);
        gc.setLineDashes((double[]) null);
    }

    private static void flecha(GraphicsContext gc, double x0, double y0, double x1, double y1, double s) {
        gc.strokeLine(x0, y0, x1, y1);
        punta(gc, x1, y1, x1 - x0, y1 - y0, s);
    }

    /** Punta de flecha en (x, y) apuntando en la dirección (dx, dy). */
    private static void punta(GraphicsContext gc, double x, double y, double dx, double dy, double s) {
        double largo = Math.hypot(dx, dy);
        if (largo == 0) {
            return;
        }
        double ux = dx / largo;
        double uy = dy / largo;
        double t = 0.12 * s;
        gc.fillPolygon(new double[]{x, x - ux * t - uy * t * 0.6, x - ux * t + uy * t * 0.6},
                new double[]{y, y - uy * t + ux * t * 0.6, y - uy * t - ux * t * 0.6}, 3);
    }

    private static void texto(GraphicsContext gc, String texto, double cx, double cy, double tamano, boolean negrita, Color color) {
        gc.setFont(Font.font("System", negrita ? FontWeight.BOLD : FontWeight.NORMAL, Math.max(6, tamano)));
        gc.setFill(color);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText(texto, cx, cy);
    }

    private static double medir(String texto, double tamano, boolean negrita) {
        Text t = new Text(texto);
        t.setFont(Font.font("System", negrita ? FontWeight.BOLD : FontWeight.NORMAL, Math.max(6, tamano)));
        return t.getLayoutBounds().getWidth();
    }

    /** Color con que se dibujaría un hallazgo de este tipo en buen estado (para listas). */
    public static Color colorTipo(TipoHallazgoDef tipo, PaletaOdontograma paleta) {
        return paleta.color(tipo.reglaColor() == ReglaColor.SIEMPRE_ROJO ? ColorHallazgo.ROJO : ColorHallazgo.AZUL);
    }
}
