/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.comun;

/**
 * Monto en letras para recibos: {@code 12050} céntimos → {@code "CIENTO VEINTE CON 50/100 SOLES"}.
 * <p>
 * Aplica el apócope ante MIL y MILLÓN ("VEINTIÚN MIL", "UN MILLÓN") y "CIEN" para la centena exacta.
 * Rango soportado: hasta 999 999 999 999 soles.
 */
public final class NumeroALetras {

    public static final long MAXIMO = 999_999_999_999L;

    private static final String[] UNIDADES = {
            "", "UNO", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE", "OCHO", "NUEVE",
            "DIEZ", "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE", "DIECISÉIS", "DIECISIETE", "DIECIOCHO", "DIECINUEVE",
            "VEINTE", "VEINTIUNO", "VEINTIDÓS", "VEINTITRÉS", "VEINTICUATRO", "VEINTICINCO", "VEINTISÉIS",
            "VEINTISIETE", "VEINTIOCHO", "VEINTINUEVE"};
    private static final String[] DECENAS = {
            "", "", "", "TREINTA", "CUARENTA", "CINCUENTA", "SESENTA", "SETENTA", "OCHENTA", "NOVENTA"};
    private static final String[] CENTENAS = {
            "", "CIENTO", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS", "QUINIENTOS", "SEISCIENTOS",
            "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS"};

    private NumeroALetras() {
    }

    /** Monto en soles con los céntimos como fracción: "… CON 05/100 SOLES". */
    public static String soles(long centimos) {
        if (centimos < 0) {
            throw new IllegalArgumentException("Monto negativo: " + centimos);
        }
        return numero(centimos / 100) + " CON " + String.format("%02d", centimos % 100) + "/100 SOLES";
    }

    /** Número entero en letras mayúsculas. */
    public static String numero(long n) {
        if (n < 0 || n > MAXIMO) {
            throw new IllegalArgumentException("Número fuera de rango: " + n);
        }
        if (n == 0) {
            return "CERO";
        }
        long millones = n / 1_000_000;
        long resto = n % 1_000_000;
        StringBuilder sb = new StringBuilder();
        if (millones > 0) {
            sb.append(millones == 1 ? "UN MILLÓN" : hastaMillon(millones, true) + " MILLONES");
        }
        if (resto > 0) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(hastaMillon(resto, false));
        }
        return sb.toString();
    }

    /** 1..999 999; con apócope, el "UNO" final se convierte en "UN"/"ÚN". */
    private static String hastaMillon(long n, boolean apocope) {
        int miles = (int) (n / 1000);
        int resto = (int) (n % 1000);
        StringBuilder sb = new StringBuilder();
        if (miles > 0) {
            sb.append(miles == 1 ? "MIL" : centenas(miles, true) + " MIL");
        }
        if (resto > 0) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(centenas(resto, apocope));
        }
        return sb.toString();
    }

    /** 1..999. */
    private static String centenas(int n, boolean apocope) {
        if (n == 100) {
            return "CIEN";
        }
        int c = n / 100;
        int resto = n % 100;
        String texto = CENTENAS[c];
        if (resto > 0) {
            texto = (texto.isEmpty() ? "" : texto + " ") + decenas(resto, apocope);
        }
        return texto;
    }

    /** 1..99. */
    private static String decenas(int n, boolean apocope) {
        String texto;
        if (n < 30) {
            texto = UNIDADES[n];
        } else {
            int u = n % 10;
            texto = DECENAS[n / 10] + (u == 0 ? "" : " Y " + UNIDADES[u]);
        }
        if (apocope) {
            if (texto.endsWith("VEINTIUNO")) {
                return texto.substring(0, texto.length() - "VEINTIUNO".length()) + "VEINTIÚN";
            }
            if (texto.endsWith("UNO")) {
                return texto.substring(0, texto.length() - 1);
            }
        }
        return texto;
    }
}
