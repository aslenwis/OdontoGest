/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.seguridad;

import com.odontogest.excepcion.PermisoDenegadoException;
import com.odontogest.servicio.dto.UsuarioSesion;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Usuario con sesión iniciada (bean singleton: la aplicación es monousuario por instancia).
 * <p>
 * Se escribe desde hilos de fondo y se lee desde el hilo de UI, por eso el campo es {@code volatile}.
 * La matriz de permisos ({@code tiene}/{@code exigir}) se agrega en la Fase 2.
 */
@Component
public class SesionUsuario {

    private volatile UsuarioSesion actual;

    public void iniciar(UsuarioSesion usuario) {
        this.actual = usuario;
    }

    public void cerrar() {
        this.actual = null;
    }

    public boolean estaAutenticada() {
        return actual != null;
    }

    public Optional<UsuarioSesion> usuario() {
        return Optional.ofNullable(actual);
    }

    /** Usuario actual; lanza {@link PermisoDenegadoException} si no hay sesión. */
    public UsuarioSesion exigirUsuario() {
        UsuarioSesion u = actual;
        if (u == null) {
            throw new PermisoDenegadoException("No hay una sesión activa. Inicie sesión nuevamente.");
        }
        return u;
    }
}
