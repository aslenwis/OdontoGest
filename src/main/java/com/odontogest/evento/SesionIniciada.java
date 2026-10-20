/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.evento;

import com.odontogest.servicio.dto.UsuarioSesion;

/**
 * Evento de aplicación publicado cuando un usuario inicia sesión correctamente.
 */
public record SesionIniciada(UsuarioSesion usuario) {
}
