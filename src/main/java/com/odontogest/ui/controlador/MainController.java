/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.controlador;

import atlantafx.base.controls.ModalPane;
import atlantafx.base.theme.Styles;
import com.odontogest.evento.NavegarA;
import com.odontogest.modelo.Rol;
import com.odontogest.seguridad.Permiso;
import com.odontogest.seguridad.SesionUsuario;
import com.odontogest.servicio.AutenticacionService;
import com.odontogest.servicio.ConfiguracionService;
import com.odontogest.servicio.UsuarioService;
import com.odontogest.servicio.dto.UsuarioSesion;
import com.odontogest.ui.soporte.CargadorVistas;
import com.odontogest.ui.soporte.Formularios;
import com.odontogest.ui.soporte.Notificador;
import com.odontogest.ui.soporte.PacienteActivo;
import com.odontogest.ui.soporte.Refrescable;
import com.odontogest.ui.soporte.TareaUi;
import com.odontogest.ui.soporte.Tema;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ventana principal: menú lateral según permisos, barra superior con el usuario y área de contenido.
 * <p>
 * Registra sus contenedores en {@link Notificador} (toasts y panel lateral) y atiende la navegación
 * desacoplada con el evento {@link NavegarA}.
 */
@Component
@RequiredArgsConstructor
public class MainController {

    public static final String VISTA_PACIENTES = "pacientes-view";
    public static final String VISTA_FICHA = "paciente-ficha-view";
    public static final String VISTA_CAJA = "caja-view";
    public static final String VISTA_AGENDA = "agenda-view";

    /** Entrada del menú lateral; {@code permiso == null} = visible para todos. */
    private record ItemMenu(String vista, String titulo, Ikon icono, Permiso permiso) {
    }

    private static final List<ItemMenu> MENU = List.of(
            new ItemMenu(null, "Inicio", Feather.HOME, null),
            new ItemMenu(VISTA_AGENDA, "Agenda", Feather.CALENDAR, Permiso.AGENDA_GESTIONAR),
            new ItemMenu(VISTA_PACIENTES, "Pacientes", Feather.USERS, Permiso.PACIENTE_VER),
            new ItemMenu(VISTA_CAJA, "Caja", Feather.DOLLAR_SIGN, Permiso.PAGO_REGISTRAR),
            new ItemMenu("catalogos-view", "Catálogos", Feather.BOOK_OPEN, Permiso.CATALOGOS_EDITAR),
            new ItemMenu("usuarios-view", "Usuarios", Feather.USER_CHECK, Permiso.USUARIOS));

    private final SesionUsuario sesionUsuario;
    private final AutenticacionService autenticacionService;
    private final ConfiguracionService configuracionService;
    private final UsuarioService usuarioService;
    private final CargadorVistas cargadorVistas;
    private final Notificador notificador;
    private final TareaUi tareaUi;
    private final PacienteActivo pacienteActivo;
    private final PacienteFichaController fichaController;
    private final CajaController cajaController;
    private final AgendaController agendaController;

    @FXML private StackPane raiz;
    @FXML private ModalPane modal;
    @FXML private VBox zonaToasts;
    @FXML private VBox menu;
    @FXML private StackPane contenido;
    @FXML private VBox bienvenida;
    @FXML private Label lblTitulo;
    @FXML private Label lblUsuario;
    @FXML private Label lblRol;
    @FXML private Label lblBienvenida;
    @FXML private Button btnTema;

    private final ToggleGroup grupoMenu = new ToggleGroup();
    private final Map<String, ToggleButton> botonesMenu = new HashMap<>();

    @FXML
    public void initialize() {
        notificador.registrar(raiz, modal, zonaToasts);
        UsuarioSesion usuario = sesionUsuario.exigirUsuario();
        lblUsuario.setText(usuario.nombreCompleto());
        lblRol.setText(Rol.valueOf(usuario.rol()).etiqueta);
        lblBienvenida.setText("Bienvenido(a), " + usuario.nombreCompleto());
        actualizarBotonTema();
        construirMenu();
    }

    private void construirMenu() {
        menu.getChildren().clear();
        botonesMenu.clear();
        grupoMenu.getToggles().clear();
        for (ItemMenu item : MENU) {
            if (item.permiso() != null && !sesionUsuario.tiene(item.permiso())) {
                continue;
            }
            ToggleButton b = new ToggleButton(item.titulo(), new FontIcon(item.icono()));
            b.setToggleGroup(grupoMenu);
            b.getStyleClass().add("menu-item");
            b.setMaxWidth(Double.MAX_VALUE);
            b.setOnAction(e -> {
                b.setSelected(true);       // un clic sobre el elemento activo no lo deselecciona
                if (item.vista() == null) {
                    mostrarInicio();
                } else {
                    mostrar(item.vista(), item.titulo());
                }
            });
            botonesMenu.put(item.vista() == null ? "" : item.vista(), b);
            menu.getChildren().add(b);
        }
        botonesMenu.get("").setSelected(true);
    }

    /** Muestra una vista en el área de contenido; si es {@link Refrescable}, recarga sus datos. */
    public void mostrar(String vista, String titulo) {
        CargadorVistas.Vista cargada = cargadorVistas.cargar(vista);
        contenido.getChildren().setAll(cargada.raiz());
        lblTitulo.setText(titulo);
        // La ficha no está en el menú: se marca "Pacientes".
        ToggleButton b = botonesMenu.get(VISTA_FICHA.equals(vista) ? VISTA_PACIENTES : vista);
        if (b != null) {
            b.setSelected(true);
        }
        if (cargada.controlador() instanceof Refrescable refrescable) {
            refrescable.refrescar();
        }
    }

    private void mostrarInicio() {
        contenido.getChildren().setAll(bienvenida);
        lblTitulo.setText("Inicio");
    }

    /** Navegación pedida por otros controladores; puede llegar desde cualquier hilo. */
    @EventListener
    public void alNavegar(NavegarA evento) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> alNavegar(evento));
            return;
        }
        if (VISTA_FICHA.equals(evento.vista())) {
            Object id = evento.parametros().get("pacienteId");
            if (id instanceof Number n) {
                pacienteActivo.abrir(n.longValue());
            }
            mostrar(VISTA_FICHA, "Ficha del paciente");
            if (evento.parametros().get("pestana") instanceof String pestana) {
                fichaController.seleccionarPestana(pestana);
            }
        } else if (VISTA_PACIENTES.equals(evento.vista())) {
            mostrar(VISTA_PACIENTES, "Pacientes");
        } else if (VISTA_AGENDA.equals(evento.vista()) && sesionUsuario.tiene(Permiso.AGENDA_GESTIONAR)) {
            mostrar(VISTA_AGENDA, "Agenda");
            if (evento.parametros().get("pacienteId") instanceof Number n) {
                agendaController.nuevaCitaPara(n.longValue());
            }
        } else if (VISTA_CAJA.equals(evento.vista()) && sesionUsuario.tiene(Permiso.PAGO_REGISTRAR)) {
            mostrar(VISTA_CAJA, "Caja");
            if (evento.parametros().get("pacienteId") instanceof Number n) {
                cajaController.seleccionarPaciente(n.longValue());
            }
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
    private void onCambiarPassword() {
        PasswordField actual = new PasswordField();
        PasswordField nueva = new PasswordField();
        PasswordField confirmacion = new PasswordField();
        GridPane g = Formularios.grilla();
        Formularios.fila(g, "Contraseña actual", actual);
        Formularios.fila(g, "Nueva contraseña", nueva);
        Formularios.fila(g, "Confirmar", confirmacion);
        Label ayuda = new Label("Mínimo 8 caracteres, con letras y números.");
        ayuda.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);
        Button guardar = Formularios.boton("Cambiar", Feather.CHECK, Styles.ACCENT);
        guardar.setDefaultButton(true);
        Button cancelar = Formularios.boton("Cancelar", null);
        cancelar.setCancelButton(true);
        cancelar.setOnAction(e -> notificador.cerrarPanel());
        guardar.setOnAction(e -> tareaUi.ejecutar(guardar, () -> {
            usuarioService.cambiarPassword(actual.getText(), nueva.getText(), confirmacion.getText());
            return null;
        }, r -> {
            notificador.cerrarPanel();
            notificador.exito("Contraseña actualizada.");
        }));
        Node panel = Formularios.panelLateral("Cambiar contraseña", new VBox(12, g, ayuda), cancelar, guardar);
        notificador.abrirPanel(panel);
        Platform.runLater(actual::requestFocus);
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
            pacienteActivo.cerrar();
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
