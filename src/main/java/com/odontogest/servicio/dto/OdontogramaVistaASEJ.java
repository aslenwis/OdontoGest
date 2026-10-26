package com.odontogest.servicio.dto;

import com.odontogest.dominio.odontograma.DenticionASEJ;
import com.odontogest.dominio.odontograma.EstadoOdontogramaASEJ;
import com.odontogest.modelo.EstadoOdontogramaRegistroASEJ;
import com.odontogest.modelo.TipoOdontogramaASEJ;

import java.time.LocalDateTime;
import java.util.List;

public record OdontogramaVistaASEJ(long pacienteIdASEJ, Long odontogramaIdASEJ, Long atencionIdASEJ, TipoOdontogramaASEJ tipoASEJ,
                               EstadoOdontogramaRegistroASEJ estadoASEJ, DenticionASEJ denticionASEJ, String especificacionesASEJ,
                               String observacionesASEJ, LocalDateTime creadoEnASEJ, LocalDateTime cerradoEnASEJ, String hashCierreASEJ,
                               boolean atencionAbiertaASEJ, LocalDateTime hastaASEJ, EstadoOdontogramaASEJ estadoBocaASEJ,
                               List<HallazgoDtoASEJ> historialASEJ, IndicesVistaASEJ indicesASEJ) {

    public OdontogramaVistaASEJ {
        historialASEJ = List.copyOf(historialASEJ);
    }

    public boolean borradorASEJ() {
        return estadoASEJ == EstadoOdontogramaRegistroASEJ.BORRADOR;
    }

    public boolean editableASEJ() {
        return odontogramaIdASEJ != null && borradorASEJ() && atencionAbiertaASEJ && hastaASEJ == null;
    }
}
