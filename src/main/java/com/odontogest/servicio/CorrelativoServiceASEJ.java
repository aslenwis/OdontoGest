package com.odontogest.servicio;

import com.odontogest.persistencia.dao.CorrelativoDaoASEJ;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CorrelativoServiceASEJ {

    public static final String SERIE_HISTORIAASEJ = "HC";
    public static final String SERIE_RECIBOASEJ = "RECIBO";

    private final CorrelativoDaoASEJ correlativoDaoASEJ;

    @Transactional(propagation = Propagation.MANDATORY)
    public long siguienteASEJ(String serieASEJ) {
        return correlativoDaoASEJ.siguienteASEJ(serieASEJ);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String siguienteHistoriaClinicaASEJ() {
        return String.format("HC-%06d", siguienteASEJ(SERIE_HISTORIAASEJ));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String siguienteReciboASEJ() {
        return String.format("R-%08d", siguienteASEJ(SERIE_RECIBOASEJ));
    }
}
