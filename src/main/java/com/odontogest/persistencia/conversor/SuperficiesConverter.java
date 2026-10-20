/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.persistencia.conversor;

import com.odontogest.dominio.odontograma.Superficie;
import com.odontogest.dominio.odontograma.Superficies;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Set;

/**
 * Guarda un conjunto de {@link Superficie} como máscara de bits {@code INTEGER} (0–31).
 * Se declara con {@code @Convert} explícito en cada atributo.
 */
@Converter
public class SuperficiesConverter implements AttributeConverter<Set<Superficie>, Integer> {

    @Override
    public Integer convertToDatabaseColumn(Set<Superficie> superficies) {
        return Superficies.aMascara(superficies);
    }

    @Override
    public Set<Superficie> convertToEntityAttribute(Integer mascara) {
        return Superficies.desdeMascara(mascara == null ? 0 : mascara);
    }
}
