/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio;

import com.odontogest.persistencia.dao.CorrelativoDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Números correlativos sin saltos: historia clínica {@code HC-000001} y recibo {@code R-00000001} (Fase 5).
 * <p>
 * Exige una transacción en curso: el número se consume en la misma transacción que crea el registro,
 * así una falla posterior no deja huecos.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CorrelativoService {

    public static final String SERIE_HISTORIA = "HC";
    public static final String SERIE_RECIBO = "RECIBO";

    private final CorrelativoDao correlativoDao;

    @Transactional(propagation = Propagation.MANDATORY)
    public long siguiente(String serie) {
        return correlativoDao.siguiente(serie);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String siguienteHistoriaClinica() {
        return String.format("HC-%06d", siguiente(SERIE_HISTORIA));
    }
}
