/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.documento;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.odontogest.dominio.comun.NumeroALetras;
import com.odontogest.modelo.EstadoPlanItem;
import com.odontogest.servicio.dto.DatosClinica;
import com.odontogest.servicio.dto.PacienteDetalle;
import com.odontogest.servicio.dto.PlanDto;
import com.odontogest.servicio.dto.PlanItemDto;
import com.odontogest.servicio.dto.PresupuestoDocumento;
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

/**
 * Consentimientos imprimibles: de datos personales (Ley N° 29733) e informado de tratamiento. El texto legal vive en
 * {@code plantillas/consentimiento-datos.txt} y {@code plantillas/consentimiento-tratamiento.txt} con marcadores
 * {@code {CLAVE}}: se puede revisar sin tocar el código. Un párrafo que solo contiene {@code {PROCEDIMIENTOS}} se
 * reemplaza por la lista de procedimientos del plan.
 */
@Component
@RequiredArgsConstructor
public class ConsentimientoPdf {

    static final String PLANTILLA_DATOS = "/plantillas/consentimiento-datos.txt";
    static final String PLANTILLA_TRATAMIENTO = "/plantillas/consentimiento-tratamiento.txt";
    private static final String MARCA_PROCEDIMIENTOS = "{PROCEDIMIENTOS}";
    private static final DateTimeFormatter FECHA_LARGA =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-PE"));

    private final PlantillaPdf plantilla;

    /** Paciente adulto o, si es menor, su apoderado. */
    private record Firmante(String nombre, String documento, String calidad) {

        static Firmante de(PacienteDetalle p, String calidadAdulto) {
            if (p.esMenorDeEdad()) {
                return new Firmante(valor(p.apoderadoNombre()), valor(p.apoderadoDocumento()),
                        "apoderado(a) (" + valor(p.apoderadoParentesco()).toLowerCase(Locale.ROOT)
                                + ") del paciente menor de edad");
            }
            return new Firmante(p.nombreCompleto(), p.documento(), calidadAdulto);
        }
    }

    /** Consentimiento para el tratamiento de datos personales (Ley N° 29733). */
    public byte[] datosPersonales(DatosClinica clinica, PacienteDetalle paciente, LocalDate fecha) {
        Firmante firmante = Firmante.de(paciente, "titular de los datos personales");
        Map<String, String> valores = new LinkedHashMap<>();
        valores.put("CLINICA", clinica.nombre());
        valores.put("RUC", clinica.ruc());
        valores.put("DIRECCION_CLINICA", clinica.direccion());
        valores.put("EMAIL_CLINICA", clinica.email());
        valores.put("PACIENTE", paciente.nombreCompleto());
        valores.put("HISTORIA", paciente.numeroHistoria());
        valores.put("FIRMANTE", firmante.nombre());
        valores.put("DOCUMENTO_FIRMANTE", firmante.documento());
        valores.put("CALIDAD_FIRMANTE", firmante.calidad());
        valores.put("FECHA", FECHA_LARGA.format(fecha));
        String texto = completar(leerPlantilla(PLANTILLA_DATOS), valores);

        return plantilla.generar(clinica, "CONSENTIMIENTO PARA EL TRATAMIENTO DE DATOS PERSONALES", doc -> {
            escribir(doc, texto, List.of());
            PlantillaPdf.agregar(doc, bloqueFirma(firmante.nombre(), firmante.documento()));
        });
    }

    /** Consentimiento informado para el plan de tratamiento (RF-PAC-05). */
    public byte[] tratamiento(PresupuestoDocumento d, LocalDate fecha) {
        PacienteDetalle paciente = d.paciente();
        PlanDto plan = d.plan();
        Firmante firmante = Firmante.de(paciente, "paciente");
        Map<String, String> valores = new LinkedHashMap<>();
        valores.put("CLINICA", d.clinica().nombre());
        valores.put("ODONTOLOGO", plan.odontologoNombre());
        valores.put("COP", plan.odontologoCop());
        valores.put("PACIENTE", paciente.nombreCompleto());
        valores.put("HISTORIA", paciente.numeroHistoria());
        valores.put("PLAN", String.valueOf(plan.id()));
        valores.put("TOTAL", PresupuestoPdf.soles(plan.total()));
        valores.put("TOTAL_LETRAS", NumeroALetras.soles(plan.totalCent()));
        valores.put("FIRMANTE", firmante.nombre());
        valores.put("DOCUMENTO_FIRMANTE", firmante.documento());
        valores.put("CALIDAD_FIRMANTE", firmante.calidad());
        valores.put("FECHA", FECHA_LARGA.format(fecha));
        String texto = completar(leerPlantilla(PLANTILLA_TRATAMIENTO), valores);
        List<String> procedimientos = plan.items().stream()
                .filter(i -> i.estado() != EstadoPlanItem.CANCELADO)
                .map(i -> PlanItemDto.nombreFase(i.fase()) + ": " + i.descripcion())
                .toList();

        return plantilla.generar(d.clinica(), "CONSENTIMIENTO INFORMADO DE TRATAMIENTO ODONTOLÓGICO", doc -> {
            escribir(doc, texto, procedimientos);
            PlantillaPdf.agregar(doc, firmasTratamiento(firmante, plan));
        });
    }

    /** Párrafos separados por líneas en blanco; el marcador de procedimientos se vuelve una lista con viñetas. */
    private static void escribir(Document doc, String texto, List<String> procedimientos) {
        for (String parrafo : texto.split("\\R\\s*\\R")) {
            String limpio = parrafo.strip();
            if (limpio.equals(MARCA_PROCEDIMIENTOS)) {
                // Párrafos con viñeta en lugar de com.lowagie.text.List: con List, OpenPDF manda a la página
                // siguiente la tabla de firmas aunque quepa.
                for (String proc : procedimientos) {
                    Paragraph item = new Paragraph("•  " + proc, PlantillaPdf.NORMAL);
                    item.setIndentationLeft(26);
                    item.setFirstLineIndent(-12);
                    item.setLeading(14);
                    PlantillaPdf.agregar(doc, item);
                }
                PlantillaPdf.agregar(doc, new Paragraph(" ", PlantillaPdf.PEQUENA));
            } else {
                PlantillaPdf.agregar(doc, PlantillaPdf.parrafo(limpio.replaceAll("\\s*\\R\\s*", " "), PlantillaPdf.NORMAL));
            }
        }
    }

    /** Firma del paciente (o apoderado), firma del odontólogo y huella en una sola fila. */
    private static PdfPTable firmasTratamiento(Firmante firmante, PlanDto plan) {
        PdfPTable t = new PdfPTable(new float[]{2.3f, 2.3f, 1.1f});
        t.setWidthPercentage(100);
        t.setSpacingBefore(28);
        t.addCell(firma(firmante.nombre(), firmante.documento()));
        t.addCell(firma("C.D. " + plan.odontologoNombre(), "COP " + plan.odontologoCop()));
        PdfPCell huella = new PdfPCell(new Paragraph("Huella digital", PlantillaPdf.PEQUENA));
        huella.setFixedHeight(72);
        huella.setHorizontalAlignment(Element.ALIGN_CENTER);
        huella.setVerticalAlignment(Element.ALIGN_BOTTOM);
        t.addCell(huella);
        return t;
    }

    private static PdfPCell firma(String nombre, String detalle) {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setVerticalAlignment(Element.ALIGN_BOTTOM);
        c.setPaddingRight(14);
        c.addElement(new Paragraph("______________________________", PlantillaPdf.NORMAL));
        c.addElement(new Paragraph(nombre, PlantillaPdf.NEGRITA));
        c.addElement(new Paragraph(detalle, PlantillaPdf.NORMAL));
        return c;
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
