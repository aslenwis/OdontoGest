/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio;

import com.odontogest.documento.ReporteExcel;
import com.odontogest.dominio.odontograma.PromedioIndices;
import com.odontogest.excepcion.NegocioException;
import com.odontogest.excepcion.PermisoDenegadoException;
import com.odontogest.modelo.MedioPago;
import com.odontogest.modelo.Rol;
import com.odontogest.persistencia.conversor.DineroConverter;
import com.odontogest.persistencia.conversor.LocalDateTimeConverter;
import com.odontogest.persistencia.dao.ReporteDao;
import com.odontogest.seguridad.Permiso;
import com.odontogest.seguridad.SesionUsuario;
import com.odontogest.servicio.dto.GrupoIndicesDto;
import com.odontogest.servicio.dto.IndicesVista;
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
import com.odontogest.servicio.dto.UsuarioSesion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

/**
 * Reportes (RF-REP-02 a RF-REP-05) y su exportación a Excel. Qué reporte ve cada rol (PLAN §3, permiso
 * REPORTES_VER):
 * <ul>
 *   <li>Administrador: todos, incluida la auditoría.</li>
 *   <li>Odontólogo: su propia producción y el epidemiológico.</li>
 *   <li>Recepción: lo de caja (ingresos y morosidad).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReporteService {

    /** Rango máximo de un reporte, para no cargar años de pagos por error. */
    static final int DIAS_MAXIMOS = 3 * 366;
    /** Hasta este largo de rango el gráfico de ingresos va por día; si no, por mes. */
    static final int DIAS_GRAFICO_DIARIO = 31;
    private static final Locale PERU = Locale.of("es", "PE");
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MMM yyyy", PERU);

    private static final Map<Rol, Set<TipoReporte>> POR_ROL = Map.of(
            Rol.ADMIN, EnumSet.allOf(TipoReporte.class),
            Rol.ODONTOLOGO, EnumSet.of(TipoReporte.PRODUCCION, TipoReporte.EPIDEMIOLOGICO),
            Rol.RECEPCION, EnumSet.of(TipoReporte.INGRESOS, TipoReporte.MOROSIDAD));

    private final ReporteDao reporteDao;
    private final OdontogramaService odontogramaService;
    private final ConfiguracionService configuracionService;
    private final ReporteExcel reporteExcel;
    private final AuditoriaService auditoriaService;
    private final SesionUsuario sesionUsuario;

    /** Reportes que el usuario de la sesión puede ver, en el orden de las pestañas. */
    public Set<TipoReporte> disponibles() {
        UsuarioSesion u = sesionUsuario.exigir(Permiso.REPORTES_VER);
        return EnumSet.copyOf(POR_ROL.getOrDefault(Rol.valueOf(u.rol()), EnumSet.noneOf(TipoReporte.class)));
    }

    // ------------------------------------------------------------------ ingresos (RF-REP-02)

    public ReporteIngresosDto ingresos(LocalDate desde, LocalDate hasta) {
        exigir(TipoReporte.INGRESOS);
        validarRango(desde, hasta);
        LocalDate fin = hasta.plusDays(1);
        Map<LocalDate, Map<MedioPago, BigDecimal>> porDia = new TreeMap<>();
        Map<LocalDate, Integer> pagosPorDia = new TreeMap<>();
        Map<MedioPago, BigDecimal> totales = new EnumMap<>(MedioPago.class);
        for (MedioPago m : MedioPago.values()) {
            totales.put(m, cero());
        }
        int pagos = 0;
        for (ReporteDao.IngresoDia f : reporteDao.ingresosPorDia(desde, fin)) {
            LocalDate dia = LocalDate.parse(f.dia());
            MedioPago medio = MedioPago.valueOf(f.medio());
            BigDecimal monto = DineroConverter.desdeCentimos(f.montoCent());
            porDia.computeIfAbsent(dia, d -> new EnumMap<>(MedioPago.class)).merge(medio, monto, BigDecimal::add);
            pagosPorDia.merge(dia, f.pagos(), Integer::sum);
            totales.merge(medio, monto, BigDecimal::add);
            pagos += f.pagos();
        }
        List<IngresoDiaDto> dias = porDia.entrySet().stream()
                .map(e -> new IngresoDiaDto(e.getKey(), pagosPorDia.get(e.getKey()), e.getValue(),
                        suma(e.getValue().values())))
                .toList();
        List<PagoReporteDto> detalle = reporteDao.pagos(desde, fin).stream()
                .map(p -> new PagoReporteDto(LocalDateTime.parse(p.fecha(), LocalDateTimeConverter.FORMATO),
                        p.recibo(), p.historia(), p.paciente(), MedioPago.valueOf(p.medio()), p.referencia(),
                        DineroConverter.desdeCentimos(p.montoCent())))
                .toList();
        return new ReporteIngresosDto(desde, hasta, dias, totales, pagos, suma(totales.values()),
                serieIngresos(dias, desde, hasta), detalle);
    }

    public byte[] excelIngresos(LocalDate desde, LocalDate hasta) {
        return reporteExcel.ingresos(ingresos(desde, hasta), configuracionService.datosClinica());
    }

    /** Por día si el rango es corto (incluidos los días sin ingresos); si no, por mes. */
    static List<MontoPeriodoDto> serieIngresos(List<IngresoDiaDto> dias, LocalDate desde, LocalDate hasta) {
        List<MontoPeriodoDto> serie = new ArrayList<>();
        if (ChronoUnit.DAYS.between(desde, hasta) < DIAS_GRAFICO_DIARIO) {
            Map<LocalDate, BigDecimal> porDia = new TreeMap<>();
            dias.forEach(d -> porDia.put(d.dia(), d.total()));
            for (LocalDate d = desde; !d.isAfter(hasta); d = d.plusDays(1)) {
                serie.add(new MontoPeriodoDto(DIA.format(d), porDia.getOrDefault(d, cero())));
            }
            return serie;
        }
        Map<YearMonth, BigDecimal> porMes = new LinkedHashMap<>();
        for (YearMonth m = YearMonth.from(desde); !m.isAfter(YearMonth.from(hasta)); m = m.plusMonths(1)) {
            porMes.put(m, cero());
        }
        dias.forEach(d -> porMes.merge(YearMonth.from(d.dia()), d.total(), BigDecimal::add));
        porMes.forEach((m, monto) -> serie.add(new MontoPeriodoDto(MES.format(m), monto)));
        return serie;
    }

    // ------------------------------------------------------------------ producción (RF-REP-03)

    /** Producción en el rango; el odontólogo solo ve la suya. */
    public ReporteProduccionDto produccion(LocalDate desde, LocalDate hasta) {
        UsuarioSesion u = exigir(TipoReporte.PRODUCCION);
        validarRango(desde, hasta);
        Long odontologoId = Rol.ODONTOLOGO.name().equals(u.rol()) ? u.odontologoId() : null;
        List<ProduccionFilaDto> filas = reporteDao.produccion(desde, hasta.plusDays(1), odontologoId).stream()
                .map(f -> new ProduccionFilaDto(f.odontologoId(), f.odontologo(), f.codigo(), f.procedimiento(),
                        f.cantidad(), DineroConverter.desdeCentimos(f.montoCent())))
                .toList();
        Map<String, BigDecimal> porOdontologo = new TreeMap<>();
        filas.forEach(f -> porOdontologo.merge(f.odontologo(), f.monto(), BigDecimal::add));
        return new ReporteProduccionDto(desde, hasta, filas,
                porOdontologo.entrySet().stream().map(e -> new MontoPeriodoDto(e.getKey(), e.getValue())).toList(),
                filas.stream().mapToInt(ProduccionFilaDto::cantidad).sum(),
                suma(filas.stream().map(ProduccionFilaDto::monto).toList()));
    }

    public byte[] excelProduccion(LocalDate desde, LocalDate hasta) {
        return reporteExcel.produccion(produccion(desde, hasta), configuracionService.datosClinica());
    }

    // ------------------------------------------------------------------ morosidad (RF-REP-04)

    public ReporteMorosidadDto morosidad() {
        return morosidad(LocalDate.now());
    }

    ReporteMorosidadDto morosidad(LocalDate hoy) {
        exigir(TipoReporte.MOROSIDAD);
        List<MorosoDto> pacientes = DashboardService.morosos(reporteDao.morosidad(hoy, 0), hoy);
        return new ReporteMorosidadDto(hoy, pacientes, pacientes.stream().mapToInt(MorosoDto::cuotasVencidas).sum(),
                suma(pacientes.stream().map(MorosoDto::saldoVencido).toList()));
    }

    public byte[] excelMorosidad() {
        return reporteExcel.morosidad(morosidad(), configuracionService.datosClinica());
    }

    // ------------------------------------------------------------------ epidemiológico (RF-REP-05)

    public ReporteEpidemiologicoDto epidemiologico(LocalDate desde, LocalDate hasta) {
        exigir(TipoReporte.EPIDEMIOLOGICO);
        validarRango(desde, hasta);
        PromedioIndices promedio = new PromedioIndices();
        for (ReporteDao.PacienteExaminado p : reporteDao.pacientesExaminados(desde, hasta.plusDays(1))) {
            Optional<IndicesVista> indices = odontogramaService.indicesFirmados(p.pacienteId());
            if (indices.isEmpty()) {
                continue;
            }
            LocalDate examen = LocalDateTime.parse(p.ultimoCierre(), LocalDateTimeConverter.FORMATO).toLocalDate();
            int edad = Math.max(0, Period.between(LocalDate.parse(p.fechaNacimiento()), examen).getYears());
            promedio.agregar(edad, indices.get().cpod(), indices.get().ceod());
        }
        List<GrupoIndicesDto> grupos = promedio.resultado().stream().map(ReporteService::grupo).toList();
        return new ReporteEpidemiologicoDto(desde, hasta, promedio.pacientes(), grupos,
                configuracionService.booleano("odontograma.excluir_terceros_molares_cpod"));
    }

    public byte[] excelEpidemiologico(LocalDate desde, LocalDate hasta) {
        return reporteExcel.epidemiologico(epidemiologico(desde, hasta), configuracionService.datosClinica());
    }

    private static GrupoIndicesDto grupo(PromedioIndices.Grupo g) {
        boolean hay = g.pacientes() > 0;
        boolean cpod = hay && g.grupo().aplicaCpod;
        return new GrupoIndicesDto(g.grupo().etiqueta, g.pacientes(), cpod ? g.cpod() : null, cpod ? g.c() : null,
                cpod ? g.p() : null, cpod ? g.o() : null, hay && g.grupo().aplicaCeod ? g.ceod() : null,
                cpod ? etiqueta(g.nivelCpod().name()) : null);
    }

    /** {@code MUY_ALTO} → "Muy alto". */
    private static String etiqueta(String nombre) {
        String t = nombre.replace('_', ' ').toLowerCase(PERU);
        return Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }

    // ------------------------------------------------------------------ auditoría

    /** Registro de auditoría en Excel (solo administrador, como la consulta). */
    public byte[] excelAuditoria(LocalDate desde, LocalDate hasta, String accion) {
        exigir(TipoReporte.AUDITORIA);
        return reporteExcel.auditoria(auditoriaService.buscar(desde, hasta, accion), desde, hasta,
                configuracionService.datosClinica());
    }

    // ------------------------------------------------------------------ comunes

    private UsuarioSesion exigir(TipoReporte tipo) {
        UsuarioSesion u = sesionUsuario.exigir(Permiso.REPORTES_VER);
        if (!POR_ROL.getOrDefault(Rol.valueOf(u.rol()), Set.of()).contains(tipo)) {
            throw new PermisoDenegadoException("No tiene permiso para ver el reporte de " + tipo.etiqueta.toLowerCase(PERU) + ".");
        }
        return u;
    }

    static void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new NegocioException("Indique la fecha inicial y la final del reporte.");
        }
        if (hasta.isBefore(desde)) {
            throw new NegocioException("La fecha final no puede ser anterior a la inicial.");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) >= DIAS_MAXIMOS) {
            throw new NegocioException("El rango del reporte no puede pasar de 3 años.");
        }
    }

    private static BigDecimal cero() {
        return BigDecimal.ZERO.setScale(2);
    }

    private static BigDecimal suma(Collection<BigDecimal> montos) {
        return montos.stream().reduce(cero(), BigDecimal::add);
    }
}
