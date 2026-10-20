/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.odontograma;

/**
 * Resultado de verificar la cadena de integridad de los odontogramas de un paciente.
 *
 * @param odontogramaIdInvalido primer odontograma cuyo hash no coincide ({@code null} si la cadena es íntegra)
 */
public record ResultadoVerificacion(boolean integra, Long odontogramaIdInvalido, String motivo) {

    public static ResultadoVerificacion correcta() {
        return new ResultadoVerificacion(true, null, null);
    }

    public static ResultadoVerificacion falla(long odontogramaId, String motivo) {
        return new ResultadoVerificacion(false, odontogramaId, motivo);
    }
}
