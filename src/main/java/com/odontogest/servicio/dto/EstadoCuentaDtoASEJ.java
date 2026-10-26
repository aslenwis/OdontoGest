package com.odontogest.servicio.dto;

import java.math.BigDecimal;
import java.util.List;

public record EstadoCuentaDtoASEJ(long pacienteIdASEJ, BigDecimal totalASEJ, BigDecimal pagadoASEJ, BigDecimal saldoASEJ, BigDecimal vencidoASEJ,
                              List<CuotaPacienteDtoASEJ> cuotasASEJ, List<PagoDtoASEJ> pagosASEJ) {

    public EstadoCuentaDtoASEJ {
        cuotasASEJ = List.copyOf(cuotasASEJ);
        pagosASEJ = List.copyOf(pagosASEJ);
    }
}
