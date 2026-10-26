package com.odontogest.dominio.comun;

import java.time.LocalDate;
import java.time.Period;
import java.util.regex.Pattern;

public final class ValidacionesASEJ {

    public static final int MAYORIA_DE_EDADASEJ = 18;

    private static final Pattern DNIASEJ = Pattern.compile("\\d{8}");
    private static final Pattern CEASEJ = Pattern.compile("[A-Za-z0-9]{9,12}");
    private static final Pattern PASAPORTEASEJ = Pattern.compile("[A-Za-z0-9]{6,12}");
    private static final Pattern EMAILASEJ = Pattern.compile("^[\\w.%+-]+@[\\w-]+(\\.[\\w-]+)*\\.[A-Za-z]{2,}$");
    private static final Pattern TELEFONOASEJ = Pattern.compile("^(\\+?51)?(9\\d{8}|0?\\d{1,2}\\d{6,7})$");

    private ValidacionesASEJ() {
    }

    public static boolean esDniASEJ(String valorASEJ) {
        return valorASEJ != null && DNIASEJ.matcher(valorASEJ).matches();
    }

    public static boolean esCarneExtranjeriaASEJ(String valorASEJ) {
        return valorASEJ != null && CEASEJ.matcher(valorASEJ).matches();
    }

    public static boolean esPasaporteASEJ(String valorASEJ) {
        return valorASEJ != null && PASAPORTEASEJ.matcher(valorASEJ).matches();
    }

    public static boolean esEmailASEJ(String valorASEJ) {
        return valorASEJ != null && valorASEJ.length() <= 254 && EMAILASEJ.matcher(valorASEJ.trim()).matches();
    }

    public static boolean esTelefonoASEJ(String valorASEJ) {
        if (valorASEJ == null) {
            return false;
        }
        String limpioASEJ = valorASEJ.replaceAll("[\\s()-]", "");
        return TELEFONOASEJ.matcher(limpioASEJ).matches();
    }

    public static int edadASEJ(LocalDate fechaNacimientoASEJ, LocalDate hoyASEJ) {
        if (fechaNacimientoASEJ.isAfter(hoyASEJ)) {
            throw new IllegalArgumentException("La fecha de nacimiento es posterior a hoy");
        }
        return Period.between(fechaNacimientoASEJ, hoyASEJ).getYears();
    }

    public static boolean esMenorDeEdadASEJ(LocalDate fechaNacimientoASEJ, LocalDate hoyASEJ) {
        return edadASEJ(fechaNacimientoASEJ, hoyASEJ) < MAYORIA_DE_EDADASEJ;
    }
}
