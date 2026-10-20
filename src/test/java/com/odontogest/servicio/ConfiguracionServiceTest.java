/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio;

import com.odontogest.PruebaIntegracion;
import com.odontogest.excepcion.NegocioException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Lectura tipada y guardado de la configuración.
 */
class ConfiguracionServiceTest extends PruebaIntegracion {

    @Autowired private ConfiguracionService configuracionService;

    @Test
    void leeValoresTipados() {
        assertThat(configuracionService.texto("clinica.nombre")).isEqualTo("Consultorio Odontológico Demo");
        assertThat(configuracionService.entero("agenda.intervalo_min")).isEqualTo(15);
        assertThat(configuracionService.booleano("email.smtp.starttls")).isTrue();
        assertThat(configuracionService.booleano("recordatorio.habilitado")).isFalse();
    }

    @Test
    void guardarInvalidaLaCache() {
        String original = configuracionService.texto("clinica.telefono");
        try {
            configuracionService.guardar(Map.of("clinica.telefono", "999 888 777"));
            assertThat(configuracionService.texto("clinica.telefono")).isEqualTo("999 888 777");
        } finally {
            configuracionService.guardar(Map.of("clinica.telefono", original));
        }
    }

    @Test
    void rechazaClavesInexistentesONoEditables() {
        assertThatThrownBy(() -> configuracionService.texto("no.existe")).isInstanceOf(NegocioException.class);
        assertThatThrownBy(() -> configuracionService.guardar(Map.of("esquema.version", "2")))
                .isInstanceOf(NegocioException.class).hasMessageContaining("no es editable");
        assertThat(configuracionService.entero("esquema.version")).isEqualTo(1);
    }
}
