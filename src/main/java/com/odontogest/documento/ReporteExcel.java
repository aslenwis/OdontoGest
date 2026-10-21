/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.documento;

import com.odontogest.modelo.MedioPago;
import com.odontogest.servicio.dto.AuditoriaDto;
import com.odontogest.servicio.dto.DatosClinica;
import com.odontogest.servicio.dto.GrupoIndicesDto;
import com.odontogest.servicio.dto.IngresoDiaDto;
import com.odontogest.servicio.dto.MontoPeriodoDto;
import com.odontogest.servicio.dto.MorosoDto;
import com.odontogest.servicio.dto.PagoReporteDto;
import com.odontogest.servicio.dto.ProduccionFilaDto;
import com.odontogest.servicio.dto.ReporteEpidemiologicoDto;
import com.odontogest.servicio.dto.ReporteIngresosDto;
import com.odontogest.servicio.dto.ReporteMorosidadDto;
import com.odontogest.servicio.dto.ReporteProduccionDto;
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

/**
 * Reportes en Excel (.xlsx, RF-REP-02 y RF-REP-04) con Apache POI. Cada hoja lleva el nombre de la clínica, el
 * título y el periodo; la cabecera queda fija y con autofiltro, las fechas y montos son celdas numéricas con formato
 * (se pueden ordenar y sumar en Excel o LibreOffice) y los totales son fórmulas {@code SUM}.
 * <p>
 * Los montos llegan como {@code BigDecimal}; se pasan a {@code double} solo al escribir la celda, porque así guarda
 * Excel todos los números.
 */
@Component
public class ReporteExcel {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int FILA_CABECERA = 4;
    private static final int ANCHO_MAXIMO = 60 * 256;

    enum Tipo { TEXTO, FECHA, FECHA_HORA, MONEDA, ENTERO, DECIMAL }

    /** Columna de una hoja; {@code sumar} agrega su total al pie. */
    record Columna<T>(String titulo, Tipo tipo, Function<T, Object> valor, boolean sumar) {

        static <T> Columna<T> de(String titulo, Tipo tipo, Function<T, Object> valor) {
            return new Columna<>(titulo, tipo, valor, false);
        }

        static <T> Columna<T> total(String titulo, Tipo tipo, Function<T, Object> valor) {
            return new Columna<>(titulo, tipo, valor, true);
        }
    }

    public byte[] ingresos(ReporteIngresosDto r, DatosClinica clinica) {
        return libro(libro -> {
            Estilos e = new Estilos(libro);
            String periodo = periodo(r.desde(), r.hasta());
            List<Columna<IngresoDiaDto>> resumen = new ArrayList<>();
            resumen.add(Columna.de("Fecha", Tipo.FECHA, IngresoDiaDto::dia));
            resumen.add(Columna.total("Pagos", Tipo.ENTERO, IngresoDiaDto::pagos));
            for (MedioPago m : MedioPago.values()) {
                resumen.add(Columna.total(m.etiqueta(), Tipo.MONEDA, d -> d.monto(m)));
            }
            resumen.add(Columna.total("Total", Tipo.MONEDA, IngresoDiaDto::total));
            hoja(libro, e, "Ingresos por día", clinica, "Reporte de ingresos por día y medio de pago", periodo,
                    resumen, r.dias());
            hoja(libro, e, "Detalle de pagos", clinica, "Detalle de pagos (sin anulados)", periodo, List.of(
                    Columna.de("Fecha y hora", Tipo.FECHA_HORA, PagoReporteDto::fecha),
                    Columna.de("Recibo", Tipo.TEXTO, PagoReporteDto::numeroRecibo),
                    Columna.de("Historia", Tipo.TEXTO, PagoReporteDto::numeroHistoria),
                    Columna.de("Paciente", Tipo.TEXTO, PagoReporteDto::paciente),
                    Columna.de("Medio de pago", Tipo.TEXTO, p -> p.medio().etiqueta()),
                    Columna.de("Referencia", Tipo.TEXTO, PagoReporteDto::referencia),
                    Columna.total("Monto (S/)", Tipo.MONEDA, PagoReporteDto::monto)), r.detalle());
        });
    }

    public byte[] produccion(ReporteProduccionDto r, DatosClinica clinica) {
        return libro(libro -> {
            Estilos e = new Estilos(libro);
            String periodo = periodo(r.desde(), r.hasta());
            hoja(libro, e, "Producción", clinica, "Producción por odontólogo (procedimientos realizados)", periodo,
                    List.of(Columna.de("Odontólogo", Tipo.TEXTO, ProduccionFilaDto::odontologo),
                            Columna.de("Código", Tipo.TEXTO, ProduccionFilaDto::codigo),
                            Columna.de("Procedimiento", Tipo.TEXTO, ProduccionFilaDto::procedimiento),
                            Columna.total("Cantidad", Tipo.ENTERO, ProduccionFilaDto::cantidad),
                            Columna.total("Monto (S/)", Tipo.MONEDA, ProduccionFilaDto::monto)), r.filas());
            hoja(libro, e, "Por odontólogo", clinica, "Total por odontólogo", periodo,
                    List.of(Columna.de("Odontólogo", Tipo.TEXTO, MontoPeriodoDto::etiqueta),
                            Columna.total("Monto (S/)", Tipo.MONEDA, MontoPeriodoDto::monto)), r.porOdontologo());
        });
    }

    public byte[] morosidad(ReporteMorosidadDto r, DatosClinica clinica) {
        return libro(libro -> hoja(libro, new Estilos(libro), "Morosidad", clinica, "Reporte de morosidad",
                "Cuotas vencidas al " + FECHA.format(r.fecha()), List.of(
                        Columna.de("Historia", Tipo.TEXTO, MorosoDto::numeroHistoria),
                        Columna.de("Paciente", Tipo.TEXTO, MorosoDto::paciente),
                        Columna.de("Teléfono", Tipo.TEXTO, MorosoDto::telefono),
                        Columna.total("Cuotas vencidas", Tipo.ENTERO, MorosoDto::cuotasVencidas),
                        Columna.de("Vencimiento más antiguo", Tipo.FECHA, MorosoDto::vencimientoMasAntiguo),
                        Columna.de("Días de atraso", Tipo.ENTERO, MorosoDto::diasAtraso),
                        Columna.total("Saldo vencido (S/)", Tipo.MONEDA, MorosoDto::saldoVencido)), r.pacientes()));
    }

    public byte[] epidemiologico(ReporteEpidemiologicoDto r, DatosClinica clinica) {
        return libro(libro -> {
            XSSFSheet hoja = hoja(libro, new Estilos(libro), "Epidemiológico", clinica,
                    "Reporte epidemiológico: CPO-D y ceo-d promedio por grupo de edad",
                    periodo(r.desde(), r.hasta()) + " · " + r.pacientes() + " paciente(s) examinado(s)", List.of(
                            Columna.de("Grupo de edad", Tipo.TEXTO, GrupoIndicesDto::grupo),
                            Columna.total("Pacientes", Tipo.ENTERO, GrupoIndicesDto::pacientes),
                            Columna.de("CPO-D", Tipo.DECIMAL, GrupoIndicesDto::cpod),
                            Columna.de("C (cariados)", Tipo.DECIMAL, GrupoIndicesDto::c),
                            Columna.de("P (perdidos)", Tipo.DECIMAL, GrupoIndicesDto::p),
                            Columna.de("O (obturados)", Tipo.DECIMAL, GrupoIndicesDto::o),
                            Columna.de("ceo-d", Tipo.DECIMAL, GrupoIndicesDto::ceod),
                            Columna.de("Nivel OMS (CPO-D)", Tipo.TEXTO, GrupoIndicesDto::nivelCpod)), r.grupos());
            int fila = hoja.getLastRowNum() + 2;
            hoja.createRow(fila).createCell(0).setCellValue("Índices calculados con lo firmado del último odontograma"
                    + " de cada paciente; edad a la fecha de ese examen."
                    + (r.excluyeTercerasMolares() ? " El CPO-D excluye las terceras molares." : ""));
        });
    }

    public byte[] auditoria(List<AuditoriaDto> filas, LocalDate desde, LocalDate hasta, DatosClinica clinica) {
        return libro(libro -> hoja(libro, new Estilos(libro), "Auditoría", clinica, "Registro de auditoría",
                periodo(desde, hasta), List.of(
                        Columna.de("Fecha y hora", Tipo.FECHA_HORA, AuditoriaDto::fecha),
                        Columna.de("Usuario", Tipo.TEXTO, AuditoriaDto::usuario),
                        Columna.de("Acción", Tipo.TEXTO, AuditoriaDto::accion),
                        Columna.de("Entidad", Tipo.TEXTO, AuditoriaDto::entidad),
                        Columna.de("Id", Tipo.ENTERO, AuditoriaDto::entidadId),
                        Columna.de("Detalle", Tipo.TEXTO, AuditoriaDto::detalle)), filas));
    }

    // ------------------------------------------------------------------ construcción de hojas

    private interface Contenido {
        void escribir(XSSFWorkbook libro);
    }

    private static byte[] libro(Contenido contenido) {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            contenido.escribir(libro);
            // Calcula las fórmulas para que el archivo ya tenga los totales al abrirse (y al leerlo con POI).
            libro.getCreationHelper().createFormulaEvaluator().evaluateAll();
            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el Excel", e);
        }
    }

    static <T> XSSFSheet hoja(XSSFWorkbook libro, Estilos e, String nombre, DatosClinica clinica, String titulo,
                              String subtitulo, List<Columna<T>> columnas, List<T> filas) {
        XSSFSheet hoja = libro.createSheet(nombre);
        celda(hoja.createRow(0), 0, clinica.nombre(), e.clinica);
        celda(hoja.createRow(1), 0, titulo, e.titulo);
        celda(hoja.createRow(2), 0, subtitulo + " · Generado el " + FECHA_HORA.format(LocalDateTime.now()), e.nota);

        Row cabecera = hoja.createRow(FILA_CABECERA);
        for (int c = 0; c < columnas.size(); c++) {
            celda(cabecera, c, columnas.get(c).titulo(), e.cabecera);
        }
        int r = FILA_CABECERA + 1;
        for (T fila : filas) {
            Row row = hoja.createRow(r++);
            for (int c = 0; c < columnas.size(); c++) {
                valor(row.createCell(c), columnas.get(c).tipo(), columnas.get(c).valor().apply(fila), e);
            }
        }
        if (columnas.stream().anyMatch(Columna::sumar)) {
            Row total = hoja.createRow(r);
            celda(total, 0, "Total", e.total);
            for (int c = 1; c < columnas.size(); c++) {
                Cell cell = total.createCell(c);
                Columna<T> col = columnas.get(c);
                cell.setCellStyle(col.tipo() == Tipo.MONEDA ? e.totalMoneda : e.total);
                if (col.sumar()) {
                    String letra = CellReference.convertNumToColString(c);
                    cell.setCellFormula(filas.isEmpty() ? "0"
                            : "SUM(" + letra + (FILA_CABECERA + 2) + ":" + letra + (FILA_CABECERA + 1 + filas.size()) + ")");
                }
            }
        }
        if (columnas.size() > 1) {
            // Combinadas, las filas del título no ensanchan la primera columna (autoSizeColumn las ignora).
            for (int f = 0; f < FILA_CABECERA - 1; f++) {
                hoja.addMergedRegion(new CellRangeAddress(f, f, 0, columnas.size() - 1));
            }
        }
        hoja.createFreezePane(0, FILA_CABECERA + 1);
        // Impresión: horizontal, una página de ancho y la cabecera repetida en cada página.
        hoja.getPrintSetup().setLandscape(true);
        hoja.getPrintSetup().setPaperSize(PrintSetup.A4_PAPERSIZE);
        hoja.setFitToPage(true);
        hoja.getPrintSetup().setFitWidth((short) 1);
        hoja.getPrintSetup().setFitHeight((short) 0);
        hoja.setRepeatingRows(new CellRangeAddress(FILA_CABECERA, FILA_CABECERA, -1, -1));
        if (!filas.isEmpty()) {
            hoja.setAutoFilter(new CellRangeAddress(FILA_CABECERA, FILA_CABECERA + filas.size(), 0, columnas.size() - 1));
        }
        for (int c = 0; c < columnas.size(); c++) {
            hoja.autoSizeColumn(c);
            hoja.setColumnWidth(c, Math.min(ANCHO_MAXIMO, Math.max(hoja.getColumnWidth(c) + 512, 10 * 256)));
        }
        return hoja;
    }

    private static void valor(Cell cell, Tipo tipo, Object v, Estilos e) {
        if (v == null) {
            cell.setCellValue(tipo == Tipo.TEXTO ? "" : "—");
            cell.setCellStyle(tipo == Tipo.TEXTO ? e.texto : e.centrado);
            return;
        }
        switch (tipo) {
            case TEXTO -> {
                cell.setCellValue(v.toString());
                cell.setCellStyle(e.texto);
            }
            case FECHA -> {
                cell.setCellValue((LocalDate) v);
                cell.setCellStyle(e.fecha);
            }
            case FECHA_HORA -> {
                cell.setCellValue((LocalDateTime) v);
                cell.setCellStyle(e.fechaHora);
            }
            case MONEDA -> {
                cell.setCellValue(((BigDecimal) v).doubleValue());
                cell.setCellStyle(e.moneda);
            }
            case ENTERO -> {
                cell.setCellValue(((Number) v).longValue());
                cell.setCellStyle(e.entero);
            }
            case DECIMAL -> {
                cell.setCellValue(((Number) v).doubleValue());
                cell.setCellStyle(e.decimal);
            }
        }
    }

    private static void celda(Row row, int columna, String texto, CellStyle estilo) {
        Cell c = row.createCell(columna);
        c.setCellValue(texto);
        c.setCellStyle(estilo);
    }

    private static String periodo(LocalDate desde, LocalDate hasta) {
        return "Del " + FECHA.format(desde) + " al " + FECHA.format(hasta);
    }

    /** Estilos compartidos de un libro (POI limita la cantidad de estilos por archivo). */
    static final class Estilos {
        final CellStyle clinica;
        final CellStyle titulo;
        final CellStyle nota;
        final CellStyle cabecera;
        final CellStyle texto;
        final CellStyle centrado;
        final CellStyle fecha;
        final CellStyle fechaHora;
        final CellStyle moneda;
        final CellStyle entero;
        final CellStyle decimal;
        final CellStyle total;
        final CellStyle totalMoneda;

        Estilos(XSSFWorkbook libro) {
            clinica = libro.createCellStyle();
            clinica.setFont(fuente(libro, 14, true, IndexedColors.BLACK));
            titulo = libro.createCellStyle();
            titulo.setFont(fuente(libro, 12, true, IndexedColors.DARK_BLUE));
            nota = libro.createCellStyle();
            nota.setFont(fuente(libro, 9, false, IndexedColors.GREY_50_PERCENT));

            cabecera = libro.createCellStyle();
            cabecera.setFont(fuente(libro, 10, true, IndexedColors.WHITE));
            cabecera.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            cabecera.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cabecera.setAlignment(HorizontalAlignment.CENTER);
            cabecera.setWrapText(true);

            texto = borde(libro.createCellStyle());
            centrado = borde(libro.createCellStyle());
            centrado.setAlignment(HorizontalAlignment.CENTER);
            short formatoFecha = libro.createDataFormat().getFormat("dd/mm/yyyy");
            fecha = borde(libro.createCellStyle());
            fecha.setDataFormat(formatoFecha);
            fecha.setAlignment(HorizontalAlignment.CENTER);
            fechaHora = borde(libro.createCellStyle());
            fechaHora.setDataFormat(libro.createDataFormat().getFormat("dd/mm/yyyy hh:mm"));
            short formatoMoneda = libro.createDataFormat().getFormat("#,##0.00");
            moneda = borde(libro.createCellStyle());
            moneda.setDataFormat(formatoMoneda);
            entero = borde(libro.createCellStyle());
            entero.setDataFormat(libro.createDataFormat().getFormat("0"));
            decimal = borde(libro.createCellStyle());
            decimal.setDataFormat(libro.createDataFormat().getFormat("0.00"));

            Font negrita = fuente(libro, 10, true, IndexedColors.BLACK);
            total = libro.createCellStyle();
            total.setFont(negrita);
            total.setBorderTop(BorderStyle.DOUBLE);
            totalMoneda = libro.createCellStyle();
            totalMoneda.cloneStyleFrom(total);
            totalMoneda.setDataFormat(formatoMoneda);
        }

        private static Font fuente(XSSFWorkbook libro, int puntos, boolean negrita, IndexedColors color) {
            Font f = libro.createFont();
            f.setFontHeightInPoints((short) puntos);
            f.setBold(negrita);
            f.setColor(color.getIndex());
            return f;
        }

        private static CellStyle borde(CellStyle s) {
            s.setBorderBottom(BorderStyle.HAIR);
            s.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            return s;
        }
    }
}
