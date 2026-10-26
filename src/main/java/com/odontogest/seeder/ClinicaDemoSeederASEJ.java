package com.odontogest.seeder;

import com.odontogest.config.RelojAplicacionASEJ;
import com.odontogest.dominio.finanzas.FrecuenciaASEJ;
import com.odontogest.dominio.odontograma.DenticionASEJ;
import com.odontogest.dominio.odontograma.EstadoHallazgoASEJ;
import com.odontogest.dominio.odontograma.SuperficieASEJ;
import com.odontogest.modelo.EstadoCitaASEJ;
import com.odontogest.modelo.EstadoPlanASEJ;
import com.odontogest.modelo.EstadoPlanItemASEJ;
import com.odontogest.modelo.MedioPagoASEJ;
import com.odontogest.modelo.OdontologoASEJ;
import com.odontogest.modelo.UsuarioASEJ;
import com.odontogest.persistencia.repositorio.CitaRepositoryASEJ;
import com.odontogest.persistencia.repositorio.UsuarioRepositoryASEJ;
import com.odontogest.seguridad.SesionUsuarioASEJ;
import com.odontogest.servicio.AtencionServiceASEJ;
import com.odontogest.servicio.CatalogoHallazgoServiceASEJ;
import com.odontogest.servicio.CitaServiceASEJ;
import com.odontogest.servicio.OdontogramaServiceASEJ;
import com.odontogest.servicio.PacienteServiceASEJ;
import com.odontogest.servicio.PagoServiceASEJ;
import com.odontogest.servicio.PlanTratamientoServiceASEJ;
import com.odontogest.servicio.dto.AtencionFormASEJ;
import com.odontogest.servicio.dto.CitaFormASEJ;
import com.odontogest.servicio.dto.CronogramaFormASEJ;
import com.odontogest.servicio.dto.CuotaDtoASEJ;
import com.odontogest.servicio.dto.CuotaPacienteDtoASEJ;
import com.odontogest.servicio.dto.HallazgoDtoASEJ;
import com.odontogest.servicio.dto.HallazgoTipoDtoASEJ;
import com.odontogest.servicio.dto.NuevoHallazgoASEJ;
import com.odontogest.servicio.dto.OdontogramaVistaASEJ;
import com.odontogest.servicio.dto.PagoFormASEJ;
import com.odontogest.servicio.dto.PlanDtoASEJ;
import com.odontogest.servicio.dto.PlanItemDtoASEJ;
import com.odontogest.servicio.dto.UsuarioSesionASEJ;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClinicaDemoSeederASEJ {

    static final int DIAS_HISTORIAASEJ = 175;
    static final int PRIMERAS_CONSULTASASEJ = 40;
    static final int EVOLUCIONESASEJ = 15;
    static final int CITAS_VENTANAASEJ = 120;
    static final int DIAS_VENTANAASEJ = 30;
    private static final int CONTROLESASEJ = 70;

    private static final int[] MOLARESASEJ = {16, 17, 26, 27, 36, 37, 46, 47};
    private static final int[] PREMOLARESASEJ = {14, 15, 24, 25, 34, 35, 44, 45};
    private static final int[] ANTERIORESASEJ = {11, 12, 13, 21, 22, 23, 31, 32, 33, 41, 42, 43};
    private static final int[] MOLARES_DECIDUOSASEJ = {54, 55, 64, 65, 74, 75, 84, 85};
    private static final int[] PRIMEROS_MOLARESASEJ = {16, 26, 36, 46};

    private final PacienteSeederASEJ pacienteSeederASEJ;
    private final PacienteServiceASEJ pacienteServiceASEJ;
    private final AtencionServiceASEJ atencionServiceASEJ;
    private final OdontogramaServiceASEJ odontogramaServiceASEJ;
    private final CatalogoHallazgoServiceASEJ catalogoServiceASEJ;
    private final PlanTratamientoServiceASEJ planServiceASEJ;
    private final PagoServiceASEJ pagoServiceASEJ;
    private final CitaServiceASEJ citaServiceASEJ;
    private final CitaRepositoryASEJ citaRepositoryASEJ;
    private final UsuarioRepositoryASEJ usuarioRepositoryASEJ;
    private final SesionUsuarioASEJ sesionUsuarioASEJ;
    private final RelojAplicacionASEJ relojASEJ;

    public record ResumenASEJ(int pacientesASEJ, int primerasConsultasASEJ, int evolucionesASEJ, int pagosASEJ, int citasVentanaASEJ,
                          int omitidosASEJ, String ultimaHistoriaASEJ) {
    }

    public ResumenASEJ sembrarASEJ(List<OdontologoASEJ> odontologosASEJ, long semillaASEJ, LocalDateTime ahoraASEJ) {
        SimulacionASEJ sASEJ = new SimulacionASEJ(odontologosASEJ.stream().map(OdontologoASEJ::getIdASEJ).toList(), new Random(semillaASEJ),
                ahoraASEJ.truncatedTo(ChronoUnit.MINUTES));
        try {
            return sASEJ.ejecutarASEJ();
        } finally {
            relojASEJ.liberarASEJ();
            sesionUsuarioASEJ.cerrarASEJ();
        }
    }

    private final class SimulacionASEJ {

        private record EventoASEJ(LocalDateTime momentoASEJ, long ordenASEJ, Runnable accionASEJ) {
        }

        private record RegistradoASEJ(long idASEJ, PacienteSeederASEJ.PacienteDemoASEJ datosASEJ, int odontologoASEJ) {
        }

        private final List<Long> odontologosASEJ;
        private final Random rASEJ;
        private final LocalDateTime ahoraASEJ;
        private final LocalDate hoyASEJ;
        private final PriorityQueue<EventoASEJ> colaASEJ = new PriorityQueue<>(
                Comparator.comparing(EventoASEJ::momentoASEJ).thenComparingLong(EventoASEJ::ordenASEJ));
        private long ordenASEJ;
        private final Map<String, Long> tiposASEJ;
        private final UsuarioSesionASEJ adminASEJ;
        private final UsuarioSesionASEJ recepcionASEJ;

        private final List<RegistradoASEJ> registradosASEJ = new ArrayList<>();
        private final List<RegistradoASEJ> atendidosASEJ = new ArrayList<>();
        private int primerasASEJ;
        private int evolucionesASEJ;
        private int evolucionesProgramadasASEJ;
        private int pagosASEJ;
        private int omitidosASEJ;

        SimulacionASEJ(List<Long> odontologosASEJ, Random rASEJ, LocalDateTime ahoraASEJ) {
            this.odontologosASEJ = odontologosASEJ;
            this.rASEJ = rASEJ;
            this.ahoraASEJ = ahoraASEJ;
            this.hoyASEJ = ahoraASEJ.toLocalDate();
            this.adminASEJ = sesionASEJ("admin");
            this.recepcionASEJ = sesionASEJ("recepcion");
            relojASEJ.fijarASEJ(ahoraASEJ);
            sesionUsuarioASEJ.iniciarASEJ(adminASEJ);
            this.tiposASEJ = catalogoServiceASEJ.listarTiposASEJ(true).stream()
                    .collect(Collectors.toMap(HallazgoTipoDtoASEJ::codigoASEJ, HallazgoTipoDtoASEJ::idASEJ));
        }

        ResumenASEJ ejecutarASEJ() {
            List<PacienteSeederASEJ.PacienteDemoASEJ> fichasASEJ = pacienteSeederASEJ.generarASEJ(rASEJ);
            LocalDate inicioASEJ = hoyASEJ.minusDays(DIAS_HISTORIAASEJ);
            List<Integer> indicesASEJ = new ArrayList<>();
            for (int iASEJ = 0; iASEJ < fichasASEJ.size() - 5; iASEJ++) {
                indicesASEJ.add(iASEJ);
            }
            Collections.shuffle(indicesASEJ, rASEJ);
            Set<Integer> conConsultaASEJ = new HashSet<>(indicesASEJ.subList(0, PRIMERAS_CONSULTASASEJ));
            for (int iASEJ = 0; iASEJ < fichasASEJ.size(); iASEJ++) {
                LocalDate diaASEJ = laborableASEJ(inicioASEJ.plusDays((long) iASEJ * (DIAS_HISTORIAASEJ - 1) / (fichasASEJ.size() - 1)));
                if (!diaASEJ.isBefore(hoyASEJ)) {
                    diaASEJ = laborableASEJ(hoyASEJ.minusDays(2));
                }
                LocalDateTime momentoASEJ = diaASEJ.atTime(8, 15).plusMinutes(5L * rASEJ.nextInt(30));
                PacienteSeederASEJ.PacienteDemoASEJ fichaASEJ = fichasASEJ.get(iASEJ);
                boolean consultaASEJ = conConsultaASEJ.contains(iASEJ);
                enASEJ(momentoASEJ, () -> registrarASEJ(fichaASEJ, consultaASEJ));
            }
            for (int iASEJ = 0; iASEJ < CONTROLESASEJ; iASEJ++) {
                programarControlASEJ();
            }
            while (!colaASEJ.isEmpty()) {
                EventoASEJ eASEJ = colaASEJ.poll();
                relojASEJ.fijarASEJ(eASEJ.momentoASEJ());
                try {
                    eASEJ.accionASEJ().run();
                } catch (RuntimeException exASEJ) {
                    omitidosASEJ++;
                    if (omitidosASEJ <= 5) {
                        logASEJ.warn("Datos demo: paso omitido el {}: {}", eASEJ.momentoASEJ(), exASEJ.toString());
                    } else {
                        logASEJ.debug("Datos demo: paso omitido el {}: {}", eASEJ.momentoASEJ(), exASEJ.getMessage());
                    }
                }
            }
            relojASEJ.fijarASEJ(ahoraASEJ);
            completarVentanaASEJ();
            String ultimaASEJ = registradosASEJ.isEmpty() ? ""
                    : pacienteServiceASEJ.obtenerASEJ(registradosASEJ.get(registradosASEJ.size() - 1).idASEJ()).numeroHistoriaASEJ();
            return new ResumenASEJ(registradosASEJ.size(), primerasASEJ, evolucionesASEJ, pagosASEJ, citasEnVentanaASEJ(), omitidosASEJ, ultimaASEJ);
        }

        private void registrarASEJ(PacienteSeederASEJ.PacienteDemoASEJ fichaASEJ, boolean conConsultaASEJ) {
            comoASEJ(recepcionASEJ);
            long idASEJ = pacienteSeederASEJ.registrarASEJ(fichaASEJ, relojASEJ.instant().atZone(relojASEJ.getZone()).toLocalDate(), rASEJ);
            int odontologoASEJ = fichaASEJ.menorASEJ() && fichaASEJ.edadASEJ() >= 10 && rASEJ.nextInt(2) == 0 ? 2
                    : rASEJ.nextInt(100) < 55 ? 0 : rASEJ.nextInt(100) < 55 ? 1 : 2;
            RegistradoASEJ pASEJ = new RegistradoASEJ(idASEJ, fichaASEJ, odontologoASEJ);
            registradosASEJ.add(pASEJ);
            comoASEJ(adminASEJ);
            pacienteSeederASEJ.registrarAnamnesisASEJ(idASEJ, fichaASEJ);
            comoASEJ(recepcionASEJ);
            if (!conConsultaASEJ) {
                return;
            }
            Optional<LocalDateTime> huecoASEJ = citaServiceASEJ.siguienteHuecoASEJ(odontologosASEJ.get(odontologoASEJ), 45,
                    momentoASEJ().plusMinutes(20), idASEJ);
            if (huecoASEJ.isEmpty()) {
                return;
            }
            long citaASEJ = citaServiceASEJ.programarASEJ(new CitaFormASEJ(null, idASEJ, odontologosASEJ.get(odontologoASEJ), huecoASEJ.get(), 45,
                    "Primera consulta: evaluación general", null, null)).idASEJ();
            enASEJ(huecoASEJ.get(), () -> primeraConsultaASEJ(pASEJ, citaASEJ));
        }

        private void primeraConsultaASEJ(RegistradoASEJ pASEJ, long citaASEJ) {
            comoASEJ(adminASEJ);
            LocalDateTime tASEJ = momentoASEJ();
            long atencionASEJ = atencionServiceASEJ.abrirDesdeCitaASEJ(citaASEJ);
            OdontogramaVistaASEJ vASEJ = odontogramaServiceASEJ.obtenerVistaASEJ(pASEJ.idASEJ(), null);
            int registradosOkASEJ = registrarHallazgosASEJ(vASEJ.odontogramaIdASEJ(), vASEJ.denticionASEJ(), pASEJ.datosASEJ().edadASEJ());
            boolean cariadoASEJ = odontogramaServiceASEJ.obtenerVistaASEJ(pASEJ.idASEJ(), null).historialASEJ().stream()
                    .anyMatch(hASEJ -> hASEJ.codigoTipoASEJ().equals("CARIES"));
            atencionServiceASEJ.actualizarASEJ(new AtencionFormASEJ(atencionASEJ, pASEJ.idASEJ(), null, "Primera consulta: evaluación general",
                    "Examen clínico intraoral y extraoral. " + registradosOkASEJ + " hallazgo(s) registrados en el odontograma.",
                    cariadoASEJ ? "Caries dental (K02.1)." : "Sin caries activas.",
                    "Se explica el plan de tratamiento al paciente.", null));
            fijarASEJ(tASEJ.plusMinutes(20));
            odontogramaServiceASEJ.cerrarASEJ(vASEJ.odontogramaIdASEJ());
            primerasASEJ++;

            long planASEJ = planServiceASEJ.crearASEJ(pASEJ.idASEJ(), odontologosASEJ.get(pASEJ.odontologoASEJ()), true);
            planServiceASEJ.cambiarEstadoASEJ(planASEJ, EstadoPlanASEJ.PROPUESTO);
            PlanDtoASEJ dtoASEJ = planServiceASEJ.obtenerASEJ(planASEJ);
            boolean aceptaASEJ = rASEJ.nextInt(100) < 88;
            if (aceptaASEJ) {
                BigDecimal totalASEJ = dtoASEJ.totalASEJ();
                if (totalASEJ.compareTo(new BigDecimal("350")) > 0) {
                    BigDecimal inicialASEJ = totalASEJ.multiply(new BigDecimal("0.3")).divide(BigDecimal.TEN, 0, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.TEN);
                    int cuotasASEJ = Math.min(5, totalASEJ.divide(new BigDecimal("400"), 0, RoundingMode.UP).intValue());
                    planServiceASEJ.generarCronogramaASEJ(new CronogramaFormASEJ(planASEJ, inicialASEJ, cuotasASEJ, tASEJ.toLocalDate().plusMonths(1),
                            FrecuenciaASEJ.MENSUAL));
                }
                dtoASEJ = planServiceASEJ.aceptarASEJ(planASEJ, pASEJ.datosASEJ().firmanteASEJ());
                realizarASEJ(atencionASEJ, dtoASEJ, iASEJ -> iASEJ.procedimientoCodigoASEJ().equals("CONS"));
            }
            fijarASEJ(tASEJ.plusMinutes(40));
            atencionServiceASEJ.cerrarASEJ(atencionASEJ);
            if (!atendidosASEJ.contains(pASEJ)) {
                atendidosASEJ.add(pASEJ);
            }
            if (!aceptaASEJ) {
                return;
            }
            comoASEJ(recepcionASEJ);
            fijarASEJ(tASEJ.plusMinutes(50));
            pagarASEJ(pASEJ.idASEJ(), false);
            for (CuotaDtoASEJ cASEJ : planServiceASEJ.obtenerASEJ(planASEJ).cuotasASEJ()) {
                if (cASEJ.numeroASEJ() == 0) {
                    continue;
                }
                int azarASEJ = rASEJ.nextInt(100);
                if (azarASEJ < 70) {
                    enASEJ(cASEJ.vencimientoASEJ().minusDays(rASEJ.nextInt(4)).atTime(9 + rASEJ.nextInt(9), 5 * rASEJ.nextInt(12)),
                            () -> {
                                comoASEJ(recepcionASEJ);
                                pagarASEJ(pASEJ.idASEJ(), false);
                            });
                } else if (azarASEJ < 80) {
                    enASEJ(cASEJ.vencimientoASEJ().plusDays(3 + rASEJ.nextInt(10)).atTime(10 + rASEJ.nextInt(8), 5 * rASEJ.nextInt(12)),
                            () -> {
                                comoASEJ(recepcionASEJ);
                                pagarASEJ(pASEJ.idASEJ(), true);
                            });
                }
            }
            boolean antiguoASEJ = !tASEJ.toLocalDate().isAfter(hoyASEJ.minusDays(35));
            if (cariadoASEJ && antiguoASEJ && evolucionesProgramadasASEJ < EVOLUCIONESASEJ) {
                evolucionesProgramadasASEJ++;
                LocalDateTime desdeASEJ = laborableASEJ(tASEJ.toLocalDate().plusDays(10 + rASEJ.nextInt(20))).atTime(9, 0);
                Optional<LocalDateTime> huecoASEJ = citaServiceASEJ.siguienteHuecoASEJ(odontologosASEJ.get(pASEJ.odontologoASEJ()), 60, desdeASEJ, pASEJ.idASEJ());
                if (huecoASEJ.isPresent()) {
                    long cita2ASEJ = citaServiceASEJ.programarASEJ(new CitaFormASEJ(null, pASEJ.idASEJ(), odontologosASEJ.get(pASEJ.odontologoASEJ()),
                            huecoASEJ.get(), 60, "Restauraciones según plan de tratamiento", null, null)).idASEJ();
                    enASEJ(huecoASEJ.get(), () -> evolucionASEJ(pASEJ, planASEJ, cita2ASEJ));
                }
            }
        }

        private void evolucionASEJ(RegistradoASEJ pASEJ, long planASEJ, long citaASEJ) {
            comoASEJ(adminASEJ);
            LocalDateTime tASEJ = momentoASEJ();
            long atencionASEJ = atencionServiceASEJ.abrirDesdeCitaASEJ(citaASEJ);
            OdontogramaVistaASEJ vASEJ = odontogramaServiceASEJ.obtenerVistaASEJ(pASEJ.idASEJ(), null);
            List<HallazgoDtoASEJ> cariesASEJ = vASEJ.historialASEJ().stream()
                    .filter(hASEJ -> hASEJ.codigoTipoASEJ().equals("CARIES") && hASEJ.vigenteASEJ() && !hASEJ.anuladoASEJ()).toList();
            for (HallazgoDtoASEJ hASEJ : cariesASEJ) {
                intentarASEJ(() -> odontogramaServiceASEJ.registrarHallazgoASEJ(new NuevoHallazgoASEJ(vASEJ.odontogramaIdASEJ(),
                        tiposASEJ.get("RESTAURACION_DEFINITIVA"), hASEJ.piezaASEJ(), null, hASEJ.superficiesASEJ(), EstadoHallazgoASEJ.BUENO,
                        hASEJ.piezaASEJ() > 50 ? "IV" : "R", "Restaurada en esta atención")));
            }
            Set<Long> cariesIdsASEJ = cariesASEJ.stream().map(HallazgoDtoASEJ::idASEJ).collect(Collectors.toSet());
            realizarASEJ(atencionASEJ, planServiceASEJ.obtenerASEJ(planASEJ),
                    iASEJ -> (iASEJ.hallazgoIdASEJ() != null && cariesIdsASEJ.contains(iASEJ.hallazgoIdASEJ())) || iASEJ.procedimientoCodigoASEJ().equals("PROF"));
            atencionServiceASEJ.actualizarASEJ(new AtencionFormASEJ(atencionASEJ, pASEJ.idASEJ(), null, "Restauraciones según plan de tratamiento",
                    "Aislamiento relativo, remoción de caries y restauración.", "Caries tratadas.",
                    "Control en seis meses.", null));
            fijarASEJ(tASEJ.plusMinutes(40));
            odontogramaServiceASEJ.cerrarASEJ(vASEJ.odontogramaIdASEJ());
            fijarASEJ(tASEJ.plusMinutes(50));
            atencionServiceASEJ.cerrarASEJ(atencionASEJ);
            evolucionesASEJ++;
        }

        private void programarControlASEJ() {
            LocalDate diaASEJ = laborableASEJ(hoyASEJ.plusDays(rASEJ.nextInt(2 * DIAS_VENTANAASEJ + 1) - DIAS_VENTANAASEJ));
            LocalDateTime objetivoASEJ = diaASEJ.atTime(rASEJ.nextBoolean() ? 9 : 15, 0).plusMinutes(15L * rASEJ.nextInt(14));
            LocalDateTime programacionASEJ = diaASEJ.minusDays(2 + rASEJ.nextInt(12)).atTime(9, 30).plusMinutes(5L * rASEJ.nextInt(60));
            if (programacionASEJ.isAfter(ahoraASEJ)) {
                programacionASEJ = ahoraASEJ.minusMinutes(1 + rASEJ.nextInt(600));
            }
            enASEJ(programacionASEJ, () -> {
                if (atendidosASEJ.isEmpty()) {
                    return;
                }
                RegistradoASEJ pASEJ = atendidosASEJ.get(rASEJ.nextInt(atendidosASEJ.size()));
                comoASEJ(recepcionASEJ);
                LocalDateTime desdeASEJ = objetivoASEJ.isBefore(momentoASEJ()) ? momentoASEJ().plusHours(1) : objetivoASEJ;
                Optional<LocalDateTime> huecoASEJ = citaServiceASEJ.siguienteHuecoASEJ(odontologosASEJ.get(pASEJ.odontologoASEJ()), 30, desdeASEJ, pASEJ.idASEJ());
                if (huecoASEJ.isEmpty() || huecoASEJ.get().toLocalDate().isAfter(hoyASEJ.plusDays(DIAS_VENTANAASEJ))) {
                    return;
                }
                long citaASEJ = citaServiceASEJ.programarASEJ(new CitaFormASEJ(null, pASEJ.idASEJ(), odontologosASEJ.get(pASEJ.odontologoASEJ()), huecoASEJ.get(),
                        30, rASEJ.nextBoolean() ? "Control" : "Profilaxis y control", null, null)).idASEJ();
                int azarASEJ = rASEJ.nextInt(100);
                if (huecoASEJ.get().isAfter(ahoraASEJ)) {
                    if (azarASEJ < 40) {
                        citaServiceASEJ.cambiarEstadoASEJ(citaASEJ, EstadoCitaASEJ.CONFIRMADA);
                    }
                } else if (azarASEJ < 65) {
                    enASEJ(huecoASEJ.get(), () -> controlASEJ(pASEJ, citaASEJ));
                } else if (azarASEJ < 85) {
                    enASEJ(huecoASEJ.get().plusMinutes(30), () -> {
                        comoASEJ(recepcionASEJ);
                        citaServiceASEJ.cambiarEstadoASEJ(citaASEJ, EstadoCitaASEJ.NO_ASISTIO);
                    });
                } else {
                    enASEJ(momentoASEJ().plusHours(2), () -> {
                        comoASEJ(recepcionASEJ);
                        citaServiceASEJ.cambiarEstadoASEJ(citaASEJ, EstadoCitaASEJ.CANCELADA);
                    });
                }
            });
        }

        private void controlASEJ(RegistradoASEJ pASEJ, long citaASEJ) {
            comoASEJ(adminASEJ);
            LocalDateTime tASEJ = momentoASEJ();
            long atencionASEJ = atencionServiceASEJ.abrirDesdeCitaASEJ(citaASEJ);
            planServiceASEJ.listarPorPacienteASEJ(pASEJ.idASEJ()).stream()
                    .filter(plASEJ -> plASEJ.estadoASEJ() == EstadoPlanASEJ.ACEPTADO || plASEJ.estadoASEJ() == EstadoPlanASEJ.EN_CURSO)
                    .findFirst()
                    .ifPresent(plASEJ -> realizarASEJ(atencionASEJ, planServiceASEJ.obtenerASEJ(plASEJ.idASEJ()),
                            iASEJ -> iASEJ.procedimientoCodigoASEJ().equals("PROF") || iASEJ.procedimientoCodigoASEJ().equals("DEST")));
            atencionServiceASEJ.actualizarASEJ(new AtencionFormASEJ(atencionASEJ, pASEJ.idASEJ(), null, "Control", "Encías sin signos de inflamación.",
                    "Paciente en mantenimiento.", null, null));
            fijarASEJ(tASEJ.plusMinutes(25));
            atencionServiceASEJ.cerrarASEJ(atencionASEJ);
        }

        private void completarVentanaASEJ() {
            comoASEJ(recepcionASEJ);
            List<RegistradoASEJ> candidatosASEJ = atendidosASEJ.isEmpty() ? registradosASEJ : atendidosASEJ;
            for (int intentoASEJ = 0; intentoASEJ < 200 && citasEnVentanaASEJ() < CITAS_VENTANAASEJ && !candidatosASEJ.isEmpty(); intentoASEJ++) {
                RegistradoASEJ pASEJ = candidatosASEJ.get(rASEJ.nextInt(candidatosASEJ.size()));
                LocalDateTime desdeASEJ = laborableASEJ(hoyASEJ.plusDays(rASEJ.nextInt(DIAS_VENTANAASEJ))).atTime(rASEJ.nextBoolean() ? 9 : 15, 0)
                        .plusMinutes(15L * rASEJ.nextInt(14));
                try {
                    Optional<LocalDateTime> huecoASEJ = citaServiceASEJ.siguienteHuecoASEJ(odontologosASEJ.get(pASEJ.odontologoASEJ()), 30,
                            desdeASEJ.isBefore(ahoraASEJ) ? ahoraASEJ : desdeASEJ, pASEJ.idASEJ());
                    if (huecoASEJ.isPresent() && !huecoASEJ.get().toLocalDate().isAfter(hoyASEJ.plusDays(DIAS_VENTANAASEJ))) {
                        long citaASEJ = citaServiceASEJ.programarASEJ(new CitaFormASEJ(null, pASEJ.idASEJ(), odontologosASEJ.get(pASEJ.odontologoASEJ()),
                                huecoASEJ.get(), 30, "Control", null, null)).idASEJ();
                        if (rASEJ.nextInt(3) == 0) {
                            citaServiceASEJ.cambiarEstadoASEJ(citaASEJ, EstadoCitaASEJ.CONFIRMADA);
                        }
                    }
                } catch (RuntimeException exASEJ) {
                    omitidosASEJ++;
                    logASEJ.debug("Datos demo: cita omitida: {}", exASEJ.getMessage());
                }
            }
        }

        private int citasEnVentanaASEJ() {
            return citaRepositoryASEJ.listarEntreASEJ(hoyASEJ.minusDays(DIAS_VENTANAASEJ).atStartOfDay(),
                    hoyASEJ.plusDays(DIAS_VENTANAASEJ + 1L).atStartOfDay(), EnumSet.allOf(EstadoCitaASEJ.class)).size();
        }

        private int registrarHallazgosASEJ(long odontogramaASEJ, DenticionASEJ denticionASEJ, int edadASEJ) {
            List<NuevoHallazgoASEJ> listaASEJ = new ArrayList<>();
            Set<Integer> usadasASEJ = new HashSet<>();
            if (denticionASEJ == DenticionASEJ.DECIDUA) {
                int nASEJ = 3 + rASEJ.nextInt(4);
                for (int kASEJ = 0; kASEJ < nASEJ; kASEJ++) {
                    int piezaASEJ = libreASEJ(MOLARES_DECIDUOSASEJ, usadasASEJ);
                    listaASEJ.add(kASEJ == nASEJ - 1 && rASEJ.nextBoolean()
                            ? hallazgoASEJ(odontogramaASEJ, "PULPOTOMIA", piezaASEJ, Set.of(), EstadoHallazgoASEJ.BUENO, null)
                            : cariesASEJ(odontogramaASEJ, piezaASEJ));
                }
            } else if (denticionASEJ == DenticionASEJ.MIXTA) {
                for (int kASEJ = 0; kASEJ < 2 + rASEJ.nextInt(3); kASEJ++) {
                    listaASEJ.add(hallazgoASEJ(odontogramaASEJ, "SELLANTE", libreASEJ(PRIMEROS_MOLARESASEJ, usadasASEJ), Set.of(SuperficieASEJ.O),
                            EstadoHallazgoASEJ.BUENO, null));
                }
                for (int kASEJ = 0; kASEJ < 1 + rASEJ.nextInt(3); kASEJ++) {
                    listaASEJ.add(cariesASEJ(odontogramaASEJ, libreASEJ(MOLARES_DECIDUOSASEJ, usadasASEJ)));
                }
                if (edadASEJ <= 8) {
                    listaASEJ.add(hallazgoASEJ(odontogramaASEJ, "PIEZA_EN_ERUPCION", rASEJ.nextBoolean() ? 12 : 22, Set.of(), null, null));
                }
            } else {
                int nASEJ = 3 + rASEJ.nextInt(10);
                for (int kASEJ = 0; kASEJ < nASEJ; kASEJ++) {
                    int azarASEJ = rASEJ.nextInt(100);
                    int piezaASEJ = libreASEJ(azarASEJ < 50 ? MOLARESASEJ : azarASEJ < 80 ? PREMOLARESASEJ : ANTERIORESASEJ, usadasASEJ);
                    if (piezaASEJ < 0) {
                        continue;
                    }
                    int tipoASEJ = rASEJ.nextInt(100) - Math.min(30, Math.max(0, edadASEJ - 30) / 2);
                    if (tipoASEJ >= 45) {
                        listaASEJ.add(cariesASEJ(odontogramaASEJ, piezaASEJ));
                    } else if (tipoASEJ >= 20) {
                        listaASEJ.add(hallazgoASEJ(odontogramaASEJ, "RESTAURACION_DEFINITIVA", piezaASEJ, superficiesASEJ(piezaASEJ),
                                rASEJ.nextInt(4) == 0 ? EstadoHallazgoASEJ.MALO : EstadoHallazgoASEJ.BUENO, rASEJ.nextBoolean() ? "AM" : "R"));
                    } else if (tipoASEJ >= 8 && esPosteriorASEJ(piezaASEJ)) {
                        listaASEJ.add(hallazgoASEJ(odontogramaASEJ, "PIEZA_AUSENTE_DEX", piezaASEJ, Set.of(), null, null));
                    } else if (tipoASEJ >= 0 && esPosteriorASEJ(piezaASEJ)) {
                        EstadoHallazgoASEJ estadoASEJ = rASEJ.nextInt(4) == 0 ? EstadoHallazgoASEJ.MALO : EstadoHallazgoASEJ.BUENO;
                        listaASEJ.add(hallazgoASEJ(odontogramaASEJ, "TRATAMIENTO_CONDUCTOS", piezaASEJ, Set.of(), estadoASEJ, "TC"));
                        listaASEJ.add(hallazgoASEJ(odontogramaASEJ, "CORONA", piezaASEJ, Set.of(), EstadoHallazgoASEJ.BUENO, "CMC"));
                    } else if (edadASEJ > 55 && !esPosteriorASEJ(piezaASEJ)) {
                        listaASEJ.add(hallazgoASEJ(odontogramaASEJ, "MOVILIDAD", piezaASEJ, Set.of(), null, rASEJ.nextBoolean() ? "M1" : "M2"));
                    } else {
                        listaASEJ.add(hallazgoASEJ(odontogramaASEJ, rASEJ.nextBoolean() ? "REMANENTE_RADICULAR" : "FRACTURA", piezaASEJ,
                                Set.of(), null, null));
                    }
                }
            }
            int okASEJ = 0;
            for (NuevoHallazgoASEJ hASEJ : listaASEJ) {
                if (okASEJ >= 12) {
                    break;
                }
                if (hASEJ != null && intentarASEJ(() -> odontogramaServiceASEJ.registrarHallazgoASEJ(hASEJ))) {
                    okASEJ++;
                }
            }
            int[] reservaASEJ = denticionASEJ == DenticionASEJ.PERMANENTE ? PREMOLARESASEJ : MOLARES_DECIDUOSASEJ;
            for (int intentoASEJ = 0; okASEJ < 3 && intentoASEJ < 10; intentoASEJ++) {
                NuevoHallazgoASEJ hASEJ = cariesASEJ(odontogramaASEJ, libreASEJ(reservaASEJ, usadasASEJ));
                if (hASEJ != null && intentarASEJ(() -> odontogramaServiceASEJ.registrarHallazgoASEJ(hASEJ))) {
                    okASEJ++;
                }
            }
            return okASEJ;
        }

        private NuevoHallazgoASEJ cariesASEJ(long odontogramaASEJ, int piezaASEJ) {
            if (piezaASEJ < 0) {
                return null;
            }
            String[] siglasASEJ = {"CE", "CD", "CD", "CDP"};
            return hallazgoASEJ(odontogramaASEJ, "CARIES", piezaASEJ, superficiesASEJ(piezaASEJ), null, siglasASEJ[rASEJ.nextInt(siglasASEJ.length)]);
        }

        private NuevoHallazgoASEJ hallazgoASEJ(long odontogramaASEJ, String codigoASEJ, int piezaASEJ, Set<SuperficieASEJ> superficiesASEJ,
                                       EstadoHallazgoASEJ estadoASEJ, String siglaASEJ) {
            Long tipoASEJ = tiposASEJ.get(codigoASEJ);
            return tipoASEJ == null || piezaASEJ < 0 ? null
                    : new NuevoHallazgoASEJ(odontogramaASEJ, tipoASEJ, piezaASEJ, null, superficiesASEJ, estadoASEJ, siglaASEJ, null);
        }

        private Set<SuperficieASEJ> superficiesASEJ(int piezaASEJ) {
            boolean anteriorASEJ = piezaASEJ % 10 <= 3;
            int azarASEJ = rASEJ.nextInt(100);
            if (anteriorASEJ) {
                return azarASEJ < 50 ? Set.of(rASEJ.nextBoolean() ? SuperficieASEJ.M : SuperficieASEJ.D) : Set.of(SuperficieASEJ.V);
            }
            if (azarASEJ < 50) {
                return Set.of(SuperficieASEJ.O);
            }
            if (azarASEJ < 85) {
                return Set.of(SuperficieASEJ.O, rASEJ.nextBoolean() ? SuperficieASEJ.M : SuperficieASEJ.D);
            }
            return Set.of(SuperficieASEJ.M, SuperficieASEJ.O, SuperficieASEJ.D);
        }

        private int libreASEJ(int[] piezasASEJ, Set<Integer> usadasASEJ) {
            for (int intentoASEJ = 0; intentoASEJ < 12; intentoASEJ++) {
                int pASEJ = piezasASEJ[rASEJ.nextInt(piezasASEJ.length)];
                if (usadasASEJ.add(pASEJ)) {
                    return pASEJ;
                }
            }
            return -1;
        }

        private static boolean esPosteriorASEJ(int piezaASEJ) {
            return piezaASEJ % 10 >= 4;
        }

        private void realizarASEJ(long atencionASEJ, PlanDtoASEJ planASEJ, Predicate<PlanItemDtoASEJ> filtroASEJ) {
            if (planASEJ.estadoASEJ() != EstadoPlanASEJ.ACEPTADO && planASEJ.estadoASEJ() != EstadoPlanASEJ.EN_CURSO) {
                return;
            }
            for (PlanItemDtoASEJ iASEJ : planASEJ.itemsASEJ()) {
                if (iASEJ.estadoASEJ() == EstadoPlanItemASEJ.PENDIENTE && filtroASEJ.test(iASEJ)) {
                    intentarASEJ(() -> planServiceASEJ.marcarEnAtencionASEJ(atencionASEJ, iASEJ.idASEJ(), true));
                }
            }
        }

        private void pagarASEJ(long pacienteIdASEJ, boolean parcialASEJ) {
            List<CuotaPacienteDtoASEJ> pendientesASEJ = pagoServiceASEJ.cuotasPendientesASEJ(pacienteIdASEJ);
            if (pendientesASEJ.isEmpty()) {
                return;
            }
            BigDecimal saldoASEJ = pendientesASEJ.get(0).saldoASEJ();
            BigDecimal montoASEJ = parcialASEJ ? saldoASEJ.divide(BigDecimal.TWO, 0, RoundingMode.DOWN).max(BigDecimal.ONE) : saldoASEJ;
            int azarASEJ = rASEJ.nextInt(100);
            MedioPagoASEJ medioASEJ = azarASEJ < 45 ? MedioPagoASEJ.EFECTIVO : azarASEJ < 70 ? MedioPagoASEJ.YAPE : azarASEJ < 82 ? MedioPagoASEJ.TARJETA
                    : azarASEJ < 92 ? MedioPagoASEJ.PLIN : MedioPagoASEJ.TRANSFERENCIA;
            String referenciaASEJ = medioASEJ == MedioPagoASEJ.EFECTIVO ? null : "OP-" + (100000 + rASEJ.nextInt(900000));
            pagoServiceASEJ.registrarASEJ(new PagoFormASEJ(pacienteIdASEJ, montoASEJ, medioASEJ, referenciaASEJ));
            pagosASEJ++;
        }

        private void enASEJ(LocalDateTime momentoASEJ, Runnable accionASEJ) {
            if (!momentoASEJ.isAfter(ahoraASEJ)) {
                colaASEJ.add(new EventoASEJ(momentoASEJ, ordenASEJ++, accionASEJ));
            }
        }

        private LocalDateTime momentoASEJ() {
            return LocalDateTime.now(relojASEJ);
        }

        private void fijarASEJ(LocalDateTime tASEJ) {
            relojASEJ.fijarASEJ(tASEJ.isAfter(ahoraASEJ) ? ahoraASEJ : tASEJ);
        }

        private boolean intentarASEJ(Runnable pasoASEJ) {
            try {
                pasoASEJ.run();
                return true;
            } catch (RuntimeException exASEJ) {
                logASEJ.debug("Datos demo: {}", exASEJ.getMessage());
                return false;
            }
        }

        private void comoASEJ(UsuarioSesionASEJ usuarioASEJ) {
            sesionUsuarioASEJ.iniciarASEJ(usuarioASEJ);
        }

        private UsuarioSesionASEJ sesionASEJ(String usernameASEJ) {
            UsuarioASEJ uASEJ = usuarioRepositoryASEJ.findByUsernameASEJIgnoreCase(usernameASEJ).orElseThrow();
            return new UsuarioSesionASEJ(uASEJ.getIdASEJ(), uASEJ.getUsernameASEJ(), uASEJ.getNombreCompletoASEJ(), uASEJ.getRolASEJ().name(),
                    uASEJ.getOdontologoASEJ() == null ? null : uASEJ.getOdontologoASEJ().getIdASEJ());
        }

        private static LocalDate laborableASEJ(LocalDate diaASEJ) {
            return diaASEJ.getDayOfWeek() == DayOfWeek.SUNDAY ? diaASEJ.plusDays(1) : diaASEJ;
        }
    }
}
