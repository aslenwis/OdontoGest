package com.odontogest.documento;

import com.odontogest.modelo.MedioPagoASEJ;
import com.odontogest.servicio.dto.AuditoriaDtoASEJ;
import com.odontogest.servicio.dto.DatosClinicaASEJ;
import com.odontogest.servicio.dto.GrupoIndicesDtoASEJ;
import com.odontogest.servicio.dto.IngresoDiaDtoASEJ;
import com.odontogest.servicio.dto.MontoPeriodoDtoASEJ;
import com.odontogest.servicio.dto.MorosoDtoASEJ;
import com.odontogest.servicio.dto.PagoReporteDtoASEJ;
import com.odontogest.servicio.dto.ProduccionFilaDtoASEJ;
import com.odontogest.servicio.dto.ReporteEpidemiologicoDtoASEJ;
import com.odontogest.servicio.dto.ReporteIngresosDtoASEJ;
import com.odontogest.servicio.dto.ReporteMorosidadDtoASEJ;
import com.odontogest.servicio.dto.ReporteProduccionDtoASEJ;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Component
public class ReporteExcelASEJ {

    private static final DateTimeFormatter FECHAASEJ = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORAASEJ = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int FILA_CABECERAASEJ = 4;
    private static final int ANCHO_MAXIMOASEJ = 60 * 256;

    enum TipoASEJ { TEXTO, FECHA, FECHA_HORA, MONEDA, ENTERO, DECIMAL }

    record ColumnaASEJ<T>(String tituloASEJ, TipoASEJ tipoASEJ, Function<T, Object> valorASEJ, boolean sumarASEJ) {

        static <T> ColumnaASEJ<T> deASEJ(String tituloASEJ, TipoASEJ tipoASEJ, Function<T, Object> valorASEJ) {
            return new ColumnaASEJ<>(tituloASEJ, tipoASEJ, valorASEJ, false);
        }

        static <T> ColumnaASEJ<T> totalASEJ(String tituloASEJ, TipoASEJ tipoASEJ, Function<T, Object> valorASEJ) {
            return new ColumnaASEJ<>(tituloASEJ, tipoASEJ, valorASEJ, true);
        }
    }

    public byte[] ingresosASEJ(ReporteIngresosDtoASEJ rASEJ, DatosClinicaASEJ clinicaASEJ) {
        return libroASEJ(libroASEJ -> {
            EstilosASEJ eASEJ = new EstilosASEJ(libroASEJ);
            String periodoASEJ = periodoASEJ(rASEJ.desdeASEJ(), rASEJ.hastaASEJ());
            List<ColumnaASEJ<IngresoDiaDtoASEJ>> resumenASEJ = new ArrayList<>();
            resumenASEJ.add(ColumnaASEJ.deASEJ("Fecha", TipoASEJ.FECHA, IngresoDiaDtoASEJ::diaASEJ));
            resumenASEJ.add(ColumnaASEJ.totalASEJ("Pagos", TipoASEJ.ENTERO, IngresoDiaDtoASEJ::pagosASEJ));
            for (MedioPagoASEJ mASEJ : MedioPagoASEJ.values()) {
                resumenASEJ.add(ColumnaASEJ.totalASEJ(mASEJ.etiquetaASEJ(), TipoASEJ.MONEDA, dASEJ -> dASEJ.montoASEJ(mASEJ)));
            }
            resumenASEJ.add(ColumnaASEJ.totalASEJ("Total", TipoASEJ.MONEDA, IngresoDiaDtoASEJ::totalASEJ));
            hojaASEJ(libroASEJ, eASEJ, "Ingresos por día", clinicaASEJ, "Reporte de ingresos por día y medio de pago", periodoASEJ,
                    resumenASEJ, rASEJ.diasASEJ());
            hojaASEJ(libroASEJ, eASEJ, "Detalle de pagos", clinicaASEJ, "Detalle de pagos (sin anulados)", periodoASEJ, List.of(
                    ColumnaASEJ.deASEJ("Fecha y hora", TipoASEJ.FECHA_HORA, PagoReporteDtoASEJ::fechaASEJ),
                    ColumnaASEJ.deASEJ("Recibo", TipoASEJ.TEXTO, PagoReporteDtoASEJ::numeroReciboASEJ),
                    ColumnaASEJ.deASEJ("Historia", TipoASEJ.TEXTO, PagoReporteDtoASEJ::numeroHistoriaASEJ),
                    ColumnaASEJ.deASEJ("Paciente", TipoASEJ.TEXTO, PagoReporteDtoASEJ::pacienteASEJ),
                    ColumnaASEJ.deASEJ("Medio de pago", TipoASEJ.TEXTO, pASEJ -> pASEJ.medioASEJ().etiquetaASEJ()),
                    ColumnaASEJ.deASEJ("Referencia", TipoASEJ.TEXTO, PagoReporteDtoASEJ::referenciaASEJ),
                    ColumnaASEJ.totalASEJ("Monto (S/)", TipoASEJ.MONEDA, PagoReporteDtoASEJ::montoASEJ)), rASEJ.detalleASEJ());
        });
    }

    public byte[] produccionASEJ(ReporteProduccionDtoASEJ rASEJ, DatosClinicaASEJ clinicaASEJ) {
        return libroASEJ(libroASEJ -> {
            EstilosASEJ eASEJ = new EstilosASEJ(libroASEJ);
            String periodoASEJ = periodoASEJ(rASEJ.desdeASEJ(), rASEJ.hastaASEJ());
            hojaASEJ(libroASEJ, eASEJ, "Producción", clinicaASEJ, "Producción por odontólogo (procedimientos realizados)", periodoASEJ,
                    List.of(ColumnaASEJ.deASEJ("Odontólogo", TipoASEJ.TEXTO, ProduccionFilaDtoASEJ::odontologoASEJ),
                            ColumnaASEJ.deASEJ("Código", TipoASEJ.TEXTO, ProduccionFilaDtoASEJ::codigoASEJ),
                            ColumnaASEJ.deASEJ("Procedimiento", TipoASEJ.TEXTO, ProduccionFilaDtoASEJ::procedimientoASEJ),
                            ColumnaASEJ.totalASEJ("Cantidad", TipoASEJ.ENTERO, ProduccionFilaDtoASEJ::cantidadASEJ),
                            ColumnaASEJ.totalASEJ("Monto (S/)", TipoASEJ.MONEDA, ProduccionFilaDtoASEJ::montoASEJ)), rASEJ.filasASEJ());
            hojaASEJ(libroASEJ, eASEJ, "Por odontólogo", clinicaASEJ, "Total por odontólogo", periodoASEJ,
                    List.of(ColumnaASEJ.deASEJ("Odontólogo", TipoASEJ.TEXTO, MontoPeriodoDtoASEJ::etiquetaASEJ),
                            ColumnaASEJ.totalASEJ("Monto (S/)", TipoASEJ.MONEDA, MontoPeriodoDtoASEJ::montoASEJ)), rASEJ.porOdontologoASEJ());
        });
    }

    public byte[] morosidadASEJ(ReporteMorosidadDtoASEJ rASEJ, DatosClinicaASEJ clinicaASEJ) {
        return libroASEJ(libroASEJ -> hojaASEJ(libroASEJ, new EstilosASEJ(libroASEJ), "Morosidad", clinicaASEJ, "Reporte de morosidad",
                "Cuotas vencidas al " + FECHAASEJ.format(rASEJ.fechaASEJ()), List.of(
                        ColumnaASEJ.deASEJ("Historia", TipoASEJ.TEXTO, MorosoDtoASEJ::numeroHistoriaASEJ),
                        ColumnaASEJ.deASEJ("Paciente", TipoASEJ.TEXTO, MorosoDtoASEJ::pacienteASEJ),
                        ColumnaASEJ.deASEJ("Teléfono", TipoASEJ.TEXTO, MorosoDtoASEJ::telefonoASEJ),
                        ColumnaASEJ.totalASEJ("Cuotas vencidas", TipoASEJ.ENTERO, MorosoDtoASEJ::cuotasVencidasASEJ),
                        ColumnaASEJ.deASEJ("Vencimiento más antiguo", TipoASEJ.FECHA, MorosoDtoASEJ::vencimientoMasAntiguoASEJ),
                        ColumnaASEJ.deASEJ("Días de atraso", TipoASEJ.ENTERO, MorosoDtoASEJ::diasAtrasoASEJ),
                        ColumnaASEJ.totalASEJ("Saldo vencido (S/)", TipoASEJ.MONEDA, MorosoDtoASEJ::saldoVencidoASEJ)), rASEJ.pacientesASEJ()));
    }

    public byte[] epidemiologicoASEJ(ReporteEpidemiologicoDtoASEJ rASEJ, DatosClinicaASEJ clinicaASEJ) {
        return libroASEJ(libroASEJ -> {
            XSSFSheet hojaASEJ = hojaASEJ(libroASEJ, new EstilosASEJ(libroASEJ), "Epidemiológico", clinicaASEJ,
                    "Reporte epidemiológico: CPO-D y ceo-d promedio por grupo de edad",
                    periodoASEJ(rASEJ.desdeASEJ(), rASEJ.hastaASEJ()) + " · " + rASEJ.pacientesASEJ() + " paciente(s) examinado(s)", List.of(
                            ColumnaASEJ.deASEJ("Grupo de edad", TipoASEJ.TEXTO, GrupoIndicesDtoASEJ::grupoASEJ),
                            ColumnaASEJ.totalASEJ("Pacientes", TipoASEJ.ENTERO, GrupoIndicesDtoASEJ::pacientesASEJ),
                            ColumnaASEJ.deASEJ("CPO-D", TipoASEJ.DECIMAL, GrupoIndicesDtoASEJ::cpodASEJ),
                            ColumnaASEJ.deASEJ("C (cariados)", TipoASEJ.DECIMAL, GrupoIndicesDtoASEJ::cASEJ),
                            ColumnaASEJ.deASEJ("P (perdidos)", TipoASEJ.DECIMAL, GrupoIndicesDtoASEJ::pASEJ),
                            ColumnaASEJ.deASEJ("O (obturados)", TipoASEJ.DECIMAL, GrupoIndicesDtoASEJ::oASEJ),
                            ColumnaASEJ.deASEJ("ceo-d", TipoASEJ.DECIMAL, GrupoIndicesDtoASEJ::ceodASEJ),
                            ColumnaASEJ.deASEJ("Nivel OMS (CPO-D)", TipoASEJ.TEXTO, GrupoIndicesDtoASEJ::nivelCpodASEJ)), rASEJ.gruposASEJ());
            int filaASEJ = hojaASEJ.getLastRowNum() + 2;
            hojaASEJ.createRow(filaASEJ).createCell(0).setCellValue("Índices calculados con lo firmado del último odontograma"
                    + " de cada paciente; edad a la fecha de ese examen."
                    + (rASEJ.excluyeTercerasMolaresASEJ() ? " El CPO-D excluye las terceras molares." : ""));
        });
    }

    public byte[] auditoriaASEJ(List<AuditoriaDtoASEJ> filasASEJ, LocalDate desdeASEJ, LocalDate hastaASEJ, DatosClinicaASEJ clinicaASEJ) {
        return libroASEJ(libroASEJ -> hojaASEJ(libroASEJ, new EstilosASEJ(libroASEJ), "Auditoría", clinicaASEJ, "Registro de auditoría",
                periodoASEJ(desdeASEJ, hastaASEJ), List.of(
                        ColumnaASEJ.deASEJ("Fecha y hora", TipoASEJ.FECHA_HORA, AuditoriaDtoASEJ::fechaASEJ),
                        ColumnaASEJ.deASEJ("Usuario", TipoASEJ.TEXTO, AuditoriaDtoASEJ::usuarioASEJ),
                        ColumnaASEJ.deASEJ("Acción", TipoASEJ.TEXTO, AuditoriaDtoASEJ::accionASEJ),
                        ColumnaASEJ.deASEJ("Entidad", TipoASEJ.TEXTO, AuditoriaDtoASEJ::entidadASEJ),
                        ColumnaASEJ.deASEJ("Id", TipoASEJ.ENTERO, AuditoriaDtoASEJ::entidadIdASEJ),
                        ColumnaASEJ.deASEJ("Detalle", TipoASEJ.TEXTO, AuditoriaDtoASEJ::detalleASEJ)), filasASEJ));
    }

    private interface ContenidoASEJ {
        void escribirASEJ(XSSFWorkbook libroASEJ);
    }

    private static byte[] libroASEJ(ContenidoASEJ contenidoASEJ) {
        try (XSSFWorkbook libroASEJ = new XSSFWorkbook(); ByteArrayOutputStream salidaASEJ = new ByteArrayOutputStream()) {
            contenidoASEJ.escribirASEJ(libroASEJ);
            libroASEJ.getCreationHelper().createFormulaEvaluator().evaluateAll();
            libroASEJ.write(salidaASEJ);
            return salidaASEJ.toByteArray();
        } catch (IOException eASEJ) {
            throw new UncheckedIOException("No se pudo generar el Excel", eASEJ);
        }
    }

    static <T> XSSFSheet hojaASEJ(XSSFWorkbook libroASEJ, EstilosASEJ eASEJ, String nombreASEJ, DatosClinicaASEJ clinicaASEJ, String tituloASEJ,
                              String subtituloASEJ, List<ColumnaASEJ<T>> columnasASEJ, List<T> filasASEJ) {
        XSSFSheet hojaASEJ = libroASEJ.createSheet(nombreASEJ);
        celdaASEJ(hojaASEJ.createRow(0), 0, clinicaASEJ.nombreASEJ(), eASEJ.clinicaASEJ);
        celdaASEJ(hojaASEJ.createRow(1), 0, tituloASEJ, eASEJ.tituloASEJ);
        celdaASEJ(hojaASEJ.createRow(2), 0, subtituloASEJ + " · Generado el " + FECHA_HORAASEJ.format(LocalDateTime.now()), eASEJ.notaASEJ);

        Row cabeceraASEJ = hojaASEJ.createRow(FILA_CABECERAASEJ);
        for (int cASEJ = 0; cASEJ < columnasASEJ.size(); cASEJ++) {
            celdaASEJ(cabeceraASEJ, cASEJ, columnasASEJ.get(cASEJ).tituloASEJ(), eASEJ.cabeceraASEJ);
        }
        int rASEJ = FILA_CABECERAASEJ + 1;
        for (T filaASEJ : filasASEJ) {
            Row rowASEJ = hojaASEJ.createRow(rASEJ++);
            for (int cASEJ = 0; cASEJ < columnasASEJ.size(); cASEJ++) {
                valorASEJ(rowASEJ.createCell(cASEJ), columnasASEJ.get(cASEJ).tipoASEJ(), columnasASEJ.get(cASEJ).valorASEJ().apply(filaASEJ), eASEJ);
            }
        }
        if (columnasASEJ.stream().anyMatch(ColumnaASEJ::sumarASEJ)) {
            Row totalASEJ = hojaASEJ.createRow(rASEJ);
            celdaASEJ(totalASEJ, 0, "Total", eASEJ.totalASEJ);
            for (int cASEJ = 1; cASEJ < columnasASEJ.size(); cASEJ++) {
                Cell cellASEJ = totalASEJ.createCell(cASEJ);
                ColumnaASEJ<T> colASEJ = columnasASEJ.get(cASEJ);
                cellASEJ.setCellStyle(colASEJ.tipoASEJ() == TipoASEJ.MONEDA ? eASEJ.totalMonedaASEJ : eASEJ.totalASEJ);
                if (colASEJ.sumarASEJ()) {
                    String letraASEJ = CellReference.convertNumToColString(cASEJ);
                    cellASEJ.setCellFormula(filasASEJ.isEmpty() ? "0"
                            : "SUM(" + letraASEJ + (FILA_CABECERAASEJ + 2) + ":" + letraASEJ + (FILA_CABECERAASEJ + 1 + filasASEJ.size()) + ")");
                }
            }
        }
        if (columnasASEJ.size() > 1) {
            for (int fASEJ = 0; fASEJ < FILA_CABECERAASEJ - 1; fASEJ++) {
                hojaASEJ.addMergedRegion(new CellRangeAddress(fASEJ, fASEJ, 0, columnasASEJ.size() - 1));
            }
        }
        hojaASEJ.createFreezePane(0, FILA_CABECERAASEJ + 1);
        hojaASEJ.getPrintSetup().setLandscape(true);
        hojaASEJ.getPrintSetup().setPaperSize(PrintSetup.A4_PAPERSIZE);
        hojaASEJ.setFitToPage(true);
        hojaASEJ.getPrintSetup().setFitWidth((short) 1);
        hojaASEJ.getPrintSetup().setFitHeight((short) 0);
        hojaASEJ.setRepeatingRows(new CellRangeAddress(FILA_CABECERAASEJ, FILA_CABECERAASEJ, -1, -1));
        if (!filasASEJ.isEmpty()) {
            hojaASEJ.setAutoFilter(new CellRangeAddress(FILA_CABECERAASEJ, FILA_CABECERAASEJ + filasASEJ.size(), 0, columnasASEJ.size() - 1));
        }
        for (int cASEJ = 0; cASEJ < columnasASEJ.size(); cASEJ++) {
            hojaASEJ.autoSizeColumn(cASEJ);
            hojaASEJ.setColumnWidth(cASEJ, Math.min(ANCHO_MAXIMOASEJ, Math.max(hojaASEJ.getColumnWidth(cASEJ) + 512, 10 * 256)));
        }
        return hojaASEJ;
    }

    private static void valorASEJ(Cell cellASEJ, TipoASEJ tipoASEJ, Object vASEJ, EstilosASEJ eASEJ) {
        if (vASEJ == null) {
            cellASEJ.setCellValue(tipoASEJ == TipoASEJ.TEXTO ? "" : "—");
            cellASEJ.setCellStyle(tipoASEJ == TipoASEJ.TEXTO ? eASEJ.textoASEJ : eASEJ.centradoASEJ);
            return;
        }
        switch (tipoASEJ) {
            case TEXTO -> {
                cellASEJ.setCellValue(vASEJ.toString());
                cellASEJ.setCellStyle(eASEJ.textoASEJ);
            }
            case FECHA -> {
                cellASEJ.setCellValue((LocalDate) vASEJ);
                cellASEJ.setCellStyle(eASEJ.fechaASEJ);
            }
            case FECHA_HORA -> {
                cellASEJ.setCellValue((LocalDateTime) vASEJ);
                cellASEJ.setCellStyle(eASEJ.fechaHoraASEJ);
            }
            case MONEDA -> {
                cellASEJ.setCellValue(((BigDecimal) vASEJ).doubleValue());
                cellASEJ.setCellStyle(eASEJ.monedaASEJ);
            }
            case ENTERO -> {
                cellASEJ.setCellValue(((Number) vASEJ).longValue());
                cellASEJ.setCellStyle(eASEJ.enteroASEJ);
            }
            case DECIMAL -> {
                cellASEJ.setCellValue(((Number) vASEJ).doubleValue());
                cellASEJ.setCellStyle(eASEJ.decimalASEJ);
            }
        }
    }

    private static void celdaASEJ(Row rowASEJ, int columnaASEJ, String textoASEJ, CellStyle estiloASEJ) {
        Cell cASEJ = rowASEJ.createCell(columnaASEJ);
        cASEJ.setCellValue(textoASEJ);
        cASEJ.setCellStyle(estiloASEJ);
    }

    private static String periodoASEJ(LocalDate desdeASEJ, LocalDate hastaASEJ) {
        return "Del " + FECHAASEJ.format(desdeASEJ) + " al " + FECHAASEJ.format(hastaASEJ);
    }

    static final class EstilosASEJ {
        final CellStyle clinicaASEJ;
        final CellStyle tituloASEJ;
        final CellStyle notaASEJ;
        final CellStyle cabeceraASEJ;
        final CellStyle textoASEJ;
        final CellStyle centradoASEJ;
        final CellStyle fechaASEJ;
        final CellStyle fechaHoraASEJ;
        final CellStyle monedaASEJ;
        final CellStyle enteroASEJ;
        final CellStyle decimalASEJ;
        final CellStyle totalASEJ;
        final CellStyle totalMonedaASEJ;

        EstilosASEJ(XSSFWorkbook libroASEJ) {
            clinicaASEJ = libroASEJ.createCellStyle();
            clinicaASEJ.setFont(fuenteASEJ(libroASEJ, 14, true, IndexedColors.BLACK));
            tituloASEJ = libroASEJ.createCellStyle();
            tituloASEJ.setFont(fuenteASEJ(libroASEJ, 12, true, IndexedColors.DARK_BLUE));
            notaASEJ = libroASEJ.createCellStyle();
            notaASEJ.setFont(fuenteASEJ(libroASEJ, 9, false, IndexedColors.GREY_50_PERCENT));

            cabeceraASEJ = libroASEJ.createCellStyle();
            cabeceraASEJ.setFont(fuenteASEJ(libroASEJ, 10, true, IndexedColors.WHITE));
            cabeceraASEJ.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            cabeceraASEJ.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cabeceraASEJ.setAlignment(HorizontalAlignment.CENTER);
            cabeceraASEJ.setWrapText(true);

            textoASEJ = bordeASEJ(libroASEJ.createCellStyle());
            centradoASEJ = bordeASEJ(libroASEJ.createCellStyle());
            centradoASEJ.setAlignment(HorizontalAlignment.CENTER);
            short formatoFechaASEJ = libroASEJ.createDataFormat().getFormat("dd/mm/yyyy");
            fechaASEJ = bordeASEJ(libroASEJ.createCellStyle());
            fechaASEJ.setDataFormat(formatoFechaASEJ);
            fechaASEJ.setAlignment(HorizontalAlignment.CENTER);
            fechaHoraASEJ = bordeASEJ(libroASEJ.createCellStyle());
            fechaHoraASEJ.setDataFormat(libroASEJ.createDataFormat().getFormat("dd/mm/yyyy hh:mm"));
            short formatoMonedaASEJ = libroASEJ.createDataFormat().getFormat("#,##0.00");
            monedaASEJ = bordeASEJ(libroASEJ.createCellStyle());
            monedaASEJ.setDataFormat(formatoMonedaASEJ);
            enteroASEJ = bordeASEJ(libroASEJ.createCellStyle());
            enteroASEJ.setDataFormat(libroASEJ.createDataFormat().getFormat("0"));
            decimalASEJ = bordeASEJ(libroASEJ.createCellStyle());
            decimalASEJ.setDataFormat(libroASEJ.createDataFormat().getFormat("0.00"));

            Font negritaASEJ = fuenteASEJ(libroASEJ, 10, true, IndexedColors.BLACK);
            totalASEJ = libroASEJ.createCellStyle();
            totalASEJ.setFont(negritaASEJ);
            totalASEJ.setBorderTop(BorderStyle.DOUBLE);
            totalMonedaASEJ = libroASEJ.createCellStyle();
            totalMonedaASEJ.cloneStyleFrom(totalASEJ);
            totalMonedaASEJ.setDataFormat(formatoMonedaASEJ);
        }

        private static Font fuenteASEJ(XSSFWorkbook libroASEJ, int puntosASEJ, boolean negritaASEJ, IndexedColors colorASEJ) {
            Font fASEJ = libroASEJ.createFont();
            fASEJ.setFontHeightInPoints((short) puntosASEJ);
            fASEJ.setBold(negritaASEJ);
            fASEJ.setColor(colorASEJ.getIndex());
            return fASEJ;
        }

        private static CellStyle bordeASEJ(CellStyle sASEJ) {
            sASEJ.setBorderBottom(BorderStyle.HAIR);
            sASEJ.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            return sASEJ;
        }
    }
}
