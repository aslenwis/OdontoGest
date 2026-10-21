/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.persistencia.repositorio;

import com.odontogest.modelo.Hallazgo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Acceso a la tabla {@code hallazgo}. No hay operaciones de actualización: los hallazgos son inmutables.
 */
public interface HallazgoRepository extends JpaRepository<Hallazgo, Long> {

    /** Eventos de todos los odontogramas del paciente, en orden de registro. */
    @Query("""
            SELECT h FROM Hallazgo h JOIN FETCH h.hallazgoTipo JOIN FETCH h.odontograma o
            WHERE o.paciente.id = :pacienteId ORDER BY h.registradoEn, h.id""")
    List<Hallazgo> listarPorPaciente(Long pacienteId);

    @Query("SELECT h FROM Hallazgo h JOIN FETCH h.hallazgoTipo WHERE h.odontograma.id = :odontogramaId ORDER BY h.id")
    List<Hallazgo> listarPorOdontograma(Long odontogramaId);

    long countByOdontogramaId(Long odontogramaId);
}
