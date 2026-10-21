/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.odontograma;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Grupos de edad y promedios de CPO-D / ceo-d del reporte epidemiológico (RF-REP-05).
 */
class PromedioIndicesTest {

    private static ResultadoIndice indice(int c, int p, int o) {
        int total = c + p + o;
        return new ResultadoIndice(c, p, o, total, 28, NivelSeveridadOms.de(total));
    }

    @ParameterizedTest
    @CsvSource({"0,INFANCIA", "5,INFANCIA", "6,ESCOLAR", "11,ESCOLAR", "12,ADOLESCENCIA", "17,ADOLESCENCIA",
            "18,JOVEN", "29,JOVEN", "30,ADULTO", "44,ADULTO", "45,ADULTO_MAYOR_JOVEN", "59,ADULTO_MAYOR_JOVEN",
            "60,ADULTO_MAYOR", "104,ADULTO_MAYOR"})
    void grupoPorEdad(int edad, GrupoEtario esperado) {
        assertThat(GrupoEtario.de(edad)).isEqualTo(esperado);
    }

    @Test
    void edadNegativa_esError() {
        assertThatThrownBy(() -> GrupoEtario.de(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void indicesRepresentativosPorGrupo() {
        assertThat(GrupoEtario.INFANCIA.aplicaCpod).isFalse();
        assertThat(GrupoEtario.INFANCIA.aplicaCeod).isTrue();
        assertThat(GrupoEtario.ESCOLAR.aplicaCpod).as("dentición mixta: ambos").isTrue();
        assertThat(GrupoEtario.ESCOLAR.aplicaCeod).isTrue();
        assertThat(GrupoEtario.ADULTO.aplicaCeod).isFalse();
    }

    @Test
    void promediosPorGrupo_redondeadosADosDecimales() {
        PromedioIndices p = new PromedioIndices();
        p.agregar(35, indice(2, 1, 3), indice(0, 0, 0));    // CPO-D 6
        p.agregar(40, indice(1, 0, 0), indice(0, 0, 0));    // CPO-D 1
        p.agregar(42, indice(0, 0, 0), indice(0, 0, 0));    // CPO-D 0
        p.agregar(8, indice(1, 0, 0), indice(3, 1, 0));     // ceo-d 4
        assertThat(p.pacientes()).isEqualTo(4);

        List<PromedioIndices.Grupo> grupos = p.resultado();
        assertThat(grupos).hasSize(GrupoEtario.values().length);
        PromedioIndices.Grupo adultos = grupos.get(GrupoEtario.ADULTO.ordinal());
        assertThat(adultos.pacientes()).isEqualTo(3);
        assertThat(adultos.cpod()).isEqualTo(2.33);
        assertThat(adultos.c()).isEqualTo(1.0);
        assertThat(adultos.p()).isEqualTo(0.33);
        assertThat(adultos.o()).isEqualTo(1.0);
        assertThat(adultos.nivelCpod()).isEqualTo(NivelSeveridadOms.BAJO);

        PromedioIndices.Grupo escolares = grupos.get(GrupoEtario.ESCOLAR.ordinal());
        assertThat(escolares.ceod()).isEqualTo(4.0);
        assertThat(escolares.cpod()).isEqualTo(1.0);

        PromedioIndices.Grupo vacio = grupos.get(GrupoEtario.JOVEN.ordinal());
        assertThat(vacio.pacientes()).isZero();
        assertThat(vacio.nivelCpod()).isNull();
    }

    @Test
    void sinIndices_esError() {
        PromedioIndices p = new PromedioIndices();
        assertThatThrownBy(() -> p.agregar(30, null, indice(0, 0, 0))).isInstanceOf(NullPointerException.class);
    }
}
