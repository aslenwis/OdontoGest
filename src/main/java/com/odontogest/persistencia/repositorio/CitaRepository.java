/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.persistencia.repositorio;

import com.odontogest.modelo.Cita;
import com.odontogest.modelo.EstadoCita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Acceso a las citas (tabla {@code cita}). Las fechas se comparan como texto ISO gracias al converter, que conserva
 * el orden cronológico.
 */
public interface CitaRepository extends JpaRepository<Cita, Long> {

    /** {@code solapan(a, b) ⇔ a.inicio < b.fin ∧ b.inicio < a.fin}, solo entre citas que ocupan el horario. */
    @Query("""
            SELECT COUNT(c) > 0 FROM Cita c
            WHERE c.odontologo.id = :odontologoId AND c.estado IN :ocupan
              AND c.inicio < :fin AND c.fin > :inicio AND c.id <> :excluirId""")
    boolean existeSolapamientoOdontologo(long odontologoId, LocalDateTime inicio, LocalDateTime fin, long excluirId,
                                         Collection<EstadoCita> ocupan);

    @Query("""
            SELECT COUNT(c) > 0 FROM Cita c
            WHERE c.paciente.id = :pacienteId AND c.estado IN :ocupan
              AND c.inicio < :fin AND c.fin > :inicio AND c.id <> :excluirId""")
    boolean existeSolapamientoPaciente(long pacienteId, LocalDateTime inicio, LocalDateTime fin, long excluirId,
                                       Collection<EstadoCita> ocupan);

    /** Citas de un odontólogo que se cruzan con {@code [desde, hasta)}. */
    @Query("""
            SELECT c FROM Cita c JOIN FETCH c.paciente JOIN FETCH c.odontologo
            WHERE c.odontologo.id = :odontologoId AND c.estado IN :estados
              AND c.inicio < :hasta AND c.fin > :desde
            ORDER BY c.inicio""")
    List<Cita> listarOdontologo(long odontologoId, LocalDateTime desde, LocalDateTime hasta, Collection<EstadoCita> estados);

    /** Citas de un paciente que se cruzan con {@code [desde, hasta)}. */
    @Query("""
            SELECT c FROM Cita c JOIN FETCH c.paciente JOIN FETCH c.odontologo
            WHERE c.paciente.id = :pacienteId AND c.estado IN :estados
              AND c.inicio < :hasta AND c.fin > :desde
            ORDER BY c.inicio""")
    List<Cita> listarPaciente(long pacienteId, LocalDateTime desde, LocalDateTime hasta, Collection<EstadoCita> estados);

    @Query("""
            SELECT c FROM Cita c JOIN FETCH c.odontologo
            WHERE c.paciente.id = :pacienteId ORDER BY c.inicio DESC""")
    List<Cita> listarPorPaciente(long pacienteId);
}
