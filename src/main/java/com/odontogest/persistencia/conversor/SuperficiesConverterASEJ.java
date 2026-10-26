package com.odontogest.persistencia.conversor;

import com.odontogest.dominio.odontograma.SuperficieASEJ;
import com.odontogest.dominio.odontograma.SuperficiesASEJ;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Set;

@Converter
public class SuperficiesConverterASEJ implements AttributeConverter<Set<SuperficieASEJ>, Integer> {

    @Override
    public Integer convertToDatabaseColumn(Set<SuperficieASEJ> superficiesASEJ) {
        return SuperficiesASEJ.aMascaraASEJ(superficiesASEJ);
    }

    @Override
    public Set<SuperficieASEJ> convertToEntityAttribute(Integer mascaraASEJ) {
        return SuperficiesASEJ.desdeMascaraASEJ(mascaraASEJ == null ? 0 : mascaraASEJ);
    }
}
