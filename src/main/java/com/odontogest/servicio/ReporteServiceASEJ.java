package com.odontogest.servicio;

import com.odontogest.documento.ReporteExcelASEJ;
import com.odontogest.dominio.odontograma.PromedioIndicesASEJ;
import com.odontogest.excepcion.NegocioExceptionASEJ;
import com.odontogest.excepcion.PermisoDenegadoExceptionASEJ;
import com.odontogest.modelo.MedioPagoASEJ;
import com.odontogest.modelo.RolASEJ;
import com.odontogest.persistencia.conversor.DineroConverterASEJ;
import com.odontogest.persistencia.conversor.LocalDateTimeConverterASEJ;
import com.odontogest.persistencia.dao.ReporteDaoASEJ;
import com.odontogest.seguridad.PermisoASEJ;
import com.odontogest.seguridad.SesionUsuarioASEJ;
import com.odontogest.servicio.dto.GrupoIndicesDtoASEJ;
import com.odontogest.servicio.dto.IndicesVistaASEJ;
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
import com.odontogest.servicio.dto.UsuarioSesionASEJ;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReporteServiceASEJ {

    static final int DIAS_MAXIMOSASEJ = 3 * 366;
    static final int DIAS_GRAFICO_DIARIOASEJ = 31;
    private static final Locale PERUASEJ = Locale.of("es", "PE");
    private static final DateTimeFormatter DIAASEJ = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter MESASEJ = DateTimeFormatter.ofPattern("MMM yyyy", PERUASEJ);

    private static final Map<RolASEJ, Set<TipoReporteASEJ>> POR_ROLASEJ = Map.of(
            RolASEJ.ADMIN, EnumSet.allOf(TipoReporteASEJ.class),
            RolASEJ.ODONTOLOGO, EnumSet.of(TipoReporteASEJ.PRODUCCION, TipoReporteASEJ.EPIDEMIOLOGICO),
            RolASEJ.RECEPCION, EnumSet.of(TipoReporteASEJ.INGRESOS, TipoReporteASEJ.MOROSIDAD));

    private final Clock relojASEJ;
    private final ReporteDaoASEJ reporteDaoASEJ;
    private final OdontogramaServiceASEJ odontogramaServiceASEJ;
    private final ConfiguracionServiceASEJ configuracionServiceASEJ;
    private final ReporteExcelASEJ reporteExcelASEJ;
    private final AuditoriaServiceASEJ auditoriaServiceASEJ;
    private final SesionUsuarioASEJ sesionUsuarioASEJ;

    public Set<TipoReporteASEJ> disponiblesASEJ() {
        UsuarioSesionASEJ uASEJ = sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.REPORTES_VER);
        return EnumSet.copyOf(POR_ROLASEJ.getOrDefault(RolASEJ.valueOf(uASEJ.rolASEJ()), EnumSet.noneOf(TipoReporteASEJ.class)));
    }

    public ReporteIngresosDtoASEJ ingresosASEJ(LocalDate desdeASEJ, LocalDate hastaASEJ) {
        exigirASEJ(TipoReporteASEJ.INGRESOS);
        validarRangoASEJ(desdeASEJ, hastaASEJ);
        LocalDate finASEJ = hastaASEJ.plusDays(1);
        Map<LocalDate, Map<MedioPagoASEJ, BigDecimal>> porDiaASEJ = new TreeMap<>();
        Map<LocalDate, Integer> pagosPorDiaASEJ = new TreeMap<>();
        Map<MedioPagoASEJ, BigDecimal> totalesASEJ = new EnumMap<>(MedioPagoASEJ.class);
        for (MedioPagoASEJ mASEJ : MedioPagoASEJ.values()) {
            totalesASEJ.put(mASEJ, ceroASEJ());
        }
        int pagosASEJ = 0;
        for (ReporteDaoASEJ.IngresoDiaASEJ fASEJ : reporteDaoASEJ.ingresosPorDiaASEJ(desdeASEJ, finASEJ)) {
            LocalDate diaASEJ = LocalDate.parse(fASEJ.diaASEJ());
            MedioPagoASEJ medioASEJ = MedioPagoASEJ.valueOf(fASEJ.medioASEJ());
            BigDecimal montoASEJ = DineroConverterASEJ.desdeCentimosASEJ(fASEJ.montoCentASEJ());
            porDiaASEJ.computeIfAbsent(diaASEJ, dASEJ -> new EnumMap<>(MedioPagoASEJ.class)).merge(medioASEJ, montoASEJ, BigDecimal::add);
            pagosPorDiaASEJ.merge(diaASEJ, fASEJ.pagosASEJ(), Integer::sum);
            totalesASEJ.merge(medioASEJ, montoASEJ, BigDecimal::add);
            pagosASEJ += fASEJ.pagosASEJ();
        }
        List<IngresoDiaDtoASEJ> diasASEJ = porDiaASEJ.entrySet().stream()
                .map(eASEJ -> new IngresoDiaDtoASEJ(eASEJ.getKey(), pagosPorDiaASEJ.get(eASEJ.getKey()), eASEJ.getValue(),
                        sumaASEJ(eASEJ.getValue().values())))
                .toList();
        List<PagoReporteDtoASEJ> detalleASEJ = reporteDaoASEJ.pagosASEJ(desdeASEJ, finASEJ).stream()
                .map(pASEJ -> new PagoReporteDtoASEJ(LocalDateTime.parse(pASEJ.fechaASEJ(), LocalDateTimeConverterASEJ.FORMATOASEJ),
                        pASEJ.reciboASEJ(), pASEJ.historiaASEJ(), pASEJ.pacienteASEJ(), MedioPagoASEJ.valueOf(pASEJ.medioASEJ()), pASEJ.referenciaASEJ(),
                        DineroConverterASEJ.desdeCentimosASEJ(pASEJ.montoCentASEJ())))
                .toList();
        return new ReporteIngresosDtoASEJ(desdeASEJ, hastaASEJ, diasASEJ, totalesASEJ, pagosASEJ, sumaASEJ(totalesASEJ.values()),
                serieIngresosASEJ(diasASEJ, desdeASEJ, hastaASEJ), detalleASEJ);
    }

    public byte[] excelIngresosASEJ(LocalDate desdeASEJ, LocalDate hastaASEJ) {
        return reporteExcelASEJ.ingresosASEJ(ingresosASEJ(desdeASEJ, hastaASEJ), configuracionServiceASEJ.datosClinicaASEJ());
    }

    static List<MontoPeriodoDtoASEJ> serieIngresosASEJ(List<IngresoDiaDtoASEJ> diasASEJ, LocalDate desdeASEJ, LocalDate hastaASEJ) {
        List<MontoPeriodoDtoASEJ> serieASEJ = new ArrayList<>();
        if (ChronoUnit.DAYS.between(desdeASEJ, hastaASEJ) < DIAS_GRAFICO_DIARIOASEJ) {
            Map<LocalDate, BigDecimal> porDiaASEJ = new TreeMap<>();
            diasASEJ.forEach(dASEJ -> porDiaASEJ.put(dASEJ.diaASEJ(), dASEJ.totalASEJ()));
            for (LocalDate dASEJ = desdeASEJ; !dASEJ.isAfter(hastaASEJ); dASEJ = dASEJ.plusDays(1)) {
                serieASEJ.add(new MontoPeriodoDtoASEJ(DIAASEJ.format(dASEJ), porDiaASEJ.getOrDefault(dASEJ, ceroASEJ())));
            }
            return serieASEJ;
        }
        Map<YearMonth, BigDecimal> porMesASEJ = new LinkedHashMap<>();
        for (YearMonth mASEJ = YearMonth.from(desdeASEJ); !mASEJ.isAfter(YearMonth.from(hastaASEJ)); mASEJ = mASEJ.plusMonths(1)) {
            porMesASEJ.put(mASEJ, ceroASEJ());
        }
        diasASEJ.forEach(dASEJ -> porMesASEJ.merge(YearMonth.from(dASEJ.diaASEJ()), dASEJ.totalASEJ(), BigDecimal::add));
        porMesASEJ.forEach((mASEJ, montoASEJ) -> serieASEJ.add(new MontoPeriodoDtoASEJ(MESASEJ.format(mASEJ), montoASEJ)));
        return serieASEJ;
    }

    public ReporteProduccionDtoASEJ produccionASEJ(LocalDate desdeASEJ, LocalDate hastaASEJ) {
        UsuarioSesionASEJ uASEJ = exigirASEJ(TipoReporteASEJ.PRODUCCION);
        validarRangoASEJ(desdeASEJ, hastaASEJ);
        Long odontologoIdASEJ = RolASEJ.ODONTOLOGO.name().equals(uASEJ.rolASEJ()) ? uASEJ.odontologoIdASEJ() : null;
        List<ProduccionFilaDtoASEJ> filasASEJ = reporteDaoASEJ.produccionASEJ(desdeASEJ, hastaASEJ.plusDays(1), odontologoIdASEJ).stream()
                .map(fASEJ -> new ProduccionFilaDtoASEJ(fASEJ.odontologoIdASEJ(), fASEJ.odontologoASEJ(), fASEJ.codigoASEJ(), fASEJ.procedimientoASEJ(),
                        fASEJ.cantidadASEJ(), DineroConverterASEJ.desdeCentimosASEJ(fASEJ.montoCentASEJ())))
                .toList();
        Map<String, BigDecimal> porOdontologoASEJ = new TreeMap<>();
        filasASEJ.forEach(fASEJ -> porOdontologoASEJ.merge(fASEJ.odontologoASEJ(), fASEJ.montoASEJ(), BigDecimal::add));
        return new ReporteProduccionDtoASEJ(desdeASEJ, hastaASEJ, filasASEJ,
                porOdontologoASEJ.entrySet().stream().map(eASEJ -> new MontoPeriodoDtoASEJ(eASEJ.getKey(), eASEJ.getValue())).toList(),
                filasASEJ.stream().mapToInt(ProduccionFilaDtoASEJ::cantidadASEJ).sum(),
                sumaASEJ(filasASEJ.stream().map(ProduccionFilaDtoASEJ::montoASEJ).toList()));
    }

    public byte[] excelProduccionASEJ(LocalDate desdeASEJ, LocalDate hastaASEJ) {
        return reporteExcelASEJ.produccionASEJ(produccionASEJ(desdeASEJ, hastaASEJ), configuracionServiceASEJ.datosClinicaASEJ());
    }

    public ReporteMorosidadDtoASEJ morosidadASEJ() {
        return morosidadASEJ(LocalDate.now(relojASEJ));
    }

    ReporteMorosidadDtoASEJ morosidadASEJ(LocalDate hoyASEJ) {
        exigirASEJ(TipoReporteASEJ.MOROSIDAD);
        List<MorosoDtoASEJ> pacientesASEJ = DashboardServiceASEJ.morososASEJ(reporteDaoASEJ.morosidadASEJ(hoyASEJ, 0), hoyASEJ);
        return new ReporteMorosidadDtoASEJ(hoyASEJ, pacientesASEJ, pacientesASEJ.stream().mapToInt(MorosoDtoASEJ::cuotasVencidasASEJ).sum(),
                sumaASEJ(pacientesASEJ.stream().map(MorosoDtoASEJ::saldoVencidoASEJ).toList()));
    }

    public byte[] excelMorosidadASEJ() {
        return reporteExcelASEJ.morosidadASEJ(morosidadASEJ(), configuracionServiceASEJ.datosClinicaASEJ());
    }

    public ReporteEpidemiologicoDtoASEJ epidemiologicoASEJ(LocalDate desdeASEJ, LocalDate hastaASEJ) {
        exigirASEJ(TipoReporteASEJ.EPIDEMIOLOGICO);
        validarRangoASEJ(desdeASEJ, hastaASEJ);
        PromedioIndicesASEJ promedioASEJ = new PromedioIndicesASEJ();
        for (ReporteDaoASEJ.PacienteExaminadoASEJ pASEJ : reporteDaoASEJ.pacientesExaminadosASEJ(desdeASEJ, hastaASEJ.plusDays(1))) {
            Optional<IndicesVistaASEJ> indicesASEJ = odontogramaServiceASEJ.indicesFirmadosASEJ(pASEJ.pacienteIdASEJ());
            if (indicesASEJ.isEmpty()) {
                continue;
            }
            LocalDate examenASEJ = LocalDateTime.parse(pASEJ.ultimoCierreASEJ(), LocalDateTimeConverterASEJ.FORMATOASEJ).toLocalDate();
            int edadASEJ = Math.max(0, Period.between(LocalDate.parse(pASEJ.fechaNacimientoASEJ()), examenASEJ).getYears());
            promedioASEJ.agregarASEJ(edadASEJ, indicesASEJ.get().cpodASEJ(), indicesASEJ.get().ceodASEJ());
        }
        List<GrupoIndicesDtoASEJ> gruposASEJ = promedioASEJ.resultadoASEJ().stream().map(ReporteServiceASEJ::grupoASEJ).toList();
        return new ReporteEpidemiologicoDtoASEJ(desdeASEJ, hastaASEJ, promedioASEJ.pacientesASEJ(), gruposASEJ,
                configuracionServiceASEJ.booleanoASEJ("odontograma.excluir_terceros_molares_cpod"));
    }

    public byte[] excelEpidemiologicoASEJ(LocalDate desdeASEJ, LocalDate hastaASEJ) {
        return reporteExcelASEJ.epidemiologicoASEJ(epidemiologicoASEJ(desdeASEJ, hastaASEJ), configuracionServiceASEJ.datosClinicaASEJ());
    }

    private static GrupoIndicesDtoASEJ grupoASEJ(PromedioIndicesASEJ.GrupoASEJ gASEJ) {
        boolean hayASEJ = gASEJ.pacientesASEJ() > 0;
        boolean cpodASEJ = hayASEJ && gASEJ.grupoASEJ().aplicaCpodASEJ;
        return new GrupoIndicesDtoASEJ(gASEJ.grupoASEJ().etiquetaASEJ, gASEJ.pacientesASEJ(), cpodASEJ ? gASEJ.cpodASEJ() : null, cpodASEJ ? gASEJ.cASEJ() : null,
                cpodASEJ ? gASEJ.pASEJ() : null, cpodASEJ ? gASEJ.oASEJ() : null, hayASEJ && gASEJ.grupoASEJ().aplicaCeodASEJ ? gASEJ.ceodASEJ() : null,
                cpodASEJ ? etiquetaASEJ(gASEJ.nivelCpodASEJ().name()) : null);
    }

    private static String etiquetaASEJ(String nombreASEJ) {
        String tASEJ = nombreASEJ.replace('_', ' ').toLowerCase(PERUASEJ);
        return Character.toUpperCase(tASEJ.charAt(0)) + tASEJ.substring(1);
    }

    public byte[] excelAuditoriaASEJ(LocalDate desdeASEJ, LocalDate hastaASEJ, String accionASEJ) {
        exigirASEJ(TipoReporteASEJ.AUDITORIA);
        return reporteExcelASEJ.auditoriaASEJ(auditoriaServiceASEJ.buscarASEJ(desdeASEJ, hastaASEJ, accionASEJ), desdeASEJ, hastaASEJ,
                configuracionServiceASEJ.datosClinicaASEJ());
    }

    private UsuarioSesionASEJ exigirASEJ(TipoReporteASEJ tipoASEJ) {
        UsuarioSesionASEJ uASEJ = sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.REPORTES_VER);
        if (!POR_ROLASEJ.getOrDefault(RolASEJ.valueOf(uASEJ.rolASEJ()), Set.of()).contains(tipoASEJ)) {
            throw new PermisoDenegadoExceptionASEJ("No tiene permiso para ver el reporte de " + tipoASEJ.etiquetaASEJ.toLowerCase(PERUASEJ) + ".");
        }
        return uASEJ;
    }

    static void validarRangoASEJ(LocalDate desdeASEJ, LocalDate hastaASEJ) {
        if (desdeASEJ == null || hastaASEJ == null) {
            throw new NegocioExceptionASEJ("Indique la fecha inicial y la final del reporte.");
        }
        if (hastaASEJ.isBefore(desdeASEJ)) {
            throw new NegocioExceptionASEJ("La fecha final no puede ser anterior a la inicial.");
        }
        if (ChronoUnit.DAYS.between(desdeASEJ, hastaASEJ) >= DIAS_MAXIMOSASEJ) {
            throw new NegocioExceptionASEJ("El rango del reporte no puede pasar de 3 años.");
        }
    }

    private static BigDecimal ceroASEJ() {
        return BigDecimal.ZERO.setScale(2);
    }

    private static BigDecimal sumaASEJ(Collection<BigDecimal> montosASEJ) {
        return montosASEJ.stream().reduce(ceroASEJ(), BigDecimal::add);
    }
}
