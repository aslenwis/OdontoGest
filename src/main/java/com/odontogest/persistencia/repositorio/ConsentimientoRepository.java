/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.persistencia.repositorio;

import com.odontogest.modelo.Consentimiento;
import com.odontogest.modelo.TipoConsentimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a la tabla {@code consentimiento}.
 */
public interface ConsentimientoRepository extends JpaRepository<Consentimiento, Long> {

    List<Consentimiento> findByPacienteIdOrderByAceptadoEnDesc(Long pacienteId);

    Optional<Consentimiento> findFirstByPacienteIdAndTipoOrderByAceptadoEnDesc(Long pacienteId, TipoConsentimiento tipo);

    /** Consentimiento informado de tratamiento firmado para un plan. */
    Optional<Consentimiento> findFirstByPlanIdAndTipoOrderByAceptadoEnDesc(Long planId, TipoConsentimiento tipo);
}
