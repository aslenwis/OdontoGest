/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.persistencia.repositorio;

import com.odontogest.modelo.HallazgoTipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Acceso al catálogo de hallazgos (tabla {@code hallazgo_tipo}).
 */
public interface HallazgoTipoRepository extends JpaRepository<HallazgoTipo, Long> {

    Optional<HallazgoTipo> findByCodigoIgnoreCase(String codigo);

    List<HallazgoTipo> findAllByOrderByOrdenAscNombreAsc();

    /** Hallazgos registrados con este tipo: si hay alguno, su estructura ya no puede cambiar. */
    @Query(value = "SELECT COUNT(*) FROM hallazgo WHERE hallazgo_tipo_id = :tipoId", nativeQuery = true)
    long contarHallazgos(Long tipoId);

    @Query("SELECT DISTINCT t.grupoExclusion FROM HallazgoTipo t ORDER BY t.grupoExclusion")
    List<String> gruposExclusion();
}
