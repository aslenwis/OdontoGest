/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.controlador;

import atlantafx.base.theme.Styles;
import com.odontogest.dominio.odontograma.ConteoIndice;
import com.odontogest.dominio.odontograma.ReglaColor;
import com.odontogest.dominio.odontograma.Representacion;
import com.odontogest.dominio.odontograma.SimboloHallazgo;
import com.odontogest.servicio.CatalogoHallazgoService;
import com.odontogest.servicio.dto.HallazgoTipoDto;
import com.odontogest.servicio.dto.HallazgoTipoForm;
import com.odontogest.ui.soporte.Formularios;
import com.odontogest.ui.soporte.Notificador;
import com.odontogest.ui.soporte.TareaUi;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Pestaña "Hallazgos" de Catálogos (RF-CAT-02). Si un tipo ya tiene hallazgos registrados, los campos
 * estructurales se bloquean (el servicio también lo valida).
 */
@Component
public class HallazgosTabController extends ListadoEditable<HallazgoTipoDto> {

    private final CatalogoHallazgoService catalogoService;

    public HallazgosTabController(TareaUi tareaUi, Notificador notificador, CatalogoHallazgoService catalogoService) {
        super(tareaUi, notificador);
        this.catalogoService = catalogoService;
    }

    static String representacion(Representacion r) {
        return switch (r) {
            case SUPERFICIE -> "Por superficie";
            case PIEZA -> "Por pieza";
            case ENTRE_PIEZAS -> "Entre dos piezas";
            case RANGO_PIEZAS -> "Rango de piezas";
        };
    }

    static String reglaColor(ReglaColor r) {
        return switch (r) {
            case POR_ESTADO -> "Según estado (azul bueno / rojo malo)";
            case SIEMPRE_ROJO -> "Siempre rojo";
            case SIEMPRE_AZUL -> "Siempre azul";
        };
    }

    static String sigla(HallazgoTipoDto t) {
        return t.siglasOpciones().isEmpty() ? (t.sigla() == null ? "" : t.sigla()) : String.join(" / ", t.siglasOpciones());
    }

    @Override
    protected void configurarColumnas() {
        Formularios.columnas(tabla,
                Formularios.columna("Orden", HallazgoTipoDto::orden, 60),
                Formularios.columna("Nombre", HallazgoTipoDto::nombre, 210),
                Formularios.columna("Sigla", HallazgosTabController::sigla, 110),
                Formularios.columna("Representación", t -> representacion(t.representacion()), 120),
                Formularios.columna("Símbolo", HallazgoTipoDto::simbolo, 140),
                Formularios.columna("Color", t -> switch (t.reglaColor()) {
                    case POR_ESTADO -> "Según estado";
                    case SIEMPRE_ROJO -> "Rojo";
                    case SIEMPRE_AZUL -> "Azul";
                }, 100),
                Formularios.columna("Capa", HallazgoTipoDto::grupoExclusion, 150),
                Formularios.columna("CPO-D", HallazgoTipoDto::conteoCpod, 65),
                Formularios.columna("ceo-d", HallazgoTipoDto::conteoCeod, 65),
                Formularios.columna("Activo", t -> Formularios.siNo(t.activo()), 60),
                Formularios.columna("En uso", t -> Formularios.siNo(t.enUso()), 60));
    }

    @Override
    protected List<HallazgoTipoDto> listar(boolean incluirInactivos) {
        return catalogoService.listarTipos(incluirInactivos);
    }

    @Override
    protected void abrirFormulario(HallazgoTipoDto t) {
        TextField codigo = new TextField(t == null ? "" : t.codigo());
        codigo.setPromptText("PIEZA_AUSENTE");
        TextField nombre = new TextField(t == null ? "" : t.nombre());
        TextField sigla = new TextField(t == null || t.sigla() == null ? "" : t.sigla());
        TextField opciones = new TextField(t == null ? "" : String.join(", ", t.siglasOpciones()));
        opciones.setPromptText("AM, R, IV (el usuario elige al registrar)");
        ComboBox<Representacion> representacion = combo(Representacion.values(), t == null ? Representacion.PIEZA : t.representacion());
        representacion.setConverter(Formularios.conversor(HallazgosTabController::representacion));
        ComboBox<SimboloHallazgo> simbolo = combo(SimboloHallazgo.values(), t == null ? SimboloHallazgo.SOLO_SIGLA : t.simbolo());
        ComboBox<ReglaColor> color = combo(ReglaColor.values(), t == null ? ReglaColor.SIEMPRE_ROJO : t.reglaColor());
        color.setConverter(Formularios.conversor(HallazgosTabController::reglaColor));
        ComboBox<String> capa = new ComboBox<>();
        capa.setEditable(true);
        capa.getItems().setAll(tabla.getItems().stream().map(HallazgoTipoDto::grupoExclusion).distinct().sorted().toList());
        capa.setValue(t == null ? null : t.grupoExclusion());
        CheckBox oculta = new CheckBox("Oculta las superficies (pieza ausente)");
        oculta.setSelected(t != null && t.ocultaSuperficies());
        ComboBox<ConteoIndice> cpod = combo(new ConteoIndice[]{ConteoIndice.NINGUNO, ConteoIndice.C, ConteoIndice.P, ConteoIndice.O},
                t == null ? ConteoIndice.NINGUNO : t.conteoCpod());
        ComboBox<ConteoIndice> ceod = combo(new ConteoIndice[]{ConteoIndice.NINGUNO, ConteoIndice.C, ConteoIndice.E, ConteoIndice.O},
                t == null ? ConteoIndice.NINGUNO : t.conteoCeod());
        Spinner<Integer> orden = new Spinner<>(0, 9999, t == null ? siguienteOrden() : t.orden(), 10);
        orden.setEditable(true);
        CheckBox activo = new CheckBox("Activo");
        activo.setSelected(t == null || t.activo());

        GridPane g = Formularios.grilla();
        Formularios.fila(g, "Código *", codigo);
        Formularios.fila(g, "Nombre *", nombre);
        Formularios.fila(g, "Sigla fija", sigla);
        Formularios.fila(g, "Opciones de sigla", opciones);
        Formularios.fila(g, "Representación *", representacion);
        Formularios.fila(g, "Símbolo *", simbolo);
        Formularios.fila(g, "Regla de color *", color);
        Formularios.fila(g, "Capa de exclusión *", capa);
        Formularios.fila(g, "", oculta);
        Formularios.fila(g, "Cuenta en CPO-D", cpod);
        Formularios.fila(g, "Cuenta en ceo-d", ceod);
        Formularios.fila(g, "Orden en la paleta", orden);
        Formularios.fila(g, "", activo);

        Node contenido = g;
        if (t != null && t.enUso()) {
            for (Node n : List.of(codigo, representacion, color, capa, oculta, cpod, ceod)) {
                n.setDisable(true);
            }
            Label aviso = new Label("Este tipo ya tiene hallazgos registrados: su estructura no puede cambiar porque "
                    + "alteraría odontogramas cerrados. Puede editar nombre, sigla, símbolo, orden y estado.");
            aviso.setWrapText(true);
            aviso.getStyleClass().addAll(Styles.WARNING, Styles.TEXT_SMALL);
            contenido = new VBox(12, aviso, g);
        }

        Long id = t == null ? null : t.id();
        abrirPanel(t == null ? "Nuevo tipo de hallazgo" : "Editar tipo de hallazgo", contenido, () -> {
            List<String> listaOpciones = Arrays.stream(opciones.getText().split("[,;/\\s]+"))
                    .filter(s -> !s.isBlank()).toList();
            HallazgoTipoForm form = new HallazgoTipoForm(id, codigo.getText(), nombre.getText(), sigla.getText(),
                    listaOpciones, representacion.getValue(), simbolo.getValue(), color.getValue(),
                    capa.getEditor().getText(), oculta.isSelected(), cpod.getValue(), ceod.getValue(),
                    activo.isSelected(), orden.getValue());
            return () -> catalogoService.guardarTipo(form);
        }, "Tipo de hallazgo guardado.");
    }

    private int siguienteOrden() {
        return tabla.getItems().stream().mapToInt(HallazgoTipoDto::orden).max().orElse(0) + 10;
    }

    private static <E> ComboBox<E> combo(E[] valores, E seleccion) {
        ComboBox<E> c = new ComboBox<>();
        c.getItems().setAll(valores);
        c.setValue(seleccion);
        return c;
    }
}
