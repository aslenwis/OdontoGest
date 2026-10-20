/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import java.util.List;

/**
 * Página de resultados de una búsqueda.
 *
 * @param numero página actual (0 = primera)
 * @param total  registros que cumplen el filtro
 */
public record Pagina<T>(List<T> contenido, int numero, int tamanio, long total) {

    public Pagina {
        contenido = List.copyOf(contenido);
    }

    public int totalPaginas() {
        return tamanio <= 0 ? 1 : (int) Math.max(1, (total + tamanio - 1) / tamanio);
    }
}
