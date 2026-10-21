/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.persistencia.repositorio;

import com.odontogest.modelo.Auditoria;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Acceso al registro de auditoría (tabla {@code auditoria}).
 */
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    /** Registros en {@code [desde, hasta)}, del más reciente al más antiguo; {@code accion} vacía = todas. */
    @Query("""
            SELECT a FROM Auditoria a
            WHERE a.fecha >= :desde AND a.fecha < :hasta AND (:accion = '' OR a.accion = :accion)
            ORDER BY a.fecha DESC, a.id DESC""")
    List<Auditoria> buscar(LocalDateTime desde, LocalDateTime hasta, String accion, Pageable pagina);

    List<Auditoria> findByEntidadAndEntidadIdOrderByIdAsc(String entidad, Long entidadId);

    @Query("SELECT DISTINCT a.accion FROM Auditoria a ORDER BY a.accion")
    List<String> acciones();
}
