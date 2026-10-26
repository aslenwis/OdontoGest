package com.odontogest.documento;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.odontogest.dominio.odontograma.ColorHallazgoASEJ;
import com.odontogest.dominio.odontograma.PiezaDentalASEJ;
import com.odontogest.dominio.odontograma.ResultadoIndiceASEJ;
import com.odontogest.dominio.odontograma.SuperficieASEJ;
import com.odontogest.servicio.dto.FichaOdontologicaASEJ;
import com.odontogest.servicio.dto.HallazgoDtoASEJ;
import com.odontogest.servicio.dto.OdontogramaVistaASEJ;
import com.odontogest.servicio.dto.PacienteDetalleASEJ;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FichaOdontologicaPdfASEJ {

    private static final DateTimeFormatter FECHAASEJ = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORAASEJ = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Color AZULASEJ = new Color(0x1D, 0x4E, 0xD8);
    private static final Color ROJOASEJ = new Color(0xDC, 0x26, 0x26);
    private static final Color FONDO_TITULOASEJ = new Color(0xF1, 0xF5, 0xF9);

    private final PlantillaPdfASEJ plantillaASEJ;

    public byte[] generarASEJ(FichaOdontologicaASEJ fASEJ) {
        return plantillaASEJ.generarASEJ(fASEJ.clinicaASEJ(), "FICHA ODONTOLÓGICA", docASEJ -> {
            datosPacienteASEJ(docASEJ, fASEJ.pacienteASEJ(), fASEJ.vistaASEJ());
            imagenASEJ(docASEJ, fASEJ.imagenPngASEJ());
            hallazgosASEJ(docASEJ, fASEJ.vistaASEJ());
            textoLibreASEJ(docASEJ, "Especificaciones", fASEJ.vistaASEJ().especificacionesASEJ());
            textoLibreASEJ(docASEJ, "Observaciones", fASEJ.vistaASEJ().observacionesASEJ());
            indicesASEJ(docASEJ, fASEJ.vistaASEJ());
            firmaASEJ(docASEJ, fASEJ);
        });
    }

    private static void datosPacienteASEJ(Document docASEJ, PacienteDetalleASEJ pASEJ, OdontogramaVistaASEJ vASEJ) {
        PdfPTable tASEJ = new PdfPTable(new float[]{1.2f, 2.6f, 1.2f, 2f});
        tASEJ.setWidthPercentage(100);
        tASEJ.setSpacingAfter(8);
        celdasASEJ(tASEJ, "Paciente", pASEJ.nombreCompletoASEJ(), "Historia", pASEJ.numeroHistoriaASEJ());
        celdasASEJ(tASEJ, "Documento", pASEJ.documentoASEJ(), "Edad", pASEJ.edadASEJ() + (pASEJ.edadASEJ() == 1 ? " año" : " años"));
        String estadoASEJ = vASEJ.borradorASEJ() ? "Borrador (sin firmar)" : "Cerrado el " + FECHA_HORAASEJ.format(vASEJ.cerradoEnASEJ());
        celdasASEJ(tASEJ, "Odontograma", vASEJ.tipoASEJ().etiquetaASEJ + " · " + estadoASEJ, "Dentición", vASEJ.denticionASEJ().name().toLowerCase());
        if (vASEJ.hastaASEJ() != null) {
            celdasASEJ(tASEJ, "Estado al", FECHA_HORAASEJ.format(vASEJ.hastaASEJ()), "Emitido", FECHAASEJ.format(LocalDateTime.now()));
        }
        if (pASEJ.tieneAlergiasASEJ()) {
            PdfPCell eASEJ = new PdfPCell(new Phrase("Alergias", PlantillaPdfASEJ.NEGRITAASEJ));
            eASEJ.setBackgroundColor(FONDO_TITULOASEJ);
            tASEJ.addCell(eASEJ);
            PdfPCell aASEJ = new PdfPCell(new Phrase(pASEJ.alergiasASEJ().replaceAll("\\s+", " "), fuenteASEJ(true, ROJOASEJ)));
            aASEJ.setColspan(3);
            tASEJ.addCell(aASEJ);
        }
        PlantillaPdfASEJ.agregarASEJ(docASEJ, tASEJ);
    }

    private static void celdasASEJ(PdfPTable tASEJ, String e1ASEJ, String v1ASEJ, String e2ASEJ, String v2ASEJ) {
        for (String[] parASEJ : new String[][]{{e1ASEJ, v1ASEJ}, {e2ASEJ, v2ASEJ}}) {
            PdfPCell etiquetaASEJ = new PdfPCell(new Phrase(parASEJ[0], PlantillaPdfASEJ.NEGRITAASEJ));
            etiquetaASEJ.setBackgroundColor(FONDO_TITULOASEJ);
            tASEJ.addCell(etiquetaASEJ);
            tASEJ.addCell(new PdfPCell(new Phrase(parASEJ[1] == null ? "" : parASEJ[1], PlantillaPdfASEJ.NORMALASEJ)));
        }
    }

    private static void imagenASEJ(Document docASEJ, byte[] pngASEJ) {
        if (pngASEJ == null || pngASEJ.length == 0) {
            return;
        }
        try {
            Image imgASEJ = Image.getInstance(pngASEJ);
            imgASEJ.scaleToFit(docASEJ.right() - docASEJ.left(), 300);
            imgASEJ.setAlignment(Element.ALIGN_CENTER);
            PlantillaPdfASEJ.agregarASEJ(docASEJ, imgASEJ);
        } catch (IOException eASEJ) {
            throw new IllegalStateException("No se pudo insertar el gráfico del odontograma", eASEJ);
        }
    }

    private static void hallazgosASEJ(Document docASEJ, OdontogramaVistaASEJ vASEJ) {
        List<HallazgoDtoASEJ> vigentesASEJ = vASEJ.historialASEJ().stream()
                .filter(hASEJ -> hASEJ.vigenteASEJ() && !hASEJ.anuladoASEJ())
                .sorted(Comparator.comparingInt((HallazgoDtoASEJ hASEJ) -> PiezaDentalASEJ.deASEJ(hASEJ.piezaASEJ()).ordenDibujoASEJ()))
                .toList();
        PlantillaPdfASEJ.agregarASEJ(docASEJ, subtituloASEJ("Hallazgos vigentes (" + vigentesASEJ.size() + ")"));
        if (vigentesASEJ.isEmpty()) {
            PlantillaPdfASEJ.agregarASEJ(docASEJ, new Paragraph("Sin hallazgos registrados.", PlantillaPdfASEJ.NORMALASEJ));
            return;
        }
        PdfPTable tASEJ = new PdfPTable(new float[]{1.1f, 3.4f, 1.6f, 1f, 1.2f});
        tASEJ.setWidthPercentage(100);
        tASEJ.setHeaderRows(1);
        for (String cASEJ : List.of("Pieza", "Hallazgo", "Superficies", "Sigla", "Estado")) {
            PdfPCell hASEJ = new PdfPCell(new Phrase(cASEJ, PlantillaPdfASEJ.NEGRITAASEJ));
            hASEJ.setBackgroundColor(FONDO_TITULOASEJ);
            tASEJ.addCell(hASEJ);
        }
        for (HallazgoDtoASEJ hASEJ : vigentesASEJ) {
            Color colorASEJ = hASEJ.colorASEJ() == ColorHallazgoASEJ.ROJO ? ROJOASEJ : AZULASEJ;
            tASEJ.addCell(new Phrase(hASEJ.piezaFinASEJ() == null ? String.valueOf(hASEJ.piezaASEJ()) : hASEJ.piezaASEJ() + "–" + hASEJ.piezaFinASEJ(), PlantillaPdfASEJ.NORMALASEJ));
            tASEJ.addCell(new Phrase(hASEJ.nombreTipoASEJ() + (hASEJ.notaASEJ() == null ? "" : " (" + hASEJ.notaASEJ() + ")"), PlantillaPdfASEJ.NORMALASEJ));
            tASEJ.addCell(new Phrase(superficiesASEJ(hASEJ), PlantillaPdfASEJ.NORMALASEJ));
            tASEJ.addCell(new Phrase(hASEJ.siglaASEJ() == null ? "" : hASEJ.siglaASEJ(),
                    fuenteASEJ(true, colorASEJ)));
            tASEJ.addCell(new Phrase(hASEJ.estadoASEJ() == null ? (hASEJ.colorASEJ() == ColorHallazgoASEJ.ROJO ? "Rojo" : "Azul")
                    : hASEJ.estadoASEJ().name().charAt(0) + hASEJ.estadoASEJ().name().substring(1).toLowerCase(),
                    fuenteASEJ(false, colorASEJ)));
        }
        PlantillaPdfASEJ.agregarASEJ(docASEJ, tASEJ);
    }

    private static String superficiesASEJ(HallazgoDtoASEJ hASEJ) {
        if (hASEJ.superficiesASEJ().isEmpty()) {
            return "—";
        }
        PiezaDentalASEJ pASEJ = PiezaDentalASEJ.deASEJ(hASEJ.piezaASEJ());
        return hASEJ.superficiesASEJ().stream().sorted().map((SuperficieASEJ sASEJ) -> sASEJ.etiquetaASEJ(pASEJ)).collect(Collectors.joining(""));
    }

    private static void textoLibreASEJ(Document docASEJ, String tituloASEJ, String textoASEJ) {
        PlantillaPdfASEJ.agregarASEJ(docASEJ, subtituloASEJ(tituloASEJ));
        PlantillaPdfASEJ.agregarASEJ(docASEJ, PlantillaPdfASEJ.parrafoASEJ(textoASEJ == null || textoASEJ.isBlank() ? "—" : textoASEJ, PlantillaPdfASEJ.NORMALASEJ));
    }

    private static void indicesASEJ(Document docASEJ, OdontogramaVistaASEJ vASEJ) {
        PlantillaPdfASEJ.agregarASEJ(docASEJ, subtituloASEJ("Índices epidemiológicos"));
        ResultadoIndiceASEJ cASEJ = vASEJ.indicesASEJ().cpodASEJ();
        ResultadoIndiceASEJ eASEJ = vASEJ.indicesASEJ().ceodASEJ();
        PlantillaPdfASEJ.agregarASEJ(docASEJ, PlantillaPdfASEJ.parrafoASEJ(String.format(
                "CPO-D: C=%d P=%d O=%d, total %d (%s)%s.   ceo-d: c=%d e=%d o=%d, total %d. Nivel de severidad según la referencia OMS a los 12 años.",
                cASEJ.cASEJ(), cASEJ.pASEJ(), cASEJ.oASEJ(), cASEJ.totalASEJ(), cASEJ.nivelASEJ().name().replace('_', ' ').toLowerCase(),
                vASEJ.indicesASEJ().excluyeTercerasMolaresASEJ() ? ", sin terceras molares" : "",
                eASEJ.cASEJ(), eASEJ.pASEJ(), eASEJ.oASEJ(), eASEJ.totalASEJ()), PlantillaPdfASEJ.NORMALASEJ));
        if (vASEJ.hashCierreASEJ() != null) {
            PlantillaPdfASEJ.agregarASEJ(docASEJ, new Paragraph("Hash de integridad (SHA-256): " + vASEJ.hashCierreASEJ(), PlantillaPdfASEJ.PEQUENAASEJ));
        }
    }

    private static void firmaASEJ(Document docASEJ, FichaOdontologicaASEJ fASEJ) {
        PdfPTable tASEJ = new PdfPTable(1);
        tASEJ.setWidthPercentage(45);
        tASEJ.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tASEJ.setSpacingBefore(40);
        PdfPCell cASEJ = new PdfPCell();
        cASEJ.setBorder(Rectangle.NO_BORDER);
        cASEJ.addElement(new Paragraph("______________________________", PlantillaPdfASEJ.NORMALASEJ));
        cASEJ.addElement(new Paragraph("C.D. " + fASEJ.odontologoNombreASEJ(), PlantillaPdfASEJ.NEGRITAASEJ));
        cASEJ.addElement(new Paragraph("COP " + fASEJ.odontologoCopASEJ(), PlantillaPdfASEJ.NORMALASEJ));
        tASEJ.addCell(cASEJ);
        PlantillaPdfASEJ.agregarASEJ(docASEJ, tASEJ);
    }

    private static Font fuenteASEJ(boolean negritaASEJ, Color colorASEJ) {
        return FontFactory.getFont(negritaASEJ ? FontFactory.HELVETICA_BOLD : FontFactory.HELVETICA, 10, colorASEJ);
    }

    private static Paragraph subtituloASEJ(String textoASEJ) {
        Paragraph pASEJ = new Paragraph(textoASEJ, PlantillaPdfASEJ.NEGRITAASEJ);
        pASEJ.setSpacingBefore(8);
        pASEJ.setSpacingAfter(4);
        return pASEJ;
    }
}
