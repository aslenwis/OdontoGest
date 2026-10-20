/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.odontograma;

/**
 * Lugar que ocupa un hallazgo vigente: pieza (o rango), superficie y capa de exclusión.
 * Dos hallazgos con la misma clave se excluyen: queda el más reciente.
 *
 * @param superficie {@code null} para hallazgos de pieza o de rango
 */
public record ClaveCapa(int piezaInicio, int piezaFin, Superficie superficie, String capa) {

    public boolean esDeRango() {
        return piezaInicio != piezaFin;
    }
}
