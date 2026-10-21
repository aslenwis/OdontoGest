/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

/**
 * Datos de la clínica para las cabeceras de los documentos PDF (configuración {@code clinica.*}).
 *
 * @param logoRuta archivo de imagen del logo ({@code ""} = sin logo)
 */
public record DatosClinica(String nombre, String ruc, String direccion, String telefono, String email, String logoRuta) {

    public DatosClinica(String nombre, String ruc, String direccion, String telefono, String email) {
        this(nombre, ruc, direccion, telefono, email, "");
    }
}
