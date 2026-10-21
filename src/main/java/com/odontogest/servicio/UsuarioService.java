/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio;

import com.odontogest.excepcion.NegocioException;
import com.odontogest.modelo.Odontologo;
import com.odontogest.modelo.Rol;
import com.odontogest.modelo.Usuario;
import com.odontogest.persistencia.repositorio.OdontologoRepository;
import com.odontogest.persistencia.repositorio.UsuarioRepository;
import com.odontogest.seguridad.Permiso;
import com.odontogest.seguridad.SesionUsuario;
import com.odontogest.servicio.dto.UsuarioDto;
import com.odontogest.servicio.dto.UsuarioForm;
import com.odontogest.servicio.dto.UsuarioSesion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Administración de usuarios (RF-SEG-03) y cambio de la propia contraseña (RF-SEG-04).
 * <p>
 * Reglas: un usuario ODONTOLOGO se vincula a un odontólogo activo; siempre debe quedar al menos un
 * administrador activo; nadie puede desactivarse a sí mismo. Contraseñas de 8 o más caracteres con letras
 * y dígitos.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UsuarioService {

    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9._-]{3,30}");
    private static final int LARGO_MINIMO_PASSWORD = 8;

    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoria;
    private final OdontologoRepository odontologoRepository;
    private final PasswordEncoder passwordEncoder;
    private final SesionUsuario sesionUsuario;

    public List<UsuarioDto> listar() {
        LocalDateTime ahora = LocalDateTime.now();
        return usuarioRepository.findAllByOrderByUsernameAsc().stream().map(u -> dto(u, ahora)).toList();
    }

    @Transactional
    public long guardar(UsuarioForm form) {
        UsuarioSesion actual = sesionUsuario.exigir(Permiso.USUARIOS);
        boolean nuevo = form.id() == null;
        String username = Validador.limpio(form.username());
        String nombre = Validador.limpio(form.nombreCompleto());

        Validador v = new Validador()
                .requerido(nombre, "el nombre completo")
                .maximo(nombre, 120, "nombre completo")
                .requerido(form.rol(), "el rol")
                .si(form.rol() == Rol.ODONTOLOGO && form.odontologoId() == null,
                        "Un usuario con rol Odontólogo debe estar vinculado a un odontólogo.");
        if (nuevo) {
            v.requerido(username, "el nombre de usuario")
                    .si(username != null && !USERNAME.matcher(username).matches(),
                            "El usuario admite de 3 a 30 letras, dígitos, punto, guion o guion bajo.");
            validarPassword(form.password(), v);
        }
        v.lanzarSiHayErrores();

        Usuario u;
        if (nuevo) {
            usuarioRepository.findByUsernameIgnoreCase(username).ifPresent(x -> {
                throw new NegocioException("El nombre de usuario " + username + " ya existe.");
            });
            u = new Usuario();
            u.setUsername(username);
            u.setPasswordHash(passwordEncoder.encode(form.password()));
            u.setCreadoEn(LocalDateTime.now());
        } else {
            u = buscar(form.id());
            boolean pierdeAdmin = u.getRol() == Rol.ADMIN && u.isActivo() && (form.rol() != Rol.ADMIN || !form.activo());
            if (pierdeAdmin && usuarioRepository.countByRolAndActivoTrue(Rol.ADMIN) <= 1) {
                throw new NegocioException("Debe quedar al menos un administrador activo.");
            }
            if (u.getId() == actual.id() && !form.activo()) {
                throw new NegocioException("No puede desactivar su propio usuario.");
            }
        }
        u.setNombreCompleto(nombre);
        u.setRol(form.rol());
        u.setOdontologo(form.rol() == Rol.ODONTOLOGO ? odontologoActivo(form.odontologoId()) : null);
        u.setActivo(form.activo());
        Usuario guardado = usuarioRepository.save(u);
        log.info("Usuario {} {} por {}", guardado.getUsername(), nuevo ? "creado" : "actualizado", actual.username());
        auditoria.registrar(nuevo ? "USUARIO_CREAR" : "USUARIO_EDITAR", "usuario", guardado.getId(),
                Map.of("username", guardado.getUsername(), "rol", guardado.getRol().name()));
        return guardado.getId();
    }

    /** Asigna una contraseña nueva y desbloquea al usuario. */
    @Transactional
    public void restablecerPassword(long usuarioId, String nueva) {
        sesionUsuario.exigir(Permiso.USUARIOS);
        Validador v = new Validador();
        validarPassword(nueva, v);
        v.lanzarSiHayErrores();
        Usuario u = buscar(usuarioId);
        u.setPasswordHash(passwordEncoder.encode(nueva));
        u.setIntentosFallidos(0);
        u.setBloqueadoHasta(null);
        auditoria.registrar("USUARIO_RESTABLECER_PASSWORD", "usuario", usuarioId, null);
    }

    /** El usuario de la sesión cambia su propia contraseña. */
    @Transactional
    public void cambiarPassword(String actual, String nueva, String confirmacion) {
        UsuarioSesion sesion = sesionUsuario.exigirUsuario();
        Usuario u = buscar(sesion.id());
        if (actual == null || !passwordEncoder.matches(actual, u.getPasswordHash())) {
            throw new NegocioException("La contraseña actual no es correcta.");
        }
        Validador v = new Validador()
                .si(nueva != null && !nueva.equals(confirmacion), "La confirmación no coincide con la nueva contraseña.")
                .si(nueva != null && nueva.equals(actual), "La nueva contraseña debe ser distinta de la actual.");
        validarPassword(nueva, v);
        v.lanzarSiHayErrores();
        u.setPasswordHash(passwordEncoder.encode(nueva));
        auditoria.registrar("USUARIO_CAMBIAR_PASSWORD", "usuario", u.getId(), null);
    }

    private static void validarPassword(String password, Validador v) {
        v.si(password == null || password.length() < LARGO_MINIMO_PASSWORD
                        || !password.chars().anyMatch(Character::isLetter) || !password.chars().anyMatch(Character::isDigit),
                "La contraseña debe tener al menos " + LARGO_MINIMO_PASSWORD + " caracteres, con letras y números.");
    }

    private Odontologo odontologoActivo(Long id) {
        Odontologo o = odontologoRepository.findById(id)
                .orElseThrow(() -> new NegocioException("El odontólogo ya no existe en la base de datos."));
        if (!o.isActivo()) {
            throw new NegocioException("El odontólogo " + o.getNombreCompleto() + " está inactivo.");
        }
        return o;
    }

    private Usuario buscar(long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new NegocioException("El usuario ya no existe en la base de datos."));
    }

    private static UsuarioDto dto(Usuario u, LocalDateTime ahora) {
        Odontologo o = u.getOdontologo();
        return new UsuarioDto(u.getId(), u.getUsername(), u.getNombreCompleto(), u.getRol(),
                o == null ? null : o.getId(), o == null ? null : o.getNombreCompleto(), u.isActivo(), u.getUltimoAcceso(),
                u.getBloqueadoHasta() != null && u.getBloqueadoHasta().isAfter(ahora));
    }
}
