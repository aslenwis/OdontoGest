/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.odontograma;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Datos de un odontograma cerrado que entran en su hash de integridad ({@link CadenaIntegridad}).
 * Las anulaciones posteriores no forman parte: se registran aparte y no alteran el hash.
 *
 * @param hashAnterior hash guardado del odontograma cerrado previo ({@code GENESIS} si es el primero)
 * @param hashCierre   hash guardado al cerrar; se compara contra el recalculado al verificar
 */
public record OdontogramaCanonico(
        long id, long pacienteId, String tipo, String denticion,
        LocalDateTime cerradoEn, long cerradoPor, String especificaciones,
        List<Linea> hallazgos, String hashAnterior, String hashCierre) {

    public OdontogramaCanonico {
        Objects.requireNonNull(cerradoEn, "cerradoEn");
        hallazgos = List.copyOf(hallazgos);
    }

    /**
     * Línea canónica de un hallazgo.
     *
     * @param superficies máscara de bits (0–31)
     */
    public record Linea(long id, String codigoTipo, int pieza, Integer piezaFin, int superficies,
                        String estado, String sigla, LocalDateTime registradoEn, long registradoPor) {

        public Linea {
            Objects.requireNonNull(registradoEn, "registradoEn");
        }
    }
}
