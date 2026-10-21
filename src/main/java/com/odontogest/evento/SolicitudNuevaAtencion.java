/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.evento;

/**
 * Pide abrir el formulario de nueva atención del paciente activo (desde la cabecera de la ficha o la pestaña
 * Odontograma). Lo atiende la pestaña Atenciones; se publica en el hilo de UI.
 */
public record SolicitudNuevaAtencion() {
}
