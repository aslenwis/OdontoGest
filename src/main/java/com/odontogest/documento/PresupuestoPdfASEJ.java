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
import com.odontogest.modelo.EstadoPlanItemASEJ;
import com.odontogest.servicio.dto.CuotaDtoASEJ;
import com.odontogest.servicio.dto.PacienteDetalleASEJ;
import com.odontogest.servicio.dto.PlanDtoASEJ;
import com.odontogest.servicio.dto.PlanItemDtoASEJ;
import com.odontogest.servicio.dto.PresupuestoDocumentoASEJ;
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

@Component
@RequiredArgsConstructor
public class PresupuestoPdfASEJ {

    private static final DateTimeFormatter FECHAASEJ = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color FONDO_TITULOASEJ = new Color(0xF1, 0xF5, 0xF9);
    private static final Color FONDO_FASEASEJ = new Color(0xE2, 0xE8, 0xF0);
    private static final Font PEQUENA_NEGRAASEJ = FontFactory.getFont(FontFactory.HELVETICA, 9);
    private static final Font PEQUENA_NEGRITAASEJ = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);

    private final PlantillaPdfASEJ plantillaASEJ;

    public byte[] generarASEJ(PresupuestoDocumentoASEJ dASEJ) {
        PlanDtoASEJ planASEJ = dASEJ.planASEJ();
        return plantillaASEJ.generarASEJ(dASEJ.clinicaASEJ(), "PRESUPUESTO DE TRATAMIENTO ODONTOLÓGICO", docASEJ -> {
            datosASEJ(docASEJ, dASEJ.pacienteASEJ(), planASEJ);
            procedimientosASEJ(docASEJ, planASEJ);
            totalesASEJ(docASEJ, planASEJ);
            cronogramaASEJ(docASEJ, planASEJ.cuotasASEJ());
            if (planASEJ.observacionesASEJ() != null) {
                PlantillaPdfASEJ.agregarASEJ(docASEJ, subtituloASEJ("Observaciones"));
                PlantillaPdfASEJ.agregarASEJ(docASEJ, PlantillaPdfASEJ.parrafoASEJ(planASEJ.observacionesASEJ(), PlantillaPdfASEJ.NORMALASEJ));
            }
            PlantillaPdfASEJ.agregarASEJ(docASEJ, PlantillaPdfASEJ.parrafoASEJ("Precios en soles. El presupuesto es referencial: puede "
                    + "variar si el diagnóstico cambia durante el tratamiento.", PlantillaPdfASEJ.PEQUENAASEJ));
            firmasASEJ(docASEJ, dASEJ.pacienteASEJ(), planASEJ);
        });
    }

    private static void datosASEJ(Document docASEJ, PacienteDetalleASEJ pASEJ, PlanDtoASEJ planASEJ) {
        PdfPTable tASEJ = new PdfPTable(new float[]{1.2f, 2.6f, 1.2f, 2f});
        tASEJ.setWidthPercentage(100);
        tASEJ.setSpacingAfter(8);
        celdasASEJ(tASEJ, "Paciente", pASEJ.nombreCompletoASEJ(), "Historia", pASEJ.numeroHistoriaASEJ());
        celdasASEJ(tASEJ, "Documento", pASEJ.documentoASEJ(), "Edad", pASEJ.edadASEJ() + (pASEJ.edadASEJ() == 1 ? " año" : " años"));
        celdasASEJ(tASEJ, "Plan", "N° " + planASEJ.idASEJ() + " · " + planASEJ.estadoASEJ().etiquetaASEJ(), "Fecha", FECHAASEJ.format(planASEJ.fechaASEJ()));
        celdasASEJ(tASEJ, "Odontólogo", "C.D. " + planASEJ.odontologoNombreASEJ(), "COP", planASEJ.odontologoCopASEJ());
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

    private static void procedimientosASEJ(Document docASEJ, PlanDtoASEJ planASEJ) {
        List<PlanItemDtoASEJ> itemsASEJ = planASEJ.itemsASEJ().stream().filter(iASEJ -> iASEJ.estadoASEJ() != EstadoPlanItemASEJ.CANCELADO).toList();
        PlantillaPdfASEJ.agregarASEJ(docASEJ, subtituloASEJ("Procedimientos (" + itemsASEJ.size() + ")"));
        if (itemsASEJ.isEmpty()) {
            PlantillaPdfASEJ.agregarASEJ(docASEJ, new Paragraph("El plan aún no tiene procedimientos.", PlantillaPdfASEJ.NORMALASEJ));
            return;
        }
        PdfPTable tASEJ = new PdfPTable(new float[]{0.8f, 0.8f, 3.6f, 0.7f, 1.2f, 1.1f, 1.2f});
        tASEJ.setWidthPercentage(100);
        tASEJ.setHeaderRows(1);
        for (String cASEJ : List.of("Pieza", "Sup.", "Procedimiento", "Cant.", "P. unit.", "Desc.", "Total")) {
            PdfPCell hASEJ = new PdfPCell(new Phrase(cASEJ, PEQUENA_NEGRITAASEJ));
            hASEJ.setBackgroundColor(FONDO_TITULOASEJ);
            hASEJ.setHorizontalAlignment(cASEJ.startsWith("P.") || cASEJ.equals("Desc.") || cASEJ.equals("Total") || cASEJ.equals("Cant.")
                    ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
            tASEJ.addCell(hASEJ);
        }
        Map<Integer, List<PlanItemDtoASEJ>> porFaseASEJ = itemsASEJ.stream()
                .collect(Collectors.groupingBy(PlanItemDtoASEJ::faseASEJ, TreeMap::new, Collectors.toList()));
        porFaseASEJ.forEach((faseASEJ, listaASEJ) -> {
            BigDecimal subtotalASEJ = listaASEJ.stream().map(PlanItemDtoASEJ::totalASEJ).reduce(BigDecimal.ZERO, BigDecimal::add);
            PdfPCell tituloASEJ = new PdfPCell(new Phrase(PlanItemDtoASEJ.nombreFaseASEJ(faseASEJ), PEQUENA_NEGRITAASEJ));
            tituloASEJ.setColspan(6);
            tituloASEJ.setBackgroundColor(FONDO_FASEASEJ);
            tASEJ.addCell(tituloASEJ);
            PdfPCell subASEJ = derechaASEJ(solesASEJ(subtotalASEJ), PEQUENA_NEGRITAASEJ);
            subASEJ.setBackgroundColor(FONDO_FASEASEJ);
            tASEJ.addCell(subASEJ);
            for (PlanItemDtoASEJ iASEJ : listaASEJ) {
                tASEJ.addCell(new Phrase(iASEJ.piezaASEJ() == null ? "—" : String.valueOf(iASEJ.piezaASEJ()), PEQUENA_NEGRAASEJ));
                tASEJ.addCell(new Phrase(iASEJ.superficiesTextoASEJ().isEmpty() ? "—" : iASEJ.superficiesTextoASEJ(), PEQUENA_NEGRAASEJ));
                tASEJ.addCell(new Phrase(iASEJ.procedimientoCodigoASEJ() + " · " + iASEJ.procedimientoNombreASEJ()
                        + (iASEJ.estadoASEJ() == EstadoPlanItemASEJ.REALIZADO ? " (realizado)" : ""), PEQUENA_NEGRAASEJ));
                tASEJ.addCell(derechaASEJ(String.valueOf(iASEJ.cantidadASEJ()), PEQUENA_NEGRAASEJ));
                tASEJ.addCell(derechaASEJ(solesASEJ(iASEJ.precioUnitASEJ()), PEQUENA_NEGRAASEJ));
                tASEJ.addCell(derechaASEJ(iASEJ.descuentoASEJ().signum() == 0 ? "—" : solesASEJ(iASEJ.descuentoASEJ()), PEQUENA_NEGRAASEJ));
                tASEJ.addCell(derechaASEJ(solesASEJ(iASEJ.totalASEJ()), PEQUENA_NEGRAASEJ));
            }
        });
        PlantillaPdfASEJ.agregarASEJ(docASEJ, tASEJ);
    }

    private static void totalesASEJ(Document docASEJ, PlanDtoASEJ planASEJ) {
        PdfPTable tASEJ = new PdfPTable(new float[]{2f, 1.3f});
        tASEJ.setWidthPercentage(40);
        tASEJ.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tASEJ.setSpacingBefore(6);
        filaASEJ(tASEJ, "Subtotal", solesASEJ(planASEJ.subtotalASEJ()), PlantillaPdfASEJ.NORMALASEJ);
        filaASEJ(tASEJ, "Descuento", planASEJ.descuentoASEJ().signum() == 0 ? "—" : "− " + solesASEJ(planASEJ.descuentoASEJ()), PlantillaPdfASEJ.NORMALASEJ);
        filaASEJ(tASEJ, "TOTAL S/", solesASEJ(planASEJ.totalASEJ()), PlantillaPdfASEJ.NEGRITAASEJ);
        PlantillaPdfASEJ.agregarASEJ(docASEJ, tASEJ);
        Paragraph letrasASEJ = new Paragraph("Son: " + NumeroALetrasASEJ.solesASEJ(planASEJ.totalCentASEJ()),
                PlantillaPdfASEJ.NEGRITAASEJ);
        letrasASEJ.setAlignment(Element.ALIGN_RIGHT);
        letrasASEJ.setSpacingAfter(6);
        PlantillaPdfASEJ.agregarASEJ(docASEJ, letrasASEJ);
    }

    private static void filaASEJ(PdfPTable tASEJ, String etiquetaASEJ, String valorASEJ, Font fuenteASEJ) {
        PdfPCell eASEJ = new PdfPCell(new Phrase(etiquetaASEJ, fuenteASEJ));
        eASEJ.setBorder(Rectangle.NO_BORDER);
        eASEJ.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tASEJ.addCell(eASEJ);
        PdfPCell vASEJ = derechaASEJ(valorASEJ, fuenteASEJ);
        vASEJ.setBorder(Rectangle.BOTTOM);
        tASEJ.addCell(vASEJ);
    }

    private static void cronogramaASEJ(Document docASEJ, List<CuotaDtoASEJ> cuotasASEJ) {
        if (cuotasASEJ.isEmpty()) {
            return;
        }
        PlantillaPdfASEJ.agregarASEJ(docASEJ, subtituloASEJ("Cronograma de pagos"));
        PdfPTable tASEJ = new PdfPTable(new float[]{1.2f, 1.4f, 1.3f});
        tASEJ.setWidthPercentage(55);
        tASEJ.setHorizontalAlignment(Element.ALIGN_LEFT);
        tASEJ.setHeaderRows(1);
        for (String cASEJ : List.of("Cuota", "Vencimiento", "Monto S/")) {
            PdfPCell hASEJ = new PdfPCell(new Phrase(cASEJ, PEQUENA_NEGRITAASEJ));
            hASEJ.setBackgroundColor(FONDO_TITULOASEJ);
            tASEJ.addCell(hASEJ);
        }
        for (CuotaDtoASEJ cASEJ : cuotasASEJ) {
            tASEJ.addCell(new Phrase(cASEJ.etiquetaASEJ(), PEQUENA_NEGRAASEJ));
            tASEJ.addCell(new Phrase(FECHAASEJ.format(cASEJ.vencimientoASEJ()), PEQUENA_NEGRAASEJ));
            tASEJ.addCell(derechaASEJ(solesASEJ(cASEJ.montoASEJ()), PEQUENA_NEGRAASEJ));
        }
        PlantillaPdfASEJ.agregarASEJ(docASEJ, tASEJ);
    }

    private static void firmasASEJ(Document docASEJ, PacienteDetalleASEJ pASEJ, PlanDtoASEJ planASEJ) {
        PdfPTable tASEJ = new PdfPTable(2);
        tASEJ.setWidthPercentage(100);
        tASEJ.setSpacingBefore(48);
        boolean menorASEJ = pASEJ.esMenorDeEdadASEJ();
        String firmanteASEJ = menorASEJ && pASEJ.apoderadoNombreASEJ() != null ? pASEJ.apoderadoNombreASEJ() + " (apoderado)" : pASEJ.nombreCompletoASEJ();
        tASEJ.addCell(firmaASEJ("C.D. " + planASEJ.odontologoNombreASEJ(), "COP " + planASEJ.odontologoCopASEJ()));
        tASEJ.addCell(firmaASEJ(firmanteASEJ, "Paciente: conforme con el presupuesto"));
        PlantillaPdfASEJ.agregarASEJ(docASEJ, tASEJ);
    }

    private static PdfPCell firmaASEJ(String nombreASEJ, String detalleASEJ) {
        PdfPCell cASEJ = new PdfPCell();
        cASEJ.setBorder(Rectangle.NO_BORDER);
        cASEJ.addElement(new Paragraph("______________________________", PlantillaPdfASEJ.NORMALASEJ));
        cASEJ.addElement(new Paragraph(nombreASEJ, PlantillaPdfASEJ.NEGRITAASEJ));
        cASEJ.addElement(new Paragraph(detalleASEJ, PlantillaPdfASEJ.NORMALASEJ));
        return cASEJ;
    }

    private static PdfPCell derechaASEJ(String textoASEJ, Font fuenteASEJ) {
        PdfPCell cASEJ = new PdfPCell(new Phrase(textoASEJ, fuenteASEJ));
        cASEJ.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cASEJ;
    }

    private static Paragraph subtituloASEJ(String textoASEJ) {
        Paragraph pASEJ = new Paragraph(textoASEJ, PlantillaPdfASEJ.NEGRITAASEJ);
        pASEJ.setSpacingBefore(8);
        pASEJ.setSpacingAfter(4);
        return pASEJ;
    }

    static String solesASEJ(BigDecimal montoASEJ) {
        return new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.ROOT)).format(montoASEJ);
    }
}
