package com.odontogest.integracion.correo;

import com.odontogest.excepcion.IntegracionExceptionASEJ;

public interface CorreoGatewayASEJ {

    void enviarASEJ(ParametrosSmtpASEJ parametrosASEJ, String destinoASEJ, String asuntoASEJ, String cuerpoASEJ);
}
