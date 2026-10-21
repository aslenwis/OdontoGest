/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.componente;

import com.odontogest.modelo.EstadoCita;
import com.odontogest.servicio.dto.CitaDto;
import com.odontogest.servicio.dto.HorarioDto;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Agenda de uno o varios días (RF-AGE-01): columnas por día, filas cada {@code intervalo} minutos, tramos fuera del
 * horario de atención sombreados y citas como bloques con el color del odontólogo.
 * <p>
 * La grilla se dibuja en un {@link Canvas}; cada cita es un {@link Label} posicionado encima que se puede elegir,
 * abrir con doble clic y arrastrar a otra hora (solo las pendientes). El componente no valida nada: informa a su
 * {@link Oyente} y el controlador llama al servicio; si el servicio rechaza el movimiento, al recargar la cita vuelve
 * a su lugar.
 */
public class AgendaSemanal extends Region {

    /** Acciones del usuario sobre la agenda. */
    public interface Oyente {
        void seleccionar(CitaDto cita);

        void abrir(CitaDto cita);

        void mover(CitaDto cita, LocalDateTime nuevoInicio);

        /** Doble clic en un espacio vacío. */
        void crear(LocalDateTime inicio);
    }

    public static final double ANCHO_HORAS = 56;
    public static final double ALTO_CABECERA = 38;
    private static final double ALTO_FILA = 24;
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final Locale ES = Locale.forLanguageTag("es-PE");

    private final Canvas canvas = new Canvas();
    private final Map<Long, Label> bloques = new HashMap<>();
    private LocalDate desde = LocalDate.now();
    private int dias = 6;
    private int intervalo = 15;
    private List<HorarioDto> horario = List.of();
    private List<CitaDto> citas = List.of();
    private LocalTime horaInicio = LocalTime.of(8, 0);
    private LocalTime horaFin = LocalTime.of(20, 0);
    private Long seleccionada;
    private boolean oscuro;
    private Oyente oyente;

    // Arrastre en curso
    private CitaDto arrastrada;
    private double arrastreDy;
    private double arrastreY0;
    private double arrastreX0;
    private boolean movio;

    public AgendaSemanal() {
        getChildren().add(canvas);
        getStyleClass().add("agenda-semanal");
        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && oyente != null) {
                LocalDateTime t = instante(e.getX(), e.getY());
                if (t != null) {
                    oyente.crear(t);
                }
            }
        });
    }

    public void setOyente(Oyente oyente) {
        this.oyente = oyente;
    }

    public void setOscuro(boolean oscuro) {
        this.oscuro = oscuro;
        dibujar();
    }

    /** Muestra {@code dias} días desde {@code desde}, con el horario del odontólogo y sus citas. */
    public void setDatos(LocalDate desde, int dias, int intervaloMin, List<HorarioDto> horario, List<CitaDto> citas) {
        this.desde = desde;
        this.dias = Math.max(1, dias);
        this.intervalo = intervaloMin;
        this.horario = List.copyOf(horario);
        this.citas = List.copyOf(citas);
        calcularRangoHorario();
        construirBloques();
        requestLayout();
        dibujar();
    }

    public void seleccionar(Long citaId) {
        seleccionada = citaId;
        bloques.forEach((id, l) -> l.setStyle(estilo(citaPorId(id), id.equals(citaId))));
    }

    /** Desplazamiento vertical (en píxeles) de una hora, para llevar la vista a la mañana o a una cita. */
    public double posicionDe(LocalTime hora) {
        return y(hora);
    }

    // ------------------------------------------------------------------ geometría

    private void calcularRangoHorario() {
        LocalTime min = LocalTime.of(8, 0);
        LocalTime max = LocalTime.of(20, 0);
        for (HorarioDto h : horario) {
            if (h.inicio().isBefore(min)) {
                min = h.inicio();
            }
            if (h.fin().isAfter(max)) {
                max = h.fin();
            }
        }
        for (CitaDto c : citas) {
            if (c.inicio().toLocalTime().isBefore(min)) {
                min = c.inicio().toLocalTime();
            }
            LocalTime fin = c.fin().toLocalDate().isAfter(c.inicio().toLocalDate()) ? LocalTime.MAX : c.fin().toLocalTime();
            if (fin.isAfter(max)) {
                max = fin;
            }
        }
        horaInicio = min.withMinute(0).withSecond(0).withNano(0);
        horaFin = max.getMinute() == 0 ? max : max.plusHours(1).withMinute(0);
        if (!horaFin.isAfter(horaInicio)) {
            horaFin = LocalTime.of(23, 0);
        }
    }

    private int filas() {
        return (int) (Duration.between(horaInicio, horaFin).toMinutes() / intervalo);
    }

    private double anchoColumna() {
        return Math.max(40, (getWidth() - ANCHO_HORAS) / dias);
    }

    private double y(LocalTime hora) {
        return ALTO_CABECERA + Duration.between(horaInicio, hora).toMinutes() / (double) intervalo * ALTO_FILA;
    }

    private double x(LocalDate dia) {
        return ANCHO_HORAS + Duration.between(desde.atStartOfDay(), dia.atStartOfDay()).toDays() * anchoColumna();
    }

    /** Fecha y hora (en la grilla) bajo el punto, o {@code null} fuera de la grilla. */
    private LocalDateTime instante(double px, double py) {
        if (px < ANCHO_HORAS || py < ALTO_CABECERA) {
            return null;
        }
        int dia = (int) ((px - ANCHO_HORAS) / anchoColumna());
        int fila = (int) ((py - ALTO_CABECERA) / ALTO_FILA);
        if (dia < 0 || dia >= dias || fila < 0 || fila >= filas()) {
            return null;
        }
        return desde.plusDays(dia).atTime(horaInicio).plusMinutes((long) fila * intervalo);
    }

    @Override
    protected double computePrefHeight(double width) {
        return ALTO_CABECERA + filas() * ALTO_FILA + 1;
    }

    @Override
    protected double computeMinHeight(double width) {
        return computePrefHeight(width);
    }

    @Override
    protected double computePrefWidth(double height) {
        return ANCHO_HORAS + dias * 140;
    }

    @Override
    protected double computeMinWidth(double height) {
        return ANCHO_HORAS + dias * 60;
    }

    @Override
    protected void layoutChildren() {
        double w = snapSizeX(getWidth());
        double h = snapSizeY(getHeight());
        if (canvas.getWidth() != w || canvas.getHeight() != h) {
            canvas.setWidth(w);
            canvas.setHeight(h);
        }
        double ancho = anchoColumna();
        for (CitaDto c : citas) {
            Label l = bloques.get(c.id());
            if (l == null) {
                continue;
            }
            LocalTime fin = c.fin().toLocalDate().isAfter(c.inicio().toLocalDate()) ? horaFin : c.fin().toLocalTime();
            double top = y(c.inicio().toLocalTime());
            double alto = Math.max(ALTO_FILA - 2, y(fin) - top - 2);
            l.resizeRelocate(x(c.inicio().toLocalDate()) + 3, top + 1, ancho - 6, alto);
        }
        dibujar();
    }

    // ------------------------------------------------------------------ dibujo

    private void dibujar() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        GraphicsContext g = canvas.getGraphicsContext2D();
        Color texto = oscuro ? Color.web("#C9D1D9") : Color.web("#24292F");
        Color tenue = oscuro ? Color.web("#8B949E") : Color.web("#57606A");
        Color linea = oscuro ? Color.web("#30363D") : Color.web("#D0D7DE");
        Color lineaSuave = oscuro ? Color.web("#21262D") : Color.web("#EAEEF2");
        Color fueraHorario = oscuro ? Color.web("#262C36") : Color.web("#EEF1F4");
        Color dentroHorario = oscuro ? Color.web("#0D1117") : Color.WHITE;
        Color hoy = oscuro ? Color.web("#1F6FEB", 0.12) : Color.web("#0969DA", 0.06);

        g.clearRect(0, 0, w, h);
        double ancho = anchoColumna();
        double yFin = ALTO_CABECERA + filas() * ALTO_FILA;

        for (int d = 0; d < dias; d++) {
            LocalDate dia = desde.plusDays(d);
            double x0 = ANCHO_HORAS + d * ancho;
            g.setFill(fueraHorario);
            g.fillRect(x0, ALTO_CABECERA, ancho, yFin - ALTO_CABECERA);
            g.setFill(dentroHorario);
            for (HorarioDto b : horario) {
                if (b.dia() == dia.getDayOfWeek()) {
                    LocalTime i = b.inicio().isBefore(horaInicio) ? horaInicio : b.inicio();
                    LocalTime f = b.fin().isAfter(horaFin) ? horaFin : b.fin();
                    if (f.isAfter(i)) {
                        g.fillRect(x0, y(i), ancho, y(f) - y(i));
                    }
                }
            }
            if (dia.equals(LocalDate.now())) {
                g.setFill(hoy);
                g.fillRect(x0, 0, ancho, yFin);
            }
            // Cabecera del día
            g.setFill(dia.equals(LocalDate.now()) ? Color.web(oscuro ? "#58A6FF" : "#0969DA") : texto);
            g.setFont(Font.font("System", FontWeight.BOLD, 12));
            g.setTextAlign(TextAlignment.CENTER);
            g.setTextBaseline(VPos.CENTER);
            String nombre = dia.getDayOfWeek().getDisplayName(TextStyle.SHORT, ES);
            g.fillText(Character.toUpperCase(nombre.charAt(0)) + nombre.substring(1) + " " + dia.getDayOfMonth() + "/"
                    + dia.getMonthValue(), x0 + ancho / 2, ALTO_CABECERA / 2);
        }

        // Líneas horizontales y horas
        g.setTextAlign(TextAlignment.RIGHT);
        g.setFont(Font.font("System", 10));
        int filas = filas();
        for (int f = 0; f <= filas; f++) {
            double yy = Math.floor(ALTO_CABECERA + f * ALTO_FILA) + 0.5;
            LocalTime t = horaInicio.plusMinutes((long) f * intervalo);
            boolean enPunto = t.getMinute() == 0;
            g.setStroke(enPunto ? linea : lineaSuave);
            g.setLineWidth(1);
            g.strokeLine(ANCHO_HORAS, yy, w, yy);
            if (enPunto && f < filas) {
                g.setFill(tenue);
                g.fillText(HORA.format(t), ANCHO_HORAS - 6, yy + 1);
            }
        }
        // Líneas verticales
        g.setStroke(linea);
        for (int d = 0; d <= dias; d++) {
            double xx = Math.floor(ANCHO_HORAS + d * ancho) + 0.5;
            g.strokeLine(xx, 0, xx, yFin);
        }
        g.strokeLine(0, ALTO_CABECERA + 0.5, w, ALTO_CABECERA + 0.5);

        // Hora actual
        LocalDateTime ahora = LocalDateTime.now();
        if (!ahora.toLocalDate().isBefore(desde) && ahora.toLocalDate().isBefore(desde.plusDays(dias))
                && !ahora.toLocalTime().isBefore(horaInicio) && ahora.toLocalTime().isBefore(horaFin)) {
            double yy = y(ahora.toLocalTime());
            double x0 = x(ahora.toLocalDate());
            g.setStroke(Color.web("#CF222E"));
            g.setLineWidth(2);
            g.strokeLine(x0, yy, x0 + ancho, yy);
        }
    }

    // ------------------------------------------------------------------ citas

    private void construirBloques() {
        getChildren().removeIf(n -> n != canvas);
        bloques.clear();
        for (CitaDto c : citas) {
            Label l = new Label((c.estado() == EstadoCita.CONFIRMADA ? "✔ " : "") + HORA.format(c.inicio()) + "  "
                    + c.pacienteNombre() + "\n" + c.motivo());
            l.setWrapText(true);
            l.setAlignment(Pos.TOP_LEFT);
            l.setPadding(new Insets(1, 6, 1, 6));
            l.setStyle(estilo(c, c.id() == (seleccionada == null ? -1 : seleccionada)));
            l.getStyleClass().add("agenda-cita");
            Tooltip.install(l, new Tooltip(HORA.format(c.inicio()) + "–" + HORA.format(c.fin()) + " · " + c.estado().etiqueta()
                    + "\n" + c.pacienteNombre() + " (" + c.numeroHistoria() + ")\n" + c.motivo()
                    + (c.planItemDescripcion() == null ? "" : "\nPlan: " + c.planItemDescripcion())));
            l.setCursor(c.estado().activa() ? Cursor.MOVE : Cursor.HAND);
            l.setOnMousePressed(e -> iniciarArrastre(c, l, e));
            l.setOnMouseDragged(e -> arrastrar(l, e));
            l.setOnMouseReleased(e -> soltar(c, l, e));
            l.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && oyente != null) {
                    oyente.abrir(c);
                }
                e.consume();
            });
            bloques.put(c.id(), l);
            getChildren().add(l);
        }
    }

    private CitaDto citaPorId(long id) {
        return citas.stream().filter(c -> c.id() == id).findFirst().orElseThrow();
    }

    private String estilo(CitaDto c, boolean elegida) {
        Color base = color(c.colorAgenda());
        double opacidad = switch (c.estado()) {
            case PROGRAMADA, CONFIRMADA -> 0.92;
            case ATENDIDA -> 0.45;
            default -> 0.3;
        };
        Color fondo = c.estado() == EstadoCita.NO_ASISTIO ? Color.gray(0.55, 0.5) : base.deriveColor(0, 1, 1, opacidad);
        String borde = elegida ? (oscuro ? "#F0F6FC" : "#1F2328") : toWeb(base.darker());
        String grosor = elegida ? "2.5" : (c.estado() == EstadoCita.CONFIRMADA ? "1.5 1.5 1.5 5" : "1");
        String textoColor = c.estado().activa() || oscuro ? "white" : "#1F2328";
        return "-fx-background-color: " + toWeb(fondo) + "; -fx-background-radius: 5; -fx-border-radius: 5;"
                + " -fx-border-color: " + borde + "; -fx-border-width: " + grosor + "; -fx-text-fill: " + textoColor
                + "; -fx-font-size: 11px;" + (c.estado() == EstadoCita.NO_ASISTIO ? " -fx-strikethrough: true;" : "");
    }

    private static Color color(String web) {
        try {
            return web == null ? Color.web("#2563EB") : Color.web(web);
        } catch (IllegalArgumentException e) {
            return Color.web("#2563EB");
        }
    }

    private static String toWeb(Color c) {
        // Locale.ROOT: con la configuración regional de Perú el decimal sería coma y el CSS no lo entendería.
        return String.format(Locale.ROOT, "rgba(%d,%d,%d,%.2f)", Math.round(c.getRed() * 255), Math.round(c.getGreen() * 255),
                Math.round(c.getBlue() * 255), c.getOpacity());
    }

    // ------------------------------------------------------------------ arrastre

    private void iniciarArrastre(CitaDto c, Label l, MouseEvent e) {
        if (e.getButton() != MouseButton.PRIMARY) {
            return;
        }
        if (oyente != null) {
            oyente.seleccionar(c);
        }
        movio = false;
        if (!c.estado().activa()) {
            arrastrada = null;
            return;
        }
        arrastrada = c;
        arrastreDy = e.getY();
        arrastreX0 = l.getLayoutX();
        arrastreY0 = l.getLayoutY();
        l.toFront();
    }

    private void arrastrar(Label l, MouseEvent e) {
        if (arrastrada == null) {
            return;
        }
        javafx.geometry.Point2D p = sceneToLocal(e.getSceneX(), e.getSceneY());
        int dia = Math.max(0, Math.min(dias - 1, (int) ((p.getX() - ANCHO_HORAS) / anchoColumna())));
        double top = p.getY() - arrastreDy;
        int fila = (int) Math.round((top - ALTO_CABECERA) / ALTO_FILA);
        fila = Math.max(0, Math.min(filas() - 1, fila));
        double nx = ANCHO_HORAS + dia * anchoColumna() + 3;
        double ny = ALTO_CABECERA + fila * ALTO_FILA + 1;
        if (nx != l.getLayoutX() || ny != l.getLayoutY()) {
            movio = true;
        }
        l.relocate(nx, ny);
        l.setOpacity(0.8);
    }

    private void soltar(CitaDto c, Label l, MouseEvent e) {
        l.setOpacity(1);
        if (arrastrada == null || !movio) {
            arrastrada = null;
            return;
        }
        arrastrada = null;
        int dia = (int) Math.round((l.getLayoutX() - 3 - ANCHO_HORAS) / anchoColumna());
        int fila = (int) Math.round((l.getLayoutY() - 1 - ALTO_CABECERA) / ALTO_FILA);
        LocalDateTime nuevo = desde.plusDays(dia).atTime(horaInicio).plusMinutes((long) fila * intervalo);
        if (nuevo.equals(c.inicio())) {
            l.relocate(arrastreX0, arrastreY0);
            return;
        }
        if (oyente != null) {
            oyente.mover(c, nuevo);
        }
    }

    /** Citas mostradas (para pruebas y para recuperar la selección tras recargar). */
    public List<CitaDto> citas() {
        return new ArrayList<>(citas);
    }
}
