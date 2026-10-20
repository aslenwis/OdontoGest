/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.controlador;

import atlantafx.base.theme.Styles;
import com.odontogest.servicio.OdontologoService;
import com.odontogest.servicio.dto.HorarioDto;
import com.odontogest.servicio.dto.OdontologoDto;
import com.odontogest.servicio.dto.OdontologoForm;
import com.odontogest.ui.soporte.Formularios;
import com.odontogest.ui.soporte.Notificador;
import com.odontogest.ui.soporte.TareaUi;
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

/**
 * Pestaña "Odontólogos y horarios" de Catálogos (RF-CAT-04). El horario se edita como una lista de bloques
 * (día, hora de inicio y fin); "Horario estándar" carga L–V 09:00–13:00 y 15:00–19:00, S 09:00–13:00.
 */
@Component
public class OdontologosTabController extends ListadoEditable<OdontologoDto> {

    private static final Locale ES = Locale.forLanguageTag("es-PE");
    private static final List<String> ESPECIALIDADES = List.of("Odontología general", "Endodoncia", "Ortodoncia",
            "Periodoncia", "Odontopediatría", "Rehabilitación oral", "Cirugía bucal y maxilofacial");

    private final OdontologoService odontologoService;

    public OdontologosTabController(TareaUi tareaUi, Notificador notificador, OdontologoService odontologoService) {
        super(tareaUi, notificador);
        this.odontologoService = odontologoService;
    }

    static String dia(DayOfWeek d) {
        String n = d.getDisplayName(TextStyle.FULL, ES);
        return n.substring(0, 1).toUpperCase(ES) + n.substring(1);
    }

    static String resumenHorario(List<HorarioDto> horarios) {
        if (horarios.isEmpty()) {
            return "Sin horario";
        }
        String dias = horarios.stream().map(HorarioDto::dia).distinct()
                .map(d -> d.getDisplayName(TextStyle.SHORT, ES)).collect(Collectors.joining(", "));
        return dias + " (" + horarios.size() + (horarios.size() == 1 ? " bloque)" : " bloques)");
    }

    @Override
    protected void configurarColumnas() {
        TableColumn<OdontologoDto, String> color = Formularios.columna("Color", OdontologoDto::colorAgenda, 70);
        color.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String valor, boolean vacio) {
                super.updateItem(valor, vacio);
                setText(null);
                setGraphic(vacio || valor == null ? null : muestra(valor));
            }
        });
        Formularios.columnas(tabla,
                Formularios.columna("Odontólogo", OdontologoDto::nombreCompleto, 230),
                Formularios.columna("COP", OdontologoDto::cop, 80),
                Formularios.columna("Especialidad", OdontologoDto::especialidad, 170),
                Formularios.columna("Teléfono", OdontologoDto::telefono, 110),
                color,
                Formularios.columna("Horario", o -> resumenHorario(o.horarios()), 260),
                Formularios.columna("Activo", o -> Formularios.siNo(o.activo()), 70));
    }

    @Override
    protected List<OdontologoDto> listar(boolean incluirInactivos) {
        return odontologoService.listar(incluirInactivos);
    }

    @Override
    protected void abrirFormulario(OdontologoDto o) {
        TextField nombres = new TextField(o == null ? "" : o.nombres());
        TextField apellidos = new TextField(o == null ? "" : o.apellidos());
        TextField cop = new TextField(o == null ? "" : o.cop());
        TextField rne = new TextField(o == null || o.rne() == null ? "" : o.rne());
        ComboBox<String> especialidad = new ComboBox<>();
        especialidad.setEditable(true);
        especialidad.getItems().setAll(ESPECIALIDADES);
        especialidad.setValue(o == null ? ESPECIALIDADES.get(0) : o.especialidad());
        TextField telefono = new TextField(o == null || o.telefono() == null ? "" : o.telefono());
        TextField email = new TextField(o == null || o.email() == null ? "" : o.email());
        ColorPicker color = new ColorPicker(Color.web(o == null ? "#2563EB" : o.colorAgenda()));
        CheckBox activo = new CheckBox("Activo");
        activo.setSelected(o == null || o.activo());

        GridPane g = Formularios.grilla();
        Formularios.fila(g, "Nombres *", nombres);
        Formularios.fila(g, "Apellidos *", apellidos);
        Formularios.fila(g, "COP *", cop);
        Formularios.fila(g, "RNE", rne);
        Formularios.fila(g, "Especialidad", especialidad);
        Formularios.fila(g, "Teléfono", telefono);
        Formularios.fila(g, "Correo", email);
        Formularios.fila(g, "Color en agenda", color);
        Formularios.fila(g, "", activo);

        VBox filasHorario = new VBox(6);
        (o == null ? List.<HorarioDto>of() : o.horarios()).forEach(h -> filasHorario.getChildren().add(filaHorario(filasHorario, h)));
        Button agregar = Formularios.boton("Agregar bloque", Feather.PLUS, Styles.SMALL);
        agregar.setOnAction(e -> filasHorario.getChildren().add(filaHorario(filasHorario,
                new HorarioDto(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0)))));
        Button estandar = Formularios.boton("Horario estándar", Feather.CLOCK, Styles.SMALL, Styles.FLAT);
        estandar.setOnAction(e -> {
            filasHorario.getChildren().clear();
            horarioEstandar().forEach(h -> filasHorario.getChildren().add(filaHorario(filasHorario, h)));
        });
        Label tituloHorario = new Label("Horario de atención");
        tituloHorario.getStyleClass().add("formulario-seccion");
        VBox contenido = new VBox(12, g, tituloHorario, filasHorario, new HBox(8, agregar, estandar));

        Long id = o == null ? null : o.id();
        abrirPanel(o == null ? "Nuevo odontólogo" : "Editar odontólogo", contenido, () -> {
            List<HorarioDto> horarios = new ArrayList<>();
            for (var nodo : filasHorario.getChildren()) {
                horarios.add(((FilaHorario) nodo.getUserData()).leer());
            }
            OdontologoForm form = new OdontologoForm(id, nombres.getText(), apellidos.getText(), cop.getText(),
                    rne.getText(), especialidad.getEditor().getText(), telefono.getText(), email.getText(),
                    hex(color.getValue()), activo.isSelected(), horarios);
            return () -> odontologoService.guardar(form);
        }, "Odontólogo guardado.");
    }

    /** Controles de un bloque del horario. */
    private record FilaHorario(ComboBox<DayOfWeek> dia, TextField inicio, TextField fin) {
        HorarioDto leer() {
            return new HorarioDto(dia.getValue(), Formularios.hora(inicio.getText()), Formularios.hora(fin.getText()));
        }
    }

    private static HBox filaHorario(VBox contenedor, HorarioDto h) {
        ComboBox<DayOfWeek> dia = new ComboBox<>();
        dia.getItems().setAll(DayOfWeek.values());
        dia.setConverter(Formularios.conversor(OdontologosTabController::dia));
        dia.setValue(h.dia());
        dia.setPrefWidth(130);
        TextField inicio = new TextField(Formularios.hora(h.inicio()));
        TextField fin = new TextField(Formularios.hora(h.fin()));
        inicio.setPrefColumnCount(5);
        fin.setPrefColumnCount(5);
        Button quitar = Formularios.boton(null, Feather.X, Styles.BUTTON_ICON, Styles.FLAT, Styles.DANGER);
        HBox fila = new HBox(8, dia, inicio, new Label("a"), fin, quitar);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.setUserData(new FilaHorario(dia, inicio, fin));
        quitar.setOnAction(e -> contenedor.getChildren().remove(fila));
        return fila;
    }

    private static List<HorarioDto> horarioEstandar() {
        List<HorarioDto> h = new ArrayList<>();
        for (DayOfWeek d : DayOfWeek.values()) {
            if (d == DayOfWeek.SUNDAY) {
                continue;
            }
            h.add(new HorarioDto(d, LocalTime.of(9, 0), LocalTime.of(13, 0)));
            if (d != DayOfWeek.SATURDAY) {
                h.add(new HorarioDto(d, LocalTime.of(15, 0), LocalTime.of(19, 0)));
            }
        }
        return h;
    }

    private static Region muestra(String color) {
        Rectangle r = new Rectangle(18, 18, Color.web(color));
        r.setArcWidth(6);
        r.setArcHeight(6);
        HBox caja = new HBox(r);
        caja.setAlignment(Pos.CENTER_LEFT);
        return caja;
    }

    private static String hex(Color c) {
        return String.format("#%02X%02X%02X", Math.round(c.getRed() * 255), Math.round(c.getGreen() * 255),
                Math.round(c.getBlue() * 255));
    }
}
