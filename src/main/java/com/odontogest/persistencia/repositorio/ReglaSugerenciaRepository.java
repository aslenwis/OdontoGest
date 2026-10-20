/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.persistencia.repositorio;

import com.odontogest.modelo.ReglaSugerencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Acceso a las reglas del motor de sugerencias (tabla {@code regla_sugerencia}).
 */
public interface ReglaSugerenciaRepository extends JpaRepository<ReglaSugerencia, Long> {

    @Query("""
            SELECT r FROM ReglaSugerencia r JOIN FETCH r.procedimiento LEFT JOIN FETCH r.hallazgoTipo
            ORDER BY r.prioridad, r.id""")
    List<ReglaSugerencia> listarConRelaciones();
}
