package com.odontogest.ui.componente;

import com.odontogest.modelo.EstadoCitaASEJ;
import com.odontogest.servicio.dto.CitaDtoASEJ;
import com.odontogest.servicio.dto.HorarioDtoASEJ;
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

public class AgendaSemanalASEJ extends Region {

    public interface OyenteASEJ {
        void seleccionarASEJ(CitaDtoASEJ citaASEJ);

        void abrirASEJ(CitaDtoASEJ citaASEJ);

        void moverASEJ(CitaDtoASEJ citaASEJ, LocalDateTime nuevoInicioASEJ);

        void crearASEJ(LocalDateTime inicioASEJ);
    }

    public static final double ANCHO_HORASASEJ = 56;
    public static final double ALTO_CABECERAASEJ = 38;
    private static final double ALTO_FILAASEJ = 24;
    private static final DateTimeFormatter HORAASEJ = DateTimeFormatter.ofPattern("HH:mm");
    private static final Locale ESASEJ = Locale.forLanguageTag("es-PE");

    private final Canvas canvasASEJ = new Canvas();
    private final Map<Long, Label> bloquesASEJ = new HashMap<>();
    private LocalDate desdeASEJ = LocalDate.now();
    private int diasASEJ = 6;
    private int intervaloASEJ = 15;
    private List<HorarioDtoASEJ> horarioASEJ = List.of();
    private List<CitaDtoASEJ> citasASEJ = List.of();
    private LocalTime horaInicioASEJ = LocalTime.of(8, 0);
    private LocalTime horaFinASEJ = LocalTime.of(20, 0);
    private Long seleccionadaASEJ;
    private boolean oscuroASEJ;
    private OyenteASEJ oyenteASEJ;

    private CitaDtoASEJ arrastradaASEJ;
    private double arrastreDyASEJ;
    private double arrastreY0ASEJ;
    private double arrastreX0ASEJ;
    private boolean movioASEJ;

    public AgendaSemanalASEJ() {
        getChildren().add(canvasASEJ);
        getStyleClass().add("agenda-semanal");
        canvasASEJ.addEventHandler(MouseEvent.MOUSE_CLICKED, eASEJ -> {
            if (eASEJ.getButton() == MouseButton.PRIMARY && eASEJ.getClickCount() == 2 && oyenteASEJ != null) {
                LocalDateTime tASEJ = instanteASEJ(eASEJ.getX(), eASEJ.getY());
                if (tASEJ != null) {
                    oyenteASEJ.crearASEJ(tASEJ);
                }
            }
        });
    }

    public void setOyenteASEJ(OyenteASEJ oyenteASEJ) {
        this.oyenteASEJ = oyenteASEJ;
    }

    public void setOscuroASEJ(boolean oscuroASEJ) {
        this.oscuroASEJ = oscuroASEJ;
        dibujarASEJ();
    }

    public void setDatosASEJ(LocalDate desdeASEJ, int diasASEJ, int intervaloMinASEJ, List<HorarioDtoASEJ> horarioASEJ, List<CitaDtoASEJ> citasASEJ) {
        this.desdeASEJ = desdeASEJ;
        this.diasASEJ = Math.max(1, diasASEJ);
        this.intervaloASEJ = intervaloMinASEJ;
        this.horarioASEJ = List.copyOf(horarioASEJ);
        this.citasASEJ = List.copyOf(citasASEJ);
        calcularRangoHorarioASEJ();
        construirBloquesASEJ();
        requestLayout();
        dibujarASEJ();
    }

    public void seleccionarASEJ(Long citaIdASEJ) {
        seleccionadaASEJ = citaIdASEJ;
        bloquesASEJ.forEach((idASEJ, lASEJ) -> lASEJ.setStyle(estiloASEJ(citaPorIdASEJ(idASEJ), idASEJ.equals(citaIdASEJ))));
    }

    public double posicionDeASEJ(LocalTime horaASEJ) {
        return yASEJ(horaASEJ);
    }

    private void calcularRangoHorarioASEJ() {
        LocalTime minASEJ = LocalTime.of(8, 0);
        LocalTime maxASEJ = LocalTime.of(20, 0);
        for (HorarioDtoASEJ hASEJ : horarioASEJ) {
            if (hASEJ.inicioASEJ().isBefore(minASEJ)) {
                minASEJ = hASEJ.inicioASEJ();
            }
            if (hASEJ.finASEJ().isAfter(maxASEJ)) {
                maxASEJ = hASEJ.finASEJ();
            }
        }
        for (CitaDtoASEJ cASEJ : citasASEJ) {
            if (cASEJ.inicioASEJ().toLocalTime().isBefore(minASEJ)) {
                minASEJ = cASEJ.inicioASEJ().toLocalTime();
            }
            LocalTime finASEJ = cASEJ.finASEJ().toLocalDate().isAfter(cASEJ.inicioASEJ().toLocalDate()) ? LocalTime.MAX : cASEJ.finASEJ().toLocalTime();
            if (finASEJ.isAfter(maxASEJ)) {
                maxASEJ = finASEJ;
            }
        }
        horaInicioASEJ = minASEJ.withMinute(0).withSecond(0).withNano(0);
        horaFinASEJ = maxASEJ.getMinute() == 0 ? maxASEJ : maxASEJ.plusHours(1).withMinute(0);
        if (!horaFinASEJ.isAfter(horaInicioASEJ)) {
            horaFinASEJ = LocalTime.of(23, 0);
        }
    }

    private int filasASEJ() {
        return (int) (Duration.between(horaInicioASEJ, horaFinASEJ).toMinutes() / intervaloASEJ);
    }

    private double anchoColumnaASEJ() {
        return Math.max(40, (getWidth() - ANCHO_HORASASEJ) / diasASEJ);
    }

    private double yASEJ(LocalTime horaASEJ) {
        return ALTO_CABECERAASEJ + Duration.between(horaInicioASEJ, horaASEJ).toMinutes() / (double) intervaloASEJ * ALTO_FILAASEJ;
    }

    private double xASEJ(LocalDate diaASEJ) {
        return ANCHO_HORASASEJ + Duration.between(desdeASEJ.atStartOfDay(), diaASEJ.atStartOfDay()).toDays() * anchoColumnaASEJ();
    }

    private LocalDateTime instanteASEJ(double pxASEJ, double pyASEJ) {
        if (pxASEJ < ANCHO_HORASASEJ || pyASEJ < ALTO_CABECERAASEJ) {
            return null;
        }
        int diaASEJ = (int) ((pxASEJ - ANCHO_HORASASEJ) / anchoColumnaASEJ());
        int filaASEJ = (int) ((pyASEJ - ALTO_CABECERAASEJ) / ALTO_FILAASEJ);
        if (diaASEJ < 0 || diaASEJ >= diasASEJ || filaASEJ < 0 || filaASEJ >= filasASEJ()) {
            return null;
        }
        return desdeASEJ.plusDays(diaASEJ).atTime(horaInicioASEJ).plusMinutes((long) filaASEJ * intervaloASEJ);
    }

    @Override
    protected double computePrefHeight(double widthASEJ) {
        return ALTO_CABECERAASEJ + filasASEJ() * ALTO_FILAASEJ + 1;
    }

    @Override
    protected double computeMinHeight(double widthASEJ) {
        return computePrefHeight(widthASEJ);
    }

    @Override
    protected double computePrefWidth(double heightASEJ) {
        return ANCHO_HORASASEJ + diasASEJ * 140;
    }

    @Override
    protected double computeMinWidth(double heightASEJ) {
        return ANCHO_HORASASEJ + diasASEJ * 60;
    }

    @Override
    protected void layoutChildren() {
        double wASEJ = snapSizeX(getWidth());
        double hASEJ = snapSizeY(getHeight());
        if (canvasASEJ.getWidth() != wASEJ || canvasASEJ.getHeight() != hASEJ) {
            canvasASEJ.setWidth(wASEJ);
            canvasASEJ.setHeight(hASEJ);
        }
        double anchoASEJ = anchoColumnaASEJ();
        for (CitaDtoASEJ cASEJ : citasASEJ) {
            Label lASEJ = bloquesASEJ.get(cASEJ.idASEJ());
            if (lASEJ == null) {
                continue;
            }
            LocalTime finASEJ = cASEJ.finASEJ().toLocalDate().isAfter(cASEJ.inicioASEJ().toLocalDate()) ? horaFinASEJ : cASEJ.finASEJ().toLocalTime();
            double topASEJ = yASEJ(cASEJ.inicioASEJ().toLocalTime());
            double altoASEJ = Math.max(ALTO_FILAASEJ - 2, yASEJ(finASEJ) - topASEJ - 2);
            lASEJ.resizeRelocate(xASEJ(cASEJ.inicioASEJ().toLocalDate()) + 3, topASEJ + 1, anchoASEJ - 6, altoASEJ);
        }
        dibujarASEJ();
    }

    private void dibujarASEJ() {
        double wASEJ = canvasASEJ.getWidth();
        double hASEJ = canvasASEJ.getHeight();
        if (wASEJ <= 0 || hASEJ <= 0) {
            return;
        }
        GraphicsContext gASEJ = canvasASEJ.getGraphicsContext2D();
        Color textoASEJ = oscuroASEJ ? Color.web("#C9D1D9") : Color.web("#24292F");
        Color tenueASEJ = oscuroASEJ ? Color.web("#8B949E") : Color.web("#57606A");
        Color lineaASEJ = oscuroASEJ ? Color.web("#30363D") : Color.web("#D0D7DE");
        Color lineaSuaveASEJ = oscuroASEJ ? Color.web("#21262D") : Color.web("#EAEEF2");
        Color fueraHorarioASEJ = oscuroASEJ ? Color.web("#262C36") : Color.web("#EEF1F4");
        Color dentroHorarioASEJ = oscuroASEJ ? Color.web("#0D1117") : Color.WHITE;
        Color hoyASEJ = oscuroASEJ ? Color.web("#1F6FEB", 0.12) : Color.web("#0969DA", 0.06);

        gASEJ.clearRect(0, 0, wASEJ, hASEJ);
        double anchoASEJ = anchoColumnaASEJ();
        double yFinASEJ = ALTO_CABECERAASEJ + filasASEJ() * ALTO_FILAASEJ;

        for (int dASEJ = 0; dASEJ < diasASEJ; dASEJ++) {
            LocalDate diaASEJ = desdeASEJ.plusDays(dASEJ);
            double x0ASEJ = ANCHO_HORASASEJ + dASEJ * anchoASEJ;
            gASEJ.setFill(fueraHorarioASEJ);
            gASEJ.fillRect(x0ASEJ, ALTO_CABECERAASEJ, anchoASEJ, yFinASEJ - ALTO_CABECERAASEJ);
            gASEJ.setFill(dentroHorarioASEJ);
            for (HorarioDtoASEJ bASEJ : horarioASEJ) {
                if (bASEJ.diaASEJ() == diaASEJ.getDayOfWeek()) {
                    LocalTime iASEJ = bASEJ.inicioASEJ().isBefore(horaInicioASEJ) ? horaInicioASEJ : bASEJ.inicioASEJ();
                    LocalTime fASEJ = bASEJ.finASEJ().isAfter(horaFinASEJ) ? horaFinASEJ : bASEJ.finASEJ();
                    if (fASEJ.isAfter(iASEJ)) {
                        gASEJ.fillRect(x0ASEJ, yASEJ(iASEJ), anchoASEJ, yASEJ(fASEJ) - yASEJ(iASEJ));
                    }
                }
            }
            if (diaASEJ.equals(LocalDate.now())) {
                gASEJ.setFill(hoyASEJ);
                gASEJ.fillRect(x0ASEJ, 0, anchoASEJ, yFinASEJ);
            }
            gASEJ.setFill(diaASEJ.equals(LocalDate.now()) ? Color.web(oscuroASEJ ? "#58A6FF" : "#0969DA") : textoASEJ);
            gASEJ.setFont(Font.font("System", FontWeight.BOLD, 12));
            gASEJ.setTextAlign(TextAlignment.CENTER);
            gASEJ.setTextBaseline(VPos.CENTER);
            String nombreASEJ = diaASEJ.getDayOfWeek().getDisplayName(TextStyle.SHORT, ESASEJ);
            gASEJ.fillText(Character.toUpperCase(nombreASEJ.charAt(0)) + nombreASEJ.substring(1) + " " + diaASEJ.getDayOfMonth() + "/"
                    + diaASEJ.getMonthValue(), x0ASEJ + anchoASEJ / 2, ALTO_CABECERAASEJ / 2);
        }

        gASEJ.setTextAlign(TextAlignment.RIGHT);
        gASEJ.setFont(Font.font("System", 10));
        int filasASEJ = filasASEJ();
        for (int fASEJ = 0; fASEJ <= filasASEJ; fASEJ++) {
            double yyASEJ = Math.floor(ALTO_CABECERAASEJ + fASEJ * ALTO_FILAASEJ) + 0.5;
            LocalTime tASEJ = horaInicioASEJ.plusMinutes((long) fASEJ * intervaloASEJ);
            boolean enPuntoASEJ = tASEJ.getMinute() == 0;
            gASEJ.setStroke(enPuntoASEJ ? lineaASEJ : lineaSuaveASEJ);
            gASEJ.setLineWidth(1);
            gASEJ.strokeLine(ANCHO_HORASASEJ, yyASEJ, wASEJ, yyASEJ);
            if (enPuntoASEJ && fASEJ < filasASEJ) {
                gASEJ.setFill(tenueASEJ);
                gASEJ.fillText(HORAASEJ.format(tASEJ), ANCHO_HORASASEJ - 6, yyASEJ + 1);
            }
        }
        gASEJ.setStroke(lineaASEJ);
        for (int dASEJ = 0; dASEJ <= diasASEJ; dASEJ++) {
            double xxASEJ = Math.floor(ANCHO_HORASASEJ + dASEJ * anchoASEJ) + 0.5;
            gASEJ.strokeLine(xxASEJ, 0, xxASEJ, yFinASEJ);
        }
        gASEJ.strokeLine(0, ALTO_CABECERAASEJ + 0.5, wASEJ, ALTO_CABECERAASEJ + 0.5);

        LocalDateTime ahoraASEJ = LocalDateTime.now();
        if (!ahoraASEJ.toLocalDate().isBefore(desdeASEJ) && ahoraASEJ.toLocalDate().isBefore(desdeASEJ.plusDays(diasASEJ))
                && !ahoraASEJ.toLocalTime().isBefore(horaInicioASEJ) && ahoraASEJ.toLocalTime().isBefore(horaFinASEJ)) {
            double yyASEJ = yASEJ(ahoraASEJ.toLocalTime());
            double x0ASEJ = xASEJ(ahoraASEJ.toLocalDate());
            gASEJ.setStroke(Color.web("#CF222E"));
            gASEJ.setLineWidth(2);
            gASEJ.strokeLine(x0ASEJ, yyASEJ, x0ASEJ + anchoASEJ, yyASEJ);
        }
    }

    private void construirBloquesASEJ() {
        getChildren().removeIf(nASEJ -> nASEJ != canvasASEJ);
        bloquesASEJ.clear();
        for (CitaDtoASEJ cASEJ : citasASEJ) {
            Label lASEJ = new Label((cASEJ.estadoASEJ() == EstadoCitaASEJ.CONFIRMADA ? "✔ " : "") + HORAASEJ.format(cASEJ.inicioASEJ()) + "  "
                    + cASEJ.pacienteNombreASEJ() + "\n" + cASEJ.motivoASEJ());
            lASEJ.setWrapText(true);
            lASEJ.setAlignment(Pos.TOP_LEFT);
            lASEJ.setPadding(new Insets(1, 6, 1, 6));
            lASEJ.setStyle(estiloASEJ(cASEJ, cASEJ.idASEJ() == (seleccionadaASEJ == null ? -1 : seleccionadaASEJ)));
            lASEJ.getStyleClass().add("agenda-cita");
            Tooltip.install(lASEJ, new Tooltip(HORAASEJ.format(cASEJ.inicioASEJ()) + "–" + HORAASEJ.format(cASEJ.finASEJ()) + " · " + cASEJ.estadoASEJ().etiquetaASEJ()
                    + "\n" + cASEJ.pacienteNombreASEJ() + " (" + cASEJ.numeroHistoriaASEJ() + ")\n" + cASEJ.motivoASEJ()
                    + (cASEJ.planItemDescripcionASEJ() == null ? "" : "\nPlan: " + cASEJ.planItemDescripcionASEJ())));
            lASEJ.setCursor(cASEJ.estadoASEJ().activaASEJ() ? Cursor.MOVE : Cursor.HAND);
            lASEJ.setOnMousePressed(eASEJ -> iniciarArrastreASEJ(cASEJ, lASEJ, eASEJ));
            lASEJ.setOnMouseDragged(eASEJ -> arrastrarASEJ(lASEJ, eASEJ));
            lASEJ.setOnMouseReleased(eASEJ -> soltarASEJ(cASEJ, lASEJ, eASEJ));
            lASEJ.setOnMouseClicked(eASEJ -> {
                if (eASEJ.getButton() == MouseButton.PRIMARY && eASEJ.getClickCount() == 2 && oyenteASEJ != null) {
                    oyenteASEJ.abrirASEJ(cASEJ);
                }
                eASEJ.consume();
            });
            bloquesASEJ.put(cASEJ.idASEJ(), lASEJ);
            getChildren().add(lASEJ);
        }
    }

    private CitaDtoASEJ citaPorIdASEJ(long idASEJ) {
        return citasASEJ.stream().filter(cASEJ -> cASEJ.idASEJ() == idASEJ).findFirst().orElseThrow();
    }

    private String estiloASEJ(CitaDtoASEJ cASEJ, boolean elegidaASEJ) {
        Color baseASEJ = colorASEJ(cASEJ.colorAgendaASEJ());
        double opacidadASEJ = switch (cASEJ.estadoASEJ()) {
            case PROGRAMADA, CONFIRMADA -> 0.92;
            case ATENDIDA -> 0.45;
            default -> 0.3;
        };
        Color fondoASEJ = cASEJ.estadoASEJ() == EstadoCitaASEJ.NO_ASISTIO ? Color.gray(0.55, 0.5) : baseASEJ.deriveColor(0, 1, 1, opacidadASEJ);
        String bordeASEJ = elegidaASEJ ? (oscuroASEJ ? "#F0F6FC" : "#1F2328") : toWebASEJ(baseASEJ.darker());
        String grosorASEJ = elegidaASEJ ? "2.5" : (cASEJ.estadoASEJ() == EstadoCitaASEJ.CONFIRMADA ? "1.5 1.5 1.5 5" : "1");
        String textoColorASEJ = cASEJ.estadoASEJ().activaASEJ() || oscuroASEJ ? "white" : "#1F2328";
        return "-fx-background-color: " + toWebASEJ(fondoASEJ) + "; -fx-background-radius: 5; -fx-border-radius: 5;"
                + " -fx-border-color: " + bordeASEJ + "; -fx-border-width: " + grosorASEJ + "; -fx-text-fill: " + textoColorASEJ
                + "; -fx-font-size: 11px;" + (cASEJ.estadoASEJ() == EstadoCitaASEJ.NO_ASISTIO ? " -fx-strikethrough: true;" : "");
    }

    private static Color colorASEJ(String webASEJ) {
        try {
            return webASEJ == null ? Color.web("#2563EB") : Color.web(webASEJ);
        } catch (IllegalArgumentException eASEJ) {
            return Color.web("#2563EB");
        }
    }

    private static String toWebASEJ(Color cASEJ) {
        return String.format(Locale.ROOT, "rgba(%d,%d,%d,%.2f)", Math.round(cASEJ.getRed() * 255), Math.round(cASEJ.getGreen() * 255),
                Math.round(cASEJ.getBlue() * 255), cASEJ.getOpacity());
    }

    private void iniciarArrastreASEJ(CitaDtoASEJ cASEJ, Label lASEJ, MouseEvent eASEJ) {
        if (eASEJ.getButton() != MouseButton.PRIMARY) {
            return;
        }
        if (oyenteASEJ != null) {
            oyenteASEJ.seleccionarASEJ(cASEJ);
        }
        movioASEJ = false;
        if (!cASEJ.estadoASEJ().activaASEJ()) {
            arrastradaASEJ = null;
            return;
        }
        arrastradaASEJ = cASEJ;
        arrastreDyASEJ = eASEJ.getY();
        arrastreX0ASEJ = lASEJ.getLayoutX();
        arrastreY0ASEJ = lASEJ.getLayoutY();
        lASEJ.toFront();
    }

    private void arrastrarASEJ(Label lASEJ, MouseEvent eASEJ) {
        if (arrastradaASEJ == null) {
            return;
        }
        javafx.geometry.Point2D pASEJ = sceneToLocal(eASEJ.getSceneX(), eASEJ.getSceneY());
        int diaASEJ = Math.max(0, Math.min(diasASEJ - 1, (int) ((pASEJ.getX() - ANCHO_HORASASEJ) / anchoColumnaASEJ())));
        double topASEJ = pASEJ.getY() - arrastreDyASEJ;
        int filaASEJ = (int) Math.round((topASEJ - ALTO_CABECERAASEJ) / ALTO_FILAASEJ);
        filaASEJ = Math.max(0, Math.min(filasASEJ() - 1, filaASEJ));
        double nxASEJ = ANCHO_HORASASEJ + diaASEJ * anchoColumnaASEJ() + 3;
        double nyASEJ = ALTO_CABECERAASEJ + filaASEJ * ALTO_FILAASEJ + 1;
        if (nxASEJ != lASEJ.getLayoutX() || nyASEJ != lASEJ.getLayoutY()) {
            movioASEJ = true;
        }
        lASEJ.relocate(nxASEJ, nyASEJ);
        lASEJ.setOpacity(0.8);
    }

    private void soltarASEJ(CitaDtoASEJ cASEJ, Label lASEJ, MouseEvent eASEJ) {
        lASEJ.setOpacity(1);
        if (arrastradaASEJ == null || !movioASEJ) {
            arrastradaASEJ = null;
            return;
        }
        arrastradaASEJ = null;
        int diaASEJ = (int) Math.round((lASEJ.getLayoutX() - 3 - ANCHO_HORASASEJ) / anchoColumnaASEJ());
        int filaASEJ = (int) Math.round((lASEJ.getLayoutY() - 1 - ALTO_CABECERAASEJ) / ALTO_FILAASEJ);
        LocalDateTime nuevoASEJ = desdeASEJ.plusDays(diaASEJ).atTime(horaInicioASEJ).plusMinutes((long) filaASEJ * intervaloASEJ);
        if (nuevoASEJ.equals(cASEJ.inicioASEJ())) {
            lASEJ.relocate(arrastreX0ASEJ, arrastreY0ASEJ);
            return;
        }
        if (oyenteASEJ != null) {
            oyenteASEJ.moverASEJ(cASEJ, nuevoASEJ);
        }
    }

    public List<CitaDtoASEJ> citasASEJ() {
        return new ArrayList<>(citasASEJ);
    }
}
