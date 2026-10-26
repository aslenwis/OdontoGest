package com.odontogest.servicio;

import com.odontogest.excepcion.NegocioExceptionASEJ;

import java.util.ArrayList;
import java.util.List;

public final class ValidadorASEJ {

    private final List<String> erroresASEJ = new ArrayList<>();

    public ValidadorASEJ siASEJ(boolean condicionDeErrorASEJ, String mensajeASEJ) {
        if (condicionDeErrorASEJ) {
            erroresASEJ.add(mensajeASEJ);
        }
        return this;
    }

    public ValidadorASEJ requeridoASEJ(String valorASEJ, String campoASEJ) {
        return siASEJ(valorASEJ == null || valorASEJ.isBlank(), "Ingrese " + campoASEJ + ".");
    }

    public ValidadorASEJ requeridoASEJ(Object valorASEJ, String campoASEJ) {
        return siASEJ(valorASEJ == null, "Seleccione " + campoASEJ + ".");
    }

    public ValidadorASEJ maximoASEJ(String valorASEJ, int largoASEJ, String campoASEJ) {
        return siASEJ(valorASEJ != null && valorASEJ.length() > largoASEJ, "El campo " + campoASEJ + " admite hasta " + largoASEJ + " caracteres.");
    }

    public boolean tieneErroresASEJ() {
        return !erroresASEJ.isEmpty();
    }

    public void lanzarSiHayErroresASEJ() {
        if (!erroresASEJ.isEmpty()) {
            throw new NegocioExceptionASEJ(String.join("\n", erroresASEJ));
        }
    }

    public static String limpioASEJ(String valorASEJ) {
        if (valorASEJ == null) {
            return null;
        }
        String tASEJ = valorASEJ.trim().replaceAll("\\s+", " ");
        return tASEJ.isEmpty() ? null : tASEJ;
    }

    public static String limpioMultilineaASEJ(String valorASEJ) {
        if (valorASEJ == null) {
            return null;
        }
        String tASEJ = valorASEJ.strip();
        return tASEJ.isEmpty() ? null : tASEJ;
    }
}
