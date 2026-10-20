/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.persistencia.repositorio;

import com.odontogest.modelo.Configuracion;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a la tabla {@code configuracion}.
 */
public interface ConfiguracionRepository extends JpaRepository<Configuracion, String> {
}
