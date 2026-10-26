package com.odontogest.ui.controlador;

import atlantafx.base.theme.Styles;
import com.odontogest.excepcion.NegocioExceptionASEJ;
import com.odontogest.modelo.RolASEJ;
import com.odontogest.servicio.OdontologoServiceASEJ;
import com.odontogest.servicio.UsuarioServiceASEJ;
import com.odontogest.servicio.dto.OdontologoDtoASEJ;
import com.odontogest.servicio.dto.UsuarioDtoASEJ;
import com.odontogest.servicio.dto.UsuarioFormASEJ;
import com.odontogest.ui.soporte.FormulariosASEJ;
import com.odontogest.ui.soporte.NotificadorASEJ;
import com.odontogest.ui.soporte.TareaUiASEJ;
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

@Component
public class UsuariosControllerASEJ extends ListadoEditableASEJ<UsuarioDtoASEJ> {

    private final UsuarioServiceASEJ usuarioServiceASEJ;
    private final OdontologoServiceASEJ odontologoServiceASEJ;

    @FXML private Button btnRestablecerASEJ;

    public UsuariosControllerASEJ(TareaUiASEJ tareaUiASEJ, NotificadorASEJ notificadorASEJ, UsuarioServiceASEJ usuarioServiceASEJ,
                              OdontologoServiceASEJ odontologoServiceASEJ) {
        super(tareaUiASEJ, notificadorASEJ);
        this.usuarioServiceASEJ = usuarioServiceASEJ;
        this.odontologoServiceASEJ = odontologoServiceASEJ;
    }

    @Override
    protected void alInicializarASEJ() {
        btnRestablecerASEJ.disableProperty().bind(tablaASEJ.getSelectionModel().selectedItemProperty().isNull());
    }

    @Override
    protected void configurarColumnasASEJ() {
        FormulariosASEJ.columnasASEJ(tablaASEJ,
                FormulariosASEJ.columnaASEJ("Usuario", UsuarioDtoASEJ::usernameASEJ, 130),
                FormulariosASEJ.columnaASEJ("Nombre", UsuarioDtoASEJ::nombreCompletoASEJ, 240),
                FormulariosASEJ.columnaASEJ("Rol", uASEJ -> uASEJ.rolASEJ().etiquetaASEJ, 120),
                FormulariosASEJ.columnaASEJ("Odontólogo", UsuarioDtoASEJ::odontologoNombreASEJ, 220),
                FormulariosASEJ.columnaASEJ("Último acceso", uASEJ -> FormulariosASEJ.fechaHoraASEJ(uASEJ.ultimoAccesoASEJ()), 140),
                FormulariosASEJ.columnaASEJ("Estado", uASEJ -> !uASEJ.activoASEJ() ? "Inactivo" : uASEJ.bloqueadoASEJ() ? "Bloqueado" : "Activo", 90));
    }

    @Override
    protected List<UsuarioDtoASEJ> listarASEJ(boolean incluirInactivosASEJ) {
        return usuarioServiceASEJ.listarASEJ();
    }

    @Override
    protected void abrirFormularioASEJ(UsuarioDtoASEJ uASEJ) {
        tareaUiASEJ.ejecutarASEJ(() -> odontologoServiceASEJ.listarASEJ(false), odontologosASEJ -> mostrarFormularioASEJ(uASEJ, odontologosASEJ));
    }

    private void mostrarFormularioASEJ(UsuarioDtoASEJ uASEJ, List<OdontologoDtoASEJ> odontologosASEJ) {
        boolean nuevoASEJ = uASEJ == null;
        TextField usernameASEJ = new TextField(nuevoASEJ ? "" : uASEJ.usernameASEJ());
        usernameASEJ.setDisable(!nuevoASEJ);
        TextField nombreASEJ = new TextField(nuevoASEJ ? "" : uASEJ.nombreCompletoASEJ());
        ComboBox<RolASEJ> rolASEJ = new ComboBox<>();
        rolASEJ.getItems().setAll(RolASEJ.values());
        rolASEJ.setConverter(FormulariosASEJ.conversorASEJ(rASEJ -> rASEJ.etiquetaASEJ));
        rolASEJ.setValue(nuevoASEJ ? RolASEJ.RECEPCION : uASEJ.rolASEJ());
        ComboBox<OdontologoDtoASEJ> odontologoASEJ = new ComboBox<>();
        odontologoASEJ.getItems().setAll(odontologosASEJ);
        odontologoASEJ.setValue(nuevoASEJ || uASEJ.odontologoIdASEJ() == null ? null
                : odontologosASEJ.stream().filter(oASEJ -> oASEJ.idASEJ() == uASEJ.odontologoIdASEJ()).findFirst().orElse(null));
        odontologoASEJ.disableProperty().bind(rolASEJ.valueProperty().isNotEqualTo(RolASEJ.ODONTOLOGO));
        rolASEJ.valueProperty().addListener((obsASEJ, aASEJ, bASEJ) -> {
            if (bASEJ == RolASEJ.ODONTOLOGO && odontologoASEJ.getValue() != null && nombreASEJ.getText().isBlank()) {
                nombreASEJ.setText(odontologoASEJ.getValue().nombreCompletoASEJ());
            }
        });
        odontologoASEJ.valueProperty().addListener((obsASEJ, aASEJ, bASEJ) -> {
            if (bASEJ != null && nombreASEJ.getText().isBlank()) {
                nombreASEJ.setText(bASEJ.nombreCompletoASEJ());
            }
        });
        CheckBox activoASEJ = new CheckBox("Activo");
        activoASEJ.setSelected(nuevoASEJ || uASEJ.activoASEJ());
        PasswordField passwordASEJ = new PasswordField();
        PasswordField confirmacionASEJ = new PasswordField();

        GridPane gASEJ = FormulariosASEJ.grillaASEJ();
        FormulariosASEJ.filaASEJ(gASEJ, "Usuario *", usernameASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "Nombre completo *", nombreASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "Rol *", rolASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "Odontólogo", odontologoASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "", activoASEJ);
        if (nuevoASEJ) {
            FormulariosASEJ.filaASEJ(gASEJ, "Contraseña *", passwordASEJ);
            FormulariosASEJ.filaASEJ(gASEJ, "Confirmar *", confirmacionASEJ);
        }

        Long idASEJ = nuevoASEJ ? null : uASEJ.idASEJ();
        abrirPanelASEJ(nuevoASEJ ? "Nuevo usuario" : "Editar usuario", gASEJ, () -> {
            if (nuevoASEJ && !passwordASEJ.getText().equals(confirmacionASEJ.getText())) {
                throw new NegocioExceptionASEJ("La confirmación no coincide con la contraseña.");
            }
            UsuarioFormASEJ formASEJ = new UsuarioFormASEJ(idASEJ, usernameASEJ.getText(), nombreASEJ.getText(), rolASEJ.getValue(),
                    rolASEJ.getValue() == RolASEJ.ODONTOLOGO && odontologoASEJ.getValue() != null ? odontologoASEJ.getValue().idASEJ() : null,
                    activoASEJ.isSelected(), nuevoASEJ ? passwordASEJ.getText() : null);
            return () -> usuarioServiceASEJ.guardarASEJ(formASEJ);
        }, "Usuario guardado.");
    }

    @FXML
    private void onRestablecerASEJ() {
        UsuarioDtoASEJ uASEJ = tablaASEJ.getSelectionModel().getSelectedItem();
        if (uASEJ == null) {
            return;
        }
        PasswordField nuevaASEJ = new PasswordField();
        PasswordField confirmacionASEJ = new PasswordField();
        GridPane gASEJ = FormulariosASEJ.grillaASEJ();
        FormulariosASEJ.filaASEJ(gASEJ, "Nueva contraseña", nuevaASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "Confirmar", confirmacionASEJ);
        Label ayudaASEJ = new Label("Mínimo 8 caracteres, con letras y números. También desbloquea al usuario.");
        ayudaASEJ.setWrapText(true);
        ayudaASEJ.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);

        Button cancelarASEJ = FormulariosASEJ.botonASEJ("Cancelar", null);
        cancelarASEJ.setCancelButton(true);
        cancelarASEJ.setOnAction(eASEJ -> notificadorASEJ.cerrarPanelASEJ());
        Button guardarASEJ = FormulariosASEJ.botonASEJ("Restablecer", Feather.UNLOCK, Styles.ACCENT);
        guardarASEJ.setOnAction(eASEJ -> {
            if (!nuevaASEJ.getText().equals(confirmacionASEJ.getText())) {
                notificadorASEJ.advertenciaASEJ("La confirmación no coincide con la contraseña.");
                return;
            }
            tareaUiASEJ.ejecutarASEJ(guardarASEJ, () -> {
                usuarioServiceASEJ.restablecerPasswordASEJ(uASEJ.idASEJ(), nuevaASEJ.getText());
                return null;
            }, rASEJ -> {
                notificadorASEJ.cerrarPanelASEJ();
                notificadorASEJ.exitoASEJ("Contraseña de " + uASEJ.usernameASEJ() + " restablecida.");
                refrescarASEJ();
            });
        });
        notificadorASEJ.abrirPanelASEJ(FormulariosASEJ.panelLateralASEJ("Restablecer contraseña de " + uASEJ.usernameASEJ(),
                new VBox(12, gASEJ, ayudaASEJ), cancelarASEJ, guardarASEJ));
    }
}
