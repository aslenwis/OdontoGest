/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.controlador;

import com.odontogest.ui.soporte.Refrescable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Vista "Catálogos" (solo ADMIN): contiene las pestañas de procedimientos, hallazgos, reglas de sugerencia y
 * odontólogos; al mostrarse recarga todas.
 */
@Component
@RequiredArgsConstructor
public class CatalogosController implements Refrescable {

    private final ProcedimientosTabController procedimientos;
    private final HallazgosTabController hallazgos;
    private final ReglasTabController reglas;
    private final OdontologosTabController odontologos;

    @Override
    public void refrescar() {
        procedimientos.refrescar();
        hallazgos.refrescar();
        reglas.refrescar();
        odontologos.refrescar();
    }
}
