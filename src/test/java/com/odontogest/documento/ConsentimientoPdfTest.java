/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.documento;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La plantilla del consentimiento solo usa marcadores que el generador completa.
 */
class ConsentimientoPdfTest {

    @Test
    void todosLosMarcadoresDeLaPlantillaSeCompletan() throws IOException {
        String plantilla;
        try (InputStream in = ConsentimientoPdf.class.getResourceAsStream(ConsentimientoPdf.PLANTILLA_DATOS)) {
            assertThat(in).isNotNull();
            plantilla = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Map<String, String> valores = new LinkedHashMap<>();
        Matcher m = Pattern.compile("\\{([A-Z_]+)}").matcher(plantilla);
        while (m.find()) {
            valores.put(m.group(1), "valor");
        }
        assertThat(valores.keySet()).containsExactlyInAnyOrder("FIRMANTE", "DOCUMENTO_FIRMANTE", "CALIDAD_FIRMANTE",
                "CLINICA", "RUC", "DIRECCION_CLINICA", "PACIENTE", "HISTORIA", "EMAIL_CLINICA", "FECHA");
        assertThat(ConsentimientoPdf.completar(plantilla, valores)).doesNotContain("{").contains("Ley N° 29733");
    }

    @Test
    void valoresVaciosQuedanComoLineaParaCompletarAMano() {
        assertThat(ConsentimientoPdf.completar("RUC {RUC}.", Map.of("RUC", " "))).isEqualTo("RUC ________________.");
    }
}
