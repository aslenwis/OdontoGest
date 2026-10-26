package com.odontogest.documento;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.odontogest.dominio.comun.NumeroALetrasASEJ;
import com.odontogest.modelo.EstadoPlanItemASEJ;
import com.odontogest.servicio.dto.DatosClinicaASEJ;
import com.odontogest.servicio.dto.PacienteDetalleASEJ;
import com.odontogest.servicio.dto.PlanDtoASEJ;
import com.odontogest.servicio.dto.PlanItemDtoASEJ;
import com.odontogest.servicio.dto.PresupuestoDocumentoASEJ;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ConsentimientoPdfASEJ {

    static final String PLANTILLA_DATOSASEJ = "/plantillas/consentimiento-datos.txt";
    static final String PLANTILLA_TRATAMIENTOASEJ = "/plantillas/consentimiento-tratamiento.txt";
    private static final String MARCA_PROCEDIMIENTOSASEJ = "{PROCEDIMIENTOS}";
    private static final DateTimeFormatter FECHA_LARGAASEJ =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-PE"));

    private final PlantillaPdfASEJ plantillaASEJ;

    private record FirmanteASEJ(String nombreASEJ, String documentoASEJ, String calidadASEJ) {

        static FirmanteASEJ deASEJ(PacienteDetalleASEJ pASEJ, String calidadAdultoASEJ) {
            if (pASEJ.esMenorDeEdadASEJ()) {
                return new FirmanteASEJ(valorASEJ(pASEJ.apoderadoNombreASEJ()), valorASEJ(pASEJ.apoderadoDocumentoASEJ()),
                        "apoderado(a) (" + valorASEJ(pASEJ.apoderadoParentescoASEJ()).toLowerCase(Locale.ROOT)
                                + ") del paciente menor de edad");
            }
            return new FirmanteASEJ(pASEJ.nombreCompletoASEJ(), pASEJ.documentoASEJ(), calidadAdultoASEJ);
        }
    }

    public byte[] datosPersonalesASEJ(DatosClinicaASEJ clinicaASEJ, PacienteDetalleASEJ pacienteASEJ, LocalDate fechaASEJ) {
        FirmanteASEJ firmanteASEJ = FirmanteASEJ.deASEJ(pacienteASEJ, "titular de los datos personales");
        Map<String, String> valoresASEJ = new LinkedHashMap<>();
        valoresASEJ.put("CLINICA", clinicaASEJ.nombreASEJ());
        valoresASEJ.put("RUC", clinicaASEJ.rucASEJ());
        valoresASEJ.put("DIRECCION_CLINICA", clinicaASEJ.direccionASEJ());
        valoresASEJ.put("EMAIL_CLINICA", clinicaASEJ.emailASEJ());
        valoresASEJ.put("PACIENTE", pacienteASEJ.nombreCompletoASEJ());
        valoresASEJ.put("HISTORIA", pacienteASEJ.numeroHistoriaASEJ());
        valoresASEJ.put("FIRMANTE", firmanteASEJ.nombreASEJ());
        valoresASEJ.put("DOCUMENTO_FIRMANTE", firmanteASEJ.documentoASEJ());
        valoresASEJ.put("CALIDAD_FIRMANTE", firmanteASEJ.calidadASEJ());
        valoresASEJ.put("FECHA", FECHA_LARGAASEJ.format(fechaASEJ));
        String textoASEJ = completarASEJ(leerPlantillaASEJ(PLANTILLA_DATOSASEJ), valoresASEJ);

        return plantillaASEJ.generarASEJ(clinicaASEJ, "CONSENTIMIENTO PARA EL TRATAMIENTO DE DATOS PERSONALES", docASEJ -> {
            escribirASEJ(docASEJ, textoASEJ, List.of());
            PlantillaPdfASEJ.agregarASEJ(docASEJ, bloqueFirmaASEJ(firmanteASEJ.nombreASEJ(), firmanteASEJ.documentoASEJ()));
        });
    }

    public byte[] tratamientoASEJ(PresupuestoDocumentoASEJ dASEJ, LocalDate fechaASEJ) {
        PacienteDetalleASEJ pacienteASEJ = dASEJ.pacienteASEJ();
        PlanDtoASEJ planASEJ = dASEJ.planASEJ();
        FirmanteASEJ firmanteASEJ = FirmanteASEJ.deASEJ(pacienteASEJ, "paciente");
        Map<String, String> valoresASEJ = new LinkedHashMap<>();
        valoresASEJ.put("CLINICA", dASEJ.clinicaASEJ().nombreASEJ());
        valoresASEJ.put("ODONTOLOGO", planASEJ.odontologoNombreASEJ());
        valoresASEJ.put("COP", planASEJ.odontologoCopASEJ());
        valoresASEJ.put("PACIENTE", pacienteASEJ.nombreCompletoASEJ());
        valoresASEJ.put("HISTORIA", pacienteASEJ.numeroHistoriaASEJ());
        valoresASEJ.put("PLAN", String.valueOf(planASEJ.idASEJ()));
        valoresASEJ.put("TOTAL", PresupuestoPdfASEJ.solesASEJ(planASEJ.totalASEJ()));
        valoresASEJ.put("TOTAL_LETRAS", NumeroALetrasASEJ.solesASEJ(planASEJ.totalCentASEJ()));
        valoresASEJ.put("FIRMANTE", firmanteASEJ.nombreASEJ());
        valoresASEJ.put("DOCUMENTO_FIRMANTE", firmanteASEJ.documentoASEJ());
        valoresASEJ.put("CALIDAD_FIRMANTE", firmanteASEJ.calidadASEJ());
        valoresASEJ.put("FECHA", FECHA_LARGAASEJ.format(fechaASEJ));
        String textoASEJ = completarASEJ(leerPlantillaASEJ(PLANTILLA_TRATAMIENTOASEJ), valoresASEJ);
        List<String> procedimientosASEJ = planASEJ.itemsASEJ().stream()
                .filter(iASEJ -> iASEJ.estadoASEJ() != EstadoPlanItemASEJ.CANCELADO)
                .map(iASEJ -> PlanItemDtoASEJ.nombreFaseASEJ(iASEJ.faseASEJ()) + ": " + iASEJ.descripcionASEJ())
                .toList();

        return plantillaASEJ.generarASEJ(dASEJ.clinicaASEJ(), "CONSENTIMIENTO INFORMADO DE TRATAMIENTO ODONTOLÓGICO", docASEJ -> {
            escribirASEJ(docASEJ, textoASEJ, procedimientosASEJ);
            PlantillaPdfASEJ.agregarASEJ(docASEJ, firmasTratamientoASEJ(firmanteASEJ, planASEJ));
        });
    }

    private static void escribirASEJ(Document docASEJ, String textoASEJ, List<String> procedimientosASEJ) {
        for (String parrafoASEJ : textoASEJ.split("\\R\\s*\\R")) {
            String limpioASEJ = parrafoASEJ.strip();
            if (limpioASEJ.equals(MARCA_PROCEDIMIENTOSASEJ)) {
                for (String procASEJ : procedimientosASEJ) {
                    Paragraph itemASEJ = new Paragraph("•  " + procASEJ, PlantillaPdfASEJ.NORMALASEJ);
                    itemASEJ.setIndentationLeft(26);
                    itemASEJ.setFirstLineIndent(-12);
                    itemASEJ.setLeading(14);
                    PlantillaPdfASEJ.agregarASEJ(docASEJ, itemASEJ);
                }
                PlantillaPdfASEJ.agregarASEJ(docASEJ, new Paragraph(" ", PlantillaPdfASEJ.PEQUENAASEJ));
            } else {
                PlantillaPdfASEJ.agregarASEJ(docASEJ, PlantillaPdfASEJ.parrafoASEJ(limpioASEJ.replaceAll("\\s*\\R\\s*", " "), PlantillaPdfASEJ.NORMALASEJ));
            }
        }
    }

    private static PdfPTable firmasTratamientoASEJ(FirmanteASEJ firmanteASEJ, PlanDtoASEJ planASEJ) {
        PdfPTable tASEJ = new PdfPTable(new float[]{2.3f, 2.3f, 1.1f});
        tASEJ.setWidthPercentage(100);
        tASEJ.setSpacingBefore(28);
        tASEJ.addCell(firmaASEJ(firmanteASEJ.nombreASEJ(), firmanteASEJ.documentoASEJ()));
        tASEJ.addCell(firmaASEJ("C.D. " + planASEJ.odontologoNombreASEJ(), "COP " + planASEJ.odontologoCopASEJ()));
        PdfPCell huellaASEJ = new PdfPCell(new Paragraph("Huella digital", PlantillaPdfASEJ.PEQUENAASEJ));
        huellaASEJ.setFixedHeight(72);
        huellaASEJ.setHorizontalAlignment(Element.ALIGN_CENTER);
        huellaASEJ.setVerticalAlignment(Element.ALIGN_BOTTOM);
        tASEJ.addCell(huellaASEJ);
        return tASEJ;
    }

    private static PdfPCell firmaASEJ(String nombreASEJ, String detalleASEJ) {
        PdfPCell cASEJ = new PdfPCell();
        cASEJ.setBorder(Rectangle.NO_BORDER);
        cASEJ.setVerticalAlignment(Element.ALIGN_BOTTOM);
        cASEJ.setPaddingRight(14);
        cASEJ.addElement(new Paragraph("______________________________", PlantillaPdfASEJ.NORMALASEJ));
        cASEJ.addElement(new Paragraph(nombreASEJ, PlantillaPdfASEJ.NEGRITAASEJ));
        cASEJ.addElement(new Paragraph(detalleASEJ, PlantillaPdfASEJ.NORMALASEJ));
        return cASEJ;
    }

    private static PdfPTable bloqueFirmaASEJ(String firmanteASEJ, String documentoASEJ) {
        PdfPTable tablaASEJ = new PdfPTable(new float[]{3, 1});
        tablaASEJ.setWidthPercentage(100);
        tablaASEJ.setSpacingBefore(56);

        PdfPCell firmaASEJ = new PdfPCell();
        firmaASEJ.setBorder(Rectangle.NO_BORDER);
        firmaASEJ.setVerticalAlignment(Element.ALIGN_BOTTOM);
        firmaASEJ.addElement(new Paragraph("_______________________________________", PlantillaPdfASEJ.NORMALASEJ));
        firmaASEJ.addElement(new Paragraph(firmanteASEJ, PlantillaPdfASEJ.NEGRITAASEJ));
        firmaASEJ.addElement(new Paragraph(documentoASEJ, PlantillaPdfASEJ.NORMALASEJ));
        tablaASEJ.addCell(firmaASEJ);

        PdfPCell huellaASEJ = new PdfPCell(new Paragraph("Huella digital", PlantillaPdfASEJ.PEQUENAASEJ));
        huellaASEJ.setFixedHeight(80);
        huellaASEJ.setHorizontalAlignment(Element.ALIGN_CENTER);
        huellaASEJ.setVerticalAlignment(Element.ALIGN_BOTTOM);
        tablaASEJ.addCell(huellaASEJ);
        return tablaASEJ;
    }

    static String completarASEJ(String plantillaTextoASEJ, Map<String, String> valoresASEJ) {
        String rASEJ = plantillaTextoASEJ;
        for (Map.Entry<String, String> eASEJ : valoresASEJ.entrySet()) {
            rASEJ = rASEJ.replace("{" + eASEJ.getKey() + "}", valorASEJ(eASEJ.getValue()));
        }
        return rASEJ;
    }

    private static String valorASEJ(String vASEJ) {
        return vASEJ == null || vASEJ.isBlank() ? "________________" : vASEJ;
    }

    private static String leerPlantillaASEJ(String rutaASEJ) {
        try (InputStream inASEJ = ConsentimientoPdfASEJ.class.getResourceAsStream(rutaASEJ)) {
            if (inASEJ == null) {
                throw new IllegalStateException("No se encontró la plantilla " + rutaASEJ);
            }
            return new String(inASEJ.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException eASEJ) {
            throw new UncheckedIOException(eASEJ);
        }
    }
}
