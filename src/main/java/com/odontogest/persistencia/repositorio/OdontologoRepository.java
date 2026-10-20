/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.persistencia.repositorio;

import com.odontogest.modelo.Odontologo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a la tabla {@code odontologo}.
 */
public interface OdontologoRepository extends JpaRepository<Odontologo, Long> {

    List<Odontologo> findAllByOrderByApellidosAscNombresAsc();

    List<Odontologo> findByActivoTrueOrderByApellidosAscNombresAsc();

    Optional<Odontologo> findByCopIgnoreCase(String cop);
}
