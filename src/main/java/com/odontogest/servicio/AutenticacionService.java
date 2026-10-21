/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio;

import com.odontogest.evento.SesionIniciada;
import com.odontogest.excepcion.NegocioException;
import com.odontogest.modelo.Usuario;
import com.odontogest.persistencia.repositorio.UsuarioRepository;
import com.odontogest.seguridad.SesionUsuario;
import com.odontogest.servicio.dto.UsuarioSesion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Inicio y cierre de sesión (RF-SEG-01, RF-SEG-04).
 * <p>
 * Contraseñas verificadas con BCrypt. Tras {@value #MAX_INTENTOS} intentos fallidos consecutivos
 * el usuario queda bloqueado {@link #DURACION_BLOQUEO}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AutenticacionService {

    public static final int MAX_INTENTOS = 5;
    public static final Duration DURACION_BLOQUEO = Duration.ofMinutes(5);

    private static final String CREDENCIALES_INVALIDAS = "Usuario o contraseña incorrectos.";
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final Clock reloj;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoria;
    private final PasswordEncoder passwordEncoder;
    private final SesionUsuario sesionUsuario;
    private final ApplicationEventPublisher eventos;

    /**
     * Verifica las credenciales y abre la sesión.
     * <p>
     * {@code noRollbackFor}: el contador de intentos fallidos debe persistir aunque se lance
     * {@link NegocioException}.
     */
    @Transactional(noRollbackFor = NegocioException.class)
    public UsuarioSesion iniciarSesion(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            throw new NegocioException("Ingrese usuario y contraseña.");
        }
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(username.trim())
                .orElseThrow(() -> new NegocioException(CREDENCIALES_INVALIDAS));

        LocalDateTime ahora = LocalDateTime.now(reloj);
        if (!usuario.isActivo()) {
            throw new NegocioException("El usuario está desactivado. Consulte con el administrador.");
        }
        if (usuario.getBloqueadoHasta() != null && ahora.isBefore(usuario.getBloqueadoHasta())) {
            throw new NegocioException("Usuario bloqueado por intentos fallidos. Intente nuevamente a las "
                    + HORA.format(usuario.getBloqueadoHasta()) + ".");
        }

        if (!passwordEncoder.matches(password, usuario.getPasswordHash())) {
            registrarIntentoFallido(usuario, ahora);
        }

        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuario.setUltimoAcceso(ahora);

        UsuarioSesion sesion = new UsuarioSesion(usuario.getId(), usuario.getUsername(),
                usuario.getNombreCompleto(), usuario.getRol().name(),
                usuario.getOdontologo() == null ? null : usuario.getOdontologo().getId());
        sesionUsuario.iniciar(sesion);
        eventos.publishEvent(new SesionIniciada(sesion));
        log.info("Inicio de sesión: {} ({})", sesion.username(), sesion.rol());
        auditoria.registrar("LOGIN", "usuario", sesion.id(), null);
        return sesion;
    }

    public void cerrarSesion() {
        sesionUsuario.usuario().ifPresent(u -> log.info("Cierre de sesión: {}", u.username()));
        sesionUsuario.cerrar();
    }

    private void registrarIntentoFallido(Usuario usuario, LocalDateTime ahora) {
        int intentos = usuario.getIntentosFallidos() + 1;
        if (intentos >= MAX_INTENTOS) {
            usuario.setIntentosFallidos(0);
            usuario.setBloqueadoHasta(ahora.plus(DURACION_BLOQUEO));
            log.warn("Usuario {} bloqueado por {} intentos fallidos", usuario.getUsername(), MAX_INTENTOS);
            auditoria.registrar("USUARIO_BLOQUEADO", "usuario", usuario.getId(), Map.of("username", usuario.getUsername()));
            throw new NegocioException("Demasiados intentos fallidos. El usuario queda bloqueado por "
                    + DURACION_BLOQUEO.toMinutes() + " minutos.");
        }
        usuario.setIntentosFallidos(intentos);
        auditoria.registrar("LOGIN_FALLIDO", "usuario", usuario.getId(), Map.of("username", usuario.getUsername(), "intento", intentos));
        throw new NegocioException(CREDENCIALES_INVALIDAS);
    }
}
