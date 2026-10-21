/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio;

import com.google.gson.Gson;
import com.odontogest.modelo.Auditoria;
import com.odontogest.modelo.Usuario;
import com.odontogest.persistencia.repositorio.AuditoriaRepository;
import com.odontogest.persistencia.repositorio.UsuarioRepository;
import com.odontogest.seguridad.Permiso;
import com.odontogest.seguridad.SesionUsuario;
import com.odontogest.servicio.dto.AuditoriaDto;
import com.odontogest.servicio.dto.UsuarioSesion;
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

/**
 * Auditoría (PLAN §5.5, RNF-11). {@link #registrar} exige una transacción en curso ({@code MANDATORY}): se llama
 * dentro de la transacción del servicio que hace el cambio, así el registro no queda huérfano si la operación falla
 * y tampoco se pierde si se confirma. El detalle se guarda como JSON (Gson) y nunca incluye contraseñas ni secretos.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditoriaService {

    public static final int MAXIMO_RESULTADOS = 500;

    private final Clock reloj;
    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final SesionUsuario sesionUsuario;
    private final Gson gson = new Gson();

    /**
     * @param detalle objeto que se serializa a JSON ({@code null} = sin detalle); típicamente un {@code Map.of(...)}
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(String accion, String entidad, Long entidadId, Object detalle) {
        Long usuarioId = sesionUsuario.usuario().map(UsuarioSesion::id).orElse(null);
        auditoriaRepository.save(Auditoria.de(usuarioId, accion, entidad, entidadId,
                detalle == null ? null : gson.toJson(detalle), LocalDateTime.now(reloj).truncatedTo(ChronoUnit.SECONDS)));
    }

    /** Consulta para el administrador (pestaña Auditoría de Reportes). */
    public List<AuditoriaDto> buscar(LocalDate desde, LocalDate hasta, String accion) {
        sesionUsuario.exigir(Permiso.CONFIGURACION);
        ReporteService.validarRango(desde, hasta);
        Map<Long, String> usuarios = usuarioRepository.findAll().stream()
                .collect(Collectors.toMap(Usuario::getId, u -> u.getUsername() + " · " + u.getNombreCompleto()));
        return auditoriaRepository.buscar(desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay(),
                        accion == null ? "" : accion, PageRequest.of(0, MAXIMO_RESULTADOS)).stream()
                .map(a -> new AuditoriaDto(a.getId(), a.getFecha(), a.getUsuarioId() == null ? "—"
                        : usuarios.getOrDefault(a.getUsuarioId(), "#" + a.getUsuarioId()), a.getAccion(), a.getEntidad(),
                        a.getEntidadId(), a.getDetalle()))
                .toList();
    }

    public List<String> acciones() {
        return auditoriaRepository.acciones();
    }
}
