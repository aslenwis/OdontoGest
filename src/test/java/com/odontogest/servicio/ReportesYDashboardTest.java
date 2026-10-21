/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio;

import com.odontogest.PruebaIntegracion;
import com.odontogest.dominio.finanzas.Frecuencia;
import com.odontogest.dominio.odontograma.Superficie;
import com.odontogest.excepcion.NegocioException;
import com.odontogest.excepcion.PermisoDenegadoException;
import com.odontogest.modelo.MedioPago;
import com.odontogest.modelo.Sexo;
import com.odontogest.modelo.TipoDocumento;
import com.odontogest.persistencia.dao.DashboardDao;
import com.odontogest.servicio.dto.AtencionForm;
import com.odontogest.servicio.dto.CronogramaForm;
import com.odontogest.servicio.dto.DashboardDto;
import com.odontogest.servicio.dto.GrupoIndicesDto;
import com.odontogest.servicio.dto.HallazgoTipoDto;
import com.odontogest.servicio.dto.IngresoDiaDto;
import com.odontogest.servicio.dto.MontoPeriodoDto;
import com.odontogest.servicio.dto.MorosoDto;
import com.odontogest.servicio.dto.NuevoHallazgo;
import com.odontogest.servicio.dto.PacienteForm;
import com.odontogest.servicio.dto.PagoForm;
import com.odontogest.servicio.dto.PlanItemForm;
import com.odontogest.servicio.dto.ProcedimientoDto;
import com.odontogest.servicio.dto.ProduccionFilaDto;
import com.odontogest.servicio.dto.ReporteEpidemiologicoDto;
import com.odontogest.servicio.dto.ReporteIngresosDto;
import com.odontogest.servicio.dto.ReporteProduccionDto;
import com.odontogest.servicio.dto.TipoReporte;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tablero de inicio, reportes y Excel (RF-REP-01 a RF-REP-05) contra la base real. Como todas las clases de prueba
 * comparten la base, cada escenario mueve sus fechas a un año lejano propio (2093 en adelante) y las aserciones
 * exactas se hacen solo sobre ese año.
 */
class ReportesYDashboardTest extends PruebaIntegracion {

    private static final AtomicInteger ANIO = new AtomicInteger(2093);

    @Autowired private PacienteService pacienteService;
    @Autowired private PlanTratamientoService planService;
    @Autowired private ProcedimientoService procedimientoService;
    @Autowired private AtencionService atencionService;
    @Autowired private OdontogramaService odontogramaService;
    @Autowired private CatalogoHallazgoService catalogoService;
    @Autowired private PagoService pagoService;
    @Autowired private ReporteService reporteService;
    @Autowired private DashboardService dashboardService;
    @Autowired private JdbcTemplate jdbc;

    /** Datos de un escenario: paciente con plan de S/ 300 (inicial 100 + 2 cuotas de 100) y dos pagos. */
    private record Escenario(int anio, long pacienteId, String historia, long planItemId) {
    }

    /**
     * En el año del escenario: paciente registrado el 02/03, odontograma con una caries firmado el 01/02,
     * profilaxis realizada el 10/03, pagos de S/ 150 en efectivo (05/03) y S/ 50 por Yape (20/03). Queda vencida la
     * segunda cuota (S/ 100) para cualquier "hoy" posterior a su vencimiento real.
     */
    private Escenario escenario() {
        int anio = ANIO.getAndIncrement();
        iniciarSesion("odontologo");
        long paciente = pacienteService.guardar(new PacienteForm(null, TipoDocumento.DNI, dniAleatorio(), "Paciente",
                "Reporte" + sufijo(), null, LocalDate.of(1990, 5, 10), Sexo.M, "987654321", null, null, null, null,
                null, null, null, null, true));
        long atencion = atencionService.abrir(new AtencionForm(null, paciente, null, "Control", null, null, null, null));
        long odontograma = odontogramaService.obtenerVista(paciente, null).odontogramaId();
        long caries = catalogoService.listarTipos(true).stream().filter(t -> t.codigo().equals("CARIES"))
                .map(HallazgoTipoDto::id).findFirst().orElseThrow();
        odontogramaService.registrarHallazgo(new NuevoHallazgo(odontograma, caries, 36, null, Set.of(Superficie.O),
                null, "cd", null));
        odontogramaService.cerrar(odontograma);

        long prof = procedimientoService.listar(false).stream().filter(p -> p.codigo().equals("PROF"))
                .map(ProcedimientoDto::id).findFirst().orElseThrow();
        long plan = planService.crear(paciente, null, false);
        long item = planService.guardarItem(new PlanItemForm(null, plan, prof, null, null, 4, 1, new BigDecimal("300"),
                null)).items().get(0).id();
        planService.cambiarEstado(plan, com.odontogest.modelo.EstadoPlan.PROPUESTO);
        planService.generarCronograma(new CronogramaForm(plan, new BigDecimal("100"), 2, LocalDate.now().plusMonths(1),
                Frecuencia.MENSUAL));
        planService.aceptar(plan, "Paciente de prueba");
        planService.marcarEnAtencion(atencion, item, true);

        iniciarSesion("recepcion");
        long efectivo = pagoService.registrar(new PagoForm(paciente, new BigDecimal("150"), MedioPago.EFECTIVO, null)).id();
        long yape = pagoService.registrar(new PagoForm(paciente, new BigDecimal("50"), MedioPago.YAPE, "OP-1")).id();

        jdbc.update("UPDATE paciente SET creado_en = ? WHERE id = ?", anio + "-03-02T08:00:00", paciente);
        jdbc.update("UPDATE odontograma SET cerrado_en = ? WHERE id = ?", anio + "-02-01T09:00:00", odontograma);
        jdbc.update("UPDATE plan_item SET realizado_en = ? WHERE id = ?", anio + "-03-10T09:30:00", item);
        jdbc.update("UPDATE pago SET fecha = ? WHERE id = ?", anio + "-03-05T10:00:00", efectivo);
        jdbc.update("UPDATE pago SET fecha = ? WHERE id = ?", anio + "-03-20T16:30:00", yape);
        String historia = pacienteService.obtener(paciente).numeroHistoria();
        return new Escenario(anio, paciente, historia, item);
    }

    // ------------------------------------------------------------------ ingresos

    @Test
    void ingresosPorDiaYMedioDePago() {
        Escenario e = escenario();
        iniciarSesion("recepcion");
        ReporteIngresosDto r = reporteService.ingresos(LocalDate.of(e.anio(), 3, 1), LocalDate.of(e.anio(), 3, 31));

        assertThat(r.total()).isEqualByComparingTo("200.00");
        assertThat(r.pagos()).isEqualTo(2);
        assertThat(r.totalesPorMedio().get(MedioPago.EFECTIVO)).isEqualByComparingTo("150");
        assertThat(r.totalesPorMedio().get(MedioPago.YAPE)).isEqualByComparingTo("50");
        assertThat(r.totalesPorMedio().get(MedioPago.TARJETA)).isEqualByComparingTo("0");
        assertThat(r.dias()).extracting(IngresoDiaDto::dia)
                .containsExactly(LocalDate.of(e.anio(), 3, 5), LocalDate.of(e.anio(), 3, 20));
        assertThat(r.dias().get(1).monto(MedioPago.YAPE)).isEqualByComparingTo("50");
        assertThat(r.serie()).as("rango corto: una barra por día, incluidos los días sin pagos").hasSize(31);
        assertThat(r.serie().get(4)).isEqualTo(new MontoPeriodoDto("05/03", new BigDecimal("150.00")));
        assertThat(r.detalle()).hasSize(2).allMatch(p -> p.numeroHistoria().equals(e.historia()));
        assertThat(r.detalle().get(1).referencia()).isEqualTo("OP-1");
        assertThat(r.detalle().get(0).paciente()).startsWith("Reporte");
    }

    @Test
    void ingresosAnulados_noSeCuentan() {
        Escenario e = escenario();
        long efectivo = jdbc.queryForObject("SELECT id FROM pago WHERE paciente_id = ? AND medio = 'EFECTIVO'",
                Long.class, e.pacienteId());
        iniciarSesion("admin");
        pagoService.anular(efectivo, "Error de digitación en la prueba");
        ReporteIngresosDto r = reporteService.ingresos(LocalDate.of(e.anio(), 1, 1), LocalDate.of(e.anio(), 12, 31));
        assertThat(r.total()).isEqualByComparingTo("50");
        assertThat(r.serie()).as("rango largo: una barra por mes").hasSize(12);
        assertThat(r.serie().get(2).monto()).isEqualByComparingTo("50");
    }

    @Test
    void rangosInvalidos() {
        iniciarSesion("admin");
        LocalDate hoy = LocalDate.now();
        assertThatThrownBy(() -> reporteService.ingresos(hoy, hoy.minusDays(1)))
                .isInstanceOf(NegocioException.class).hasMessageContaining("anterior");
        assertThatThrownBy(() -> reporteService.ingresos(null, hoy)).isInstanceOf(NegocioException.class);
        assertThatThrownBy(() -> reporteService.produccion(hoy.minusYears(4), hoy))
                .isInstanceOf(NegocioException.class).hasMessageContaining("3 años");
    }

    // ------------------------------------------------------------------ permisos por rol

    @Test
    void cadaRolVeSusReportes() {
        iniciarSesion("admin");
        assertThat(reporteService.disponibles()).containsExactly(TipoReporte.values());
        iniciarSesion("odontologo");
        assertThat(reporteService.disponibles()).containsExactly(TipoReporte.PRODUCCION, TipoReporte.EPIDEMIOLOGICO);
        LocalDate hoy = LocalDate.now();
        assertThatThrownBy(() -> reporteService.ingresos(hoy, hoy)).isInstanceOf(PermisoDenegadoException.class);
        assertThatThrownBy(() -> reporteService.morosidad()).isInstanceOf(PermisoDenegadoException.class);
        iniciarSesion("recepcion");
        assertThat(reporteService.disponibles()).containsExactly(TipoReporte.INGRESOS, TipoReporte.MOROSIDAD);
        assertThatThrownBy(() -> reporteService.produccion(hoy, hoy)).isInstanceOf(PermisoDenegadoException.class);
        assertThatThrownBy(() -> reporteService.epidemiologico(hoy, hoy)).isInstanceOf(PermisoDenegadoException.class);
        assertThatThrownBy(() -> reporteService.excelAuditoria(hoy, hoy, null)).isInstanceOf(PermisoDenegadoException.class);
    }

    // ------------------------------------------------------------------ producción

    @Test
    void produccion_odontologoVeLaSuya_adminLaDeTodos() {
        Escenario e = escenario();
        LocalDate desde = LocalDate.of(e.anio(), 3, 1);
        LocalDate hasta = LocalDate.of(e.anio(), 3, 31);
        iniciarSesion("odontologo");
        ReporteProduccionDto propia = reporteService.produccion(desde, hasta);
        assertThat(propia.filas()).extracting(ProduccionFilaDto::codigo, ProduccionFilaDto::cantidad)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("PROF", 1));
        assertThat(propia.total()).isEqualByComparingTo("300");
        assertThat(propia.porOdontologo()).hasSize(1);

        // Otro odontólogo realiza un procedimiento ese mes: el odontólogo de la sesión no lo ve; el admin, sí.
        Long otro = jdbc.queryForObject("SELECT MIN(id) FROM odontologo WHERE id <> ?", Long.class,
                iniciarSesion("odontologo").odontologoId());
        if (otro != null) {
            jdbc.update("UPDATE atencion SET odontologo_id = ? WHERE id = (SELECT atencion_id FROM plan_item WHERE id = ?)",
                    otro, e.planItemId());
            assertThat(reporteService.produccion(desde, hasta).filas()).isEmpty();
            iniciarSesion("admin");
            assertThat(reporteService.produccion(desde, hasta).filas()).extracting(ProduccionFilaDto::odontologoId)
                    .containsExactly(otro);
        }
    }

    // ------------------------------------------------------------------ morosidad

    @Test
    void morosidadDelPaciente() {
        Escenario e = escenario();
        iniciarSesion("recepcion");
        LocalDate hoy = LocalDate.now().plusYears(1);
        MorosoDto m = reporteService.morosidad(hoy).pacientes().stream()
                .filter(p -> p.pacienteId() == e.pacienteId()).findFirst().orElseThrow();
        assertThat(m.cuotasVencidas()).as("la inicial y la primera cuota están pagadas").isEqualTo(1);
        assertThat(m.saldoVencido()).isEqualByComparingTo("100");
        assertThat(m.vencimientoMasAntiguo()).isEqualTo(LocalDate.now().plusMonths(2));
        assertThat(m.diasAtraso()).isPositive();
        assertThat(m.telefono()).isEqualTo("987654321");
        assertThat(reporteService.morosidad(LocalDate.now()).pacientes())
                .noneMatch(p -> p.pacienteId() == e.pacienteId());
    }

    // ------------------------------------------------------------------ epidemiológico

    @Test
    void epidemiologicoPorGrupoDeEdad() throws IOException {
        Escenario e = escenario();
        iniciarSesion("odontologo");
        LocalDate desde = LocalDate.of(e.anio(), 1, 1);
        LocalDate hasta = LocalDate.of(e.anio(), 12, 31);
        ReporteEpidemiologicoDto r = reporteService.epidemiologico(desde, hasta);
        assertThat(r.pacientes()).isEqualTo(1);
        assertThat(r.grupos()).hasSize(7);
        GrupoIndicesDto mayores = r.grupos().get(6);
        assertThat(mayores.grupo()).isEqualTo("60 años a más");   // nacido en 1990, examinado en 2093+
        assertThat(mayores.pacientes()).isEqualTo(1);
        assertThat(mayores.cpod()).isEqualTo(1.0);
        assertThat(mayores.c()).isEqualTo(1.0);
        assertThat(mayores.ceod()).as("ceo-d no aplica en adultos").isNull();
        assertThat(mayores.nivelCpod()).isEqualTo("Muy bajo");
        assertThat(r.grupos().get(0).cpod()).as("grupo sin pacientes").isNull();

        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(reporteService.excelEpidemiologico(desde, hasta)))) {
            Sheet hoja = libro.getSheet("Epidemiológico");
            assertThat(hoja.getRow(4).getCell(2).getStringCellValue()).isEqualTo("CPO-D");
            assertThat(hoja.getRow(11).getCell(2).getNumericCellValue()).isEqualTo(1.0);
            assertThat(hoja.getRow(5).getCell(2).getStringCellValue()).isEqualTo("—");
        }
    }

    // ------------------------------------------------------------------ Excel

    @Test
    void excelDeIngresos_seLeeConPoi() throws IOException {
        Escenario e = escenario();
        iniciarSesion("recepcion");
        byte[] xlsx = reporteService.excelIngresos(LocalDate.of(e.anio(), 3, 1), LocalDate.of(e.anio(), 3, 31));
        assertThat(xlsx).startsWith(0x50, 0x4B);   // ZIP: "PK"
        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            assertThat(libro.getSheetAt(0).getSheetName()).isEqualTo("Ingresos por día");
            assertThat(libro.getSheetAt(1).getSheetName()).isEqualTo("Detalle de pagos");
            Sheet dias = libro.getSheetAt(0);
            assertThat(dias.getRow(1).getCell(0).getStringCellValue()).contains("ingresos");
            Row cabecera = dias.getRow(4);
            assertThat(cabecera.getCell(0).getStringCellValue()).isEqualTo("Fecha");
            assertThat(cabecera.getCell(7).getStringCellValue()).isEqualTo("Total");
            assertThat(dias.getRow(5).getCell(0).getLocalDateTimeCellValue().toLocalDate())
                    .isEqualTo(LocalDate.of(e.anio(), 3, 5));
            Row total = dias.getRow(7);
            assertThat(total.getCell(0).getStringCellValue()).isEqualTo("Total");
            assertThat(total.getCell(7).getCellType()).isEqualTo(CellType.FORMULA);
            assertThat(total.getCell(7).getCellFormula()).isEqualTo("SUM(H6:H7)");
            assertThat(total.getCell(7).getNumericCellValue()).as("valor calculado guardado").isEqualTo(200.0);
            assertThat(dias.getPaneInformation().isFreezePane()).isTrue();

            Sheet detalle = libro.getSheetAt(1);
            assertThat(detalle.getRow(5).getCell(1).getStringCellValue()).startsWith("R-");
            assertThat(detalle.getRow(7).getCell(6).getNumericCellValue()).isEqualTo(200.0);
        }
        iniciarSesion("recepcion");
        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(reporteService.excelMorosidad()))) {
            assertThat(libro.getSheet("Morosidad").getRow(4).getCell(6).getStringCellValue()).startsWith("Saldo vencido");
        }
    }

    // ------------------------------------------------------------------ tablero de inicio

    @Test
    void tablero_recepcionVeIngresos() {
        Escenario e = escenario();
        iniciarSesion("recepcion");
        DashboardDto d = dashboardService.cargar(LocalDate.of(e.anio(), 3, 25));
        assertThat(d.esProduccion()).isFalse();
        assertThat(d.montoMes()).isEqualByComparingTo("200");
        assertThat(d.pacientesNuevosMes()).isEqualTo(1);
        assertThat(d.citasHoy()).isZero();
        assertThat(d.agendaHoy()).isEmpty();
        assertThat(d.ultimosMeses()).hasSize(6);
        assertThat(d.ultimosMeses().get(5).monto()).isEqualByComparingTo("200");
        assertThat(d.ultimosMeses().get(0).monto()).isEqualByComparingTo("0");
        assertThat(d.ultimosMeses().get(5).etiqueta()).endsWith(String.valueOf(e.anio()));
        assertThat(d.cuotasVencidas()).as("la segunda cuota del escenario ya venció en " + e.anio()).isPositive();
        assertThat(d.saldoVencido()).isPositive();
        assertThat(d.morosos()).hasSizeLessThanOrEqualTo(DashboardService.MOROSOS_TABLERO);
    }

    @Test
    void tablero_odontologoVeSuProduccion() {
        Escenario e = escenario();
        iniciarSesion("odontologo");
        DashboardDto d = dashboardService.cargar(LocalDate.of(e.anio(), 3, 25));
        assertThat(d.esProduccion()).isTrue();
        assertThat(d.montoMes()).isEqualByComparingTo("300");
    }

    @Test
    void tablero_citasDeHoy() {
        iniciarSesion("recepcion");
        DashboardDto d = dashboardService.cargar();
        assertThat(d.citasHoy()).isEqualTo(d.agendaHoy().size());
        assertThat(d.agendaHoy()).allMatch(c -> c.inicio().toLocalDate().equals(LocalDate.now()));
    }

    @Test
    void serieMensual_rellenaLosMesesSinMovimiento() {
        List<MontoPeriodoDto> serie = DashboardService.serieMensual(
                List.of(new DashboardDao.MontoMes("2026-08", 1234), new DashboardDao.MontoMes("2026-10", 500)),
                YearMonth.of(2026, 10));
        assertThat(serie).extracting(MontoPeriodoDto::monto).map(BigDecimal::toPlainString)
                .containsExactly("0.00", "0.00", "0.00", "12.34", "0.00", "5.00");
        assertThat(serie.get(0).etiqueta()).startsWith("may");
    }
}
