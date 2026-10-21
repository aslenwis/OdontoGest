/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio;

import com.odontogest.modelo.Rol;
import com.odontogest.persistencia.conversor.DineroConverter;
import com.odontogest.persistencia.dao.DashboardDao;
import com.odontogest.persistencia.dao.ReporteDao;
import com.odontogest.seguridad.SesionUsuario;
import com.odontogest.servicio.dto.DashboardDto;
import com.odontogest.servicio.dto.MontoPeriodoDto;
import com.odontogest.servicio.dto.MorosoDto;
import com.odontogest.servicio.dto.UsuarioSesion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tablero de inicio (RF-REP-01). Lo ven todos los roles; el odontólogo ve sus citas y su producción en lugar de los
 * ingresos de la clínica (PLAN §3: el odontólogo solo consulta su propia producción).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    static final int MESES_GRAFICO = 6;
    static final int MOROSOS_TABLERO = 10;
    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MMM yyyy", Locale.of("es", "PE"));

    private final Clock reloj;
    private final DashboardDao dashboardDao;
    private final ReporteDao reporteDao;
    private final CitaService citaService;
    private final SesionUsuario sesionUsuario;

    public DashboardDto cargar() {
        return cargar(LocalDate.now(reloj));
    }

    DashboardDto cargar(LocalDate hoy) {
        UsuarioSesion usuario = sesionUsuario.exigirUsuario();
        Long odontologoId = Rol.ODONTOLOGO.name().equals(usuario.rol()) ? usuario.odontologoId() : null;
        boolean produccion = odontologoId != null;

        YearMonth mes = YearMonth.from(hoy);
        LocalDate inicioMes = mes.atDay(1);
        LocalDate finMes = mes.plusMonths(1).atDay(1);
        LocalDate inicioSerie = mes.minusMonths(MESES_GRAFICO - 1L).atDay(1);
        List<DashboardDao.MontoMes> meses = produccion
                ? dashboardDao.produccionPorMes(odontologoId, inicioSerie, finMes)
                : dashboardDao.ingresosPorMes(inicioSerie, finMes);
        List<MontoPeriodoDto> serie = serieMensual(meses, mes);
        long montoMes = meses.stream().filter(m -> m.mes().equals(mes.toString()))
                .mapToLong(DashboardDao.MontoMes::montoCent).sum();

        DashboardDao.Vencidas vencidas = dashboardDao.vencidas(hoy);
        return new DashboardDto(dashboardDao.citasDelDia(hoy, odontologoId), DineroConverter.desdeCentimos(montoMes),
                produccion, dashboardDao.pacientesNuevos(inicioMes, finMes), vencidas.cuotas(), vencidas.pacientes(),
                DineroConverter.desdeCentimos(vencidas.saldoCent()), serie, citaService.delDia(hoy, odontologoId),
                produccion ? List.of() : morosos(reporteDao.morosidad(hoy, MOROSOS_TABLERO), hoy));
    }

    /** Los {@link #MESES_GRAFICO} meses que terminan en {@code ultimo}, con cero en los meses sin movimiento. */
    static List<MontoPeriodoDto> serieMensual(List<DashboardDao.MontoMes> meses, YearMonth ultimo) {
        Map<String, Long> porMes = meses.stream()
                .collect(Collectors.toMap(DashboardDao.MontoMes::mes, DashboardDao.MontoMes::montoCent, Long::sum));
        List<MontoPeriodoDto> serie = new ArrayList<>();
        for (int i = MESES_GRAFICO - 1; i >= 0; i--) {
            YearMonth m = ultimo.minusMonths(i);
            serie.add(new MontoPeriodoDto(MES.format(m), DineroConverter.desdeCentimos(porMes.getOrDefault(m.toString(), 0L))));
        }
        return serie;
    }

    static List<MorosoDto> morosos(List<ReporteDao.MorosoFila> filas, LocalDate hoy) {
        return filas.stream().map(f -> {
            LocalDate masAntiguo = LocalDate.parse(f.vencimientoMasAntiguo());
            return new MorosoDto(f.pacienteId(), f.historia(), f.paciente(), f.telefono(), f.cuotas(),
                    DineroConverter.desdeCentimos(f.saldoCent()), masAntiguo, ChronoUnit.DAYS.between(masAntiguo, hoy));
        }).toList();
    }
}
