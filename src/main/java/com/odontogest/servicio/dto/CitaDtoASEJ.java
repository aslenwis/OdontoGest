package com.odontogest.servicio.dto;

import com.odontogest.modelo.EstadoCitaASEJ;

import java.time.Duration;
import java.time.LocalDateTime;

public record CitaDtoASEJ(long idASEJ, long pacienteIdASEJ, String pacienteNombreASEJ, String numeroHistoriaASEJ, String pacienteTelefonoASEJ,
                      long odontologoIdASEJ, String odontologoNombreASEJ, String colorAgendaASEJ, LocalDateTime inicioASEJ,
                      LocalDateTime finASEJ, String motivoASEJ, EstadoCitaASEJ estadoASEJ, Long planItemIdASEJ, String planItemDescripcionASEJ,
                      String notasASEJ, Long atencionIdASEJ) {

    public int duracionMinASEJ() {
        return (int) Duration.between(inicioASEJ, finASEJ).toMinutes();
    }
}
