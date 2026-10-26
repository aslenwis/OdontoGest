package com.odontogest.servicio.dto;

import com.odontogest.dominio.odontograma.DenticionASEJ;

public record AtencionFormASEJ(Long idASEJ, long pacienteIdASEJ, Long odontologoIdASEJ, String motivoConsultaASEJ, String examenClinicoASEJ,
                           String diagnosticoASEJ, String observacionesASEJ, DenticionASEJ denticionASEJ) {
}
