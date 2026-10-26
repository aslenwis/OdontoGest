package com.odontogest.dominio.comun;

public final class NumeroALetrasASEJ {

    public static final long MAXIMOASEJ = 999_999_999_999L;

    private static final String[] UNIDADESASEJ = {
            "", "UNO", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE", "OCHO", "NUEVE",
            "DIEZ", "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE", "DIECISÉIS", "DIECISIETE", "DIECIOCHO", "DIECINUEVE",
            "VEINTE", "VEINTIUNO", "VEINTIDÓS", "VEINTITRÉS", "VEINTICUATRO", "VEINTICINCO", "VEINTISÉIS",
            "VEINTISIETE", "VEINTIOCHO", "VEINTINUEVE"};
    private static final String[] DECENASASEJ = {
            "", "", "", "TREINTA", "CUARENTA", "CINCUENTA", "SESENTA", "SETENTA", "OCHENTA", "NOVENTA"};
    private static final String[] CENTENASASEJ = {
            "", "CIENTO", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS", "QUINIENTOS", "SEISCIENTOS",
            "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS"};

    private NumeroALetrasASEJ() {
    }

    public static String solesASEJ(long centimosASEJ) {
        if (centimosASEJ < 0) {
            throw new IllegalArgumentException("Monto negativo: " + centimosASEJ);
        }
        return numeroASEJ(centimosASEJ / 100) + " CON " + String.format("%02d", centimosASEJ % 100) + "/100 SOLES";
    }

    public static String numeroASEJ(long nASEJ) {
        if (nASEJ < 0 || nASEJ > MAXIMOASEJ) {
            throw new IllegalArgumentException("Número fuera de rango: " + nASEJ);
        }
        if (nASEJ == 0) {
            return "CERO";
        }
        long millonesASEJ = nASEJ / 1_000_000;
        long restoASEJ = nASEJ % 1_000_000;
        StringBuilder sbASEJ = new StringBuilder();
        if (millonesASEJ > 0) {
            sbASEJ.append(millonesASEJ == 1 ? "UN MILLÓN" : hastaMillonASEJ(millonesASEJ, true) + " MILLONES");
        }
        if (restoASEJ > 0) {
            if (!sbASEJ.isEmpty()) {
                sbASEJ.append(' ');
            }
            sbASEJ.append(hastaMillonASEJ(restoASEJ, false));
        }
        return sbASEJ.toString();
    }

    private static String hastaMillonASEJ(long nASEJ, boolean apocopeASEJ) {
        int milesASEJ = (int) (nASEJ / 1000);
        int restoASEJ = (int) (nASEJ % 1000);
        StringBuilder sbASEJ = new StringBuilder();
        if (milesASEJ > 0) {
            sbASEJ.append(milesASEJ == 1 ? "MIL" : centenasASEJ(milesASEJ, true) + " MIL");
        }
        if (restoASEJ > 0) {
            if (!sbASEJ.isEmpty()) {
                sbASEJ.append(' ');
            }
            sbASEJ.append(centenasASEJ(restoASEJ, apocopeASEJ));
        }
        return sbASEJ.toString();
    }

    private static String centenasASEJ(int nASEJ, boolean apocopeASEJ) {
        if (nASEJ == 100) {
            return "CIEN";
        }
        int cASEJ = nASEJ / 100;
        int restoASEJ = nASEJ % 100;
        String textoASEJ = CENTENASASEJ[cASEJ];
        if (restoASEJ > 0) {
            textoASEJ = (textoASEJ.isEmpty() ? "" : textoASEJ + " ") + decenasASEJ(restoASEJ, apocopeASEJ);
        }
        return textoASEJ;
    }

    private static String decenasASEJ(int nASEJ, boolean apocopeASEJ) {
        String textoASEJ;
        if (nASEJ < 30) {
            textoASEJ = UNIDADESASEJ[nASEJ];
        } else {
            int uASEJ = nASEJ % 10;
            textoASEJ = DECENASASEJ[nASEJ / 10] + (uASEJ == 0 ? "" : " Y " + UNIDADESASEJ[uASEJ]);
        }
        if (apocopeASEJ) {
            if (textoASEJ.endsWith("VEINTIUNO")) {
                return textoASEJ.substring(0, textoASEJ.length() - "VEINTIUNO".length()) + "VEINTIÚN";
            }
            if (textoASEJ.endsWith("UNO")) {
                return textoASEJ.substring(0, textoASEJ.length() - 1);
            }
        }
        return textoASEJ;
    }
}
