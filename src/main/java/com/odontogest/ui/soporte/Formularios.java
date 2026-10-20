/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.soporte;

import atlantafx.base.theme.Styles;
import com.odontogest.excepcion.NegocioException;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.function.Function;

/**
 * Utilidades para construir formularios, tablas y formatos de la interfaz con un estilo uniforme.
 */
public final class Formularios {

    public static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    public static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    public static final double ANCHO_PANEL = 460;

    /** Opción de un ComboBox con texto propio; permite el valor {@code null} ("Cualquiera", "General"). */
    public record Opcion<T>(String etiqueta, T valor) {

        @Override
        public String toString() {
            return etiqueta;
        }
    }

    private Formularios() {
    }

    // ------------------------------------------------------------------ estructura

    /** Grilla de dos columnas: etiqueta y campo. */
    public static GridPane grilla() {
        GridPane g = new GridPane();
        g.setHgap(12);
        g.setVgap(10);
        ColumnConstraints etiquetas = new ColumnConstraints();
        etiquetas.setMinWidth(130);
        ColumnConstraints campos = new ColumnConstraints();
        campos.setHgrow(Priority.ALWAYS);
        campos.setFillWidth(true);
        g.getColumnConstraints().addAll(etiquetas, campos);
        return g;
    }

    /** Agrega una fila "etiqueta / campo" al final de la grilla. */
    public static void fila(GridPane grilla, String etiqueta, Node campo) {
        int fila = grilla.getRowCount();
        Label l = new Label(etiqueta);
        l.setLabelFor(campo);
        grilla.add(l, 0, fila);
        if (campo instanceof Region r) {
            r.setMaxWidth(Double.MAX_VALUE);
        }
        grilla.add(campo, 1, fila);
    }

    /** Título de sección dentro de una grilla (ocupa las dos columnas). */
    public static void seccion(GridPane grilla, String titulo) {
        Label l = new Label(titulo);
        l.getStyleClass().add("formulario-seccion");
        grilla.add(l, 0, grilla.getRowCount(), 2, 1);
    }

    /** Contenedor del panel lateral: título, contenido desplazable y botones al pie. */
    public static VBox panelLateral(String titulo, Node contenido, Node... botones) {
        Label t = new Label(titulo);
        t.getStyleClass().add(Styles.TITLE_3);
        ScrollPane desplazable = new ScrollPane(contenido);
        desplazable.setFitToWidth(true);
        desplazable.getStyleClass().add("edge-to-edge");
        VBox.setVgrow(desplazable, Priority.ALWAYS);
        HBox pie = new HBox(8, botones);
        pie.setAlignment(Pos.CENTER_RIGHT);
        VBox panel = new VBox(16, t, desplazable, pie);
        panel.getStyleClass().add("panel-lateral");
        panel.setPadding(new Insets(20));
        panel.setPrefWidth(ANCHO_PANEL);
        panel.setMaxWidth(ANCHO_PANEL);
        return panel;
    }

    public static Button boton(String texto, Ikon icono, String... estilos) {
        Button b = new Button(texto);
        if (icono != null) {
            b.setGraphic(new FontIcon(icono));
        }
        b.getStyleClass().addAll(estilos);
        return b;
    }

    // ------------------------------------------------------------------ tablas

    public static <T> TableColumn<T, String> columna(String titulo, Function<T, ?> valor, double ancho) {
        TableColumn<T, String> c = new TableColumn<>(titulo);
        c.setCellValueFactory(f -> {
            Object v = valor.apply(f.getValue());
            return new SimpleStringProperty(v == null ? "" : v.toString());
        });
        if (ancho > 0) {
            c.setPrefWidth(ancho);
        }
        return c;
    }

    @SafeVarargs
    public static <T> void columnas(TableView<T> tabla, TableColumn<T, ?>... columnas) {
        tabla.getColumns().setAll(List.of(columnas));
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    // ------------------------------------------------------------------ conversores y lectura

    public static <T> StringConverter<T> conversor(Function<T, String> texto) {
        return new StringConverter<>() {
            @Override
            public String toString(T valor) {
                return valor == null ? "" : texto.apply(valor);
            }

            @Override
            public T fromString(String s) {
                return null;
            }
        };
    }

    /** DatePicker con formato dd/MM/yyyy. */
    public static void configurarFecha(DatePicker selector) {
        selector.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate fecha) {
                return fecha == null ? "" : FECHA.format(fecha);
            }

            @Override
            public LocalDate fromString(String texto) {
                if (texto == null || texto.isBlank()) {
                    return null;
                }
                try {
                    return LocalDate.parse(texto.trim(), FECHA);
                } catch (DateTimeParseException e) {
                    return null;
                }
            }
        });
        selector.setPromptText("dd/mm/aaaa");
    }

    /** Valor del DatePicker incluyendo lo escrito y aún no confirmado con Enter. */
    public static LocalDate fecha(DatePicker selector) {
        String texto = selector.getEditor().getText();
        if (texto == null || texto.isBlank()) {
            return null;
        }
        LocalDate escrita = selector.getConverter().fromString(texto);
        if (escrita == null) {
            throw new NegocioException("La fecha \"" + texto + "\" no es válida (use dd/mm/aaaa).");
        }
        return escrita;
    }

    public static <T> T seleccion(ComboBox<Opcion<T>> combo) {
        Opcion<T> o = combo.getValue();
        return o == null ? null : o.valor();
    }

    /** Monto escrito por el usuario (acepta coma o punto decimal). */
    public static BigDecimal monto(String texto, String campo) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(texto.trim().replace("S/", "").replace(" ", "").replace(",", "."));
        } catch (NumberFormatException e) {
            throw new NegocioException("El " + campo + " \"" + texto + "\" no es un monto válido.");
        }
    }

    public static int entero(String texto, String campo) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException | NullPointerException e) {
            throw new NegocioException("El campo " + campo + " debe ser un número entero.");
        }
    }

    public static LocalTime hora(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(texto.trim().length() == 4 ? "0" + texto.trim() : texto.trim(), HORA);
        } catch (DateTimeParseException e) {
            throw new NegocioException("La hora \"" + texto + "\" no es válida (use HH:mm, p. ej. 09:00).");
        }
    }

    // ------------------------------------------------------------------ formatos

    public static String soles(BigDecimal monto) {
        return monto == null ? "" : "S/ " + monto.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    public static String fecha(LocalDate fecha) {
        return fecha == null ? "" : FECHA.format(fecha);
    }

    public static String fechaHora(LocalDateTime momento) {
        return momento == null ? "" : FECHA_HORA.format(momento);
    }

    public static String hora(LocalTime hora) {
        return hora == null ? "" : HORA.format(hora);
    }

    public static String siNo(boolean valor) {
        return valor ? "Sí" : "No";
    }
}
