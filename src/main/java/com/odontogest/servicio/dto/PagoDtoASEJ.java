package com.odontogest.servicio.dto;

import com.odontogest.modelo.MedioPagoASEJ;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PagoDtoASEJ(long idASEJ, String numeroReciboASEJ, long pacienteIdASEJ, String pacienteNombreASEJ, String numeroHistoriaASEJ,
                      LocalDateTime fechaASEJ, BigDecimal montoASEJ, MedioPagoASEJ medioASEJ, String referenciaASEJ, String registradoPorASEJ,
                      boolean anuladoASEJ, String motivoAnulacionASEJ, LocalDateTime anuladoEnASEJ, List<AplicacionDtoASEJ> aplicacionesASEJ) {

    public PagoDtoASEJ {
        aplicacionesASEJ = List.copyOf(aplicacionesASEJ);
    }
}
