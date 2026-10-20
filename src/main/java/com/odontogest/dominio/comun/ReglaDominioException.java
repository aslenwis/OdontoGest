/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.comun;

/**
 * Regla de negocio violada dentro del dominio puro (p. ej. pago mayor que el saldo pendiente).
 * <p>
 * Equivale a {@code NegocioException} pero sin depender de otros paquetes: el mensaje está pensado para
 * el usuario y la UI lo presenta como advertencia.
 */
public class ReglaDominioException extends RuntimeException {

    public ReglaDominioException(String mensaje) {
        super(mensaje);
    }
}
