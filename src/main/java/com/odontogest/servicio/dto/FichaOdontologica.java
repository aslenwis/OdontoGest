/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

/**
 * Datos de la ficha odontológica en PDF (RF-ODO-10).
 *
 * @param imagenPng gráfico del odontograma dibujado por la UI con el mismo renderizador de pantalla
 */
public record FichaOdontologica(DatosClinica clinica, PacienteDetalle paciente, OdontogramaVista vista,
                                String odontologoNombre, String odontologoCop, byte[] imagenPng) {
}
