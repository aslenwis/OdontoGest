/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.controlador;

import com.odontogest.evento.NavegarA;
import com.odontogest.evento.SolicitudNuevaAtencion;
import com.odontogest.seguridad.Permiso;
import com.odontogest.seguridad.SesionUsuario;
import com.odontogest.servicio.dto.PacienteDetalle;
import com.odontogest.ui.soporte.PacienteActivo;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Ficha del paciente: cabecera con identificación, alertas (alergias en rojo, consentimiento de datos
 * pendiente) y "Nueva atención"; pestañas Datos, Anamnesis, Odontograma y Atenciones, que siguen a
 * {@link PacienteActivo}.
 */
@Component
@RequiredArgsConstructor
public class PacienteFichaController {

    private final PacienteActivo pacienteActivo;
    private final ApplicationEventPublisher eventos;
    private final SesionUsuario sesionUsuario;

    @FXML private Label lblIniciales;
    @FXML private Label lblNombre;
    @FXML private Label lblDetalle;
    @FXML private Label lblAlergias;
    @FXML private Label lblConsentimiento;
    @FXML private Label lblInactivo;
    @FXML private TabPane pestanas;
    @FXML private Button btnNuevaAtencion;

    private ChangeListener<PacienteDetalle> oyente;

    @FXML
    public void initialize() {
        boolean atiende = sesionUsuario.tiene(Permiso.ATENCION_GESTIONAR);
        btnNuevaAtencion.setVisible(atiende);
        btnNuevaAtencion.setManaged(atiende);
        if (oyente != null) {
            pacienteActivo.pacienteProperty().removeListener(oyente);
        }
        oyente = (obs, anterior, actual) -> mostrar(actual);
        pacienteActivo.pacienteProperty().addListener(oyente);
        mostrar(pacienteActivo.get());
    }

    private void mostrar(PacienteDetalle p) {
        if (p == null) {
            lblIniciales.setText("");
            lblNombre.setText("Cargando…");
            lblDetalle.setText("");
            visible(lblAlergias, false);
            visible(lblConsentimiento, false);
            visible(lblInactivo, false);
            return;
        }
        if (pestanas.getSelectionModel().getSelectedIndex() < 0) {
            pestanas.getSelectionModel().selectFirst();
        }
        lblIniciales.setText(iniciales(p));
        lblNombre.setText(p.nombreCompleto());
        lblDetalle.setText(p.numeroHistoria() + "  ·  " + p.documento() + "  ·  " + p.edad()
                + (p.edad() == 1 ? " año" : " años"));
        lblAlergias.setText("Alergias: " + (p.tieneAlergias() ? p.alergias().replaceAll("\\s+", " ") : ""));
        visible(lblAlergias, p.tieneAlergias());
        lblConsentimiento.setText("Falta consentimiento de datos personales");
        visible(lblConsentimiento, p.consentimientoDatosEn() == null);
        visible(lblInactivo, !p.activo());
    }

    /** Muestra la pestaña con ese id ("datos", "anamnesis", "odontograma", "plan", "atenciones"). */
    public void seleccionarPestana(String id) {
        if (pestanas == null || id == null) {
            return;
        }
        pestanas.getTabs().stream().filter(t -> id.equals(t.getId())).findFirst()
                .ifPresent(t -> pestanas.getSelectionModel().select(t));
    }

    @FXML
    private void onNuevaAtencion() {
        eventos.publishEvent(new SolicitudNuevaAtencion());
    }

    @FXML
    private void onVolver() {
        eventos.publishEvent(new NavegarA(MainController.VISTA_PACIENTES));
    }

    private static String iniciales(PacienteDetalle p) {
        return (p.nombres().substring(0, 1) + p.apellidoPaterno().substring(0, 1)).toUpperCase();
    }

    private static void visible(Label l, boolean visible) {
        l.setVisible(visible);
        l.setManaged(visible);
    }
}
