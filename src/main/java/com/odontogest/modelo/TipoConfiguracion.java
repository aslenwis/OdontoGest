/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.modelo;

/**
 * Tipo del valor de una clave de configuración (columna {@code configuracion.tipo}).
 * Los valores {@link #SECRETO} se guardan cifrados (Fase 7).
 */
public enum TipoConfiguracion {
    TEXTO, ENTERO, BOOLEANO, SECRETO, JSON
}
