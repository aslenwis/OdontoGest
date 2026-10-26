package com.odontogest.servicio.dto;

import com.odontogest.modelo.EstadoAtencionASEJ;

import java.time.LocalDateTime;

public record AtencionDetalleASEJ(long idASEJ, long pacienteIdASEJ, long odontologoIdASEJ, String odontologoNombreASEJ, LocalDateTime fechaASEJ,
                              String motivoConsultaASEJ, String examenClinicoASEJ, String diagnosticoASEJ, String observacionesASEJ,
                              EstadoAtencionASEJ estadoASEJ, LocalDateTime cerradaEnASEJ) {

    public boolean abiertaASEJ() {
        return estadoASEJ == EstadoAtencionASEJ.ABIERTA;
    }
}
