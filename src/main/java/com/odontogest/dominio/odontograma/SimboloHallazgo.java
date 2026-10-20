/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.odontograma;

/**
 * Forma con la que se dibuja un hallazgo (columna {@code hallazgo_tipo.simbolo}; mismos valores que su CHECK).
 */
public enum SimboloHallazgo {
    RELLENO, CONTORNO_SUPERFICIE, ASPA, CIRCUNFERENCIA, LINEA_RAIZ,
    FLECHA_ARRIBA, FLECHA_ABAJO, FLECHA_GIRO, LINEA_FRACTURA,
    PARENTESIS, PUENTE, LLAVE, SOLO_SIGLA, RAIZ_TRIANGULO, ZIGZAG
}
