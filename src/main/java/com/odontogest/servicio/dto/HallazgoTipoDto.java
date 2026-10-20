/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import com.odontogest.dominio.odontograma.ConteoIndice;
import com.odontogest.dominio.odontograma.ReglaColor;
import com.odontogest.dominio.odontograma.Representacion;
import com.odontogest.dominio.odontograma.SimboloHallazgo;

import java.util.List;

/**
 * Tipo del catálogo de hallazgos.
 *
 * @param enUso ya hay hallazgos registrados con este tipo: su estructura no se puede modificar
 */
public record HallazgoTipoDto(long id, String codigo, String nombre, String sigla, List<String> siglasOpciones,
                              Representacion representacion, SimboloHallazgo simbolo, ReglaColor reglaColor,
                              String grupoExclusion, boolean ocultaSuperficies, ConteoIndice conteoCpod,
                              ConteoIndice conteoCeod, boolean activo, int orden, boolean enUso) {

    public HallazgoTipoDto {
        siglasOpciones = List.copyOf(siglasOpciones);
    }

    @Override
    public String toString() {
        return nombre;
    }
}
