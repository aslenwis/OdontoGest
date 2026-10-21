/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio;

import com.odontogest.PruebaIntegracion;
import com.odontogest.servicio.dto.InformacionSistemaDto;
import com.odontogest.servicio.dto.IntegridadBaseDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * "Acerca de": identificación del software y verificación de integridad de la base.
 */
class SistemaServiceTest extends PruebaIntegracion {

    @Autowired private SistemaService sistemaService;

    @Test
    void informacionParaAcercaDe() {
        InformacionSistemaDto info = sistemaService.informacion();
        assertThat(info.nombre()).isEqualTo("OdontoGest");
        assertThat(info.autores()).containsExactly("Apaza Mamani Ruben", "Apaza Suca Elwin junior",
                "Inofuente Trujillo David Alex");
        assertThat(info.universidad()).isEqualTo("Universidad Nacional de Juliaca");
        assertThat(info.aviso()).contains("Derecho de Autor");
        assertThat(info.version()).isNotBlank();
        assertThat(Path.of(info.carpetaDatos())).isEqualTo(HOME_PRUEBAS.toAbsolutePath());
        assertThat(Path.of(info.baseDatos())).exists();
        assertThat(Path.of(info.carpetaLogs())).isEqualTo(HOME_PRUEBAS.toAbsolutePath().resolve("logs"));
    }

    @Test
    void laBaseDePruebasEstaIntegra() {
        IntegridadBaseDto r = sistemaService.verificarIntegridad();
        assertThat(r.problemas()).isEmpty();
        assertThat(r.clavesForaneas()).isZero();
        assertThat(r.correcta()).isTrue();
        assertThat(r.tamanioBytes()).isPositive();
    }
}
