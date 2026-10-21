/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.integracion.dni;

import com.odontogest.excepcion.IntegracionException;

import java.util.Optional;

/**
 * Consulta de nombres por DNI en una API externa (RF-PAC-06).
 */
public interface ConsultaDniGateway {

    /**
     * @return vacío si la API no encontró el DNI
     * @throws IntegracionException si la API no responde, rechaza el token o devuelve algo inesperado
     */
    Optional<DatosPersona> consultar(String dni, ParametrosDni parametros);
}
