/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
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
import com.odontogest.dominio.comun.NumeroALetras;
import com.odontogest.servicio.dto.AplicacionDto;
import com.odontogest.servicio.dto.PacienteDetalle;
import com.odontogest.servicio.dto.PagoDto;
import com.odontogest.servicio.dto.ReciboDocumento;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Recibo interno de pago en PDF (RF-CAJ-02): número correlativo, datos del paciente, medio de pago, cuotas a las
 * que se aplicó, monto en letras y saldo pendiente. Un pago anulado se imprime con la marca ANULADO y su motivo.
 */
@Component
@RequiredArgsConstructor
public class ReciboPdf {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Color FONDO_TITULO = new Color(0xF1, 0xF5, 0xF9);
    private static final Color ROJO = new Color(0xDC, 0x26, 0x26);
    private static final Font NUMERO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final Font MONTO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
    private static final Font ANULADO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, ROJO);

    private final PlantillaPdf plantilla;

    public byte[] generar(ReciboDocumento r) {
        PagoDto pago = r.pago();
        return plantilla.generar(r.clinica(), "RECIBO INTERNO DE PAGO", doc -> {
            Paragraph numero = new Paragraph("N° " + pago.numeroRecibo(), NUMERO);
            numero.setAlignment(Element.ALIGN_CENTER);
            numero.setSpacingAfter(10);
            PlantillaPdf.agregar(doc, numero);
            if (pago.anulado()) {
                anulado(doc, pago);
            }
            datos(doc, r.paciente(), pago);
            aplicaciones(doc, pago.aplicaciones());
            total(doc, pago);
            if (!pago.anulado()) {
                Paragraph saldo = new Paragraph("Saldo pendiente del paciente al emitir este recibo: S/ "
                        + PresupuestoPdf.soles(r.saldoPendiente()), PlantillaPdf.NORMAL);
                saldo.setSpacingBefore(4);
                PlantillaPdf.agregar(doc, saldo);
            }
            firma(doc, pago);
            PlantillaPdf.agregar(doc, PlantillaPdf.parrafo("Documento interno de control. No es un comprobante de "
                    + "pago electrónico (boleta o factura) de la SUNAT.", PlantillaPdf.PEQUENA));
        });
    }

    private static void anulado(Document doc, PagoDto pago) {
        Paragraph p = new Paragraph("ANULADO", ANULADO);
        p.setAlignment(Element.ALIGN_CENTER);
        PlantillaPdf.agregar(doc, p);
        Paragraph motivo = new Paragraph("Anulado el " + FECHA_HORA.format(pago.anuladoEn()) + ". Motivo: "
                + pago.motivoAnulacion(), FontFactory.getFont(FontFactory.HELVETICA, 10, ROJO));
        motivo.setAlignment(Element.ALIGN_CENTER);
        motivo.setSpacingAfter(10);
        PlantillaPdf.agregar(doc, motivo);
    }

    private static void datos(Document doc, PacienteDetalle p, PagoDto pago) {
        PdfPTable t = new PdfPTable(new float[]{1.2f, 2.6f, 1.2f, 2f});
        t.setWidthPercentage(100);
        t.setSpacingAfter(10);
        celdas(t, "Paciente", p.nombreCompleto(), "Historia", p.numeroHistoria());
        celdas(t, "Documento", p.documento(), "Fecha", FECHA_HORA.format(pago.fecha()));
        celdas(t, "Medio de pago", pago.medio().etiqueta(), "Referencia", pago.referencia() == null ? "—" : pago.referencia());
        PlantillaPdf.agregar(doc, t);
    }

    private static void celdas(PdfPTable t, String e1, String v1, String e2, String v2) {
        for (String[] par : new String[][]{{e1, v1}, {e2, v2}}) {
            PdfPCell etiqueta = new PdfPCell(new Phrase(par[0], PlantillaPdf.NEGRITA));
            etiqueta.setBackgroundColor(FONDO_TITULO);
            t.addCell(etiqueta);
            t.addCell(new PdfPCell(new Phrase(par[1] == null ? "" : par[1], PlantillaPdf.NORMAL)));
        }
    }

    private static void aplicaciones(Document doc, List<AplicacionDto> aplicaciones) {
        Paragraph titulo = new Paragraph("Aplicado a", PlantillaPdf.NEGRITA);
        titulo.setSpacingAfter(4);
        PlantillaPdf.agregar(doc, titulo);
        PdfPTable t = new PdfPTable(new float[]{3f, 1.6f, 1.4f});
        t.setWidthPercentage(70);
        t.setHorizontalAlignment(Element.ALIGN_LEFT);
        for (String c : List.of("Cuota", "Vencimiento", "Monto S/")) {
            PdfPCell h = new PdfPCell(new Phrase(c, PlantillaPdf.NEGRITA));
            h.setBackgroundColor(FONDO_TITULO);
            if (c.startsWith("Monto")) {
                h.setHorizontalAlignment(Element.ALIGN_RIGHT);
            }
            t.addCell(h);
        }
        for (AplicacionDto a : aplicaciones) {
            t.addCell(new Phrase(a.etiqueta(), PlantillaPdf.NORMAL));
            t.addCell(new Phrase(FECHA.format(a.vencimiento()), PlantillaPdf.NORMAL));
            PdfPCell m = new PdfPCell(new Phrase(PresupuestoPdf.soles(a.monto()), PlantillaPdf.NORMAL));
            m.setHorizontalAlignment(Element.ALIGN_RIGHT);
            t.addCell(m);
        }
        PlantillaPdf.agregar(doc, t);
    }

    private static void total(Document doc, PagoDto pago) {
        Paragraph total = new Paragraph("TOTAL RECIBIDO: S/ " + PresupuestoPdf.soles(pago.monto()), MONTO);
        total.setSpacingBefore(12);
        PlantillaPdf.agregar(doc, total);
        BigDecimal centimos = pago.monto().setScale(2).movePointRight(2);
        PlantillaPdf.agregar(doc, new Paragraph("Son: " + NumeroALetras.soles(centimos.longValueExact()), PlantillaPdf.NEGRITA));
    }

    private static void firma(Document doc, PagoDto pago) {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(45);
        t.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.setSpacingBefore(40);
        t.setSpacingAfter(16);
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.addElement(new Paragraph("______________________________", PlantillaPdf.NORMAL));
        c.addElement(new Paragraph("Recibido por: " + pago.registradoPor(), PlantillaPdf.NEGRITA));
        t.addCell(c);
        PlantillaPdf.agregar(doc, t);
    }
}
