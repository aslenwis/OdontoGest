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
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.odontogest.dominio.odontograma.ColorHallazgo;
import com.odontogest.dominio.odontograma.PiezaDental;
import com.odontogest.dominio.odontograma.ResultadoIndice;
import com.odontogest.dominio.odontograma.Superficie;
import com.odontogest.servicio.dto.FichaOdontologica;
import com.odontogest.servicio.dto.HallazgoDto;
import com.odontogest.servicio.dto.OdontogramaVista;
import com.odontogest.servicio.dto.PacienteDetalle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Ficha odontológica en PDF (RF-ODO-10): datos del paciente, gráfico del odontograma, hallazgos vigentes,
 * especificaciones, observaciones, índices CPO-D/ceo-d, hash de cierre y firma del odontólogo con su COP.
 */
@Component
@RequiredArgsConstructor
public class FichaOdontologicaPdf {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Color AZUL = new Color(0x1D, 0x4E, 0xD8);
    private static final Color ROJO = new Color(0xDC, 0x26, 0x26);
    private static final Color FONDO_TITULO = new Color(0xF1, 0xF5, 0xF9);

    private final PlantillaPdf plantilla;

    public byte[] generar(FichaOdontologica f) {
        return plantilla.generar(f.clinica(), "FICHA ODONTOLÓGICA", doc -> {
            datosPaciente(doc, f.paciente(), f.vista());
            imagen(doc, f.imagenPng());
            hallazgos(doc, f.vista());
            textoLibre(doc, "Especificaciones", f.vista().especificaciones());
            textoLibre(doc, "Observaciones", f.vista().observaciones());
            indices(doc, f.vista());
            firma(doc, f);
        });
    }

    private static void datosPaciente(Document doc, PacienteDetalle p, OdontogramaVista v) {
        PdfPTable t = new PdfPTable(new float[]{1.2f, 2.6f, 1.2f, 2f});
        t.setWidthPercentage(100);
        t.setSpacingAfter(8);
        celdas(t, "Paciente", p.nombreCompleto(), "Historia", p.numeroHistoria());
        celdas(t, "Documento", p.documento(), "Edad", p.edad() + (p.edad() == 1 ? " año" : " años"));
        String estado = v.borrador() ? "Borrador (sin firmar)" : "Cerrado el " + FECHA_HORA.format(v.cerradoEn());
        celdas(t, "Odontograma", v.tipo().etiqueta + " · " + estado, "Dentición", v.denticion().name().toLowerCase());
        if (v.hasta() != null) {
            celdas(t, "Estado al", FECHA_HORA.format(v.hasta()), "Emitido", FECHA.format(LocalDateTime.now()));
        }
        if (p.tieneAlergias()) {
            PdfPCell e = new PdfPCell(new Phrase("Alergias", PlantillaPdf.NEGRITA));
            e.setBackgroundColor(FONDO_TITULO);
            t.addCell(e);
            PdfPCell a = new PdfPCell(new Phrase(p.alergias().replaceAll("\\s+", " "), fuente(true, ROJO)));
            a.setColspan(3);
            t.addCell(a);
        }
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

    private static void imagen(Document doc, byte[] png) {
        if (png == null || png.length == 0) {
            return;
        }
        try {
            Image img = Image.getInstance(png);
            img.scaleToFit(doc.right() - doc.left(), 300);
            img.setAlignment(Element.ALIGN_CENTER);
            PlantillaPdf.agregar(doc, img);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo insertar el gráfico del odontograma", e);
        }
    }

    private static void hallazgos(Document doc, OdontogramaVista v) {
        List<HallazgoDto> vigentes = v.historial().stream()
                .filter(h -> h.vigente() && !h.anulado())
                .sorted(Comparator.comparingInt((HallazgoDto h) -> PiezaDental.de(h.pieza()).ordenDibujo()))
                .toList();
        PlantillaPdf.agregar(doc, subtitulo("Hallazgos vigentes (" + vigentes.size() + ")"));
        if (vigentes.isEmpty()) {
            PlantillaPdf.agregar(doc, new Paragraph("Sin hallazgos registrados.", PlantillaPdf.NORMAL));
            return;
        }
        PdfPTable t = new PdfPTable(new float[]{1.1f, 3.4f, 1.6f, 1f, 1.2f});
        t.setWidthPercentage(100);
        t.setHeaderRows(1);
        for (String c : List.of("Pieza", "Hallazgo", "Superficies", "Sigla", "Estado")) {
            PdfPCell h = new PdfPCell(new Phrase(c, PlantillaPdf.NEGRITA));
            h.setBackgroundColor(FONDO_TITULO);
            t.addCell(h);
        }
        for (HallazgoDto h : vigentes) {
            Color color = h.color() == ColorHallazgo.ROJO ? ROJO : AZUL;
            t.addCell(new Phrase(h.piezaFin() == null ? String.valueOf(h.pieza()) : h.pieza() + "–" + h.piezaFin(), PlantillaPdf.NORMAL));
            t.addCell(new Phrase(h.nombreTipo() + (h.nota() == null ? "" : " (" + h.nota() + ")"), PlantillaPdf.NORMAL));
            t.addCell(new Phrase(superficies(h), PlantillaPdf.NORMAL));
            t.addCell(new Phrase(h.sigla() == null ? "" : h.sigla(),
                    fuente(true, color)));
            t.addCell(new Phrase(h.estado() == null ? (h.color() == ColorHallazgo.ROJO ? "Rojo" : "Azul")
                    : h.estado().name().charAt(0) + h.estado().name().substring(1).toLowerCase(),
                    fuente(false, color)));
        }
        PlantillaPdf.agregar(doc, t);
    }

    private static String superficies(HallazgoDto h) {
        if (h.superficies().isEmpty()) {
            return "—";
        }
        PiezaDental p = PiezaDental.de(h.pieza());
        return h.superficies().stream().sorted().map((Superficie s) -> s.etiqueta(p)).collect(Collectors.joining(""));
    }

    private static void textoLibre(Document doc, String titulo, String texto) {
        PlantillaPdf.agregar(doc, subtitulo(titulo));
        PlantillaPdf.agregar(doc, PlantillaPdf.parrafo(texto == null || texto.isBlank() ? "—" : texto, PlantillaPdf.NORMAL));
    }

    private static void indices(Document doc, OdontogramaVista v) {
        PlantillaPdf.agregar(doc, subtitulo("Índices epidemiológicos"));
        ResultadoIndice c = v.indices().cpod();
        ResultadoIndice e = v.indices().ceod();
        PlantillaPdf.agregar(doc, PlantillaPdf.parrafo(String.format(
                "CPO-D: C=%d P=%d O=%d, total %d (%s)%s.   ceo-d: c=%d e=%d o=%d, total %d. Nivel de severidad según la referencia OMS a los 12 años.",
                c.c(), c.p(), c.o(), c.total(), c.nivel().name().replace('_', ' ').toLowerCase(),
                v.indices().excluyeTercerasMolares() ? ", sin terceras molares" : "",
                e.c(), e.p(), e.o(), e.total()), PlantillaPdf.NORMAL));
        if (v.hashCierre() != null) {
            PlantillaPdf.agregar(doc, new Paragraph("Hash de integridad (SHA-256): " + v.hashCierre(), PlantillaPdf.PEQUENA));
        }
    }

    private static void firma(Document doc, FichaOdontologica f) {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(45);
        t.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.setSpacingBefore(40);
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.addElement(new Paragraph("______________________________", PlantillaPdf.NORMAL));
        c.addElement(new Paragraph("C.D. " + f.odontologoNombre(), PlantillaPdf.NEGRITA));
        c.addElement(new Paragraph("COP " + f.odontologoCop(), PlantillaPdf.NORMAL));
        t.addCell(c);
        PlantillaPdf.agregar(doc, t);
    }

    private static Font fuente(boolean negrita, Color color) {
        return FontFactory.getFont(negrita ? FontFactory.HELVETICA_BOLD : FontFactory.HELVETICA, 10, color);
    }

    private static Paragraph subtitulo(String texto) {
        Paragraph p = new Paragraph(texto, PlantillaPdf.NEGRITA);
        p.setSpacingBefore(8);
        p.setSpacingAfter(4);
        return p;
    }
}
