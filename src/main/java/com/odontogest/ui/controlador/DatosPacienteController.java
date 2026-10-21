/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.controlador;

import atlantafx.base.theme.Styles;
import com.odontogest.seguridad.Permiso;
import com.odontogest.seguridad.SesionUsuario;
import com.odontogest.servicio.ConsentimientoService;
import com.odontogest.servicio.PacienteService;
import com.odontogest.servicio.dto.PacienteDetalle;
import com.odontogest.ui.soporte.CargadorVistas;
import com.odontogest.ui.soporte.Exportador;
import com.odontogest.ui.soporte.Formularios;
import com.odontogest.ui.soporte.Notificador;
import com.odontogest.ui.soporte.PacienteActivo;
import com.odontogest.ui.soporte.TareaUi;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

/**
 * Pestaña "Datos" de la ficha: edición de datos personales, consentimiento de datos en PDF y registro de
 * su firma, y activación/desactivación del paciente.
 */
@Component
@RequiredArgsConstructor
public class DatosPacienteController {

    private final PacienteActivo pacienteActivo;
    private final PacienteService pacienteService;
    private final ConsentimientoService consentimientoService;
    private final CargadorVistas cargadorVistas;
    private final Exportador exportador;
    private final Notificador notificador;
    private final TareaUi tareaUi;
    private final SesionUsuario sesionUsuario;

    @FXML private StackPane contenedorFormulario;
    @FXML private HBox barraAcciones;
    @FXML private Label lblEstadoConsentimiento;
    @FXML private Button btnPdfConsentimiento;
    @FXML private Button btnRegistrarFirma;
    @FXML private Button btnEstado;

    private PacienteFormController formulario;
    private ChangeListener<PacienteDetalle> oyente;

    @FXML
    public void initialize() {
        CargadorVistas.Vista vista = cargadorVistas.cargarNueva("paciente-form-view");
        formulario = (PacienteFormController) vista.controlador();
        formulario.configurar(null, id -> {
            notificador.exito("Datos del paciente guardados.");
            pacienteActivo.recargar();
        }, null);
        contenedorFormulario.getChildren().setAll(vista.raiz());
        HBox pie = formulario.extraerPie();
        HBox.setHgrow(pie, Priority.ALWAYS);
        pie.setMaxWidth(640);
        barraAcciones.getChildren().setAll(pie);

        boolean puedeEditar = sesionUsuario.tiene(Permiso.PACIENTE_EDITAR);
        btnRegistrarFirma.setDisable(!puedeEditar);
        btnEstado.setDisable(!puedeEditar);

        if (oyente != null) {
            pacienteActivo.pacienteProperty().removeListener(oyente);
        }
        oyente = (obs, anterior, actual) -> mostrar(actual);
        pacienteActivo.pacienteProperty().addListener(oyente);
        mostrar(pacienteActivo.get());
    }

    private void mostrar(PacienteDetalle p) {
        formulario.cargar(p);
        if (p == null) {
            lblEstadoConsentimiento.setText("");
            return;
        }
        lblEstadoConsentimiento.setText(p.consentimientoDatosEn() == null
                ? "Pendiente: aún no se registró la firma."
                : "Firmado el " + Formularios.fechaHora(p.consentimientoDatosEn()) + ".");
        lblEstadoConsentimiento.getStyleClass().removeAll(Styles.SUCCESS, Styles.WARNING);
        lblEstadoConsentimiento.getStyleClass().add(p.consentimientoDatosEn() == null ? Styles.WARNING : Styles.SUCCESS);
        btnEstado.setText(p.activo() ? "Desactivar paciente" : "Reactivar paciente");
        btnEstado.setGraphic(new FontIcon(p.activo() ? Feather.USER_X : Feather.USER_CHECK));
    }

    @FXML
    private void onPdfConsentimiento() {
        PacienteDetalle p = pacienteActivo.get();
        if (p != null) {
            exportador.guardarPdf(btnPdfConsentimiento, "consentimiento-datos-" + p.numeroHistoria() + ".pdf",
                    () -> consentimientoService.pdfDatosPersonales(p.id()));
        }
    }

    @FXML
    private void onRegistrarFirma() {
        PacienteDetalle p = pacienteActivo.get();
        if (p == null) {
            return;
        }
        TextField txtFirmante = new TextField(p.esMenorDeEdad() && p.apoderadoNombre() != null
                ? p.apoderadoNombre() : p.nombreCompleto());
        GridPane g = Formularios.grilla();
        Formularios.fila(g, "Firmado por", txtFirmante);
        Label ayuda = new Label(p.esMenorDeEdad()
                ? "El paciente es menor de edad: firma su apoderado."
                : "Registre la firma después de que el paciente firme el documento impreso.");
        ayuda.setWrapText(true);
        ayuda.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);

        Button cancelar = Formularios.boton("Cancelar", null);
        cancelar.setCancelButton(true);
        cancelar.setOnAction(e -> notificador.cerrarPanel());
        Button registrar = Formularios.boton("Registrar firma", Feather.CHECK, Styles.ACCENT);
        registrar.setDefaultButton(true);
        registrar.setOnAction(e -> tareaUi.ejecutar(registrar,
                () -> consentimientoService.registrarDatosPersonales(p.id(), txtFirmante.getText()),
                c -> {
                    notificador.cerrarPanel();
                    notificador.exito("Consentimiento registrado.");
                    pacienteActivo.recargar();
                }));
        notificador.abrirPanel(Formularios.panelLateral("Consentimiento de datos personales",
                new VBox(12, g, ayuda), cancelar, registrar));
    }

    @FXML
    private void onCambiarEstado() {
        PacienteDetalle p = pacienteActivo.get();
        if (p == null) {
            return;
        }
        boolean activar = !p.activo();
        String pregunta = activar ? "¿Reactivar a " + p.nombreCompleto() + "?"
                : "El paciente dejará de aparecer en las búsquedas; su historia clínica se conserva. ¿Desactivar a "
                  + p.nombreCompleto() + "?";
        if (!notificador.confirmar(activar ? "Reactivar paciente" : "Desactivar paciente", pregunta)) {
            return;
        }
        tareaUi.ejecutar(btnEstado, () -> {
            pacienteService.cambiarEstado(p.id(), activar);
            return null;
        }, r -> {
            notificador.exito(activar ? "Paciente reactivado." : "Paciente desactivado.");
            pacienteActivo.recargar();
        });
    }
}
