/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

/**
 * Promedios de CPO-D y ceo-d de un grupo de edad (RF-REP-05). Un índice es {@code null} cuando no es
 * representativo para la edad del grupo (ceo-d desde los 12 años, CPO-D antes de los 6).
 *
 * @param nivelCpod nivel OMS del CPO-D promedio, o {@code null}
 */
public record GrupoIndicesDto(String grupo, int pacientes, Double cpod, Double c, Double p, Double o, Double ceod,
                              String nivelCpod) {
}
