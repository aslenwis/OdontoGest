/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.controlador;

import com.odontogest.dominio.odontograma.EstadoHallazgo;
import com.odontogest.dominio.odontograma.TipoRadicular;
import com.odontogest.servicio.CatalogoHallazgoService;
import com.odontogest.servicio.ProcedimientoService;
import com.odontogest.servicio.dto.HallazgoTipoDto;
import com.odontogest.servicio.dto.ProcedimientoDto;
import com.odontogest.servicio.dto.ReglaSugerenciaDto;
import com.odontogest.servicio.dto.ReglaSugerenciaForm;
import com.odontogest.ui.soporte.Formularios;
import com.odontogest.ui.soporte.Formularios.Opcion;
import com.odontogest.ui.soporte.Notificador;
import com.odontogest.ui.soporte.TareaUi;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.layout.GridPane;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Pestaña "Reglas de sugerencia" de Catálogos (RF-CAT-03).
 */
@Component
public class ReglasTabController extends ListadoEditable<ReglaSugerenciaDto> {

    private record Listas(List<HallazgoTipoDto> tipos, List<ProcedimientoDto> procedimientos) {
    }

    private final CatalogoHallazgoService catalogoService;
    private final ProcedimientoService procedimientoService;

    @FXML private Button btnEliminar;

    public ReglasTabController(TareaUi tareaUi, Notificador notificador, CatalogoHallazgoService catalogoService,
                               ProcedimientoService procedimientoService) {
        super(tareaUi, notificador);
        this.catalogoService = catalogoService;
        this.procedimientoService = procedimientoService;
    }

    static String estado(EstadoHallazgo e) {
        return e == null ? "Cualquiera" : e == EstadoHallazgo.BUENO ? "Bueno" : "Malo";
    }

    static String radicular(TipoRadicular t) {
        return t == null ? "Cualquiera" : switch (t) {
            case UNIRRADICULAR -> "Unirradicular";
            case BIRRADICULAR -> "Birradicular";
            case MULTIRRADICULAR -> "Multirradicular";
        };
    }

    @Override
    protected void alInicializar() {
        btnEliminar.disableProperty().bind(tabla.getSelectionModel().selectedItemProperty().isNull());
    }

    @Override
    protected void configurarColumnas() {
        Formularios.columnas(tabla,
                Formularios.columna("Prioridad", ReglaSugerenciaDto::prioridad, 80),
                Formularios.columna("Hallazgo", ReglaSugerenciaDto::hallazgoTipoNombre, 200),
                Formularios.columna("Estado", r -> estado(r.estado()), 90),
                Formularios.columna("Tipo radicular", r -> radicular(r.tipoRadicular()), 120),
                Formularios.columna("Superficies", r -> r.minSuperficies() + "–" + r.maxSuperficies(), 90),
                Formularios.columna("Procedimiento", ReglaSugerenciaDto::procedimientoNombre, 260),
                Formularios.columna("Fase", ReglaSugerenciaDto::fase, 60),
                Formularios.columna("Activa", r -> Formularios.siNo(r.activo()), 70));
    }

    @Override
    protected List<ReglaSugerenciaDto> listar(boolean incluirInactivos) {
        return catalogoService.listarReglas();
    }

    @Override
    protected void abrirFormulario(ReglaSugerenciaDto regla) {
        tareaUi.ejecutar(() -> new Listas(catalogoService.listarTipos(true), procedimientoService.listar(true)),
                listas -> mostrarFormulario(regla, listas));
    }

    private void mostrarFormulario(ReglaSugerenciaDto r, Listas listas) {
        List<Opcion<Long>> opcionesTipo = new ArrayList<>();
        opcionesTipo.add(new Opcion<>("(general · toda la boca)", null));
        listas.tipos().forEach(t -> opcionesTipo.add(new Opcion<>(t.nombre(), t.id())));
        ComboBox<Opcion<Long>> hallazgo = new ComboBox<>();
        hallazgo.getItems().setAll(opcionesTipo);
        hallazgo.setValue(opcionesTipo.stream()
                .filter(o -> r != null && Objects.equals(o.valor(), r.hallazgoTipoId())).findFirst()
                .orElse(opcionesTipo.get(r == null && opcionesTipo.size() > 1 ? 1 : 0)));

        ComboBox<Opcion<EstadoHallazgo>> estado = new ComboBox<>();
        estado.getItems().setAll(new Opcion<>(estado(null), null), new Opcion<>("Bueno", EstadoHallazgo.BUENO),
                new Opcion<>("Malo", EstadoHallazgo.MALO));
        estado.setValue(estado.getItems().stream().filter(o -> r != null && o.valor() == r.estado()).findFirst()
                .orElse(estado.getItems().get(0)));

        ComboBox<Opcion<TipoRadicular>> radicular = new ComboBox<>();
        radicular.getItems().add(new Opcion<>(radicular(null), null));
        for (TipoRadicular t : TipoRadicular.values()) {
            radicular.getItems().add(new Opcion<>(radicular(t), t));
        }
        radicular.setValue(radicular.getItems().stream().filter(o -> r != null && o.valor() == r.tipoRadicular())
                .findFirst().orElse(radicular.getItems().get(0)));

        Spinner<Integer> minimo = new Spinner<>(0, 5, r == null ? 0 : r.minSuperficies());
        Spinner<Integer> maximo = new Spinner<>(0, 5, r == null ? 5 : r.maxSuperficies());
        ComboBox<ProcedimientoDto> procedimiento = new ComboBox<>();
        procedimiento.getItems().setAll(listas.procedimientos());
        procedimiento.setValue(r == null ? null : listas.procedimientos().stream()
                .filter(p -> p.id() == r.procedimientoId()).findFirst().orElse(null));
        Spinner<Integer> fase = new Spinner<>(1, 4, r == null ? 2 : r.fase());
        Spinner<Integer> prioridad = new Spinner<>(0, 9999, r == null ? 100 : r.prioridad(), 1);
        prioridad.setEditable(true);
        CheckBox activa = new CheckBox("Activa");
        activa.setSelected(r == null || r.activo());

        hallazgo.valueProperty().addListener((obs, a, b) -> {
            boolean general = b == null || b.valor() == null;
            estado.setDisable(general);
            radicular.setDisable(general);
        });
        boolean general = hallazgo.getValue().valor() == null;
        estado.setDisable(general);
        radicular.setDisable(general);

        GridPane g = Formularios.grilla();
        Formularios.fila(g, "Hallazgo", hallazgo);
        Formularios.fila(g, "Estado", estado);
        Formularios.fila(g, "Tipo radicular", radicular);
        Formularios.fila(g, "Superficies desde", minimo);
        Formularios.fila(g, "Superficies hasta", maximo);
        Formularios.fila(g, "Procedimiento *", procedimiento);
        Formularios.fila(g, "Fase (1–4)", fase);
        Formularios.fila(g, "Prioridad", prioridad);
        Formularios.fila(g, "", activa);

        Long id = r == null ? null : r.id();
        abrirPanel(r == null ? "Nueva regla" : "Editar regla", g, () -> {
            Long tipoId = Formularios.seleccion(hallazgo);
            ReglaSugerenciaForm form = new ReglaSugerenciaForm(id, tipoId,
                    tipoId == null ? null : Formularios.seleccion(estado),
                    tipoId == null ? null : Formularios.seleccion(radicular),
                    minimo.getValue(), maximo.getValue(),
                    procedimiento.getValue() == null ? null : procedimiento.getValue().id(),
                    fase.getValue(), prioridad.getValue(), activa.isSelected());
            return () -> catalogoService.guardarRegla(form);
        }, "Regla guardada.");
    }

    @FXML
    private void onEliminar() {
        ReglaSugerenciaDto r = tabla.getSelectionModel().getSelectedItem();
        if (r == null || !notificador.confirmar("Eliminar regla",
                "¿Eliminar la regla \"" + r.hallazgoTipoNombre() + " → " + r.procedimientoNombre() + "\"?")) {
            return;
        }
        tareaUi.ejecutar(btnEliminar, () -> {
            catalogoService.eliminarRegla(r.id());
            return null;
        }, x -> {
            notificador.exito("Regla eliminada.");
            refrescar();
        });
    }
}
