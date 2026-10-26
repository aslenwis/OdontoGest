package com.odontogest.servicio;

import com.odontogest.PruebaIntegracionASEJ;
import com.odontogest.servicio.dto.InformacionSistemaDtoASEJ;
import com.odontogest.servicio.dto.IntegridadBaseDtoASEJ;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SistemaServiceTestASEJ extends PruebaIntegracionASEJ {

    @Autowired private SistemaServiceASEJ sistemaServiceASEJ;

    @Test
    void informacionParaAcercaDeASEJ() {
        InformacionSistemaDtoASEJ infoASEJ = sistemaServiceASEJ.informacionASEJ();
        assertThat(infoASEJ.nombreASEJ()).isEqualTo("OdontoGest");
        assertThat(infoASEJ.autoresASEJ()).containsExactly("Apaza Mamani Ruben", "Apaza Suca Elwin junior",
                "Inofuente Trujillo David Alex");
        assertThat(infoASEJ.universidadASEJ()).isEqualTo("Universidad Nacional de Juliaca");
        assertThat(infoASEJ.avisoASEJ()).contains("Derecho de Autor");
        assertThat(infoASEJ.versionASEJ()).isNotBlank();
        assertThat(Path.of(infoASEJ.carpetaDatosASEJ())).isEqualTo(HOME_PRUEBASASEJ.toAbsolutePath());
        assertThat(Path.of(infoASEJ.baseDatosASEJ())).exists();
        assertThat(Path.of(infoASEJ.carpetaLogsASEJ())).isEqualTo(HOME_PRUEBASASEJ.toAbsolutePath().resolve("logs"));
    }

    @Test
    void laBaseDePruebasEstaIntegraASEJ() {
        IntegridadBaseDtoASEJ rASEJ = sistemaServiceASEJ.verificarIntegridadASEJ();
        assertThat(rASEJ.problemasASEJ()).isEmpty();
        assertThat(rASEJ.clavesForaneasASEJ()).isZero();
        assertThat(rASEJ.correctaASEJ()).isTrue();
        assertThat(rASEJ.tamanioBytesASEJ()).isPositive();
    }
}
