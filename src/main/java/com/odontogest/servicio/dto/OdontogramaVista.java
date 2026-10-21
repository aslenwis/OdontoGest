/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import com.odontogest.dominio.odontograma.Denticion;
import com.odontogest.dominio.odontograma.EstadoOdontograma;
import com.odontogest.modelo.EstadoOdontogramaRegistro;
import com.odontogest.modelo.TipoOdontograma;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Lo que muestra la pestaña Odontograma: el estado proyectado de la boca en un momento, los metadatos del
 * odontograma correspondiente, el historial de hallazgos y los índices.
 *
 * @param odontogramaId    {@code null} si el paciente aún no tiene odontograma
 * @param atencionAbierta  la atención del odontograma sigue abierta
 * @param hasta            momento mostrado; {@code null} = estado actual (incluye el borrador)
 * @param estadoBoca       proyección de los hallazgos hasta {@code hasta}
 * @param historial        todos los hallazgos registrados hasta {@code hasta} (vigentes, reemplazados y anulados)
 */
public record OdontogramaVista(long pacienteId, Long odontogramaId, Long atencionId, TipoOdontograma tipo,
                               EstadoOdontogramaRegistro estado, Denticion denticion, String especificaciones,
                               String observaciones, LocalDateTime creadoEn, LocalDateTime cerradoEn, String hashCierre,
                               boolean atencionAbierta, LocalDateTime hasta, EstadoOdontograma estadoBoca,
                               List<HallazgoDto> historial, IndicesVista indices) {

    public OdontogramaVista {
        historial = List.copyOf(historial);
    }

    public boolean borrador() {
        return estado == EstadoOdontogramaRegistro.BORRADOR;
    }

    /** Se pueden registrar hallazgos (la UI además verifica el permiso ODONTOGRAMA_EDITAR). */
    public boolean editable() {
        return odontogramaId != null && borrador() && atencionAbierta && hasta == null;
    }
}
