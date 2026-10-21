/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.persistencia.repositorio;

import com.odontogest.modelo.PlanItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

/**
 * Acceso a los ítems de los planes (tabla {@code plan_item}).
 */
public interface PlanItemRepository extends JpaRepository<PlanItem, Long> {

    @Query("""
            SELECT i FROM PlanItem i JOIN FETCH i.procedimiento
            WHERE i.plan.id = :planId ORDER BY i.fase, i.orden, i.id""")
    List<PlanItem> listarPorPlan(long planId);

    @Query("""
            SELECT i FROM PlanItem i JOIN FETCH i.procedimiento JOIN FETCH i.plan
            WHERE i.plan.id IN :planIds ORDER BY i.plan.id, i.fase, i.orden, i.id""")
    List<PlanItem> listarPorPlanes(Collection<Long> planIds);
}
