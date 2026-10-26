package com.odontogest.documento;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.odontogest.servicio.dto.DatosClinicaASEJ;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Component
public class PlantillaPdfASEJ {

    public static final Font TITULOASEJ = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
    public static final Font CLINICAASEJ = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
    public static final Font NORMALASEJ = FontFactory.getFont(FontFactory.HELVETICA, 10);
    public static final Font NEGRITAASEJ = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    public static final Font PEQUENAASEJ = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY);

    private static final DateTimeFormatter GENERADOASEJ = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] generarASEJ(DatosClinicaASEJ clinicaASEJ, String tituloASEJ, Consumer<Document> contenidoASEJ) {
        ByteArrayOutputStream salidaASEJ = new ByteArrayOutputStream();
        Document documentoASEJ = new Document(PageSize.A4, 56, 56, 56, 56);
        try {
            PdfWriter writerASEJ = PdfWriter.getInstance(documentoASEJ, salidaASEJ);
            writerASEJ.setPageEvent(new PiePaginaASEJ(LocalDateTime.now()));
            documentoASEJ.addTitle(tituloASEJ);
            documentoASEJ.addCreator("OdontoGest");
            documentoASEJ.open();
            cabeceraASEJ(documentoASEJ, clinicaASEJ);
            Paragraph tASEJ = new Paragraph(tituloASEJ, TITULOASEJ);
            tASEJ.setAlignment(Element.ALIGN_CENTER);
            tASEJ.setSpacingBefore(14);
            tASEJ.setSpacingAfter(14);
            documentoASEJ.add(tASEJ);
            contenidoASEJ.accept(documentoASEJ);
        } catch (DocumentException eASEJ) {
            throw new IllegalStateException("No se pudo generar el PDF: " + eASEJ.getMessage(), eASEJ);
        } finally {
            if (documentoASEJ.isOpen()) {
                documentoASEJ.close();
            }
        }
        return salidaASEJ.toByteArray();
    }

    public static void agregarASEJ(Document documentoASEJ, Element elementoASEJ) {
        try {
            documentoASEJ.add(elementoASEJ);
        } catch (DocumentException eASEJ) {
            throw new IllegalStateException("No se pudo escribir el PDF: " + eASEJ.getMessage(), eASEJ);
        }
    }

    public static Paragraph parrafoASEJ(String textoASEJ, Font fuenteASEJ) {
        Paragraph pASEJ = new Paragraph(textoASEJ, fuenteASEJ);
        pASEJ.setAlignment(Element.ALIGN_JUSTIFIED);
        pASEJ.setSpacingAfter(7);
        pASEJ.setLeading(14);
        return pASEJ;
    }

    private static void cabeceraASEJ(Document documentoASEJ, DatosClinicaASEJ clinicaASEJ) throws DocumentException {
        Image logoASEJ = logoASEJ(clinicaASEJ.logoRutaASEJ());
        if (logoASEJ != null) {
            logoASEJ.scaleToFit(140, 48);
            logoASEJ.setAbsolutePosition(documentoASEJ.right() - logoASEJ.getScaledWidth(), documentoASEJ.top() - logoASEJ.getScaledHeight() + 6);
            documentoASEJ.add(logoASEJ);
        }
        documentoASEJ.add(new Paragraph(clinicaASEJ.nombreASEJ(), CLINICAASEJ));
        List<String> datosASEJ = new ArrayList<>();
        if (!clinicaASEJ.rucASEJ().isBlank()) {
            datosASEJ.add("RUC " + clinicaASEJ.rucASEJ());
        }
        for (String dASEJ : List.of(clinicaASEJ.direccionASEJ(), clinicaASEJ.telefonoASEJ(), clinicaASEJ.emailASEJ())) {
            if (!dASEJ.isBlank()) {
                datosASEJ.add(dASEJ);
            }
        }
        documentoASEJ.add(new Paragraph(String.join("  ·  ", datosASEJ), PEQUENAASEJ));
        documentoASEJ.add(new Chunk(new LineSeparator(0.8f, 100, Color.GRAY, Element.ALIGN_CENTER, -4)));
    }

    private static Image logoASEJ(String rutaASEJ) {
        if (rutaASEJ == null || rutaASEJ.isBlank()) {
            return null;
        }
        Path archivoASEJ = Path.of(rutaASEJ);
        if (!Files.isRegularFile(archivoASEJ)) {
            return null;
        }
        try {
            return Image.getInstance(Files.readAllBytes(archivoASEJ));
        } catch (IOException | RuntimeException eASEJ) {
            return null;
        }
    }

    private static final class PiePaginaASEJ extends PdfPageEventHelper {

        private final String generadoASEJ;

        PiePaginaASEJ(LocalDateTime momentoASEJ) {
            this.generadoASEJ = "Generado con OdontoGest el " + GENERADOASEJ.format(momentoASEJ);
        }

        @Override
        public void onEndPage(PdfWriter writerASEJ, Document documentASEJ) {
            float yASEJ = documentASEJ.bottom() - 24;
            ColumnText.showTextAligned(writerASEJ.getDirectContent(), Element.ALIGN_LEFT,
                    new Phrase(generadoASEJ, PEQUENAASEJ), documentASEJ.left(), yASEJ, 0);
            ColumnText.showTextAligned(writerASEJ.getDirectContent(), Element.ALIGN_RIGHT,
                    new Phrase("Página " + writerASEJ.getPageNumber(), PEQUENAASEJ), documentASEJ.right(), yASEJ, 0);
        }
    }
}
