/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.controlador;

import atlantafx.base.theme.Styles;
import com.odontogest.dominio.comun.NumeroALetras;
import com.odontogest.dominio.finanzas.EstadoCuota;
import com.odontogest.dominio.finanzas.Frecuencia;
import com.odontogest.dominio.odontograma.PiezaDental;
import com.odontogest.dominio.odontograma.Superficie;
import com.odontogest.dominio.tratamiento.FormaCobro;
import com.odontogest.evento.PlanCambiado;
import com.odontogest.modelo.EstadoPlan;
import com.odontogest.modelo.Rol;
import com.odontogest.seguridad.Permiso;
import com.odontogest.seguridad.SesionUsuario;
import com.odontogest.servicio.OdontologoService;
import com.odontogest.servicio.PlanTratamientoService;
import com.odontogest.servicio.ProcedimientoService;
import com.odontogest.servicio.dto.CronogramaForm;
import com.odontogest.servicio.dto.CuotaDto;
import com.odontogest.servicio.dto.OdontologoDto;
import com.odontogest.servicio.dto.PacienteDetalle;
import com.odontogest.servicio.dto.PlanDto;
import com.odontogest.servicio.dto.PlanItemDto;
import com.odontogest.servicio.dto.PlanItemForm;
import com.odontogest.servicio.dto.PlanResumen;
import com.odontogest.servicio.dto.ProcedimientoDto;
import com.odontogest.servicio.dto.UsuarioSesion;
import com.odontogest.ui.soporte.Exportador;
import com.odontogest.ui.soporte.Formularios;
import com.odontogest.ui.soporte.Formularios.Opcion;
import com.odontogest.ui.soporte.Notificador;
import com.odontogest.ui.soporte.PacienteActivo;
import com.odontogest.ui.soporte.TareaUi;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.converter.DefaultStringConverter;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Pestaña "Plan" de la ficha (RF-PLN-01 a RF-PLN-06): planes del paciente, procedimientos agrupados por fase con
 * descuento editable, transiciones de estado, totales, cronograma de cuotas y documentos PDF (presupuesto y
 * consentimiento informado de tratamiento).
 */
@Component
@RequiredArgsConstructor
public class PlanTratamientoController {

    /** Orden de los botones de transición en la barra. */
    private static final List<EstadoPlan> ORDEN_TRANSICIONES = List.of(EstadoPlan.PROPUESTO, EstadoPlan.ACEPTADO,
            EstadoPlan.EN_CURSO, EstadoPlan.FINALIZADO, EstadoPlan.BORRADOR, EstadoPlan.ANULADO);

    private final PlanTratamientoService planService;
    private final OdontologoService odontologoService;
    private final ProcedimientoService procedimientoService;
    private final PacienteActivo pacienteActivo;
    private final TareaUi tareaUi;
    private final Notificador notificador;
    private final SesionUsuario sesionUsuario;
    private final Exportador exportador;
    private final ApplicationEventPublisher eventos;

    @FXML private SplitPane raiz;
    @FXML private ComboBox<PlanResumen> cmbPlanes;
    @FXML private Button btnNuevo;
    @FXML private Button btnSugerir;
    @FXML private Button btnAgregar;
    @FXML private Button btnQuitar;
    @FXML private Label lblEstado;
    @FXML private HBox boxTransiciones;
    @FXML private Button btnPresupuesto;
    @FXML private Button btnConsentimiento;
    @FXML private Label lblInfo;
    @FXML private TreeTableView<Fila> tabla;
    @FXML private Label lblSubtotal;
    @FXML private TextField txtDescuento;
    @FXML private Label lblTotal;
    @FXML private Label lblLetras;
    @FXML private TextArea txtObservaciones;
    @FXML private Button btnGuardarCabecera;
    @FXML private VBox panelCronograma;
    @FXML private TextField txtInicial;
    @FXML private Spinner<Integer> spnCuotas;
    @FXML private DatePicker dpPrimerVencimiento;
    @FXML private ChoiceBox<Frecuencia> chFrecuencia;
    @FXML private Button btnGenerar;
    @FXML private Label lblCronograma;
    @FXML private TableView<CuotaDto> tblCuotas;

    private ChangeListener<PacienteDetalle> oyentePaciente;
    private PlanDto actual;
    /** Evita recargar el plan al llenar el ComboBox desde el código. */
    private boolean llenando;
    /** Este controlador está publicando {@link PlanCambiado}. */
    private boolean avisando;

    /** Fila del árbol: un grupo por fase (con su subtotal) o un procedimiento. */
    public record Fila(String fase, BigDecimal subtotalFase, PlanItemDto item) {

        boolean esGrupo() {
            return item == null;
        }
    }

    @FXML
    public void initialize() {
        configurarTabla();
        configurarCuotas();
        spnCuotas.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 60, 3));
        Formularios.configurarFecha(dpPrimerVencimiento);
        chFrecuencia.getItems().setAll(Frecuencia.values());
        chFrecuencia.setConverter(Formularios.conversor(f -> switch (f) {
            case SEMANAL -> "Semanal";
            case QUINCENAL -> "Quincenal";
            case MENSUAL -> "Mensual";
        }));
        chFrecuencia.setValue(Frecuencia.MENSUAL);
        cmbPlanes.valueProperty().addListener((obs, a, b) -> {
            if (!llenando && b != null && (actual == null || actual.id() != b.id())) {
                cargarPlan(b.id());
            }
        });
        boolean edita = sesionUsuario.tiene(Permiso.PLAN_EDITAR);
        btnNuevo.setVisible(edita);
        btnNuevo.setManaged(edita);
        mostrar(null);

        if (oyentePaciente != null) {
            pacienteActivo.pacienteProperty().removeListener(oyentePaciente);
        }
        oyentePaciente = (obs, anterior, nuevo) -> {
            if (nuevo == null || anterior == null || nuevo.id() != anterior.id()) {
                actual = null;
            }
            recargar(null);
        };
        pacienteActivo.pacienteProperty().addListener(oyentePaciente);
        recargar(null);
    }

    // ------------------------------------------------------------------ carga

    /** Recarga la lista de planes y muestra el indicado (o el actual, o el más reciente). */
    public void recargar(Long seleccionar) {
        PacienteDetalle p = pacienteActivo.get();
        if (p == null || cmbPlanes == null) {
            return;
        }
        Long objetivo = seleccionar != null ? seleccionar : actual == null ? null : actual.id();
        tareaUi.ejecutar(() -> {
            List<PlanResumen> planes = planService.listarPorPaciente(p.id());
            PlanResumen elegido = planes.stream().filter(r -> objetivo != null && r.id() == objetivo).findFirst()
                    .orElse(planes.isEmpty() ? null : planes.get(0));
            return new Carga(planes, elegido == null ? null : planService.obtener(elegido.id()));
        }, c -> {
            llenando = true;
            cmbPlanes.getItems().setAll(c.planes());
            cmbPlanes.setValue(c.plan() == null ? null : c.planes().stream()
                    .filter(r -> r.id() == c.plan().id()).findFirst().orElse(null));
            llenando = false;
            mostrar(c.plan());
        });
    }

    private record Carga(List<PlanResumen> planes, PlanDto plan) {
    }

    private void cargarPlan(long id) {
        tareaUi.ejecutar(() -> planService.obtener(id), this::mostrar);
    }

    /** Otra pestaña (Atenciones) cambió un plan del paciente. */
    @EventListener
    public void alCambiarPlan(PlanCambiado e) {
        if (avisando) {
            return;
        }
        Runnable r = () -> {
            PacienteDetalle p = pacienteActivo.get();
            if (p != null && p.id() == e.pacienteId()) {
                recargar(null);
            }
        };
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }

    /** Muestra el plan actualizado y avisa a las demás pestañas. */
    private void aplicar(PlanDto p) {
        boolean cambioEstado = actual == null || actual.estado() != p.estado() || actual.total().compareTo(p.total()) != 0;
        mostrar(p);
        if (cambioEstado) {
            int i = cmbPlanes.getItems().indexOf(cmbPlanes.getItems().stream().filter(r -> r.id() == p.id()).findFirst()
                    .orElse(null));
            if (i >= 0) {
                llenando = true;
                PlanResumen nuevo = new PlanResumen(p.id(), p.fecha(), p.estado(), p.total(), p.odontologoNombre());
                cmbPlanes.getItems().set(i, nuevo);
                cmbPlanes.setValue(nuevo);
                llenando = false;
            }
        }
        avisarCambioDePlan(p.pacienteId());
    }

    private void mostrar(PlanDto p) {
        actual = p;
        boolean hay = p != null;
        boolean edita = sesionUsuario.tiene(Permiso.PLAN_EDITAR);
        boolean editable = hay && edita && p.editable();

        btnSugerir.setDisable(!editable);
        btnAgregar.setDisable(!editable);
        btnQuitar.setDisable(true);
        btnPresupuesto.setDisable(!hay);
        btnConsentimiento.setDisable(!hay || p.items().isEmpty());
        txtDescuento.setEditable(editable);
        txtObservaciones.setEditable(hay && edita && !p.estado().esFinal());
        btnGuardarCabecera.setDisable(!hay || !edita || p.estado().esFinal());

        lblEstado.setText(hay ? p.estado().etiqueta() : "");
        lblEstado.getStyleClass().removeAll(Styles.SUCCESS, Styles.WARNING, Styles.DANGER, Styles.ACCENT);
        if (hay) {
            lblEstado.getStyleClass().add(switch (p.estado()) {
                case BORRADOR, PROPUESTO -> Styles.WARNING;
                case ACEPTADO, EN_CURSO -> Styles.ACCENT;
                case FINALIZADO -> Styles.SUCCESS;
                case ANULADO -> Styles.DANGER;
            });
        }
        construirTransiciones(p, edita);
        lblInfo.setText(info(p));

        tabla.setRoot(arbol(p));
        tabla.setPlaceholder(new Label(hay ? "El plan aún no tiene procedimientos: use «Sugerir desde odontograma» "
                + "o «Agregar procedimiento»." : "El paciente no tiene planes de tratamiento."));

        lblSubtotal.setText(hay ? Formularios.soles(p.subtotal()) : "—");
        txtDescuento.setText(hay ? p.descuento().setScale(2).toPlainString() : "");
        lblTotal.setText(hay ? Formularios.soles(p.total()) : "—");
        lblLetras.setText(hay ? "Son: " + NumeroALetras.soles(p.totalCent()) : "");
        txtObservaciones.setText(hay && p.observaciones() != null ? p.observaciones() : "");

        tblCuotas.getItems().setAll(hay ? p.cuotas() : List.of());
        boolean cronograma = hay && edita && p.estado().admiteCronograma() && !p.tienePagos();
        for (var n : List.of(txtInicial, spnCuotas, dpPrimerVencimiento, chFrecuencia, btnGenerar)) {
            n.setDisable(!cronograma);
        }
        if (hay && dpPrimerVencimiento.getValue() == null) {
            dpPrimerVencimiento.setValue(LocalDate.now().plusMonths(1));
        }
        lblCronograma.setText(textoCronograma(p));
    }

    private static String info(PlanDto p) {
        if (p == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder("Responsable: C.D. ").append(p.odontologoNombre())
                .append(" · creado el ").append(Formularios.fechaHora(p.fecha()));
        if (p.odontogramaId() != null) {
            sb.append(" · sugerencias del odontograma firmado N° ").append(p.odontogramaId());
        }
        if (p.consentimientoFirmadoPor() != null) {
            sb.append(" · consentimiento firmado por ").append(p.consentimientoFirmadoPor()).append(" el ")
                    .append(Formularios.fechaHora(p.consentimientoEn()));
        }
        if (!p.items().isEmpty() && (p.estado().enEjecucion() || p.estado() == EstadoPlan.FINALIZADO)) {
            sb.append(" · ").append(p.realizados()).append(" de ").append(p.items().size()).append(" realizado(s)");
        }
        return sb.toString();
    }

    private static String textoCronograma(PlanDto p) {
        if (p == null) {
            return "";
        }
        if (p.tienePagos()) {
            return "El plan tiene pagos por " + Formularios.soles(p.pagado()) + ": el cronograma ya no se puede regenerar.";
        }
        if (p.estado() == EstadoPlan.BORRADOR) {
            return "Proponga el plan para generar el cronograma (el total ya no cambiará).";
        }
        if (p.estado().esFinal()) {
            return "";
        }
        if (p.cuotas().isEmpty()) {
            return "Sin cronograma: al aceptar el plan se crea una cuota única al contado.";
        }
        return "Generar de nuevo reemplaza las cuotas actuales. La cuota inicial vence hoy; los domingos pasan al lunes.";
    }

    // ------------------------------------------------------------------ tabla de procedimientos

    private TreeItem<Fila> arbol(PlanDto p) {
        TreeItem<Fila> raizArbol = new TreeItem<>(new Fila("", BigDecimal.ZERO, null));
        if (p == null) {
            return raizArbol;
        }
        Map<Integer, List<PlanItemDto>> porFase = p.items().stream()
                .collect(Collectors.groupingBy(PlanItemDto::fase, TreeMap::new, Collectors.toList()));
        porFase.forEach((fase, items) -> {
            BigDecimal subtotal = items.stream().map(PlanItemDto::total).reduce(BigDecimal.ZERO, BigDecimal::add);
            TreeItem<Fila> grupo = new TreeItem<>(new Fila(PlanItemDto.nombreFase(fase), subtotal, null));
            grupo.setExpanded(true);
            for (PlanItemDto i : items) {
                grupo.getChildren().add(new TreeItem<>(new Fila(null, null, i)));
            }
            raizArbol.getChildren().add(grupo);
        });
        return raizArbol;
    }

    private void configurarTabla() {
        TreeTableColumn<Fila, String> procedimiento = columna("Procedimiento", f -> f.esGrupo() ? f.fase()
                : f.item().procedimientoCodigo() + " · " + f.item().procedimientoNombre(), 300);
        TreeTableColumn<Fila, String> pieza = columna("Pieza", f -> f.esGrupo() || f.item().pieza() == null ? ""
                : String.valueOf(f.item().pieza()), 60);
        TreeTableColumn<Fila, String> sup = columna("Sup.", f -> f.esGrupo() ? "" : f.item().superficiesTexto(), 60);
        TreeTableColumn<Fila, String> cantidad = columna("Cant.", f -> f.esGrupo() ? "" : String.valueOf(f.item().cantidad()), 55);
        TreeTableColumn<Fila, String> precio = columna("P. unit.", f -> f.esGrupo() ? "" : Formularios.soles(f.item().precioUnit()), 95);
        TreeTableColumn<Fila, String> descuento = columna("Descuento", f -> f.esGrupo() ? ""
                : f.item().descuento().setScale(2).toPlainString(), 95);
        TreeTableColumn<Fila, String> total = columna("Total", f -> Formularios.soles(f.esGrupo() ? f.subtotalFase()
                : f.item().total()), 100);
        TreeTableColumn<Fila, String> estado = columna("Estado", f -> f.esGrupo() ? "" : f.item().estado().etiqueta()
                + (f.item().realizadoEn() == null ? "" : " · " + Formularios.fecha(f.item().realizadoEn().toLocalDate())), 140);
        for (var c : List.of(cantidad, precio, descuento, total)) {
            c.setStyle("-fx-alignment: CENTER-RIGHT;");
        }

        // Descuento editable en la tabla (solo en borrador y en filas de procedimiento).
        descuento.setEditable(true);
        descuento.setCellFactory(c -> new TextFieldTreeTableCell<>(new DefaultStringConverter()) {
            @Override
            public void startEdit() {
                TreeItem<Fila> fila = getTableRow() == null ? null : getTableRow().getTreeItem();
                if (fila != null && !fila.getValue().esGrupo() && actual != null && actual.editable()
                        && sesionUsuario.tiene(Permiso.PLAN_EDITAR)) {
                    super.startEdit();
                }
            }
        });
        descuento.setOnEditCommit(e -> {
            Fila f = e.getRowValue().getValue();
            try {
                BigDecimal monto = Formularios.monto(e.getNewValue(), "descuento");
                tareaUi.ejecutar(() -> planService.actualizarDescuentoItem(f.item().id(), monto), this::aplicar,
                        error -> {
                            tareaUi.manejarError(error);
                            mostrar(actual);
                        });
            } catch (RuntimeException ex) {
                tareaUi.manejarError(ex);
                mostrar(actual);
            }
        });

        tabla.getColumns().setAll(List.of(procedimiento, pieza, sup, cantidad, precio, descuento, total, estado));
        tabla.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabla.setRowFactory(t -> {
            TreeTableRow<Fila> fila = new TreeTableRow<>() {
                @Override
                protected void updateItem(Fila f, boolean vacio) {
                    super.updateItem(f, vacio);
                    getStyleClass().remove("plan-fila-fase");
                    if (!vacio && f != null && f.esGrupo()) {
                        getStyleClass().add("plan-fila-fase");
                    }
                }
            };
            fila.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !fila.isEmpty()
                        && !fila.getItem().esGrupo() && actual != null && actual.editable()
                        && sesionUsuario.tiene(Permiso.PLAN_EDITAR)) {
                    formularioItem(fila.getItem().item());
                }
            });
            return fila;
        });
        tabla.getSelectionModel().selectedItemProperty().addListener((obs, a, b) -> btnQuitar.setDisable(
                b == null || b.getValue().esGrupo() || actual == null || !actual.editable()
                        || !sesionUsuario.tiene(Permiso.PLAN_EDITAR)));
    }

    private static TreeTableColumn<Fila, String> columna(String titulo, Function<Fila, String> valor, double ancho) {
        TreeTableColumn<Fila, String> c = new TreeTableColumn<>(titulo);
        c.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getValue() == null ? ""
                : valor.apply(f.getValue().getValue())));
        c.setPrefWidth(ancho);
        c.setSortable(false);
        return c;
    }

    private void configurarCuotas() {
        TableColumn<CuotaDto, String> estado = Formularios.columna("Estado", c -> etiqueta(c.estado()), 100);
        estado.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String texto, boolean vacio) {
                super.updateItem(texto, vacio);
                getStyleClass().removeAll(Styles.SUCCESS, Styles.DANGER, Styles.WARNING);
                setText(vacio ? null : texto);
                CuotaDto cuota = vacio || getTableRow() == null ? null : getTableRow().getItem();
                if (cuota != null) {
                    switch (cuota.estado()) {
                        case PAGADA -> getStyleClass().add(Styles.SUCCESS);
                        case VENCIDA -> getStyleClass().add(Styles.DANGER);
                        case PARCIAL -> getStyleClass().add(Styles.WARNING);
                        default -> {
                        }
                    }
                }
            }
        });
        Formularios.columnas(tblCuotas,
                Formularios.columna("Cuota", CuotaDto::etiqueta, 80),
                Formularios.columna("Vencimiento", c -> Formularios.fecha(c.vencimiento()), 100),
                Formularios.columna("Monto", c -> Formularios.soles(c.monto()), 100),
                Formularios.columna("Pagado", c -> Formularios.soles(c.pagado()), 100),
                Formularios.columna("Saldo", c -> Formularios.soles(c.saldo()), 100),
                estado);
        tblCuotas.setPlaceholder(new Label("Sin cronograma de cuotas."));
    }

    private static String etiqueta(EstadoCuota e) {
        return switch (e) {
            case PENDIENTE -> "Pendiente";
            case PARCIAL -> "Parcial";
            case VENCIDA -> "Vencida";
            case PAGADA -> "Pagada";
        };
    }

    // ------------------------------------------------------------------ transiciones

    private void construirTransiciones(PlanDto p, boolean edita) {
        boxTransiciones.getChildren().clear();
        if (p == null || !edita) {
            return;
        }
        for (EstadoPlan destino : ORDEN_TRANSICIONES) {
            if (!p.transiciones().contains(destino)) {
                continue;
            }
            Button b = switch (destino) {
                case PROPUESTO -> Formularios.boton("Proponer", Feather.SEND);
                case ACEPTADO -> Formularios.boton("Aceptar…", Feather.CHECK_CIRCLE, Styles.SUCCESS, Styles.BUTTON_OUTLINED);
                case EN_CURSO -> Formularios.boton("Iniciar", Feather.PLAY);
                case FINALIZADO -> Formularios.boton("Finalizar", Feather.FLAG, Styles.SUCCESS, Styles.BUTTON_OUTLINED);
                case BORRADOR -> Formularios.boton("Volver a borrador", Feather.EDIT_2);
                case ANULADO -> Formularios.boton("Anular", Feather.X_CIRCLE, Styles.DANGER, Styles.BUTTON_OUTLINED);
            };
            b.setOnAction(e -> transicion(b, destino));
            boxTransiciones.getChildren().add(b);
        }
    }

    private void transicion(Button origen, EstadoPlan destino) {
        PlanDto p = actual;
        if (p == null) {
            return;
        }
        if (destino == EstadoPlan.ACEPTADO) {
            formularioAceptar(p);
            return;
        }
        String pregunta = switch (destino) {
            case PROPUESTO -> "El plan pasará a propuesto: ya no se podrán editar procedimientos ni precios "
                    + "(puede volver a borrador mientras no tenga pagos). ¿Continuar?";
            case BORRADOR -> "El plan volverá a borrador para editarlo; se borrará el cronograma de cuotas. ¿Continuar?";
            case ANULADO -> "El plan quedará anulado y no se podrá reabrir. ¿Anular el plan?";
            case FINALIZADO -> "Todos los procedimientos están realizados. ¿Finalizar el plan?";
            default -> null;
        };
        if (pregunta != null && !notificador.confirmar(origen.getText().replace("…", ""), pregunta)) {
            return;
        }
        tareaUi.ejecutar(origen, () -> planService.cambiarEstado(p.id(), destino), d -> {
            notificador.exito("Plan " + d.estado().etiqueta().toLowerCase() + ".");
            aplicar(d);
        });
    }

    /** El paciente acepta: imprime el consentimiento informado y registra quién lo firmó. */
    private void formularioAceptar(PlanDto p) {
        PacienteDetalle paciente = pacienteActivo.get();
        TextField firmante = new TextField(paciente == null ? "" : paciente.esMenorDeEdad()
                && paciente.apoderadoNombre() != null ? paciente.apoderadoNombre() : paciente.nombreCompleto());
        Label texto = new Label("Imprima el consentimiento informado de tratamiento, hágalo firmar por el paciente "
                + "(o su apoderado, si es menor de edad) y registre la aceptación. "
                + (p.cuotas().isEmpty() ? "Como el plan no tiene cronograma, se creará una cuota única de "
                + Formularios.soles(p.total()) + " con vencimiento hoy." : "El cronograma de " + p.cuotas().size()
                + " cuota(s) se mantiene."));
        texto.setWrapText(true);
        Button imprimir = Formularios.boton("Imprimir consentimiento", Feather.PRINTER);
        imprimir.setOnAction(e -> exportador.guardarPdf(imprimir, "consentimiento-tratamiento-plan-" + p.id() + ".pdf",
                () -> planService.pdfConsentimiento(p.id())));
        GridPane g = Formularios.grilla();
        Formularios.fila(g, "Plan", new Label(p.etiqueta()));
        Formularios.fila(g, "Total", new Label(Formularios.soles(p.total())));
        Formularios.fila(g, "Firmado por *", firmante);

        Button cancelar = Formularios.boton("Cancelar", null);
        cancelar.setOnAction(e -> notificador.cerrarPanel());
        Button aceptar = Formularios.boton("Registrar aceptación", Feather.CHECK, Styles.ACCENT);
        aceptar.setOnAction(e -> tareaUi.ejecutar(aceptar, () -> planService.aceptar(p.id(), firmante.getText()), d -> {
            notificador.cerrarPanel();
            notificador.exito("Plan aceptado. Los procedimientos se marcan como realizados en la pestaña Atenciones.");
            aplicar(d);
        }));
        notificador.abrirPanel(Formularios.panelLateral("Aceptar plan de tratamiento", new VBox(14, texto, imprimir, g),
                cancelar, aceptar));
    }

    // ------------------------------------------------------------------ acciones de la barra

    @FXML
    private void onNuevo() {
        PacienteDetalle p = pacienteActivo.get();
        if (p == null || !sesionUsuario.tiene(Permiso.PLAN_EDITAR)) {
            return;
        }
        tareaUi.ejecutar(() -> odontologoService.listar(false), odontologos -> formularioNuevo(p, odontologos));
    }

    private void formularioNuevo(PacienteDetalle p, List<OdontologoDto> odontologos) {
        UsuarioSesion usuario = sesionUsuario.exigirUsuario();
        boolean esOdontologo = Rol.valueOf(usuario.rol()) == Rol.ODONTOLOGO;
        ComboBox<OdontologoDto> odontologo = new ComboBox<>();
        odontologo.getItems().setAll(odontologos);
        odontologo.setValue(odontologos.stream()
                .filter(o -> esOdontologo && usuario.odontologoId() != null && o.id() == usuario.odontologoId())
                .findFirst().orElse(odontologos.size() == 1 ? odontologos.get(0) : null));
        odontologo.setDisable(esOdontologo);
        CheckBox sugerir = new CheckBox("Agregar los procedimientos sugeridos por el último odontograma firmado");
        sugerir.setWrapText(true);
        sugerir.setSelected(true);

        GridPane g = Formularios.grilla();
        Formularios.fila(g, "Paciente", new Label(p.nombreCompleto() + " · " + p.numeroHistoria()));
        Formularios.fila(g, "Odontólogo *", odontologo);
        Label ayuda = new Label("El plan se crea en borrador. Las sugerencias usan las reglas del catálogo "
                + "(Catálogos → Reglas de sugerencia) y los precios del tarifario.");
        ayuda.setWrapText(true);
        ayuda.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);

        Button cancelar = Formularios.boton("Cancelar", null);
        cancelar.setOnAction(e -> notificador.cerrarPanel());
        Button crear = Formularios.boton("Crear plan", Feather.CHECK, Styles.ACCENT);
        crear.setOnAction(e -> {
            Long odontologoId = odontologo.getValue() == null ? null : odontologo.getValue().id();
            tareaUi.ejecutar(crear, () -> planService.crear(p.id(), odontologoId, sugerir.isSelected()), id -> {
                notificador.cerrarPanel();
                notificador.exito("Plan N° " + id + " creado en borrador.");
                actual = null;
                recargar(id);
                avisarCambioDePlan(p.id());
            });
        });
        notificador.abrirPanel(Formularios.panelLateral("Nuevo plan de tratamiento", new VBox(12, g, sugerir, ayuda),
                cancelar, crear));
    }

    @FXML
    private void onSugerir() {
        PlanDto p = actual;
        if (p == null) {
            return;
        }
        int antes = p.items().size();
        tareaUi.ejecutar(btnSugerir, () -> planService.sugerir(p.id()), d -> {
            notificador.exito((d.items().size() - antes) + " procedimiento(s) sugerido(s) agregado(s).");
            aplicar(d);
        });
    }

    @FXML
    private void onAgregar() {
        formularioItem(null);
    }

    @FXML
    private void onQuitar() {
        PlanDto p = actual;
        TreeItem<Fila> sel = tabla.getSelectionModel().getSelectedItem();
        if (p == null || sel == null || sel.getValue().esGrupo()) {
            return;
        }
        PlanItemDto i = sel.getValue().item();
        if (!notificador.confirmar("Quitar procedimiento", "¿Quitar «" + i.descripcion() + "» del plan?")) {
            return;
        }
        tareaUi.ejecutar(btnQuitar, () -> planService.eliminarItem(i.id()), this::aplicar);
    }

    @FXML
    private void onGuardarCabecera() {
        PlanDto p = actual;
        if (p == null) {
            return;
        }
        BigDecimal descuento = Formularios.monto(txtDescuento.getText(), "descuento global");
        String obs = txtObservaciones.getText();
        tareaUi.ejecutar(btnGuardarCabecera, () -> planService.actualizarCabecera(p.id(), descuento, obs), d -> {
            notificador.exito("Plan guardado.");
            aplicar(d);
        });
    }

    @FXML
    private void onGenerar() {
        PlanDto p = actual;
        if (p == null) {
            return;
        }
        if (!p.cuotas().isEmpty() && !notificador.confirmar("Generar cronograma",
                "Se reemplazarán las " + p.cuotas().size() + " cuota(s) actuales. ¿Continuar?")) {
            return;
        }
        spnCuotas.increment(0);   // confirma lo escrito en el Spinner editable
        CronogramaForm form = new CronogramaForm(p.id(), Formularios.monto(txtInicial.getText(), "monto inicial"),
                spnCuotas.getValue(), Formularios.fecha(dpPrimerVencimiento), chFrecuencia.getValue());
        tareaUi.ejecutar(btnGenerar, () -> planService.generarCronograma(form), d -> {
            notificador.exito("Cronograma generado: " + d.cuotas().size() + " cuota(s).");
            aplicar(d);
        });
    }

    @FXML
    private void onPresupuesto() {
        PlanDto p = actual;
        if (p != null) {
            exportador.guardarPdf(btnPresupuesto, "presupuesto-plan-" + p.id() + ".pdf", () -> planService.pdfPresupuesto(p.id()));
        }
    }

    @FXML
    private void onConsentimiento() {
        PlanDto p = actual;
        if (p != null) {
            exportador.guardarPdf(btnConsentimiento, "consentimiento-tratamiento-plan-" + p.id() + ".pdf",
                    () -> planService.pdfConsentimiento(p.id()));
        }
    }

    // ------------------------------------------------------------------ formulario de ítem

    /** Alta ({@code item == null}) o edición de un procedimiento del plan. */
    private void formularioItem(PlanItemDto item) {
        PlanDto p = actual;
        if (p == null) {
            return;
        }
        tareaUi.ejecutar(() -> procedimientoService.listar(item != null), procs -> formularioItem(p, item, procs));
    }

    private void formularioItem(PlanDto p, PlanItemDto item, List<ProcedimientoDto> procedimientos) {
        ComboBox<ProcedimientoDto> proc = new ComboBox<>();
        proc.getItems().setAll(procedimientos);
        proc.setVisibleRowCount(14);
        TextField pieza = new TextField(item == null || item.pieza() == null ? "" : String.valueOf(item.pieza()));
        pieza.setPromptText("FDI, p. ej. 36 (vacío = sin pieza)");
        Map<Superficie, CheckBox> superficies = new LinkedHashMap<>();
        HBox boxSup = new HBox(10);
        for (Superficie s : Superficie.values()) {
            CheckBox c = new CheckBox(switch (s) {
                case O -> "O/I";
                case L -> "L/P";
                default -> s.name();
            });
            c.setSelected(item != null && item.superficies().contains(s));
            superficies.put(s, c);
            boxSup.getChildren().add(c);
        }
        ComboBox<Opcion<Integer>> fase = new ComboBox<>();
        for (int i = 1; i <= 4; i++) {
            fase.getItems().add(new Opcion<>(PlanItemDto.nombreFase(i), i));
        }
        int faseInicial = item == null ? 2 : item.fase();
        fase.setValue(fase.getItems().get(faseInicial - 1));
        Spinner<Integer> cantidad = new Spinner<>(1, 99, item == null ? 1 : item.cantidad());
        cantidad.setEditable(true);
        TextField precio = new TextField(item == null ? "" : item.precioUnit().setScale(2).toPlainString());
        TextField descuento = new TextField(item == null ? "0.00" : item.descuento().setScale(2).toPlainString());
        Label forma = new Label();
        forma.setWrapText(true);
        forma.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);

        Runnable actualizarForma = () -> {
            ProcedimientoDto d = proc.getValue();
            boolean porSuperficie = d != null && d.formaCobro() == FormaCobro.SUPERFICIE;
            cantidad.setDisable(porSuperficie);
            forma.setText(d == null ? "" : switch (d.formaCobro()) {
                case SUPERFICIE -> "Se cobra por superficie: la cantidad es el número de superficies marcadas.";
                case PIEZA -> "Se cobra por pieza.";
                case ARCADA -> "Se cobra por arcada.";
                case BOCA -> "Se cobra una vez para toda la boca.";
                case SESION -> "Se cobra por sesión: indique el número de sesiones en la cantidad.";
            } + " Precio del tarifario: " + Formularios.soles(d.precio()) + ".");
        };
        proc.valueProperty().addListener((obs, a, b) -> {
            if (b != null && (item == null || b.id() != item.procedimientoId())) {
                precio.setText(b.precio().setScale(2).toPlainString());
            }
            actualizarForma.run();
        });
        proc.setValue(item == null ? null : procedimientos.stream().filter(d -> d.id() == item.procedimientoId())
                .findFirst().orElse(null));
        actualizarForma.run();

        GridPane g = Formularios.grilla();
        Formularios.fila(g, "Procedimiento *", proc);
        Formularios.fila(g, "", forma);
        Formularios.fila(g, "Pieza", pieza);
        Formularios.fila(g, "Superficies", boxSup);
        Formularios.fila(g, "Fase *", fase);
        Formularios.fila(g, "Cantidad", cantidad);
        Formularios.fila(g, "Precio unitario S/", precio);
        Formularios.fila(g, "Descuento S/", descuento);

        Button cancelar = Formularios.boton("Cancelar", null);
        cancelar.setOnAction(e -> notificador.cerrarPanel());
        Button guardar = Formularios.boton(item == null ? "Agregar" : "Guardar", Feather.CHECK, Styles.ACCENT);
        guardar.setOnAction(e -> {
            try {
                cantidad.increment(0);
                Integer fdi = pieza.getText().isBlank() ? null : Formularios.entero(pieza.getText(), "pieza");
                if (fdi != null && !PiezaDental.esValida(fdi)) {
                    throw new com.odontogest.excepcion.NegocioException("La pieza " + fdi + " no es un número FDI válido.");
                }
                Set<Superficie> marcadas = EnumSet.noneOf(Superficie.class);
                superficies.forEach((s, c) -> {
                    if (c.isSelected()) {
                        marcadas.add(s);
                    }
                });
                PlanItemForm form = new PlanItemForm(item == null ? null : item.id(), p.id(),
                        proc.getValue() == null ? null : proc.getValue().id(), fdi, marcadas,
                        Formularios.seleccion(fase), cantidad.getValue(), Formularios.monto(precio.getText(), "precio"),
                        Formularios.monto(descuento.getText(), "descuento"));
                tareaUi.ejecutar(guardar, () -> planService.guardarItem(form), d -> {
                    notificador.cerrarPanel();
                    aplicar(d);
                });
            } catch (RuntimeException ex) {
                tareaUi.manejarError(ex);
            }
        });
        notificador.abrirPanel(Formularios.panelLateral(item == null ? "Agregar procedimiento" : "Editar procedimiento",
                g, cancelar, guardar));
        Platform.runLater(proc::requestFocus);
    }

    /** Avisa a las demás pestañas; el oyente propio ignora el aviso (ya está al día). */
    private void avisarCambioDePlan(long pacienteId) {
        avisando = true;
        try {
            eventos.publishEvent(new PlanCambiado(pacienteId));
        } finally {
            avisando = false;
        }
    }
}
