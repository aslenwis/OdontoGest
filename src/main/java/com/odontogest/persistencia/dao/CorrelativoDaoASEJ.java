package com.odontogest.persistencia.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CorrelativoDaoASEJ {

    private final JdbcTemplate jdbcTemplateASEJ;

    public long siguienteASEJ(String serieASEJ) {
        int filasASEJ = jdbcTemplateASEJ.update("UPDATE correlativo SET ultimo = ultimo + 1 WHERE serie = ?", serieASEJ);
        if (filasASEJ != 1) {
            throw new IllegalStateException("No existe la serie de correlativo " + serieASEJ);
        }
        Long ultimoASEJ = jdbcTemplateASEJ.queryForObject("SELECT ultimo FROM correlativo WHERE serie = ?", Long.class, serieASEJ);
        if (ultimoASEJ == null) {
            throw new IllegalStateException("Correlativo nulo para la serie " + serieASEJ);
        }
        return ultimoASEJ;
    }
}
