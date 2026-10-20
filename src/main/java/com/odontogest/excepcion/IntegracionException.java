/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.excepcion;

/**
 * Fallo de un servicio externo (API de DNI, correo). No es bloqueante: la UI lo muestra como aviso.
 */
public class IntegracionException extends RuntimeException {

    public IntegracionException(String mensaje) {
        super(mensaje);
    }

    public IntegracionException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
