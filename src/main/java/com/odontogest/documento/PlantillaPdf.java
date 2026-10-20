/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.documento;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.odontogest.servicio.dto.DatosClinica;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Plantilla común de los PDF de OdontoGest (OpenPDF): hoja A4, cabecera con los datos de la clínica,
 * título y pie con número de página.
 */
@Component
public class PlantillaPdf {

    public static final Font TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
    public static final Font CLINICA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
    public static final Font NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 10);
    public static final Font NEGRITA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    public static final Font PEQUENA = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY);

    private static final DateTimeFormatter GENERADO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Crea el documento, escribe la cabecera y el título, y delega el cuerpo a {@code contenido}.
     *
     * @return bytes del PDF
     */
    public byte[] generar(DatosClinica clinica, String titulo, Consumer<Document> contenido) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4, 56, 56, 56, 56);
        try {
            PdfWriter writer = PdfWriter.getInstance(documento, salida);
            writer.setPageEvent(new PiePagina(LocalDateTime.now()));
            documento.addTitle(titulo);
            documento.addCreator("OdontoGest");
            documento.open();
            cabecera(documento, clinica);
            Paragraph t = new Paragraph(titulo, TITULO);
            t.setAlignment(Element.ALIGN_CENTER);
            t.setSpacingBefore(14);
            t.setSpacingAfter(14);
            documento.add(t);
            contenido.accept(documento);
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo generar el PDF: " + e.getMessage(), e);
        } finally {
            if (documento.isOpen()) {
                documento.close();
            }
        }
        return salida.toByteArray();
    }

    /** Agrega un elemento convirtiendo la excepción comprobada de OpenPDF. */
    public static void agregar(Document documento, Element elemento) {
        try {
            documento.add(elemento);
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo escribir el PDF: " + e.getMessage(), e);
        }
    }

    /** Párrafo justificado con el espaciado estándar. */
    public static Paragraph parrafo(String texto, Font fuente) {
        Paragraph p = new Paragraph(texto, fuente);
        p.setAlignment(Element.ALIGN_JUSTIFIED);
        p.setSpacingAfter(7);
        p.setLeading(14);
        return p;
    }

    private static void cabecera(Document documento, DatosClinica clinica) throws DocumentException {
        documento.add(new Paragraph(clinica.nombre(), CLINICA));
        List<String> datos = new ArrayList<>();
        if (!clinica.ruc().isBlank()) {
            datos.add("RUC " + clinica.ruc());
        }
        for (String d : List.of(clinica.direccion(), clinica.telefono(), clinica.email())) {
            if (!d.isBlank()) {
                datos.add(d);
            }
        }
        documento.add(new Paragraph(String.join("  ·  ", datos), PEQUENA));
        documento.add(new Chunk(new LineSeparator(0.8f, 100, Color.GRAY, Element.ALIGN_CENTER, -4)));
    }

    /** Pie: fecha de generación y número de página. */
    private static final class PiePagina extends PdfPageEventHelper {

        private final String generado;

        PiePagina(LocalDateTime momento) {
            this.generado = "Generado con OdontoGest el " + GENERADO.format(momento);
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            float y = document.bottom() - 24;
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_LEFT,
                    new Phrase(generado, PEQUENA), document.left(), y, 0);
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT,
                    new Phrase("Página " + writer.getPageNumber(), PEQUENA), document.right(), y, 0);
        }
    }
}
