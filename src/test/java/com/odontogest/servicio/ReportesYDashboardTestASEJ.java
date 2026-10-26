package com.odontogest.servicio;

import com.odontogest.PruebaIntegracionASEJ;
import com.odontogest.dominio.finanzas.FrecuenciaASEJ;
import com.odontogest.dominio.odontograma.SuperficieASEJ;
import com.odontogest.excepcion.NegocioExceptionASEJ;
import com.odontogest.excepcion.PermisoDenegadoExceptionASEJ;
import com.odontogest.modelo.MedioPagoASEJ;
import com.odontogest.modelo.SexoASEJ;
import com.odontogest.modelo.TipoDocumentoASEJ;
import com.odontogest.persistencia.dao.DashboardDaoASEJ;
import com.odontogest.servicio.dto.AtencionFormASEJ;
import com.odontogest.servicio.dto.CronogramaFormASEJ;
import com.odontogest.servicio.dto.DashboardDtoASEJ;
import com.odontogest.servicio.dto.GrupoIndicesDtoASEJ;
import com.odontogest.servicio.dto.HallazgoTipoDtoASEJ;
import com.odontogest.servicio.dto.IngresoDiaDtoASEJ;
import com.odontogest.servicio.dto.MontoPeriodoDtoASEJ;
import com.odontogest.servicio.dto.MorosoDtoASEJ;
import com.odontogest.servicio.dto.NuevoHallazgoASEJ;
import com.odontogest.servicio.dto.PacienteFormASEJ;
import com.odontogest.servicio.dto.PagoFormASEJ;
import com.odontogest.servicio.dto.PlanItemFormASEJ;
import com.odontogest.servicio.dto.ProcedimientoDtoASEJ;
import com.odontogest.servicio.dto.ProduccionFilaDtoASEJ;
import com.odontogest.servicio.dto.ReporteEpidemiologicoDtoASEJ;
import com.odontogest.servicio.dto.ReporteIngresosDtoASEJ;
import com.odontogest.servicio.dto.ReporteProduccionDtoASEJ;
import com.odontogest.servicio.dto.TipoReporteASEJ;
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

class ReportesYDashboardTestASEJ extends PruebaIntegracionASEJ {

    private static final AtomicInteger ANIOASEJ = new AtomicInteger(2093);

    @Autowired private PacienteServiceASEJ pacienteServiceASEJ;
    @Autowired private PlanTratamientoServiceASEJ planServiceASEJ;
    @Autowired private ProcedimientoServiceASEJ procedimientoServiceASEJ;
    @Autowired private AtencionServiceASEJ atencionServiceASEJ;
    @Autowired private OdontogramaServiceASEJ odontogramaServiceASEJ;
    @Autowired private CatalogoHallazgoServiceASEJ catalogoServiceASEJ;
    @Autowired private PagoServiceASEJ pagoServiceASEJ;
    @Autowired private ReporteServiceASEJ reporteServiceASEJ;
    @Autowired private DashboardServiceASEJ dashboardServiceASEJ;
    @Autowired private JdbcTemplate jdbcASEJ;

    private record EscenarioASEJ(int anioASEJ, long pacienteIdASEJ, String historiaASEJ, long planItemIdASEJ) {
    }

    private EscenarioASEJ escenarioASEJ() {
        int anioASEJ = ANIOASEJ.getAndIncrement();
        iniciarSesionASEJ("odontologo");
        long pacienteASEJ = pacienteServiceASEJ.guardarASEJ(new PacienteFormASEJ(null, TipoDocumentoASEJ.DNI, dniAleatorioASEJ(), "Paciente",
                "Reporte" + sufijoASEJ(), null, LocalDate.of(1990, 5, 10), SexoASEJ.M, "987654321", null, null, null, null,
                null, null, null, null, true));
        long atencionASEJ = atencionServiceASEJ.abrirASEJ(new AtencionFormASEJ(null, pacienteASEJ, null, "Control", null, null, null, null));
        long odontogramaASEJ = odontogramaServiceASEJ.obtenerVistaASEJ(pacienteASEJ, null).odontogramaIdASEJ();
        long cariesASEJ = catalogoServiceASEJ.listarTiposASEJ(true).stream().filter(tASEJ -> tASEJ.codigoASEJ().equals("CARIES"))
                .map(HallazgoTipoDtoASEJ::idASEJ).findFirst().orElseThrow();
        odontogramaServiceASEJ.registrarHallazgoASEJ(new NuevoHallazgoASEJ(odontogramaASEJ, cariesASEJ, 36, null, Set.of(SuperficieASEJ.O),
                null, "cd", null));
        odontogramaServiceASEJ.cerrarASEJ(odontogramaASEJ);

        long profASEJ = procedimientoServiceASEJ.listarASEJ(false).stream().filter(pASEJ -> pASEJ.codigoASEJ().equals("PROF"))
                .map(ProcedimientoDtoASEJ::idASEJ).findFirst().orElseThrow();
        long planASEJ = planServiceASEJ.crearASEJ(pacienteASEJ, null, false);
        long itemASEJ = planServiceASEJ.guardarItemASEJ(new PlanItemFormASEJ(null, planASEJ, profASEJ, null, null, 4, 1, new BigDecimal("300"),
                null)).itemsASEJ().get(0).idASEJ();
        planServiceASEJ.cambiarEstadoASEJ(planASEJ, com.odontogest.modelo.EstadoPlanASEJ.PROPUESTO);
        planServiceASEJ.generarCronogramaASEJ(new CronogramaFormASEJ(planASEJ, new BigDecimal("100"), 2, LocalDate.now().plusMonths(1),
                FrecuenciaASEJ.MENSUAL));
        planServiceASEJ.aceptarASEJ(planASEJ, "Paciente de prueba");
        planServiceASEJ.marcarEnAtencionASEJ(atencionASEJ, itemASEJ, true);

        iniciarSesionASEJ("recepcion");
        long efectivoASEJ = pagoServiceASEJ.registrarASEJ(new PagoFormASEJ(pacienteASEJ, new BigDecimal("150"), MedioPagoASEJ.EFECTIVO, null)).idASEJ();
        long yapeASEJ = pagoServiceASEJ.registrarASEJ(new PagoFormASEJ(pacienteASEJ, new BigDecimal("50"), MedioPagoASEJ.YAPE, "OP-1")).idASEJ();

        jdbcASEJ.update("UPDATE paciente SET creado_en = ? WHERE id = ?", anioASEJ + "-03-02T08:00:00", pacienteASEJ);
        jdbcASEJ.update("UPDATE odontograma SET cerrado_en = ? WHERE id = ?", anioASEJ + "-02-01T09:00:00", odontogramaASEJ);
        jdbcASEJ.update("UPDATE plan_item SET realizado_en = ? WHERE id = ?", anioASEJ + "-03-10T09:30:00", itemASEJ);
        jdbcASEJ.update("UPDATE pago SET fecha = ? WHERE id = ?", anioASEJ + "-03-05T10:00:00", efectivoASEJ);
        jdbcASEJ.update("UPDATE pago SET fecha = ? WHERE id = ?", anioASEJ + "-03-20T16:30:00", yapeASEJ);
        String historiaASEJ = pacienteServiceASEJ.obtenerASEJ(pacienteASEJ).numeroHistoriaASEJ();
        return new EscenarioASEJ(anioASEJ, pacienteASEJ, historiaASEJ, itemASEJ);
    }

    @Test
    void ingresosPorDiaYMedioDePagoASEJ() {
        EscenarioASEJ eASEJ = escenarioASEJ();
        iniciarSesionASEJ("recepcion");
        ReporteIngresosDtoASEJ rASEJ = reporteServiceASEJ.ingresosASEJ(LocalDate.of(eASEJ.anioASEJ(), 3, 1), LocalDate.of(eASEJ.anioASEJ(), 3, 31));

        assertThat(rASEJ.totalASEJ()).isEqualByComparingTo("200.00");
        assertThat(rASEJ.pagosASEJ()).isEqualTo(2);
        assertThat(rASEJ.totalesPorMedioASEJ().get(MedioPagoASEJ.EFECTIVO)).isEqualByComparingTo("150");
        assertThat(rASEJ.totalesPorMedioASEJ().get(MedioPagoASEJ.YAPE)).isEqualByComparingTo("50");
        assertThat(rASEJ.totalesPorMedioASEJ().get(MedioPagoASEJ.TARJETA)).isEqualByComparingTo("0");
        assertThat(rASEJ.diasASEJ()).extracting(IngresoDiaDtoASEJ::diaASEJ)
                .containsExactly(LocalDate.of(eASEJ.anioASEJ(), 3, 5), LocalDate.of(eASEJ.anioASEJ(), 3, 20));
        assertThat(rASEJ.diasASEJ().get(1).montoASEJ(MedioPagoASEJ.YAPE)).isEqualByComparingTo("50");
        assertThat(rASEJ.serieASEJ()).as("rango corto: una barra por día, incluidos los días sin pagos").hasSize(31);
        assertThat(rASEJ.serieASEJ().get(4)).isEqualTo(new MontoPeriodoDtoASEJ("05/03", new BigDecimal("150.00")));
        assertThat(rASEJ.detalleASEJ()).hasSize(2).allMatch(pASEJ -> pASEJ.numeroHistoriaASEJ().equals(eASEJ.historiaASEJ()));
        assertThat(rASEJ.detalleASEJ().get(1).referenciaASEJ()).isEqualTo("OP-1");
        assertThat(rASEJ.detalleASEJ().get(0).pacienteASEJ()).startsWith("Reporte");
    }

    @Test
    void ingresosAnulados_noSeCuentanASEJ() {
        EscenarioASEJ eASEJ = escenarioASEJ();
        long efectivoASEJ = jdbcASEJ.queryForObject("SELECT id FROM pago WHERE paciente_id = ? AND medio = 'EFECTIVO'",
                Long.class, eASEJ.pacienteIdASEJ());
        iniciarSesionASEJ("admin");
        pagoServiceASEJ.anularASEJ(efectivoASEJ, "Error de digitación en la prueba");
        ReporteIngresosDtoASEJ rASEJ = reporteServiceASEJ.ingresosASEJ(LocalDate.of(eASEJ.anioASEJ(), 1, 1), LocalDate.of(eASEJ.anioASEJ(), 12, 31));
        assertThat(rASEJ.totalASEJ()).isEqualByComparingTo("50");
        assertThat(rASEJ.serieASEJ()).as("rango largo: una barra por mes").hasSize(12);
        assertThat(rASEJ.serieASEJ().get(2).montoASEJ()).isEqualByComparingTo("50");
    }

    @Test
    void rangosInvalidosASEJ() {
        iniciarSesionASEJ("admin");
        LocalDate hoyASEJ = LocalDate.now();
        assertThatThrownBy(() -> reporteServiceASEJ.ingresosASEJ(hoyASEJ, hoyASEJ.minusDays(1)))
                .isInstanceOf(NegocioExceptionASEJ.class).hasMessageContaining("anterior");
        assertThatThrownBy(() -> reporteServiceASEJ.ingresosASEJ(null, hoyASEJ)).isInstanceOf(NegocioExceptionASEJ.class);
        assertThatThrownBy(() -> reporteServiceASEJ.produccionASEJ(hoyASEJ.minusYears(4), hoyASEJ))
                .isInstanceOf(NegocioExceptionASEJ.class).hasMessageContaining("3 años");
    }

    @Test
    void cadaRolVeSusReportesASEJ() {
        iniciarSesionASEJ("admin");
        assertThat(reporteServiceASEJ.disponiblesASEJ()).containsExactly(TipoReporteASEJ.values());
        iniciarSesionASEJ("odontologo");
        assertThat(reporteServiceASEJ.disponiblesASEJ()).containsExactly(TipoReporteASEJ.PRODUCCION, TipoReporteASEJ.EPIDEMIOLOGICO);
        LocalDate hoyASEJ = LocalDate.now();
        assertThatThrownBy(() -> reporteServiceASEJ.ingresosASEJ(hoyASEJ, hoyASEJ)).isInstanceOf(PermisoDenegadoExceptionASEJ.class);
        assertThatThrownBy(() -> reporteServiceASEJ.morosidadASEJ()).isInstanceOf(PermisoDenegadoExceptionASEJ.class);
        iniciarSesionASEJ("recepcion");
        assertThat(reporteServiceASEJ.disponiblesASEJ()).containsExactly(TipoReporteASEJ.INGRESOS, TipoReporteASEJ.MOROSIDAD);
        assertThatThrownBy(() -> reporteServiceASEJ.produccionASEJ(hoyASEJ, hoyASEJ)).isInstanceOf(PermisoDenegadoExceptionASEJ.class);
        assertThatThrownBy(() -> reporteServiceASEJ.epidemiologicoASEJ(hoyASEJ, hoyASEJ)).isInstanceOf(PermisoDenegadoExceptionASEJ.class);
        assertThatThrownBy(() -> reporteServiceASEJ.excelAuditoriaASEJ(hoyASEJ, hoyASEJ, null)).isInstanceOf(PermisoDenegadoExceptionASEJ.class);
    }

    @Test
    void produccion_odontologoVeLaSuya_adminLaDeTodosASEJ() {
        EscenarioASEJ eASEJ = escenarioASEJ();
        LocalDate desdeASEJ = LocalDate.of(eASEJ.anioASEJ(), 3, 1);
        LocalDate hastaASEJ = LocalDate.of(eASEJ.anioASEJ(), 3, 31);
        iniciarSesionASEJ("odontologo");
        ReporteProduccionDtoASEJ propiaASEJ = reporteServiceASEJ.produccionASEJ(desdeASEJ, hastaASEJ);
        assertThat(propiaASEJ.filasASEJ()).extracting(ProduccionFilaDtoASEJ::codigoASEJ, ProduccionFilaDtoASEJ::cantidadASEJ)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("PROF", 1));
        assertThat(propiaASEJ.totalASEJ()).isEqualByComparingTo("300");
        assertThat(propiaASEJ.porOdontologoASEJ()).hasSize(1);

        Long otroASEJ = jdbcASEJ.queryForObject("SELECT MIN(id) FROM odontologo WHERE id <> ?", Long.class,
                iniciarSesionASEJ("odontologo").odontologoIdASEJ());
        if (otroASEJ != null) {
            jdbcASEJ.update("UPDATE atencion SET odontologo_id = ? WHERE id = (SELECT atencion_id FROM plan_item WHERE id = ?)",
                    otroASEJ, eASEJ.planItemIdASEJ());
            assertThat(reporteServiceASEJ.produccionASEJ(desdeASEJ, hastaASEJ).filasASEJ()).isEmpty();
            iniciarSesionASEJ("admin");
            assertThat(reporteServiceASEJ.produccionASEJ(desdeASEJ, hastaASEJ).filasASEJ()).extracting(ProduccionFilaDtoASEJ::odontologoIdASEJ)
                    .containsExactly(otroASEJ);
        }
    }

    @Test
    void morosidadDelPacienteASEJ() {
        EscenarioASEJ eASEJ = escenarioASEJ();
        iniciarSesionASEJ("recepcion");
        LocalDate hoyASEJ = LocalDate.now().plusYears(1);
        MorosoDtoASEJ mASEJ = reporteServiceASEJ.morosidadASEJ(hoyASEJ).pacientesASEJ().stream()
                .filter(pASEJ -> pASEJ.pacienteIdASEJ() == eASEJ.pacienteIdASEJ()).findFirst().orElseThrow();
        assertThat(mASEJ.cuotasVencidasASEJ()).as("la inicial y la primera cuota están pagadas").isEqualTo(1);
        assertThat(mASEJ.saldoVencidoASEJ()).isEqualByComparingTo("100");
        assertThat(mASEJ.vencimientoMasAntiguoASEJ()).isEqualTo(LocalDate.now().plusMonths(2));
        assertThat(mASEJ.diasAtrasoASEJ()).isPositive();
        assertThat(mASEJ.telefonoASEJ()).isEqualTo("987654321");
        assertThat(reporteServiceASEJ.morosidadASEJ(LocalDate.now()).pacientesASEJ())
                .noneMatch(pASEJ -> pASEJ.pacienteIdASEJ() == eASEJ.pacienteIdASEJ());
    }

    @Test
    void epidemiologicoPorGrupoDeEdadASEJ() throws IOException {
        EscenarioASEJ eASEJ = escenarioASEJ();
        iniciarSesionASEJ("odontologo");
        LocalDate desdeASEJ = LocalDate.of(eASEJ.anioASEJ(), 1, 1);
        LocalDate hastaASEJ = LocalDate.of(eASEJ.anioASEJ(), 12, 31);
        ReporteEpidemiologicoDtoASEJ rASEJ = reporteServiceASEJ.epidemiologicoASEJ(desdeASEJ, hastaASEJ);
        assertThat(rASEJ.pacientesASEJ()).isEqualTo(1);
        assertThat(rASEJ.gruposASEJ()).hasSize(7);
        GrupoIndicesDtoASEJ mayoresASEJ = rASEJ.gruposASEJ().get(6);
        assertThat(mayoresASEJ.grupoASEJ()).isEqualTo("60 años a más");
        assertThat(mayoresASEJ.pacientesASEJ()).isEqualTo(1);
        assertThat(mayoresASEJ.cpodASEJ()).isEqualTo(1.0);
        assertThat(mayoresASEJ.cASEJ()).isEqualTo(1.0);
        assertThat(mayoresASEJ.ceodASEJ()).as("ceo-d no aplica en adultos").isNull();
        assertThat(mayoresASEJ.nivelCpodASEJ()).isEqualTo("Muy bajo");
        assertThat(rASEJ.gruposASEJ().get(0).cpodASEJ()).as("grupo sin pacientes").isNull();

        try (XSSFWorkbook libroASEJ = new XSSFWorkbook(new ByteArrayInputStream(reporteServiceASEJ.excelEpidemiologicoASEJ(desdeASEJ, hastaASEJ)))) {
            Sheet hojaASEJ = libroASEJ.getSheet("Epidemiológico");
            assertThat(hojaASEJ.getRow(4).getCell(2).getStringCellValue()).isEqualTo("CPO-D");
            assertThat(hojaASEJ.getRow(11).getCell(2).getNumericCellValue()).isEqualTo(1.0);
            assertThat(hojaASEJ.getRow(5).getCell(2).getStringCellValue()).isEqualTo("—");
        }
    }

    @Test
    void excelDeIngresos_seLeeConPoiASEJ() throws IOException {
        EscenarioASEJ eASEJ = escenarioASEJ();
        iniciarSesionASEJ("recepcion");
        byte[] xlsxASEJ = reporteServiceASEJ.excelIngresosASEJ(LocalDate.of(eASEJ.anioASEJ(), 3, 1), LocalDate.of(eASEJ.anioASEJ(), 3, 31));
        assertThat(xlsxASEJ).startsWith(0x50, 0x4B);
        try (XSSFWorkbook libroASEJ = new XSSFWorkbook(new ByteArrayInputStream(xlsxASEJ))) {
            assertThat(libroASEJ.getSheetAt(0).getSheetName()).isEqualTo("Ingresos por día");
            assertThat(libroASEJ.getSheetAt(1).getSheetName()).isEqualTo("Detalle de pagos");
            Sheet diasASEJ = libroASEJ.getSheetAt(0);
            assertThat(diasASEJ.getRow(1).getCell(0).getStringCellValue()).contains("ingresos");
            Row cabeceraASEJ = diasASEJ.getRow(4);
            assertThat(cabeceraASEJ.getCell(0).getStringCellValue()).isEqualTo("Fecha");
            assertThat(cabeceraASEJ.getCell(7).getStringCellValue()).isEqualTo("Total");
            assertThat(diasASEJ.getRow(5).getCell(0).getLocalDateTimeCellValue().toLocalDate())
                    .isEqualTo(LocalDate.of(eASEJ.anioASEJ(), 3, 5));
            Row totalASEJ = diasASEJ.getRow(7);
            assertThat(totalASEJ.getCell(0).getStringCellValue()).isEqualTo("Total");
            assertThat(totalASEJ.getCell(7).getCellType()).isEqualTo(CellType.FORMULA);
            assertThat(totalASEJ.getCell(7).getCellFormula()).isEqualTo("SUM(H6:H7)");
            assertThat(totalASEJ.getCell(7).getNumericCellValue()).as("valor calculado guardado").isEqualTo(200.0);
            assertThat(diasASEJ.getPaneInformation().isFreezePane()).isTrue();

            Sheet detalleASEJ = libroASEJ.getSheetAt(1);
            assertThat(detalleASEJ.getRow(5).getCell(1).getStringCellValue()).startsWith("R-");
            assertThat(detalleASEJ.getRow(7).getCell(6).getNumericCellValue()).isEqualTo(200.0);
        }
        iniciarSesionASEJ("recepcion");
        try (XSSFWorkbook libroASEJ = new XSSFWorkbook(new ByteArrayInputStream(reporteServiceASEJ.excelMorosidadASEJ()))) {
            assertThat(libroASEJ.getSheet("Morosidad").getRow(4).getCell(6).getStringCellValue()).startsWith("Saldo vencido");
        }
    }

    @Test
    void tablero_recepcionVeIngresosASEJ() {
        EscenarioASEJ eASEJ = escenarioASEJ();
        iniciarSesionASEJ("recepcion");
        DashboardDtoASEJ dASEJ = dashboardServiceASEJ.cargarASEJ(LocalDate.of(eASEJ.anioASEJ(), 3, 25));
        assertThat(dASEJ.esProduccionASEJ()).isFalse();
        assertThat(dASEJ.montoMesASEJ()).isEqualByComparingTo("200");
        assertThat(dASEJ.pacientesNuevosMesASEJ()).isEqualTo(1);
        assertThat(dASEJ.citasHoyASEJ()).isZero();
        assertThat(dASEJ.agendaHoyASEJ()).isEmpty();
        assertThat(dASEJ.ultimosMesesASEJ()).hasSize(6);
        assertThat(dASEJ.ultimosMesesASEJ().get(5).montoASEJ()).isEqualByComparingTo("200");
        assertThat(dASEJ.ultimosMesesASEJ().get(0).montoASEJ()).isEqualByComparingTo("0");
        assertThat(dASEJ.ultimosMesesASEJ().get(5).etiquetaASEJ()).endsWith(String.valueOf(eASEJ.anioASEJ()));
        assertThat(dASEJ.cuotasVencidasASEJ()).as("la segunda cuota del escenario ya venció en " + eASEJ.anioASEJ()).isPositive();
        assertThat(dASEJ.saldoVencidoASEJ()).isPositive();
        assertThat(dASEJ.morososASEJ()).hasSizeLessThanOrEqualTo(DashboardServiceASEJ.MOROSOS_TABLEROASEJ);
    }

    @Test
    void tablero_odontologoVeSuProduccionASEJ() {
        EscenarioASEJ eASEJ = escenarioASEJ();
        iniciarSesionASEJ("odontologo");
        DashboardDtoASEJ dASEJ = dashboardServiceASEJ.cargarASEJ(LocalDate.of(eASEJ.anioASEJ(), 3, 25));
        assertThat(dASEJ.esProduccionASEJ()).isTrue();
        assertThat(dASEJ.montoMesASEJ()).isEqualByComparingTo("300");
    }

    @Test
    void tablero_citasDeHoyASEJ() {
        iniciarSesionASEJ("recepcion");
        DashboardDtoASEJ dASEJ = dashboardServiceASEJ.cargarASEJ();
        assertThat(dASEJ.citasHoyASEJ()).isEqualTo(dASEJ.agendaHoyASEJ().size());
        assertThat(dASEJ.agendaHoyASEJ()).allMatch(cASEJ -> cASEJ.inicioASEJ().toLocalDate().equals(LocalDate.now()));
    }

    @Test
    void serieMensual_rellenaLosMesesSinMovimientoASEJ() {
        List<MontoPeriodoDtoASEJ> serieASEJ = DashboardServiceASEJ.serieMensualASEJ(
                List.of(new DashboardDaoASEJ.MontoMesASEJ("2026-08", 1234), new DashboardDaoASEJ.MontoMesASEJ("2026-10", 500)),
                YearMonth.of(2026, 10));
        assertThat(serieASEJ).extracting(MontoPeriodoDtoASEJ::montoASEJ).map(BigDecimal::toPlainString)
                .containsExactly("0.00", "0.00", "0.00", "12.34", "0.00", "5.00");
        assertThat(serieASEJ.get(0).etiquetaASEJ()).startsWith("may");
    }
}
