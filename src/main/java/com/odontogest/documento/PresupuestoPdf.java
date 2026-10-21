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
import com.odontogest.modelo.EstadoPlanItem;
import com.odontogest.servicio.dto.CuotaDto;
import com.odontogest.servicio.dto.PacienteDetalle;
import com.odontogest.servicio.dto.PlanDto;
import com.odontogest.servicio.dto.PlanItemDto;
import com.odontogest.servicio.dto.PresupuestoDocumento;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Presupuesto del plan de tratamiento en PDF (RF-PLN-06): datos del paciente, procedimientos agrupados por fase con
 * subtotales, totales con el monto en letras, cronograma de cuotas (si existe) y firmas.
 */
@Component
@RequiredArgsConstructor
public class PresupuestoPdf {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color FONDO_TITULO = new Color(0xF1, 0xF5, 0xF9);
    private static final Color FONDO_FASE = new Color(0xE2, 0xE8, 0xF0);
    private static final Font PEQUENA_NEGRA = FontFactory.getFont(FontFactory.HELVETICA, 9);
    private static final Font PEQUENA_NEGRITA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);

    private final PlantillaPdf plantilla;

    public byte[] generar(PresupuestoDocumento d) {
        PlanDto plan = d.plan();
        return plantilla.generar(d.clinica(), "PRESUPUESTO DE TRATAMIENTO ODONTOLÓGICO", doc -> {
            datos(doc, d.paciente(), plan);
            procedimientos(doc, plan);
            totales(doc, plan);
            cronograma(doc, plan.cuotas());
            if (plan.observaciones() != null) {
                PlantillaPdf.agregar(doc, subtitulo("Observaciones"));
                PlantillaPdf.agregar(doc, PlantillaPdf.parrafo(plan.observaciones(), PlantillaPdf.NORMAL));
            }
            PlantillaPdf.agregar(doc, PlantillaPdf.parrafo("Precios en soles. El presupuesto es referencial: puede "
                    + "variar si el diagnóstico cambia durante el tratamiento.", PlantillaPdf.PEQUENA));
            firmas(doc, d.paciente(), plan);
        });
    }

    private static void datos(Document doc, PacienteDetalle p, PlanDto plan) {
        PdfPTable t = new PdfPTable(new float[]{1.2f, 2.6f, 1.2f, 2f});
        t.setWidthPercentage(100);
        t.setSpacingAfter(8);
        celdas(t, "Paciente", p.nombreCompleto(), "Historia", p.numeroHistoria());
        celdas(t, "Documento", p.documento(), "Edad", p.edad() + (p.edad() == 1 ? " año" : " años"));
        celdas(t, "Plan", "N° " + plan.id() + " · " + plan.estado().etiqueta(), "Fecha", FECHA.format(plan.fecha()));
        celdas(t, "Odontólogo", "C.D. " + plan.odontologoNombre(), "COP", plan.odontologoCop());
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

    private static void procedimientos(Document doc, PlanDto plan) {
        List<PlanItemDto> items = plan.items().stream().filter(i -> i.estado() != EstadoPlanItem.CANCELADO).toList();
        PlantillaPdf.agregar(doc, subtitulo("Procedimientos (" + items.size() + ")"));
        if (items.isEmpty()) {
            PlantillaPdf.agregar(doc, new Paragraph("El plan aún no tiene procedimientos.", PlantillaPdf.NORMAL));
            return;
        }
        PdfPTable t = new PdfPTable(new float[]{0.8f, 0.8f, 3.6f, 0.7f, 1.2f, 1.1f, 1.2f});
        t.setWidthPercentage(100);
        t.setHeaderRows(1);
        for (String c : List.of("Pieza", "Sup.", "Procedimiento", "Cant.", "P. unit.", "Desc.", "Total")) {
            PdfPCell h = new PdfPCell(new Phrase(c, PEQUENA_NEGRITA));
            h.setBackgroundColor(FONDO_TITULO);
            h.setHorizontalAlignment(c.startsWith("P.") || c.equals("Desc.") || c.equals("Total") || c.equals("Cant.")
                    ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
            t.addCell(h);
        }
        Map<Integer, List<PlanItemDto>> porFase = items.stream()
                .collect(Collectors.groupingBy(PlanItemDto::fase, TreeMap::new, Collectors.toList()));
        porFase.forEach((fase, lista) -> {
            BigDecimal subtotal = lista.stream().map(PlanItemDto::total).reduce(BigDecimal.ZERO, BigDecimal::add);
            PdfPCell titulo = new PdfPCell(new Phrase(PlanItemDto.nombreFase(fase), PEQUENA_NEGRITA));
            titulo.setColspan(6);
            titulo.setBackgroundColor(FONDO_FASE);
            t.addCell(titulo);
            PdfPCell sub = derecha(soles(subtotal), PEQUENA_NEGRITA);
            sub.setBackgroundColor(FONDO_FASE);
            t.addCell(sub);
            for (PlanItemDto i : lista) {
                t.addCell(new Phrase(i.pieza() == null ? "—" : String.valueOf(i.pieza()), PEQUENA_NEGRA));
                t.addCell(new Phrase(i.superficiesTexto().isEmpty() ? "—" : i.superficiesTexto(), PEQUENA_NEGRA));
                t.addCell(new Phrase(i.procedimientoCodigo() + " · " + i.procedimientoNombre()
                        + (i.estado() == EstadoPlanItem.REALIZADO ? " (realizado)" : ""), PEQUENA_NEGRA));
                t.addCell(derecha(String.valueOf(i.cantidad()), PEQUENA_NEGRA));
                t.addCell(derecha(soles(i.precioUnit()), PEQUENA_NEGRA));
                t.addCell(derecha(i.descuento().signum() == 0 ? "—" : soles(i.descuento()), PEQUENA_NEGRA));
                t.addCell(derecha(soles(i.total()), PEQUENA_NEGRA));
            }
        });
        PlantillaPdf.agregar(doc, t);
    }

    private static void totales(Document doc, PlanDto plan) {
        PdfPTable t = new PdfPTable(new float[]{2f, 1.3f});
        t.setWidthPercentage(40);
        t.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.setSpacingBefore(6);
        fila(t, "Subtotal", soles(plan.subtotal()), PlantillaPdf.NORMAL);
        fila(t, "Descuento", plan.descuento().signum() == 0 ? "—" : "− " + soles(plan.descuento()), PlantillaPdf.NORMAL);
        fila(t, "TOTAL S/", soles(plan.total()), PlantillaPdf.NEGRITA);
        PlantillaPdf.agregar(doc, t);
        Paragraph letras = new Paragraph("Son: " + NumeroALetras.soles(plan.totalCent()),
                PlantillaPdf.NEGRITA);
        letras.setAlignment(Element.ALIGN_RIGHT);
        letras.setSpacingAfter(6);
        PlantillaPdf.agregar(doc, letras);
    }

    private static void fila(PdfPTable t, String etiqueta, String valor, Font fuente) {
        PdfPCell e = new PdfPCell(new Phrase(etiqueta, fuente));
        e.setBorder(Rectangle.NO_BORDER);
        e.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(e);
        PdfPCell v = derecha(valor, fuente);
        v.setBorder(Rectangle.BOTTOM);
        t.addCell(v);
    }

    private static void cronograma(Document doc, List<CuotaDto> cuotas) {
        if (cuotas.isEmpty()) {
            return;
        }
        PlantillaPdf.agregar(doc, subtitulo("Cronograma de pagos"));
        PdfPTable t = new PdfPTable(new float[]{1.2f, 1.4f, 1.3f});
        t.setWidthPercentage(55);
        t.setHorizontalAlignment(Element.ALIGN_LEFT);
        t.setHeaderRows(1);
        for (String c : List.of("Cuota", "Vencimiento", "Monto S/")) {
            PdfPCell h = new PdfPCell(new Phrase(c, PEQUENA_NEGRITA));
            h.setBackgroundColor(FONDO_TITULO);
            t.addCell(h);
        }
        for (CuotaDto c : cuotas) {
            t.addCell(new Phrase(c.etiqueta(), PEQUENA_NEGRA));
            t.addCell(new Phrase(FECHA.format(c.vencimiento()), PEQUENA_NEGRA));
            t.addCell(derecha(soles(c.monto()), PEQUENA_NEGRA));
        }
        PlantillaPdf.agregar(doc, t);
    }

    private static void firmas(Document doc, PacienteDetalle p, PlanDto plan) {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setSpacingBefore(48);
        boolean menor = p.esMenorDeEdad();
        String firmante = menor && p.apoderadoNombre() != null ? p.apoderadoNombre() + " (apoderado)" : p.nombreCompleto();
        t.addCell(firma("C.D. " + plan.odontologoNombre(), "COP " + plan.odontologoCop()));
        t.addCell(firma(firmante, "Paciente: conforme con el presupuesto"));
        PlantillaPdf.agregar(doc, t);
    }

    private static PdfPCell firma(String nombre, String detalle) {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.addElement(new Paragraph("______________________________", PlantillaPdf.NORMAL));
        c.addElement(new Paragraph(nombre, PlantillaPdf.NEGRITA));
        c.addElement(new Paragraph(detalle, PlantillaPdf.NORMAL));
        return c;
    }

    private static PdfPCell derecha(String texto, Font fuente) {
        PdfPCell c = new PdfPCell(new Phrase(texto, fuente));
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return c;
    }

    private static Paragraph subtitulo(String texto) {
        Paragraph p = new Paragraph(texto, PlantillaPdf.NEGRITA);
        p.setSpacingBefore(8);
        p.setSpacingAfter(4);
        return p;
    }

    /** Monto con separador de miles: 1,234.50. */
    static String soles(BigDecimal monto) {
        return new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.ROOT)).format(monto);
    }
}
