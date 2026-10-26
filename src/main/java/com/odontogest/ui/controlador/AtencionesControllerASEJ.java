package com.odontogest.ui.controlador;

import atlantafx.base.theme.Styles;
import com.odontogest.dominio.odontograma.DenticionASEJ;
import com.odontogest.evento.NavegarAASEJ;
import com.odontogest.evento.PlanCambiadoASEJ;
import com.odontogest.evento.SolicitudNuevaAtencionASEJ;
import com.odontogest.modelo.EstadoAtencionASEJ;
import com.odontogest.modelo.RolASEJ;
import com.odontogest.seguridad.PermisoASEJ;
import com.odontogest.seguridad.SesionUsuarioASEJ;
import com.odontogest.servicio.AtencionServiceASEJ;
import com.odontogest.servicio.OdontologoServiceASEJ;
import com.odontogest.servicio.PlanTratamientoServiceASEJ;
import com.odontogest.servicio.dto.AtencionDetalleASEJ;
import com.odontogest.servicio.dto.AtencionFormASEJ;
import com.odontogest.servicio.dto.AtencionResumenASEJ;
import com.odontogest.servicio.dto.ItemAtencionDtoASEJ;
import com.odontogest.servicio.dto.OdontologoDtoASEJ;
import com.odontogest.servicio.dto.PacienteDetalleASEJ;
import com.odontogest.servicio.dto.UsuarioSesionASEJ;
import com.odontogest.ui.soporte.FormulariosASEJ;
import com.odontogest.ui.soporte.HiloUiASEJ;
import com.odontogest.ui.soporte.NotificadorASEJ;
import com.odontogest.ui.soporte.PacienteActivoASEJ;
import com.odontogest.ui.soporte.TareaUiASEJ;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AtencionesControllerASEJ {

    private final AtencionServiceASEJ atencionServiceASEJ;
    private final OdontologoServiceASEJ odontologoServiceASEJ;
    private final PlanTratamientoServiceASEJ planServiceASEJ;
    private final OdontogramaControllerASEJ odontogramaControllerASEJ;
    private final PacienteActivoASEJ pacienteActivoASEJ;
    private final TareaUiASEJ tareaUiASEJ;
    private final NotificadorASEJ notificadorASEJ;
    private final SesionUsuarioASEJ sesionUsuarioASEJ;
    private final ApplicationEventPublisher eventosASEJ;

    @FXML private Button btnNuevaASEJ;
    @FXML private ListView<AtencionResumenASEJ> lstAtencionesASEJ;
    @FXML private VBox panelDetalleASEJ;
    @FXML private Label lblTituloASEJ;
    @FXML private Label lblDetalleASEJ;
    @FXML private TextArea txtMotivoASEJ;
    @FXML private TextArea txtExamenASEJ;
    @FXML private TextArea txtDiagnosticoASEJ;
    @FXML private TextArea txtObservacionesASEJ;
    @FXML private Button btnOdontogramaASEJ;
    @FXML private Button btnCerrarASEJ;
    @FXML private Button btnGuardarASEJ;
    @FXML private Label lblItemsASEJ;
    @FXML private VBox boxItemsASEJ;

    private ChangeListener<PacienteDetalleASEJ> oyenteASEJ;
    private AtencionDetalleASEJ actualASEJ;
    private boolean avisandoASEJ;

    @FXML
    public void initialize() {
        btnNuevaASEJ.setDisable(!sesionUsuarioASEJ.tieneASEJ(PermisoASEJ.ATENCION_GESTIONAR));
        lstAtencionesASEJ.setCellFactory(lASEJ -> new ListCell<>() {
            @Override
            protected void updateItem(AtencionResumenASEJ aASEJ, boolean vacioASEJ) {
                super.updateItem(aASEJ, vacioASEJ);
                if (vacioASEJ || aASEJ == null) {
                    setText(null);
                    return;
                }
                setText(FormulariosASEJ.fechaHoraASEJ(aASEJ.fechaASEJ()) + (aASEJ.estadoASEJ() == EstadoAtencionASEJ.ABIERTA ? "  · ABIERTA" : "")
                        + "\n" + aASEJ.motivoConsultaASEJ().replaceAll("\\s+", " ") + "\n" + aASEJ.odontologoNombreASEJ());
            }
        });
        lstAtencionesASEJ.getSelectionModel().selectedItemProperty().addListener((obsASEJ, aASEJ, bASEJ) -> {
            if (bASEJ != null) {
                tareaUiASEJ.ejecutarASEJ(() -> atencionServiceASEJ.obtenerASEJ(bASEJ.idASEJ()), this::mostrarASEJ);
            }
        });
        mostrarASEJ(null);

        if (oyenteASEJ != null) {
            pacienteActivoASEJ.pacientePropertyASEJ().removeListener(oyenteASEJ);
        }
        oyenteASEJ = (obsASEJ, anteriorASEJ, nuevoASEJ) -> recargarASEJ(null);
        pacienteActivoASEJ.pacientePropertyASEJ().addListener(oyenteASEJ);
        recargarASEJ(null);
    }

    public void refrescarASEJ() {
        recargarASEJ(null);
    }

    private void recargarASEJ(Long seleccionarASEJ) {
        PacienteDetalleASEJ pASEJ = pacienteActivoASEJ.getASEJ();
        if (pASEJ == null || lstAtencionesASEJ == null) {
            return;
        }
        tareaUiASEJ.ejecutarASEJ(() -> atencionServiceASEJ.listarPorPacienteASEJ(pASEJ.idASEJ()), listaASEJ -> {
            lstAtencionesASEJ.getItems().setAll(listaASEJ);
            AtencionResumenASEJ elegidoASEJ = listaASEJ.stream().filter(aASEJ -> seleccionarASEJ != null && aASEJ.idASEJ() == seleccionarASEJ).findFirst()
                    .orElse(listaASEJ.isEmpty() ? null : listaASEJ.get(0));
            if (elegidoASEJ == null) {
                mostrarASEJ(null);
            } else {
                lstAtencionesASEJ.getSelectionModel().select(elegidoASEJ);
            }
        });
    }

    private void mostrarASEJ(AtencionDetalleASEJ aASEJ) {
        actualASEJ = aASEJ;
        boolean hayASEJ = aASEJ != null;
        panelDetalleASEJ.setDisable(!hayASEJ);
        lblTituloASEJ.setText(hayASEJ ? "Atención del " + FormulariosASEJ.fechaHoraASEJ(aASEJ.fechaASEJ()) : "Sin atenciones registradas");
        lblDetalleASEJ.setText(hayASEJ ? aASEJ.odontologoNombreASEJ() + " · " + (aASEJ.abiertaASEJ() ? "Abierta"
                : "Cerrada el " + FormulariosASEJ.fechaHoraASEJ(aASEJ.cerradaEnASEJ()) + " (solo lectura)") : "");
        txtMotivoASEJ.setText(hayASEJ ? textoASEJ(aASEJ.motivoConsultaASEJ()) : "");
        txtExamenASEJ.setText(hayASEJ ? textoASEJ(aASEJ.examenClinicoASEJ()) : "");
        txtDiagnosticoASEJ.setText(hayASEJ ? textoASEJ(aASEJ.diagnosticoASEJ()) : "");
        txtObservacionesASEJ.setText(hayASEJ ? textoASEJ(aASEJ.observacionesASEJ()) : "");
        boolean editableASEJ = hayASEJ && aASEJ.abiertaASEJ() && sesionUsuarioASEJ.tieneASEJ(PermisoASEJ.ATENCION_GESTIONAR);
        for (TextArea tASEJ : List.of(txtMotivoASEJ, txtExamenASEJ, txtDiagnosticoASEJ, txtObservacionesASEJ)) {
            tASEJ.setEditable(editableASEJ);
        }
        btnGuardarASEJ.setDisable(!editableASEJ);
        btnCerrarASEJ.setDisable(!editableASEJ);
        btnOdontogramaASEJ.setDisable(!hayASEJ);
        cargarItemsASEJ();
    }

    private void cargarItemsASEJ() {
        AtencionDetalleASEJ aASEJ = actualASEJ;
        if (aASEJ == null) {
            mostrarItemsASEJ(null, List.of());
            return;
        }
        tareaUiASEJ.ejecutarASEJ(() -> planServiceASEJ.itemsParaAtencionASEJ(aASEJ.idASEJ()), listaASEJ -> mostrarItemsASEJ(aASEJ, listaASEJ));
    }

    private void mostrarItemsASEJ(AtencionDetalleASEJ aASEJ, List<ItemAtencionDtoASEJ> itemsASEJ) {
        if (aASEJ != actualASEJ) {
            return;
        }
        boxItemsASEJ.getChildren().clear();
        boolean puedeASEJ = sesionUsuarioASEJ.tieneASEJ(PermisoASEJ.ATENCION_GESTIONAR);
        for (ItemAtencionDtoASEJ iASEJ : itemsASEJ) {
            CheckBox cASEJ = new CheckBox(iASEJ.descripcionASEJ() + "  ·  " + iASEJ.faseASEJ());
            cASEJ.setWrapText(true);
            cASEJ.setSelected(iASEJ.realizadoAquiASEJ());
            cASEJ.setDisable(!iASEJ.editableASEJ() || !puedeASEJ);
            cASEJ.setOnAction(eASEJ -> {
                boolean realizadoASEJ = cASEJ.isSelected();
                tareaUiASEJ.ejecutarASEJ(cASEJ, () -> planServiceASEJ.marcarEnAtencionASEJ(aASEJ.idASEJ(), iASEJ.itemIdASEJ(), realizadoASEJ), listaASEJ -> {
                    mostrarItemsASEJ(aASEJ, listaASEJ);
                    avisarCambioDePlanASEJ(aASEJ.pacienteIdASEJ());
                }, errorASEJ -> {
                    tareaUiASEJ.manejarErrorASEJ(errorASEJ);
                    cargarItemsASEJ();
                });
            });
            boxItemsASEJ.getChildren().add(cASEJ);
        }
        lblItemsASEJ.setText(aASEJ == null ? "" : itemsASEJ.isEmpty()
                ? "El paciente no tiene planes aceptados con procedimientos pendientes."
                : aASEJ.abiertaASEJ() ? "Marque los procedimientos realizados hoy; el primero pasa el plan a «en curso»."
                : "Atención cerrada: solo lectura.");
    }

    @EventListener
    public void alCambiarPlanASEJ(PlanCambiadoASEJ eASEJ) {
        if (avisandoASEJ) {
            return;
        }
        Runnable rASEJ = () -> {
            if (actualASEJ != null && actualASEJ.pacienteIdASEJ() == eASEJ.pacienteIdASEJ() && boxItemsASEJ != null) {
                cargarItemsASEJ();
            }
        };
        HiloUiASEJ.ejecutarASEJ(rASEJ);
    }

    @EventListener
    public void alSolicitarNuevaAtencionASEJ(SolicitudNuevaAtencionASEJ solicitudASEJ) {
        HiloUiASEJ.ejecutarASEJ(this::onNuevaASEJ);
    }

    @FXML
    private void onNuevaASEJ() {
        PacienteDetalleASEJ pASEJ = pacienteActivoASEJ.getASEJ();
        if (pASEJ == null || !sesionUsuarioASEJ.tieneASEJ(PermisoASEJ.ATENCION_GESTIONAR)) {
            return;
        }
        tareaUiASEJ.ejecutarASEJ(() -> odontologoServiceASEJ.listarASEJ(false), odontologosASEJ -> formularioNuevaASEJ(pASEJ, odontologosASEJ));
    }

    private void formularioNuevaASEJ(PacienteDetalleASEJ pASEJ, List<OdontologoDtoASEJ> odontologosASEJ) {
        UsuarioSesionASEJ usuarioASEJ = sesionUsuarioASEJ.exigirUsuarioASEJ();
        boolean esOdontologoASEJ = RolASEJ.valueOf(usuarioASEJ.rolASEJ()) == RolASEJ.ODONTOLOGO;
        ComboBox<OdontologoDtoASEJ> odontologoASEJ = new ComboBox<>();
        odontologoASEJ.getItems().setAll(odontologosASEJ);
        odontologoASEJ.setValue(odontologosASEJ.stream()
                .filter(oASEJ -> esOdontologoASEJ && usuarioASEJ.odontologoIdASEJ() != null && oASEJ.idASEJ() == usuarioASEJ.odontologoIdASEJ())
                .findFirst().orElse(odontologosASEJ.size() == 1 ? odontologosASEJ.get(0) : null));
        odontologoASEJ.setDisable(esOdontologoASEJ);
        TextArea motivoASEJ = new TextArea();
        motivoASEJ.setPrefRowCount(3);
        motivoASEJ.setWrapText(true);
        motivoASEJ.setPromptText("Dolor, control, limpieza…");
        ComboBox<DenticionASEJ> denticionASEJ = new ComboBox<>();
        denticionASEJ.getItems().setAll(DenticionASEJ.values());
        denticionASEJ.setConverter(FormulariosASEJ.conversorASEJ(dASEJ -> switch (dASEJ) {
            case PERMANENTE -> "Permanente";
            case DECIDUA -> "Decidua";
            case MIXTA -> "Mixta";
        }));
        denticionASEJ.setValue(DenticionASEJ.sugeridaASEJ(pASEJ.edadASEJ()));

        GridPane gASEJ = FormulariosASEJ.grillaASEJ();
        FormulariosASEJ.filaASEJ(gASEJ, "Paciente", new Label(pASEJ.nombreCompletoASEJ() + " · " + pASEJ.numeroHistoriaASEJ()));
        FormulariosASEJ.filaASEJ(gASEJ, "Odontólogo *", odontologoASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "Motivo *", motivoASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "Dentición", denticionASEJ);
        Label ayudaASEJ = new Label("Se abrirá el odontograma en borrador (inicial si es el primero del paciente, "
                + "si no de evolución). La dentición sugerida corresponde a la edad (" + pASEJ.edadASEJ() + " años).");
        ayudaASEJ.setWrapText(true);
        ayudaASEJ.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);

        Button cancelarASEJ = FormulariosASEJ.botonASEJ("Cancelar", null);
        cancelarASEJ.setOnAction(eASEJ -> notificadorASEJ.cerrarPanelASEJ());
        Button abrirASEJ = FormulariosASEJ.botonASEJ("Abrir atención", Feather.CHECK, Styles.ACCENT);
        abrirASEJ.setOnAction(eASEJ -> {
            AtencionFormASEJ formASEJ = new AtencionFormASEJ(null, pASEJ.idASEJ(), odontologoASEJ.getValue() == null ? null : odontologoASEJ.getValue().idASEJ(),
                    motivoASEJ.getText(), null, null, null, denticionASEJ.getValue());
            tareaUiASEJ.ejecutarASEJ(abrirASEJ, () -> atencionServiceASEJ.abrirASEJ(formASEJ), idASEJ -> {
                notificadorASEJ.cerrarPanelASEJ();
                notificadorASEJ.exitoASEJ("Atención abierta: registre el odontograma.");
                recargarASEJ(idASEJ);
                odontogramaControllerASEJ.recargarASEJ();
                eventosASEJ.publishEvent(new NavegarAASEJ(MainControllerASEJ.VISTA_FICHAASEJ, Map.of("pacienteId", pASEJ.idASEJ(), "pestana", "odontograma")));
            });
        });
        notificadorASEJ.abrirPanelASEJ(FormulariosASEJ.panelLateralASEJ("Nueva atención", new VBox(12, gASEJ, ayudaASEJ), cancelarASEJ, abrirASEJ));
        Platform.runLater(motivoASEJ::requestFocus);
    }

    @FXML
    private void onGuardarASEJ() {
        AtencionDetalleASEJ aASEJ = actualASEJ;
        if (aASEJ == null) {
            return;
        }
        AtencionFormASEJ formASEJ = new AtencionFormASEJ(aASEJ.idASEJ(), aASEJ.pacienteIdASEJ(), aASEJ.odontologoIdASEJ(), txtMotivoASEJ.getText(), txtExamenASEJ.getText(),
                txtDiagnosticoASEJ.getText(), txtObservacionesASEJ.getText(), null);
        tareaUiASEJ.ejecutarASEJ(btnGuardarASEJ, () -> atencionServiceASEJ.actualizarASEJ(formASEJ), dASEJ -> {
            notificadorASEJ.exitoASEJ("Atención guardada.");
            recargarASEJ(dASEJ.idASEJ());
        });
    }

    @FXML
    private void onCerrarASEJ() {
        AtencionDetalleASEJ aASEJ = actualASEJ;
        if (aASEJ == null || !notificadorASEJ.confirmarASEJ("Cerrar atención",
                "La atención quedará de solo lectura. El odontograma de la atención debe estar firmado. ¿Continuar?")) {
            return;
        }
        tareaUiASEJ.ejecutarASEJ(btnCerrarASEJ, () -> atencionServiceASEJ.cerrarASEJ(aASEJ.idASEJ()), dASEJ -> {
            notificadorASEJ.exitoASEJ("Atención cerrada.");
            recargarASEJ(dASEJ.idASEJ());
            odontogramaControllerASEJ.recargarASEJ();
        });
    }

    @FXML
    private void onIrOdontogramaASEJ() {
        PacienteDetalleASEJ pASEJ = pacienteActivoASEJ.getASEJ();
        if (pASEJ != null) {
            eventosASEJ.publishEvent(new NavegarAASEJ(MainControllerASEJ.VISTA_FICHAASEJ, Map.of("pacienteId", pASEJ.idASEJ(), "pestana", "odontograma")));
        }
    }

    private static String textoASEJ(String vASEJ) {
        return vASEJ == null ? "" : vASEJ;
    }

    private void avisarCambioDePlanASEJ(long pacienteIdASEJ) {
        avisandoASEJ = true;
        try {
            eventosASEJ.publishEvent(new PlanCambiadoASEJ(pacienteIdASEJ));
        } finally {
            avisandoASEJ = false;
        }
    }
}
