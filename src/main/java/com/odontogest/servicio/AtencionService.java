/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio;

import com.odontogest.excepcion.NegocioException;
import com.odontogest.modelo.Atencion;
import com.odontogest.modelo.Cita;
import com.odontogest.modelo.EstadoAtencion;
import com.odontogest.modelo.Odontograma;
import com.odontogest.modelo.Odontologo;
import com.odontogest.modelo.Paciente;
import com.odontogest.modelo.Rol;
import com.odontogest.modelo.TipoOdontograma;
import com.odontogest.persistencia.repositorio.AtencionRepository;
import com.odontogest.persistencia.repositorio.CitaRepository;
import com.odontogest.persistencia.repositorio.HallazgoRepository;
import com.odontogest.persistencia.repositorio.OdontogramaRepository;
import com.odontogest.persistencia.repositorio.OdontologoRepository;
import com.odontogest.persistencia.repositorio.PacienteRepository;
import com.odontogest.seguridad.Permiso;
import com.odontogest.seguridad.SesionUsuario;
import com.odontogest.servicio.dto.AtencionDetalle;
import com.odontogest.servicio.dto.AtencionForm;
import com.odontogest.servicio.dto.AtencionResumen;
import com.odontogest.servicio.dto.UsuarioSesion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Atenciones del paciente (RF-ATE-01, RF-ATE-03, RF-ATE-04). Abrir una atención abre también su odontograma en
 * borrador; un paciente tiene como máximo una atención abierta. Para cerrarla, su odontograma debe estar firmado
 * (una evolución sin hallazgos se descarta automáticamente). Una atención abierta desde una cita (RF-AGE-05) deja la
 * cita como ATENDIDA al cerrarse.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AtencionService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AtencionRepository atencionRepository;
    private final PacienteRepository pacienteRepository;
    private final OdontologoRepository odontologoRepository;
    private final OdontogramaRepository odontogramaRepository;
    private final HallazgoRepository hallazgoRepository;
    private final OdontogramaService odontogramaService;
    private final CitaRepository citaRepository;
    private final CitaService citaService;
    private final SesionUsuario sesionUsuario;

    public List<AtencionResumen> listarPorPaciente(long pacienteId) {
        return atencionRepository.findByPacienteIdOrderByFechaDescIdDesc(pacienteId).stream()
                .map(a -> new AtencionResumen(a.getId(), a.getFecha(), a.getOdontologo().getNombreCompleto(),
                        a.getMotivoConsulta(), a.getEstado()))
                .toList();
    }

    public AtencionDetalle obtener(long id) {
        return detalle(buscar(id));
    }

    public Optional<AtencionDetalle> abierta(long pacienteId) {
        return atencionRepository.findFirstByPacienteIdAndEstado(pacienteId, EstadoAtencion.ABIERTA).map(AtencionService::detalle);
    }

    /** Abre la atención y su odontograma en borrador; devuelve el id de la atención. */
    @Transactional
    public long abrir(AtencionForm form) {
        return abrir(form, null);
    }

    /**
     * "Atender ahora" desde la agenda (RF-AGE-05): abre la atención del paciente de la cita con su odontólogo y su
     * motivo. Solo para citas pendientes del día.
     *
     * @return id de la atención
     */
    @Transactional
    public long abrirDesdeCita(long citaId) {
        sesionUsuario.exigir(Permiso.ATENCION_GESTIONAR);
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new NegocioException("La cita ya no existe en la base de datos."));
        if (!cita.getEstado().activa()) {
            throw new NegocioException("La cita está " + cita.getEstado().etiqueta().toLowerCase() + ": no se puede atender.");
        }
        if (!cita.getInicio().toLocalDate().equals(LocalDate.now())) {
            throw new NegocioException("Solo se atienden las citas del día; esta es del " + FECHA.format(cita.getInicio()) + ".");
        }
        if (!atencionRepository.findByCitaIdIn(List.of(citaId)).isEmpty()) {
            throw new NegocioException("La cita ya tiene una atención abierta.");
        }
        AtencionForm form = new AtencionForm(null, cita.getPaciente().getId(), cita.getOdontologo().getId(), cita.getMotivo(),
                null, null, null, null);
        return abrir(form, citaId);
    }

    private long abrir(AtencionForm form, Long citaId) {
        UsuarioSesion usuario = sesionUsuario.exigir(Permiso.ATENCION_GESTIONAR);
        Paciente paciente = pacienteRepository.findById(form.pacienteId())
                .orElseThrow(() -> new NegocioException("El paciente ya no existe en la base de datos."));
        if (!paciente.isActivo()) {
            throw new NegocioException("El paciente está inactivo: reactívelo antes de atenderlo.");
        }
        atencionRepository.findFirstByPacienteIdAndEstado(paciente.getId(), EstadoAtencion.ABIERTA).ifPresent(a -> {
            throw new NegocioException("El paciente ya tiene una atención abierta del " + FECHA.format(a.getFecha())
                    + ": continúe en ella o ciérrela primero.");
        });
        Long odontologoId = Rol.valueOf(usuario.rol()) == Rol.ODONTOLOGO ? usuario.odontologoId() : form.odontologoId();
        String motivo = Validador.limpioMultilinea(form.motivoConsulta());
        new Validador()
                .requerido(odontologoId, "el odontólogo que atiende")
                .requerido(motivo, "el motivo de consulta")
                .maximo(motivo, 500, "motivo de consulta")
                .lanzarSiHayErrores();
        Odontologo odontologo = odontologoRepository.findById(odontologoId)
                .orElseThrow(() -> new NegocioException("El odontólogo ya no existe en la base de datos."));
        if (!odontologo.isActivo()) {
            throw new NegocioException("El odontólogo " + odontologo.getNombreCompleto() + " está inactivo.");
        }

        Atencion a = new Atencion();
        a.setPaciente(paciente);
        a.setOdontologo(odontologo);
        a.setFecha(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        a.setMotivoConsulta(motivo);
        a.setExamenClinico(Validador.limpioMultilinea(form.examenClinico()));
        a.setDiagnostico(Validador.limpioMultilinea(form.diagnostico()));
        a.setObservaciones(Validador.limpioMultilinea(form.observaciones()));
        a.setEstado(EstadoAtencion.ABIERTA);
        a.setCitaId(citaId);
        Atencion guardada = atencionRepository.save(a);
        odontogramaService.abrirBorrador(guardada.getId(), form.denticion());
        log.info("Atención {} abierta para {}", guardada.getId(), paciente.getNumeroHistoria());
        return guardada.getId();
    }

    /** Actualiza los datos clínicos de una atención abierta. */
    @Transactional
    public AtencionDetalle actualizar(AtencionForm form) {
        sesionUsuario.exigir(Permiso.ATENCION_GESTIONAR);
        Atencion a = abiertaPorId(form.id());
        String motivo = Validador.limpioMultilinea(form.motivoConsulta());
        new Validador().requerido(motivo, "el motivo de consulta").maximo(motivo, 500, "motivo de consulta")
                .lanzarSiHayErrores();
        a.setMotivoConsulta(motivo);
        a.setExamenClinico(Validador.limpioMultilinea(form.examenClinico()));
        a.setDiagnostico(Validador.limpioMultilinea(form.diagnostico()));
        a.setObservaciones(Validador.limpioMultilinea(form.observaciones()));
        return detalle(a);
    }

    /** Cierra la atención; desde entonces es de solo lectura (RF-ATE-03). */
    @Transactional
    public AtencionDetalle cerrar(long id) {
        sesionUsuario.exigir(Permiso.ATENCION_GESTIONAR);
        Atencion a = abiertaPorId(id);
        for (Odontograma o : odontogramaRepository.findByAtencionId(a.getId())) {
            if (!o.esBorrador()) {
                continue;
            }
            boolean vacio = hallazgoRepository.countByOdontogramaId(o.getId()) == 0;
            if (o.getTipo() == TipoOdontograma.EVOLUCION && vacio) {
                odontogramaRepository.delete(o);
                log.info("Odontograma de evolución {} sin hallazgos descartado al cerrar la atención {}", o.getId(), id);
            } else {
                throw new NegocioException("Cierre y firme el odontograma antes de cerrar la atención.");
            }
        }
        a.setEstado(EstadoAtencion.CERRADA);
        a.setCerradaEn(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        if (a.getCitaId() != null) {
            citaService.marcarAtendida(a.getCitaId());
        }
        return detalle(a);
    }

    private Atencion buscar(long id) {
        return atencionRepository.findById(id)
                .orElseThrow(() -> new NegocioException("La atención ya no existe en la base de datos."));
    }

    private Atencion abiertaPorId(Long id) {
        Atencion a = buscar(id);
        if (a.getEstado() != EstadoAtencion.ABIERTA) {
            throw new NegocioException("La atención ya está cerrada y es de solo lectura.");
        }
        return a;
    }

    private static AtencionDetalle detalle(Atencion a) {
        return new AtencionDetalle(a.getId(), a.getPaciente().getId(), a.getOdontologo().getId(),
                a.getOdontologo().getNombreCompleto(), a.getFecha(), a.getMotivoConsulta(), a.getExamenClinico(),
                a.getDiagnostico(), a.getObservaciones(), a.getEstado(), a.getCerradaEn());
    }
}
