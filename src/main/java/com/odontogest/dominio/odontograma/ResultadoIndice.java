/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.odontograma;

/**
 * Resultado de un índice CPO-D o ceo-d.
 *
 * @param p               piezas perdidas (CPO-D) o extraídas/indicadas para extracción, "e" (ceo-d)
 * @param piezasEvaluadas piezas del universo del índice (32 o 28 permanentes; 20 deciduas)
 */
public record ResultadoIndice(int c, int p, int o, int total, int piezasEvaluadas, NivelSeveridadOms nivel) {
}
