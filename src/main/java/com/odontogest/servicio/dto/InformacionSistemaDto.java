/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Datos de la ventana "Acerca de": identificación del software (para el registro ante INDECOPI), versión y rutas
 * de la carpeta de datos.
 *
 * @param compilado fecha de compilación; {@code null} al ejecutar desde el IDE sin {@code build-info}
 */
public record InformacionSistemaDto(String nombre, String descripcion, String version, LocalDateTime compilado,
                                    List<String> autores, String curso, String universidad, int anio, String aviso,
                                    String javaVersion, String carpetaDatos, String baseDatos, String carpetaLogs,
                                    String carpetaRespaldos) {

    public InformacionSistemaDto {
        autores = List.copyOf(autores);
    }
}
