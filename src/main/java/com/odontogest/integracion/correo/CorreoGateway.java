/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.integracion.correo;

import com.odontogest.excepcion.IntegracionException;

/**
 * Envío de correo electrónico (recordatorios de citas y correo de prueba).
 */
public interface CorreoGateway {

    /**
     * @throws IntegracionException si el servidor rechaza la conexión, las credenciales o el destinatario
     */
    void enviar(ParametrosSmtp parametros, String destino, String asunto, String cuerpo);
}
