package com.odontogest.servicio;

import com.odontogest.excepcion.NegocioExceptionASEJ;
import com.odontogest.modelo.AtencionASEJ;
import com.odontogest.modelo.CitaASEJ;
import com.odontogest.modelo.EstadoAtencionASEJ;
import com.odontogest.modelo.OdontogramaASEJ;
import com.odontogest.modelo.OdontologoASEJ;
import com.odontogest.modelo.PacienteASEJ;
import com.odontogest.modelo.RolASEJ;
import com.odontogest.modelo.TipoOdontogramaASEJ;
import com.odontogest.persistencia.repositorio.AtencionRepositoryASEJ;
import com.odontogest.persistencia.repositorio.CitaRepositoryASEJ;
import com.odontogest.persistencia.repositorio.HallazgoRepositoryASEJ;
import com.odontogest.persistencia.repositorio.OdontogramaRepositoryASEJ;
import com.odontogest.persistencia.repositorio.OdontologoRepositoryASEJ;
import com.odontogest.persistencia.repositorio.PacienteRepositoryASEJ;
import com.odontogest.seguridad.PermisoASEJ;
import com.odontogest.seguridad.SesionUsuarioASEJ;
import com.odontogest.servicio.dto.AtencionDetalleASEJ;
import com.odontogest.servicio.dto.AtencionFormASEJ;
import com.odontogest.servicio.dto.AtencionResumenASEJ;
import com.odontogest.servicio.dto.UsuarioSesionASEJ;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AtencionServiceASEJ {

    private static final DateTimeFormatter FECHAASEJ = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Clock relojASEJ;
    private final AtencionRepositoryASEJ atencionRepositoryASEJ;
    private final AuditoriaServiceASEJ auditoriaASEJ;
    private final PacienteRepositoryASEJ pacienteRepositoryASEJ;
    private final OdontologoRepositoryASEJ odontologoRepositoryASEJ;
    private final OdontogramaRepositoryASEJ odontogramaRepositoryASEJ;
    private final HallazgoRepositoryASEJ hallazgoRepositoryASEJ;
    private final OdontogramaServiceASEJ odontogramaServiceASEJ;
    private final CitaRepositoryASEJ citaRepositoryASEJ;
    private final CitaServiceASEJ citaServiceASEJ;
    private final SesionUsuarioASEJ sesionUsuarioASEJ;

    public List<AtencionResumenASEJ> listarPorPacienteASEJ(long pacienteIdASEJ) {
        return atencionRepositoryASEJ.findByPacienteASEJIdASEJOrderByFechaASEJDescIdASEJDesc(pacienteIdASEJ).stream()
                .map(aASEJ -> new AtencionResumenASEJ(aASEJ.getIdASEJ(), aASEJ.getFechaASEJ(), aASEJ.getOdontologoASEJ().getNombreCompletoASEJ(),
                        aASEJ.getMotivoConsultaASEJ(), aASEJ.getEstadoASEJ()))
                .toList();
    }

    public AtencionDetalleASEJ obtenerASEJ(long idASEJ) {
        return detalleASEJ(buscarASEJ(idASEJ));
    }

    public Optional<AtencionDetalleASEJ> abiertaASEJ(long pacienteIdASEJ) {
        return atencionRepositoryASEJ.findFirstByPacienteASEJIdASEJAndEstadoASEJ(pacienteIdASEJ, EstadoAtencionASEJ.ABIERTA).map(AtencionServiceASEJ::detalleASEJ);
    }

    @Transactional
    public long abrirASEJ(AtencionFormASEJ formASEJ) {
        return abrirASEJ(formASEJ, null);
    }

    @Transactional
    public long abrirDesdeCitaASEJ(long citaIdASEJ) {
        sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.ATENCION_GESTIONAR);
        CitaASEJ citaASEJ = citaRepositoryASEJ.findById(citaIdASEJ)
                .orElseThrow(() -> new NegocioExceptionASEJ("La cita ya no existe en la base de datos."));
        if (!citaASEJ.getEstadoASEJ().activaASEJ()) {
            throw new NegocioExceptionASEJ("La cita está " + citaASEJ.getEstadoASEJ().etiquetaASEJ().toLowerCase() + ": no se puede atender.");
        }
        if (!citaASEJ.getInicioASEJ().toLocalDate().equals(LocalDate.now(relojASEJ))) {
            throw new NegocioExceptionASEJ("Solo se atienden las citas del día; esta es del " + FECHAASEJ.format(citaASEJ.getInicioASEJ()) + ".");
        }
        if (!atencionRepositoryASEJ.findByCitaIdASEJIn(List.of(citaIdASEJ)).isEmpty()) {
            throw new NegocioExceptionASEJ("La cita ya tiene una atención abierta.");
        }
        AtencionFormASEJ formASEJ = new AtencionFormASEJ(null, citaASEJ.getPacienteASEJ().getIdASEJ(), citaASEJ.getOdontologoASEJ().getIdASEJ(), citaASEJ.getMotivoASEJ(),
                null, null, null, null);
        return abrirASEJ(formASEJ, citaIdASEJ);
    }

    private long abrirASEJ(AtencionFormASEJ formASEJ, Long citaIdASEJ) {
        UsuarioSesionASEJ usuarioASEJ = sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.ATENCION_GESTIONAR);
        PacienteASEJ pacienteASEJ = pacienteRepositoryASEJ.findById(formASEJ.pacienteIdASEJ())
                .orElseThrow(() -> new NegocioExceptionASEJ("El paciente ya no existe en la base de datos."));
        if (!pacienteASEJ.isActivoASEJ()) {
            throw new NegocioExceptionASEJ("El paciente está inactivo: reactívelo antes de atenderlo.");
        }
        atencionRepositoryASEJ.findFirstByPacienteASEJIdASEJAndEstadoASEJ(pacienteASEJ.getIdASEJ(), EstadoAtencionASEJ.ABIERTA).ifPresent(aASEJ -> {
            throw new NegocioExceptionASEJ("El paciente ya tiene una atención abierta del " + FECHAASEJ.format(aASEJ.getFechaASEJ())
                    + ": continúe en ella o ciérrela primero.");
        });
        Long odontologoIdASEJ = RolASEJ.valueOf(usuarioASEJ.rolASEJ()) == RolASEJ.ODONTOLOGO ? usuarioASEJ.odontologoIdASEJ() : formASEJ.odontologoIdASEJ();
        String motivoASEJ = ValidadorASEJ.limpioMultilineaASEJ(formASEJ.motivoConsultaASEJ());
        new ValidadorASEJ()
                .requeridoASEJ(odontologoIdASEJ, "el odontólogo que atiende")
                .requeridoASEJ(motivoASEJ, "el motivo de consulta")
                .maximoASEJ(motivoASEJ, 500, "motivo de consulta")
                .lanzarSiHayErroresASEJ();
        OdontologoASEJ odontologoASEJ = odontologoRepositoryASEJ.findById(odontologoIdASEJ)
                .orElseThrow(() -> new NegocioExceptionASEJ("El odontólogo ya no existe en la base de datos."));
        if (!odontologoASEJ.isActivoASEJ()) {
            throw new NegocioExceptionASEJ("El odontólogo " + odontologoASEJ.getNombreCompletoASEJ() + " está inactivo.");
        }

        AtencionASEJ aASEJ = new AtencionASEJ();
        aASEJ.setPacienteASEJ(pacienteASEJ);
        aASEJ.setOdontologoASEJ(odontologoASEJ);
        aASEJ.setFechaASEJ(LocalDateTime.now(relojASEJ).truncatedTo(ChronoUnit.SECONDS));
        aASEJ.setMotivoConsultaASEJ(motivoASEJ);
        aASEJ.setExamenClinicoASEJ(ValidadorASEJ.limpioMultilineaASEJ(formASEJ.examenClinicoASEJ()));
        aASEJ.setDiagnosticoASEJ(ValidadorASEJ.limpioMultilineaASEJ(formASEJ.diagnosticoASEJ()));
        aASEJ.setObservacionesASEJ(ValidadorASEJ.limpioMultilineaASEJ(formASEJ.observacionesASEJ()));
        aASEJ.setEstadoASEJ(EstadoAtencionASEJ.ABIERTA);
        aASEJ.setCitaIdASEJ(citaIdASEJ);
        AtencionASEJ guardadaASEJ = atencionRepositoryASEJ.save(aASEJ);
        odontogramaServiceASEJ.abrirBorradorASEJ(guardadaASEJ.getIdASEJ(), formASEJ.denticionASEJ());
        logASEJ.info("Atención {} abierta para {}", guardadaASEJ.getIdASEJ(), pacienteASEJ.getNumeroHistoriaASEJ());
        auditoriaASEJ.registrarASEJ("ATENCION_ABRIR", "atencion", guardadaASEJ.getIdASEJ(),
                Map.of("paciente", pacienteASEJ.getNumeroHistoriaASEJ(), "cita", String.valueOf(citaIdASEJ)));
        return guardadaASEJ.getIdASEJ();
    }

    @Transactional
    public AtencionDetalleASEJ actualizarASEJ(AtencionFormASEJ formASEJ) {
        sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.ATENCION_GESTIONAR);
        AtencionASEJ aASEJ = abiertaPorIdASEJ(formASEJ.idASEJ());
        String motivoASEJ = ValidadorASEJ.limpioMultilineaASEJ(formASEJ.motivoConsultaASEJ());
        new ValidadorASEJ().requeridoASEJ(motivoASEJ, "el motivo de consulta").maximoASEJ(motivoASEJ, 500, "motivo de consulta")
                .lanzarSiHayErroresASEJ();
        aASEJ.setMotivoConsultaASEJ(motivoASEJ);
        aASEJ.setExamenClinicoASEJ(ValidadorASEJ.limpioMultilineaASEJ(formASEJ.examenClinicoASEJ()));
        aASEJ.setDiagnosticoASEJ(ValidadorASEJ.limpioMultilineaASEJ(formASEJ.diagnosticoASEJ()));
        aASEJ.setObservacionesASEJ(ValidadorASEJ.limpioMultilineaASEJ(formASEJ.observacionesASEJ()));
        return detalleASEJ(aASEJ);
    }

    @Transactional
    public AtencionDetalleASEJ cerrarASEJ(long idASEJ) {
        sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.ATENCION_GESTIONAR);
        AtencionASEJ aASEJ = abiertaPorIdASEJ(idASEJ);
        for (OdontogramaASEJ oASEJ : odontogramaRepositoryASEJ.findByAtencionASEJIdASEJ(aASEJ.getIdASEJ())) {
            if (!oASEJ.esBorradorASEJ()) {
                continue;
            }
            boolean vacioASEJ = hallazgoRepositoryASEJ.countByOdontogramaASEJIdASEJ(oASEJ.getIdASEJ()) == 0;
            if (oASEJ.getTipoASEJ() == TipoOdontogramaASEJ.EVOLUCION && vacioASEJ) {
                odontogramaRepositoryASEJ.delete(oASEJ);
                logASEJ.info("Odontograma de evolución {} sin hallazgos descartado al cerrar la atención {}", oASEJ.getIdASEJ(), idASEJ);
            } else {
                throw new NegocioExceptionASEJ("Cierre y firme el odontograma antes de cerrar la atención.");
            }
        }
        aASEJ.setEstadoASEJ(EstadoAtencionASEJ.CERRADA);
        auditoriaASEJ.registrarASEJ("ATENCION_CERRAR", "atencion", aASEJ.getIdASEJ(), null);
        aASEJ.setCerradaEnASEJ(LocalDateTime.now(relojASEJ).truncatedTo(ChronoUnit.SECONDS));
        if (aASEJ.getCitaIdASEJ() != null) {
            citaServiceASEJ.marcarAtendidaASEJ(aASEJ.getCitaIdASEJ());
        }
        return detalleASEJ(aASEJ);
    }

    private AtencionASEJ buscarASEJ(long idASEJ) {
        return atencionRepositoryASEJ.findById(idASEJ)
                .orElseThrow(() -> new NegocioExceptionASEJ("La atención ya no existe en la base de datos."));
    }

    private AtencionASEJ abiertaPorIdASEJ(Long idASEJ) {
        AtencionASEJ aASEJ = buscarASEJ(idASEJ);
        if (aASEJ.getEstadoASEJ() != EstadoAtencionASEJ.ABIERTA) {
            throw new NegocioExceptionASEJ("La atención ya está cerrada y es de solo lectura.");
        }
        return aASEJ;
    }

    private static AtencionDetalleASEJ detalleASEJ(AtencionASEJ aASEJ) {
        return new AtencionDetalleASEJ(aASEJ.getIdASEJ(), aASEJ.getPacienteASEJ().getIdASEJ(), aASEJ.getOdontologoASEJ().getIdASEJ(),
                aASEJ.getOdontologoASEJ().getNombreCompletoASEJ(), aASEJ.getFechaASEJ(), aASEJ.getMotivoConsultaASEJ(), aASEJ.getExamenClinicoASEJ(),
                aASEJ.getDiagnosticoASEJ(), aASEJ.getObservacionesASEJ(), aASEJ.getEstadoASEJ(), aASEJ.getCerradaEnASEJ());
    }
}
