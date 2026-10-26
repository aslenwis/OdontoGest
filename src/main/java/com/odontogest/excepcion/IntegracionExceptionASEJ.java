package com.odontogest.excepcion;

public class IntegracionExceptionASEJ extends RuntimeException {

    public IntegracionExceptionASEJ(String mensajeASEJ) {
        super(mensajeASEJ);
    }

    public IntegracionExceptionASEJ(String mensajeASEJ, Throwable causaASEJ) {
        super(mensajeASEJ, causaASEJ);
    }
}
