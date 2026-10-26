package com.odontogest.servicio;

import com.odontogest.dominio.agenda.BloqueHorarioASEJ;
import com.odontogest.dominio.agenda.BuscadorHuecosASEJ;
import com.odontogest.dominio.agenda.IntervaloASEJ;
import com.odontogest.evento.CitaCambiadaASEJ;
import com.odontogest.excepcion.NegocioExceptionASEJ;
import com.odontogest.modelo.AtencionASEJ;
import com.odontogest.modelo.CitaASEJ;
import com.odontogest.modelo.EstadoCitaASEJ;
import com.odontogest.modelo.EstadoPlanASEJ;
import com.odontogest.modelo.EstadoPlanItemASEJ;
import com.odontogest.modelo.OdontologoASEJ;
import com.odontogest.modelo.PacienteASEJ;
import com.odontogest.modelo.PlanItemASEJ;
import com.odontogest.persistencia.repositorio.AtencionRepositoryASEJ;
import com.odontogest.persistencia.repositorio.CitaRepositoryASEJ;
import com.odontogest.persistencia.repositorio.HorarioAtencionRepositoryASEJ;
import com.odontogest.persistencia.repositorio.OdontologoRepositoryASEJ;
import com.odontogest.persistencia.repositorio.PacienteRepositoryASEJ;
import com.odontogest.persistencia.repositorio.PlanItemRepositoryASEJ;
import com.odontogest.persistencia.repositorio.PlanTratamientoRepositoryASEJ;
import com.odontogest.seguridad.PermisoASEJ;
import com.odontogest.seguridad.SesionUsuarioASEJ;
import com.odontogest.servicio.dto.AgendaDtoASEJ;
import com.odontogest.servicio.dto.CitaDtoASEJ;
import com.odontogest.servicio.dto.CitaFormASEJ;
import com.odontogest.servicio.dto.ItemPlanPendienteDtoASEJ;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CitaServiceASEJ {

    public static final int DURACION_MAXIMA_MINASEJ = 240;
    private static final DateTimeFormatter HORAASEJ = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FECHA_HORAASEJ = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Clock relojASEJ;
    private final CitaRepositoryASEJ citaRepositoryASEJ;
    private final AuditoriaServiceASEJ auditoriaASEJ;
    private final PacienteRepositoryASEJ pacienteRepositoryASEJ;
    private final OdontologoRepositoryASEJ odontologoRepositoryASEJ;
    private final HorarioAtencionRepositoryASEJ horarioRepositoryASEJ;
    private final AtencionRepositoryASEJ atencionRepositoryASEJ;
    private final PlanItemRepositoryASEJ planItemRepositoryASEJ;
    private final PlanTratamientoRepositoryASEJ planRepositoryASEJ;
    private final OdontologoServiceASEJ odontologoServiceASEJ;
    private final ConfiguracionServiceASEJ configuracionServiceASEJ;
    private final SesionUsuarioASEJ sesionUsuarioASEJ;
    private final ApplicationEventPublisher eventosASEJ;

    private final BuscadorHuecosASEJ buscadorASEJ = new BuscadorHuecosASEJ();

    public AgendaDtoASEJ agendaASEJ(long odontologoIdASEJ, LocalDate desdeASEJ, int diasASEJ) {
        List<CitaASEJ> citasASEJ = citaRepositoryASEJ.listarOdontologoASEJ(odontologoIdASEJ, desdeASEJ.atStartOfDay(),
                desdeASEJ.plusDays(diasASEJ).atStartOfDay(), EnumSet.complementOf(EnumSet.of(EstadoCitaASEJ.CANCELADA)));
        return new AgendaDtoASEJ(desdeASEJ, diasASEJ, odontologoServiceASEJ.obtenerASEJ(odontologoIdASEJ), dtosASEJ(citasASEJ), intervaloMinASEJ());
    }

    public List<CitaDtoASEJ> delDiaASEJ(LocalDate diaASEJ, Long odontologoIdASEJ) {
        Set<EstadoCitaASEJ> estadosASEJ = EnumSet.complementOf(EnumSet.of(EstadoCitaASEJ.CANCELADA));
        LocalDateTime desdeASEJ = diaASEJ.atStartOfDay();
        LocalDateTime hastaASEJ = diaASEJ.plusDays(1).atStartOfDay();
        return dtosASEJ(odontologoIdASEJ == null ? citaRepositoryASEJ.listarEntreASEJ(desdeASEJ, hastaASEJ, estadosASEJ)
                : citaRepositoryASEJ.listarOdontologoASEJ(odontologoIdASEJ, desdeASEJ, hastaASEJ, estadosASEJ).stream()
                        .filter(cASEJ -> !cASEJ.getInicioASEJ().isBefore(desdeASEJ)).toList());
    }

    public CitaDtoASEJ obtenerASEJ(long citaIdASEJ) {
        return dtosASEJ(List.of(buscarASEJ(citaIdASEJ))).get(0);
    }

    public List<CitaDtoASEJ> proximasDelPacienteASEJ(long pacienteIdASEJ) {
        LocalDateTime ahoraASEJ = LocalDateTime.now(relojASEJ);
        return dtosASEJ(citaRepositoryASEJ.listarPacienteASEJ(pacienteIdASEJ, ahoraASEJ, ahoraASEJ.plusYears(2),
                EnumSet.of(EstadoCitaASEJ.PROGRAMADA, EstadoCitaASEJ.CONFIRMADA)));
    }

    public List<ItemPlanPendienteDtoASEJ> itemsPendientesASEJ(long pacienteIdASEJ) {
        List<Long> planesASEJ = planRepositoryASEJ.findByPacienteASEJIdASEJAndEstadoASEJIn(pacienteIdASEJ,
                EnumSet.of(EstadoPlanASEJ.ACEPTADO, EstadoPlanASEJ.EN_CURSO)).stream().map(pASEJ -> pASEJ.getIdASEJ()).toList();
        if (planesASEJ.isEmpty()) {
            return List.of();
        }
        return planItemRepositoryASEJ.listarPorPlanesASEJ(planesASEJ).stream()
                .filter(iASEJ -> iASEJ.getEstadoASEJ() == EstadoPlanItemASEJ.PENDIENTE || iASEJ.getEstadoASEJ() == EstadoPlanItemASEJ.EN_PROCESO)
                .map(iASEJ -> new ItemPlanPendienteDtoASEJ(iASEJ.getIdASEJ(), iASEJ.getPlanASEJ().getIdASEJ(), descripcionASEJ(iASEJ)))
                .toList();
    }

    public Optional<LocalDateTime> siguienteHuecoASEJ(long odontologoIdASEJ, int duracionMinASEJ, LocalDateTime desdeASEJ, Long pacienteIdASEJ) {
        validarDuracionASEJ(duracionMinASEJ);
        LocalDateTime inicioASEJ = desdeASEJ.isBefore(LocalDateTime.now(relojASEJ)) ? LocalDateTime.now(relojASEJ) : desdeASEJ;
        int diasASEJ = configuracionServiceASEJ.enteroASEJ("agenda.dias_busqueda_hueco");
        LocalDateTime hastaASEJ = inicioASEJ.toLocalDate().plusDays(diasASEJ + 1L).atStartOfDay();
        List<IntervaloASEJ> ocupadosASEJ = new ArrayList<>();
        citaRepositoryASEJ.listarOdontologoASEJ(odontologoIdASEJ, inicioASEJ, hastaASEJ, EstadoCitaASEJ.ocupanASEJ())
                .forEach(cASEJ -> ocupadosASEJ.add(new IntervaloASEJ(cASEJ.getInicioASEJ(), cASEJ.getFinASEJ())));
        if (pacienteIdASEJ != null) {
            citaRepositoryASEJ.listarPacienteASEJ(pacienteIdASEJ, inicioASEJ, hastaASEJ, EstadoCitaASEJ.ocupanASEJ())
                    .forEach(cASEJ -> ocupadosASEJ.add(new IntervaloASEJ(cASEJ.getInicioASEJ(), cASEJ.getFinASEJ())));
        }
        return buscadorASEJ.siguienteHuecoASEJ(inicioASEJ, Duration.ofMinutes(duracionMinASEJ), Duration.ofMinutes(intervaloMinASEJ()),
                horarioASEJ(odontologoIdASEJ), ocupadosASEJ, diasASEJ).map(IntervaloASEJ::inicioASEJ);
    }

    public int intervaloMinASEJ() {
        return configuracionServiceASEJ.enteroASEJ("agenda.intervalo_min");
    }

    public int duracionPorDefectoMinASEJ() {
        return configuracionServiceASEJ.enteroASEJ("agenda.duracion_cita_min");
    }

    @Transactional
    public CitaDtoASEJ programarASEJ(CitaFormASEJ formASEJ) {
        long usuarioIdASEJ = sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.AGENDA_GESTIONAR).idASEJ();
        String motivoASEJ = ValidadorASEJ.limpioASEJ(formASEJ.motivoASEJ());
        String notasASEJ = ValidadorASEJ.limpioMultilineaASEJ(formASEJ.notasASEJ());
        new ValidadorASEJ()
                .requeridoASEJ(formASEJ.pacienteIdASEJ(), "el paciente")
                .requeridoASEJ(formASEJ.odontologoIdASEJ(), "el odontólogo")
                .requeridoASEJ(formASEJ.inicioASEJ(), "la fecha y hora")
                .requeridoASEJ(motivoASEJ, "el motivo")
                .maximoASEJ(motivoASEJ, 200, "motivo")
                .maximoASEJ(notasASEJ, 500, "notas")
                .lanzarSiHayErroresASEJ();
        CitaASEJ citaASEJ;
        if (formASEJ.idASEJ() == null) {
            citaASEJ = new CitaASEJ();
            citaASEJ.setEstadoASEJ(EstadoCitaASEJ.PROGRAMADA);
            citaASEJ.setCreadoPorASEJ(usuarioIdASEJ);
            citaASEJ.setCreadoEnASEJ(ahoraASEJ());
        } else {
            citaASEJ = activaASEJ(formASEJ.idASEJ());
        }
        PacienteASEJ pacienteASEJ = pacienteRepositoryASEJ.findById(formASEJ.pacienteIdASEJ())
                .orElseThrow(() -> new NegocioExceptionASEJ("El paciente ya no existe en la base de datos."));
        if (!pacienteASEJ.isActivoASEJ()) {
            throw new NegocioExceptionASEJ("El paciente está inactivo: reactívelo antes de darle una cita.");
        }
        OdontologoASEJ odontologoASEJ = odontologoASEJ(formASEJ.odontologoIdASEJ());
        LocalDateTime inicioASEJ = formASEJ.inicioASEJ().truncatedTo(ChronoUnit.MINUTES);
        boolean cambiaHorarioASEJ = formASEJ.idASEJ() == null || !inicioASEJ.equals(citaASEJ.getInicioASEJ())
                || formASEJ.duracionMinASEJ() != Duration.between(citaASEJ.getInicioASEJ(), citaASEJ.getFinASEJ()).toMinutes()
                || !odontologoASEJ.getIdASEJ().equals(citaASEJ.getOdontologoASEJ().getIdASEJ())
                || !pacienteASEJ.getIdASEJ().equals(citaASEJ.getPacienteASEJ().getIdASEJ());
        LocalDateTime finASEJ = validarIntervaloASEJ(citaASEJ.getIdASEJ(), pacienteASEJ.getIdASEJ(), odontologoASEJ, inicioASEJ, formASEJ.duracionMinASEJ(), cambiaHorarioASEJ);
        citaASEJ.setPacienteASEJ(pacienteASEJ);
        citaASEJ.setOdontologoASEJ(odontologoASEJ);
        citaASEJ.setInicioASEJ(inicioASEJ);
        citaASEJ.setFinASEJ(finASEJ);
        citaASEJ.setMotivoASEJ(motivoASEJ);
        citaASEJ.setNotasASEJ(notasASEJ);
        citaASEJ.setPlanItemIdASEJ(validarItemASEJ(formASEJ.planItemIdASEJ(), pacienteASEJ.getIdASEJ()));
        CitaASEJ guardadaASEJ = citaRepositoryASEJ.save(citaASEJ);
        logASEJ.info("Cita {} {} para {} el {}", guardadaASEJ.getIdASEJ(), formASEJ.idASEJ() == null ? "programada" : "actualizada",
                pacienteASEJ.getNumeroHistoriaASEJ(), FECHA_HORAASEJ.format(inicioASEJ));
        auditoriaASEJ.registrarASEJ(formASEJ.idASEJ() == null ? "CITA_PROGRAMAR" : "CITA_EDITAR", "cita", guardadaASEJ.getIdASEJ(),
                Map.of("inicio", inicioASEJ.toString()));
        publicarASEJ(guardadaASEJ);
        return obtenerASEJ(guardadaASEJ.getIdASEJ());
    }

    @Transactional
    public CitaDtoASEJ moverASEJ(long citaIdASEJ, LocalDateTime nuevoInicioASEJ) {
        sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.AGENDA_GESTIONAR);
        CitaASEJ citaASEJ = activaASEJ(citaIdASEJ);
        LocalDateTime inicioASEJ = nuevoInicioASEJ.truncatedTo(ChronoUnit.MINUTES);
        int duracionASEJ = (int) Duration.between(citaASEJ.getInicioASEJ(), citaASEJ.getFinASEJ()).toMinutes();
        citaASEJ.setFinASEJ(validarIntervaloASEJ(citaASEJ.getIdASEJ(), citaASEJ.getPacienteASEJ().getIdASEJ(), citaASEJ.getOdontologoASEJ(), inicioASEJ, duracionASEJ, true));
        citaASEJ.setInicioASEJ(inicioASEJ);
        logASEJ.info("Cita {} movida al {}", citaIdASEJ, FECHA_HORAASEJ.format(inicioASEJ));
        auditoriaASEJ.registrarASEJ("CITA_MOVER", "cita", citaIdASEJ, Map.of("inicio", inicioASEJ.toString()));
        publicarASEJ(citaASEJ);
        return obtenerASEJ(citaIdASEJ);
    }

    @Transactional
    public CitaDtoASEJ cambiarEstadoASEJ(long citaIdASEJ, EstadoCitaASEJ destinoASEJ) {
        sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.AGENDA_GESTIONAR);
        CitaASEJ citaASEJ = buscarASEJ(citaIdASEJ);
        if (destinoASEJ == EstadoCitaASEJ.ATENDIDA) {
            throw new NegocioExceptionASEJ("La cita queda atendida al cerrar la atención abierta desde ella.");
        }
        if (!citaASEJ.getEstadoASEJ().puedePasarAASEJ(destinoASEJ)) {
            throw new NegocioExceptionASEJ("Una cita " + citaASEJ.getEstadoASEJ().etiquetaASEJ().toLowerCase() + " no puede pasar a "
                    + destinoASEJ.etiquetaASEJ().toLowerCase() + ".");
        }
        if (destinoASEJ == EstadoCitaASEJ.NO_ASISTIO && citaASEJ.getInicioASEJ().isAfter(LocalDateTime.now(relojASEJ))) {
            throw new NegocioExceptionASEJ("La cita aún no empieza: no se puede marcar como inasistencia.");
        }
        if (!atencionRepositoryASEJ.findByCitaIdASEJIn(List.of(citaIdASEJ)).isEmpty()) {
            throw new NegocioExceptionASEJ("La cita ya tiene una atención abierta: ciérrela desde la pestaña Atenciones.");
        }
        citaASEJ.setEstadoASEJ(destinoASEJ);
        logASEJ.info("Cita {} → {}", citaIdASEJ, destinoASEJ);
        auditoriaASEJ.registrarASEJ("CITA_ESTADO", "cita", citaIdASEJ, Map.of("estado", destinoASEJ.name()));
        publicarASEJ(citaASEJ);
        return obtenerASEJ(citaIdASEJ);
    }

    @Transactional
    public void marcarAtendidaASEJ(long citaIdASEJ) {
        CitaASEJ citaASEJ = buscarASEJ(citaIdASEJ);
        if (citaASEJ.getEstadoASEJ().activaASEJ()) {
            citaASEJ.setEstadoASEJ(EstadoCitaASEJ.ATENDIDA);
            publicarASEJ(citaASEJ);
        }
    }

    private LocalDateTime validarIntervaloASEJ(Long citaIdASEJ, long pacienteIdASEJ, OdontologoASEJ odontologoASEJ, LocalDateTime inicioASEJ,
                                           int duracionMinASEJ, boolean cambiaHorarioASEJ) {
        validarDuracionASEJ(duracionMinASEJ);
        LocalDateTime finASEJ = inicioASEJ.plusMinutes(duracionMinASEJ);
        if (!cambiaHorarioASEJ) {
            return finASEJ;
        }
        int intervaloASEJ = intervaloMinASEJ();
        if (inicioASEJ.getMinute() % intervaloASEJ != 0) {
            throw new NegocioExceptionASEJ("La hora debe coincidir con la grilla de " + intervaloASEJ + " minutos (p. ej. 09:00, 09:"
                    + String.format("%02d", intervaloASEJ) + ").");
        }
        if (inicioASEJ.isBefore(LocalDateTime.now(relojASEJ))) {
            throw new NegocioExceptionASEJ("No se puede programar una cita en el pasado.");
        }
        IntervaloASEJ citaASEJ = new IntervaloASEJ(inicioASEJ, finASEJ);
        if (!BloqueHorarioASEJ.dentroDelHorarioASEJ(citaASEJ, horarioASEJ(odontologoASEJ.getIdASEJ()))) {
            throw new NegocioExceptionASEJ("El " + FECHA_HORAASEJ.format(inicioASEJ) + "–" + HORAASEJ.format(finASEJ) + " está fuera del horario de "
                    + "atención de " + odontologoASEJ.getNombreCompletoASEJ() + ".");
        }
        long excluirASEJ = citaIdASEJ == null ? -1 : citaIdASEJ;
        if (citaRepositoryASEJ.existeSolapamientoOdontologoASEJ(odontologoASEJ.getIdASEJ(), inicioASEJ, finASEJ, excluirASEJ, EstadoCitaASEJ.ocupanASEJ())) {
            throw new NegocioExceptionASEJ(odontologoASEJ.getNombreCompletoASEJ() + " ya tiene una cita que se cruza con ese horario.");
        }
        if (citaRepositoryASEJ.existeSolapamientoPacienteASEJ(pacienteIdASEJ, inicioASEJ, finASEJ, excluirASEJ, EstadoCitaASEJ.ocupanASEJ())) {
            throw new NegocioExceptionASEJ("El paciente ya tiene otra cita que se cruza con ese horario.");
        }
        return finASEJ;
    }

    private void validarDuracionASEJ(int duracionMinASEJ) {
        int intervaloASEJ = intervaloMinASEJ();
        if (duracionMinASEJ < intervaloASEJ || duracionMinASEJ > DURACION_MAXIMA_MINASEJ || duracionMinASEJ % intervaloASEJ != 0) {
            throw new NegocioExceptionASEJ("La duración debe ser múltiplo de " + intervaloASEJ + " minutos, entre " + intervaloASEJ
                    + " y " + DURACION_MAXIMA_MINASEJ + ".");
        }
    }

    private Long validarItemASEJ(Long planItemIdASEJ, long pacienteIdASEJ) {
        if (planItemIdASEJ == null) {
            return null;
        }
        PlanItemASEJ itemASEJ = planItemRepositoryASEJ.findById(planItemIdASEJ)
                .orElseThrow(() -> new NegocioExceptionASEJ("El procedimiento del plan ya no existe."));
        if (!itemASEJ.getPlanASEJ().getPacienteASEJ().getIdASEJ().equals(pacienteIdASEJ)) {
            throw new NegocioExceptionASEJ("El procedimiento elegido es del plan de otro paciente.");
        }
        return planItemIdASEJ;
    }

    private List<BloqueHorarioASEJ> horarioASEJ(long odontologoIdASEJ) {
        return horarioRepositoryASEJ.findByOdontologoASEJIdASEJOrderByDiaSemanaASEJAscHoraInicioASEJAsc(odontologoIdASEJ).stream()
                .map(hASEJ -> new BloqueHorarioASEJ(hASEJ.getDiaSemanaASEJ(), hASEJ.getHoraInicioASEJ(), hASEJ.getHoraFinASEJ()))
                .toList();
    }

    private OdontologoASEJ odontologoASEJ(long idASEJ) {
        OdontologoASEJ oASEJ = odontologoRepositoryASEJ.findById(idASEJ)
                .orElseThrow(() -> new NegocioExceptionASEJ("El odontólogo ya no existe en la base de datos."));
        if (!oASEJ.isActivoASEJ()) {
            throw new NegocioExceptionASEJ("El odontólogo " + oASEJ.getNombreCompletoASEJ() + " está inactivo.");
        }
        return oASEJ;
    }

    private CitaASEJ buscarASEJ(long idASEJ) {
        return citaRepositoryASEJ.findById(idASEJ).orElseThrow(() -> new NegocioExceptionASEJ("La cita ya no existe en la base de datos."));
    }

    private CitaASEJ activaASEJ(long idASEJ) {
        CitaASEJ cASEJ = buscarASEJ(idASEJ);
        if (!cASEJ.getEstadoASEJ().activaASEJ()) {
            throw new NegocioExceptionASEJ("La cita está " + cASEJ.getEstadoASEJ().etiquetaASEJ().toLowerCase() + ": ya no se puede modificar.");
        }
        return cASEJ;
    }

    private void publicarASEJ(CitaASEJ cASEJ) {
        eventosASEJ.publishEvent(new CitaCambiadaASEJ(cASEJ.getIdASEJ(), cASEJ.getPacienteASEJ().getIdASEJ(), cASEJ.getOdontologoASEJ().getIdASEJ()));
    }

    private List<CitaDtoASEJ> dtosASEJ(List<CitaASEJ> citasASEJ) {
        if (citasASEJ.isEmpty()) {
            return List.of();
        }
        List<Long> idsASEJ = citasASEJ.stream().map(CitaASEJ::getIdASEJ).toList();
        Map<Long, Long> atencionesASEJ = atencionRepositoryASEJ.findByCitaIdASEJIn(idsASEJ).stream()
                .collect(Collectors.toMap(AtencionASEJ::getCitaIdASEJ, AtencionASEJ::getIdASEJ, (aASEJ, bASEJ) -> aASEJ));
        Set<Long> itemIdsASEJ = citasASEJ.stream().map(CitaASEJ::getPlanItemIdASEJ).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> itemsASEJ = itemIdsASEJ.isEmpty() ? Map.of() : planItemRepositoryASEJ.findAllById(itemIdsASEJ).stream()
                .collect(Collectors.toMap(PlanItemASEJ::getIdASEJ, CitaServiceASEJ::descripcionASEJ));
        Map<Long, CitaASEJ> porIdASEJ = citasASEJ.stream().collect(Collectors.toMap(CitaASEJ::getIdASEJ, Function.identity()));
        return idsASEJ.stream().map(porIdASEJ::get).map(cASEJ -> new CitaDtoASEJ(cASEJ.getIdASEJ(), cASEJ.getPacienteASEJ().getIdASEJ(),
                        cASEJ.getPacienteASEJ().getNombreCompletoASEJ(), cASEJ.getPacienteASEJ().getNumeroHistoriaASEJ(), cASEJ.getPacienteASEJ().getTelefonoASEJ(),
                        cASEJ.getOdontologoASEJ().getIdASEJ(), cASEJ.getOdontologoASEJ().getNombreCompletoASEJ(), cASEJ.getOdontologoASEJ().getColorAgendaASEJ(),
                        cASEJ.getInicioASEJ(), cASEJ.getFinASEJ(), cASEJ.getMotivoASEJ(), cASEJ.getEstadoASEJ(), cASEJ.getPlanItemIdASEJ(),
                        cASEJ.getPlanItemIdASEJ() == null ? null : itemsASEJ.get(cASEJ.getPlanItemIdASEJ()), cASEJ.getNotasASEJ(), atencionesASEJ.get(cASEJ.getIdASEJ())))
                .toList();
    }

    private static String descripcionASEJ(PlanItemASEJ iASEJ) {
        StringBuilder sbASEJ = new StringBuilder(iASEJ.getProcedimientoASEJ().getNombreASEJ());
        if (iASEJ.getPiezaFdiASEJ() != null) {
            sbASEJ.append(" · pieza ").append(iASEJ.getPiezaFdiASEJ());
        }
        return sbASEJ.toString();
    }

    private LocalDateTime ahoraASEJ() {
        return LocalDateTime.now(relojASEJ).truncatedTo(ChronoUnit.SECONDS);
    }
}
