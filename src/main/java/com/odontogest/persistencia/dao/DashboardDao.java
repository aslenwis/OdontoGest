/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.persistencia.dao;

import com.odontogest.persistencia.conversor.LocalDateTimeConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Indicadores del tablero de inicio (RF-REP-01) con SQL nativo sobre las fechas ISO-8601 en TEXT y los montos en
 * céntimos: comparar textos {@code yyyy-MM-dd...} equivale a comparar fechas.
 */
@Repository
@RequiredArgsConstructor
public class DashboardDao {

    /** Planes cuyas cuotas se cobran (mismos estados que la caja). */
    static final String PLANES_COBRABLES = "('ACEPTADO','EN_CURSO','FINALIZADO')";

    /** Monto de un mes {@code yyyy-MM}. */
    public record MontoMes(String mes, long montoCent) {
    }

    /** Cantidad y saldo de las cuotas vencidas. */
    public record Vencidas(int cuotas, int pacientes, long saldoCent) {
    }

    private final JdbcTemplate jdbcTemplate;

    /** Citas no canceladas del día; {@code odontologoId == null} = de todos los odontólogos. */
    public int citasDelDia(LocalDate dia, Long odontologoId) {
        Integer n = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM cita
                WHERE inicio >= ? AND inicio < ? AND estado <> 'CANCELADA' AND (? IS NULL OR odontologo_id = ?)""",
                Integer.class, inicioDe(dia), inicioDe(dia.plusDays(1)), odontologoId, odontologoId);
        return n == null ? 0 : n;
    }

    /** Ingresos (pagos no anulados) en {@code [desde, hasta)}. */
    public long ingresos(LocalDate desde, LocalDate hasta) {
        Long total = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(monto_cent), 0) FROM pago WHERE anulado = 0 AND fecha >= ? AND fecha < ?",
                Long.class, inicioDe(desde), inicioDe(hasta));
        return total == null ? 0 : total;
    }

    /** Ingresos por mes en {@code [desde, hasta)}; los meses sin pagos no aparecen. */
    public List<MontoMes> ingresosPorMes(LocalDate desde, LocalDate hasta) {
        return jdbcTemplate.query("""
                SELECT substr(fecha, 1, 7) AS mes, SUM(monto_cent) AS monto FROM pago
                WHERE anulado = 0 AND fecha >= ? AND fecha < ?
                GROUP BY mes ORDER BY mes""",
                (rs, i) -> new MontoMes(rs.getString("mes"), rs.getLong("monto")), inicioDe(desde), inicioDe(hasta));
    }

    /**
     * Producción de un odontólogo por mes: procedimientos realizados en {@code [desde, hasta)} en sus atenciones (o,
     * sin atención, en sus planes).
     */
    public List<MontoMes> produccionPorMes(long odontologoId, LocalDate desde, LocalDate hasta) {
        return jdbcTemplate.query("""
                SELECT substr(i.realizado_en, 1, 7) AS mes, SUM(i.total_cent) AS monto
                FROM plan_item i
                JOIN plan_tratamiento p ON p.id = i.plan_id
                LEFT JOIN atencion a ON a.id = i.atencion_id
                WHERE i.estado = 'REALIZADO' AND p.estado <> 'ANULADO'
                  AND i.realizado_en >= ? AND i.realizado_en < ?
                  AND COALESCE(a.odontologo_id, p.odontologo_id) = ?
                GROUP BY mes ORDER BY mes""",
                (rs, i) -> new MontoMes(rs.getString("mes"), rs.getLong("monto")),
                inicioDe(desde), inicioDe(hasta), odontologoId);
    }

    /** Pacientes registrados en {@code [desde, hasta)}. */
    public int pacientesNuevos(LocalDate desde, LocalDate hasta) {
        Integer n = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM paciente WHERE creado_en >= ? AND creado_en < ?",
                Integer.class, inicioDe(desde), inicioDe(hasta));
        return n == null ? 0 : n;
    }

    /** Cuotas con saldo cuyo vencimiento es anterior a {@code hoy}, en planes cobrables. */
    public Vencidas vencidas(LocalDate hoy) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) AS cuotas, COUNT(DISTINCT p.paciente_id) AS pacientes,
                       COALESCE(SUM(c.monto_cent - c.pagado_cent), 0) AS saldo
                FROM cuota c JOIN plan_tratamiento p ON p.id = c.plan_id
                WHERE p.estado IN """ + PLANES_COBRABLES + " AND c.vencimiento < ? AND c.pagado_cent < c.monto_cent",
                (rs, i) -> new Vencidas(rs.getInt("cuotas"), rs.getInt("pacientes"), rs.getLong("saldo")),
                hoy.toString());
    }

    /** Inicio del día en el formato persistido, para comparar con columnas fecha-hora. */
    static String inicioDe(LocalDate dia) {
        return LocalDateTimeConverter.aTexto(dia.atStartOfDay());
    }
}
