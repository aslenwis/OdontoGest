/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.odontograma;

/**
 * Forma con la que se dibuja un hallazgo según la NTS N° 188-MINSA/DGIESP-2022, numeral 6.1
 * (columna {@code hallazgo_tipo.simbolo}; mismos valores que su CHECK).
 */
public enum SimboloHallazgo {
    /** Superficie pintada: caries (6.1.16), restauración definitiva (6.1.33). */
    RELLENO,
    /** Contorno de la superficie: restauración temporal (6.1.34). */
    CONTORNO_SUPERFICIE,
    /** Recorrido de fosas y fisuras sobre la cara oclusal: sellante (6.1.35). */
    TRAZO_FOSAS,
    /** Línea sobre la zona desgastada: superficie desgastada (6.1.36). */
    LINEA_DESGASTE,
    /** Aspa sobre corona y raíz: pieza dentaria ausente (6.1.20). */
    ASPA,
    /** Cuadrado que bordea la corona clínica: corona y corona temporal (6.1.3, 6.1.4). */
    CUADRADO_CORONA,
    /** Línea vertical en la raíz: tratamiento de conductos (6.1.37). */
    LINEA_RAIZ,
    /** Línea vertical en la raíz unida a un cuadrado en la corona: espigo-muñón (6.1.8). */
    ESPIGO_MUNON,
    /** Pulpa coronal dibujada: pulpotomía (6.1.27). */
    PULPA_CORONAL,
    /** Flecha vertical fuera de la pieza, alejándose del plano oclusal: pieza extruida (6.1.24). */
    FLECHA_EXTRUSION,
    /** Flecha vertical fuera de la pieza, dirigida hacia el plano oclusal: pieza intruida (6.1.25). */
    FLECHA_INTRUSION,
    /** Flecha curva a nivel oclusal: giroversión (6.1.13). */
    FLECHA_GIRO,
    /** Flecha en zigzag hacia el plano oclusal: pieza en erupción (6.1.23). */
    FLECHA_ZIGZAG,
    /** Línea sobre la corona o la raíz: fractura dental (6.1.10). */
    LINEA_FRACTURA,
    /** Triángulo por encima o debajo de la raíz: pieza en clavija (6.1.21). */
    TRIANGULO_RAIZ,
    /** Circunferencia alrededor del número: geminación (6.1.12). */
    CIRCULO_NUMERO,
    /** Dos circunferencias interceptadas alrededor de los números: fusión (6.1.11). */
    CIRCULOS_NUMEROS,
    /** Circunferencia con "S" entre los ápices de dos piezas: pieza supernumeraria (6.1.26). */
    CIRCULO_S,
    /** Dos flechas curvas entrecruzadas sobre los números: transposición (6.1.38). */
    FLECHAS_CRUZADAS,
    /** Paréntesis invertido entre dos piezas: diastema (6.1.6). */
    PARENTESIS,
    /** Línea horizontal con verticales sobre los pilares, a nivel de los ápices: prótesis fija (6.1.29). */
    PUENTE,
    /** Dos líneas horizontales paralelas a nivel de los ápices: prótesis removible y completa (6.1.30, 6.1.31). */
    LINEAS_PARALELAS,
    /** Cuadrados con cruz en los ápices de los extremos unidos por una línea: aparato ortodóntico fijo (6.1.1). */
    CUADROS_APICES,
    /** Línea en zigzag a la altura de los ápices: aparato ortodóntico removible (6.1.2). */
    ZIGZAG_APICES,
    /** Línea horizontal sobre las coronas: edéntulo total (6.1.7). */
    LINEA_CORONAS,
    /** Solo la sigla en el recuadro (p. ej. RR, IMP, FFP, MAC, M1). */
    SOLO_SIGLA
}
