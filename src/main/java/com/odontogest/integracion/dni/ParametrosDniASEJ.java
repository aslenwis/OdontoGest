package com.odontogest.integracion.dni;

import java.util.Map;

public record ParametrosDniASEJ(String urlPlantillaASEJ, String cabeceraAuthASEJ, String prefijoTokenASEJ, String tokenASEJ,
                            Map<String, String> mapeoASEJ, int timeoutSegASEJ) {

    public ParametrosDniASEJ {
        mapeoASEJ = Map.copyOf(mapeoASEJ);
    }
}
