package com.odontogest.servicio.dto;

import com.odontogest.modelo.RolASEJ;

import java.time.LocalDateTime;

public record UsuarioDtoASEJ(long idASEJ, String usernameASEJ, String nombreCompletoASEJ, RolASEJ rolASEJ, Long odontologoIdASEJ,
                         String odontologoNombreASEJ, boolean activoASEJ, LocalDateTime ultimoAccesoASEJ, boolean bloqueadoASEJ) {
}
