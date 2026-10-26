package com.odontogest.servicio.dto;

import com.odontogest.modelo.TipoConsentimientoASEJ;

import java.time.LocalDateTime;

public record ConsentimientoDtoASEJ(long idASEJ, TipoConsentimientoASEJ tipoASEJ, LocalDateTime aceptadoEnASEJ, String firmadoPorASEJ) {
}
