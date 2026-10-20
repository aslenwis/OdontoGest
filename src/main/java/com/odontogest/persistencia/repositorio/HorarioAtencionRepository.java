/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.persistencia.repositorio;

import com.odontogest.modelo.HorarioAtencion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Acceso a la tabla {@code horario_atencion}.
 */
public interface HorarioAtencionRepository extends JpaRepository<HorarioAtencion, Long> {

    List<HorarioAtencion> findByOdontologoIdOrderByDiaSemanaAscHoraInicioAsc(Long odontologoId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM HorarioAtencion h WHERE h.odontologo.id = :odontologoId")
    void eliminarPorOdontologo(Long odontologoId);
}
