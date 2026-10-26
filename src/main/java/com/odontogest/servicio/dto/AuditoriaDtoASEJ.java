package com.odontogest.servicio.dto;

import java.time.LocalDateTime;

public record AuditoriaDtoASEJ(long idASEJ, LocalDateTime fechaASEJ, String usuarioASEJ, String accionASEJ, String entidadASEJ, Long entidadIdASEJ,
                           String detalleASEJ) {
}
