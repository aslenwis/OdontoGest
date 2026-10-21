/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.controlador;

import atlantafx.base.controls.CustomTextField;
import atlantafx.base.theme.Styles;
import com.odontogest.evento.CitaCambiada;
import com.odontogest.evento.NavegarA;
import com.odontogest.modelo.EstadoCita;
import com.odontogest.modelo.Rol;
import com.odontogest.seguridad.Permiso;
import com.odontogest.seguridad.SesionUsuario;
import com.odontogest.servicio.AtencionService;
import com.odontogest.servicio.CitaService;
import com.odontogest.servicio.OdontologoService;
import com.odontogest.servicio.PacienteService;
import com.odontogest.servicio.dto.AgendaDto;
import com.odontogest.servicio.dto.CitaDto;
import com.odontogest.servicio.dto.CitaForm;
import com.odontogest.servicio.dto.ItemPlanPendienteDto;
import com.odontogest.servicio.dto.OdontologoDto;
import com.odontogest.servicio.dto.PacienteDetalle;
import com.odontogest.servicio.dto.PacienteResumen;
import com.odontogest.servicio.dto.UsuarioSesion;
import com.odontogest.ui.componente.AgendaSemanal;
import com.odontogest.ui.componente.Autocompletar;
import com.odontogest.ui.soporte.Formularios;
import com.odontogest.ui.soporte.Formularios.Opcion;
import com.odontogest.ui.soporte.HiloUi;
import com.odontogest.ui.soporte.Notificador;
import com.odontogest.ui.soporte.Refrescable;
import com.odontogest.ui.soporte.TareaUi;
import com.odontogest.ui.soporte.Tema;
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

/**
 * Agenda (RF-AGE-01 a RF-AGE-05): semana o día de un odontólogo, citas que se crean con doble clic o con
 * "Nueva cita", se mueven arrastrándolas, se confirman, cancelan o marcan como inasistencia, búsqueda del siguiente
 * hueco y "Atender ahora", que abre la atención del paciente.
 */
@Component
@RequiredArgsConstructor
public class AgendaController implements Refrescable {

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final CitaService citaService;
    private final OdontologoService odontologoService;
    private final PacienteService pacienteService;
    private final AtencionService atencionService;
    private final OdontogramaController odontogramaController;
    private final AtencionesController atencionesController;
    private final TareaUi tareaUi;
    private final Notificador notificador;
    private final SesionUsuario sesionUsuario;
    private final ApplicationEventPublisher eventos;

    @FXML private DatePicker dpFecha;
    @FXML private Label lblRango;
    @FXML private ComboBox<OdontologoDto> cmbOdontologo;
    @FXML private ToggleButton tglDia;
    @FXML private ToggleButton tglSemana;
    @FXML private Button btnHueco;
    @FXML private Button btnNueva;
    @FXML private ScrollPane desplazable;
    @FXML private AgendaSemanal agenda;
    @FXML private Label lblTitulo;
    @FXML private Label lblDetalle;
    @FXML private VBox boxAcciones;
    @FXML private Button btnAtender;
    @FXML private Button btnConfirmar;
    @FXML private Button btnEditar;
    @FXML private Button btnNoAsistio;
    @FXML private Button btnCancelar;
    @FXML private Button btnFicha;

    private final ToggleGroup grupoVista = new ToggleGroup();
    private CitaDto seleccionada;
    private AgendaDto actual;
    private boolean primeraCarga = true;
    /** Paciente para el que se pidió una cita antes de que la vista terminara de cargar. */
    private Long pacientePendiente;

    @FXML
    public void initialize() {
        tglDia.setToggleGroup(grupoVista);
        tglSemana.setToggleGroup(grupoVista);
        tglSemana.setSelected(true);
        grupoVista.selectedToggleProperty().addListener((obs, a, b) -> {
            if (b == null) {
                a.setSelected(true);      // siempre hay una vista elegida
            } else {
                recargar();
            }
        });
        Formularios.configurarFecha(dpFecha);
        dpFecha.setValue(LocalDate.now());
        dpFecha.valueProperty().addListener((obs, a, b) -> recargar());
        cmbOdontologo.valueProperty().addListener((obs, a, b) -> recargar());
        boolean gestiona = sesionUsuario.tiene(Permiso.AGENDA_GESTIONAR);
        btnNueva.setDisable(!gestiona);
        btnHueco.setDisable(!gestiona);
        agenda.setOyente(new AgendaSemanal.Oyente() {
            @Override
            public void seleccionar(CitaDto cita) {
                mostrarDetalle(cita);
            }

            @Override
            public void abrir(CitaDto cita) {
                mostrarDetalle(cita);
                if (cita.estado().activa() && gestiona) {
                    formularioCita(cita, null, null);
                }
            }

            @Override
            public void mover(CitaDto cita, LocalDateTime nuevoInicio) {
                moverCita(cita, nuevoInicio);
            }

            @Override
            public void crear(LocalDateTime inicio) {
                if (gestiona) {
                    formularioCita(null, inicio, null);
                }
            }
        });
        mostrarDetalle(null);
        primeraCarga = true;
        tareaUi.ejecutar(() -> odontologoService.listar(false), this::llenarOdontologos);
    }

    private void llenarOdontologos(List<OdontologoDto> lista) {
        cmbOdontologo.getItems().setAll(lista);
        UsuarioSesion u = sesionUsuario.exigirUsuario();
        OdontologoDto inicial = lista.stream()
                .filter(o -> Rol.valueOf(u.rol()) == Rol.ODONTOLOGO && u.odontologoId() != null && o.id() == u.odontologoId())
                .findFirst().orElse(lista.isEmpty() ? null : lista.get(0));
        cmbOdontologo.setValue(inicial);
        if (pacientePendiente != null) {
            Long p = pacientePendiente;
            pacientePendiente = null;
            nuevaCitaPara(p);
        }
    }

    @Override
    public void refrescar() {
        recargar();
    }

    /** Desde la ficha ("Nueva cita"): abre el formulario con el paciente elegido. */
    public void nuevaCitaPara(long pacienteId) {
        if (cmbOdontologo == null || cmbOdontologo.getItems().isEmpty()) {
            pacientePendiente = pacienteId;
            return;
        }
        formularioCita(null, null, pacienteId);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void alCambiarCita(CitaCambiada e) {
        HiloUi.ejecutar(() -> {
            if (agenda != null && agenda.getScene() != null && actual != null
                    && actual.odontologo().id() == e.odontologoId()) {
                recargar();
            }
        });
    }

    // ------------------------------------------------------------------ carga

    private void recargar() {
        OdontologoDto od = cmbOdontologo.getValue();
        LocalDate fecha = dpFecha.getValue() == null ? LocalDate.now() : dpFecha.getValue();
        if (od == null) {
            return;
        }
        boolean dia = tglDia.isSelected();
        LocalDate desde = dia ? fecha : fecha.with(DayOfWeek.MONDAY);
        int dias = dia ? 1 : 7;
        tareaUi.ejecutar(() -> citaService.agenda(od.id(), desde, dias), this::mostrar);
    }

    private void mostrar(AgendaDto a) {
        actual = a;
        int dias = a.dias();
        if (dias == 7 && a.odontologo().horarios().stream().noneMatch(h -> h.dia() == DayOfWeek.SUNDAY)
                && a.citas().stream().noneMatch(c -> c.inicio().getDayOfWeek() == DayOfWeek.SUNDAY)) {
            dias = 6;                                         // sin domingo si no se atiende ese día
        }
        agenda.setOscuro(Tema.actual() == Tema.OSCURO);
        agenda.setDatos(a.desde(), dias, a.intervaloMin(), a.odontologo().horarios(), a.citas());
        lblRango.setText(dias == 1 ? FECHA.format(a.desde())
                : DIA.format(a.desde()) + " – " + FECHA.format(a.desde().plusDays(dias - 1L)));
        if (seleccionada != null) {
            Optional<CitaDto> misma = a.citas().stream().filter(c -> c.id() == seleccionada.id()).findFirst();
            mostrarDetalle(misma.orElse(null));
        }
        if (primeraCarga) {
            primeraCarga = false;
            Platform.runLater(() -> {
                double total = agenda.getHeight() - desplazable.getViewportBounds().getHeight();
                if (total > 0) {
                    desplazable.setVvalue(Math.min(1, Math.max(0, agenda.posicionDe(LocalTime.of(8, 30)) - 38) / total));
                }
            });
        }
    }

    // ------------------------------------------------------------------ detalle y acciones

    private void mostrarDetalle(CitaDto c) {
        seleccionada = c;
        agenda.seleccionar(c == null ? null : c.id());
        boolean hay = c != null;
        boxAcciones.setVisible(hay);
        boxAcciones.setManaged(hay);
        if (!hay) {
            lblTitulo.setText("Cita");
            lblDetalle.setText("Elija una cita de la agenda, o haga doble clic en un espacio libre para crear una.");
            return;
        }
        lblTitulo.setText(HORA.format(c.inicio()) + "–" + HORA.format(c.fin()) + " · " + c.estado().etiqueta());
        StringBuilder sb = new StringBuilder();
        sb.append(FECHA.format(c.inicio())).append(" (").append(c.duracionMin()).append(" min)\n\n");
        sb.append(c.pacienteNombre()).append("\n").append(c.numeroHistoria());
        if (c.pacienteTelefono() != null) {
            sb.append(" · Tel. ").append(c.pacienteTelefono());
        }
        sb.append("\n\nC.D. ").append(c.odontologoNombre()).append("\n\nMotivo: ").append(c.motivo());
        if (c.planItemDescripcion() != null) {
            sb.append("\nPlan: ").append(c.planItemDescripcion());
        }
        if (c.notas() != null) {
            sb.append("\nNotas: ").append(c.notas());
        }
        if (c.atencionId() != null) {
            sb.append("\n\nAtención abierta desde esta cita.");
        }
        lblDetalle.setText(sb.toString());

        boolean gestiona = sesionUsuario.tiene(Permiso.AGENDA_GESTIONAR);
        boolean atiende = sesionUsuario.tiene(Permiso.ATENCION_GESTIONAR);
        boolean activa = c.estado().activa();
        boolean deHoy = c.inicio().toLocalDate().equals(LocalDate.now());
        boolean conAtencion = c.atencionId() != null;
        visible(btnAtender, atiende && (conAtencion || (activa && deHoy)));
        btnAtender.setText(conAtencion ? "Ir a la atención" : "Atender ahora");
        visible(btnConfirmar, gestiona && c.estado() == EstadoCita.PROGRAMADA);
        visible(btnEditar, gestiona && activa && !conAtencion);
        visible(btnNoAsistio, gestiona && activa && !conAtencion && !c.inicio().isAfter(LocalDateTime.now()));
        visible(btnCancelar, gestiona && activa && !conAtencion);
    }

    private static void visible(Button b, boolean v) {
        b.setVisible(v);
        b.setManaged(v);
    }

    private void moverCita(CitaDto c, LocalDateTime nuevoInicio) {
        tareaUi.ejecutar(() -> citaService.mover(c.id(), nuevoInicio), d -> {
            notificador.exito("Cita movida al " + FECHA.format(d.inicio()) + " a las " + HORA.format(d.inicio()) + ".");
            seleccionada = d;
            recargar();
        }, error -> {
            tareaUi.manejarError(error);
            recargar();                                       // la cita vuelve a su lugar
        });
    }

    private void cambiarEstado(EstadoCita destino, String pregunta) {
        CitaDto c = seleccionada;
        if (c == null || (pregunta != null && !notificador.confirmar(destino.etiqueta(), pregunta))) {
            return;
        }
        tareaUi.ejecutar(() -> citaService.cambiarEstado(c.id(), destino), d -> {
            notificador.exito("Cita " + d.estado().etiqueta().toLowerCase() + ".");
            seleccionada = d;
            recargar();
        });
    }

    @FXML
    private void onConfirmar() {
        cambiarEstado(EstadoCita.CONFIRMADA, null);
    }

    @FXML
    private void onNoAsistio() {
        cambiarEstado(EstadoCita.NO_ASISTIO, "¿Marcar que el paciente no asistió a la cita? El horario queda libre.");
    }

    @FXML
    private void onCancelar() {
        cambiarEstado(EstadoCita.CANCELADA, "¿Cancelar la cita? El horario queda libre y la cita ya no se podrá reactivar.");
    }

    @FXML
    private void onEditar() {
        if (seleccionada != null) {
            formularioCita(seleccionada, null, null);
        }
    }

    @FXML
    private void onFicha() {
        if (seleccionada != null) {
            eventos.publishEvent(new NavegarA(MainController.VISTA_FICHA, Map.of("pacienteId", seleccionada.pacienteId())));
        }
    }

    /** RF-AGE-05: abre la atención del paciente desde la cita y lleva al odontograma. */
    @FXML
    private void onAtender() {
        CitaDto c = seleccionada;
        if (c == null) {
            return;
        }
        if (c.atencionId() != null) {
            irAFicha(c.pacienteId(), "atenciones");
            return;
        }
        tareaUi.ejecutar(btnAtender, () -> atencionService.abrirDesdeCita(c.id()), id -> {
            notificador.exito("Atención abierta: registre el odontograma.");
            irAFicha(c.pacienteId(), "odontograma");
            odontogramaController.recargar();
            atencionesController.refrescar();
        });
    }

    private void irAFicha(long pacienteId, String pestana) {
        eventos.publishEvent(new NavegarA(MainController.VISTA_FICHA, Map.of("pacienteId", pacienteId, "pestana", pestana)));
    }

    @FXML
    private void onAnterior() {
        dpFecha.setValue(dpFecha.getValue().minusDays(tglDia.isSelected() ? 1 : 7));
    }

    @FXML
    private void onSiguiente() {
        dpFecha.setValue(dpFecha.getValue().plusDays(tglDia.isSelected() ? 1 : 7));
    }

    @FXML
    private void onHoy() {
        dpFecha.setValue(LocalDate.now());
    }

    @FXML
    private void onNueva() {
        formularioCita(null, null, null);
    }

    /** Busca el siguiente hueco del odontólogo con la duración por defecto y abre el formulario con esa hora. */
    @FXML
    private void onSiguienteHueco() {
        OdontologoDto od = cmbOdontologo.getValue();
        if (od == null) {
            return;
        }
        LocalDate fecha = dpFecha.getValue();
        LocalDateTime desde = fecha.isAfter(LocalDate.now()) ? fecha.atStartOfDay() : LocalDateTime.now();
        tareaUi.ejecutar(btnHueco, () -> citaService.siguienteHueco(od.id(), citaService.duracionPorDefectoMin(), desde, null),
                hueco -> {
                    if (hueco.isEmpty()) {
                        notificador.advertencia("No hay horarios libres en los próximos días para " + od.nombreCompleto() + ".");
                        return;
                    }
                    dpFecha.setValue(hueco.get().toLocalDate());
                    formularioCita(null, hueco.get(), null);
                });
    }

    // ------------------------------------------------------------------ formulario

    /**
     * Formulario de cita en el panel lateral.
     *
     * @param existente  cita a editar ({@code null} = nueva)
     * @param inicio     hora sugerida para una cita nueva
     * @param pacienteId paciente ya elegido para una cita nueva
     */
    private void formularioCita(CitaDto existente, LocalDateTime inicio, Long pacienteId) {
        tareaUi.ejecutar(() -> new DatosFormulario(citaService.intervaloMin(), citaService.duracionPorDefectoMin(),
                        pacienteId == null ? null : pacienteService.obtener(pacienteId)),
                d -> construirFormulario(existente, inicio, d));
    }

    private record DatosFormulario(int intervalo, int duracion, PacienteDetalle paciente) {
    }

    private void construirFormulario(CitaDto existente, LocalDateTime inicioSugerido, DatosFormulario datos) {
        Long[] pacienteElegido = {existente != null ? Long.valueOf(existente.pacienteId())
                : datos.paciente() == null ? null : datos.paciente().id()};

        CustomTextField txtPaciente = new CustomTextField();
        txtPaciente.setLeft(new FontIcon(Feather.SEARCH));
        txtPaciente.setPromptText("Nombre, documento o N° de historia");
        ComboBox<Opcion<ItemPlanPendienteDto>> cmbItem = new ComboBox<>();
        Runnable cargarItems = () -> {
            Long p = pacienteElegido[0];
            cmbItem.getItems().setAll(new Opcion<>("Ninguno", null));
            cmbItem.getSelectionModel().selectFirst();
            if (p != null) {
                tareaUi.ejecutar(() -> citaService.itemsPendientes(p), items -> {
                    items.forEach(i -> cmbItem.getItems().add(new Opcion<>(i.toString(), i)));
                    if (existente != null && existente.planItemId() != null) {
                        cmbItem.getItems().stream().filter(o -> o.valor() != null && o.valor().itemId() == existente.planItemId())
                                .findFirst().ifPresent(cmbItem::setValue);
                    }
                });
            }
        };
        if (existente != null) {
            txtPaciente.setText(existente.pacienteNombre() + "  ·  " + existente.numeroHistoria());
            txtPaciente.setDisable(true);
        } else if (datos.paciente() != null) {
            txtPaciente.setText(datos.paciente().nombreCompleto() + "  ·  " + datos.paciente().numeroHistoria());
        }
        String[] textoElegido = {txtPaciente.getText()};
        new Autocompletar<PacienteResumen>(txtPaciente,
                (filtro, entregar) -> tareaUi.ejecutar(() -> pacienteService.buscar(filtro, false, 0).contenido(), entregar),
                p -> p.nombreCompleto() + "  ·  " + p.numeroHistoria(),
                p -> {
                    pacienteElegido[0] = p.id();
                    textoElegido[0] = txtPaciente.getText();
                    cargarItems.run();
                });
        // Si se cambia el texto a mano después de elegir, el paciente deja de estar elegido.
        txtPaciente.textProperty().addListener((obs, a, b) -> {
            if (existente == null && pacienteElegido[0] != null && !b.equals(textoElegido[0])) {
                pacienteElegido[0] = null;
                cargarItems.run();
            }
        });

        ComboBox<OdontologoDto> cmbOd = new ComboBox<>();
        cmbOd.getItems().setAll(cmbOdontologo.getItems());
        cmbOd.setValue(cmbOd.getItems().stream()
                .filter(o -> o.id() == (existente != null ? existente.odontologoId()
                        : cmbOdontologo.getValue() == null ? -1 : cmbOdontologo.getValue().id()))
                .findFirst().orElse(null));

        DatePicker fecha = new DatePicker();
        Formularios.configurarFecha(fecha);
        ComboBox<LocalTime> hora = new ComboBox<>();
        for (LocalTime t = LocalTime.of(7, 0); !t.isAfter(LocalTime.of(21, 0)); t = t.plusMinutes(datos.intervalo())) {
            hora.getItems().add(t);
        }
        hora.setConverter(Formularios.conversor(HORA::format));
        hora.setVisibleRowCount(12);
        LocalDateTime base = existente != null ? existente.inicio() : inicioSugerido;
        fecha.setValue(base != null ? base.toLocalDate() : dpFecha.getValue());
        hora.setValue(base != null ? base.toLocalTime() : null);

        Spinner<Integer> duracion = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(datos.intervalo(),
                CitaService.DURACION_MAXIMA_MIN, existente != null ? existente.duracionMin() : datos.duracion(), datos.intervalo()));
        duracion.setEditable(true);
        TextField motivo = new TextField(existente != null ? existente.motivo() : "");
        motivo.setPromptText("Control, profilaxis, resina en pieza 36…");
        TextArea notas = new TextArea(existente != null && existente.notas() != null ? existente.notas() : "");
        notas.setPrefRowCount(2);
        notas.setWrapText(true);
        cargarItems.run();

        Button buscarHueco = Formularios.boton("Buscar hueco libre", Feather.SEARCH);
        buscarHueco.setOnAction(e -> {
            OdontologoDto od = cmbOd.getValue();
            if (od == null) {
                notificador.advertencia("Elija el odontólogo.");
                return;
            }
            duracion.increment(0);
            LocalDate f = fecha.getValue() == null ? LocalDate.now() : fecha.getValue();
            LocalDateTime desde = f.atTime(hora.getValue() == null ? LocalTime.MIN : hora.getValue());
            int min = duracion.getValue();
            Long p = pacienteElegido[0];
            tareaUi.ejecutar(buscarHueco, () -> citaService.siguienteHueco(od.id(), min, desde, p), hueco -> {
                if (hueco.isEmpty()) {
                    notificador.advertencia("No hay horarios libres en los próximos días.");
                } else {
                    fecha.setValue(hueco.get().toLocalDate());
                    hora.setValue(hueco.get().toLocalTime());
                }
            });
        });

        GridPane g = Formularios.grilla();
        Formularios.fila(g, "Paciente *", txtPaciente);
        Formularios.fila(g, "Odontólogo *", cmbOd);
        HBox fechaHora = new HBox(8, fecha, hora);
        fecha.setPrefWidth(150);
        hora.setPrefWidth(110);
        Formularios.fila(g, "Fecha y hora *", fechaHora);
        Formularios.fila(g, "Duración (min)", duracion);
        Formularios.fila(g, "", buscarHueco);
        Formularios.fila(g, "Motivo *", motivo);
        Formularios.fila(g, "Del plan", cmbItem);
        Formularios.fila(g, "Notas", notas);
        Label ayuda = new Label("La cita debe estar dentro del horario del odontólogo y no cruzarse con otras citas suyas "
                + "ni del paciente. «Buscar hueco libre» propone la primera hora disponible desde la fecha elegida.");
        ayuda.setWrapText(true);
        ayuda.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);

        Button cancelar = Formularios.boton("Cancelar", null);
        cancelar.setOnAction(e -> notificador.cerrarPanel());
        Button guardar = Formularios.boton(existente == null ? "Programar" : "Guardar", Feather.CHECK, Styles.ACCENT);
        guardar.setOnAction(e -> {
            try {
                duracion.increment(0);
                LocalDate f = Formularios.fecha(fecha);
                if (pacienteElegido[0] == null) {
                    notificador.advertencia("Busque y elija al paciente de la lista.");
                    return;
                }
                LocalDateTime inicio = f == null || hora.getValue() == null ? null : f.atTime(hora.getValue());
                Opcion<ItemPlanPendienteDto> item = cmbItem.getValue();
                CitaForm form = new CitaForm(existente == null ? null : existente.id(), pacienteElegido[0],
                        cmbOd.getValue() == null ? null : cmbOd.getValue().id(), inicio, duracion.getValue(), motivo.getText(),
                        notas.getText(), item == null || item.valor() == null ? null : item.valor().itemId());
                tareaUi.ejecutar(guardar, () -> citaService.programar(form), d -> {
                    notificador.cerrarPanel();
                    notificador.exito("Cita " + (existente == null ? "programada" : "actualizada") + " el "
                            + FECHA.format(d.inicio()) + " a las " + HORA.format(d.inicio()) + ".");
                    seleccionada = d;
                    if (cmbOdontologo.getValue() == null || cmbOdontologo.getValue().id() != d.odontologoId()) {
                        cmbOdontologo.getItems().stream().filter(o -> o.id() == d.odontologoId()).findFirst()
                                .ifPresent(cmbOdontologo::setValue);
                    }
                    if (!d.inicio().toLocalDate().equals(dpFecha.getValue())) {
                        dpFecha.setValue(d.inicio().toLocalDate());
                    } else {
                        recargar();
                    }
                });
            } catch (RuntimeException ex) {
                tareaUi.manejarError(ex);
            }
        });
        notificador.abrirPanel(Formularios.panelLateral(existente == null ? "Nueva cita" : "Editar cita",
                new VBox(12, g, ayuda), cancelar, guardar));
        Platform.runLater(() -> (existente == null && pacienteElegido[0] == null ? txtPaciente : motivo).requestFocus());
    }
}
