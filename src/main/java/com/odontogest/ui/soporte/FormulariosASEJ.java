package com.odontogest.ui.soporte;

import atlantafx.base.theme.Styles;
import com.odontogest.excepcion.NegocioExceptionASEJ;
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
import javafx.scene.layout.StackPane;
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

public final class FormulariosASEJ {

    public static final DateTimeFormatter FECHAASEJ = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter FECHA_HORAASEJ = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    public static final DateTimeFormatter HORAASEJ = DateTimeFormatter.ofPattern("HH:mm");
    public static final double ANCHO_PANELASEJ = 460;
    public static final String ESTADO_OKASEJ = "estado-ok";
    public static final String ESTADO_ALERTAASEJ = "estado-alerta";
    public static final String ESTADO_PELIGROASEJ = "estado-peligro";

    public record OpcionASEJ<T>(String etiquetaASEJ, T valorASEJ) {

        @Override
        public String toString() {
            return etiquetaASEJ;
        }
    }

    private FormulariosASEJ() {
    }

    public static GridPane grillaASEJ() {
        GridPane gASEJ = new GridPane();
        gASEJ.setHgap(12);
        gASEJ.setVgap(10);
        ColumnConstraints etiquetasASEJ = new ColumnConstraints();
        etiquetasASEJ.setMinWidth(130);
        ColumnConstraints camposASEJ = new ColumnConstraints();
        camposASEJ.setHgrow(Priority.ALWAYS);
        camposASEJ.setFillWidth(true);
        gASEJ.getColumnConstraints().addAll(etiquetasASEJ, camposASEJ);
        return gASEJ;
    }

    public static void filaASEJ(GridPane grillaASEJ, String etiquetaASEJ, Node campoASEJ) {
        int filaASEJ = grillaASEJ.getRowCount();
        Label lASEJ = new Label(etiquetaASEJ);
        lASEJ.setLabelFor(campoASEJ);
        grillaASEJ.add(lASEJ, 0, filaASEJ);
        if (campoASEJ instanceof Region rASEJ) {
            rASEJ.setMaxWidth(Double.MAX_VALUE);
        }
        grillaASEJ.add(campoASEJ, 1, filaASEJ);
    }

    public static void seccionASEJ(GridPane grillaASEJ, String tituloASEJ) {
        Label lASEJ = new Label(tituloASEJ);
        lASEJ.getStyleClass().add("formulario-seccion");
        grillaASEJ.add(lASEJ, 0, grillaASEJ.getRowCount(), 2, 1);
    }

    public static VBox panelLateralASEJ(String tituloASEJ, Node contenidoASEJ, Node... botonesASEJ) {
        Label tASEJ = new Label(tituloASEJ);
        tASEJ.getStyleClass().add(Styles.TITLE_3);
        StackPane envolturaASEJ = new StackPane(contenidoASEJ);
        envolturaASEJ.setPadding(new Insets(0, 14, 0, 0));
        ScrollPane desplazableASEJ = new ScrollPane(envolturaASEJ);
        desplazableASEJ.setFitToWidth(true);
        desplazableASEJ.getStyleClass().add("edge-to-edge");
        VBox.setVgrow(desplazableASEJ, Priority.ALWAYS);
        HBox pieASEJ = new HBox(8, botonesASEJ);
        pieASEJ.setAlignment(Pos.CENTER_RIGHT);
        VBox panelASEJ = new VBox(16, tASEJ, desplazableASEJ, pieASEJ);
        panelASEJ.getStyleClass().add("panel-lateral");
        panelASEJ.setPadding(new Insets(20));
        panelASEJ.setPrefWidth(ANCHO_PANELASEJ);
        panelASEJ.setMaxWidth(ANCHO_PANELASEJ);
        return panelASEJ;
    }

    public static Button botonASEJ(String textoASEJ, Ikon iconoASEJ, String... estilosASEJ) {
        Button bASEJ = new Button(textoASEJ);
        if (iconoASEJ != null) {
            bASEJ.setGraphic(new FontIcon(iconoASEJ));
        }
        bASEJ.getStyleClass().addAll(estilosASEJ);
        return bASEJ;
    }

    public static <T> TableColumn<T, String> columnaASEJ(String tituloASEJ, Function<T, ?> valorASEJ, double anchoASEJ) {
        TableColumn<T, String> cASEJ = new TableColumn<>(tituloASEJ);
        cASEJ.setCellValueFactory(fASEJ -> {
            Object vASEJ = valorASEJ.apply(fASEJ.getValue());
            return new SimpleStringProperty(vASEJ == null ? "" : vASEJ.toString());
        });
        if (anchoASEJ > 0) {
            cASEJ.setPrefWidth(anchoASEJ);
        }
        return cASEJ;
    }

    @SafeVarargs
    public static <T> void columnasASEJ(TableView<T> tablaASEJ, TableColumn<T, ?>... columnasASEJ) {
        tablaASEJ.getColumns().setAll(List.of(columnasASEJ));
        tablaASEJ.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    public static <T> StringConverter<T> conversorASEJ(Function<T, String> textoASEJ) {
        return new StringConverter<>() {
            @Override
            public String toString(T valorASEJ) {
                return valorASEJ == null ? "" : textoASEJ.apply(valorASEJ);
            }

            @Override
            public T fromString(String sASEJ) {
                return null;
            }
        };
    }

    public static void configurarFechaASEJ(DatePicker selectorASEJ) {
        selectorASEJ.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate fechaASEJ) {
                return fechaASEJ == null ? "" : FECHAASEJ.format(fechaASEJ);
            }

            @Override
            public LocalDate fromString(String textoASEJ) {
                if (textoASEJ == null || textoASEJ.isBlank()) {
                    return null;
                }
                try {
                    return LocalDate.parse(textoASEJ.trim(), FECHAASEJ);
                } catch (DateTimeParseException eASEJ) {
                    return null;
                }
            }
        });
        selectorASEJ.setPromptText("dd/mm/aaaa");
    }

    public static LocalDate fechaASEJ(DatePicker selectorASEJ) {
        String textoASEJ = selectorASEJ.getEditor().getText();
        if (textoASEJ == null || textoASEJ.isBlank()) {
            return null;
        }
        LocalDate escritaASEJ = selectorASEJ.getConverter().fromString(textoASEJ);
        if (escritaASEJ == null) {
            throw new NegocioExceptionASEJ("La fecha \"" + textoASEJ + "\" no es válida (use dd/mm/aaaa).");
        }
        return escritaASEJ;
    }

    public static <T> T seleccionASEJ(ComboBox<OpcionASEJ<T>> comboASEJ) {
        OpcionASEJ<T> oASEJ = comboASEJ.getValue();
        return oASEJ == null ? null : oASEJ.valorASEJ();
    }

    public static BigDecimal montoASEJ(String textoASEJ, String campoASEJ) {
        if (textoASEJ == null || textoASEJ.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(textoASEJ.trim().replace("S/", "").replace(" ", "").replace(",", "."));
        } catch (NumberFormatException eASEJ) {
            throw new NegocioExceptionASEJ("El " + campoASEJ + " \"" + textoASEJ + "\" no es un monto válido.");
        }
    }

    public static int enteroASEJ(String textoASEJ, String campoASEJ) {
        try {
            return Integer.parseInt(textoASEJ.trim());
        } catch (NumberFormatException | NullPointerException eASEJ) {
            throw new NegocioExceptionASEJ("El campo " + campoASEJ + " debe ser un número entero.");
        }
    }

    public static LocalTime horaASEJ(String textoASEJ) {
        if (textoASEJ == null || textoASEJ.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(textoASEJ.trim().length() == 4 ? "0" + textoASEJ.trim() : textoASEJ.trim(), HORAASEJ);
        } catch (DateTimeParseException eASEJ) {
            throw new NegocioExceptionASEJ("La hora \"" + textoASEJ + "\" no es válida (use HH:mm, p. ej. 09:00).");
        }
    }

    public static String solesASEJ(BigDecimal montoASEJ) {
        return montoASEJ == null ? "" : "S/ " + montoASEJ.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    public static String fechaASEJ(LocalDate fechaASEJ) {
        return fechaASEJ == null ? "" : FECHAASEJ.format(fechaASEJ);
    }

    public static String fechaHoraASEJ(LocalDateTime momentoASEJ) {
        return momentoASEJ == null ? "" : FECHA_HORAASEJ.format(momentoASEJ);
    }

    public static String horaASEJ(LocalTime horaASEJ) {
        return horaASEJ == null ? "" : HORAASEJ.format(horaASEJ);
    }

    public static String siNoASEJ(boolean valorASEJ) {
        return valorASEJ ? "Sí" : "No";
    }
}
