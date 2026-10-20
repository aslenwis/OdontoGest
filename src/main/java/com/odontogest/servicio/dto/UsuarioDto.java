/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import com.odontogest.modelo.Rol;

import java.time.LocalDateTime;

/**
 * Usuario del sistema para el listado de administración.
 *
 * @param bloqueado bloqueado temporalmente por intentos fallidos
 */
public record UsuarioDto(long id, String username, String nombreCompleto, Rol rol, Long odontologoId,
                         String odontologoNombre, boolean activo, LocalDateTime ultimoAcceso, boolean bloqueado) {
}
