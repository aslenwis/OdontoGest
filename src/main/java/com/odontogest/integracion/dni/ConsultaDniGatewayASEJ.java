package com.odontogest.integracion.dni;

import com.odontogest.excepcion.IntegracionExceptionASEJ;

import java.util.Optional;

public interface ConsultaDniGatewayASEJ {

    Optional<DatosPersonaASEJ> consultarASEJ(String dniASEJ, ParametrosDniASEJ parametrosASEJ);
}
