/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Archivo de respaldo (.zip) de la carpeta {@code respaldos/}.
 *
 * @param ruta        ruta absoluta del archivo
 * @param automatico  creado al cerrar la aplicación (se conservan solo los últimos {@code respaldo.mantener})
 */
public record RespaldoDto(String nombre, String ruta, LocalDateTime fecha, long tamanioBytes, boolean automatico) {

    /** Tamaño legible, p. ej. "1.4 MB". */
    public String tamanio() {
        if (tamanioBytes < 1024) {
            return tamanioBytes + " B";
        }
        if (tamanioBytes < 1024 * 1024) {
            return String.format(Locale.ROOT, "%.1f KB", tamanioBytes / 1024.0);
        }
        return String.format(Locale.ROOT, "%.1f MB", tamanioBytes / (1024.0 * 1024));
    }
}
