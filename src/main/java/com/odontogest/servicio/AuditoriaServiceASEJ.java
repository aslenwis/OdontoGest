package com.odontogest.servicio;

import com.google.gson.Gson;
import com.odontogest.modelo.AuditoriaASEJ;
import com.odontogest.modelo.UsuarioASEJ;
import com.odontogest.persistencia.repositorio.AuditoriaRepositoryASEJ;
import com.odontogest.persistencia.repositorio.UsuarioRepositoryASEJ;
import com.odontogest.seguridad.PermisoASEJ;
import com.odontogest.seguridad.SesionUsuarioASEJ;
import com.odontogest.servicio.dto.AuditoriaDtoASEJ;
import com.odontogest.servicio.dto.UsuarioSesionASEJ;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditoriaServiceASEJ {

    public static final int MAXIMO_RESULTADOSASEJ = 500;

    private final Clock relojASEJ;
    private final AuditoriaRepositoryASEJ auditoriaRepositoryASEJ;
    private final UsuarioRepositoryASEJ usuarioRepositoryASEJ;
    private final SesionUsuarioASEJ sesionUsuarioASEJ;
    private final Gson gsonASEJ = new Gson();

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarASEJ(String accionASEJ, String entidadASEJ, Long entidadIdASEJ, Object detalleASEJ) {
        Long usuarioIdASEJ = sesionUsuarioASEJ.usuarioASEJ().map(UsuarioSesionASEJ::idASEJ).orElse(null);
        auditoriaRepositoryASEJ.save(AuditoriaASEJ.deASEJ(usuarioIdASEJ, accionASEJ, entidadASEJ, entidadIdASEJ,
                detalleASEJ == null ? null : gsonASEJ.toJson(detalleASEJ), LocalDateTime.now(relojASEJ).truncatedTo(ChronoUnit.SECONDS)));
    }

    public List<AuditoriaDtoASEJ> buscarASEJ(LocalDate desdeASEJ, LocalDate hastaASEJ, String accionASEJ) {
        sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.CONFIGURACION);
        ReporteServiceASEJ.validarRangoASEJ(desdeASEJ, hastaASEJ);
        Map<Long, String> usuariosASEJ = usuarioRepositoryASEJ.findAll().stream()
                .collect(Collectors.toMap(UsuarioASEJ::getIdASEJ, uASEJ -> uASEJ.getUsernameASEJ() + " · " + uASEJ.getNombreCompletoASEJ()));
        return auditoriaRepositoryASEJ.buscarASEJ(desdeASEJ.atStartOfDay(), hastaASEJ.plusDays(1).atStartOfDay(),
                        accionASEJ == null ? "" : accionASEJ, PageRequest.of(0, MAXIMO_RESULTADOSASEJ)).stream()
                .map(aASEJ -> new AuditoriaDtoASEJ(aASEJ.getIdASEJ(), aASEJ.getFechaASEJ(), aASEJ.getUsuarioIdASEJ() == null ? "—"
                        : usuariosASEJ.getOrDefault(aASEJ.getUsuarioIdASEJ(), "#" + aASEJ.getUsuarioIdASEJ()), aASEJ.getAccionASEJ(), aASEJ.getEntidadASEJ(),
                        aASEJ.getEntidadIdASEJ(), aASEJ.getDetalleASEJ()))
                .toList();
    }

    public List<String> accionesASEJ() {
        return auditoriaRepositoryASEJ.accionesASEJ();
    }
}
