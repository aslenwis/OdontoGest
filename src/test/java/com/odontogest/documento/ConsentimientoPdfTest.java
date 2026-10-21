/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.documento;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.odontogest.dominio.odontograma.Superficie;
import com.odontogest.dominio.tratamiento.FormaCobro;
import com.odontogest.modelo.EstadoPlan;
import com.odontogest.modelo.EstadoPlanItem;
import com.odontogest.modelo.Sexo;
import com.odontogest.modelo.TipoDocumento;
import com.odontogest.servicio.dto.DatosClinica;
import com.odontogest.servicio.dto.PacienteDetalle;
import com.odontogest.servicio.dto.PlanDto;
import com.odontogest.servicio.dto.PlanItemDto;
import com.odontogest.servicio.dto.PresupuestoDocumento;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Las plantillas de los consentimientos solo usan marcadores que el generador completa, y el consentimiento de
 * tratamiento cabe con sus firmas en una página.
 */
class ConsentimientoPdfTest {

    private static Set<String> marcadores(String ruta) throws IOException {
        String plantilla;
        try (InputStream in = ConsentimientoPdf.class.getResourceAsStream(ruta)) {
            assertThat(in).isNotNull();
            plantilla = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Map<String, String> valores = new LinkedHashMap<>();
        Matcher m = Pattern.compile("\\{([A-Z_]+)}").matcher(plantilla);
        while (m.find()) {
            valores.put(m.group(1), "valor");
        }
        assertThat(ConsentimientoPdf.completar(plantilla, valores)).doesNotContain("{");
        return valores.keySet();
    }

    @Test
    void todosLosMarcadoresDeLaPlantillaSeCompletan() throws IOException {
        assertThat(marcadores(ConsentimientoPdf.PLANTILLA_DATOS)).containsExactlyInAnyOrder("FIRMANTE", "DOCUMENTO_FIRMANTE",
                "CALIDAD_FIRMANTE", "CLINICA", "RUC", "DIRECCION_CLINICA", "PACIENTE", "HISTORIA", "EMAIL_CLINICA", "FECHA");
        assertThat(marcadores(ConsentimientoPdf.PLANTILLA_TRATAMIENTO)).containsExactlyInAnyOrder("FIRMANTE",
                "DOCUMENTO_FIRMANTE", "CALIDAD_FIRMANTE", "ODONTOLOGO", "COP", "CLINICA", "PACIENTE", "HISTORIA", "PLAN",
                "PROCEDIMIENTOS", "TOTAL", "TOTAL_LETRAS", "FECHA");
    }

    @Test
    void valoresVaciosQuedanComoLineaParaCompletarAMano() {
        assertThat(ConsentimientoPdf.completar("RUC {RUC}.", Map.of("RUC", " "))).isEqualTo("RUC ________________.");
    }

    @Test
    void consentimientoDeTratamientoEnUnaPaginaConLosProcedimientos() throws IOException {
        DatosClinica clinica = new DatosClinica("Consultorio Odontológico Demo", "20000000001", "Av. Demostración 123, Lima",
                "(01) 000-0000", "contacto@consultorio-demo.pe");
        PacienteDetalle paciente = new PacienteDetalle(1, "HC-000001", TipoDocumento.DNI, "72004779", "Ana", "Prueba",
                "Demo", LocalDate.of(1986, 1, 1), Sexo.F, null, null, null, null, null, null, null, null, null, false, true,
                40, null, null, false);
        List<PlanItemDto> items = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            items.add(new PlanItemDto(i, 7, 1, "RES-C", "Resina compuesta (2 o más superficies)", FormaCobro.PIEZA, null,
                    31 + i % 8, Set.of(Superficie.O, Superficie.M), 2, 1, new BigDecimal("180.00"), BigDecimal.ZERO,
                    new BigDecimal("180.00"), EstadoPlanItem.PENDIENTE, null, null));
        }
        PlanDto plan = new PlanDto(7, 1, 1, "Carlos Alberto Quispe Mamani", "90101", null, LocalDateTime.now(),
                EstadoPlan.PROPUESTO, new BigDecimal("1800.00"), BigDecimal.ZERO, new BigDecimal("1800.00"), null, null,
                items, List.of(), null, null, Set.of(EstadoPlan.ACEPTADO));

        byte[] pdf = new ConsentimientoPdf(new PlantillaPdf())
                .tratamiento(new PresupuestoDocumento(clinica, paciente, plan), LocalDate.of(2026, 10, 20));
        PdfReader lector = new PdfReader(pdf);
        String texto = new PdfTextExtractor(lector).getTextFromPage(1);

        assertThat(lector.getNumberOfPages()).as("firmas en la misma página").isEqualTo(1);
        assertThat(texto).contains("plan de tratamiento N° 7", "Resina compuesta (2 o más superficies) · pieza 36 OM",
                "S/ 1,800.00", "MIL OCHOCIENTOS CON 00/100 SOLES", "C.D. Carlos Alberto Quispe Mamani", "Huella digital");
    }
}
