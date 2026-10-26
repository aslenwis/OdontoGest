package com.odontogest.ui.controlador;

import atlantafx.base.controls.ModalPane;
import atlantafx.base.theme.Styles;
import com.odontogest.evento.ConectividadCambiadaASEJ;
import com.odontogest.evento.NavegarAASEJ;
import com.odontogest.modelo.RolASEJ;
import com.odontogest.seguridad.PermisoASEJ;
import com.odontogest.seguridad.SesionUsuarioASEJ;
import com.odontogest.servicio.AutenticacionServiceASEJ;
import com.odontogest.servicio.ConfiguracionServiceASEJ;
import com.odontogest.servicio.IntegracionServiceASEJ;
import com.odontogest.servicio.UsuarioServiceASEJ;
import com.odontogest.servicio.dto.UsuarioSesionASEJ;
import com.odontogest.ui.soporte.CargadorVistasASEJ;
import com.odontogest.ui.soporte.FormulariosASEJ;
import com.odontogest.ui.soporte.HiloUiASEJ;
import com.odontogest.ui.soporte.NotificadorASEJ;
import com.odontogest.ui.soporte.PacienteActivoASEJ;
import com.odontogest.ui.soporte.RefrescableASEJ;
import com.odontogest.ui.soporte.TareaUiASEJ;
import com.odontogest.ui.soporte.TemaASEJ;
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

@Component
@RequiredArgsConstructor
public class MainControllerASEJ {

    public static final String VISTA_PACIENTESASEJ = "pacientes-view";
    public static final String VISTA_FICHAASEJ = "paciente-ficha-view";
    public static final String VISTA_CAJAASEJ = "caja-view";
    public static final String VISTA_AGENDAASEJ = "agenda-view";
    public static final String VISTA_DASHBOARDASEJ = "dashboard-view";
    public static final String VISTA_REPORTESASEJ = "reportes-view";

    private record ItemMenuASEJ(String vistaASEJ, String tituloASEJ, Ikon iconoASEJ, PermisoASEJ permisoASEJ) {
    }

    private static final List<ItemMenuASEJ> MENUASEJ = List.of(
            new ItemMenuASEJ(VISTA_DASHBOARDASEJ, "Inicio", Feather.HOME, null),
            new ItemMenuASEJ(VISTA_AGENDAASEJ, "Agenda", Feather.CALENDAR, PermisoASEJ.AGENDA_GESTIONAR),
            new ItemMenuASEJ(VISTA_PACIENTESASEJ, "Pacientes", Feather.USERS, PermisoASEJ.PACIENTE_VER),
            new ItemMenuASEJ(VISTA_CAJAASEJ, "Caja", Feather.DOLLAR_SIGN, PermisoASEJ.PAGO_REGISTRAR),
            new ItemMenuASEJ(VISTA_REPORTESASEJ, "Reportes", Feather.BAR_CHART_2, PermisoASEJ.REPORTES_VER),
            new ItemMenuASEJ("catalogos-view", "Catálogos", Feather.BOOK_OPEN, PermisoASEJ.CATALOGOS_EDITAR),
            new ItemMenuASEJ("usuarios-view", "Usuarios", Feather.USER_CHECK, PermisoASEJ.USUARIOS),
            new ItemMenuASEJ("configuracion-view", "Configuración", Feather.SETTINGS, PermisoASEJ.CONFIGURACION));

    private final SesionUsuarioASEJ sesionUsuarioASEJ;
    private final AutenticacionServiceASEJ autenticacionServiceASEJ;
    private final ConfiguracionServiceASEJ configuracionServiceASEJ;
    private final UsuarioServiceASEJ usuarioServiceASEJ;
    private final CargadorVistasASEJ cargadorVistasASEJ;
    private final NotificadorASEJ notificadorASEJ;
    private final TareaUiASEJ tareaUiASEJ;
    private final PacienteActivoASEJ pacienteActivoASEJ;
    private final PacienteFichaControllerASEJ fichaControllerASEJ;
    private final CajaControllerASEJ cajaControllerASEJ;
    private final AgendaControllerASEJ agendaControllerASEJ;
    private final ReportesControllerASEJ reportesControllerASEJ;
    private final IntegracionServiceASEJ integracionServiceASEJ;

    @FXML private StackPane raizASEJ;
    @FXML private ModalPane modalASEJ;
    @FXML private VBox zonaToastsASEJ;
    @FXML private VBox menuASEJ;
    @FXML private StackPane contenidoASEJ;
    @FXML private Label lblTituloASEJ;
    @FXML private Label lblUsuarioASEJ;
    @FXML private Label lblRolASEJ;
    @FXML private Button btnTemaASEJ;
    @FXML private Label lblSinInternetASEJ;

    private final ToggleGroup grupoMenuASEJ = new ToggleGroup();
    private final Map<String, ToggleButton> botonesMenuASEJ = new HashMap<>();

    @FXML
    public void initialize() {
        notificadorASEJ.registrarASEJ(raizASEJ, modalASEJ, zonaToastsASEJ);
        UsuarioSesionASEJ usuarioASEJ = sesionUsuarioASEJ.exigirUsuarioASEJ();
        lblUsuarioASEJ.setText(usuarioASEJ.nombreCompletoASEJ());
        lblRolASEJ.setText(RolASEJ.valueOf(usuarioASEJ.rolASEJ()).etiquetaASEJ);
        actualizarBotonTemaASEJ();
        construirMenuASEJ();
        mostrarConectividadASEJ(integracionServiceASEJ.enLineaASEJ());
        Platform.runLater(() -> mostrarASEJ(VISTA_DASHBOARDASEJ, "Inicio"));
    }

    @EventListener
    public void alCambiarConectividadASEJ(ConectividadCambiadaASEJ eASEJ) {
        HiloUiASEJ.ejecutarASEJ(() -> {
            if (lblSinInternetASEJ != null) {
                mostrarConectividadASEJ(eASEJ.enLineaASEJ());
            }
        });
    }

    private void mostrarConectividadASEJ(boolean enLineaASEJ) {
        lblSinInternetASEJ.setVisible(!enLineaASEJ);
        lblSinInternetASEJ.setManaged(!enLineaASEJ);
    }

    private void construirMenuASEJ() {
        menuASEJ.getChildren().clear();
        botonesMenuASEJ.clear();
        grupoMenuASEJ.getToggles().clear();
        for (ItemMenuASEJ itemASEJ : MENUASEJ) {
            if (itemASEJ.permisoASEJ() != null && !sesionUsuarioASEJ.tieneASEJ(itemASEJ.permisoASEJ())) {
                continue;
            }
            ToggleButton bASEJ = new ToggleButton(itemASEJ.tituloASEJ(), new FontIcon(itemASEJ.iconoASEJ()));
            bASEJ.setToggleGroup(grupoMenuASEJ);
            bASEJ.getStyleClass().add("menu-item");
            bASEJ.setMaxWidth(Double.MAX_VALUE);
            bASEJ.setOnAction(eASEJ -> {
                bASEJ.setSelected(true);
                mostrarASEJ(itemASEJ.vistaASEJ(), itemASEJ.tituloASEJ());
            });
            botonesMenuASEJ.put(itemASEJ.vistaASEJ(), bASEJ);
            menuASEJ.getChildren().add(bASEJ);
        }
        botonesMenuASEJ.get(VISTA_DASHBOARDASEJ).setSelected(true);
    }

    public void mostrarASEJ(String vistaASEJ, String tituloASEJ) {
        CargadorVistasASEJ.VistaASEJ cargadaASEJ = cargadorVistasASEJ.cargarASEJ(vistaASEJ);
        contenidoASEJ.getChildren().setAll(cargadaASEJ.raizASEJ());
        lblTituloASEJ.setText(tituloASEJ);
        ToggleButton bASEJ = botonesMenuASEJ.get(VISTA_FICHAASEJ.equals(vistaASEJ) ? VISTA_PACIENTESASEJ : vistaASEJ);
        if (bASEJ != null) {
            bASEJ.setSelected(true);
        }
        if (cargadaASEJ.controladorASEJ() instanceof RefrescableASEJ refrescableASEJ) {
            refrescableASEJ.refrescarASEJ();
        }
    }

    @EventListener
    public void alNavegarASEJ(NavegarAASEJ eventoASEJ) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> alNavegarASEJ(eventoASEJ));
            return;
        }
        if (VISTA_FICHAASEJ.equals(eventoASEJ.vistaASEJ())) {
            Object idASEJ = eventoASEJ.parametrosASEJ().get("pacienteId");
            if (idASEJ instanceof Number nASEJ) {
                pacienteActivoASEJ.abrirASEJ(nASEJ.longValue());
            }
            mostrarASEJ(VISTA_FICHAASEJ, "Ficha del paciente");
            if (eventoASEJ.parametrosASEJ().get("pestana") instanceof String pestanaASEJ) {
                fichaControllerASEJ.seleccionarPestanaASEJ(pestanaASEJ);
            }
        } else if (VISTA_PACIENTESASEJ.equals(eventoASEJ.vistaASEJ())) {
            mostrarASEJ(VISTA_PACIENTESASEJ, "Pacientes");
        } else if (VISTA_AGENDAASEJ.equals(eventoASEJ.vistaASEJ()) && sesionUsuarioASEJ.tieneASEJ(PermisoASEJ.AGENDA_GESTIONAR)) {
            mostrarASEJ(VISTA_AGENDAASEJ, "Agenda");
            if (eventoASEJ.parametrosASEJ().get("pacienteId") instanceof Number nASEJ) {
                agendaControllerASEJ.nuevaCitaParaASEJ(nASEJ.longValue());
            }
        } else if (VISTA_REPORTESASEJ.equals(eventoASEJ.vistaASEJ()) && sesionUsuarioASEJ.tieneASEJ(PermisoASEJ.REPORTES_VER)) {
            mostrarASEJ(VISTA_REPORTESASEJ, "Reportes");
            if (eventoASEJ.parametrosASEJ().get("pestana") instanceof String pestanaASEJ) {
                reportesControllerASEJ.seleccionarPestanaASEJ(pestanaASEJ);
            }
        } else if (VISTA_DASHBOARDASEJ.equals(eventoASEJ.vistaASEJ())) {
            mostrarASEJ(VISTA_DASHBOARDASEJ, "Inicio");
        } else if (VISTA_CAJAASEJ.equals(eventoASEJ.vistaASEJ()) && sesionUsuarioASEJ.tieneASEJ(PermisoASEJ.PAGO_REGISTRAR)) {
            mostrarASEJ(VISTA_CAJAASEJ, "Caja");
            if (eventoASEJ.parametrosASEJ().get("pacienteId") instanceof Number nASEJ) {
                cajaControllerASEJ.seleccionarPacienteASEJ(nASEJ.longValue());
            }
        }
    }

    @FXML
    private void onAlternarTemaASEJ() {
        TemaASEJ nuevoASEJ = TemaASEJ.actualASEJ().alternarASEJ();
        nuevoASEJ.aplicarASEJ();
        actualizarBotonTemaASEJ();
        tareaUiASEJ.ejecutarASEJ(() -> {
            configuracionServiceASEJ.guardarASEJ(Map.of("ui.tema", nuevoASEJ.name()));
            return null;
        }, rASEJ -> { });
    }

    @FXML
    private void onAcercaDeASEJ() {
        cargadorVistasASEJ.mostrarModalASEJ("acerca-de-view", "Acerca de OdontoGest");
    }

    @FXML
    private void onCambiarPasswordASEJ() {
        PasswordField actualASEJ = new PasswordField();
        PasswordField nuevaASEJ = new PasswordField();
        PasswordField confirmacionASEJ = new PasswordField();
        GridPane gASEJ = FormulariosASEJ.grillaASEJ();
        FormulariosASEJ.filaASEJ(gASEJ, "Contraseña actual", actualASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "Nueva contraseña", nuevaASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "Confirmar", confirmacionASEJ);
        Label ayudaASEJ = new Label("Mínimo 8 caracteres, con letras y números.");
        ayudaASEJ.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);
        Button guardarASEJ = FormulariosASEJ.botonASEJ("Cambiar", Feather.CHECK, Styles.ACCENT);
        guardarASEJ.setDefaultButton(true);
        Button cancelarASEJ = FormulariosASEJ.botonASEJ("Cancelar", null);
        cancelarASEJ.setCancelButton(true);
        cancelarASEJ.setOnAction(eASEJ -> notificadorASEJ.cerrarPanelASEJ());
        guardarASEJ.setOnAction(eASEJ -> tareaUiASEJ.ejecutarASEJ(guardarASEJ, () -> {
            usuarioServiceASEJ.cambiarPasswordASEJ(actualASEJ.getText(), nuevaASEJ.getText(), confirmacionASEJ.getText());
            return null;
        }, rASEJ -> {
            notificadorASEJ.cerrarPanelASEJ();
            notificadorASEJ.exitoASEJ("Contraseña actualizada.");
        }));
        Node panelASEJ = FormulariosASEJ.panelLateralASEJ("Cambiar contraseña", new VBox(12, gASEJ, ayudaASEJ), cancelarASEJ, guardarASEJ);
        notificadorASEJ.abrirPanelASEJ(panelASEJ);
        Platform.runLater(actualASEJ::requestFocus);
    }

    @FXML
    private void onCerrarSesionASEJ() {
        if (!notificadorASEJ.confirmarASEJ("Cerrar sesión", "¿Desea cerrar la sesión actual?")) {
            return;
        }
        tareaUiASEJ.ejecutarASEJ(() -> {
            autenticacionServiceASEJ.cerrarSesionASEJ();
            return null;
        }, rASEJ -> {
            pacienteActivoASEJ.cerrarASEJ();
            notificadorASEJ.desregistrarASEJ();
            cargadorVistasASEJ.mostrarLoginASEJ(cargadorVistasASEJ.ventanaASEJ());
        });
    }

    private void actualizarBotonTemaASEJ() {
        boolean oscuroASEJ = TemaASEJ.actualASEJ() == TemaASEJ.OSCURO;
        btnTemaASEJ.setText(oscuroASEJ ? "Tema claro" : "Tema oscuro");
        btnTemaASEJ.setGraphic(new FontIcon(oscuroASEJ ? Feather.SUN : Feather.MOON));
    }
}
