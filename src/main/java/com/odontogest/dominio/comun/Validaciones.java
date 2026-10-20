/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.comun;

import java.time.LocalDate;
import java.time.Period;
import java.util.regex.Pattern;

/**
 * Validaciones de formato de datos personales (Perú).
 */
public final class Validaciones {

    public static final int MAYORIA_DE_EDAD = 18;

    private static final Pattern DNI = Pattern.compile("\\d{8}");
    /** Carné de extranjería: 9 a 12 caracteres alfanuméricos. */
    private static final Pattern CE = Pattern.compile("[A-Za-z0-9]{9,12}");
    private static final Pattern PASAPORTE = Pattern.compile("[A-Za-z0-9]{6,12}");
    private static final Pattern EMAIL = Pattern.compile("^[\\w.%+-]+@[\\w-]+(\\.[\\w-]+)*\\.[A-Za-z]{2,}$");
    /** Celular peruano (9 dígitos empezando en 9) o fijo con código de área opcional; admite +51, espacios, guiones y paréntesis. */
    private static final Pattern TELEFONO = Pattern.compile("^(\\+?51)?(9\\d{8}|0?\\d{1,2}\\d{6,7})$");

    private Validaciones() {
    }

    public static boolean esDni(String valor) {
        return valor != null && DNI.matcher(valor).matches();
    }

    public static boolean esCarneExtranjeria(String valor) {
        return valor != null && CE.matcher(valor).matches();
    }

    public static boolean esPasaporte(String valor) {
        return valor != null && PASAPORTE.matcher(valor).matches();
    }

    public static boolean esEmail(String valor) {
        return valor != null && valor.length() <= 254 && EMAIL.matcher(valor.trim()).matches();
    }

    public static boolean esTelefono(String valor) {
        if (valor == null) {
            return false;
        }
        String limpio = valor.replaceAll("[\\s()-]", "");
        return TELEFONO.matcher(limpio).matches();
    }

    public static int edad(LocalDate fechaNacimiento, LocalDate hoy) {
        if (fechaNacimiento.isAfter(hoy)) {
            throw new IllegalArgumentException("La fecha de nacimiento es posterior a hoy");
        }
        return Period.between(fechaNacimiento, hoy).getYears();
    }

    /** Menor de 18 años: requiere apoderado (RF-PAC-03). */
    public static boolean esMenorDeEdad(LocalDate fechaNacimiento, LocalDate hoy) {
        return edad(fechaNacimiento, hoy) < MAYORIA_DE_EDAD;
    }
}
