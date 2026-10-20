/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.controlador;

import atlantafx.base.controls.ModalPane;
import com.odontogest.seguridad.SesionUsuario;
import com.odontogest.servicio.AutenticacionService;
import com.odontogest.servicio.ConfiguracionService;
import com.odontogest.servicio.dto.UsuarioSesion;
import com.odontogest.ui.soporte.CargadorVistas;
import com.odontogest.ui.soporte.Notificador;
import com.odontogest.ui.soporte.Refrescable;
import com.odontogest.ui.soporte.TareaUi;
import com.odontogest.ui.soporte.Tema;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Ventana principal: menú lateral, barra superior con el usuario y área de contenido.
 * <p>
 * Registra sus contenedores en {@link Notificador} (toasts y panel lateral). El menú se llena con
 * los módulos a partir de la Fase 2.
 */
@Component
@RequiredArgsConstructor
public class MainController {

    private final SesionUsuario sesionUsuario;
    private final AutenticacionService autenticacionService;
    private final ConfiguracionService configuracionService;
    private final CargadorVistas cargadorVistas;
    private final Notificador notificador;
    private final TareaUi tareaUi;

    @FXML private StackPane raiz;
    @FXML private ModalPane modal;
    @FXML private VBox zonaToasts;
    @FXML private VBox menu;
    @FXML private StackPane contenido;
    @FXML private Label lblTitulo;
    @FXML private Label lblUsuario;
    @FXML private Label lblRol;
    @FXML private Label lblBienvenida;
    @FXML private Button btnTema;

    @FXML
    public void initialize() {
        notificador.registrar(raiz, modal, zonaToasts);
        UsuarioSesion usuario = sesionUsuario.exigirUsuario();
        lblUsuario.setText(usuario.nombreCompleto());
        lblRol.setText(usuario.rol());
        lblBienvenida.setText("Bienvenido(a), " + usuario.nombreCompleto());
        actualizarBotonTema();
    }

    /** Muestra una vista en el área de contenido; si es {@link Refrescable}, recarga sus datos. */
    public void mostrar(String vista, String titulo) {
        CargadorVistas.Vista cargada = cargadorVistas.cargar(vista);
        contenido.getChildren().setAll(cargada.raiz());
        lblTitulo.setText(titulo);
        if (cargada.controlador() instanceof Refrescable refrescable) {
            refrescable.refrescar();
        }
    }

    @FXML
    private void onAlternarTema() {
        Tema nuevo = Tema.actual().alternar();
        nuevo.aplicar();
        actualizarBotonTema();
        tareaUi.ejecutar(() -> {
            configuracionService.guardar(Map.of("ui.tema", nuevo.name()));
            return null;
        }, r -> { });
    }

    @FXML
    private void onCerrarSesion() {
        if (!notificador.confirmar("Cerrar sesión", "¿Desea cerrar la sesión actual?")) {
            return;
        }
        tareaUi.ejecutar(() -> {
            autenticacionService.cerrarSesion();
            return null;
        }, r -> {
            notificador.desregistrar();
            cargadorVistas.mostrarLogin(cargadorVistas.ventana());
        });
    }

    private void actualizarBotonTema() {
        boolean oscuro = Tema.actual() == Tema.OSCURO;
        btnTema.setText(oscuro ? "Tema claro" : "Tema oscuro");
        btnTema.setGraphic(new FontIcon(oscuro ? Feather.SUN : Feather.MOON));
    }
}
