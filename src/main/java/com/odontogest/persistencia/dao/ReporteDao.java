/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.persistencia.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

import static com.odontogest.persistencia.dao.DashboardDao.PLANES_COBRABLES;
import static com.odontogest.persistencia.dao.DashboardDao.inicioDe;

/**
 * Consultas de agregación de los reportes (RF-REP-02 a RF-REP-05). Los rangos son {@code [desde, hasta)} y los
 * montos se devuelven en céntimos; el servicio los convierte a {@code BigDecimal}.
 */
@Repository
@RequiredArgsConstructor
public class ReporteDao {

    /** "Apellido Paterno Apellido Materno, Nombres", como {@code Paciente.getNombreCompleto()}. */
    private static final String NOMBRE_PACIENTE =
            "pa.apellido_paterno || COALESCE(' ' || NULLIF(pa.apellido_materno, ''), '') || ', ' || pa.nombres";

    /** Ingresos de un día con un medio de pago. */
    public record IngresoDia(String dia, String medio, int pagos, long montoCent) {
    }

    /** Un pago no anulado del periodo. */
    public record PagoFila(String fecha, String recibo, String historia, String paciente, String medio,
                           String referencia, long montoCent) {
    }

    /** Procedimientos realizados por un odontólogo. */
    public record ProduccionFila(long odontologoId, String odontologo, String codigo, String procedimiento,
                                 int cantidad, long montoCent) {
    }

    /** Deuda vencida de un paciente. */
    public record MorosoFila(long pacienteId, String historia, String paciente, String telefono, int cuotas,
                             long saldoCent, String vencimientoMasAntiguo) {
    }

    /** Paciente con al menos un odontograma firmado en el periodo. */
    public record PacienteExaminado(long pacienteId, String fechaNacimiento, String ultimoCierre) {
    }

    private final JdbcTemplate jdbcTemplate;

    public List<IngresoDia> ingresosPorDia(LocalDate desde, LocalDate hasta) {
        return jdbcTemplate.query("""
                SELECT substr(fecha, 1, 10) AS dia, medio, COUNT(*) AS pagos, SUM(monto_cent) AS monto FROM pago
                WHERE anulado = 0 AND fecha >= ? AND fecha < ?
                GROUP BY dia, medio ORDER BY dia, medio""",
                (rs, i) -> new IngresoDia(rs.getString("dia"), rs.getString("medio"), rs.getInt("pagos"),
                        rs.getLong("monto")),
                inicioDe(desde), inicioDe(hasta));
    }

    public List<PagoFila> pagos(LocalDate desde, LocalDate hasta) {
        return jdbcTemplate.query("""
                SELECT pg.fecha, pg.numero_recibo, pa.numero_historia, %s AS paciente, pg.medio, pg.referencia,
                       pg.monto_cent
                FROM pago pg JOIN paciente pa ON pa.id = pg.paciente_id
                WHERE pg.anulado = 0 AND pg.fecha >= ? AND pg.fecha < ?
                ORDER BY pg.fecha, pg.id""".formatted(NOMBRE_PACIENTE),
                (rs, i) -> new PagoFila(rs.getString("fecha"), rs.getString("numero_recibo"),
                        rs.getString("numero_historia"), rs.getString("paciente"), rs.getString("medio"),
                        rs.getString("referencia"), rs.getLong("monto_cent")),
                inicioDe(desde), inicioDe(hasta));
    }

    /**
     * Procedimientos realizados en {@code [desde, hasta)} agrupados por odontólogo (el de la atención en que se
     * realizó o, si no hay atención, el del plan) y procedimiento. {@code odontologoId == null} = todos.
     */
    public List<ProduccionFila> produccion(LocalDate desde, LocalDate hasta, Long odontologoId) {
        return jdbcTemplate.query("""
                SELECT o.id AS odontologo_id, o.nombres || ' ' || o.apellidos AS odontologo, pr.codigo, pr.nombre,
                       SUM(i.cantidad) AS cantidad, SUM(i.total_cent) AS monto
                FROM plan_item i
                JOIN plan_tratamiento p ON p.id = i.plan_id
                JOIN procedimiento pr ON pr.id = i.procedimiento_id
                LEFT JOIN atencion a ON a.id = i.atencion_id
                JOIN odontologo o ON o.id = COALESCE(a.odontologo_id, p.odontologo_id)
                WHERE i.estado = 'REALIZADO' AND p.estado <> 'ANULADO'
                  AND i.realizado_en >= ? AND i.realizado_en < ? AND (? IS NULL OR o.id = ?)
                GROUP BY o.id, pr.id
                ORDER BY odontologo, monto DESC, pr.codigo""",
                (rs, i) -> new ProduccionFila(rs.getLong("odontologo_id"), rs.getString("odontologo"),
                        rs.getString("codigo"), rs.getString("nombre"), rs.getInt("cantidad"), rs.getLong("monto")),
                inicioDe(desde), inicioDe(hasta), odontologoId, odontologoId);
    }

    /** Pacientes con cuotas vencidas a {@code hoy}, de mayor a menor deuda; {@code limite <= 0} = todos. */
    public List<MorosoFila> morosidad(LocalDate hoy, int limite) {
        return jdbcTemplate.query("""
                SELECT pa.id, pa.numero_historia, %s AS paciente, pa.telefono, COUNT(*) AS cuotas,
                       SUM(c.monto_cent - c.pagado_cent) AS saldo, MIN(c.vencimiento) AS mas_antiguo
                FROM cuota c
                JOIN plan_tratamiento p ON p.id = c.plan_id
                JOIN paciente pa ON pa.id = p.paciente_id
                WHERE p.estado IN %s AND c.vencimiento < ? AND c.pagado_cent < c.monto_cent
                GROUP BY pa.id
                ORDER BY saldo DESC, mas_antiguo
                LIMIT ?""".formatted(NOMBRE_PACIENTE, PLANES_COBRABLES),
                (rs, i) -> new MorosoFila(rs.getLong("id"), rs.getString("numero_historia"), rs.getString("paciente"),
                        rs.getString("telefono"), rs.getInt("cuotas"), rs.getLong("saldo"), rs.getString("mas_antiguo")),
                hoy.toString(), limite <= 0 ? -1 : limite);
    }

    /** Pacientes activos cuyo último odontograma firmado se cerró en {@code [desde, hasta)}. */
    public List<PacienteExaminado> pacientesExaminados(LocalDate desde, LocalDate hasta) {
        return jdbcTemplate.query("""
                SELECT pa.id, pa.fecha_nacimiento, MAX(o.cerrado_en) AS ultimo
                FROM odontograma o JOIN paciente pa ON pa.id = o.paciente_id
                WHERE o.estado = 'CERRADO' AND pa.activo = 1
                GROUP BY pa.id
                HAVING ultimo >= ? AND ultimo < ?
                ORDER BY pa.id""",
                (rs, i) -> new PacienteExaminado(rs.getLong("id"), rs.getString("fecha_nacimiento"),
                        rs.getString("ultimo")),
                inicioDe(desde), inicioDe(hasta));
    }
}
