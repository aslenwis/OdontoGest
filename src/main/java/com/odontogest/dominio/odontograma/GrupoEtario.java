/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.odontograma;

/**
 * Grupos de edad del reporte epidemiológico (RF-REP-05). Indica qué índice tiene sentido en cada grupo: ceo-d con
 * dentición decidua (hasta los 11 años), CPO-D con dentición permanente (desde los 6); de 6 a 11 años, dentición
 * mixta, se informan ambos.
 */
public enum GrupoEtario {
    INFANCIA("0 a 5 años", 0, 5, false, true),
    ESCOLAR("6 a 11 años", 6, 11, true, true),
    ADOLESCENCIA("12 a 17 años", 12, 17, true, false),
    JOVEN("18 a 29 años", 18, 29, true, false),
    ADULTO("30 a 44 años", 30, 44, true, false),
    ADULTO_MAYOR_JOVEN("45 a 59 años", 45, 59, true, false),
    ADULTO_MAYOR("60 años a más", 60, Integer.MAX_VALUE, true, false);

    public final String etiqueta;
    public final int edadMinima;
    public final int edadMaxima;
    /** El CPO-D es representativo en este grupo. */
    public final boolean aplicaCpod;
    /** El ceo-d es representativo en este grupo. */
    public final boolean aplicaCeod;

    GrupoEtario(String etiqueta, int edadMinima, int edadMaxima, boolean aplicaCpod, boolean aplicaCeod) {
        this.etiqueta = etiqueta;
        this.edadMinima = edadMinima;
        this.edadMaxima = edadMaxima;
        this.aplicaCpod = aplicaCpod;
        this.aplicaCeod = aplicaCeod;
    }

    public static GrupoEtario de(int edad) {
        if (edad < 0) {
            throw new IllegalArgumentException("Edad negativa: " + edad);
        }
        for (GrupoEtario g : values()) {
            if (edad <= g.edadMaxima) {
                return g;
            }
        }
        return ADULTO_MAYOR;
    }
}
