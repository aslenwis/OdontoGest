/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.controlador;

import atlantafx.base.theme.Styles;
import com.odontogest.evento.CitaCambiada;
import com.odontogest.evento.NavegarA;
import com.odontogest.evento.PagoRegistrado;
import com.odontogest.modelo.EstadoCita;
import com.odontogest.seguridad.Permiso;
import com.odontogest.seguridad.SesionUsuario;
import com.odontogest.servicio.DashboardService;
import com.odontogest.servicio.ReporteService;
import com.odontogest.servicio.dto.CitaDto;
import com.odontogest.servicio.dto.DashboardDto;
import com.odontogest.servicio.dto.MontoPeriodoDto;
import com.odontogest.servicio.dto.MorosoDto;
import com.odontogest.servicio.dto.TipoReporte;
import com.odontogest.ui.componente.TarjetaKpi;
import com.odontogest.ui.soporte.Formularios;
import com.odontogest.ui.soporte.Graficos;
import com.odontogest.ui.soporte.HiloUi;
import com.odontogest.ui.soporte.Refrescable;
import com.odontogest.ui.soporte.TareaUi;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/**
 * Tablero de inicio (RF-REP-01): indicadores del día y del mes, gráfico de los últimos seis meses, citas de hoy y
 * pacientes con mayor deuda vencida. Se recarga al mostrarse y cuando cambia una cita o se registra un pago.
 */
@Component
@RequiredArgsConstructor
public class DashboardController implements Refrescable {

    /** Días de atraso desde los que la deuda se resalta en rojo (antes, en ámbar). */
    static final long DIAS_ATRASO_GRAVE = 30;
    private static final DateTimeFormatter FECHA_LARGA =
            DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", Locale.of("es", "PE"));

    private final DashboardService dashboardService;
    private final ReporteService reporteService;
    private final SesionUsuario sesionUsuario;
    private final TareaUi tareaUi;
    private final ApplicationEventPublisher eventos;

    @FXML private Label lblSaludo;
    @FXML private Label lblFecha;
    @FXML private Button btnActualizar;
    @FXML private TarjetaKpi kpiCitas;
    @FXML private TarjetaKpi kpiMonto;
    @FXML private TarjetaKpi kpiPacientes;
    @FXML private TarjetaKpi kpiVencidas;
    @FXML private Label lblGrafico;
    @FXML private BarChart<String, Number> grafico;
    @FXML private NumberAxis ejeMonto;
    @FXML private Label lblCitas;
    @FXML private Button btnAgenda;
    @FXML private ListView<CitaDto> lstCitas;
    @FXML private Button btnMorosidad;
    @FXML private VBox panelMorosos;
    @FXML private TableView<MorosoDto> tblMorosos;

    @FXML
    public void initialize() {
        lblSaludo.setText("Bienvenido(a), " + sesionUsuario.exigirUsuario().nombreCompleto());
        boolean agenda = sesionUsuario.tiene(Permiso.AGENDA_GESTIONAR);
        btnAgenda.setVisible(agenda);
        kpiCitas.setAlHacerClic(agenda ? this::onAgenda : null);
        // La lista de deudores sigue los permisos del reporte de morosidad (administrador y recepción).
        boolean morosidad = sesionUsuario.tiene(Permiso.REPORTES_VER)
                && reporteService.disponibles().contains(TipoReporte.MOROSIDAD);
        panelMorosos.setVisible(morosidad);
        panelMorosos.setManaged(morosidad);
        ejeMonto.setTickLabelFormatter(new NumberAxis.DefaultFormatter(ejeMonto, "S/ ", null));
        Graficos.limitarAnchoBarras(grafico);
        configurarCitas();
        configurarMorosos();
    }

    @Override
    public void refrescar() {
        lblFecha.setText(primeraMayuscula(FECHA_LARGA.format(LocalDate.now())));
        tareaUi.ejecutar(btnActualizar, dashboardService::cargar, this::mostrar);
    }

    /** Una cita programada, movida o atendida (tras confirmarse la transacción). */
    @TransactionalEventListener(fallbackExecution = true)
    public void alCambiarCita(CitaCambiada e) {
        refrescarSiVisible();
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void alRegistrarPago(PagoRegistrado e) {
        refrescarSiVisible();
    }

    private void refrescarSiVisible() {
        HiloUi.ejecutar(() -> {
            if (lstCitas != null && lstCitas.getScene() != null) {
                refrescar();
            }
        });
    }

    private void mostrar(DashboardDto d) {
        long atendidas = d.agendaHoy().stream().filter(c -> c.estado() == EstadoCita.ATENDIDA).count();
        kpiCitas.mostrar(String.valueOf(d.citasHoy()), d.citasHoy() == 0 ? "Sin citas programadas"
                : atendidas + " atendida(s), " + (d.citasHoy() - atendidas) + " por atender", false);
        kpiMonto.setTitulo(d.esProduccion() ? "Mi producción del mes" : "Ingresos del mes");
        kpiMonto.mostrar(Formularios.soles(d.montoMes()), d.esProduccion()
                ? "Procedimientos realizados" : "Pagos registrados en caja, sin anulados", false);
        kpiPacientes.mostrar(String.valueOf(d.pacientesNuevosMes()), "Historias clínicas abiertas este mes", false);
        kpiVencidas.mostrar(String.valueOf(d.cuotasVencidas()), d.cuotasVencidas() == 0 ? "Todo al día"
                : Formularios.soles(d.saldoVencido()) + " de " + d.pacientesMorosos() + " paciente(s)",
                d.cuotasVencidas() > 0);
        kpiVencidas.setAlHacerClic(panelMorosos.isVisible() && d.cuotasVencidas() > 0 ? this::onMorosidad : null);

        lblGrafico.setText((d.esProduccion() ? "Mi producción" : "Ingresos") + " de los últimos 6 meses (S/)");
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        for (MontoPeriodoDto m : d.ultimosMeses()) {
            serie.getData().add(new XYChart.Data<>(m.etiqueta(), m.monto()));
        }
        grafico.getData().setAll(serie);
        for (int i = 0; i < serie.getData().size(); i++) {
            XYChart.Data<String, Number> dato = serie.getData().get(i);
            if (dato.getNode() != null) {
                Tooltip.install(dato.getNode(), new Tooltip(dato.getXValue() + ": "
                        + Formularios.soles(d.ultimosMeses().get(i).monto())));
            }
        }

        lblCitas.setText("Citas de hoy (" + d.agendaHoy().size() + ")");
        lstCitas.getItems().setAll(d.agendaHoy());
        tblMorosos.getItems().setAll(d.morosos());
    }

    // ------------------------------------------------------------------ listas

    private void configurarCitas() {
        lstCitas.setPlaceholder(new Label("No hay citas para hoy."));
        lstCitas.setCellFactory(l -> new ListCell<>() {
            @Override
            protected void updateItem(CitaDto c, boolean vacio) {
                super.updateItem(c, vacio);
                if (vacio || c == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Region color = new Region();
                color.setMinWidth(4);
                color.setPrefWidth(4);
                color.setStyle("-fx-background-color: " + c.colorAgenda() + "; -fx-background-radius: 2;");
                Label hora = new Label(Formularios.hora(c.inicio().toLocalTime()));
                hora.getStyleClass().add(Styles.TEXT_BOLD);
                hora.setMinWidth(44);
                Label paciente = new Label(c.pacienteNombre());
                Label detalle = new Label(c.motivo().replaceAll("\\s+", " ") + " · " + c.odontologoNombre());
                detalle.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);
                VBox textos = new VBox(1, paciente, detalle);
                HBox.setHgrow(textos, Priority.ALWAYS);
                textos.setMinWidth(0);
                Label estado = new Label(c.estado().etiqueta());
                estado.getStyleClass().addAll(Styles.TEXT_SMALL, switch (c.estado()) {
                    case ATENDIDA -> Styles.SUCCESS;
                    case NO_ASISTIO -> Styles.DANGER;
                    case CONFIRMADA -> Styles.ACCENT;
                    default -> Styles.TEXT_MUTED;
                });
                HBox fila = new HBox(10, color, hora, textos, estado);
                fila.setAlignment(Pos.CENTER_LEFT);
                setText(null);
                setGraphic(fila);
            }
        });
        lstCitas.setOnMouseClicked(e -> {
            CitaDto c = lstCitas.getSelectionModel().getSelectedItem();
            if (e.getClickCount() == 2 && c != null) {
                eventos.publishEvent(new NavegarA(MainController.VISTA_FICHA, Map.of("pacienteId", c.pacienteId())));
            }
        });
    }

    private void configurarMorosos() {
        TableColumn<MorosoDto, String> atraso = Formularios.columna("Atraso", m -> m.diasAtraso() + " días", 80);
        atraso.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String texto, boolean vacio) {
                super.updateItem(texto, vacio);
                getStyleClass().removeAll(Formularios.ESTADO_ALERTA, Formularios.ESTADO_PELIGRO);
                setText(vacio ? null : texto);
                MorosoDto m = vacio || getTableRow() == null ? null : getTableRow().getItem();
                if (m != null) {
                    getStyleClass().add(m.diasAtraso() > DIAS_ATRASO_GRAVE ? Formularios.ESTADO_PELIGRO
                            : Formularios.ESTADO_ALERTA);
                }
            }
        });
        Formularios.columnas(tblMorosos,
                Formularios.columna("Historia", MorosoDto::numeroHistoria, 95),
                Formularios.columna("Paciente", MorosoDto::paciente, 240),
                Formularios.columna("Teléfono", MorosoDto::telefono, 105),
                Formularios.columna("Cuotas", MorosoDto::cuotasVencidas, 70),
                Formularios.columna("Desde", m -> Formularios.fecha(m.vencimientoMasAntiguo()), 95),
                atraso,
                Formularios.columna("Saldo vencido", m -> Formularios.soles(m.saldoVencido()), 110));
        tblMorosos.setPlaceholder(new Label("Ningún paciente tiene cuotas vencidas."));
        tblMorosos.setRowFactory(t -> {
            TableRow<MorosoDto> fila = new TableRow<>();
            fila.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !fila.isEmpty()) {
                    eventos.publishEvent(new NavegarA(MainController.VISTA_FICHA,
                            Map.of("pacienteId", fila.getItem().pacienteId(), "pestana", "pagos")));
                }
            });
            return fila;
        });
    }

    @FXML
    private void onActualizar() {
        refrescar();
    }

    @FXML
    private void onAgenda() {
        eventos.publishEvent(new NavegarA(MainController.VISTA_AGENDA));
    }

    @FXML
    private void onMorosidad() {
        eventos.publishEvent(new NavegarA(MainController.VISTA_REPORTES, Map.of("pestana", TipoReporte.MOROSIDAD.name())));
    }

    private static String primeraMayuscula(String texto) {
        return texto.isEmpty() ? texto : Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }
}
