/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.seeder;

import com.odontogest.modelo.Odontologo;
import com.odontogest.modelo.Rol;
import com.odontogest.modelo.Usuario;
import com.odontogest.persistencia.repositorio.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Usuarios de demostración: {@code admin / Admin123*}, {@code odontologo / Odonto123*} (vinculado al
 * odontólogo general) y {@code recepcion / Recep123*}.
 */
@Component
@RequiredArgsConstructor
public class UsuarioSeeder {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock reloj;

    public void sembrar(Odontologo odontologoGeneral) {
        crear("admin", "Admin123*", "Administrador del sistema", Rol.ADMIN, null);
        crear("odontologo", "Odonto123*", odontologoGeneral.getNombreCompleto(), Rol.ODONTOLOGO, odontologoGeneral);
        crear("recepcion", "Recep123*", "Recepción", Rol.RECEPCION, null);
    }

    private void crear(String username, String password, String nombreCompleto, Rol rol, Odontologo odontologo) {
        Usuario u = new Usuario();
        u.setUsername(username);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setNombreCompleto(nombreCompleto);
        u.setRol(rol);
        u.setOdontologo(odontologo);
        u.setActivo(true);
        u.setCreadoEn(LocalDateTime.now(reloj).truncatedTo(ChronoUnit.SECONDS));
        usuarioRepository.save(u);
    }
}
