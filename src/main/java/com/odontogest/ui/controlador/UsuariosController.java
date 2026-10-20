/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.controlador;

import atlantafx.base.theme.Styles;
import com.odontogest.excepcion.NegocioException;
import com.odontogest.modelo.Rol;
import com.odontogest.servicio.OdontologoService;
import com.odontogest.servicio.UsuarioService;
import com.odontogest.servicio.dto.OdontologoDto;
import com.odontogest.servicio.dto.UsuarioDto;
import com.odontogest.servicio.dto.UsuarioForm;
import com.odontogest.ui.soporte.Formularios;
import com.odontogest.ui.soporte.Notificador;
import com.odontogest.ui.soporte.TareaUi;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Vista "Usuarios" (RF-SEG-03): alta y edición de usuarios, rol, vínculo con odontólogo, activación y
 * restablecimiento de contraseña (que también desbloquea al usuario).
 */
@Component
public class UsuariosController extends ListadoEditable<UsuarioDto> {

    private final UsuarioService usuarioService;
    private final OdontologoService odontologoService;

    @FXML private Button btnRestablecer;

    public UsuariosController(TareaUi tareaUi, Notificador notificador, UsuarioService usuarioService,
                              OdontologoService odontologoService) {
        super(tareaUi, notificador);
        this.usuarioService = usuarioService;
        this.odontologoService = odontologoService;
    }

    @Override
    protected void alInicializar() {
        btnRestablecer.disableProperty().bind(tabla.getSelectionModel().selectedItemProperty().isNull());
    }

    @Override
    protected void configurarColumnas() {
        Formularios.columnas(tabla,
                Formularios.columna("Usuario", UsuarioDto::username, 130),
                Formularios.columna("Nombre", UsuarioDto::nombreCompleto, 240),
                Formularios.columna("Rol", u -> u.rol().etiqueta, 120),
                Formularios.columna("Odontólogo", UsuarioDto::odontologoNombre, 220),
                Formularios.columna("Último acceso", u -> Formularios.fechaHora(u.ultimoAcceso()), 140),
                Formularios.columna("Estado", u -> !u.activo() ? "Inactivo" : u.bloqueado() ? "Bloqueado" : "Activo", 90));
    }

    @Override
    protected List<UsuarioDto> listar(boolean incluirInactivos) {
        return usuarioService.listar();
    }

    @Override
    protected void abrirFormulario(UsuarioDto u) {
        tareaUi.ejecutar(() -> odontologoService.listar(false), odontologos -> mostrarFormulario(u, odontologos));
    }

    private void mostrarFormulario(UsuarioDto u, List<OdontologoDto> odontologos) {
        boolean nuevo = u == null;
        TextField username = new TextField(nuevo ? "" : u.username());
        username.setDisable(!nuevo);
        TextField nombre = new TextField(nuevo ? "" : u.nombreCompleto());
        ComboBox<Rol> rol = new ComboBox<>();
        rol.getItems().setAll(Rol.values());
        rol.setConverter(Formularios.conversor(r -> r.etiqueta));
        rol.setValue(nuevo ? Rol.RECEPCION : u.rol());
        ComboBox<OdontologoDto> odontologo = new ComboBox<>();
        odontologo.getItems().setAll(odontologos);
        odontologo.setValue(nuevo || u.odontologoId() == null ? null
                : odontologos.stream().filter(o -> o.id() == u.odontologoId()).findFirst().orElse(null));
        odontologo.disableProperty().bind(rol.valueProperty().isNotEqualTo(Rol.ODONTOLOGO));
        rol.valueProperty().addListener((obs, a, b) -> {
            if (b == Rol.ODONTOLOGO && odontologo.getValue() != null && nombre.getText().isBlank()) {
                nombre.setText(odontologo.getValue().nombreCompleto());
            }
        });
        odontologo.valueProperty().addListener((obs, a, b) -> {
            if (b != null && nombre.getText().isBlank()) {
                nombre.setText(b.nombreCompleto());
            }
        });
        CheckBox activo = new CheckBox("Activo");
        activo.setSelected(nuevo || u.activo());
        PasswordField password = new PasswordField();
        PasswordField confirmacion = new PasswordField();

        GridPane g = Formularios.grilla();
        Formularios.fila(g, "Usuario *", username);
        Formularios.fila(g, "Nombre completo *", nombre);
        Formularios.fila(g, "Rol *", rol);
        Formularios.fila(g, "Odontólogo", odontologo);
        Formularios.fila(g, "", activo);
        if (nuevo) {
            Formularios.fila(g, "Contraseña *", password);
            Formularios.fila(g, "Confirmar *", confirmacion);
        }

        Long id = nuevo ? null : u.id();
        abrirPanel(nuevo ? "Nuevo usuario" : "Editar usuario", g, () -> {
            if (nuevo && !password.getText().equals(confirmacion.getText())) {
                throw new NegocioException("La confirmación no coincide con la contraseña.");
            }
            UsuarioForm form = new UsuarioForm(id, username.getText(), nombre.getText(), rol.getValue(),
                    rol.getValue() == Rol.ODONTOLOGO && odontologo.getValue() != null ? odontologo.getValue().id() : null,
                    activo.isSelected(), nuevo ? password.getText() : null);
            return () -> usuarioService.guardar(form);
        }, "Usuario guardado.");
    }

    @FXML
    private void onRestablecer() {
        UsuarioDto u = tabla.getSelectionModel().getSelectedItem();
        if (u == null) {
            return;
        }
        PasswordField nueva = new PasswordField();
        PasswordField confirmacion = new PasswordField();
        GridPane g = Formularios.grilla();
        Formularios.fila(g, "Nueva contraseña", nueva);
        Formularios.fila(g, "Confirmar", confirmacion);
        Label ayuda = new Label("Mínimo 8 caracteres, con letras y números. También desbloquea al usuario.");
        ayuda.setWrapText(true);
        ayuda.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);

        Button cancelar = Formularios.boton("Cancelar", null);
        cancelar.setCancelButton(true);
        cancelar.setOnAction(e -> notificador.cerrarPanel());
        Button guardar = Formularios.boton("Restablecer", Feather.UNLOCK, Styles.ACCENT);
        guardar.setOnAction(e -> {
            if (!nueva.getText().equals(confirmacion.getText())) {
                notificador.advertencia("La confirmación no coincide con la contraseña.");
                return;
            }
            tareaUi.ejecutar(guardar, () -> {
                usuarioService.restablecerPassword(u.id(), nueva.getText());
                return null;
            }, r -> {
                notificador.cerrarPanel();
                notificador.exito("Contraseña de " + u.username() + " restablecida.");
                refrescar();
            });
        });
        notificador.abrirPanel(Formularios.panelLateral("Restablecer contraseña de " + u.username(),
                new VBox(12, g, ayuda), cancelar, guardar));
    }
}
