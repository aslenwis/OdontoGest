/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.persistencia.repositorio;

import com.odontogest.modelo.EstadoOdontogramaRegistro;
import com.odontogest.modelo.Odontograma;
import com.odontogest.modelo.TipoOdontograma;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a la tabla {@code odontograma}.
 */
public interface OdontogramaRepository extends JpaRepository<Odontograma, Long> {

    List<Odontograma> findByPacienteIdOrderByCreadoEnAscIdAsc(Long pacienteId);

    /** Odontogramas cerrados en orden de cierre: es el orden de la cadena de integridad. */
    List<Odontograma> findByPacienteIdAndEstadoOrderByCerradoEnAscIdAsc(Long pacienteId, EstadoOdontogramaRegistro estado);

    Optional<Odontograma> findFirstByPacienteIdAndEstado(Long pacienteId, EstadoOdontogramaRegistro estado);

    boolean existsByPacienteIdAndTipo(Long pacienteId, TipoOdontograma tipo);

    List<Odontograma> findByAtencionId(Long atencionId);
}
