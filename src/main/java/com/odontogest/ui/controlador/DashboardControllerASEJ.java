package com.odontogest.ui.controlador;

import atlantafx.base.theme.Styles;
import com.odontogest.evento.CitaCambiadaASEJ;
import com.odontogest.evento.NavegarAASEJ;
import com.odontogest.evento.PagoRegistradoASEJ;
import com.odontogest.modelo.EstadoCitaASEJ;
import com.odontogest.seguridad.PermisoASEJ;
import com.odontogest.seguridad.SesionUsuarioASEJ;
import com.odontogest.servicio.DashboardServiceASEJ;
import com.odontogest.servicio.ReporteServiceASEJ;
import com.odontogest.servicio.dto.CitaDtoASEJ;
import com.odontogest.servicio.dto.DashboardDtoASEJ;
import com.odontogest.servicio.dto.MontoPeriodoDtoASEJ;
import com.odontogest.servicio.dto.MorosoDtoASEJ;
import com.odontogest.servicio.dto.TipoReporteASEJ;
import com.odontogest.ui.componente.TarjetaKpiASEJ;
import com.odontogest.ui.soporte.FormulariosASEJ;
import com.odontogest.ui.soporte.GraficosASEJ;
import com.odontogest.ui.soporte.HiloUiASEJ;
import com.odontogest.ui.soporte.RefrescableASEJ;
import com.odontogest.ui.soporte.TareaUiASEJ;
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

@Component
@RequiredArgsConstructor
public class DashboardControllerASEJ implements RefrescableASEJ {

    static final long DIAS_ATRASO_GRAVEASEJ = 30;
    private static final DateTimeFormatter FECHA_LARGAASEJ =
            DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", Locale.of("es", "PE"));

    private final DashboardServiceASEJ dashboardServiceASEJ;
    private final ReporteServiceASEJ reporteServiceASEJ;
    private final SesionUsuarioASEJ sesionUsuarioASEJ;
    private final TareaUiASEJ tareaUiASEJ;
    private final ApplicationEventPublisher eventosASEJ;

    @FXML private Label lblSaludoASEJ;
    @FXML private Label lblFechaASEJ;
    @FXML private Button btnActualizarASEJ;
    @FXML private TarjetaKpiASEJ kpiCitasASEJ;
    @FXML private TarjetaKpiASEJ kpiMontoASEJ;
    @FXML private TarjetaKpiASEJ kpiPacientesASEJ;
    @FXML private TarjetaKpiASEJ kpiVencidasASEJ;
    @FXML private Label lblGraficoASEJ;
    @FXML private BarChart<String, Number> graficoASEJ;
    @FXML private NumberAxis ejeMontoASEJ;
    @FXML private Label lblCitasASEJ;
    @FXML private Button btnAgendaASEJ;
    @FXML private ListView<CitaDtoASEJ> lstCitasASEJ;
    @FXML private Button btnMorosidadASEJ;
    @FXML private VBox panelMorososASEJ;
    @FXML private TableView<MorosoDtoASEJ> tblMorososASEJ;

    @FXML
    public void initialize() {
        lblSaludoASEJ.setText("Bienvenido(a), " + sesionUsuarioASEJ.exigirUsuarioASEJ().nombreCompletoASEJ());
        boolean agendaASEJ = sesionUsuarioASEJ.tieneASEJ(PermisoASEJ.AGENDA_GESTIONAR);
        btnAgendaASEJ.setVisible(agendaASEJ);
        kpiCitasASEJ.setAlHacerClicASEJ(agendaASEJ ? this::onAgendaASEJ : null);
        boolean morosidadASEJ = sesionUsuarioASEJ.tieneASEJ(PermisoASEJ.REPORTES_VER)
                && reporteServiceASEJ.disponiblesASEJ().contains(TipoReporteASEJ.MOROSIDAD);
        panelMorososASEJ.setVisible(morosidadASEJ);
        panelMorososASEJ.setManaged(morosidadASEJ);
        ejeMontoASEJ.setTickLabelFormatter(new NumberAxis.DefaultFormatter(ejeMontoASEJ, "S/ ", null));
        GraficosASEJ.limitarAnchoBarrasASEJ(graficoASEJ);
        configurarCitasASEJ();
        configurarMorososASEJ();
    }

    @Override
    public void refrescarASEJ() {
        lblFechaASEJ.setText(primeraMayusculaASEJ(FECHA_LARGAASEJ.format(LocalDate.now())));
        tareaUiASEJ.ejecutarASEJ(btnActualizarASEJ, dashboardServiceASEJ::cargarASEJ, this::mostrarASEJ);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void alCambiarCitaASEJ(CitaCambiadaASEJ eASEJ) {
        refrescarSiVisibleASEJ();
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void alRegistrarPagoASEJ(PagoRegistradoASEJ eASEJ) {
        refrescarSiVisibleASEJ();
    }

    private void refrescarSiVisibleASEJ() {
        HiloUiASEJ.ejecutarASEJ(() -> {
            if (lstCitasASEJ != null && lstCitasASEJ.getScene() != null) {
                refrescarASEJ();
            }
        });
    }

    private void mostrarASEJ(DashboardDtoASEJ dASEJ) {
        long atendidasASEJ = dASEJ.agendaHoyASEJ().stream().filter(cASEJ -> cASEJ.estadoASEJ() == EstadoCitaASEJ.ATENDIDA).count();
        kpiCitasASEJ.mostrarASEJ(String.valueOf(dASEJ.citasHoyASEJ()), dASEJ.citasHoyASEJ() == 0 ? "Sin citas programadas"
                : atendidasASEJ + " atendida(s), " + (dASEJ.citasHoyASEJ() - atendidasASEJ) + " por atender", false);
        kpiMontoASEJ.setTituloASEJ(dASEJ.esProduccionASEJ() ? "Mi producción del mes" : "Ingresos del mes");
        kpiMontoASEJ.mostrarASEJ(FormulariosASEJ.solesASEJ(dASEJ.montoMesASEJ()), dASEJ.esProduccionASEJ()
                ? "Procedimientos realizados" : "Pagos registrados en caja, sin anulados", false);
        kpiPacientesASEJ.mostrarASEJ(String.valueOf(dASEJ.pacientesNuevosMesASEJ()), "Historias clínicas abiertas este mes", false);
        kpiVencidasASEJ.mostrarASEJ(String.valueOf(dASEJ.cuotasVencidasASEJ()), dASEJ.cuotasVencidasASEJ() == 0 ? "Todo al día"
                : FormulariosASEJ.solesASEJ(dASEJ.saldoVencidoASEJ()) + " de " + dASEJ.pacientesMorososASEJ() + " paciente(s)",
                dASEJ.cuotasVencidasASEJ() > 0);
        kpiVencidasASEJ.setAlHacerClicASEJ(panelMorososASEJ.isVisible() && dASEJ.cuotasVencidasASEJ() > 0 ? this::onMorosidadASEJ : null);

        lblGraficoASEJ.setText((dASEJ.esProduccionASEJ() ? "Mi producción" : "Ingresos") + " de los últimos 6 meses (S/)");
        XYChart.Series<String, Number> serieASEJ = new XYChart.Series<>();
        for (MontoPeriodoDtoASEJ mASEJ : dASEJ.ultimosMesesASEJ()) {
            serieASEJ.getData().add(new XYChart.Data<>(mASEJ.etiquetaASEJ(), mASEJ.montoASEJ()));
        }
        graficoASEJ.getData().setAll(serieASEJ);
        for (int iASEJ = 0; iASEJ < serieASEJ.getData().size(); iASEJ++) {
            XYChart.Data<String, Number> datoASEJ = serieASEJ.getData().get(iASEJ);
            if (datoASEJ.getNode() != null) {
                Tooltip.install(datoASEJ.getNode(), new Tooltip(datoASEJ.getXValue() + ": "
                        + FormulariosASEJ.solesASEJ(dASEJ.ultimosMesesASEJ().get(iASEJ).montoASEJ())));
            }
        }

        lblCitasASEJ.setText("Citas de hoy (" + dASEJ.agendaHoyASEJ().size() + ")");
        lstCitasASEJ.getItems().setAll(dASEJ.agendaHoyASEJ());
        tblMorososASEJ.getItems().setAll(dASEJ.morososASEJ());
    }

    private void configurarCitasASEJ() {
        lstCitasASEJ.setPlaceholder(new Label("No hay citas para hoy."));
        lstCitasASEJ.setCellFactory(lASEJ -> new ListCell<>() {
            @Override
            protected void updateItem(CitaDtoASEJ cASEJ, boolean vacioASEJ) {
                super.updateItem(cASEJ, vacioASEJ);
                if (vacioASEJ || cASEJ == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Region colorASEJ = new Region();
                colorASEJ.setMinWidth(4);
                colorASEJ.setPrefWidth(4);
                colorASEJ.setStyle("-fx-background-color: " + cASEJ.colorAgendaASEJ() + "; -fx-background-radius: 2;");
                Label horaASEJ = new Label(FormulariosASEJ.horaASEJ(cASEJ.inicioASEJ().toLocalTime()));
                horaASEJ.getStyleClass().add(Styles.TEXT_BOLD);
                horaASEJ.setMinWidth(44);
                Label pacienteASEJ = new Label(cASEJ.pacienteNombreASEJ());
                Label detalleASEJ = new Label(cASEJ.motivoASEJ().replaceAll("\\s+", " ") + " · " + cASEJ.odontologoNombreASEJ());
                detalleASEJ.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);
                VBox textosASEJ = new VBox(1, pacienteASEJ, detalleASEJ);
                HBox.setHgrow(textosASEJ, Priority.ALWAYS);
                textosASEJ.setMinWidth(0);
                Label estadoASEJ = new Label(cASEJ.estadoASEJ().etiquetaASEJ());
                estadoASEJ.getStyleClass().addAll(Styles.TEXT_SMALL, switch (cASEJ.estadoASEJ()) {
                    case ATENDIDA -> Styles.SUCCESS;
                    case NO_ASISTIO -> Styles.DANGER;
                    case CONFIRMADA -> Styles.ACCENT;
                    default -> Styles.TEXT_MUTED;
                });
                HBox filaASEJ = new HBox(10, colorASEJ, horaASEJ, textosASEJ, estadoASEJ);
                filaASEJ.setAlignment(Pos.CENTER_LEFT);
                setText(null);
                setGraphic(filaASEJ);
            }
        });
        lstCitasASEJ.setOnMouseClicked(eASEJ -> {
            CitaDtoASEJ cASEJ = lstCitasASEJ.getSelectionModel().getSelectedItem();
            if (eASEJ.getClickCount() == 2 && cASEJ != null) {
                eventosASEJ.publishEvent(new NavegarAASEJ(MainControllerASEJ.VISTA_FICHAASEJ, Map.of("pacienteId", cASEJ.pacienteIdASEJ())));
            }
        });
    }

    private void configurarMorososASEJ() {
        TableColumn<MorosoDtoASEJ, String> atrasoASEJ = FormulariosASEJ.columnaASEJ("Atraso", mASEJ -> mASEJ.diasAtrasoASEJ() + " días", 80);
        atrasoASEJ.setCellFactory(cASEJ -> new TableCell<>() {
            @Override
            protected void updateItem(String textoASEJ, boolean vacioASEJ) {
                super.updateItem(textoASEJ, vacioASEJ);
                getStyleClass().removeAll(FormulariosASEJ.ESTADO_ALERTAASEJ, FormulariosASEJ.ESTADO_PELIGROASEJ);
                setText(vacioASEJ ? null : textoASEJ);
                MorosoDtoASEJ mASEJ = vacioASEJ || getTableRow() == null ? null : getTableRow().getItem();
                if (mASEJ != null) {
                    getStyleClass().add(mASEJ.diasAtrasoASEJ() > DIAS_ATRASO_GRAVEASEJ ? FormulariosASEJ.ESTADO_PELIGROASEJ
                            : FormulariosASEJ.ESTADO_ALERTAASEJ);
                }
            }
        });
        FormulariosASEJ.columnasASEJ(tblMorososASEJ,
                FormulariosASEJ.columnaASEJ("Historia", MorosoDtoASEJ::numeroHistoriaASEJ, 95),
                FormulariosASEJ.columnaASEJ("Paciente", MorosoDtoASEJ::pacienteASEJ, 240),
                FormulariosASEJ.columnaASEJ("Teléfono", MorosoDtoASEJ::telefonoASEJ, 105),
                FormulariosASEJ.columnaASEJ("Cuotas", MorosoDtoASEJ::cuotasVencidasASEJ, 70),
                FormulariosASEJ.columnaASEJ("Desde", mASEJ -> FormulariosASEJ.fechaASEJ(mASEJ.vencimientoMasAntiguoASEJ()), 95),
                atrasoASEJ,
                FormulariosASEJ.columnaASEJ("Saldo vencido", mASEJ -> FormulariosASEJ.solesASEJ(mASEJ.saldoVencidoASEJ()), 110));
        tblMorososASEJ.setPlaceholder(new Label("Ningún paciente tiene cuotas vencidas."));
        tblMorososASEJ.setRowFactory(tASEJ -> {
            TableRow<MorosoDtoASEJ> filaASEJ = new TableRow<>();
            filaASEJ.setOnMouseClicked(eASEJ -> {
                if (eASEJ.getClickCount() == 2 && !filaASEJ.isEmpty()) {
                    eventosASEJ.publishEvent(new NavegarAASEJ(MainControllerASEJ.VISTA_FICHAASEJ,
                            Map.of("pacienteId", filaASEJ.getItem().pacienteIdASEJ(), "pestana", "pagos")));
                }
            });
            return filaASEJ;
        });
    }

    @FXML
    private void onActualizarASEJ() {
        refrescarASEJ();
    }

    @FXML
    private void onAgendaASEJ() {
        eventosASEJ.publishEvent(new NavegarAASEJ(MainControllerASEJ.VISTA_AGENDAASEJ));
    }

    @FXML
    private void onMorosidadASEJ() {
        eventosASEJ.publishEvent(new NavegarAASEJ(MainControllerASEJ.VISTA_REPORTESASEJ, Map.of("pestana", TipoReporteASEJ.MOROSIDAD.name())));
    }

    private static String primeraMayusculaASEJ(String textoASEJ) {
        return textoASEJ.isEmpty() ? textoASEJ : Character.toUpperCase(textoASEJ.charAt(0)) + textoASEJ.substring(1);
    }
}
