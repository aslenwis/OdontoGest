/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.finanzas;

import java.time.LocalDate;

/**
 * Cuota generada por {@link GeneradorCronograma}.
 *
 * @param numero 0 = cuota inicial; 1..n = cuotas del cronograma
 */
public record CuotaProgramada(int numero, LocalDate vencimiento, long montoCent) {
}
