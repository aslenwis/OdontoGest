/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.controlador;

import atlantafx.base.theme.Styles;
import com.odontogest.servicio.SistemaService;
import com.odontogest.servicio.dto.InformacionSistemaDto;
import com.odontogest.servicio.dto.IntegridadBaseDto;
import com.odontogest.ui.soporte.Exportador;
import com.odontogest.ui.soporte.Formularios;
import com.odontogest.ui.soporte.TareaUi;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * Ventana "Acerca de" (PLAN §6.1): nombre, versión ({@code BuildProperties}), autores, curso, año, aviso de derechos
 * de autor, rutas de datos y del log, y el botón "Verificar integridad de la base de datos".
 */
@Component
@RequiredArgsConstructor
public class AcercaDeController {

    private final SistemaService sistemaService;
    private final TareaUi tareaUi;
    private final Exportador exportador;

    @FXML private Label lblNombre;
    @FXML private Label lblDescripcion;
    @FXML private Label lblVersion;
    @FXML private VBox boxAutores;
    @FXML private Label lblCurso;
    @FXML private Label lblUniversidad;
    @FXML private Label lblAnio;
    @FXML private Label lblJava;
    @FXML private Label lblAviso;
    @FXML private Hyperlink lnkDatos;
    @FXML private Label lblBase;
    @FXML private Hyperlink lnkLogs;
    @FXML private Button btnVerificar;
    @FXML private Label lblIntegridad;
    @FXML private Button btnCerrar;

    @FXML
    public void initialize() {
        lblNombre.setText(SistemaService.NOMBRE);
        lblDescripcion.setText(SistemaService.DESCRIPCION);
        tareaUi.ejecutar(sistemaService::informacion, this::mostrar);
    }

    private void mostrar(InformacionSistemaDto info) {
        lblVersion.setText(info.version() + (info.compilado() == null ? ""
                : "  ·  compilada el " + Formularios.fechaHora(info.compilado())));
        boxAutores.getChildren().setAll(info.autores().stream().map(Label::new).toList());
        lblCurso.setText(info.curso());
        lblUniversidad.setText(info.universidad());
        lblAnio.setText(String.valueOf(info.anio()));
        lblJava.setText(info.javaVersion() + "  ·  JavaFX " + System.getProperty("javafx.version", "—"));
        lblAviso.setText(info.aviso());
        lnkDatos.setText(info.carpetaDatos());
        lblBase.setText(info.baseDatos());
        lnkLogs.setText(info.carpetaLogs());
        ajustarVentana();
    }

    /** La ventana se crea antes de que lleguen los datos: se ajusta a su contenido cuando cambian. */
    private void ajustarVentana() {
        if (btnCerrar.getScene() != null && btnCerrar.getScene().getWindow() instanceof Stage ventana) {
            ventana.sizeToScene();
        }
    }

    @FXML
    private void onVerificar() {
        lblIntegridad.setText("Verificando…");
        lblIntegridad.getStyleClass().removeAll(Styles.SUCCESS, Styles.DANGER);
        lblIntegridad.setVisible(true);
        lblIntegridad.setManaged(true);
        ajustarVentana();
        tareaUi.ejecutar(btnVerificar, sistemaService::verificarIntegridad, this::mostrarIntegridad,
                error -> {
                    lblIntegridad.setText("");
                    tareaUi.manejarError(error);
                });
    }

    private void mostrarIntegridad(IntegridadBaseDto r) {
        String tamanio = r.tamanioBytes() < 0 ? "" : r.tamanioBytes() < 1024 * 1024
                ? String.format(" (%d KB)", r.tamanioBytes() / 1024)
                : String.format(" (%.1f MB)", r.tamanioBytes() / (1024.0 * 1024));
        if (r.correcta()) {
            lblIntegridad.setText("✔ La base de datos está íntegra" + tamanio + ": sin errores de estructura ni "
                    + "referencias rotas.");
            lblIntegridad.getStyleClass().add(Styles.SUCCESS);
            ajustarVentana();
            return;
        }
        StringBuilder sb = new StringBuilder("✖ Se encontraron problemas");
        if (!r.problemas().isEmpty()) {
            sb.append(": ").append(String.join("; ", r.problemas().subList(0, Math.min(3, r.problemas().size()))));
        }
        if (r.clavesForaneas() > 0) {
            sb.append(". ").append(r.clavesForaneas()).append(" referencia(s) a registros inexistentes");
        }
        sb.append(". Restaure el último respaldo (Configuración → Respaldo) y revise el log.");
        lblIntegridad.setText(sb.toString());
        lblIntegridad.getStyleClass().add(Styles.DANGER);
        ajustarVentana();
    }

    @FXML
    private void onAbrirDatos() {
        exportador.abrir(new File(lnkDatos.getText()));
    }

    @FXML
    private void onAbrirLogs() {
        exportador.abrir(new File(lnkLogs.getText()));
    }

    @FXML
    private void onCerrar() {
        ((Stage) btnCerrar.getScene().getWindow()).close();
    }
}
