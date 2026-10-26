package com.odontogest.servicio.dto;

import java.time.LocalDateTime;

public record CitaFormASEJ(Long idASEJ, Long pacienteIdASEJ, Long odontologoIdASEJ, LocalDateTime inicioASEJ, int duracionMinASEJ, String motivoASEJ,
                       String notasASEJ, Long planItemIdASEJ) {
}
