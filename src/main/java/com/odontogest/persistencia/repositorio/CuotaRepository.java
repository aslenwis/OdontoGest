/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.persistencia.repositorio;

import com.odontogest.modelo.Cuota;
import com.odontogest.modelo.EstadoPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

/**
 * Acceso a las cuotas del cronograma (tabla {@code cuota}).
 */
public interface CuotaRepository extends JpaRepository<Cuota, Long> {

    List<Cuota> findByPlanIdOrderByNumeroAsc(long planId);

    /** Cuotas del paciente en planes con esos estados, en orden FIFO (vencimiento, plan, número). */
    @Query("""
            SELECT c FROM Cuota c JOIN FETCH c.plan p
            WHERE p.paciente.id = :pacienteId AND p.estado IN :estados
            ORDER BY c.vencimiento, p.id, c.numero""")
    List<Cuota> listarPorPaciente(long pacienteId, Collection<EstadoPlan> estados);
}
