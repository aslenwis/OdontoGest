/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio;

import com.odontogest.dominio.agenda.BloqueHorario;
import com.odontogest.dominio.agenda.BuscadorHuecos;
import com.odontogest.dominio.agenda.Intervalo;
import com.odontogest.evento.CitaCambiada;
import com.odontogest.excepcion.NegocioException;
import com.odontogest.modelo.Atencion;
import com.odontogest.modelo.Cita;
import com.odontogest.modelo.EstadoCita;
import com.odontogest.modelo.EstadoPlan;
import com.odontogest.modelo.EstadoPlanItem;
import com.odontogest.modelo.Odontologo;
import com.odontogest.modelo.Paciente;
import com.odontogest.modelo.PlanItem;
import com.odontogest.persistencia.repositorio.AtencionRepository;
import com.odontogest.persistencia.repositorio.CitaRepository;
import com.odontogest.persistencia.repositorio.HorarioAtencionRepository;
import com.odontogest.persistencia.repositorio.OdontologoRepository;
import com.odontogest.persistencia.repositorio.PacienteRepository;
import com.odontogest.persistencia.repositorio.PlanItemRepository;
import com.odontogest.persistencia.repositorio.PlanTratamientoRepository;
import com.odontogest.seguridad.Permiso;
import com.odontogest.seguridad.SesionUsuario;
import com.odontogest.servicio.dto.AgendaDto;
import com.odontogest.servicio.dto.CitaDto;
import com.odontogest.servicio.dto.CitaForm;
import com.odontogest.servicio.dto.ItemPlanPendienteDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

/**
 * Agenda (RF-AGE-01 a RF-AGE-05): citas de 15 en 15 minutos dentro del horario de atención del odontólogo, sin
 * solapamientos del mismo odontólogo ni del mismo paciente, búsqueda del siguiente hueco libre y estados.
 * <ul>
 *   <li>Las citas canceladas o a las que el paciente no asistió liberan el horario.</li>
 *   <li>Solo se programan o mueven citas hacia el futuro; la duración es múltiplo del intervalo de la grilla.</li>
 *   <li>La cita pasa a ATENDIDA al cerrar la atención abierta desde ella ({@code AtencionService}).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CitaService {

    public static final int DURACION_MAXIMA_MIN = 240;
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final CitaRepository citaRepository;
    private final PacienteRepository pacienteRepository;
    private final OdontologoRepository odontologoRepository;
    private final HorarioAtencionRepository horarioRepository;
    private final AtencionRepository atencionRepository;
    private final PlanItemRepository planItemRepository;
    private final PlanTratamientoRepository planRepository;
    private final OdontologoService odontologoService;
    private final ConfiguracionService configuracionService;
    private final SesionUsuario sesionUsuario;
    private final ApplicationEventPublisher eventos;

    private final BuscadorHuecos buscador = new BuscadorHuecos();

    // ------------------------------------------------------------------ consultas

    /** Agenda de un odontólogo desde {@code desde} durante {@code dias} días (sin las citas canceladas). */
    public AgendaDto agenda(long odontologoId, LocalDate desde, int dias) {
        List<Cita> citas = citaRepository.listarOdontologo(odontologoId, desde.atStartOfDay(),
                desde.plusDays(dias).atStartOfDay(), EnumSet.complementOf(EnumSet.of(EstadoCita.CANCELADA)));
        return new AgendaDto(desde, dias, odontologoService.obtener(odontologoId), dtos(citas), intervaloMin());
    }

    public CitaDto obtener(long citaId) {
        return dtos(List.of(buscar(citaId))).get(0);
    }

    /** Próximas citas activas del paciente. */
    public List<CitaDto> proximasDelPaciente(long pacienteId) {
        LocalDateTime ahora = LocalDateTime.now();
        return dtos(citaRepository.listarPaciente(pacienteId, ahora, ahora.plusYears(2),
                EnumSet.of(EstadoCita.PROGRAMADA, EstadoCita.CONFIRMADA)));
    }

    /** Procedimientos pendientes de los planes aceptados o en curso del paciente (para asociarlos a la cita). */
    public List<ItemPlanPendienteDto> itemsPendientes(long pacienteId) {
        List<Long> planes = planRepository.findByPacienteIdAndEstadoIn(pacienteId,
                EnumSet.of(EstadoPlan.ACEPTADO, EstadoPlan.EN_CURSO)).stream().map(p -> p.getId()).toList();
        if (planes.isEmpty()) {
            return List.of();
        }
        return planItemRepository.listarPorPlanes(planes).stream()
                .filter(i -> i.getEstado() == EstadoPlanItem.PENDIENTE || i.getEstado() == EstadoPlanItem.EN_PROCESO)
                .map(i -> new ItemPlanPendienteDto(i.getId(), i.getPlan().getId(), descripcion(i)))
                .toList();
    }

    /**
     * Siguiente hueco libre del odontólogo (RF-AGE-04) a partir de {@code desde}, dentro de su horario y sin cruzarse
     * con otras citas del odontólogo ni, si se indica, del paciente.
     *
     * @return inicio del hueco, o vacío si no hay en los próximos {@code agenda.dias_busqueda_hueco} días
     */
    public Optional<LocalDateTime> siguienteHueco(long odontologoId, int duracionMin, LocalDateTime desde, Long pacienteId) {
        validarDuracion(duracionMin);
        LocalDateTime inicio = desde.isBefore(LocalDateTime.now()) ? LocalDateTime.now() : desde;
        int dias = configuracionService.entero("agenda.dias_busqueda_hueco");
        LocalDateTime hasta = inicio.toLocalDate().plusDays(dias + 1L).atStartOfDay();
        List<Intervalo> ocupados = new ArrayList<>();
        citaRepository.listarOdontologo(odontologoId, inicio, hasta, EstadoCita.ocupan())
                .forEach(c -> ocupados.add(new Intervalo(c.getInicio(), c.getFin())));
        if (pacienteId != null) {
            citaRepository.listarPaciente(pacienteId, inicio, hasta, EstadoCita.ocupan())
                    .forEach(c -> ocupados.add(new Intervalo(c.getInicio(), c.getFin())));
        }
        return buscador.siguienteHueco(inicio, Duration.ofMinutes(duracionMin), Duration.ofMinutes(intervaloMin()),
                horario(odontologoId), ocupados, dias).map(Intervalo::inicio);
    }

    public int intervaloMin() {
        return configuracionService.entero("agenda.intervalo_min");
    }

    public int duracionPorDefectoMin() {
        return configuracionService.entero("agenda.duracion_cita_min");
    }

    // ------------------------------------------------------------------ escritura

    /** Programa una cita nueva o edita una existente (RF-AGE-02, RF-AGE-03). */
    @Transactional
    public CitaDto programar(CitaForm form) {
        long usuarioId = sesionUsuario.exigir(Permiso.AGENDA_GESTIONAR).id();
        String motivo = Validador.limpio(form.motivo());
        String notas = Validador.limpioMultilinea(form.notas());
        new Validador()
                .requerido(form.pacienteId(), "el paciente")
                .requerido(form.odontologoId(), "el odontólogo")
                .requerido(form.inicio(), "la fecha y hora")
                .requerido(motivo, "el motivo")
                .maximo(motivo, 200, "motivo")
                .maximo(notas, 500, "notas")
                .lanzarSiHayErrores();
        Cita cita;
        if (form.id() == null) {
            cita = new Cita();
            cita.setEstado(EstadoCita.PROGRAMADA);
            cita.setCreadoPor(usuarioId);
            cita.setCreadoEn(ahora());
        } else {
            cita = activa(form.id());
        }
        Paciente paciente = pacienteRepository.findById(form.pacienteId())
                .orElseThrow(() -> new NegocioException("El paciente ya no existe en la base de datos."));
        if (!paciente.isActivo()) {
            throw new NegocioException("El paciente está inactivo: reactívelo antes de darle una cita.");
        }
        Odontologo odontologo = odontologo(form.odontologoId());
        LocalDateTime inicio = form.inicio().truncatedTo(ChronoUnit.MINUTES);
        boolean cambiaHorario = form.id() == null || !inicio.equals(cita.getInicio())
                || form.duracionMin() != Duration.between(cita.getInicio(), cita.getFin()).toMinutes()
                || !odontologo.getId().equals(cita.getOdontologo().getId())
                || !paciente.getId().equals(cita.getPaciente().getId());
        LocalDateTime fin = validarIntervalo(cita.getId(), paciente.getId(), odontologo, inicio, form.duracionMin(), cambiaHorario);
        cita.setPaciente(paciente);
        cita.setOdontologo(odontologo);
        cita.setInicio(inicio);
        cita.setFin(fin);
        cita.setMotivo(motivo);
        cita.setNotas(notas);
        cita.setPlanItemId(validarItem(form.planItemId(), paciente.getId()));
        Cita guardada = citaRepository.save(cita);
        log.info("Cita {} {} para {} el {}", guardada.getId(), form.id() == null ? "programada" : "actualizada",
                paciente.getNumeroHistoria(), FECHA_HORA.format(inicio));
        publicar(guardada);
        return obtener(guardada.getId());
    }

    /** Mueve la cita a otra hora conservando su duración (arrastrar en la agenda). */
    @Transactional
    public CitaDto mover(long citaId, LocalDateTime nuevoInicio) {
        sesionUsuario.exigir(Permiso.AGENDA_GESTIONAR);
        Cita cita = activa(citaId);
        LocalDateTime inicio = nuevoInicio.truncatedTo(ChronoUnit.MINUTES);
        int duracion = (int) Duration.between(cita.getInicio(), cita.getFin()).toMinutes();
        cita.setFin(validarIntervalo(cita.getId(), cita.getPaciente().getId(), cita.getOdontologo(), inicio, duracion, true));
        cita.setInicio(inicio);
        log.info("Cita {} movida al {}", citaId, FECHA_HORA.format(inicio));
        publicar(cita);
        return obtener(citaId);
    }

    /** Confirmar, volver a programada, cancelar o marcar inasistencia (ATENDIDA la fija la atención). */
    @Transactional
    public CitaDto cambiarEstado(long citaId, EstadoCita destino) {
        sesionUsuario.exigir(Permiso.AGENDA_GESTIONAR);
        Cita cita = buscar(citaId);
        if (destino == EstadoCita.ATENDIDA) {
            throw new NegocioException("La cita queda atendida al cerrar la atención abierta desde ella.");
        }
        if (!cita.getEstado().puedePasarA(destino)) {
            throw new NegocioException("Una cita " + cita.getEstado().etiqueta().toLowerCase() + " no puede pasar a "
                    + destino.etiqueta().toLowerCase() + ".");
        }
        if (destino == EstadoCita.NO_ASISTIO && cita.getInicio().isAfter(LocalDateTime.now())) {
            throw new NegocioException("La cita aún no empieza: no se puede marcar como inasistencia.");
        }
        if (!atencionRepository.findByCitaIdIn(List.of(citaId)).isEmpty()) {
            throw new NegocioException("La cita ya tiene una atención abierta: ciérrela desde la pestaña Atenciones.");
        }
        cita.setEstado(destino);
        log.info("Cita {} → {}", citaId, destino);
        publicar(cita);
        return obtener(citaId);
    }

    /** Lo llama {@code AtencionService} al cerrar la atención abierta desde la cita. */
    @Transactional
    public void marcarAtendida(long citaId) {
        Cita cita = buscar(citaId);
        if (cita.getEstado().activa()) {
            cita.setEstado(EstadoCita.ATENDIDA);
            publicar(cita);
        }
    }

    // ------------------------------------------------------------------ validaciones

    /**
     * Valida duración, alineación a la grilla, horario de atención y solapamientos.
     *
     * @param cambiaHorario {@code false} si solo se editan motivo o notas (una cita ya pasada se puede corregir)
     * @return fin de la cita
     */
    private LocalDateTime validarIntervalo(Long citaId, long pacienteId, Odontologo odontologo, LocalDateTime inicio,
                                           int duracionMin, boolean cambiaHorario) {
        validarDuracion(duracionMin);
        LocalDateTime fin = inicio.plusMinutes(duracionMin);
        if (!cambiaHorario) {
            return fin;
        }
        int intervalo = intervaloMin();
        if (inicio.getMinute() % intervalo != 0) {
            throw new NegocioException("La hora debe coincidir con la grilla de " + intervalo + " minutos (p. ej. 09:00, 09:"
                    + String.format("%02d", intervalo) + ").");
        }
        if (inicio.isBefore(LocalDateTime.now())) {
            throw new NegocioException("No se puede programar una cita en el pasado.");
        }
        Intervalo cita = new Intervalo(inicio, fin);
        if (!BloqueHorario.dentroDelHorario(cita, horario(odontologo.getId()))) {
            throw new NegocioException("El " + FECHA_HORA.format(inicio) + "–" + HORA.format(fin) + " está fuera del horario de "
                    + "atención de " + odontologo.getNombreCompleto() + ".");
        }
        long excluir = citaId == null ? -1 : citaId;
        if (citaRepository.existeSolapamientoOdontologo(odontologo.getId(), inicio, fin, excluir, EstadoCita.ocupan())) {
            throw new NegocioException(odontologo.getNombreCompleto() + " ya tiene una cita que se cruza con ese horario.");
        }
        if (citaRepository.existeSolapamientoPaciente(pacienteId, inicio, fin, excluir, EstadoCita.ocupan())) {
            throw new NegocioException("El paciente ya tiene otra cita que se cruza con ese horario.");
        }
        return fin;
    }

    private void validarDuracion(int duracionMin) {
        int intervalo = intervaloMin();
        if (duracionMin < intervalo || duracionMin > DURACION_MAXIMA_MIN || duracionMin % intervalo != 0) {
            throw new NegocioException("La duración debe ser múltiplo de " + intervalo + " minutos, entre " + intervalo
                    + " y " + DURACION_MAXIMA_MIN + ".");
        }
    }

    private Long validarItem(Long planItemId, long pacienteId) {
        if (planItemId == null) {
            return null;
        }
        PlanItem item = planItemRepository.findById(planItemId)
                .orElseThrow(() -> new NegocioException("El procedimiento del plan ya no existe."));
        if (!item.getPlan().getPaciente().getId().equals(pacienteId)) {
            throw new NegocioException("El procedimiento elegido es del plan de otro paciente.");
        }
        return planItemId;
    }

    // ------------------------------------------------------------------ apoyo

    private List<BloqueHorario> horario(long odontologoId) {
        return horarioRepository.findByOdontologoIdOrderByDiaSemanaAscHoraInicioAsc(odontologoId).stream()
                .map(h -> new BloqueHorario(h.getDiaSemana(), h.getHoraInicio(), h.getHoraFin()))
                .toList();
    }

    private Odontologo odontologo(long id) {
        Odontologo o = odontologoRepository.findById(id)
                .orElseThrow(() -> new NegocioException("El odontólogo ya no existe en la base de datos."));
        if (!o.isActivo()) {
            throw new NegocioException("El odontólogo " + o.getNombreCompleto() + " está inactivo.");
        }
        return o;
    }

    private Cita buscar(long id) {
        return citaRepository.findById(id).orElseThrow(() -> new NegocioException("La cita ya no existe en la base de datos."));
    }

    private Cita activa(long id) {
        Cita c = buscar(id);
        if (!c.getEstado().activa()) {
            throw new NegocioException("La cita está " + c.getEstado().etiqueta().toLowerCase() + ": ya no se puede modificar.");
        }
        return c;
    }

    private void publicar(Cita c) {
        eventos.publishEvent(new CitaCambiada(c.getId(), c.getPaciente().getId(), c.getOdontologo().getId()));
    }

    private List<CitaDto> dtos(List<Cita> citas) {
        if (citas.isEmpty()) {
            return List.of();
        }
        List<Long> ids = citas.stream().map(Cita::getId).toList();
        Map<Long, Long> atenciones = atencionRepository.findByCitaIdIn(ids).stream()
                .collect(Collectors.toMap(Atencion::getCitaId, Atencion::getId, (a, b) -> a));
        Set<Long> itemIds = citas.stream().map(Cita::getPlanItemId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> items = itemIds.isEmpty() ? Map.of() : planItemRepository.findAllById(itemIds).stream()
                .collect(Collectors.toMap(PlanItem::getId, CitaService::descripcion));
        Map<Long, Cita> porId = citas.stream().collect(Collectors.toMap(Cita::getId, Function.identity()));
        return ids.stream().map(porId::get).map(c -> new CitaDto(c.getId(), c.getPaciente().getId(),
                        c.getPaciente().getNombreCompleto(), c.getPaciente().getNumeroHistoria(), c.getPaciente().getTelefono(),
                        c.getOdontologo().getId(), c.getOdontologo().getNombreCompleto(), c.getOdontologo().getColorAgenda(),
                        c.getInicio(), c.getFin(), c.getMotivo(), c.getEstado(), c.getPlanItemId(),
                        c.getPlanItemId() == null ? null : items.get(c.getPlanItemId()), c.getNotas(), atenciones.get(c.getId())))
                .toList();
    }

    private static String descripcion(PlanItem i) {
        StringBuilder sb = new StringBuilder(i.getProcedimiento().getNombre());
        if (i.getPiezaFdi() != null) {
            sb.append(" · pieza ").append(i.getPiezaFdi());
        }
        return sb.toString();
    }

    private static LocalDateTime ahora() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
