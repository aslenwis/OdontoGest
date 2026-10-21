/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import com.odontogest.dominio.odontograma.ColorHallazgo;
import com.odontogest.dominio.odontograma.EstadoHallazgo;
import com.odontogest.dominio.odontograma.Representacion;
import com.odontogest.dominio.odontograma.Superficie;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Hallazgo para el inspector del odontograma y los reportes.
 *
 * @param sigla      sigla visible: la elegida o la fija del tipo
 * @param vigente    ocupa hoy su lugar en el odontograma (no fue reemplazado ni anulado)
 * @param eliminable pertenece al borrador abierto: se puede eliminar en lugar de anular
 */
public record HallazgoDto(long id, long odontogramaId, long tipoId, String codigoTipo, String nombreTipo,
                          Representacion representacion, int pieza, Integer piezaFin, Set<Superficie> superficies,
                          EstadoHallazgo estado, String sigla, String nota, LocalDateTime registradoEn,
                          String registradoPor, ColorHallazgo color, boolean anulado, String motivoAnulacion,
                          LocalDateTime anuladoEn, boolean vigente, boolean eliminable) {

    public HallazgoDto {
        superficies = Set.copyOf(superficies);
    }
}
