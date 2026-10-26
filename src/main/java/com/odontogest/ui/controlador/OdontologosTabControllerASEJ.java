package com.odontogest.ui.controlador;

import atlantafx.base.theme.Styles;
import com.odontogest.servicio.OdontologoServiceASEJ;
import com.odontogest.servicio.dto.HorarioDtoASEJ;
import com.odontogest.servicio.dto.OdontologoDtoASEJ;
import com.odontogest.servicio.dto.OdontologoFormASEJ;
import com.odontogest.ui.soporte.FormulariosASEJ;
import com.odontogest.ui.soporte.NotificadorASEJ;
import com.odontogest.ui.soporte.TareaUiASEJ;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class OdontologosTabControllerASEJ extends ListadoEditableASEJ<OdontologoDtoASEJ> {

    private static final Locale ESASEJ = Locale.forLanguageTag("es-PE");
    private static final List<String> ESPECIALIDADESASEJ = List.of("Odontología general", "Endodoncia", "Ortodoncia",
            "Periodoncia", "Odontopediatría", "Rehabilitación oral", "Cirugía bucal y maxilofacial");

    private final OdontologoServiceASEJ odontologoServiceASEJ;

    public OdontologosTabControllerASEJ(TareaUiASEJ tareaUiASEJ, NotificadorASEJ notificadorASEJ, OdontologoServiceASEJ odontologoServiceASEJ) {
        super(tareaUiASEJ, notificadorASEJ);
        this.odontologoServiceASEJ = odontologoServiceASEJ;
    }

    static String diaASEJ(DayOfWeek dASEJ) {
        String nASEJ = dASEJ.getDisplayName(TextStyle.FULL, ESASEJ);
        return nASEJ.substring(0, 1).toUpperCase(ESASEJ) + nASEJ.substring(1);
    }

    static String resumenHorarioASEJ(List<HorarioDtoASEJ> horariosASEJ) {
        if (horariosASEJ.isEmpty()) {
            return "Sin horario";
        }
        String diasASEJ = horariosASEJ.stream().map(HorarioDtoASEJ::diaASEJ).distinct()
                .map(dASEJ -> dASEJ.getDisplayName(TextStyle.SHORT, ESASEJ)).collect(Collectors.joining(", "));
        return diasASEJ + " (" + horariosASEJ.size() + (horariosASEJ.size() == 1 ? " bloque)" : " bloques)");
    }

    @Override
    protected void configurarColumnasASEJ() {
        TableColumn<OdontologoDtoASEJ, String> colorASEJ = FormulariosASEJ.columnaASEJ("Color", OdontologoDtoASEJ::colorAgendaASEJ, 70);
        colorASEJ.setCellFactory(cASEJ -> new TableCell<>() {
            @Override
            protected void updateItem(String valorASEJ, boolean vacioASEJ) {
                super.updateItem(valorASEJ, vacioASEJ);
                setText(null);
                setGraphic(vacioASEJ || valorASEJ == null ? null : muestraASEJ(valorASEJ));
            }
        });
        FormulariosASEJ.columnasASEJ(tablaASEJ,
                FormulariosASEJ.columnaASEJ("Odontólogo", OdontologoDtoASEJ::nombreCompletoASEJ, 230),
                FormulariosASEJ.columnaASEJ("COP", OdontologoDtoASEJ::copASEJ, 80),
                FormulariosASEJ.columnaASEJ("Especialidad", OdontologoDtoASEJ::especialidadASEJ, 170),
                FormulariosASEJ.columnaASEJ("Teléfono", OdontologoDtoASEJ::telefonoASEJ, 110),
                colorASEJ,
                FormulariosASEJ.columnaASEJ("Horario", oASEJ -> resumenHorarioASEJ(oASEJ.horariosASEJ()), 260),
                FormulariosASEJ.columnaASEJ("Activo", oASEJ -> FormulariosASEJ.siNoASEJ(oASEJ.activoASEJ()), 70));
    }

    @Override
    protected List<OdontologoDtoASEJ> listarASEJ(boolean incluirInactivosASEJ) {
        return odontologoServiceASEJ.listarASEJ(incluirInactivosASEJ);
    }

    @Override
    protected void abrirFormularioASEJ(OdontologoDtoASEJ oASEJ) {
        TextField nombresASEJ = new TextField(oASEJ == null ? "" : oASEJ.nombresASEJ());
        TextField apellidosASEJ = new TextField(oASEJ == null ? "" : oASEJ.apellidosASEJ());
        TextField copASEJ = new TextField(oASEJ == null ? "" : oASEJ.copASEJ());
        TextField rneASEJ = new TextField(oASEJ == null || oASEJ.rneASEJ() == null ? "" : oASEJ.rneASEJ());
        ComboBox<String> especialidadASEJ = new ComboBox<>();
        especialidadASEJ.setEditable(true);
        especialidadASEJ.getItems().setAll(ESPECIALIDADESASEJ);
        especialidadASEJ.setValue(oASEJ == null ? ESPECIALIDADESASEJ.get(0) : oASEJ.especialidadASEJ());
        TextField telefonoASEJ = new TextField(oASEJ == null || oASEJ.telefonoASEJ() == null ? "" : oASEJ.telefonoASEJ());
        TextField emailASEJ = new TextField(oASEJ == null || oASEJ.emailASEJ() == null ? "" : oASEJ.emailASEJ());
        ColorPicker colorASEJ = new ColorPicker(Color.web(oASEJ == null ? "#2563EB" : oASEJ.colorAgendaASEJ()));
        CheckBox activoASEJ = new CheckBox("Activo");
        activoASEJ.setSelected(oASEJ == null || oASEJ.activoASEJ());

        GridPane gASEJ = FormulariosASEJ.grillaASEJ();
        FormulariosASEJ.filaASEJ(gASEJ, "Nombres *", nombresASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "Apellidos *", apellidosASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "COP *", copASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "RNE", rneASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "Especialidad", especialidadASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "Teléfono", telefonoASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "Correo", emailASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "Color en agenda", colorASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "", activoASEJ);

        VBox filasHorarioASEJ = new VBox(6);
        (oASEJ == null ? List.<HorarioDtoASEJ>of() : oASEJ.horariosASEJ()).forEach(hASEJ -> filasHorarioASEJ.getChildren().add(filaHorarioASEJ(filasHorarioASEJ, hASEJ)));
        Button agregarASEJ = FormulariosASEJ.botonASEJ("Agregar bloque", Feather.PLUS, Styles.SMALL);
        agregarASEJ.setOnAction(eASEJ -> filasHorarioASEJ.getChildren().add(filaHorarioASEJ(filasHorarioASEJ,
                new HorarioDtoASEJ(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0)))));
        Button estandarASEJ = FormulariosASEJ.botonASEJ("Horario estándar", Feather.CLOCK, Styles.SMALL, Styles.FLAT);
        estandarASEJ.setOnAction(eASEJ -> {
            filasHorarioASEJ.getChildren().clear();
            horarioEstandarASEJ().forEach(hASEJ -> filasHorarioASEJ.getChildren().add(filaHorarioASEJ(filasHorarioASEJ, hASEJ)));
        });
        Label tituloHorarioASEJ = new Label("Horario de atención");
        tituloHorarioASEJ.getStyleClass().add("formulario-seccion");
        VBox contenidoASEJ = new VBox(12, gASEJ, tituloHorarioASEJ, filasHorarioASEJ, new HBox(8, agregarASEJ, estandarASEJ));

        Long idASEJ = oASEJ == null ? null : oASEJ.idASEJ();
        abrirPanelASEJ(oASEJ == null ? "Nuevo odontólogo" : "Editar odontólogo", contenidoASEJ, () -> {
            List<HorarioDtoASEJ> horariosASEJ = new ArrayList<>();
            for (var nodoASEJ : filasHorarioASEJ.getChildren()) {
                horariosASEJ.add(((FilaHorarioASEJ) nodoASEJ.getUserData()).leerASEJ());
            }
            OdontologoFormASEJ formASEJ = new OdontologoFormASEJ(idASEJ, nombresASEJ.getText(), apellidosASEJ.getText(), copASEJ.getText(),
                    rneASEJ.getText(), especialidadASEJ.getEditor().getText(), telefonoASEJ.getText(), emailASEJ.getText(),
                    hexASEJ(colorASEJ.getValue()), activoASEJ.isSelected(), horariosASEJ);
            return () -> odontologoServiceASEJ.guardarASEJ(formASEJ);
        }, "Odontólogo guardado.");
    }

    private record FilaHorarioASEJ(ComboBox<DayOfWeek> diaASEJ, TextField inicioASEJ, TextField finASEJ) {
        HorarioDtoASEJ leerASEJ() {
            return new HorarioDtoASEJ(diaASEJ.getValue(), FormulariosASEJ.horaASEJ(inicioASEJ.getText()), FormulariosASEJ.horaASEJ(finASEJ.getText()));
        }
    }

    private static HBox filaHorarioASEJ(VBox contenedorASEJ, HorarioDtoASEJ hASEJ) {
        ComboBox<DayOfWeek> diaASEJ = new ComboBox<>();
        diaASEJ.getItems().setAll(DayOfWeek.values());
        diaASEJ.setConverter(FormulariosASEJ.conversorASEJ(OdontologosTabControllerASEJ::diaASEJ));
        diaASEJ.setValue(hASEJ.diaASEJ());
        diaASEJ.setPrefWidth(130);
        TextField inicioASEJ = new TextField(FormulariosASEJ.horaASEJ(hASEJ.inicioASEJ()));
        TextField finASEJ = new TextField(FormulariosASEJ.horaASEJ(hASEJ.finASEJ()));
        inicioASEJ.setPrefColumnCount(5);
        finASEJ.setPrefColumnCount(5);
        Button quitarASEJ = FormulariosASEJ.botonASEJ(null, Feather.X, Styles.BUTTON_ICON, Styles.FLAT, Styles.DANGER);
        HBox filaASEJ = new HBox(8, diaASEJ, inicioASEJ, new Label("a"), finASEJ, quitarASEJ);
        filaASEJ.setAlignment(Pos.CENTER_LEFT);
        filaASEJ.setUserData(new FilaHorarioASEJ(diaASEJ, inicioASEJ, finASEJ));
        quitarASEJ.setOnAction(eASEJ -> contenedorASEJ.getChildren().remove(filaASEJ));
        return filaASEJ;
    }

    private static List<HorarioDtoASEJ> horarioEstandarASEJ() {
        List<HorarioDtoASEJ> hASEJ = new ArrayList<>();
        for (DayOfWeek dASEJ : DayOfWeek.values()) {
            if (dASEJ == DayOfWeek.SUNDAY) {
                continue;
            }
            hASEJ.add(new HorarioDtoASEJ(dASEJ, LocalTime.of(9, 0), LocalTime.of(13, 0)));
            if (dASEJ != DayOfWeek.SATURDAY) {
                hASEJ.add(new HorarioDtoASEJ(dASEJ, LocalTime.of(15, 0), LocalTime.of(19, 0)));
            }
        }
        return hASEJ;
    }

    private static Region muestraASEJ(String colorASEJ) {
        Rectangle rASEJ = new Rectangle(18, 18, Color.web(colorASEJ));
        rASEJ.setArcWidth(6);
        rASEJ.setArcHeight(6);
        HBox cajaASEJ = new HBox(rASEJ);
        cajaASEJ.setAlignment(Pos.CENTER_LEFT);
        return cajaASEJ;
    }

    private static String hexASEJ(Color cASEJ) {
        return String.format("#%02X%02X%02X", Math.round(cASEJ.getRed() * 255), Math.round(cASEJ.getGreen() * 255),
                Math.round(cASEJ.getBlue() * 255));
    }
}
