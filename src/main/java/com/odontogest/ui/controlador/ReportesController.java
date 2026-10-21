/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.controlador;

import atlantafx.base.theme.Styles;
import com.odontogest.evento.NavegarA;
import com.odontogest.modelo.MedioPago;
import com.odontogest.servicio.AuditoriaService;
import com.odontogest.servicio.ReporteService;
import com.odontogest.servicio.dto.AuditoriaDto;
import com.odontogest.servicio.dto.GrupoIndicesDto;
import com.odontogest.servicio.dto.IngresoDiaDto;
import com.odontogest.servicio.dto.MontoPeriodoDto;
import com.odontogest.servicio.dto.MorosoDto;
import com.odontogest.servicio.dto.PagoReporteDto;
import com.odontogest.servicio.dto.ProduccionFilaDto;
import com.odontogest.servicio.dto.ReporteEpidemiologicoDto;
import com.odontogest.servicio.dto.ReporteIngresosDto;
import com.odontogest.servicio.dto.ReporteMorosidadDto;
import com.odontogest.servicio.dto.ReporteProduccionDto;
import com.odontogest.servicio.dto.TipoReporte;
import com.odontogest.ui.soporte.Exportador;
import com.odontogest.ui.soporte.Formularios;
import com.odontogest.ui.soporte.Graficos;
import com.odontogest.ui.soporte.Refrescable;
import com.odontogest.ui.soporte.TareaUi;
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

/**
 * Pantalla de reportes (RF-REP-02 a RF-REP-05 y auditoría). Cada pestaña tiene su rango de fechas, un resumen,
 * gráfico y tabla, y "Exportar Excel". Solo se muestran las pestañas que {@link ReporteService#disponibles()}
 * permite al rol del usuario.
 */
@Component
@RequiredArgsConstructor
public class ReportesController implements Refrescable {

    private static final String TODAS = "(todas las acciones)";

    private final ReporteService reporteService;
    private final AuditoriaService auditoriaService;
    private final TareaUi tareaUi;
    private final Exportador exportador;
    private final ApplicationEventPublisher eventos;

    @FXML private TabPane pestanas;
    @FXML private Tab tabIngresos;
    @FXML private Tab tabProduccion;
    @FXML private Tab tabMorosidad;
    @FXML private Tab tabEpidemiologico;
    @FXML private Tab tabAuditoria;

    /** Generar de cada pestaña ya construida; se ejecuta al refrescar la vista. */
    private final List<Runnable> generadores = new ArrayList<>();

    @FXML
    public void initialize() {
        // El controlador es único y la vista se vuelve a cargar en cada inicio de sesión (quizá con otro rol).
        generadores.clear();
        Set<TipoReporte> disponibles = reporteService.disponibles();
        Map<TipoReporte, Tab> tabs = Map.of(TipoReporte.INGRESOS, tabIngresos, TipoReporte.PRODUCCION, tabProduccion,
                TipoReporte.MOROSIDAD, tabMorosidad, TipoReporte.EPIDEMIOLOGICO, tabEpidemiologico,
                TipoReporte.AUDITORIA, tabAuditoria);
        tabs.forEach((tipo, tab) -> {
            if (!disponibles.contains(tipo)) {
                pestanas.getTabs().remove(tab);
            }
        });
        if (disponibles.contains(TipoReporte.INGRESOS)) {
            tabIngresos.setContent(ingresos());
        }
        if (disponibles.contains(TipoReporte.PRODUCCION)) {
            tabProduccion.setContent(produccion());
        }
        if (disponibles.contains(TipoReporte.MOROSIDAD)) {
            tabMorosidad.setContent(morosidad());
        }
        if (disponibles.contains(TipoReporte.EPIDEMIOLOGICO)) {
            tabEpidemiologico.setContent(epidemiologico());
        }
        if (disponibles.contains(TipoReporte.AUDITORIA)) {
            tabAuditoria.setContent(auditoria());
        }
    }

    @Override
    public void refrescar() {
        generadores.forEach(Runnable::run);
    }

    /** Selecciona la pestaña por el nombre de {@link TipoReporte} (desde el tablero de inicio). */
    public void seleccionarPestana(String tipo) {
        pestanas.getTabs().stream().filter(t -> t.getId().equals(tipo)).findFirst()
                .ifPresent(t -> pestanas.getSelectionModel().select(t));
    }

    // ------------------------------------------------------------------ ingresos

    private Node ingresos() {
        Rango rango = new Rango();
        FlowPane resumen = resumen();
        BarChart<String, Number> grafico = grafico("grafico-montos");
        TableView<IngresoDiaDto> dias = new TableView<>();
        List<TableColumn<IngresoDiaDto, ?>> columnas = new ArrayList<>();
        columnas.add(Formularios.columna("Fecha", d -> Formularios.fecha(d.dia()), 95));
        columnas.add(Formularios.columna("Pagos", IngresoDiaDto::pagos, 60));
        for (MedioPago m : MedioPago.values()) {
            columnas.add(Formularios.columna(m.etiqueta(), d -> Formularios.soles(d.monto(m)), 100));
        }
        columnas.add(Formularios.columna("Total", d -> Formularios.soles(d.total()), 110));
        dias.getColumns().setAll(columnas);
        dias.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        dias.setPlaceholder(new Label("No hay pagos en el rango."));
        TableView<PagoReporteDto> detalle = new TableView<>();
        Formularios.columnas(detalle,
                Formularios.columna("Fecha y hora", p -> Formularios.fechaHora(p.fecha()), 130),
                Formularios.columna("Recibo", PagoReporteDto::numeroRecibo, 105),
                Formularios.columna("Historia", PagoReporteDto::numeroHistoria, 95),
                Formularios.columna("Paciente", PagoReporteDto::paciente, 220),
                Formularios.columna("Medio", p -> p.medio().etiqueta(), 100),
                Formularios.columna("Referencia", PagoReporteDto::referencia, 120),
                Formularios.columna("Monto", p -> Formularios.soles(p.monto()), 100));
        detalle.setPlaceholder(new Label("No hay pagos en el rango."));
        TabPane tablas = new TabPane(new Tab("Por día", dias), new Tab("Detalle de pagos", detalle));
        tablas.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tablas.getStyleClass().add(Styles.TABS_FLOATING);
        VBox.setVgrow(tablas, Priority.ALWAYS);

        Button generar = botonGenerar();
        Button excel = botonExcel();
        Runnable cargar = () -> conPeriodo(rango, p -> tareaUi.ejecutar(generar, () -> reporteService.ingresos(p.desde(), p.hasta()),
                (ReporteIngresosDto r) -> {
                    List<Node> tarjetas = new ArrayList<>();
                    tarjetas.add(tarjeta("Total", Formularios.soles(r.total())));
                    tarjetas.add(tarjeta("Pagos", String.valueOf(r.pagos())));
                    for (MedioPago m : MedioPago.values()) {
                        tarjetas.add(tarjeta(m.etiqueta(), Formularios.soles(r.totalesPorMedio().get(m))));
                    }
                    resumen.getChildren().setAll(tarjetas);
                    mostrarSerie(grafico, "Ingresos", r.serie());
                    dias.getItems().setAll(r.dias());
                    detalle.getItems().setAll(r.detalle());
                }));
        generar.setOnAction(e -> cargar.run());
        excel.setOnAction(e -> conPeriodo(rango, p -> exportador.guardarExcel(excel, "ingresos-" + rango.sufijo() + ".xlsx",
                () -> reporteService.excelIngresos(p.desde(), p.hasta()))));
        generadores.add(cargar);
        return panel(barra(rango, generar, excel), resumen, grafico, tablas);
    }

    // ------------------------------------------------------------------ producción

    private Node produccion() {
        Rango rango = new Rango();
        FlowPane resumen = resumen();
        BarChart<String, Number> grafico = grafico("grafico-montos");
        TableView<ProduccionFilaDto> tabla = new TableView<>();
        Formularios.columnas(tabla,
                Formularios.columna("Odontólogo", ProduccionFilaDto::odontologo, 200),
                Formularios.columna("Código", ProduccionFilaDto::codigo, 90),
                Formularios.columna("Procedimiento", ProduccionFilaDto::procedimiento, 260),
                Formularios.columna("Cantidad", ProduccionFilaDto::cantidad, 80),
                Formularios.columna("Monto", f -> Formularios.soles(f.monto()), 110));
        tabla.setPlaceholder(new Label("No hay procedimientos realizados en el rango."));
        VBox.setVgrow(tabla, Priority.ALWAYS);

        Button generar = botonGenerar();
        Button excel = botonExcel();
        Runnable cargar = () -> conPeriodo(rango, p -> tareaUi.ejecutar(generar, () -> reporteService.produccion(p.desde(), p.hasta()),
                (ReporteProduccionDto r) -> {
                    resumen.getChildren().setAll(tarjeta("Total producido", Formularios.soles(r.total())),
                            tarjeta("Procedimientos", String.valueOf(r.procedimientos())),
                            tarjeta("Odontólogos", String.valueOf(r.porOdontologo().size())));
                    mostrarSerie(grafico, "Producción", r.porOdontologo());
                    tabla.getItems().setAll(r.filas());
                }));
        generar.setOnAction(e -> cargar.run());
        excel.setOnAction(e -> conPeriodo(rango, p -> exportador.guardarExcel(excel, "produccion-" + rango.sufijo() + ".xlsx",
                () -> reporteService.excelProduccion(p.desde(), p.hasta()))));
        generadores.add(cargar);
        return panel(barra(rango, generar, excel), resumen, grafico, tabla);
    }

    // ------------------------------------------------------------------ morosidad

    private Node morosidad() {
        FlowPane resumen = resumen();
        TableView<MorosoDto> tabla = new TableView<>();
        Formularios.columnas(tabla,
                Formularios.columna("Historia", MorosoDto::numeroHistoria, 95),
                Formularios.columna("Paciente", MorosoDto::paciente, 240),
                Formularios.columna("Teléfono", MorosoDto::telefono, 105),
                Formularios.columna("Cuotas vencidas", MorosoDto::cuotasVencidas, 110),
                Formularios.columna("Vence desde", m -> Formularios.fecha(m.vencimientoMasAntiguo()), 100),
                Formularios.columna("Días de atraso", MorosoDto::diasAtraso, 100),
                Formularios.columna("Saldo vencido", m -> Formularios.soles(m.saldoVencido()), 120));
        tabla.setPlaceholder(new Label("Ningún paciente tiene cuotas vencidas."));
        tabla.setRowFactory(t -> {
            TableRow<MorosoDto> fila = new TableRow<>();
            fila.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !fila.isEmpty()) {
                    eventos.publishEvent(new NavegarA(MainController.VISTA_FICHA,
                            Map.of("pacienteId", fila.getItem().pacienteId(), "pestana", "pagos")));
                }
            });
            return fila;
        });
        VBox.setVgrow(tabla, Priority.ALWAYS);

        Label fecha = new Label();
        fecha.getStyleClass().add(Styles.TEXT_MUTED);
        Button generar = Formularios.boton("Actualizar", Feather.REFRESH_CW);
        Button excel = botonExcel();
        Runnable cargar = () -> tareaUi.ejecutar(generar, reporteService::morosidad, (ReporteMorosidadDto r) -> {
            fecha.setText("Cuotas vencidas al " + Formularios.fecha(r.fecha())
                    + " (doble clic en un paciente para abrir sus pagos)");
            resumen.getChildren().setAll(tarjeta("Deuda vencida", Formularios.soles(r.total())),
                    tarjeta("Pacientes", String.valueOf(r.pacientes().size())),
                    tarjeta("Cuotas", String.valueOf(r.cuotas())));
            tabla.getItems().setAll(r.pacientes());
        });
        generar.setOnAction(e -> cargar.run());
        excel.setOnAction(e -> exportador.guardarExcel(excel, "morosidad-" + LocalDate.now() + ".xlsx",
                reporteService::excelMorosidad));
        generadores.add(cargar);
        HBox barra = new HBox(8, fecha, espacio(), generar, excel);
        barra.setAlignment(Pos.CENTER_LEFT);
        return panel(barra, resumen, tabla);
    }

    // ------------------------------------------------------------------ epidemiológico

    private Node epidemiologico() {
        Rango rango = new Rango();
        Rango.fijar(rango.desdeSelector, LocalDate.now().withDayOfYear(1));
        FlowPane resumen = resumen();
        BarChart<String, Number> grafico = grafico("grafico-indices");
        // Leyenda propia: la del BarChart se oculta sola cuando el gráfico es bajo.
        HBox leyenda = new HBox(16, itemLeyenda("CPO-D (dentición permanente)", "leyenda-cpod"),
                itemLeyenda("ceo-d (dentición decidua)", "leyenda-ceod"));
        leyenda.setAlignment(Pos.CENTER_LEFT);
        TableView<GrupoIndicesDto> tabla = new TableView<>();
        Function<Double, String> num = v -> v == null ? "—" : String.format(Locale.ROOT, "%.2f", v);
        Formularios.columnas(tabla,
                Formularios.columna("Grupo de edad", GrupoIndicesDto::grupo, 130),
                Formularios.columna("Pacientes", GrupoIndicesDto::pacientes, 80),
                Formularios.columna("CPO-D", g -> num.apply(g.cpod()), 70),
                Formularios.columna("C", g -> num.apply(g.c()), 60),
                Formularios.columna("P", g -> num.apply(g.p()), 60),
                Formularios.columna("O", g -> num.apply(g.o()), 60),
                Formularios.columna("ceo-d", g -> num.apply(g.ceod()), 70),
                Formularios.columna("Nivel OMS (CPO-D)", g -> g.nivelCpod() == null ? "—" : g.nivelCpod(), 140));
        tabla.setPlaceholder(new Label("Sin pacientes examinados en el rango."));
        VBox.setVgrow(tabla, Priority.ALWAYS);
        Label nota = new Label();
        nota.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);
        nota.setWrapText(true);

        Button generar = botonGenerar();
        Button excel = botonExcel();
        Runnable cargar = () -> conPeriodo(rango, p -> tareaUi.ejecutar(generar,
                () -> reporteService.epidemiologico(p.desde(), p.hasta()), (ReporteEpidemiologicoDto r) -> {
                    resumen.getChildren().setAll(tarjeta("Pacientes examinados", String.valueOf(r.pacientes())));
                    XYChart.Series<String, Number> cpod = new XYChart.Series<>();
                    cpod.setName("CPO-D");
                    XYChart.Series<String, Number> ceod = new XYChart.Series<>();
                    ceod.setName("ceo-d");
                    for (GrupoIndicesDto g : r.grupos()) {
                        cpod.getData().add(new XYChart.Data<>(g.grupo(), g.cpod() == null ? 0 : g.cpod()));
                        ceod.getData().add(new XYChart.Data<>(g.grupo(), g.ceod() == null ? 0 : g.ceod()));
                    }
                    grafico.getData().setAll(List.of(cpod, ceod));
                    tabla.getItems().setAll(r.grupos());
                    nota.setText("Índices de lo firmado en el último odontograma de cada paciente, con la edad a la fecha"
                            + " de ese examen. «—»: índice no representativo para la edad."
                            + (r.excluyeTercerasMolares() ? " El CPO-D excluye las terceras molares." : ""));
                }));
        generar.setOnAction(e -> cargar.run());
        excel.setOnAction(e -> conPeriodo(rango, p -> exportador.guardarExcel(excel, "epidemiologico-" + rango.sufijo() + ".xlsx",
                () -> reporteService.excelEpidemiologico(p.desde(), p.hasta()))));
        generadores.add(cargar);
        return panel(barra(rango, generar, excel), resumen, leyenda, grafico, tabla, nota);
    }

    // ------------------------------------------------------------------ auditoría

    /** Resultado de la búsqueda y acciones registradas (para el filtro). */
    private record Auditoria(List<AuditoriaDto> filas, List<String> acciones) {
    }

    private Node auditoria() {
        Rango rango = new Rango();
        ComboBox<String> accion = new ComboBox<>();
        accion.getItems().add(TODAS);
        accion.setValue(TODAS);
        accion.setPrefWidth(230);
        TableView<AuditoriaDto> tabla = new TableView<>();
        Formularios.columnas(tabla,
                Formularios.columna("Fecha y hora", a -> Formularios.fechaHora(a.fecha()), 130),
                Formularios.columna("Usuario", AuditoriaDto::usuario, 200),
                Formularios.columna("Acción", AuditoriaDto::accion, 190),
                Formularios.columna("Entidad", AuditoriaDto::entidad, 120),
                Formularios.columna("Id", AuditoriaDto::entidadId, 60),
                Formularios.columna("Detalle", AuditoriaDto::detalle, 300));
        tabla.setPlaceholder(new Label("Sin registros en el rango."));
        VBox.setVgrow(tabla, Priority.ALWAYS);
        Label total = new Label();
        total.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);

        Button generar = Formularios.boton("Buscar", Feather.SEARCH, Styles.ACCENT);
        Button excel = botonExcel();
        Runnable cargar = () -> conPeriodo(rango, p -> {
            String filtro = TODAS.equals(accion.getValue()) ? null : accion.getValue();
            tareaUi.ejecutar(generar, () -> new Auditoria(auditoriaService.buscar(p.desde(), p.hasta(), filtro),
                    auditoriaService.acciones()), datos -> {
                List<AuditoriaDto> filas = datos.filas();
                tabla.getItems().setAll(filas);
                total.setText(filas.size() + " registro(s)" + (filas.size() >= AuditoriaService.MAXIMO_RESULTADOS
                        ? " (se muestran los " + AuditoriaService.MAXIMO_RESULTADOS + " más recientes; acote el rango)" : ""));
                String elegida = accion.getValue();
                List<String> opciones = new ArrayList<>(List.of(TODAS));
                opciones.addAll(datos.acciones());
                accion.getItems().setAll(opciones);
                accion.setValue(opciones.contains(elegida) ? elegida : TODAS);
            });
        });
        generar.setOnAction(e -> cargar.run());
        excel.setOnAction(e -> conPeriodo(rango, p -> {
            String filtro = TODAS.equals(accion.getValue()) ? null : accion.getValue();
            exportador.guardarExcel(excel, "auditoria-" + rango.sufijo() + ".xlsx",
                    () -> reporteService.excelAuditoria(p.desde(), p.hasta(), filtro));
        }));
        generadores.add(cargar);
        HBox barra = barra(rango, generar, excel);
        barra.getChildren().add(4, new Label("Acción"));
        barra.getChildren().add(5, accion);
        return panel(barra, tabla, total);
    }

    // ------------------------------------------------------------------ piezas comunes

    /** Selectores "Desde" y "Hasta"; por defecto, el mes en curso hasta hoy. */
    private static final class Rango {
        final DatePicker desdeSelector = new DatePicker();
        final DatePicker hastaSelector = new DatePicker();

        Rango() {
            Formularios.configurarFecha(desdeSelector);
            Formularios.configurarFecha(hastaSelector);
            desdeSelector.setPrefWidth(140);
            hastaSelector.setPrefWidth(140);
            fijar(desdeSelector, LocalDate.now().withDayOfMonth(1));
            fijar(hastaSelector, LocalDate.now());
        }

        /**
         * Valor y texto del editor a la vez: {@link Formularios#fecha} lee el texto, y el DatePicker solo lo actualiza
         * cuando ya está en pantalla (la pestaña se genera antes de mostrarse).
         */
        static void fijar(DatePicker selector, LocalDate fecha) {
            selector.setValue(fecha);
            selector.getEditor().setText(Formularios.fecha(fecha));
        }

        String sufijo() {
            return desdeSelector.getValue() + "_" + hastaSelector.getValue();
        }
    }

    /** Fechas elegidas, leídas en el hilo de la interfaz. */
    private record Periodo(LocalDate desde, LocalDate hasta) {
    }

    /** Lee el rango en el hilo de la interfaz y, si es válido, continúa con él; si no, avisa al usuario. */
    private void conPeriodo(Rango rango, Consumer<Periodo> accion) {
        Periodo periodo;
        try {
            periodo = new Periodo(Formularios.fecha(rango.desdeSelector), Formularios.fecha(rango.hastaSelector));
        } catch (RuntimeException e) {
            tareaUi.manejarError(e);
            return;
        }
        accion.accept(periodo);
    }

    private static HBox barra(Rango rango, Button generar, Button excel) {
        HBox barra = new HBox(8, new Label("Desde"), rango.desdeSelector, new Label("Hasta"), rango.hastaSelector,
                generar, espacio(), excel);
        barra.setAlignment(Pos.CENTER_LEFT);
        return barra;
    }

    private static VBox panel(Node... hijos) {
        VBox v = new VBox(12, hijos);
        v.setPadding(new Insets(14, 20, 16, 20));
        return v;
    }

    private static Button botonGenerar() {
        return Formularios.boton("Generar", Feather.PLAY, Styles.ACCENT);
    }

    private static Button botonExcel() {
        return Formularios.boton("Exportar Excel", Feather.DOWNLOAD);
    }

    private static Region espacio() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    private static FlowPane resumen() {
        return new FlowPane(12, 8);
    }

    private static Node tarjeta(String titulo, String valor) {
        Label t = new Label(titulo);
        t.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);
        Label v = new Label(valor);
        v.getStyleClass().add("tarjeta-cuenta-valor");
        VBox caja = new VBox(2, t, v);
        caja.getStyleClass().add("tarjeta-cuenta");
        return caja;
    }

    private static BarChart<String, Number> grafico(String estilo) {
        NumberAxis eje = new NumberAxis();
        eje.setForceZeroInRange(true);
        BarChart<String, Number> g = new BarChart<>(new CategoryAxis(), eje);
        g.setAnimated(false);
        g.setLegendVisible(false);
        g.setMinHeight(190);
        g.setPrefHeight(230);
        g.getStyleClass().add(estilo);
        Graficos.limitarAnchoBarras(g);
        return g;
    }

    private static Node itemLeyenda(String texto, String estiloColor) {
        Region color = new Region();
        color.getStyleClass().addAll("leyenda-color", estiloColor);
        Label l = new Label(texto);
        l.getStyleClass().add(Styles.TEXT_SMALL);
        HBox item = new HBox(6, color, l);
        item.setAlignment(Pos.CENTER_LEFT);
        return item;
    }

    private static void mostrarSerie(BarChart<String, Number> grafico, String nombre, List<MontoPeriodoDto> montos) {
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName(nombre);
        for (MontoPeriodoDto m : montos) {
            serie.getData().add(new XYChart.Data<>(m.etiqueta(), m.monto()));
        }
        grafico.getData().setAll(List.of(serie));
        for (int i = 0; i < serie.getData().size(); i++) {
            XYChart.Data<String, Number> dato = serie.getData().get(i);
            BigDecimal monto = montos.get(i).monto();
            if (dato.getNode() != null) {
                Tooltip.install(dato.getNode(), new Tooltip(dato.getXValue() + ": " + Formularios.soles(monto)));
            }
        }
    }
}
