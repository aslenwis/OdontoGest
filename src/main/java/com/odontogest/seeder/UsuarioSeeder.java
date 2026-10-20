/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.seeder;

import com.odontogest.modelo.Rol;
import com.odontogest.modelo.Usuario;
import com.odontogest.persistencia.repositorio.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Usuarios de demostración. Fase 0: solo {@code admin / Admin123*}.
 * Los usuarios {@code odontologo} y {@code recepcion} se agregan en la Fase 2, cuando exista
 * {@code OdontologoSeeder} (el rol ODONTOLOGO exige un odontólogo vinculado).
 */
@Component
@RequiredArgsConstructor
public class UsuarioSeeder {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public void sembrar() {
        crear("admin", "Admin123*", "Administrador del sistema", Rol.ADMIN);
    }

    private void crear(String username, String password, String nombreCompleto, Rol rol) {
        Usuario u = new Usuario();
        u.setUsername(username);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setNombreCompleto(nombreCompleto);
        u.setRol(rol);
        u.setActivo(true);
        u.setCreadoEn(LocalDateTime.now());
        usuarioRepository.save(u);
    }
}
