/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.controlador;

import atlantafx.base.controls.CustomTextField;
import com.odontogest.evento.NavegarA;
import com.odontogest.seguridad.Permiso;
import com.odontogest.seguridad.SesionUsuario;
import com.odontogest.servicio.PacienteService;
import com.odontogest.servicio.dto.PacienteResumen;
import com.odontogest.ui.componente.Paginador;
import com.odontogest.ui.soporte.CargadorVistas;
import com.odontogest.ui.soporte.Formularios;
import com.odontogest.ui.soporte.Notificador;
import com.odontogest.ui.soporte.Refrescable;
import com.odontogest.ui.soporte.TareaUi;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.util.Duration;
import javafx.scene.layout.HBox;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Listado de pacientes con búsqueda (debounce de 300 ms) y paginación (RF-PAC-02). Doble clic o Enter
 * abre la ficha; "Nuevo paciente" abre el formulario en el panel lateral.
 */
@Component
@RequiredArgsConstructor
public class PacientesController implements Refrescable {

    private final PacienteService pacienteService;
    private final CargadorVistas cargadorVistas;
    private final Notificador notificador;
    private final TareaUi tareaUi;
    private final SesionUsuario sesionUsuario;
    private final ApplicationEventPublisher eventos;

    @FXML private CustomTextField txtBuscar;
    @FXML private CheckBox chkInactivos;
    @FXML private Button btnAbrir;
    @FXML private Button btnNuevo;
    @FXML private TableView<PacienteResumen> tabla;
    @FXML private Paginador paginador;

    /** Se crea en {@link #initialize()}: las animaciones requieren el toolkit de JavaFX. */
    private PauseTransition espera;

    @FXML
    public void initialize() {
        Formularios.columnas(tabla,
                Formularios.columna("Historia", PacienteResumen::numeroHistoria, 110),
                Formularios.columna("Documento", PacienteResumen::documento, 150),
                Formularios.columna("Paciente", PacienteResumen::nombreCompleto, 320),
                Formularios.columna("Edad", p -> p.edad() + " años", 80),
                Formularios.columna("Teléfono", PacienteResumen::telefono, 130),
                Formularios.columna("Estado", p -> p.activo() ? "Activo" : "Inactivo", 90));
        tabla.setRowFactory(t -> {
            TableRow<PacienteResumen> fila = new TableRow<>();
            fila.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !fila.isEmpty()) {
                    abrir(fila.getItem());
                }
            });
            return fila;
        });
        tabla.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER && tabla.getSelectionModel().getSelectedItem() != null) {
                abrir(tabla.getSelectionModel().getSelectedItem());
            }
        });
        btnAbrir.disableProperty().bind(tabla.getSelectionModel().selectedItemProperty().isNull());
        btnNuevo.setDisable(!sesionUsuario.tiene(Permiso.PACIENTE_EDITAR));

        espera = new PauseTransition(Duration.millis(300));
        espera.setOnFinished(e -> cargar(0));
        txtBuscar.textProperty().addListener((obs, a, b) -> espera.playFromStart());
        chkInactivos.selectedProperty().addListener((obs, a, b) -> cargar(0));
        paginador.setAlCambiar(this::cargar);
    }

    @Override
    public void refrescar() {
        cargar(paginador.pagina());
    }

    private void cargar(int pagina) {
        String filtro = txtBuscar.getText();
        boolean inactivos = chkInactivos.isSelected();
        tareaUi.ejecutar(() -> pacienteService.buscar(filtro, inactivos, pagina), resultado -> {
            tabla.getItems().setAll(resultado.contenido());
            paginador.actualizar(resultado);
        });
    }

    @FXML
    private void onAbrir() {
        PacienteResumen seleccionado = tabla.getSelectionModel().getSelectedItem();
        if (seleccionado != null) {
            abrir(seleccionado);
        }
    }

    @FXML
    private void onNuevo() {
        CargadorVistas.Vista vista = cargadorVistas.cargarNueva("paciente-form-view");
        PacienteFormController formulario = (PacienteFormController) vista.controlador();
        formulario.configurar(null, id -> {
            notificador.cerrarPanel();
            notificador.exito("Paciente registrado.");
            eventos.publishEvent(new NavegarA(MainController.VISTA_FICHA, Map.of("pacienteId", id)));
        }, notificador::cerrarPanel);
        HBox pie = formulario.extraerPie();
        notificador.abrirPanel(Formularios.panelLateral("Nuevo paciente", vista.raiz(), pie));
        formulario.enfocar();
    }

    private void abrir(PacienteResumen paciente) {
        eventos.publishEvent(new NavegarA(MainController.VISTA_FICHA, Map.of("pacienteId", paciente.id())));
    }
}
