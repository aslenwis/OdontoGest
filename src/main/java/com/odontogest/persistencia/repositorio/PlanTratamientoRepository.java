/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.persistencia.repositorio;

import com.odontogest.modelo.EstadoPlan;
import com.odontogest.modelo.PlanTratamiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

/**
 * Acceso a los planes de tratamiento (tabla {@code plan_tratamiento}).
 */
public interface PlanTratamientoRepository extends JpaRepository<PlanTratamiento, Long> {

    @Query("""
            SELECT p FROM PlanTratamiento p JOIN FETCH p.odontologo
            WHERE p.paciente.id = :pacienteId ORDER BY p.fecha DESC, p.id DESC""")
    List<PlanTratamiento> listarPorPaciente(long pacienteId);

    List<PlanTratamiento> findByPacienteIdAndEstadoIn(long pacienteId, Collection<EstadoPlan> estados);
}
