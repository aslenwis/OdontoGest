/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.controlador;

import com.odontogest.dominio.comun.Validaciones;
import com.odontogest.excepcion.NegocioException;
import com.odontogest.modelo.Sexo;
import com.odontogest.modelo.TipoDocumento;
import com.odontogest.seguridad.Permiso;
import com.odontogest.seguridad.SesionUsuario;
import com.odontogest.servicio.IntegracionService;
import com.odontogest.servicio.PacienteService;
import com.odontogest.servicio.dto.PacienteDetalle;
import com.odontogest.servicio.dto.PacienteForm;
import com.odontogest.servicio.dto.PersonaDniDto;
import com.odontogest.ui.soporte.Formularios;
import com.odontogest.ui.soporte.Notificador;
import com.odontogest.ui.soporte.TareaUi;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.function.LongConsumer;

/**
 * Formulario de registro y edición de paciente. Es {@code prototype}: cada carga del FXML obtiene su propia
 * instancia, porque puede estar abierto a la vez en el panel lateral y en la ficha.
 */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class PacienteFormController {

    private final PacienteService pacienteService;
    private final IntegracionService integracionService;
    private final TareaUi tareaUi;
    private final Notificador notificador;
    private final SesionUsuario sesionUsuario;

    @FXML private ComboBox<TipoDocumento> cmbTipoDocumento;
    @FXML private TextField txtNumeroDocumento;
    @FXML private Button btnConsultarDni;
    @FXML private TextField txtNombres;
    @FXML private TextField txtApellidoPaterno;
    @FXML private TextField txtApellidoMaterno;
    @FXML private DatePicker dpFechaNacimiento;
    @FXML private Label lblEdad;
    @FXML private ComboBox<Sexo> cmbSexo;
    @FXML private TextField txtOcupacion;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtEmail;
    @FXML private TextField txtDireccion;
    @FXML private TextField txtContactoNombre;
    @FXML private TextField txtContactoTelefono;
    @FXML private CheckBox chkRecordatorios;
    @FXML private VBox seccionApoderado;
    @FXML private HBox pie;
    @FXML private TextField txtApoderadoNombre;
    @FXML private TextField txtApoderadoDocumento;
    @FXML private ComboBox<String> cmbParentesco;
    @FXML private Button btnGuardar;
    @FXML private Button btnCancelar;

    private Long pacienteId;
    private boolean dniHabilitado;
    private LongConsumer alGuardar = id -> { };
    private Runnable alCancelar;

    @FXML
    public void initialize() {
        cmbTipoDocumento.getItems().setAll(TipoDocumento.values());
        cmbTipoDocumento.setConverter(Formularios.conversor(t -> t.etiqueta));
        cmbTipoDocumento.setValue(TipoDocumento.DNI);
        cmbTipoDocumento.valueProperty().addListener((obs, a, tipo) -> {
            boolean sinDocumento = tipo == TipoDocumento.SIN_DOCUMENTO;
            txtNumeroDocumento.setDisable(sinDocumento);
            if (sinDocumento) {
                txtNumeroDocumento.clear();
            }
            actualizarBotonDni();
        });
        txtNumeroDocumento.textProperty().addListener((obs, a, b) -> actualizarBotonDni());
        btnConsultarDni.setDisable(true);
        tareaUi.ejecutar(integracionService::dniHabilitado, habilitado -> {
            dniHabilitado = habilitado;
            btnConsultarDni.setTooltip(new Tooltip(habilitado
                    ? "Completa nombres y apellidos desde la API de DNI (requiere internet)."
                    : "Consulta de DNI deshabilitada: actívela en Configuración → API DNI."));
            actualizarBotonDni();
        }, error -> btnConsultarDni.setDisable(true));
        cmbSexo.getItems().setAll(Sexo.values());
        cmbSexo.setConverter(Formularios.conversor(s -> s.etiqueta));
        cmbParentesco.getItems().setAll("Madre", "Padre", "Tutor(a) legal", "Abuelo(a)", "Hermano(a)", "Otro");

        Formularios.configurarFecha(dpFechaNacimiento);
        dpFechaNacimiento.valueProperty().addListener((obs, a, fecha) -> actualizarEdad(fecha));
        actualizarEdad(null);
        btnGuardar.setDisable(!sesionUsuario.tiene(Permiso.PACIENTE_EDITAR));
    }

    /**
     * Saca los botones Guardar/Cancelar del formulario para ponerlos en una barra fija fuera del desplazamiento
     * (así se ven aunque la pantalla sea baja).
     */
    public HBox extraerPie() {
        ((Pane) pie.getParent()).getChildren().remove(pie);
        return pie;
    }

    /**
     * @param existente  paciente a editar, o {@code null} para uno nuevo
     * @param alGuardar  recibe el id guardado
     * @param alCancelar {@code null} oculta el botón Cancelar (formulario incrustado en la ficha)
     */
    public void configurar(PacienteDetalle existente, LongConsumer alGuardar, Runnable alCancelar) {
        this.alGuardar = alGuardar;
        this.alCancelar = alCancelar;
        btnCancelar.setVisible(alCancelar != null);
        btnCancelar.setManaged(alCancelar != null);
        cargar(existente);
    }

    private void actualizarBotonDni() {
        String n = txtNumeroDocumento.getText() == null ? "" : txtNumeroDocumento.getText().trim();
        btnConsultarDni.setDisable(!dniHabilitado || cmbTipoDocumento.getValue() != TipoDocumento.DNI
                || !n.matches("\\d{8}") || !sesionUsuario.tiene(Permiso.PACIENTE_EDITAR));
    }

    /** RF-PAC-06: completa los nombres desde la API de DNI; si falla, se siguen escribiendo a mano. */
    @FXML
    private void onConsultarDni() {
        String dni = txtNumeroDocumento.getText().trim();
        tareaUi.ejecutar(btnConsultarDni, () -> integracionService.consultarDni(dni), resultado -> {
            if (resultado.isEmpty()) {
                notificador.info("La API no encontró el DNI " + dni + ": ingrese los datos a mano.");
                return;
            }
            PersonaDniDto p = resultado.get();
            txtNombres.setText(texto(p.nombres()));
            txtApellidoPaterno.setText(texto(p.apellidoPaterno()));
            txtApellidoMaterno.setText(texto(p.apellidoMaterno()));
            notificador.exito("Datos completados desde la consulta de DNI.");
        });
    }

    /** Llena el formulario con el paciente (o lo limpia si es {@code null}). */
    public void cargar(PacienteDetalle p) {
        pacienteId = p == null ? null : p.id();
        cmbTipoDocumento.setValue(p == null ? TipoDocumento.DNI : p.tipoDocumento());
        txtNumeroDocumento.setText(p == null ? "" : texto(p.numeroDocumento()));
        txtNombres.setText(p == null ? "" : p.nombres());
        txtApellidoPaterno.setText(p == null ? "" : p.apellidoPaterno());
        txtApellidoMaterno.setText(p == null ? "" : texto(p.apellidoMaterno()));
        dpFechaNacimiento.setValue(p == null ? null : p.fechaNacimiento());
        cmbSexo.setValue(p == null ? null : p.sexo());
        txtOcupacion.setText(p == null ? "" : texto(p.ocupacion()));
        txtTelefono.setText(p == null ? "" : texto(p.telefono()));
        txtEmail.setText(p == null ? "" : texto(p.email()));
        txtDireccion.setText(p == null ? "" : texto(p.direccion()));
        txtContactoNombre.setText(p == null ? "" : texto(p.contactoEmergenciaNombre()));
        txtContactoTelefono.setText(p == null ? "" : texto(p.contactoEmergenciaTelefono()));
        chkRecordatorios.setSelected(p == null || p.aceptaRecordatorios());
        txtApoderadoNombre.setText(p == null ? "" : texto(p.apoderadoNombre()));
        txtApoderadoDocumento.setText(p == null ? "" : texto(p.apoderadoDocumento()));
        cmbParentesco.setValue(p == null ? null : p.apoderadoParentesco());
    }

    public void enfocar() {
        Platform.runLater(txtNumeroDocumento::requestFocus);
    }

    @FXML
    private void onGuardar() {
        LocalDate fechaNacimiento;
        try {
            fechaNacimiento = Formularios.fecha(dpFechaNacimiento);
        } catch (NegocioException e) {
            tareaUi.manejarError(e);
            return;
        }
        PacienteForm form = new PacienteForm(pacienteId, cmbTipoDocumento.getValue(), txtNumeroDocumento.getText(),
                txtNombres.getText(), txtApellidoPaterno.getText(), txtApellidoMaterno.getText(),
                fechaNacimiento, cmbSexo.getValue(), txtTelefono.getText(), txtEmail.getText(),
                txtDireccion.getText(), txtOcupacion.getText(), txtContactoNombre.getText(),
                txtContactoTelefono.getText(), txtApoderadoNombre.getText(), txtApoderadoDocumento.getText(),
                cmbParentesco.getEditor().getText(), chkRecordatorios.isSelected());
        tareaUi.ejecutar(btnGuardar, () -> pacienteService.guardar(form), id -> {
            pacienteId = id;
            alGuardar.accept(id);
        });
    }

    @FXML
    private void onCancelar() {
        if (alCancelar != null) {
            alCancelar.run();
        }
    }

    private void actualizarEdad(LocalDate fecha) {
        LocalDate hoy = LocalDate.now();
        boolean valida = fecha != null && !fecha.isAfter(hoy);
        int edad = valida ? Validaciones.edad(fecha, hoy) : 0;
        lblEdad.setText(valida ? edad + (edad == 1 ? " año" : " años") : "");
        boolean menor = valida && edad < Validaciones.MAYORIA_DE_EDAD;
        seccionApoderado.setVisible(menor);
        seccionApoderado.setManaged(menor);
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor;
    }
}
