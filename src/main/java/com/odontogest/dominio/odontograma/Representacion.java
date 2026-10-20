/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.odontograma;

/**
 * Cómo se registra un hallazgo en el odontograma: por superficie, por pieza, entre dos piezas contiguas o sobre un rango de piezas.
 */
public enum Representacion {
    SUPERFICIE, PIEZA, ENTRE_PIEZAS, RANGO_PIEZAS
}
