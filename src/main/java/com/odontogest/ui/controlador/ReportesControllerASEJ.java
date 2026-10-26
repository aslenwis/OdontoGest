package com.odontogest.ui.controlador;

import atlantafx.base.theme.Styles;
import com.odontogest.evento.NavegarAASEJ;
import com.odontogest.modelo.MedioPagoASEJ;
import com.odontogest.servicio.AuditoriaServiceASEJ;
import com.odontogest.servicio.ReporteServiceASEJ;
import com.odontogest.servicio.dto.AuditoriaDtoASEJ;
import com.odontogest.servicio.dto.GrupoIndicesDtoASEJ;
import com.odontogest.servicio.dto.IngresoDiaDtoASEJ;
import com.odontogest.servicio.dto.MontoPeriodoDtoASEJ;
import com.odontogest.servicio.dto.MorosoDtoASEJ;
import com.odontogest.servicio.dto.PagoReporteDtoASEJ;
import com.odontogest.servicio.dto.ProduccionFilaDtoASEJ;
import com.odontogest.servicio.dto.ReporteEpidemiologicoDtoASEJ;
import com.odontogest.servicio.dto.ReporteIngresosDtoASEJ;
import com.odontogest.servicio.dto.ReporteMorosidadDtoASEJ;
import com.odontogest.servicio.dto.ReporteProduccionDtoASEJ;
import com.odontogest.servicio.dto.TipoReporteASEJ;
import com.odontogest.ui.soporte.ExportadorASEJ;
import com.odontogest.ui.soporte.FormulariosASEJ;
import com.odontogest.ui.soporte.GraficosASEJ;
import com.odontogest.ui.soporte.RefrescableASEJ;
import com.odontogest.ui.soporte.TareaUiASEJ;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class ReportesControllerASEJ implements RefrescableASEJ {

    private static final String TODASASEJ = "(todas las acciones)";

    private final ReporteServiceASEJ reporteServiceASEJ;
    private final AuditoriaServiceASEJ auditoriaServiceASEJ;
    private final TareaUiASEJ tareaUiASEJ;
    private final ExportadorASEJ exportadorASEJ;
    private final ApplicationEventPublisher eventosASEJ;

    @FXML private TabPane pestanasASEJ;
    @FXML private Tab tabIngresosASEJ;
    @FXML private Tab tabProduccionASEJ;
    @FXML private Tab tabMorosidadASEJ;
    @FXML private Tab tabEpidemiologicoASEJ;
    @FXML private Tab tabAuditoriaASEJ;

    private final List<Runnable> generadoresASEJ = new ArrayList<>();

    @FXML
    public void initialize() {
        generadoresASEJ.clear();
        Set<TipoReporteASEJ> disponiblesASEJ = reporteServiceASEJ.disponiblesASEJ();
        Map<TipoReporteASEJ, Tab> tabsASEJ = Map.of(TipoReporteASEJ.INGRESOS, tabIngresosASEJ, TipoReporteASEJ.PRODUCCION, tabProduccionASEJ,
                TipoReporteASEJ.MOROSIDAD, tabMorosidadASEJ, TipoReporteASEJ.EPIDEMIOLOGICO, tabEpidemiologicoASEJ,
                TipoReporteASEJ.AUDITORIA, tabAuditoriaASEJ);
        tabsASEJ.forEach((tipoASEJ, tabASEJ) -> {
            if (!disponiblesASEJ.contains(tipoASEJ)) {
                pestanasASEJ.getTabs().remove(tabASEJ);
            }
        });
        if (disponiblesASEJ.contains(TipoReporteASEJ.INGRESOS)) {
            tabIngresosASEJ.setContent(ingresosASEJ());
        }
        if (disponiblesASEJ.contains(TipoReporteASEJ.PRODUCCION)) {
            tabProduccionASEJ.setContent(produccionASEJ());
        }
        if (disponiblesASEJ.contains(TipoReporteASEJ.MOROSIDAD)) {
            tabMorosidadASEJ.setContent(morosidadASEJ());
        }
        if (disponiblesASEJ.contains(TipoReporteASEJ.EPIDEMIOLOGICO)) {
            tabEpidemiologicoASEJ.setContent(epidemiologicoASEJ());
        }
        if (disponiblesASEJ.contains(TipoReporteASEJ.AUDITORIA)) {
            tabAuditoriaASEJ.setContent(auditoriaASEJ());
        }
    }

    @Override
    public void refrescarASEJ() {
        generadoresASEJ.forEach(Runnable::run);
    }

    public void seleccionarPestanaASEJ(String tipoASEJ) {
        pestanasASEJ.getTabs().stream().filter(tASEJ -> tASEJ.getId().equals(tipoASEJ)).findFirst()
                .ifPresent(tASEJ -> pestanasASEJ.getSelectionModel().select(tASEJ));
    }

    private Node ingresosASEJ() {
        RangoASEJ rangoASEJ = new RangoASEJ();
        FlowPane resumenASEJ = resumenASEJ();
        BarChart<String, Number> graficoASEJ = graficoASEJ("grafico-montos");
        TableView<IngresoDiaDtoASEJ> diasASEJ = new TableView<>();
        List<TableColumn<IngresoDiaDtoASEJ, ?>> columnasASEJ = new ArrayList<>();
        columnasASEJ.add(FormulariosASEJ.columnaASEJ("Fecha", dASEJ -> FormulariosASEJ.fechaASEJ(dASEJ.diaASEJ()), 95));
        columnasASEJ.add(FormulariosASEJ.columnaASEJ("Pagos", IngresoDiaDtoASEJ::pagosASEJ, 60));
        for (MedioPagoASEJ mASEJ : MedioPagoASEJ.values()) {
            columnasASEJ.add(FormulariosASEJ.columnaASEJ(mASEJ.etiquetaASEJ(), dASEJ -> FormulariosASEJ.solesASEJ(dASEJ.montoASEJ(mASEJ)), 100));
        }
        columnasASEJ.add(FormulariosASEJ.columnaASEJ("Total", dASEJ -> FormulariosASEJ.solesASEJ(dASEJ.totalASEJ()), 110));
        diasASEJ.getColumns().setAll(columnasASEJ);
        diasASEJ.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        diasASEJ.setPlaceholder(new Label("No hay pagos en el rango."));
        TableView<PagoReporteDtoASEJ> detalleASEJ = new TableView<>();
        FormulariosASEJ.columnasASEJ(detalleASEJ,
                FormulariosASEJ.columnaASEJ("Fecha y hora", pASEJ -> FormulariosASEJ.fechaHoraASEJ(pASEJ.fechaASEJ()), 130),
                FormulariosASEJ.columnaASEJ("Recibo", PagoReporteDtoASEJ::numeroReciboASEJ, 105),
                FormulariosASEJ.columnaASEJ("Historia", PagoReporteDtoASEJ::numeroHistoriaASEJ, 95),
                FormulariosASEJ.columnaASEJ("Paciente", PagoReporteDtoASEJ::pacienteASEJ, 220),
                FormulariosASEJ.columnaASEJ("Medio", pASEJ -> pASEJ.medioASEJ().etiquetaASEJ(), 100),
                FormulariosASEJ.columnaASEJ("Referencia", PagoReporteDtoASEJ::referenciaASEJ, 120),
                FormulariosASEJ.columnaASEJ("Monto", pASEJ -> FormulariosASEJ.solesASEJ(pASEJ.montoASEJ()), 100));
        detalleASEJ.setPlaceholder(new Label("No hay pagos en el rango."));
        TabPane tablasASEJ = new TabPane(new Tab("Por día", diasASEJ), new Tab("Detalle de pagos", detalleASEJ));
        tablasASEJ.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tablasASEJ.getStyleClass().add(Styles.TABS_FLOATING);
        VBox.setVgrow(tablasASEJ, Priority.ALWAYS);

        Button generarASEJ = botonGenerarASEJ();
        Button excelASEJ = botonExcelASEJ();
        Runnable cargarASEJ = () -> conPeriodoASEJ(rangoASEJ, pASEJ -> tareaUiASEJ.ejecutarASEJ(generarASEJ, () -> reporteServiceASEJ.ingresosASEJ(pASEJ.desdeASEJ(), pASEJ.hastaASEJ()),
                (ReporteIngresosDtoASEJ rASEJ) -> {
                    List<Node> tarjetasASEJ = new ArrayList<>();
                    tarjetasASEJ.add(tarjetaASEJ("Total", FormulariosASEJ.solesASEJ(rASEJ.totalASEJ())));
                    tarjetasASEJ.add(tarjetaASEJ("Pagos", String.valueOf(rASEJ.pagosASEJ())));
                    for (MedioPagoASEJ mASEJ : MedioPagoASEJ.values()) {
                        tarjetasASEJ.add(tarjetaASEJ(mASEJ.etiquetaASEJ(), FormulariosASEJ.solesASEJ(rASEJ.totalesPorMedioASEJ().get(mASEJ))));
                    }
                    resumenASEJ.getChildren().setAll(tarjetasASEJ);
                    mostrarSerieASEJ(graficoASEJ, "Ingresos", rASEJ.serieASEJ());
                    diasASEJ.getItems().setAll(rASEJ.diasASEJ());
                    detalleASEJ.getItems().setAll(rASEJ.detalleASEJ());
                }));
        generarASEJ.setOnAction(eASEJ -> cargarASEJ.run());
        excelASEJ.setOnAction(eASEJ -> conPeriodoASEJ(rangoASEJ, pASEJ -> exportadorASEJ.guardarExcelASEJ(excelASEJ, "ingresos-" + rangoASEJ.sufijoASEJ() + ".xlsx",
                () -> reporteServiceASEJ.excelIngresosASEJ(pASEJ.desdeASEJ(), pASEJ.hastaASEJ()))));
        generadoresASEJ.add(cargarASEJ);
        return panelASEJ(barraASEJ(rangoASEJ, generarASEJ, excelASEJ), resumenASEJ, graficoASEJ, tablasASEJ);
    }

    private Node produccionASEJ() {
        RangoASEJ rangoASEJ = new RangoASEJ();
        FlowPane resumenASEJ = resumenASEJ();
        BarChart<String, Number> graficoASEJ = graficoASEJ("grafico-montos");
        TableView<ProduccionFilaDtoASEJ> tablaASEJ = new TableView<>();
        FormulariosASEJ.columnasASEJ(tablaASEJ,
                FormulariosASEJ.columnaASEJ("Odontólogo", ProduccionFilaDtoASEJ::odontologoASEJ, 200),
                FormulariosASEJ.columnaASEJ("Código", ProduccionFilaDtoASEJ::codigoASEJ, 90),
                FormulariosASEJ.columnaASEJ("Procedimiento", ProduccionFilaDtoASEJ::procedimientoASEJ, 260),
                FormulariosASEJ.columnaASEJ("Cantidad", ProduccionFilaDtoASEJ::cantidadASEJ, 80),
                FormulariosASEJ.columnaASEJ("Monto", fASEJ -> FormulariosASEJ.solesASEJ(fASEJ.montoASEJ()), 110));
        tablaASEJ.setPlaceholder(new Label("No hay procedimientos realizados en el rango."));
        VBox.setVgrow(tablaASEJ, Priority.ALWAYS);

        Button generarASEJ = botonGenerarASEJ();
        Button excelASEJ = botonExcelASEJ();
        Runnable cargarASEJ = () -> conPeriodoASEJ(rangoASEJ, pASEJ -> tareaUiASEJ.ejecutarASEJ(generarASEJ, () -> reporteServiceASEJ.produccionASEJ(pASEJ.desdeASEJ(), pASEJ.hastaASEJ()),
                (ReporteProduccionDtoASEJ rASEJ) -> {
                    resumenASEJ.getChildren().setAll(tarjetaASEJ("Total producido", FormulariosASEJ.solesASEJ(rASEJ.totalASEJ())),
                            tarjetaASEJ("Procedimientos", String.valueOf(rASEJ.procedimientosASEJ())),
                            tarjetaASEJ("Odontólogos", String.valueOf(rASEJ.porOdontologoASEJ().size())));
                    mostrarSerieASEJ(graficoASEJ, "Producción", rASEJ.porOdontologoASEJ());
                    tablaASEJ.getItems().setAll(rASEJ.filasASEJ());
                }));
        generarASEJ.setOnAction(eASEJ -> cargarASEJ.run());
        excelASEJ.setOnAction(eASEJ -> conPeriodoASEJ(rangoASEJ, pASEJ -> exportadorASEJ.guardarExcelASEJ(excelASEJ, "produccion-" + rangoASEJ.sufijoASEJ() + ".xlsx",
                () -> reporteServiceASEJ.excelProduccionASEJ(pASEJ.desdeASEJ(), pASEJ.hastaASEJ()))));
        generadoresASEJ.add(cargarASEJ);
        return panelASEJ(barraASEJ(rangoASEJ, generarASEJ, excelASEJ), resumenASEJ, graficoASEJ, tablaASEJ);
    }

    private Node morosidadASEJ() {
        FlowPane resumenASEJ = resumenASEJ();
        TableView<MorosoDtoASEJ> tablaASEJ = new TableView<>();
        FormulariosASEJ.columnasASEJ(tablaASEJ,
                FormulariosASEJ.columnaASEJ("Historia", MorosoDtoASEJ::numeroHistoriaASEJ, 95),
                FormulariosASEJ.columnaASEJ("Paciente", MorosoDtoASEJ::pacienteASEJ, 240),
                FormulariosASEJ.columnaASEJ("Teléfono", MorosoDtoASEJ::telefonoASEJ, 105),
                FormulariosASEJ.columnaASEJ("Cuotas vencidas", MorosoDtoASEJ::cuotasVencidasASEJ, 110),
                FormulariosASEJ.columnaASEJ("Vence desde", mASEJ -> FormulariosASEJ.fechaASEJ(mASEJ.vencimientoMasAntiguoASEJ()), 100),
                FormulariosASEJ.columnaASEJ("Días de atraso", MorosoDtoASEJ::diasAtrasoASEJ, 100),
                FormulariosASEJ.columnaASEJ("Saldo vencido", mASEJ -> FormulariosASEJ.solesASEJ(mASEJ.saldoVencidoASEJ()), 120));
        tablaASEJ.setPlaceholder(new Label("Ningún paciente tiene cuotas vencidas."));
        tablaASEJ.setRowFactory(tASEJ -> {
            TableRow<MorosoDtoASEJ> filaASEJ = new TableRow<>();
            filaASEJ.setOnMouseClicked(eASEJ -> {
                if (eASEJ.getClickCount() == 2 && !filaASEJ.isEmpty()) {
                    eventosASEJ.publishEvent(new NavegarAASEJ(MainControllerASEJ.VISTA_FICHAASEJ,
                            Map.of("pacienteId", filaASEJ.getItem().pacienteIdASEJ(), "pestana", "pagos")));
                }
            });
            return filaASEJ;
        });
        VBox.setVgrow(tablaASEJ, Priority.ALWAYS);

        Label fechaASEJ = new Label();
        fechaASEJ.getStyleClass().add(Styles.TEXT_MUTED);
        Button generarASEJ = FormulariosASEJ.botonASEJ("Actualizar", Feather.REFRESH_CW);
        Button excelASEJ = botonExcelASEJ();
        Runnable cargarASEJ = () -> tareaUiASEJ.ejecutarASEJ(generarASEJ, reporteServiceASEJ::morosidadASEJ, (ReporteMorosidadDtoASEJ rASEJ) -> {
            fechaASEJ.setText("Cuotas vencidas al " + FormulariosASEJ.fechaASEJ(rASEJ.fechaASEJ())
                    + " (doble clic en un paciente para abrir sus pagos)");
            resumenASEJ.getChildren().setAll(tarjetaASEJ("Deuda vencida", FormulariosASEJ.solesASEJ(rASEJ.totalASEJ())),
                    tarjetaASEJ("Pacientes", String.valueOf(rASEJ.pacientesASEJ().size())),
                    tarjetaASEJ("Cuotas", String.valueOf(rASEJ.cuotasASEJ())));
            tablaASEJ.getItems().setAll(rASEJ.pacientesASEJ());
        });
        generarASEJ.setOnAction(eASEJ -> cargarASEJ.run());
        excelASEJ.setOnAction(eASEJ -> exportadorASEJ.guardarExcelASEJ(excelASEJ, "morosidad-" + LocalDate.now() + ".xlsx",
                reporteServiceASEJ::excelMorosidadASEJ));
        generadoresASEJ.add(cargarASEJ);
        HBox barraASEJ = new HBox(8, fechaASEJ, espacioASEJ(), generarASEJ, excelASEJ);
        barraASEJ.setAlignment(Pos.CENTER_LEFT);
        return panelASEJ(barraASEJ, resumenASEJ, tablaASEJ);
    }

    private Node epidemiologicoASEJ() {
        RangoASEJ rangoASEJ = new RangoASEJ();
        RangoASEJ.fijarASEJ(rangoASEJ.desdeSelectorASEJ, LocalDate.now().withDayOfYear(1));
        FlowPane resumenASEJ = resumenASEJ();
        BarChart<String, Number> graficoASEJ = graficoASEJ("grafico-indices");
        HBox leyendaASEJ = new HBox(16, itemLeyendaASEJ("CPO-D (dentición permanente)", "leyenda-cpod"),
                itemLeyendaASEJ("ceo-d (dentición decidua)", "leyenda-ceod"));
        leyendaASEJ.setAlignment(Pos.CENTER_LEFT);
        TableView<GrupoIndicesDtoASEJ> tablaASEJ = new TableView<>();
        Function<Double, String> numASEJ = vASEJ -> vASEJ == null ? "—" : String.format(Locale.ROOT, "%.2f", vASEJ);
        FormulariosASEJ.columnasASEJ(tablaASEJ,
                FormulariosASEJ.columnaASEJ("Grupo de edad", GrupoIndicesDtoASEJ::grupoASEJ, 130),
                FormulariosASEJ.columnaASEJ("Pacientes", GrupoIndicesDtoASEJ::pacientesASEJ, 80),
                FormulariosASEJ.columnaASEJ("CPO-D", gASEJ -> numASEJ.apply(gASEJ.cpodASEJ()), 70),
                FormulariosASEJ.columnaASEJ("C", gASEJ -> numASEJ.apply(gASEJ.cASEJ()), 60),
                FormulariosASEJ.columnaASEJ("P", gASEJ -> numASEJ.apply(gASEJ.pASEJ()), 60),
                FormulariosASEJ.columnaASEJ("O", gASEJ -> numASEJ.apply(gASEJ.oASEJ()), 60),
                FormulariosASEJ.columnaASEJ("ceo-d", gASEJ -> numASEJ.apply(gASEJ.ceodASEJ()), 70),
                FormulariosASEJ.columnaASEJ("Nivel OMS (CPO-D)", gASEJ -> gASEJ.nivelCpodASEJ() == null ? "—" : gASEJ.nivelCpodASEJ(), 140));
        tablaASEJ.setPlaceholder(new Label("Sin pacientes examinados en el rango."));
        VBox.setVgrow(tablaASEJ, Priority.ALWAYS);
        Label notaASEJ = new Label();
        notaASEJ.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);
        notaASEJ.setWrapText(true);

        Button generarASEJ = botonGenerarASEJ();
        Button excelASEJ = botonExcelASEJ();
        Runnable cargarASEJ = () -> conPeriodoASEJ(rangoASEJ, pASEJ -> tareaUiASEJ.ejecutarASEJ(generarASEJ,
                () -> reporteServiceASEJ.epidemiologicoASEJ(pASEJ.desdeASEJ(), pASEJ.hastaASEJ()), (ReporteEpidemiologicoDtoASEJ rASEJ) -> {
                    resumenASEJ.getChildren().setAll(tarjetaASEJ("Pacientes examinados", String.valueOf(rASEJ.pacientesASEJ())));
                    XYChart.Series<String, Number> cpodASEJ = new XYChart.Series<>();
                    cpodASEJ.setName("CPO-D");
                    XYChart.Series<String, Number> ceodASEJ = new XYChart.Series<>();
                    ceodASEJ.setName("ceo-d");
                    for (GrupoIndicesDtoASEJ gASEJ : rASEJ.gruposASEJ()) {
                        cpodASEJ.getData().add(new XYChart.Data<>(gASEJ.grupoASEJ(), gASEJ.cpodASEJ() == null ? 0 : gASEJ.cpodASEJ()));
                        ceodASEJ.getData().add(new XYChart.Data<>(gASEJ.grupoASEJ(), gASEJ.ceodASEJ() == null ? 0 : gASEJ.ceodASEJ()));
                    }
                    graficoASEJ.getData().setAll(List.of(cpodASEJ, ceodASEJ));
                    tablaASEJ.getItems().setAll(rASEJ.gruposASEJ());
                    notaASEJ.setText("Índices de lo firmado en el último odontograma de cada paciente, con la edad a la fecha"
                            + " de ese examen. «—»: índice no representativo para la edad."
                            + (rASEJ.excluyeTercerasMolaresASEJ() ? " El CPO-D excluye las terceras molares." : ""));
                }));
        generarASEJ.setOnAction(eASEJ -> cargarASEJ.run());
        excelASEJ.setOnAction(eASEJ -> conPeriodoASEJ(rangoASEJ, pASEJ -> exportadorASEJ.guardarExcelASEJ(excelASEJ, "epidemiologico-" + rangoASEJ.sufijoASEJ() + ".xlsx",
                () -> reporteServiceASEJ.excelEpidemiologicoASEJ(pASEJ.desdeASEJ(), pASEJ.hastaASEJ()))));
        generadoresASEJ.add(cargarASEJ);
        return panelASEJ(barraASEJ(rangoASEJ, generarASEJ, excelASEJ), resumenASEJ, leyendaASEJ, graficoASEJ, tablaASEJ, notaASEJ);
    }

    private record AuditoriaASEJ(List<AuditoriaDtoASEJ> filasASEJ, List<String> accionesASEJ) {
    }

    private Node auditoriaASEJ() {
        RangoASEJ rangoASEJ = new RangoASEJ();
        ComboBox<String> accionASEJ = new ComboBox<>();
        accionASEJ.getItems().add(TODASASEJ);
        accionASEJ.setValue(TODASASEJ);
        accionASEJ.setPrefWidth(230);
        TableView<AuditoriaDtoASEJ> tablaASEJ = new TableView<>();
        FormulariosASEJ.columnasASEJ(tablaASEJ,
                FormulariosASEJ.columnaASEJ("Fecha y hora", aASEJ -> FormulariosASEJ.fechaHoraASEJ(aASEJ.fechaASEJ()), 130),
                FormulariosASEJ.columnaASEJ("Usuario", AuditoriaDtoASEJ::usuarioASEJ, 200),
                FormulariosASEJ.columnaASEJ("Acción", AuditoriaDtoASEJ::accionASEJ, 190),
                FormulariosASEJ.columnaASEJ("Entidad", AuditoriaDtoASEJ::entidadASEJ, 120),
                FormulariosASEJ.columnaASEJ("Id", AuditoriaDtoASEJ::entidadIdASEJ, 60),
                FormulariosASEJ.columnaASEJ("Detalle", AuditoriaDtoASEJ::detalleASEJ, 300));
        tablaASEJ.setPlaceholder(new Label("Sin registros en el rango."));
        VBox.setVgrow(tablaASEJ, Priority.ALWAYS);
        Label totalASEJ = new Label();
        totalASEJ.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);

        Button generarASEJ = FormulariosASEJ.botonASEJ("Buscar", Feather.SEARCH, Styles.ACCENT);
        Button excelASEJ = botonExcelASEJ();
        Runnable cargarASEJ = () -> conPeriodoASEJ(rangoASEJ, pASEJ -> {
            String filtroASEJ = TODASASEJ.equals(accionASEJ.getValue()) ? null : accionASEJ.getValue();
            tareaUiASEJ.ejecutarASEJ(generarASEJ, () -> new AuditoriaASEJ(auditoriaServiceASEJ.buscarASEJ(pASEJ.desdeASEJ(), pASEJ.hastaASEJ(), filtroASEJ),
                    auditoriaServiceASEJ.accionesASEJ()), datosASEJ -> {
                List<AuditoriaDtoASEJ> filasASEJ = datosASEJ.filasASEJ();
                tablaASEJ.getItems().setAll(filasASEJ);
                totalASEJ.setText(filasASEJ.size() + " registro(s)" + (filasASEJ.size() >= AuditoriaServiceASEJ.MAXIMO_RESULTADOSASEJ
                        ? " (se muestran los " + AuditoriaServiceASEJ.MAXIMO_RESULTADOSASEJ + " más recientes; acote el rango)" : ""));
                String elegidaASEJ = accionASEJ.getValue();
                List<String> opcionesASEJ = new ArrayList<>(List.of(TODASASEJ));
                opcionesASEJ.addAll(datosASEJ.accionesASEJ());
                accionASEJ.getItems().setAll(opcionesASEJ);
                accionASEJ.setValue(opcionesASEJ.contains(elegidaASEJ) ? elegidaASEJ : TODASASEJ);
            });
        });
        generarASEJ.setOnAction(eASEJ -> cargarASEJ.run());
        excelASEJ.setOnAction(eASEJ -> conPeriodoASEJ(rangoASEJ, pASEJ -> {
            String filtroASEJ = TODASASEJ.equals(accionASEJ.getValue()) ? null : accionASEJ.getValue();
            exportadorASEJ.guardarExcelASEJ(excelASEJ, "auditoria-" + rangoASEJ.sufijoASEJ() + ".xlsx",
                    () -> reporteServiceASEJ.excelAuditoriaASEJ(pASEJ.desdeASEJ(), pASEJ.hastaASEJ(), filtroASEJ));
        }));
        generadoresASEJ.add(cargarASEJ);
        HBox barraASEJ = barraASEJ(rangoASEJ, generarASEJ, excelASEJ);
        barraASEJ.getChildren().add(4, new Label("Acción"));
        barraASEJ.getChildren().add(5, accionASEJ);
        return panelASEJ(barraASEJ, tablaASEJ, totalASEJ);
    }

    private static final class RangoASEJ {
        final DatePicker desdeSelectorASEJ = new DatePicker();
        final DatePicker hastaSelectorASEJ = new DatePicker();

        RangoASEJ() {
            FormulariosASEJ.configurarFechaASEJ(desdeSelectorASEJ);
            FormulariosASEJ.configurarFechaASEJ(hastaSelectorASEJ);
            desdeSelectorASEJ.setPrefWidth(140);
            hastaSelectorASEJ.setPrefWidth(140);
            fijarASEJ(desdeSelectorASEJ, LocalDate.now().withDayOfMonth(1));
            fijarASEJ(hastaSelectorASEJ, LocalDate.now());
        }

        static void fijarASEJ(DatePicker selectorASEJ, LocalDate fechaASEJ) {
            selectorASEJ.setValue(fechaASEJ);
            selectorASEJ.getEditor().setText(FormulariosASEJ.fechaASEJ(fechaASEJ));
        }

        String sufijoASEJ() {
            return desdeSelectorASEJ.getValue() + "_" + hastaSelectorASEJ.getValue();
        }
    }

    private record PeriodoASEJ(LocalDate desdeASEJ, LocalDate hastaASEJ) {
    }

    private void conPeriodoASEJ(RangoASEJ rangoASEJ, Consumer<PeriodoASEJ> accionASEJ) {
        PeriodoASEJ periodoASEJ;
        try {
            periodoASEJ = new PeriodoASEJ(FormulariosASEJ.fechaASEJ(rangoASEJ.desdeSelectorASEJ), FormulariosASEJ.fechaASEJ(rangoASEJ.hastaSelectorASEJ));
        } catch (RuntimeException eASEJ) {
            tareaUiASEJ.manejarErrorASEJ(eASEJ);
            return;
        }
        accionASEJ.accept(periodoASEJ);
    }

    private static HBox barraASEJ(RangoASEJ rangoASEJ, Button generarASEJ, Button excelASEJ) {
        HBox barraASEJ = new HBox(8, new Label("Desde"), rangoASEJ.desdeSelectorASEJ, new Label("Hasta"), rangoASEJ.hastaSelectorASEJ,
                generarASEJ, espacioASEJ(), excelASEJ);
        barraASEJ.setAlignment(Pos.CENTER_LEFT);
        return barraASEJ;
    }

    private static VBox panelASEJ(Node... hijosASEJ) {
        VBox vASEJ = new VBox(12, hijosASEJ);
        vASEJ.setPadding(new Insets(14, 20, 16, 20));
        return vASEJ;
    }

    private static Button botonGenerarASEJ() {
        return FormulariosASEJ.botonASEJ("Generar", Feather.PLAY, Styles.ACCENT);
    }

    private static Button botonExcelASEJ() {
        return FormulariosASEJ.botonASEJ("Exportar Excel", Feather.DOWNLOAD);
    }

    private static Region espacioASEJ() {
        Region rASEJ = new Region();
        HBox.setHgrow(rASEJ, Priority.ALWAYS);
        return rASEJ;
    }

    private static FlowPane resumenASEJ() {
        return new FlowPane(12, 8);
    }

    private static Node tarjetaASEJ(String tituloASEJ, String valorASEJ) {
        Label tASEJ = new Label(tituloASEJ);
        tASEJ.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);
        Label vASEJ = new Label(valorASEJ);
        vASEJ.getStyleClass().add("tarjeta-cuenta-valor");
        VBox cajaASEJ = new VBox(2, tASEJ, vASEJ);
        cajaASEJ.getStyleClass().add("tarjeta-cuenta");
        return cajaASEJ;
    }

    private static BarChart<String, Number> graficoASEJ(String estiloASEJ) {
        NumberAxis ejeASEJ = new NumberAxis();
        ejeASEJ.setForceZeroInRange(true);
        BarChart<String, Number> gASEJ = new BarChart<>(new CategoryAxis(), ejeASEJ);
        gASEJ.setAnimated(false);
        gASEJ.setLegendVisible(false);
        gASEJ.setMinHeight(190);
        gASEJ.setPrefHeight(230);
        gASEJ.getStyleClass().add(estiloASEJ);
        GraficosASEJ.limitarAnchoBarrasASEJ(gASEJ);
        return gASEJ;
    }

    private static Node itemLeyendaASEJ(String textoASEJ, String estiloColorASEJ) {
        Region colorASEJ = new Region();
        colorASEJ.getStyleClass().addAll("leyenda-color", estiloColorASEJ);
        Label lASEJ = new Label(textoASEJ);
        lASEJ.getStyleClass().add(Styles.TEXT_SMALL);
        HBox itemASEJ = new HBox(6, colorASEJ, lASEJ);
        itemASEJ.setAlignment(Pos.CENTER_LEFT);
        return itemASEJ;
    }

    private static void mostrarSerieASEJ(BarChart<String, Number> graficoASEJ, String nombreASEJ, List<MontoPeriodoDtoASEJ> montosASEJ) {
        XYChart.Series<String, Number> serieASEJ = new XYChart.Series<>();
        serieASEJ.setName(nombreASEJ);
        for (MontoPeriodoDtoASEJ mASEJ : montosASEJ) {
            serieASEJ.getData().add(new XYChart.Data<>(mASEJ.etiquetaASEJ(), mASEJ.montoASEJ()));
        }
        graficoASEJ.getData().setAll(List.of(serieASEJ));
        for (int iASEJ = 0; iASEJ < serieASEJ.getData().size(); iASEJ++) {
            XYChart.Data<String, Number> datoASEJ = serieASEJ.getData().get(iASEJ);
            BigDecimal montoASEJ = montosASEJ.get(iASEJ).montoASEJ();
            if (datoASEJ.getNode() != null) {
                Tooltip.install(datoASEJ.getNode(), new Tooltip(datoASEJ.getXValue() + ": " + FormulariosASEJ.solesASEJ(montoASEJ)));
            }
        }
    }
}
