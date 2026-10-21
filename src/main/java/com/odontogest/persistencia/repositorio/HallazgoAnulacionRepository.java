/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.persistencia.repositorio;

import com.odontogest.modelo.HallazgoAnulacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Acceso a la tabla {@code hallazgo_anulacion}.
 */
public interface HallazgoAnulacionRepository extends JpaRepository<HallazgoAnulacion, Long> {

    @Query("""
            SELECT a FROM HallazgoAnulacion a JOIN FETCH a.hallazgo h
            WHERE h.odontograma.paciente.id = :pacienteId""")
    List<HallazgoAnulacion> listarPorPaciente(Long pacienteId);

    boolean existsByHallazgoId(Long hallazgoId);
}
