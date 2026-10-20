/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.persistencia.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Correlativos sin saltos (tabla {@code correlativo}): historias clínicas y recibos.
 * <p>
 * {@code UPDATE ... SET ultimo = ultimo + 1} seguido de {@code SELECT} en la transacción del llamador;
 * con un pool de una sola conexión no hay carrera posible.
 */
@Repository
@RequiredArgsConstructor
public class CorrelativoDao {

    private final JdbcTemplate jdbcTemplate;

    public long siguiente(String serie) {
        int filas = jdbcTemplate.update("UPDATE correlativo SET ultimo = ultimo + 1 WHERE serie = ?", serie);
        if (filas != 1) {
            throw new IllegalStateException("No existe la serie de correlativo " + serie);
        }
        Long ultimo = jdbcTemplate.queryForObject("SELECT ultimo FROM correlativo WHERE serie = ?", Long.class, serie);
        if (ultimo == null) {
            throw new IllegalStateException("Correlativo nulo para la serie " + serie);
        }
        return ultimo;
    }
}
