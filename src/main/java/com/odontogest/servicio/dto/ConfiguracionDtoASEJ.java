package com.odontogest.servicio.dto;

import com.odontogest.modelo.TipoConfiguracionASEJ;

public record ConfiguracionDtoASEJ(String claveASEJ, String valorASEJ, TipoConfiguracionASEJ tipoASEJ, String grupoASEJ, String descripcionASEJ,
                               boolean secretoConfiguradoASEJ) {
}
