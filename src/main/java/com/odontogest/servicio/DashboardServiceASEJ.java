package com.odontogest.servicio;

import com.odontogest.modelo.RolASEJ;
import com.odontogest.persistencia.conversor.DineroConverterASEJ;
import com.odontogest.persistencia.dao.DashboardDaoASEJ;
import com.odontogest.persistencia.dao.ReporteDaoASEJ;
import com.odontogest.seguridad.SesionUsuarioASEJ;
import com.odontogest.servicio.dto.DashboardDtoASEJ;
import com.odontogest.servicio.dto.MontoPeriodoDtoASEJ;
import com.odontogest.servicio.dto.MorosoDtoASEJ;
import com.odontogest.servicio.dto.UsuarioSesionASEJ;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceASEJ {

    static final int MESES_GRAFICOASEJ = 6;
    static final int MOROSOS_TABLEROASEJ = 10;
    private static final DateTimeFormatter MESASEJ = DateTimeFormatter.ofPattern("MMM yyyy", Locale.of("es", "PE"));

    private final Clock relojASEJ;
    private final DashboardDaoASEJ dashboardDaoASEJ;
    private final ReporteDaoASEJ reporteDaoASEJ;
    private final CitaServiceASEJ citaServiceASEJ;
    private final SesionUsuarioASEJ sesionUsuarioASEJ;

    public DashboardDtoASEJ cargarASEJ() {
        return cargarASEJ(LocalDate.now(relojASEJ));
    }

    DashboardDtoASEJ cargarASEJ(LocalDate hoyASEJ) {
        UsuarioSesionASEJ usuarioASEJ = sesionUsuarioASEJ.exigirUsuarioASEJ();
        Long odontologoIdASEJ = RolASEJ.ODONTOLOGO.name().equals(usuarioASEJ.rolASEJ()) ? usuarioASEJ.odontologoIdASEJ() : null;
        boolean produccionASEJ = odontologoIdASEJ != null;

        YearMonth mesASEJ = YearMonth.from(hoyASEJ);
        LocalDate inicioMesASEJ = mesASEJ.atDay(1);
        LocalDate finMesASEJ = mesASEJ.plusMonths(1).atDay(1);
        LocalDate inicioSerieASEJ = mesASEJ.minusMonths(MESES_GRAFICOASEJ - 1L).atDay(1);
        List<DashboardDaoASEJ.MontoMesASEJ> mesesASEJ = produccionASEJ
                ? dashboardDaoASEJ.produccionPorMesASEJ(odontologoIdASEJ, inicioSerieASEJ, finMesASEJ)
                : dashboardDaoASEJ.ingresosPorMesASEJ(inicioSerieASEJ, finMesASEJ);
        List<MontoPeriodoDtoASEJ> serieASEJ = serieMensualASEJ(mesesASEJ, mesASEJ);
        long montoMesASEJ = mesesASEJ.stream().filter(mASEJ -> mASEJ.mesASEJ().equals(mesASEJ.toString()))
                .mapToLong(DashboardDaoASEJ.MontoMesASEJ::montoCentASEJ).sum();

        DashboardDaoASEJ.VencidasASEJ vencidasASEJ = dashboardDaoASEJ.vencidasASEJ(hoyASEJ);
        return new DashboardDtoASEJ(dashboardDaoASEJ.citasDelDiaASEJ(hoyASEJ, odontologoIdASEJ), DineroConverterASEJ.desdeCentimosASEJ(montoMesASEJ),
                produccionASEJ, dashboardDaoASEJ.pacientesNuevosASEJ(inicioMesASEJ, finMesASEJ), vencidasASEJ.cuotasASEJ(), vencidasASEJ.pacientesASEJ(),
                DineroConverterASEJ.desdeCentimosASEJ(vencidasASEJ.saldoCentASEJ()), serieASEJ, citaServiceASEJ.delDiaASEJ(hoyASEJ, odontologoIdASEJ),
                produccionASEJ ? List.of() : morososASEJ(reporteDaoASEJ.morosidadASEJ(hoyASEJ, MOROSOS_TABLEROASEJ), hoyASEJ));
    }

    static List<MontoPeriodoDtoASEJ> serieMensualASEJ(List<DashboardDaoASEJ.MontoMesASEJ> mesesASEJ, YearMonth ultimoASEJ) {
        Map<String, Long> porMesASEJ = mesesASEJ.stream()
                .collect(Collectors.toMap(DashboardDaoASEJ.MontoMesASEJ::mesASEJ, DashboardDaoASEJ.MontoMesASEJ::montoCentASEJ, Long::sum));
        List<MontoPeriodoDtoASEJ> serieASEJ = new ArrayList<>();
        for (int iASEJ = MESES_GRAFICOASEJ - 1; iASEJ >= 0; iASEJ--) {
            YearMonth mASEJ = ultimoASEJ.minusMonths(iASEJ);
            serieASEJ.add(new MontoPeriodoDtoASEJ(MESASEJ.format(mASEJ), DineroConverterASEJ.desdeCentimosASEJ(porMesASEJ.getOrDefault(mASEJ.toString(), 0L))));
        }
        return serieASEJ;
    }

    static List<MorosoDtoASEJ> morososASEJ(List<ReporteDaoASEJ.MorosoFilaASEJ> filasASEJ, LocalDate hoyASEJ) {
        return filasASEJ.stream().map(fASEJ -> {
            LocalDate masAntiguoASEJ = LocalDate.parse(fASEJ.vencimientoMasAntiguoASEJ());
            return new MorosoDtoASEJ(fASEJ.pacienteIdASEJ(), fASEJ.historiaASEJ(), fASEJ.pacienteASEJ(), fASEJ.telefonoASEJ(), fASEJ.cuotasASEJ(),
                    DineroConverterASEJ.desdeCentimosASEJ(fASEJ.saldoCentASEJ()), masAntiguoASEJ, ChronoUnit.DAYS.between(masAntiguoASEJ, hoyASEJ));
        }).toList();
    }
}
