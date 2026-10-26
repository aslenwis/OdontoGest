package com.odontogest.servicio;

import com.odontogest.evento.SesionIniciadaASEJ;
import com.odontogest.excepcion.NegocioExceptionASEJ;
import com.odontogest.modelo.UsuarioASEJ;
import com.odontogest.persistencia.repositorio.UsuarioRepositoryASEJ;
import com.odontogest.seguridad.SesionUsuarioASEJ;
import com.odontogest.servicio.dto.UsuarioSesionASEJ;
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

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AutenticacionServiceASEJ {

    public static final int MAX_INTENTOSASEJ = 5;
    public static final Duration DURACION_BLOQUEOASEJ = Duration.ofMinutes(5);

    private static final String CREDENCIALES_INVALIDASASEJ = "Usuario o contraseña incorrectos.";
    private static final DateTimeFormatter HORAASEJ = DateTimeFormatter.ofPattern("HH:mm");

    private final Clock relojASEJ;
    private final UsuarioRepositoryASEJ usuarioRepositoryASEJ;
    private final AuditoriaServiceASEJ auditoriaASEJ;
    private final PasswordEncoder passwordEncoderASEJ;
    private final SesionUsuarioASEJ sesionUsuarioASEJ;
    private final ApplicationEventPublisher eventosASEJ;

    @Transactional(noRollbackFor = NegocioExceptionASEJ.class)
    public UsuarioSesionASEJ iniciarSesionASEJ(String usernameASEJ, String passwordASEJ) {
        if (usernameASEJ == null || usernameASEJ.isBlank() || passwordASEJ == null || passwordASEJ.isEmpty()) {
            throw new NegocioExceptionASEJ("Ingrese usuario y contraseña.");
        }
        UsuarioASEJ usuarioASEJ = usuarioRepositoryASEJ.findByUsernameASEJIgnoreCase(usernameASEJ.trim())
                .orElseThrow(() -> new NegocioExceptionASEJ(CREDENCIALES_INVALIDASASEJ));

        LocalDateTime ahoraASEJ = LocalDateTime.now(relojASEJ);
        if (!usuarioASEJ.isActivoASEJ()) {
            throw new NegocioExceptionASEJ("El usuario está desactivado. Consulte con el administrador.");
        }
        if (usuarioASEJ.getBloqueadoHastaASEJ() != null && ahoraASEJ.isBefore(usuarioASEJ.getBloqueadoHastaASEJ())) {
            throw new NegocioExceptionASEJ("Usuario bloqueado por intentos fallidos. Intente nuevamente a las "
                    + HORAASEJ.format(usuarioASEJ.getBloqueadoHastaASEJ()) + ".");
        }

        if (!passwordEncoderASEJ.matches(passwordASEJ, usuarioASEJ.getPasswordHashASEJ())) {
            registrarIntentoFallidoASEJ(usuarioASEJ, ahoraASEJ);
        }

        usuarioASEJ.setIntentosFallidosASEJ(0);
        usuarioASEJ.setBloqueadoHastaASEJ(null);
        usuarioASEJ.setUltimoAccesoASEJ(ahoraASEJ);

        UsuarioSesionASEJ sesionASEJ = new UsuarioSesionASEJ(usuarioASEJ.getIdASEJ(), usuarioASEJ.getUsernameASEJ(),
                usuarioASEJ.getNombreCompletoASEJ(), usuarioASEJ.getRolASEJ().name(),
                usuarioASEJ.getOdontologoASEJ() == null ? null : usuarioASEJ.getOdontologoASEJ().getIdASEJ());
        sesionUsuarioASEJ.iniciarASEJ(sesionASEJ);
        eventosASEJ.publishEvent(new SesionIniciadaASEJ(sesionASEJ));
        logASEJ.info("Inicio de sesión: {} ({})", sesionASEJ.usernameASEJ(), sesionASEJ.rolASEJ());
        auditoriaASEJ.registrarASEJ("LOGIN", "usuario", sesionASEJ.idASEJ(), null);
        return sesionASEJ;
    }

    public void cerrarSesionASEJ() {
        sesionUsuarioASEJ.usuarioASEJ().ifPresent(uASEJ -> logASEJ.info("Cierre de sesión: {}", uASEJ.usernameASEJ()));
        sesionUsuarioASEJ.cerrarASEJ();
    }

    private void registrarIntentoFallidoASEJ(UsuarioASEJ usuarioASEJ, LocalDateTime ahoraASEJ) {
        int intentosASEJ = usuarioASEJ.getIntentosFallidosASEJ() + 1;
        if (intentosASEJ >= MAX_INTENTOSASEJ) {
            usuarioASEJ.setIntentosFallidosASEJ(0);
            usuarioASEJ.setBloqueadoHastaASEJ(ahoraASEJ.plus(DURACION_BLOQUEOASEJ));
            logASEJ.warn("Usuario {} bloqueado por {} intentos fallidos", usuarioASEJ.getUsernameASEJ(), MAX_INTENTOSASEJ);
            auditoriaASEJ.registrarASEJ("USUARIO_BLOQUEADO", "usuario", usuarioASEJ.getIdASEJ(), Map.of("username", usuarioASEJ.getUsernameASEJ()));
            throw new NegocioExceptionASEJ("Demasiados intentos fallidos. El usuario queda bloqueado por "
                    + DURACION_BLOQUEOASEJ.toMinutes() + " minutos.");
        }
        usuarioASEJ.setIntentosFallidosASEJ(intentosASEJ);
        auditoriaASEJ.registrarASEJ("LOGIN_FALLIDO", "usuario", usuarioASEJ.getIdASEJ(), Map.of("username", usuarioASEJ.getUsernameASEJ(), "intento", intentosASEJ));
        throw new NegocioExceptionASEJ(CREDENCIALES_INVALIDASASEJ);
    }
}
