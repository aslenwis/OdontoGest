/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import java.util.List;

/**
 * Resultado de "Verificar integridad de la base de datos": {@code PRAGMA integrity_check} y
 * {@code PRAGMA foreign_key_check}.
 *
 * @param problemas      mensajes de {@code integrity_check} distintos de "ok" (como máximo 100)
 * @param clavesForaneas filas que apuntan a registros inexistentes
 */
public record IntegridadBaseDto(List<String> problemas, int clavesForaneas, long tamanioBytes) {

    public IntegridadBaseDto {
        problemas = List.copyOf(problemas);
    }

    public boolean correcta() {
        return problemas.isEmpty() && clavesForaneas == 0;
    }
}
