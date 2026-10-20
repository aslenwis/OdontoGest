/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.controlador;

import com.odontogest.dominio.tratamiento.FormaCobro;
import com.odontogest.modelo.CategoriaProcedimiento;
import com.odontogest.servicio.ProcedimientoService;
import com.odontogest.servicio.dto.ProcedimientoDto;
import com.odontogest.servicio.dto.ProcedimientoForm;
import com.odontogest.ui.soporte.Formularios;
import com.odontogest.ui.soporte.Notificador;
import com.odontogest.ui.soporte.TareaUi;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Pestaña "Procedimientos" de Catálogos: tarifario (RF-CAT-01).
 */
@Component
public class ProcedimientosTabController extends ListadoEditable<ProcedimientoDto> {

    private final ProcedimientoService procedimientoService;

    public ProcedimientosTabController(TareaUi tareaUi, Notificador notificador, ProcedimientoService procedimientoService) {
        super(tareaUi, notificador);
        this.procedimientoService = procedimientoService;
    }

    /** Texto visible de la forma de cobro. */
    static String formaCobro(FormaCobro f) {
        return switch (f) {
            case PIEZA -> "Por pieza";
            case SUPERFICIE -> "Por superficie";
            case ARCADA -> "Por arcada";
            case BOCA -> "Toda la boca";
            case SESION -> "Por sesión";
        };
    }

    @Override
    protected void configurarColumnas() {
        Formularios.columnas(tabla,
                Formularios.columna("Código", ProcedimientoDto::codigo, 90),
                Formularios.columna("Nombre", ProcedimientoDto::nombre, 300),
                Formularios.columna("Categoría", p -> p.categoria().etiqueta, 130),
                Formularios.columna("Cobro", p -> formaCobro(p.formaCobro()), 120),
                Formularios.columna("Precio", p -> Formularios.soles(p.precio()), 100),
                Formularios.columna("Duración", p -> p.duracionMin() + " min", 90),
                Formularios.columna("Activo", p -> Formularios.siNo(p.activo()), 70));
    }

    @Override
    protected List<ProcedimientoDto> listar(boolean incluirInactivos) {
        return procedimientoService.listar(incluirInactivos);
    }

    @Override
    protected void abrirFormulario(ProcedimientoDto p) {
        TextField codigo = new TextField(p == null ? "" : p.codigo());
        codigo.setPromptText("RES-S");
        TextField nombre = new TextField(p == null ? "" : p.nombre());
        ComboBox<CategoriaProcedimiento> categoria = new ComboBox<>();
        categoria.getItems().setAll(CategoriaProcedimiento.values());
        categoria.setConverter(Formularios.conversor(c -> c.etiqueta));
        categoria.setValue(p == null ? null : p.categoria());
        ComboBox<FormaCobro> forma = new ComboBox<>();
        forma.getItems().setAll(FormaCobro.values());
        forma.setConverter(Formularios.conversor(ProcedimientosTabController::formaCobro));
        forma.setValue(p == null ? FormaCobro.PIEZA : p.formaCobro());
        TextField precio = new TextField(p == null ? "" : p.precio().toPlainString());
        precio.setPromptText("0.00");
        Spinner<Integer> duracion = new Spinner<>(5, 480, p == null ? 30 : p.duracionMin(), 5);
        duracion.setEditable(true);
        CheckBox activo = new CheckBox("Activo");
        activo.setSelected(p == null || p.activo());

        GridPane g = Formularios.grilla();
        Formularios.fila(g, "Código *", codigo);
        Formularios.fila(g, "Nombre *", nombre);
        Formularios.fila(g, "Categoría *", categoria);
        Formularios.fila(g, "Forma de cobro *", forma);
        Formularios.fila(g, "Precio (S/) *", precio);
        Formularios.fila(g, "Duración (min)", duracion);
        Formularios.fila(g, "", activo);

        Long id = p == null ? null : p.id();
        abrirPanel(p == null ? "Nuevo procedimiento" : "Editar procedimiento", g, () -> {
            BigDecimal monto = Formularios.monto(precio.getText(), "precio");
            ProcedimientoForm form = new ProcedimientoForm(id, codigo.getText(), nombre.getText(), categoria.getValue(),
                    monto, forma.getValue(), duracion.getValue(), activo.isSelected());
            return () -> procedimientoService.guardar(form);
        }, "Procedimiento guardado.");
    }
}
