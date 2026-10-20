/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.controlador;

import atlantafx.base.theme.Styles;
import com.odontogest.excepcion.NegocioException;
import com.odontogest.ui.soporte.Formularios;
import com.odontogest.ui.soporte.Notificador;
import com.odontogest.ui.soporte.Refrescable;
import com.odontogest.ui.soporte.TareaUi;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import org.kordamp.ikonli.feather.Feather;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * Base de las vistas "tabla + formulario en panel lateral" (catálogos y usuarios): carga en segundo plano,
 * doble clic para editar, filtro de inactivos opcional y guardado con validación.
 *
 * @param <T> DTO de cada fila
 */
public abstract class ListadoEditable<T> implements Refrescable {

    protected final TareaUi tareaUi;
    protected final Notificador notificador;

    @FXML protected TableView<T> tabla;
    @FXML protected Button btnEditar;
    /** Opcional: no todas las vistas lo declaran en su FXML. */
    @FXML protected CheckBox chkInactivos;

    protected ListadoEditable(TareaUi tareaUi, Notificador notificador) {
        this.tareaUi = tareaUi;
        this.notificador = notificador;
    }

    @FXML
    public void initialize() {
        configurarColumnas();
        tabla.setRowFactory(t -> {
            TableRow<T> fila = new TableRow<>();
            fila.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !fila.isEmpty()) {
                    abrirFormulario(fila.getItem());
                }
            });
            return fila;
        });
        btnEditar.disableProperty().bind(tabla.getSelectionModel().selectedItemProperty().isNull());
        if (chkInactivos != null) {
            chkInactivos.selectedProperty().addListener((obs, a, b) -> refrescar());
        }
        alInicializar();
    }

    /** Punto de extensión para configuración adicional. */
    protected void alInicializar() {
    }

    @Override
    public void refrescar() {
        boolean inactivos = chkInactivos != null && chkInactivos.isSelected();
        tareaUi.ejecutar(() -> listar(inactivos), filas -> tabla.getItems().setAll(filas));
    }

    @FXML
    protected void onNuevo() {
        abrirFormulario(null);
    }

    @FXML
    protected void onEditar() {
        T seleccionado = tabla.getSelectionModel().getSelectedItem();
        if (seleccionado != null) {
            abrirFormulario(seleccionado);
        }
    }

    protected abstract void configurarColumnas();

    /** Se ejecuta en segundo plano. */
    protected abstract List<T> listar(boolean incluirInactivos);

    /** @param existente fila a editar, o {@code null} para crear */
    protected abstract void abrirFormulario(T existente);

    /**
     * Abre el panel lateral con botones Cancelar y Guardar.
     *
     * @param armar lee los controles (hilo de UI) y devuelve el trabajo a ejecutar en segundo plano; puede
     *              lanzar {@link NegocioException} si un valor escrito no es válido
     */
    protected void abrirPanel(String titulo, Node contenido, Supplier<Callable<?>> armar, String mensajeExito) {
        Button cancelar = Formularios.boton("Cancelar", null);
        cancelar.setCancelButton(true);
        cancelar.setOnAction(e -> notificador.cerrarPanel());
        Button guardar = Formularios.boton("Guardar", Feather.SAVE, Styles.ACCENT);
        guardar.setOnAction(e -> {
            Callable<?> trabajo;
            try {
                trabajo = armar.get();
            } catch (NegocioException ex) {
                tareaUi.manejarError(ex);
                return;
            }
            tareaUi.ejecutar(guardar, trabajo, r -> {
                notificador.cerrarPanel();
                notificador.exito(mensajeExito);
                refrescar();
            });
        });
        notificador.abrirPanel(Formularios.panelLateral(titulo, contenido, cancelar, guardar));
    }
}
