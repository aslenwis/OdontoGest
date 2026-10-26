package com.odontogest.evento;

import java.util.Map;

public record NavegarAASEJ(String vistaASEJ, Map<String, Object> parametrosASEJ) {

    public NavegarAASEJ {
        parametrosASEJ = parametrosASEJ == null ? Map.of() : Map.copyOf(parametrosASEJ);
    }

    public NavegarAASEJ(String vistaASEJ) {
        this(vistaASEJ, Map.of());
    }
}
