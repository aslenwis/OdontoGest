/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.persistencia.repositorio;

import com.odontogest.modelo.Rol;
import com.odontogest.modelo.Usuario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a la tabla {@code usuario}.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @EntityGraph(attributePaths = "odontologo")
    Optional<Usuario> findByUsernameIgnoreCase(String username);

    @EntityGraph(attributePaths = "odontologo")
    List<Usuario> findAllByOrderByUsernameAsc();

    long countByRolAndActivoTrue(Rol rol);
}
