package com.odontogest.servicio.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record AnamnesisDtoASEJ(
        long pacienteIdASEJ, String antecedentesMedicosASEJ, String alergiasASEJ, String medicacionActualASEJ,
        Map<String, Boolean> enfermedadesASEJ, String habitosASEJ, boolean fumaASEJ, boolean gestanteASEJ,
        String presionArterialASEJ, String observacionesASEJ, LocalDateTime actualizadoEnASEJ) {

    public AnamnesisDtoASEJ {
        enfermedadesASEJ = enfermedadesASEJ == null ? Map.of() : Map.copyOf(enfermedadesASEJ);
    }
}
