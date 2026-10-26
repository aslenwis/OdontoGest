package com.odontogest.ui.controlador;

import atlantafx.base.controls.CustomTextField;
import com.odontogest.evento.NavegarAASEJ;
import com.odontogest.seguridad.PermisoASEJ;
import com.odontogest.seguridad.SesionUsuarioASEJ;
import com.odontogest.servicio.PacienteServiceASEJ;
import com.odontogest.servicio.dto.PacienteResumenASEJ;
import com.odontogest.ui.componente.PaginadorASEJ;
import com.odontogest.ui.soporte.CargadorVistasASEJ;
import com.odontogest.ui.soporte.FormulariosASEJ;
import com.odontogest.ui.soporte.NotificadorASEJ;
import com.odontogest.ui.soporte.RefrescableASEJ;
import com.odontogest.ui.soporte.TareaUiASEJ;
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

@Component
@RequiredArgsConstructor
public class PacientesControllerASEJ implements RefrescableASEJ {

    private final PacienteServiceASEJ pacienteServiceASEJ;
    private final CargadorVistasASEJ cargadorVistasASEJ;
    private final NotificadorASEJ notificadorASEJ;
    private final TareaUiASEJ tareaUiASEJ;
    private final SesionUsuarioASEJ sesionUsuarioASEJ;
    private final ApplicationEventPublisher eventosASEJ;

    @FXML private CustomTextField txtBuscarASEJ;
    @FXML private CheckBox chkInactivosASEJ;
    @FXML private Button btnAbrirASEJ;
    @FXML private Button btnNuevoASEJ;
    @FXML private TableView<PacienteResumenASEJ> tablaASEJ;
    @FXML private PaginadorASEJ paginadorASEJ;

    private PauseTransition esperaASEJ;

    @FXML
    public void initialize() {
        FormulariosASEJ.columnasASEJ(tablaASEJ,
                FormulariosASEJ.columnaASEJ("Historia", PacienteResumenASEJ::numeroHistoriaASEJ, 110),
                FormulariosASEJ.columnaASEJ("Documento", PacienteResumenASEJ::documentoASEJ, 150),
                FormulariosASEJ.columnaASEJ("Paciente", PacienteResumenASEJ::nombreCompletoASEJ, 320),
                FormulariosASEJ.columnaASEJ("Edad", pASEJ -> pASEJ.edadASEJ() + " años", 80),
                FormulariosASEJ.columnaASEJ("Teléfono", PacienteResumenASEJ::telefonoASEJ, 130),
                FormulariosASEJ.columnaASEJ("Estado", pASEJ -> (pASEJ.activoASEJ() ? "Activo" : "Inactivo") + (pASEJ.demoASEJ() ? " · Paciente demo" : ""),
                        150));
        tablaASEJ.setRowFactory(tASEJ -> {
            TableRow<PacienteResumenASEJ> filaASEJ = new TableRow<>();
            filaASEJ.setOnMouseClicked(eASEJ -> {
                if (eASEJ.getClickCount() == 2 && !filaASEJ.isEmpty()) {
                    abrirASEJ(filaASEJ.getItem());
                }
            });
            return filaASEJ;
        });
        tablaASEJ.setOnKeyPressed(eASEJ -> {
            if (eASEJ.getCode() == KeyCode.ENTER && tablaASEJ.getSelectionModel().getSelectedItem() != null) {
                abrirASEJ(tablaASEJ.getSelectionModel().getSelectedItem());
            }
        });
        btnAbrirASEJ.disableProperty().bind(tablaASEJ.getSelectionModel().selectedItemProperty().isNull());
        btnNuevoASEJ.setDisable(!sesionUsuarioASEJ.tieneASEJ(PermisoASEJ.PACIENTE_EDITAR));

        esperaASEJ = new PauseTransition(Duration.millis(300));
        esperaASEJ.setOnFinished(eASEJ -> cargarASEJ(0));
        txtBuscarASEJ.textProperty().addListener((obsASEJ, aASEJ, bASEJ) -> esperaASEJ.playFromStart());
        chkInactivosASEJ.selectedProperty().addListener((obsASEJ, aASEJ, bASEJ) -> cargarASEJ(0));
        paginadorASEJ.setAlCambiarASEJ(this::cargarASEJ);
    }

    @Override
    public void refrescarASEJ() {
        cargarASEJ(paginadorASEJ.paginaASEJ());
    }

    private void cargarASEJ(int paginaASEJ) {
        String filtroASEJ = txtBuscarASEJ.getText();
        boolean inactivosASEJ = chkInactivosASEJ.isSelected();
        tareaUiASEJ.ejecutarASEJ(() -> pacienteServiceASEJ.buscarASEJ(filtroASEJ, inactivosASEJ, paginaASEJ), resultadoASEJ -> {
            tablaASEJ.getItems().setAll(resultadoASEJ.contenidoASEJ());
            paginadorASEJ.actualizarASEJ(resultadoASEJ);
        });
    }

    @FXML
    private void onAbrirASEJ() {
        PacienteResumenASEJ seleccionadoASEJ = tablaASEJ.getSelectionModel().getSelectedItem();
        if (seleccionadoASEJ != null) {
            abrirASEJ(seleccionadoASEJ);
        }
    }

    @FXML
    private void onNuevoASEJ() {
        CargadorVistasASEJ.VistaASEJ vistaASEJ = cargadorVistasASEJ.cargarNuevaASEJ("paciente-form-view");
        PacienteFormControllerASEJ formularioASEJ = (PacienteFormControllerASEJ) vistaASEJ.controladorASEJ();
        formularioASEJ.configurarASEJ(null, idASEJ -> {
            notificadorASEJ.cerrarPanelASEJ();
            notificadorASEJ.exitoASEJ("Paciente registrado.");
            eventosASEJ.publishEvent(new NavegarAASEJ(MainControllerASEJ.VISTA_FICHAASEJ, Map.of("pacienteId", idASEJ)));
        }, notificadorASEJ::cerrarPanelASEJ);
        HBox pieASEJ = formularioASEJ.extraerPieASEJ();
        notificadorASEJ.abrirPanelASEJ(FormulariosASEJ.panelLateralASEJ("Nuevo paciente", vistaASEJ.raizASEJ(), pieASEJ));
        formularioASEJ.enfocarASEJ();
    }

    private void abrirASEJ(PacienteResumenASEJ pacienteASEJ) {
        eventosASEJ.publishEvent(new NavegarAASEJ(MainControllerASEJ.VISTA_FICHAASEJ, Map.of("pacienteId", pacienteASEJ.idASEJ())));
    }
}
