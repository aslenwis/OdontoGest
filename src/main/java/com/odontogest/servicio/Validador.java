/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio;

import com.odontogest.excepcion.NegocioException;

import java.util.ArrayList;
import java.util.List;

/**
 * Acumula los errores de validación de un formulario y los lanza juntos en una sola
 * {@link NegocioException} (un mensaje por línea), para que el usuario los corrija de una vez.
 */
public final class Validador {

    private final List<String> errores = new ArrayList<>();

    /** Registra {@code mensaje} si {@code condicionDeError} es verdadera. */
    public Validador si(boolean condicionDeError, String mensaje) {
        if (condicionDeError) {
            errores.add(mensaje);
        }
        return this;
    }

    public Validador requerido(String valor, String campo) {
        return si(valor == null || valor.isBlank(), "Ingrese " + campo + ".");
    }

    public Validador requerido(Object valor, String campo) {
        return si(valor == null, "Seleccione " + campo + ".");
    }

    public Validador maximo(String valor, int largo, String campo) {
        return si(valor != null && valor.length() > largo, "El campo " + campo + " admite hasta " + largo + " caracteres.");
    }

    public boolean tieneErrores() {
        return !errores.isEmpty();
    }

    public void lanzarSiHayErrores() {
        if (!errores.isEmpty()) {
            throw new NegocioException(String.join("\n", errores));
        }
    }

    /** Texto sin espacios sobrantes; {@code null} si queda vacío. */
    public static String limpio(String valor) {
        if (valor == null) {
            return null;
        }
        String t = valor.trim().replaceAll("\\s+", " ");
        return t.isEmpty() ? null : t;
    }

    /** Texto multilínea: conserva saltos de línea, recorta extremos; {@code null} si queda vacío. */
    public static String limpioMultilinea(String valor) {
        if (valor == null) {
            return null;
        }
        String t = valor.strip();
        return t.isEmpty() ? null : t;
    }
}
