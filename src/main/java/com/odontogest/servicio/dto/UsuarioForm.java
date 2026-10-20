/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import com.odontogest.modelo.Rol;

/**
 * Datos del formulario de usuario.
 *
 * @param password solo al crear; para cambiarla se usa "Restablecer contraseña"
 */
public record UsuarioForm(Long id, String username, String nombreCompleto, Rol rol, Long odontologoId,
                          boolean activo, String password) {
}
