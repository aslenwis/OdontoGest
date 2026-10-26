package com.odontogest.servicio.dto;

import com.odontogest.modelo.RolASEJ;

public record UsuarioFormASEJ(Long idASEJ, String usernameASEJ, String nombreCompletoASEJ, RolASEJ rolASEJ, Long odontologoIdASEJ,
                          boolean activoASEJ, String passwordASEJ) {
}
