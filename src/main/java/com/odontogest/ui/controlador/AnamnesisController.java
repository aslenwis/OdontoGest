/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.controlador;

import com.odontogest.modelo.Sexo;
import com.odontogest.seguridad.Permiso;
import com.odontogest.seguridad.SesionUsuario;
import com.odontogest.servicio.AnamnesisService;
import com.odontogest.servicio.dto.AnamnesisDto;
import com.odontogest.servicio.dto.PacienteDetalle;
import com.odontogest.ui.soporte.Formularios;
import com.odontogest.ui.soporte.Notificador;
import com.odontogest.ui.soporte.PacienteActivo;
import com.odontogest.ui.soporte.TareaUi;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pestaña "Anamnesis" de la ficha (RF-PAC-04). Al guardar, la cabecera de la ficha actualiza la alerta de
 * alergias. Solo ADMIN y ODONTOLOGO pueden editarla.
 */
@Component
@RequiredArgsConstructor
public class AnamnesisController {

    private final AnamnesisService anamnesisService;
    private final PacienteActivo pacienteActivo;
    private final Notificador notificador;
    private final TareaUi tareaUi;
    private final SesionUsuario sesionUsuario;

    @FXML private TextArea txtAlergias;
    @FXML private TextArea txtMedicacion;
    @FXML private TextArea txtAntecedentes;
    @FXML private TextArea txtHabitos;
    @FXML private FlowPane panelEnfermedades;
    @FXML private CheckBox chkFuma;
    @FXML private CheckBox chkGestante;
    @FXML private TextField txtPresion;
    @FXML private TextArea txtObservaciones;
    @FXML private Label lblActualizado;
    @FXML private Button btnGuardar;

    private final Map<String, CheckBox> enfermedades = new LinkedHashMap<>();
    private ChangeListener<PacienteDetalle> oyente;

    @FXML
    public void initialize() {
        enfermedades.clear();
        // Lista fija en memoria: no requiere consultar la BD desde el hilo de UI.
        AnamnesisService.ENFERMEDADES.forEach((clave, etiqueta) -> {
            CheckBox c = new CheckBox(etiqueta);
            enfermedades.put(clave, c);
            panelEnfermedades.getChildren().add(c);
        });
        boolean puedeEditar = sesionUsuario.tiene(Permiso.ANAMNESIS_EDITAR);
        btnGuardar.setDisable(!puedeEditar);
        if (!puedeEditar) {
            lblActualizado.setText("Solo lectura: su rol no puede editar la anamnesis.");
        }

        if (oyente != null) {
            pacienteActivo.pacienteProperty().removeListener(oyente);
        }
        oyente = (obs, anterior, actual) -> cargar(actual);
        pacienteActivo.pacienteProperty().addListener(oyente);
        cargar(pacienteActivo.get());
    }

    private void cargar(PacienteDetalle p) {
        if (p == null) {
            mostrar(null);
            return;
        }
        chkGestante.setDisable(p.sexo() != Sexo.F);
        tareaUi.ejecutar(() -> anamnesisService.obtener(p.id()), this::mostrar);
    }

    private void mostrar(AnamnesisDto a) {
        txtAlergias.setText(a == null ? "" : texto(a.alergias()));
        txtMedicacion.setText(a == null ? "" : texto(a.medicacionActual()));
        txtAntecedentes.setText(a == null ? "" : texto(a.antecedentesMedicos()));
        txtHabitos.setText(a == null ? "" : texto(a.habitos()));
        enfermedades.forEach((clave, c) -> c.setSelected(a != null && Boolean.TRUE.equals(a.enfermedades().get(clave))));
        chkFuma.setSelected(a != null && a.fuma());
        chkGestante.setSelected(a != null && a.gestante());
        txtPresion.setText(a == null ? "" : texto(a.presionArterial()));
        txtObservaciones.setText(a == null ? "" : texto(a.observaciones()));
        if (sesionUsuario.tiene(Permiso.ANAMNESIS_EDITAR)) {
            lblActualizado.setText(a == null || a.actualizadoEn() == null ? "Sin registrar"
                    : "Actualizada el " + Formularios.fechaHora(a.actualizadoEn()));
        }
    }

    @FXML
    private void onGuardar() {
        PacienteDetalle p = pacienteActivo.get();
        if (p == null) {
            return;
        }
        Map<String, Boolean> marcadas = new LinkedHashMap<>();
        enfermedades.forEach((clave, c) -> marcadas.put(clave, c.isSelected()));
        AnamnesisDto dto = new AnamnesisDto(p.id(), txtAntecedentes.getText(), txtAlergias.getText(),
                txtMedicacion.getText(), marcadas, txtHabitos.getText(), chkFuma.isSelected(), chkGestante.isSelected(),
                txtPresion.getText(), txtObservaciones.getText(), null);
        tareaUi.ejecutar(btnGuardar, () -> anamnesisService.guardar(dto), guardada -> {
            mostrar(guardada);
            notificador.exito("Anamnesis guardada.");
            pacienteActivo.recargar();
        });
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor;
    }
}
