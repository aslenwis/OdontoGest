/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.integracion.correo;

/**
 * Configuración del servidor de correo (claves {@code email.*}).
 *
 * @param remitente dirección "De:"; si está vacía se usa el usuario
 */
public record ParametrosSmtp(String host, int puerto, String usuario, String password, boolean starttls, String remitente,
                             String nombreRemitente) {

    /** Hay lo mínimo para intentar un envío. */
    public boolean configurado() {
        return host != null && !host.isBlank() && puerto > 0
                && ((remitente != null && !remitente.isBlank()) || (usuario != null && !usuario.isBlank()));
    }

    public String desde() {
        return remitente != null && !remitente.isBlank() ? remitente : usuario;
    }
}
