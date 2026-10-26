package com.odontogest.servicio;

import com.odontogest.excepcion.NegocioExceptionASEJ;
import com.odontogest.modelo.OdontologoASEJ;
import com.odontogest.modelo.RolASEJ;
import com.odontogest.modelo.UsuarioASEJ;
import com.odontogest.persistencia.repositorio.OdontologoRepositoryASEJ;
import com.odontogest.persistencia.repositorio.UsuarioRepositoryASEJ;
import com.odontogest.seguridad.PermisoASEJ;
import com.odontogest.seguridad.SesionUsuarioASEJ;
import com.odontogest.servicio.dto.UsuarioDtoASEJ;
import com.odontogest.servicio.dto.UsuarioFormASEJ;
import com.odontogest.servicio.dto.UsuarioSesionASEJ;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UsuarioServiceASEJ {

    private static final Pattern USERNAMEASEJ = Pattern.compile("[A-Za-z0-9._-]{3,30}");
    private static final int LARGO_MINIMO_PASSWORDASEJ = 8;

    private final Clock relojASEJ;
    private final UsuarioRepositoryASEJ usuarioRepositoryASEJ;
    private final AuditoriaServiceASEJ auditoriaASEJ;
    private final OdontologoRepositoryASEJ odontologoRepositoryASEJ;
    private final PasswordEncoder passwordEncoderASEJ;
    private final SesionUsuarioASEJ sesionUsuarioASEJ;

    public List<UsuarioDtoASEJ> listarASEJ() {
        LocalDateTime ahoraASEJ = LocalDateTime.now(relojASEJ);
        return usuarioRepositoryASEJ.findAllByOrderByUsernameASEJAsc().stream().map(uASEJ -> dtoASEJ(uASEJ, ahoraASEJ)).toList();
    }

    @Transactional
    public long guardarASEJ(UsuarioFormASEJ formASEJ) {
        UsuarioSesionASEJ actualASEJ = sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.USUARIOS);
        boolean nuevoASEJ = formASEJ.idASEJ() == null;
        String usernameASEJ = ValidadorASEJ.limpioASEJ(formASEJ.usernameASEJ());
        String nombreASEJ = ValidadorASEJ.limpioASEJ(formASEJ.nombreCompletoASEJ());

        ValidadorASEJ vASEJ = new ValidadorASEJ()
                .requeridoASEJ(nombreASEJ, "el nombre completo")
                .maximoASEJ(nombreASEJ, 120, "nombre completo")
                .requeridoASEJ(formASEJ.rolASEJ(), "el rol")
                .siASEJ(formASEJ.rolASEJ() == RolASEJ.ODONTOLOGO && formASEJ.odontologoIdASEJ() == null,
                        "Un usuario con rol Odontólogo debe estar vinculado a un odontólogo.");
        if (nuevoASEJ) {
            vASEJ.requeridoASEJ(usernameASEJ, "el nombre de usuario")
                    .siASEJ(usernameASEJ != null && !USERNAMEASEJ.matcher(usernameASEJ).matches(),
                            "El usuario admite de 3 a 30 letras, dígitos, punto, guion o guion bajo.");
            validarPasswordASEJ(formASEJ.passwordASEJ(), vASEJ);
        }
        vASEJ.lanzarSiHayErroresASEJ();

        UsuarioASEJ uASEJ;
        if (nuevoASEJ) {
            usuarioRepositoryASEJ.findByUsernameASEJIgnoreCase(usernameASEJ).ifPresent(xASEJ -> {
                throw new NegocioExceptionASEJ("El nombre de usuario " + usernameASEJ + " ya existe.");
            });
            uASEJ = new UsuarioASEJ();
            uASEJ.setUsernameASEJ(usernameASEJ);
            uASEJ.setPasswordHashASEJ(passwordEncoderASEJ.encode(formASEJ.passwordASEJ()));
            uASEJ.setCreadoEnASEJ(LocalDateTime.now(relojASEJ));
        } else {
            uASEJ = buscarASEJ(formASEJ.idASEJ());
            boolean pierdeAdminASEJ = uASEJ.getRolASEJ() == RolASEJ.ADMIN && uASEJ.isActivoASEJ() && (formASEJ.rolASEJ() != RolASEJ.ADMIN || !formASEJ.activoASEJ());
            if (pierdeAdminASEJ && usuarioRepositoryASEJ.countByRolASEJAndActivoASEJTrue(RolASEJ.ADMIN) <= 1) {
                throw new NegocioExceptionASEJ("Debe quedar al menos un administrador activo.");
            }
            if (uASEJ.getIdASEJ() == actualASEJ.idASEJ() && !formASEJ.activoASEJ()) {
                throw new NegocioExceptionASEJ("No puede desactivar su propio usuario.");
            }
        }
        uASEJ.setNombreCompletoASEJ(nombreASEJ);
        uASEJ.setRolASEJ(formASEJ.rolASEJ());
        uASEJ.setOdontologoASEJ(formASEJ.rolASEJ() == RolASEJ.ODONTOLOGO ? odontologoActivoASEJ(formASEJ.odontologoIdASEJ()) : null);
        uASEJ.setActivoASEJ(formASEJ.activoASEJ());
        UsuarioASEJ guardadoASEJ = usuarioRepositoryASEJ.save(uASEJ);
        logASEJ.info("Usuario {} {} por {}", guardadoASEJ.getUsernameASEJ(), nuevoASEJ ? "creado" : "actualizado", actualASEJ.usernameASEJ());
        auditoriaASEJ.registrarASEJ(nuevoASEJ ? "USUARIO_CREAR" : "USUARIO_EDITAR", "usuario", guardadoASEJ.getIdASEJ(),
                Map.of("username", guardadoASEJ.getUsernameASEJ(), "rol", guardadoASEJ.getRolASEJ().name()));
        return guardadoASEJ.getIdASEJ();
    }

    @Transactional
    public void restablecerPasswordASEJ(long usuarioIdASEJ, String nuevaASEJ) {
        sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.USUARIOS);
        ValidadorASEJ vASEJ = new ValidadorASEJ();
        validarPasswordASEJ(nuevaASEJ, vASEJ);
        vASEJ.lanzarSiHayErroresASEJ();
        UsuarioASEJ uASEJ = buscarASEJ(usuarioIdASEJ);
        uASEJ.setPasswordHashASEJ(passwordEncoderASEJ.encode(nuevaASEJ));
        uASEJ.setIntentosFallidosASEJ(0);
        uASEJ.setBloqueadoHastaASEJ(null);
        auditoriaASEJ.registrarASEJ("USUARIO_RESTABLECER_PASSWORD", "usuario", usuarioIdASEJ, null);
    }

    @Transactional
    public void cambiarPasswordASEJ(String actualASEJ, String nuevaASEJ, String confirmacionASEJ) {
        UsuarioSesionASEJ sesionASEJ = sesionUsuarioASEJ.exigirUsuarioASEJ();
        UsuarioASEJ uASEJ = buscarASEJ(sesionASEJ.idASEJ());
        if (actualASEJ == null || !passwordEncoderASEJ.matches(actualASEJ, uASEJ.getPasswordHashASEJ())) {
            throw new NegocioExceptionASEJ("La contraseña actual no es correcta.");
        }
        ValidadorASEJ vASEJ = new ValidadorASEJ()
                .siASEJ(nuevaASEJ != null && !nuevaASEJ.equals(confirmacionASEJ), "La confirmación no coincide con la nueva contraseña.")
                .siASEJ(nuevaASEJ != null && nuevaASEJ.equals(actualASEJ), "La nueva contraseña debe ser distinta de la actual.");
        validarPasswordASEJ(nuevaASEJ, vASEJ);
        vASEJ.lanzarSiHayErroresASEJ();
        uASEJ.setPasswordHashASEJ(passwordEncoderASEJ.encode(nuevaASEJ));
        auditoriaASEJ.registrarASEJ("USUARIO_CAMBIAR_PASSWORD", "usuario", uASEJ.getIdASEJ(), null);
    }

    private static void validarPasswordASEJ(String passwordASEJ, ValidadorASEJ vASEJ) {
        vASEJ.siASEJ(passwordASEJ == null || passwordASEJ.length() < LARGO_MINIMO_PASSWORDASEJ
                        || !passwordASEJ.chars().anyMatch(Character::isLetter) || !passwordASEJ.chars().anyMatch(Character::isDigit),
                "La contraseña debe tener al menos " + LARGO_MINIMO_PASSWORDASEJ + " caracteres, con letras y números.");
    }

    private OdontologoASEJ odontologoActivoASEJ(Long idASEJ) {
        OdontologoASEJ oASEJ = odontologoRepositoryASEJ.findById(idASEJ)
                .orElseThrow(() -> new NegocioExceptionASEJ("El odontólogo ya no existe en la base de datos."));
        if (!oASEJ.isActivoASEJ()) {
            throw new NegocioExceptionASEJ("El odontólogo " + oASEJ.getNombreCompletoASEJ() + " está inactivo.");
        }
        return oASEJ;
    }

    private UsuarioASEJ buscarASEJ(long idASEJ) {
        return usuarioRepositoryASEJ.findById(idASEJ)
                .orElseThrow(() -> new NegocioExceptionASEJ("El usuario ya no existe en la base de datos."));
    }

    private static UsuarioDtoASEJ dtoASEJ(UsuarioASEJ uASEJ, LocalDateTime ahoraASEJ) {
        OdontologoASEJ oASEJ = uASEJ.getOdontologoASEJ();
        return new UsuarioDtoASEJ(uASEJ.getIdASEJ(), uASEJ.getUsernameASEJ(), uASEJ.getNombreCompletoASEJ(), uASEJ.getRolASEJ(),
                oASEJ == null ? null : oASEJ.getIdASEJ(), oASEJ == null ? null : oASEJ.getNombreCompletoASEJ(), uASEJ.isActivoASEJ(), uASEJ.getUltimoAccesoASEJ(),
                uASEJ.getBloqueadoHastaASEJ() != null && uASEJ.getBloqueadoHastaASEJ().isAfter(ahoraASEJ));
    }
}
