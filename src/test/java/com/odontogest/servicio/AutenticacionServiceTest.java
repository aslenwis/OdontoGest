/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio;

import com.odontogest.PruebaIntegracion;
import com.odontogest.excepcion.NegocioException;
import com.odontogest.modelo.Rol;
import com.odontogest.modelo.Usuario;
import com.odontogest.persistencia.repositorio.UsuarioRepository;
import com.odontogest.seguridad.SesionUsuario;
import com.odontogest.servicio.dto.UsuarioSesion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Inicio de sesión con BCrypt y bloqueo tras 5 intentos fallidos (RF-SEG-01).
 */
class AutenticacionServiceTest extends PruebaIntegracion {

    private static final String CLAVE = "Clave123*";

    @Autowired private AutenticacionService autenticacionService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private SesionUsuario sesionUsuario;

    @AfterEach
    void cerrarSesion() {
        sesionUsuario.cerrar();
    }

    @Test
    void adminDemoIniciaSesion() {
        UsuarioSesion sesion = autenticacionService.iniciarSesion("admin", "Admin123*");

        assertThat(sesion.rol()).isEqualTo("ADMIN");
        assertThat(sesionUsuario.usuario()).contains(sesion);
        assertThat(usuarioRepository.findByUsernameIgnoreCase("admin").orElseThrow().getUltimoAcceso()).isNotNull();
    }

    @Test
    void usuarioNoDistingueMayusculas() {
        String username = crearUsuario();
        assertThat(autenticacionService.iniciarSesion(username.toUpperCase(), CLAVE).username()).isEqualTo(username);
    }

    @Test
    void credencialesInvalidasNoRevelanSiExisteElUsuario() {
        String username = crearUsuario();
        assertThatThrownBy(() -> autenticacionService.iniciarSesion(username, "otra"))
                .isInstanceOf(NegocioException.class).hasMessage("Usuario o contraseña incorrectos.");
        assertThatThrownBy(() -> autenticacionService.iniciarSesion("no.existe", "otra"))
                .isInstanceOf(NegocioException.class).hasMessage("Usuario o contraseña incorrectos.");
        assertThat(usuarioRepository.findByUsernameIgnoreCase(username).orElseThrow().getIntentosFallidos()).isEqualTo(1);
        assertThat(sesionUsuario.estaAutenticada()).isFalse();
    }

    @Test
    void bloqueaTrasCincoIntentosFallidosYDesbloqueaAlVencer() {
        String username = crearUsuario();
        for (int i = 1; i < AutenticacionService.MAX_INTENTOS; i++) {
            assertThatThrownBy(() -> autenticacionService.iniciarSesion(username, "mala"))
                    .hasMessage("Usuario o contraseña incorrectos.");
        }
        assertThatThrownBy(() -> autenticacionService.iniciarSesion(username, "mala"))
                .hasMessageContaining("bloqueado por 5 minutos");

        // Bloqueado: ni siquiera la contraseña correcta funciona.
        assertThatThrownBy(() -> autenticacionService.iniciarSesion(username, CLAVE))
                .isInstanceOf(NegocioException.class).hasMessageContaining("Usuario bloqueado");

        Usuario u = usuarioRepository.findByUsernameIgnoreCase(username).orElseThrow();
        assertThat(u.getBloqueadoHasta()).isAfter(LocalDateTime.now().plusMinutes(4));
        u.setBloqueadoHasta(LocalDateTime.now().minusSeconds(1));
        usuarioRepository.save(u);

        autenticacionService.iniciarSesion(username, CLAVE);
        Usuario desbloqueado = usuarioRepository.findByUsernameIgnoreCase(username).orElseThrow();
        assertThat(desbloqueado.getBloqueadoHasta()).isNull();
        assertThat(desbloqueado.getIntentosFallidos()).isZero();
    }

    @Test
    void usuarioInactivoNoIngresa() {
        String username = crearUsuario();
        Usuario u = usuarioRepository.findByUsernameIgnoreCase(username).orElseThrow();
        u.setActivo(false);
        usuarioRepository.save(u);

        assertThatThrownBy(() -> autenticacionService.iniciarSesion(username, CLAVE))
                .isInstanceOf(NegocioException.class).hasMessageContaining("desactivado");
    }

    @Test
    void cerrarSesionLimpiaElUsuarioActual() {
        autenticacionService.iniciarSesion("admin", "Admin123*");
        autenticacionService.cerrarSesion();
        assertThat(sesionUsuario.estaAutenticada()).isFalse();
    }

    private String crearUsuario() {
        String username = "u" + UUID.randomUUID().toString().substring(0, 8);
        Usuario u = new Usuario();
        u.setUsername(username);
        u.setPasswordHash(passwordEncoder.encode(CLAVE));
        u.setNombreCompleto("Usuario de prueba");
        u.setRol(Rol.RECEPCION);
        u.setCreadoEn(LocalDateTime.now());
        usuarioRepository.save(u);
        return username;
    }
}
