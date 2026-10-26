package com.odontogest.ui.controlador;

import atlantafx.base.controls.CustomTextField;
import atlantafx.base.theme.Styles;
import com.odontogest.evento.CitaCambiadaASEJ;
import com.odontogest.evento.NavegarAASEJ;
import com.odontogest.modelo.EstadoCitaASEJ;
import com.odontogest.modelo.RolASEJ;
import com.odontogest.seguridad.PermisoASEJ;
import com.odontogest.seguridad.SesionUsuarioASEJ;
import com.odontogest.servicio.AtencionServiceASEJ;
import com.odontogest.servicio.CitaServiceASEJ;
import com.odontogest.servicio.OdontologoServiceASEJ;
import com.odontogest.servicio.PacienteServiceASEJ;
import com.odontogest.servicio.dto.AgendaDtoASEJ;
import com.odontogest.servicio.dto.CitaDtoASEJ;
import com.odontogest.servicio.dto.CitaFormASEJ;
import com.odontogest.servicio.dto.ItemPlanPendienteDtoASEJ;
import com.odontogest.servicio.dto.OdontologoDtoASEJ;
import com.odontogest.servicio.dto.PacienteDetalleASEJ;
import com.odontogest.servicio.dto.PacienteResumenASEJ;
import com.odontogest.servicio.dto.UsuarioSesionASEJ;
import com.odontogest.ui.componente.AgendaSemanalASEJ;
import com.odontogest.ui.componente.AutocompletarASEJ;
import com.odontogest.ui.soporte.FormulariosASEJ;
import com.odontogest.ui.soporte.FormulariosASEJ.OpcionASEJ;
import com.odontogest.ui.soporte.HiloUiASEJ;
import com.odontogest.ui.soporte.NotificadorASEJ;
import com.odontogest.ui.soporte.RefrescableASEJ;
import com.odontogest.ui.soporte.TareaUiASEJ;
import com.odontogest.ui.soporte.TemaASEJ;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AgendaControllerASEJ implements RefrescableASEJ {

    private static final DateTimeFormatter DIAASEJ = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter FECHAASEJ = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORAASEJ = DateTimeFormatter.ofPattern("HH:mm");

    private final CitaServiceASEJ citaServiceASEJ;
    private final OdontologoServiceASEJ odontologoServiceASEJ;
    private final PacienteServiceASEJ pacienteServiceASEJ;
    private final AtencionServiceASEJ atencionServiceASEJ;
    private final OdontogramaControllerASEJ odontogramaControllerASEJ;
    private final AtencionesControllerASEJ atencionesControllerASEJ;
    private final TareaUiASEJ tareaUiASEJ;
    private final NotificadorASEJ notificadorASEJ;
    private final SesionUsuarioASEJ sesionUsuarioASEJ;
    private final ApplicationEventPublisher eventosASEJ;

    @FXML private DatePicker dpFechaASEJ;
    @FXML private Label lblRangoASEJ;
    @FXML private ComboBox<OdontologoDtoASEJ> cmbOdontologoASEJ;
    @FXML private ToggleButton tglDiaASEJ;
    @FXML private ToggleButton tglSemanaASEJ;
    @FXML private Button btnHuecoASEJ;
    @FXML private Button btnNuevaASEJ;
    @FXML private ScrollPane desplazableASEJ;
    @FXML private AgendaSemanalASEJ agendaASEJ;
    @FXML private Label lblTituloASEJ;
    @FXML private Label lblDetalleASEJ;
    @FXML private VBox boxAccionesASEJ;
    @FXML private Button btnAtenderASEJ;
    @FXML private Button btnConfirmarASEJ;
    @FXML private Button btnEditarASEJ;
    @FXML private Button btnNoAsistioASEJ;
    @FXML private Button btnCancelarASEJ;
    @FXML private Button btnFichaASEJ;

    private final ToggleGroup grupoVistaASEJ = new ToggleGroup();
    private CitaDtoASEJ seleccionadaASEJ;
    private AgendaDtoASEJ actualASEJ;
    private boolean primeraCargaASEJ = true;
    private Long pacientePendienteASEJ;

    @FXML
    public void initialize() {
        tglDiaASEJ.setToggleGroup(grupoVistaASEJ);
        tglSemanaASEJ.setToggleGroup(grupoVistaASEJ);
        tglSemanaASEJ.setSelected(true);
        grupoVistaASEJ.selectedToggleProperty().addListener((obsASEJ, aASEJ, bASEJ) -> {
            if (bASEJ == null) {
                aASEJ.setSelected(true);
            } else {
                recargarASEJ();
            }
        });
        FormulariosASEJ.configurarFechaASEJ(dpFechaASEJ);
        dpFechaASEJ.setValue(LocalDate.now());
        dpFechaASEJ.valueProperty().addListener((obsASEJ, aASEJ, bASEJ) -> recargarASEJ());
        cmbOdontologoASEJ.valueProperty().addListener((obsASEJ, aASEJ, bASEJ) -> recargarASEJ());
        boolean gestionaASEJ = sesionUsuarioASEJ.tieneASEJ(PermisoASEJ.AGENDA_GESTIONAR);
        btnNuevaASEJ.setDisable(!gestionaASEJ);
        btnHuecoASEJ.setDisable(!gestionaASEJ);
        agendaASEJ.setOyenteASEJ(new AgendaSemanalASEJ.OyenteASEJ() {
            @Override
            public void seleccionarASEJ(CitaDtoASEJ citaASEJ) {
                mostrarDetalleASEJ(citaASEJ);
            }

            @Override
            public void abrirASEJ(CitaDtoASEJ citaASEJ) {
                mostrarDetalleASEJ(citaASEJ);
                if (citaASEJ.estadoASEJ().activaASEJ() && gestionaASEJ) {
                    formularioCitaASEJ(citaASEJ, null, null);
                }
            }

            @Override
            public void moverASEJ(CitaDtoASEJ citaASEJ, LocalDateTime nuevoInicioASEJ) {
                moverCitaASEJ(citaASEJ, nuevoInicioASEJ);
            }

            @Override
            public void crearASEJ(LocalDateTime inicioASEJ) {
                if (gestionaASEJ) {
                    formularioCitaASEJ(null, inicioASEJ, null);
                }
            }
        });
        mostrarDetalleASEJ(null);
        primeraCargaASEJ = true;
        tareaUiASEJ.ejecutarASEJ(() -> odontologoServiceASEJ.listarASEJ(false), this::llenarOdontologosASEJ);
    }

    private void llenarOdontologosASEJ(List<OdontologoDtoASEJ> listaASEJ) {
        cmbOdontologoASEJ.getItems().setAll(listaASEJ);
        UsuarioSesionASEJ uASEJ = sesionUsuarioASEJ.exigirUsuarioASEJ();
        OdontologoDtoASEJ inicialASEJ = listaASEJ.stream()
                .filter(oASEJ -> RolASEJ.valueOf(uASEJ.rolASEJ()) == RolASEJ.ODONTOLOGO && uASEJ.odontologoIdASEJ() != null && oASEJ.idASEJ() == uASEJ.odontologoIdASEJ())
                .findFirst().orElse(listaASEJ.isEmpty() ? null : listaASEJ.get(0));
        cmbOdontologoASEJ.setValue(inicialASEJ);
        if (pacientePendienteASEJ != null) {
            Long pASEJ = pacientePendienteASEJ;
            pacientePendienteASEJ = null;
            nuevaCitaParaASEJ(pASEJ);
        }
    }

    @Override
    public void refrescarASEJ() {
        recargarASEJ();
    }

    public void nuevaCitaParaASEJ(long pacienteIdASEJ) {
        if (cmbOdontologoASEJ == null || cmbOdontologoASEJ.getItems().isEmpty()) {
            pacientePendienteASEJ = pacienteIdASEJ;
            return;
        }
        formularioCitaASEJ(null, null, pacienteIdASEJ);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void alCambiarCitaASEJ(CitaCambiadaASEJ eASEJ) {
        HiloUiASEJ.ejecutarASEJ(() -> {
            if (agendaASEJ != null && agendaASEJ.getScene() != null && actualASEJ != null
                    && actualASEJ.odontologoASEJ().idASEJ() == eASEJ.odontologoIdASEJ()) {
                recargarASEJ();
            }
        });
    }

    private void recargarASEJ() {
        OdontologoDtoASEJ odASEJ = cmbOdontologoASEJ.getValue();
        LocalDate fechaASEJ = dpFechaASEJ.getValue() == null ? LocalDate.now() : dpFechaASEJ.getValue();
        if (odASEJ == null) {
            return;
        }
        boolean diaASEJ = tglDiaASEJ.isSelected();
        LocalDate desdeASEJ = diaASEJ ? fechaASEJ : fechaASEJ.with(DayOfWeek.MONDAY);
        int diasASEJ = diaASEJ ? 1 : 7;
        tareaUiASEJ.ejecutarASEJ(() -> citaServiceASEJ.agendaASEJ(odASEJ.idASEJ(), desdeASEJ, diasASEJ), this::mostrarASEJ);
    }

    private void mostrarASEJ(AgendaDtoASEJ aASEJ) {
        actualASEJ = aASEJ;
        int diasASEJ = aASEJ.diasASEJ();
        if (diasASEJ == 7 && aASEJ.odontologoASEJ().horariosASEJ().stream().noneMatch(hASEJ -> hASEJ.diaASEJ() == DayOfWeek.SUNDAY)
                && aASEJ.citasASEJ().stream().noneMatch(cASEJ -> cASEJ.inicioASEJ().getDayOfWeek() == DayOfWeek.SUNDAY)) {
            diasASEJ = 6;
        }
        agendaASEJ.setOscuroASEJ(TemaASEJ.actualASEJ() == TemaASEJ.OSCURO);
        agendaASEJ.setDatosASEJ(aASEJ.desdeASEJ(), diasASEJ, aASEJ.intervaloMinASEJ(), aASEJ.odontologoASEJ().horariosASEJ(), aASEJ.citasASEJ());
        lblRangoASEJ.setText(diasASEJ == 1 ? FECHAASEJ.format(aASEJ.desdeASEJ())
                : DIAASEJ.format(aASEJ.desdeASEJ()) + " – " + FECHAASEJ.format(aASEJ.desdeASEJ().plusDays(diasASEJ - 1L)));
        if (seleccionadaASEJ != null) {
            Optional<CitaDtoASEJ> mismaASEJ = aASEJ.citasASEJ().stream().filter(cASEJ -> cASEJ.idASEJ() == seleccionadaASEJ.idASEJ()).findFirst();
            mostrarDetalleASEJ(mismaASEJ.orElse(null));
        }
        if (primeraCargaASEJ) {
            primeraCargaASEJ = false;
            Platform.runLater(() -> {
                double totalASEJ = agendaASEJ.getHeight() - desplazableASEJ.getViewportBounds().getHeight();
                if (totalASEJ > 0) {
                    desplazableASEJ.setVvalue(Math.min(1, Math.max(0, agendaASEJ.posicionDeASEJ(LocalTime.of(8, 30)) - 38) / totalASEJ));
                }
            });
        }
    }

    private void mostrarDetalleASEJ(CitaDtoASEJ cASEJ) {
        seleccionadaASEJ = cASEJ;
        agendaASEJ.seleccionarASEJ(cASEJ == null ? null : cASEJ.idASEJ());
        boolean hayASEJ = cASEJ != null;
        boxAccionesASEJ.setVisible(hayASEJ);
        boxAccionesASEJ.setManaged(hayASEJ);
        if (!hayASEJ) {
            lblTituloASEJ.setText("Cita");
            lblDetalleASEJ.setText("Elija una cita de la agenda, o haga doble clic en un espacio libre para crear una.");
            return;
        }
        lblTituloASEJ.setText(HORAASEJ.format(cASEJ.inicioASEJ()) + "–" + HORAASEJ.format(cASEJ.finASEJ()) + " · " + cASEJ.estadoASEJ().etiquetaASEJ());
        StringBuilder sbASEJ = new StringBuilder();
        sbASEJ.append(FECHAASEJ.format(cASEJ.inicioASEJ())).append(" (").append(cASEJ.duracionMinASEJ()).append(" min)\n\n");
        sbASEJ.append(cASEJ.pacienteNombreASEJ()).append("\n").append(cASEJ.numeroHistoriaASEJ());
        if (cASEJ.pacienteTelefonoASEJ() != null) {
            sbASEJ.append(" · Tel. ").append(cASEJ.pacienteTelefonoASEJ());
        }
        sbASEJ.append("\n\nC.D. ").append(cASEJ.odontologoNombreASEJ()).append("\n\nMotivo: ").append(cASEJ.motivoASEJ());
        if (cASEJ.planItemDescripcionASEJ() != null) {
            sbASEJ.append("\nPlan: ").append(cASEJ.planItemDescripcionASEJ());
        }
        if (cASEJ.notasASEJ() != null) {
            sbASEJ.append("\nNotas: ").append(cASEJ.notasASEJ());
        }
        if (cASEJ.atencionIdASEJ() != null) {
            sbASEJ.append("\n\nAtención abierta desde esta cita.");
        }
        lblDetalleASEJ.setText(sbASEJ.toString());

        boolean gestionaASEJ = sesionUsuarioASEJ.tieneASEJ(PermisoASEJ.AGENDA_GESTIONAR);
        boolean atiendeASEJ = sesionUsuarioASEJ.tieneASEJ(PermisoASEJ.ATENCION_GESTIONAR);
        boolean activaASEJ = cASEJ.estadoASEJ().activaASEJ();
        boolean deHoyASEJ = cASEJ.inicioASEJ().toLocalDate().equals(LocalDate.now());
        boolean conAtencionASEJ = cASEJ.atencionIdASEJ() != null;
        visibleASEJ(btnAtenderASEJ, atiendeASEJ && (conAtencionASEJ || (activaASEJ && deHoyASEJ)));
        btnAtenderASEJ.setText(conAtencionASEJ ? "Ir a la atención" : "Atender ahora");
        visibleASEJ(btnConfirmarASEJ, gestionaASEJ && cASEJ.estadoASEJ() == EstadoCitaASEJ.PROGRAMADA);
        visibleASEJ(btnEditarASEJ, gestionaASEJ && activaASEJ && !conAtencionASEJ);
        visibleASEJ(btnNoAsistioASEJ, gestionaASEJ && activaASEJ && !conAtencionASEJ && !cASEJ.inicioASEJ().isAfter(LocalDateTime.now()));
        visibleASEJ(btnCancelarASEJ, gestionaASEJ && activaASEJ && !conAtencionASEJ);
    }

    private static void visibleASEJ(Button bASEJ, boolean vASEJ) {
        bASEJ.setVisible(vASEJ);
        bASEJ.setManaged(vASEJ);
    }

    private void moverCitaASEJ(CitaDtoASEJ cASEJ, LocalDateTime nuevoInicioASEJ) {
        tareaUiASEJ.ejecutarASEJ(() -> citaServiceASEJ.moverASEJ(cASEJ.idASEJ(), nuevoInicioASEJ), dASEJ -> {
            notificadorASEJ.exitoASEJ("Cita movida al " + FECHAASEJ.format(dASEJ.inicioASEJ()) + " a las " + HORAASEJ.format(dASEJ.inicioASEJ()) + ".");
            seleccionadaASEJ = dASEJ;
            recargarASEJ();
        }, errorASEJ -> {
            tareaUiASEJ.manejarErrorASEJ(errorASEJ);
            recargarASEJ();
        });
    }

    private void cambiarEstadoASEJ(EstadoCitaASEJ destinoASEJ, String preguntaASEJ) {
        CitaDtoASEJ cASEJ = seleccionadaASEJ;
        if (cASEJ == null || (preguntaASEJ != null && !notificadorASEJ.confirmarASEJ(destinoASEJ.etiquetaASEJ(), preguntaASEJ))) {
            return;
        }
        tareaUiASEJ.ejecutarASEJ(() -> citaServiceASEJ.cambiarEstadoASEJ(cASEJ.idASEJ(), destinoASEJ), dASEJ -> {
            notificadorASEJ.exitoASEJ("Cita " + dASEJ.estadoASEJ().etiquetaASEJ().toLowerCase() + ".");
            seleccionadaASEJ = dASEJ;
            recargarASEJ();
        });
    }

    @FXML
    private void onConfirmarASEJ() {
        cambiarEstadoASEJ(EstadoCitaASEJ.CONFIRMADA, null);
    }

    @FXML
    private void onNoAsistioASEJ() {
        cambiarEstadoASEJ(EstadoCitaASEJ.NO_ASISTIO, "¿Marcar que el paciente no asistió a la cita? El horario queda libre.");
    }

    @FXML
    private void onCancelarASEJ() {
        cambiarEstadoASEJ(EstadoCitaASEJ.CANCELADA, "¿Cancelar la cita? El horario queda libre y la cita ya no se podrá reactivar.");
    }

    @FXML
    private void onEditarASEJ() {
        if (seleccionadaASEJ != null) {
            formularioCitaASEJ(seleccionadaASEJ, null, null);
        }
    }

    @FXML
    private void onFichaASEJ() {
        if (seleccionadaASEJ != null) {
            eventosASEJ.publishEvent(new NavegarAASEJ(MainControllerASEJ.VISTA_FICHAASEJ, Map.of("pacienteId", seleccionadaASEJ.pacienteIdASEJ())));
        }
    }

    @FXML
    private void onAtenderASEJ() {
        CitaDtoASEJ cASEJ = seleccionadaASEJ;
        if (cASEJ == null) {
            return;
        }
        if (cASEJ.atencionIdASEJ() != null) {
            irAFichaASEJ(cASEJ.pacienteIdASEJ(), "atenciones");
            return;
        }
        tareaUiASEJ.ejecutarASEJ(btnAtenderASEJ, () -> atencionServiceASEJ.abrirDesdeCitaASEJ(cASEJ.idASEJ()), idASEJ -> {
            notificadorASEJ.exitoASEJ("Atención abierta: registre el odontograma.");
            irAFichaASEJ(cASEJ.pacienteIdASEJ(), "odontograma");
            odontogramaControllerASEJ.recargarASEJ();
            atencionesControllerASEJ.refrescarASEJ();
        });
    }

    private void irAFichaASEJ(long pacienteIdASEJ, String pestanaASEJ) {
        eventosASEJ.publishEvent(new NavegarAASEJ(MainControllerASEJ.VISTA_FICHAASEJ, Map.of("pacienteId", pacienteIdASEJ, "pestana", pestanaASEJ)));
    }

    @FXML
    private void onAnteriorASEJ() {
        dpFechaASEJ.setValue(dpFechaASEJ.getValue().minusDays(tglDiaASEJ.isSelected() ? 1 : 7));
    }

    @FXML
    private void onSiguienteASEJ() {
        dpFechaASEJ.setValue(dpFechaASEJ.getValue().plusDays(tglDiaASEJ.isSelected() ? 1 : 7));
    }

    @FXML
    private void onHoyASEJ() {
        dpFechaASEJ.setValue(LocalDate.now());
    }

    @FXML
    private void onNuevaASEJ() {
        formularioCitaASEJ(null, null, null);
    }

    @FXML
    private void onSiguienteHuecoASEJ() {
        OdontologoDtoASEJ odASEJ = cmbOdontologoASEJ.getValue();
        if (odASEJ == null) {
            return;
        }
        LocalDate fechaASEJ = dpFechaASEJ.getValue();
        LocalDateTime desdeASEJ = fechaASEJ.isAfter(LocalDate.now()) ? fechaASEJ.atStartOfDay() : LocalDateTime.now();
        tareaUiASEJ.ejecutarASEJ(btnHuecoASEJ, () -> citaServiceASEJ.siguienteHuecoASEJ(odASEJ.idASEJ(), citaServiceASEJ.duracionPorDefectoMinASEJ(), desdeASEJ, null),
                huecoASEJ -> {
                    if (huecoASEJ.isEmpty()) {
                        notificadorASEJ.advertenciaASEJ("No hay horarios libres en los próximos días para " + odASEJ.nombreCompletoASEJ() + ".");
                        return;
                    }
                    dpFechaASEJ.setValue(huecoASEJ.get().toLocalDate());
                    formularioCitaASEJ(null, huecoASEJ.get(), null);
                });
    }

    private void formularioCitaASEJ(CitaDtoASEJ existenteASEJ, LocalDateTime inicioASEJ, Long pacienteIdASEJ) {
        tareaUiASEJ.ejecutarASEJ(() -> new DatosFormularioASEJ(citaServiceASEJ.intervaloMinASEJ(), citaServiceASEJ.duracionPorDefectoMinASEJ(),
                        pacienteIdASEJ == null ? null : pacienteServiceASEJ.obtenerASEJ(pacienteIdASEJ)),
                dASEJ -> construirFormularioASEJ(existenteASEJ, inicioASEJ, dASEJ));
    }

    private record DatosFormularioASEJ(int intervaloASEJ, int duracionASEJ, PacienteDetalleASEJ pacienteASEJ) {
    }

    private void construirFormularioASEJ(CitaDtoASEJ existenteASEJ, LocalDateTime inicioSugeridoASEJ, DatosFormularioASEJ datosASEJ) {
        Long[] pacienteElegidoASEJ = {existenteASEJ != null ? Long.valueOf(existenteASEJ.pacienteIdASEJ())
                : datosASEJ.pacienteASEJ() == null ? null : datosASEJ.pacienteASEJ().idASEJ()};

        CustomTextField txtPacienteASEJ = new CustomTextField();
        txtPacienteASEJ.setLeft(new FontIcon(Feather.SEARCH));
        txtPacienteASEJ.setPromptText("Nombre, documento o N° de historia");
        ComboBox<OpcionASEJ<ItemPlanPendienteDtoASEJ>> cmbItemASEJ = new ComboBox<>();
        Runnable cargarItemsASEJ = () -> {
            Long pASEJ = pacienteElegidoASEJ[0];
            cmbItemASEJ.getItems().setAll(new OpcionASEJ<>("Ninguno", null));
            cmbItemASEJ.getSelectionModel().selectFirst();
            if (pASEJ != null) {
                tareaUiASEJ.ejecutarASEJ(() -> citaServiceASEJ.itemsPendientesASEJ(pASEJ), itemsASEJ -> {
                    itemsASEJ.forEach(iASEJ -> cmbItemASEJ.getItems().add(new OpcionASEJ<>(iASEJ.toString(), iASEJ)));
                    if (existenteASEJ != null && existenteASEJ.planItemIdASEJ() != null) {
                        cmbItemASEJ.getItems().stream().filter(oASEJ -> oASEJ.valorASEJ() != null && oASEJ.valorASEJ().itemIdASEJ() == existenteASEJ.planItemIdASEJ())
                                .findFirst().ifPresent(cmbItemASEJ::setValue);
                    }
                });
            }
        };
        if (existenteASEJ != null) {
            txtPacienteASEJ.setText(existenteASEJ.pacienteNombreASEJ() + "  ·  " + existenteASEJ.numeroHistoriaASEJ());
            txtPacienteASEJ.setDisable(true);
        } else if (datosASEJ.pacienteASEJ() != null) {
            txtPacienteASEJ.setText(datosASEJ.pacienteASEJ().nombreCompletoASEJ() + "  ·  " + datosASEJ.pacienteASEJ().numeroHistoriaASEJ());
        }
        String[] textoElegidoASEJ = {txtPacienteASEJ.getText()};
        new AutocompletarASEJ<PacienteResumenASEJ>(txtPacienteASEJ,
                (filtroASEJ, entregarASEJ) -> tareaUiASEJ.ejecutarASEJ(() -> pacienteServiceASEJ.buscarASEJ(filtroASEJ, false, 0).contenidoASEJ(), entregarASEJ),
                pASEJ -> pASEJ.nombreCompletoASEJ() + "  ·  " + pASEJ.numeroHistoriaASEJ(),
                pASEJ -> {
                    pacienteElegidoASEJ[0] = pASEJ.idASEJ();
                    textoElegidoASEJ[0] = txtPacienteASEJ.getText();
                    cargarItemsASEJ.run();
                });
        txtPacienteASEJ.textProperty().addListener((obsASEJ, aASEJ, bASEJ) -> {
            if (existenteASEJ == null && pacienteElegidoASEJ[0] != null && !bASEJ.equals(textoElegidoASEJ[0])) {
                pacienteElegidoASEJ[0] = null;
                cargarItemsASEJ.run();
            }
        });

        ComboBox<OdontologoDtoASEJ> cmbOdASEJ = new ComboBox<>();
        cmbOdASEJ.getItems().setAll(cmbOdontologoASEJ.getItems());
        cmbOdASEJ.setValue(cmbOdASEJ.getItems().stream()
                .filter(oASEJ -> oASEJ.idASEJ() == (existenteASEJ != null ? existenteASEJ.odontologoIdASEJ()
                        : cmbOdontologoASEJ.getValue() == null ? -1 : cmbOdontologoASEJ.getValue().idASEJ()))
                .findFirst().orElse(null));

        DatePicker fechaASEJ = new DatePicker();
        FormulariosASEJ.configurarFechaASEJ(fechaASEJ);
        ComboBox<LocalTime> horaASEJ = new ComboBox<>();
        for (LocalTime tASEJ = LocalTime.of(7, 0); !tASEJ.isAfter(LocalTime.of(21, 0)); tASEJ = tASEJ.plusMinutes(datosASEJ.intervaloASEJ())) {
            horaASEJ.getItems().add(tASEJ);
        }
        horaASEJ.setConverter(FormulariosASEJ.conversorASEJ(HORAASEJ::format));
        horaASEJ.setVisibleRowCount(12);
        LocalDateTime baseASEJ = existenteASEJ != null ? existenteASEJ.inicioASEJ() : inicioSugeridoASEJ;
        fechaASEJ.setValue(baseASEJ != null ? baseASEJ.toLocalDate() : dpFechaASEJ.getValue());
        horaASEJ.setValue(baseASEJ != null ? baseASEJ.toLocalTime() : null);

        Spinner<Integer> duracionASEJ = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(datosASEJ.intervaloASEJ(),
                CitaServiceASEJ.DURACION_MAXIMA_MINASEJ, existenteASEJ != null ? existenteASEJ.duracionMinASEJ() : datosASEJ.duracionASEJ(), datosASEJ.intervaloASEJ()));
        duracionASEJ.setEditable(true);
        TextField motivoASEJ = new TextField(existenteASEJ != null ? existenteASEJ.motivoASEJ() : "");
        motivoASEJ.setPromptText("Control, profilaxis, resina en pieza 36…");
        TextArea notasASEJ = new TextArea(existenteASEJ != null && existenteASEJ.notasASEJ() != null ? existenteASEJ.notasASEJ() : "");
        notasASEJ.setPrefRowCount(2);
        notasASEJ.setWrapText(true);
        cargarItemsASEJ.run();

        Button buscarHuecoASEJ = FormulariosASEJ.botonASEJ("Buscar hueco libre", Feather.SEARCH);
        buscarHuecoASEJ.setOnAction(eASEJ -> {
            OdontologoDtoASEJ odASEJ = cmbOdASEJ.getValue();
            if (odASEJ == null) {
                notificadorASEJ.advertenciaASEJ("Elija el odontólogo.");
                return;
            }
            duracionASEJ.increment(0);
            LocalDate fASEJ = fechaASEJ.getValue() == null ? LocalDate.now() : fechaASEJ.getValue();
            LocalDateTime desdeASEJ = fASEJ.atTime(horaASEJ.getValue() == null ? LocalTime.MIN : horaASEJ.getValue());
            int minASEJ = duracionASEJ.getValue();
            Long pASEJ = pacienteElegidoASEJ[0];
            tareaUiASEJ.ejecutarASEJ(buscarHuecoASEJ, () -> citaServiceASEJ.siguienteHuecoASEJ(odASEJ.idASEJ(), minASEJ, desdeASEJ, pASEJ), huecoASEJ -> {
                if (huecoASEJ.isEmpty()) {
                    notificadorASEJ.advertenciaASEJ("No hay horarios libres en los próximos días.");
                } else {
                    fechaASEJ.setValue(huecoASEJ.get().toLocalDate());
                    horaASEJ.setValue(huecoASEJ.get().toLocalTime());
                }
            });
        });

        GridPane gASEJ = FormulariosASEJ.grillaASEJ();
        FormulariosASEJ.filaASEJ(gASEJ, "Paciente *", txtPacienteASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "Odontólogo *", cmbOdASEJ);
        HBox fechaHoraASEJ = new HBox(8, fechaASEJ, horaASEJ);
        fechaASEJ.setPrefWidth(150);
        horaASEJ.setPrefWidth(110);
        FormulariosASEJ.filaASEJ(gASEJ, "Fecha y hora *", fechaHoraASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "Duración (min)", duracionASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "", buscarHuecoASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "Motivo *", motivoASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "Del plan", cmbItemASEJ);
        FormulariosASEJ.filaASEJ(gASEJ, "Notas", notasASEJ);
        Label ayudaASEJ = new Label("La cita debe estar dentro del horario del odontólogo y no cruzarse con otras citas suyas "
                + "ni del paciente. «Buscar hueco libre» propone la primera hora disponible desde la fecha elegida.");
        ayudaASEJ.setWrapText(true);
        ayudaASEJ.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);

        Button cancelarASEJ = FormulariosASEJ.botonASEJ("Cancelar", null);
        cancelarASEJ.setOnAction(eASEJ -> notificadorASEJ.cerrarPanelASEJ());
        Button guardarASEJ = FormulariosASEJ.botonASEJ(existenteASEJ == null ? "Programar" : "Guardar", Feather.CHECK, Styles.ACCENT);
        guardarASEJ.setOnAction(eASEJ -> {
            try {
                duracionASEJ.increment(0);
                LocalDate fASEJ = FormulariosASEJ.fechaASEJ(fechaASEJ);
                if (pacienteElegidoASEJ[0] == null) {
                    notificadorASEJ.advertenciaASEJ("Busque y elija al paciente de la lista.");
                    return;
                }
                LocalDateTime inicioASEJ = fASEJ == null || horaASEJ.getValue() == null ? null : fASEJ.atTime(horaASEJ.getValue());
                OpcionASEJ<ItemPlanPendienteDtoASEJ> itemASEJ = cmbItemASEJ.getValue();
                CitaFormASEJ formASEJ = new CitaFormASEJ(existenteASEJ == null ? null : existenteASEJ.idASEJ(), pacienteElegidoASEJ[0],
                        cmbOdASEJ.getValue() == null ? null : cmbOdASEJ.getValue().idASEJ(), inicioASEJ, duracionASEJ.getValue(), motivoASEJ.getText(),
                        notasASEJ.getText(), itemASEJ == null || itemASEJ.valorASEJ() == null ? null : itemASEJ.valorASEJ().itemIdASEJ());
                tareaUiASEJ.ejecutarASEJ(guardarASEJ, () -> citaServiceASEJ.programarASEJ(formASEJ), dASEJ -> {
                    notificadorASEJ.cerrarPanelASEJ();
                    notificadorASEJ.exitoASEJ("Cita " + (existenteASEJ == null ? "programada" : "actualizada") + " el "
                            + FECHAASEJ.format(dASEJ.inicioASEJ()) + " a las " + HORAASEJ.format(dASEJ.inicioASEJ()) + ".");
                    seleccionadaASEJ = dASEJ;
                    if (cmbOdontologoASEJ.getValue() == null || cmbOdontologoASEJ.getValue().idASEJ() != dASEJ.odontologoIdASEJ()) {
                        cmbOdontologoASEJ.getItems().stream().filter(oASEJ -> oASEJ.idASEJ() == dASEJ.odontologoIdASEJ()).findFirst()
                                .ifPresent(cmbOdontologoASEJ::setValue);
                    }
                    if (!dASEJ.inicioASEJ().toLocalDate().equals(dpFechaASEJ.getValue())) {
                        dpFechaASEJ.setValue(dASEJ.inicioASEJ().toLocalDate());
                    } else {
                        recargarASEJ();
                    }
                });
            } catch (RuntimeException exASEJ) {
                tareaUiASEJ.manejarErrorASEJ(exASEJ);
            }
        });
        notificadorASEJ.abrirPanelASEJ(FormulariosASEJ.panelLateralASEJ(existenteASEJ == null ? "Nueva cita" : "Editar cita",
                new VBox(12, gASEJ, ayudaASEJ), cancelarASEJ, guardarASEJ));
        Platform.runLater(() -> (existenteASEJ == null && pacienteElegidoASEJ[0] == null ? txtPacienteASEJ : motivoASEJ).requestFocus());
    }
}
