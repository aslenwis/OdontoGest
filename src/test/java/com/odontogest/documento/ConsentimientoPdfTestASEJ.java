package com.odontogest.documento;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.odontogest.dominio.odontograma.SuperficieASEJ;
import com.odontogest.dominio.tratamiento.FormaCobroASEJ;
import com.odontogest.modelo.EstadoPlanASEJ;
import com.odontogest.modelo.EstadoPlanItemASEJ;
import com.odontogest.modelo.SexoASEJ;
import com.odontogest.modelo.TipoDocumentoASEJ;
import com.odontogest.servicio.dto.DatosClinicaASEJ;
import com.odontogest.servicio.dto.PacienteDetalleASEJ;
import com.odontogest.servicio.dto.PlanDtoASEJ;
import com.odontogest.servicio.dto.PlanItemDtoASEJ;
import com.odontogest.servicio.dto.PresupuestoDocumentoASEJ;
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

class ConsentimientoPdfTestASEJ {

    private static Set<String> marcadoresASEJ(String rutaASEJ) throws IOException {
        String plantillaASEJ;
        try (InputStream inASEJ = ConsentimientoPdfASEJ.class.getResourceAsStream(rutaASEJ)) {
            assertThat(inASEJ).isNotNull();
            plantillaASEJ = new String(inASEJ.readAllBytes(), StandardCharsets.UTF_8);
        }
        Map<String, String> valoresASEJ = new LinkedHashMap<>();
        Matcher mASEJ = Pattern.compile("\\{([A-Z_]+)}").matcher(plantillaASEJ);
        while (mASEJ.find()) {
            valoresASEJ.put(mASEJ.group(1), "valor");
        }
        assertThat(ConsentimientoPdfASEJ.completarASEJ(plantillaASEJ, valoresASEJ)).doesNotContain("{");
        return valoresASEJ.keySet();
    }

    @Test
    void todosLosMarcadoresDeLaPlantillaSeCompletanASEJ() throws IOException {
        assertThat(marcadoresASEJ(ConsentimientoPdfASEJ.PLANTILLA_DATOSASEJ)).containsExactlyInAnyOrder("FIRMANTE", "DOCUMENTO_FIRMANTE",
                "CALIDAD_FIRMANTE", "CLINICA", "RUC", "DIRECCION_CLINICA", "PACIENTE", "HISTORIA", "EMAIL_CLINICA", "FECHA");
        assertThat(marcadoresASEJ(ConsentimientoPdfASEJ.PLANTILLA_TRATAMIENTOASEJ)).containsExactlyInAnyOrder("FIRMANTE",
                "DOCUMENTO_FIRMANTE", "CALIDAD_FIRMANTE", "ODONTOLOGO", "COP", "CLINICA", "PACIENTE", "HISTORIA", "PLAN",
                "PROCEDIMIENTOS", "TOTAL", "TOTAL_LETRAS", "FECHA");
    }

    @Test
    void valoresVaciosQuedanComoLineaParaCompletarAManoASEJ() {
        assertThat(ConsentimientoPdfASEJ.completarASEJ("RUC {RUC}.", Map.of("RUC", " "))).isEqualTo("RUC ________________.");
    }

    @Test
    void consentimientoDeTratamientoEnUnaPaginaConLosProcedimientosASEJ() throws IOException {
        DatosClinicaASEJ clinicaASEJ = new DatosClinicaASEJ("Consultorio Odontológico Demo", "20000000001", "Av. Demostración 123, Lima",
                "(01) 000-0000", "contacto@consultorio-demo.pe");
        PacienteDetalleASEJ pacienteASEJ = new PacienteDetalleASEJ(1, "HC-000001", TipoDocumentoASEJ.DNI, "72004779", "Ana", "Prueba",
                "Demo", LocalDate.of(1986, 1, 1), SexoASEJ.F, null, null, null, null, null, null, null, null, null, false, true,
                40, null, null, false);
        List<PlanItemDtoASEJ> itemsASEJ = new ArrayList<>();
        for (int iASEJ = 0; iASEJ < 10; iASEJ++) {
            itemsASEJ.add(new PlanItemDtoASEJ(iASEJ, 7, 1, "RES-C", "Resina compuesta (2 o más superficies)", FormaCobroASEJ.PIEZA, null,
                    31 + iASEJ % 8, Set.of(SuperficieASEJ.O, SuperficieASEJ.M), 2, 1, new BigDecimal("180.00"), BigDecimal.ZERO,
                    new BigDecimal("180.00"), EstadoPlanItemASEJ.PENDIENTE, null, null));
        }
        PlanDtoASEJ planASEJ = new PlanDtoASEJ(7, 1, 1, "Carlos Alberto Quispe Mamani", "90101", null, LocalDateTime.now(),
                EstadoPlanASEJ.PROPUESTO, new BigDecimal("1800.00"), BigDecimal.ZERO, new BigDecimal("1800.00"), null, null,
                itemsASEJ, List.of(), null, null, Set.of(EstadoPlanASEJ.ACEPTADO));

        byte[] pdfASEJ = new ConsentimientoPdfASEJ(new PlantillaPdfASEJ())
                .tratamientoASEJ(new PresupuestoDocumentoASEJ(clinicaASEJ, pacienteASEJ, planASEJ), LocalDate.of(2026, 10, 20));
        PdfReader lectorASEJ = new PdfReader(pdfASEJ);
        String textoASEJ = new PdfTextExtractor(lectorASEJ).getTextFromPage(1);

        assertThat(lectorASEJ.getNumberOfPages()).as("firmas en la misma página").isEqualTo(1);
        assertThat(textoASEJ).contains("plan de tratamiento N° 7", "Resina compuesta (2 o más superficies) · pieza 36 OM",
                "S/ 1,800.00", "MIL OCHOCIENTOS CON 00/100 SOLES", "C.D. Carlos Alberto Quispe Mamani", "Huella digital");
    }
}
