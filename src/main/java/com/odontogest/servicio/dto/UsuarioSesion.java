/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

/**
 * Datos del usuario autenticado que la UI puede consultar.
 *
 * @param rol nombre del rol ({@code ADMIN}, {@code ODONTOLOGO}, {@code RECEPCION}); se expone como
 *            texto para que la UI no dependa de los enums del paquete {@code modelo}
 */
public record UsuarioSesion(long id, String username, String nombreCompleto, String rol, Long odontologoId) {
}
