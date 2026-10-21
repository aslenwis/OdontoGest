/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.controlador;

import atlantafx.base.controls.CustomTextField;
import atlantafx.base.theme.Styles;
import com.odontogest.dominio.finanzas.EstadoCuota;
import com.odontogest.evento.NavegarA;
import com.odontogest.evento.PagoRegistrado;
import com.odontogest.modelo.MedioPago;
import com.odontogest.servicio.PacienteService;
import com.odontogest.servicio.PagoService;
import com.odontogest.servicio.dto.AplicacionDto;
import com.odontogest.servicio.dto.CuotaPacienteDto;
import com.odontogest.servicio.dto.PacienteResumen;
import com.odontogest.servicio.dto.PagoDto;
import com.odontogest.servicio.dto.PagoForm;
import com.odontogest.servicio.dto.ResumenCajaDto;
import com.odontogest.ui.componente.Autocompletar;
import com.odontogest.ui.soporte.AccionesPago;
import com.odontogest.ui.soporte.Formularios;
import com.odontogest.ui.soporte.HiloUi;
import com.odontogest.ui.soporte.Notificador;
import com.odontogest.ui.soporte.Refrescable;
import com.odontogest.ui.soporte.TareaUi;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.odontogest.ui.soporte.Formularios.ESTADO_ALERTA;
import static com.odontogest.ui.soporte.Formularios.ESTADO_OK;
import static com.odontogest.ui.soporte.Formularios.ESTADO_PELIGRO;

/**
 * Caja (RF-CAJ-01, RF-CAJ-05): cobro con vista previa del reparto FIFO que se actualiza al escribir el monto, recibo
 * al terminar, y pagos del día con totales por medio de pago.
 */
@Component
@RequiredArgsConstructor
public class CajaController implements Refrescable {

    private final PagoService pagoService;
    private final PacienteService pacienteService;
    private final AccionesPago accionesPago;
    private final TareaUi tareaUi;
    private final Notificador notificador;
    private final ApplicationEventPublisher eventos;

    @FXML private CustomTextField txtPaciente;
    @FXML private Label lblPaciente;
    @FXML private Label lblSaldo;
    @FXML private TableView<CuotaPacienteDto> tblPendientes;
    @FXML private TextField txtMonto;
    @FXML private Button btnTodo;
    @FXML private Button btnVencido;
    @FXML private ChoiceBox<MedioPago> chMedio;
    @FXML private TextField txtReferencia;
    @FXML private Label lblPrevia;
    @FXML private TableView<AplicacionDto> tblPrevia;
    @FXML private Button btnCobrar;
    @FXML private DatePicker dpFecha;
    @FXML private TableView<PagoDto> tblDia;
    @FXML private Button btnRecibo;
    @FXML private Button btnAnular;
    @FXML private Button btnFicha;
    @FXML private FlowPane boxTotales;
    @FXML private Label lblTotalDia;

    private Autocompletar<PacienteResumen> autocompletar;
    private PauseTransition esperaPrevia;
    private PacienteResumen paciente;
    private List<CuotaPacienteDto> pendientes = List.of();

    @FXML
    public void initialize() {
        autocompletar = new Autocompletar<>(txtPaciente,
                (filtro, entregar) -> tareaUi.ejecutar(() -> pacienteService.buscar(filtro, false, 0).contenido(), entregar),
                p -> p.nombreCompleto() + "  ·  " + p.numeroHistoria() + "  ·  " + p.documento(),
                this::elegirPaciente);
        configurarTablas();
        chMedio.getItems().setAll(MedioPago.values());
        chMedio.setConverter(Formularios.conversor(MedioPago::etiqueta));
        chMedio.setValue(MedioPago.EFECTIVO);
        chMedio.valueProperty().addListener((obs, a, b) -> txtReferencia.setPromptText(b != null && b.pideReferencia()
                ? "N° de operación (recomendado)" : "Opcional"));
        Formularios.configurarFecha(dpFecha);
        dpFecha.setValue(LocalDate.now());
        dpFecha.valueProperty().addListener((obs, a, b) -> cargarDia());
        esperaPrevia = new PauseTransition(Duration.millis(250));
        esperaPrevia.setOnFinished(e -> actualizarPrevia());
        txtMonto.textProperty().addListener((obs, a, b) -> esperaPrevia.playFromStart());
        tblDia.getSelectionModel().selectedItemProperty().addListener((obs, a, b) -> actualizarBotonesDia());
        mostrarPaciente(null, List.of());
    }

    @Override
    public void refrescar() {
        cargarDia();
        if (paciente != null) {
            cargarPendientes();
        }
    }

    /** Desde la ficha ("Cobrar en caja"): deja elegido al paciente. */
    public void seleccionarPaciente(long pacienteId) {
        tareaUi.ejecutar(() -> {
            var d = pacienteService.obtener(pacienteId);
            return new PacienteResumen(d.id(), d.numeroHistoria(), d.documento(), d.nombreCompleto(), d.edad(),
                    d.telefono(), d.activo(), d.demo());
        }, p -> {
            autocompletar.fijar(p);
            elegirPaciente(p);
        });
    }

    /** Un pago registrado o anulado (tras confirmarse la transacción). */
    @TransactionalEventListener(fallbackExecution = true)
    public void alRegistrarPago(PagoRegistrado e) {
        HiloUi.ejecutar(() -> {
            if (tblDia != null && tblDia.getScene() != null) {
                refrescar();
            }
        });
    }

    // ------------------------------------------------------------------ cobro

    private void elegirPaciente(PacienteResumen p) {
        paciente = p;
        txtMonto.clear();
        txtReferencia.clear();
        cargarPendientes();
    }

    private void cargarPendientes() {
        PacienteResumen p = paciente;
        tareaUi.ejecutar(() -> pagoService.cuotasPendientes(p.id()), cuotas -> {
            if (p == paciente) {
                mostrarPaciente(p, cuotas);
                actualizarPrevia();
            }
        });
    }

    private void mostrarPaciente(PacienteResumen p, List<CuotaPacienteDto> cuotas) {
        pendientes = cuotas;
        tblPendientes.getItems().setAll(cuotas);
        lblPaciente.setText(p == null ? "Elija un paciente" : p.nombreCompleto() + " · " + p.numeroHistoria());
        BigDecimal saldo = cuotas.stream().map(CuotaPacienteDto::saldo).reduce(BigDecimal.ZERO, BigDecimal::add);
        lblSaldo.setText(p == null ? "" : "Saldo: " + Formularios.soles(saldo));
        tblPendientes.setPlaceholder(new Label(p == null ? "Busque un paciente para ver sus cuotas."
                : "El paciente no tiene cuotas pendientes (los planes se cobran desde que son aceptados)."));
        boolean hay = p != null && !cuotas.isEmpty();
        for (var n : List.of(txtMonto, btnTodo, chMedio, txtReferencia, btnCobrar)) {
            n.setDisable(!hay);
        }
        btnVencido.setDisable(!hay || cuotas.stream().noneMatch(c -> c.estado() == EstadoCuota.VENCIDA));
    }

    private void actualizarPrevia() {
        PacienteResumen p = paciente;
        tblPrevia.getItems().clear();
        lblPrevia.getStyleClass().remove(Styles.DANGER);
        if (!lblPrevia.getStyleClass().contains(Styles.TEXT_MUTED)) {
            lblPrevia.getStyleClass().add(Styles.TEXT_MUTED);
        }
        if (p == null || pendientes.isEmpty()) {
            lblPrevia.setText("");
            return;
        }
        BigDecimal monto;
        try {
            monto = Formularios.monto(txtMonto.getText(), "monto");
        } catch (RuntimeException e) {
            error(e.getMessage());
            return;
        }
        if (monto == null) {
            lblPrevia.setText("Escriba el monto para ver a qué cuotas se aplica.");
            return;
        }
        tareaUi.ejecutar(() -> pagoService.vistaPrevia(p.id(), monto), aplicaciones -> {
            if (p != paciente) {
                return;
            }
            tblPrevia.getItems().setAll(aplicaciones);
            BigDecimal saldo = pendientes.stream().map(CuotaPacienteDto::saldo).reduce(BigDecimal.ZERO, BigDecimal::add);
            lblPrevia.setText("Se aplica a " + aplicaciones.size() + " cuota(s). Saldo después del pago: "
                    + Formularios.soles(saldo.subtract(monto)) + ".");
        }, ex -> error(ex.getMessage()));
    }

    private void error(String mensaje) {
        lblPrevia.setText(mensaje);
        lblPrevia.getStyleClass().remove(Styles.TEXT_MUTED);
        if (!lblPrevia.getStyleClass().contains(Styles.DANGER)) {
            lblPrevia.getStyleClass().add(Styles.DANGER);
        }
    }

    @FXML
    private void onTodo() {
        txtMonto.setText(pendientes.stream().map(CuotaPacienteDto::saldo).reduce(BigDecimal.ZERO, BigDecimal::add)
                .toPlainString());
    }

    @FXML
    private void onVencido() {
        txtMonto.setText(pendientes.stream().filter(c -> c.estado() == EstadoCuota.VENCIDA).map(CuotaPacienteDto::saldo)
                .reduce(BigDecimal.ZERO, BigDecimal::add).toPlainString());
    }

    @FXML
    private void onCobrar() {
        PacienteResumen p = paciente;
        if (p == null) {
            return;
        }
        PagoForm form = new PagoForm(p.id(), Formularios.monto(txtMonto.getText(), "monto"), chMedio.getValue(),
                txtReferencia.getText());
        if (form.monto() != null && !notificador.confirmar("Cobrar", "¿Registrar el cobro de " + Formularios.soles(form.monto())
                + " (" + (form.medio() == null ? "—" : form.medio().etiqueta()) + ") a " + p.nombreCompleto() + "?")) {
            return;
        }
        tareaUi.ejecutar(btnCobrar, () -> pagoService.registrar(form), pago -> {
            txtMonto.clear();
            txtReferencia.clear();
            refrescar();
            exito(pago);
        });
    }

    /** Panel de éxito con el botón para imprimir el recibo. */
    private void exito(PagoDto pago) {
        Label numero = new Label("Recibo " + pago.numeroRecibo());
        numero.getStyleClass().add(Styles.TITLE_3);
        GridPane g = Formularios.grilla();
        Formularios.fila(g, "Paciente", new Label(pago.pacienteNombre()));
        Formularios.fila(g, "Monto", new Label(Formularios.soles(pago.monto()) + " · " + pago.medio().etiqueta()));
        Label aplicado = new Label(AccionesPago.aplicadoA(pago));
        aplicado.setWrapText(true);
        Formularios.fila(g, "Aplicado a", aplicado);
        Button cerrar = Formularios.boton("Cerrar", null);
        cerrar.setOnAction(e -> notificador.cerrarPanel());
        Button imprimir = Formularios.boton("Imprimir recibo", Feather.PRINTER, Styles.ACCENT);
        imprimir.setOnAction(e -> {
            notificador.cerrarPanel();
            accionesPago.imprimirRecibo(btnRecibo, pago);
        });
        notificador.abrirPanel(Formularios.panelLateral("Pago registrado", new VBox(14, numero, g), cerrar, imprimir));
    }

    // ------------------------------------------------------------------ caja del día

    private void cargarDia() {
        LocalDate fecha = dpFecha.getValue() == null ? LocalDate.now() : dpFecha.getValue();
        tareaUi.ejecutar(() -> pagoService.cajaDelDia(fecha), this::mostrarDia);
    }

    private void mostrarDia(ResumenCajaDto r) {
        tblDia.getItems().setAll(r.pagos());
        tblDia.setPlaceholder(new Label("Sin cobros el " + Formularios.fecha(r.fecha()) + "."));
        boxTotales.getChildren().clear();
        for (MedioPago m : MedioPago.values()) {
            Label l = new Label(m.etiqueta() + ": " + Formularios.soles(r.porMedio().get(m)));
            if (r.porMedio().get(m).signum() == 0) {
                l.getStyleClass().add(Styles.TEXT_MUTED);
            }
            boxTotales.getChildren().add(l);
        }
        lblTotalDia.setText("Total: " + Formularios.soles(r.total()));
        actualizarBotonesDia();
    }

    private void actualizarBotonesDia() {
        PagoDto p = tblDia.getSelectionModel().getSelectedItem();
        btnRecibo.setDisable(p == null);
        btnFicha.setDisable(p == null);
        btnAnular.setDisable(!accionesPago.puedeAnular(p));
    }

    @FXML
    private void onHoy() {
        dpFecha.setValue(LocalDate.now());
    }

    @FXML
    private void onRecibo() {
        accionesPago.imprimirRecibo(btnRecibo, tblDia.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void onAnular() {
        accionesPago.anular(tblDia.getSelectionModel().getSelectedItem(), d -> refrescar());
    }

    @FXML
    private void onFicha() {
        PagoDto p = tblDia.getSelectionModel().getSelectedItem();
        if (p != null) {
            eventos.publishEvent(new NavegarA(MainController.VISTA_FICHA, Map.of("pacienteId", p.pacienteId(), "pestana", "pagos")));
        }
    }

    // ------------------------------------------------------------------ tablas

    private void configurarTablas() {
        TableColumn<CuotaPacienteDto, String> estado = Formularios.columna("Estado", c -> etiqueta(c.estado()), 90);
        estado.setCellFactory(c -> celdaEstado());
        Formularios.columnas(tblPendientes,
                Formularios.columna("Cuota", CuotaPacienteDto::etiqueta, 150),
                Formularios.columna("Vence", c -> Formularios.fecha(c.vencimiento()), 90),
                Formularios.columna("Monto", c -> Formularios.soles(c.monto()), 90),
                Formularios.columna("Saldo", c -> Formularios.soles(c.saldo()), 90),
                estado);

        Formularios.columnas(tblPrevia,
                Formularios.columna("Cuota", AplicacionDto::etiqueta, 150),
                Formularios.columna("Vence", a -> Formularios.fecha(a.vencimiento()), 90),
                Formularios.columna("Saldo antes", a -> Formularios.soles(a.saldoAntes()), 95),
                Formularios.columna("Se aplica", a -> Formularios.soles(a.monto()), 95),
                Formularios.columna("Saldo después", a -> Formularios.soles(a.saldoDespues()), 100));
        tblPrevia.setPlaceholder(new Label("—"));

        TableColumn<PagoDto, String> hora = Formularios.columna("Hora", p -> Formularios.HORA.format(p.fecha()), 60);
        hora.setMinWidth(60);
        TableColumn<PagoDto, String> colRecibo = Formularios.columna("Recibo", PagoDto::numeroRecibo, 105);
        colRecibo.setMinWidth(100);
        Formularios.columnas(tblDia,
                colRecibo,
                hora,
                Formularios.columna("Paciente", PagoDto::pacienteNombre, 170),
                Formularios.columna("Medio", p -> p.medio().etiqueta(), 90),
                Formularios.columna("Monto", p -> Formularios.soles(p.monto()), 90),
                AccionesPago.columnaEstado());
        tblDia.setRowFactory(t -> {
            TableRow<PagoDto> fila = new TableRow<>();
            MenuItem recibo = new MenuItem("Recibo PDF");
            recibo.setOnAction(e -> accionesPago.imprimirRecibo(btnRecibo, fila.getItem()));
            MenuItem anular = new MenuItem("Anular…");
            anular.setOnAction(e -> accionesPago.anular(fila.getItem(), d -> refrescar()));
            ContextMenu menu = new ContextMenu(recibo, anular);
            menu.setOnShowing(e -> anular.setDisable(!accionesPago.puedeAnular(fila.getItem())));
            fila.contextMenuProperty().bind(javafx.beans.binding.Bindings.when(fila.emptyProperty())
                    .then((ContextMenu) null).otherwise(menu));
            return fila;
        });
    }

    static TableCell<CuotaPacienteDto, String> celdaEstado() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String texto, boolean vacio) {
                super.updateItem(texto, vacio);
                getStyleClass().removeAll(ESTADO_OK, ESTADO_ALERTA, ESTADO_PELIGRO);
                setText(vacio ? null : texto);
                CuotaPacienteDto c = vacio || getTableRow() == null ? null : getTableRow().getItem();
                if (c != null) {
                    switch (c.estado()) {
                        case PAGADA -> getStyleClass().add(ESTADO_OK);
                        case VENCIDA -> getStyleClass().add(ESTADO_PELIGRO);
                        case PARCIAL -> getStyleClass().add(ESTADO_ALERTA);
                        default -> {
                        }
                    }
                }
            }
        };
    }

    static String etiqueta(EstadoCuota e) {
        return switch (e) {
            case PENDIENTE -> "Pendiente";
            case PARCIAL -> "Parcial";
            case VENCIDA -> "Vencida";
            case PAGADA -> "Pagada";
        };
    }
}
