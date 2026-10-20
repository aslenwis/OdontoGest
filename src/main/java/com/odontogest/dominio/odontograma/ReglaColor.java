/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.odontograma;

/**
 * Regla del catálogo que decide el color de un hallazgo (NTS 188: azul = buen estado, rojo = mal estado o patológico).
 */
public enum ReglaColor {
    POR_ESTADO, SIEMPRE_ROJO, SIEMPRE_AZUL
}
