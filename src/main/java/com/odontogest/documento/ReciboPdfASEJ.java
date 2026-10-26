package com.odontogest.documento;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.odontogest.dominio.comun.NumeroALetrasASEJ;
import com.odontogest.servicio.dto.AplicacionDtoASEJ;
import com.odontogest.servicio.dto.PacienteDetalleASEJ;
import com.odontogest.servicio.dto.PagoDtoASEJ;
import com.odontogest.servicio.dto.ReciboDocumentoASEJ;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ReciboPdfASEJ {

    private static final DateTimeFormatter FECHAASEJ = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORAASEJ = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Color FONDO_TITULOASEJ = new Color(0xF1, 0xF5, 0xF9);
    private static final Color ROJOASEJ = new Color(0xDC, 0x26, 0x26);
    private static final Font NUMEROASEJ = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final Font MONTOASEJ = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
    private static final Font ANULADOASEJ = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, ROJOASEJ);

    private final PlantillaPdfASEJ plantillaASEJ;

    public byte[] generarASEJ(ReciboDocumentoASEJ rASEJ) {
        PagoDtoASEJ pagoASEJ = rASEJ.pagoASEJ();
        return plantillaASEJ.generarASEJ(rASEJ.clinicaASEJ(), "RECIBO INTERNO DE PAGO", docASEJ -> {
            Paragraph numeroASEJ = new Paragraph("N° " + pagoASEJ.numeroReciboASEJ(), NUMEROASEJ);
            numeroASEJ.setAlignment(Element.ALIGN_CENTER);
            numeroASEJ.setSpacingAfter(10);
            PlantillaPdfASEJ.agregarASEJ(docASEJ, numeroASEJ);
            if (pagoASEJ.anuladoASEJ()) {
                anuladoASEJ(docASEJ, pagoASEJ);
            }
            datosASEJ(docASEJ, rASEJ.pacienteASEJ(), pagoASEJ);
            aplicacionesASEJ(docASEJ, pagoASEJ.aplicacionesASEJ());
            totalASEJ(docASEJ, pagoASEJ);
            if (!pagoASEJ.anuladoASEJ()) {
                Paragraph saldoASEJ = new Paragraph("Saldo pendiente del paciente al emitir este recibo: S/ "
                        + PresupuestoPdfASEJ.solesASEJ(rASEJ.saldoPendienteASEJ()), PlantillaPdfASEJ.NORMALASEJ);
                saldoASEJ.setSpacingBefore(4);
                PlantillaPdfASEJ.agregarASEJ(docASEJ, saldoASEJ);
            }
            firmaASEJ(docASEJ, pagoASEJ);
            PlantillaPdfASEJ.agregarASEJ(docASEJ, PlantillaPdfASEJ.parrafoASEJ("Documento interno de control. No es un comprobante de "
                    + "pago electrónico (boleta o factura) de la SUNAT.", PlantillaPdfASEJ.PEQUENAASEJ));
        });
    }

    private static void anuladoASEJ(Document docASEJ, PagoDtoASEJ pagoASEJ) {
        Paragraph pASEJ = new Paragraph("ANULADO", ANULADOASEJ);
        pASEJ.setAlignment(Element.ALIGN_CENTER);
        PlantillaPdfASEJ.agregarASEJ(docASEJ, pASEJ);
        Paragraph motivoASEJ = new Paragraph("Anulado el " + FECHA_HORAASEJ.format(pagoASEJ.anuladoEnASEJ()) + ". Motivo: "
                + pagoASEJ.motivoAnulacionASEJ(), FontFactory.getFont(FontFactory.HELVETICA, 10, ROJOASEJ));
        motivoASEJ.setAlignment(Element.ALIGN_CENTER);
        motivoASEJ.setSpacingAfter(10);
        PlantillaPdfASEJ.agregarASEJ(docASEJ, motivoASEJ);
    }

    private static void datosASEJ(Document docASEJ, PacienteDetalleASEJ pASEJ, PagoDtoASEJ pagoASEJ) {
        PdfPTable tASEJ = new PdfPTable(new float[]{1.2f, 2.6f, 1.2f, 2f});
        tASEJ.setWidthPercentage(100);
        tASEJ.setSpacingAfter(10);
        celdasASEJ(tASEJ, "Paciente", pASEJ.nombreCompletoASEJ(), "Historia", pASEJ.numeroHistoriaASEJ());
        celdasASEJ(tASEJ, "Documento", pASEJ.documentoASEJ(), "Fecha", FECHA_HORAASEJ.format(pagoASEJ.fechaASEJ()));
        celdasASEJ(tASEJ, "Medio de pago", pagoASEJ.medioASEJ().etiquetaASEJ(), "Referencia", pagoASEJ.referenciaASEJ() == null ? "—" : pagoASEJ.referenciaASEJ());
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

    private static void aplicacionesASEJ(Document docASEJ, List<AplicacionDtoASEJ> aplicacionesASEJ) {
        Paragraph tituloASEJ = new Paragraph("Aplicado a", PlantillaPdfASEJ.NEGRITAASEJ);
        tituloASEJ.setSpacingAfter(4);
        PlantillaPdfASEJ.agregarASEJ(docASEJ, tituloASEJ);
        PdfPTable tASEJ = new PdfPTable(new float[]{3f, 1.6f, 1.4f});
        tASEJ.setWidthPercentage(70);
        tASEJ.setHorizontalAlignment(Element.ALIGN_LEFT);
        for (String cASEJ : List.of("Cuota", "Vencimiento", "Monto S/")) {
            PdfPCell hASEJ = new PdfPCell(new Phrase(cASEJ, PlantillaPdfASEJ.NEGRITAASEJ));
            hASEJ.setBackgroundColor(FONDO_TITULOASEJ);
            if (cASEJ.startsWith("Monto")) {
                hASEJ.setHorizontalAlignment(Element.ALIGN_RIGHT);
            }
            tASEJ.addCell(hASEJ);
        }
        for (AplicacionDtoASEJ aASEJ : aplicacionesASEJ) {
            tASEJ.addCell(new Phrase(aASEJ.etiquetaASEJ(), PlantillaPdfASEJ.NORMALASEJ));
            tASEJ.addCell(new Phrase(FECHAASEJ.format(aASEJ.vencimientoASEJ()), PlantillaPdfASEJ.NORMALASEJ));
            PdfPCell mASEJ = new PdfPCell(new Phrase(PresupuestoPdfASEJ.solesASEJ(aASEJ.montoASEJ()), PlantillaPdfASEJ.NORMALASEJ));
            mASEJ.setHorizontalAlignment(Element.ALIGN_RIGHT);
            tASEJ.addCell(mASEJ);
        }
        PlantillaPdfASEJ.agregarASEJ(docASEJ, tASEJ);
    }

    private static void totalASEJ(Document docASEJ, PagoDtoASEJ pagoASEJ) {
        Paragraph totalASEJ = new Paragraph("TOTAL RECIBIDO: S/ " + PresupuestoPdfASEJ.solesASEJ(pagoASEJ.montoASEJ()), MONTOASEJ);
        totalASEJ.setSpacingBefore(12);
        PlantillaPdfASEJ.agregarASEJ(docASEJ, totalASEJ);
        BigDecimal centimosASEJ = pagoASEJ.montoASEJ().setScale(2).movePointRight(2);
        PlantillaPdfASEJ.agregarASEJ(docASEJ, new Paragraph("Son: " + NumeroALetrasASEJ.solesASEJ(centimosASEJ.longValueExact()), PlantillaPdfASEJ.NEGRITAASEJ));
    }

    private static void firmaASEJ(Document docASEJ, PagoDtoASEJ pagoASEJ) {
        PdfPTable tASEJ = new PdfPTable(1);
        tASEJ.setWidthPercentage(45);
        tASEJ.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tASEJ.setSpacingBefore(40);
        tASEJ.setSpacingAfter(16);
        PdfPCell cASEJ = new PdfPCell();
        cASEJ.setBorder(Rectangle.NO_BORDER);
        cASEJ.addElement(new Paragraph("______________________________", PlantillaPdfASEJ.NORMALASEJ));
        cASEJ.addElement(new Paragraph("Recibido por: " + pagoASEJ.registradoPorASEJ(), PlantillaPdfASEJ.NEGRITAASEJ));
        tASEJ.addCell(cASEJ);
        PlantillaPdfASEJ.agregarASEJ(docASEJ, tASEJ);
    }
}
