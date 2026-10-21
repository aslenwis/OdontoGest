/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.persistencia.repositorio;

import com.odontogest.modelo.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Acceso a los pagos (tabla {@code pago}).
 */
public interface PagoRepository extends JpaRepository<Pago, Long> {

    @Query("""
            SELECT p FROM Pago p JOIN FETCH p.paciente
            WHERE p.paciente.id = :pacienteId ORDER BY p.fecha DESC, p.id DESC""")
    List<Pago> listarPorPaciente(long pacienteId);

    /** Pagos con fecha en {@code [desde, hasta)}, incluidos los anulados. */
    @Query("""
            SELECT p FROM Pago p JOIN FETCH p.paciente
            WHERE p.fecha >= :desde AND p.fecha < :hasta ORDER BY p.fecha, p.id""")
    List<Pago> listarEntre(LocalDateTime desde, LocalDateTime hasta);
}
