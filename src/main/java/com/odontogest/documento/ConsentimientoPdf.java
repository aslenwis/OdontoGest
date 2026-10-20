/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.documento;

import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.odontogest.servicio.dto.DatosClinica;
import com.odontogest.servicio.dto.PacienteDetalle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Consentimientos imprimibles. El texto legal vive en {@code plantillas/consentimiento-datos.txt} con
 * marcadores {@code {CLAVE}}: se puede revisar sin tocar el código.
 */
@Component
@RequiredArgsConstructor
public class ConsentimientoPdf {

    static final String PLANTILLA_DATOS = "/plantillas/consentimiento-datos.txt";
    private static final DateTimeFormatter FECHA_LARGA =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-PE"));

    private final PlantillaPdf plantilla;

    /** Consentimiento para el tratamiento de datos personales (Ley N° 29733). */
    public byte[] datosPersonales(DatosClinica clinica, PacienteDetalle paciente, LocalDate fecha) {
        boolean menor = paciente.esMenorDeEdad();
        String firmante = menor ? valor(paciente.apoderadoNombre()) : paciente.nombreCompleto();
        String documentoFirmante = menor ? valor(paciente.apoderadoDocumento()) : paciente.documento();
        String calidad = menor
                ? "apoderado(a) (" + valor(paciente.apoderadoParentesco()).toLowerCase(Locale.ROOT)
                  + ") del paciente menor de edad"
                : "titular de los datos personales";

        Map<String, String> valores = new LinkedHashMap<>();
        valores.put("CLINICA", clinica.nombre());
        valores.put("RUC", clinica.ruc());
        valores.put("DIRECCION_CLINICA", clinica.direccion());
        valores.put("EMAIL_CLINICA", clinica.email());
        valores.put("PACIENTE", paciente.nombreCompleto());
        valores.put("HISTORIA", paciente.numeroHistoria());
        valores.put("FIRMANTE", firmante);
        valores.put("DOCUMENTO_FIRMANTE", documentoFirmante);
        valores.put("CALIDAD_FIRMANTE", calidad);
        valores.put("FECHA", FECHA_LARGA.format(fecha));
        String texto = completar(leerPlantilla(PLANTILLA_DATOS), valores);

        return plantilla.generar(clinica, "CONSENTIMIENTO PARA EL TRATAMIENTO DE DATOS PERSONALES", doc -> {
            for (String parrafo : texto.split("\\R\\s*\\R")) {
                PlantillaPdf.agregar(doc, PlantillaPdf.parrafo(parrafo.strip().replaceAll("\\s*\\R\\s*", " "),
                        PlantillaPdf.NORMAL));
            }
            PlantillaPdf.agregar(doc, bloqueFirma(firmante, documentoFirmante));
        });
    }

    private static PdfPTable bloqueFirma(String firmante, String documento) {
        PdfPTable tabla = new PdfPTable(new float[]{3, 1});
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(56);

        PdfPCell firma = new PdfPCell();
        firma.setBorder(Rectangle.NO_BORDER);
        firma.setVerticalAlignment(Element.ALIGN_BOTTOM);
        firma.addElement(new Paragraph("_______________________________________", PlantillaPdf.NORMAL));
        firma.addElement(new Paragraph(firmante, PlantillaPdf.NEGRITA));
        firma.addElement(new Paragraph(documento, PlantillaPdf.NORMAL));
        tabla.addCell(firma);

        PdfPCell huella = new PdfPCell(new Paragraph("Huella digital", PlantillaPdf.PEQUENA));
        huella.setFixedHeight(80);
        huella.setHorizontalAlignment(Element.ALIGN_CENTER);
        huella.setVerticalAlignment(Element.ALIGN_BOTTOM);
        tabla.addCell(huella);
        return tabla;
    }

    static String completar(String plantillaTexto, Map<String, String> valores) {
        String r = plantillaTexto;
        for (Map.Entry<String, String> e : valores.entrySet()) {
            r = r.replace("{" + e.getKey() + "}", valor(e.getValue()));
        }
        return r;
    }

    private static String valor(String v) {
        return v == null || v.isBlank() ? "________________" : v;
    }

    private static String leerPlantilla(String ruta) {
        try (InputStream in = ConsentimientoPdf.class.getResourceAsStream(ruta)) {
            if (in == null) {
                throw new IllegalStateException("No se encontró la plantilla " + ruta);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
