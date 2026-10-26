package com.odontogest.persistencia.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

import static com.odontogest.persistencia.dao.DashboardDaoASEJ.PLANES_COBRABLESASEJ;
import static com.odontogest.persistencia.dao.DashboardDaoASEJ.inicioDeASEJ;

@Repository
@RequiredArgsConstructor
public class ReporteDaoASEJ {

    private static final String NOMBRE_PACIENTEASEJ =
            "pa.apellido_paterno || COALESCE(' ' || NULLIF(pa.apellido_materno, ''), '') || ', ' || pa.nombres";

    public record IngresoDiaASEJ(String diaASEJ, String medioASEJ, int pagosASEJ, long montoCentASEJ) {
    }

    public record PagoFilaASEJ(String fechaASEJ, String reciboASEJ, String historiaASEJ, String pacienteASEJ, String medioASEJ,
                           String referenciaASEJ, long montoCentASEJ) {
    }

    public record ProduccionFilaASEJ(long odontologoIdASEJ, String odontologoASEJ, String codigoASEJ, String procedimientoASEJ,
                                 int cantidadASEJ, long montoCentASEJ) {
    }

    public record MorosoFilaASEJ(long pacienteIdASEJ, String historiaASEJ, String pacienteASEJ, String telefonoASEJ, int cuotasASEJ,
                             long saldoCentASEJ, String vencimientoMasAntiguoASEJ) {
    }

    public record PacienteExaminadoASEJ(long pacienteIdASEJ, String fechaNacimientoASEJ, String ultimoCierreASEJ) {
    }

    private final JdbcTemplate jdbcTemplateASEJ;

    public List<IngresoDiaASEJ> ingresosPorDiaASEJ(LocalDate desdeASEJ, LocalDate hastaASEJ) {
        return jdbcTemplateASEJ.query("""
                SELECT substr(fecha, 1, 10) AS dia, medio, COUNT(*) AS pagos, SUM(monto_cent) AS monto FROM pago
                WHERE anulado = 0 AND fecha >= ? AND fecha < ?
                GROUP BY dia, medio ORDER BY dia, medio""",
                (rsASEJ, iASEJ) -> new IngresoDiaASEJ(rsASEJ.getString("dia"), rsASEJ.getString("medio"), rsASEJ.getInt("pagos"),
                        rsASEJ.getLong("monto")),
                inicioDeASEJ(desdeASEJ), inicioDeASEJ(hastaASEJ));
    }

    public List<PagoFilaASEJ> pagosASEJ(LocalDate desdeASEJ, LocalDate hastaASEJ) {
        return jdbcTemplateASEJ.query("""
                SELECT pg.fecha, pg.numero_recibo, pa.numero_historia, %s AS paciente, pg.medio, pg.referencia,
                       pg.monto_cent
                FROM pago pg JOIN paciente pa ON pa.id = pg.paciente_id
                WHERE pg.anulado = 0 AND pg.fecha >= ? AND pg.fecha < ?
                ORDER BY pg.fecha, pg.id""".formatted(NOMBRE_PACIENTEASEJ),
                (rsASEJ, iASEJ) -> new PagoFilaASEJ(rsASEJ.getString("fecha"), rsASEJ.getString("numero_recibo"),
                        rsASEJ.getString("numero_historia"), rsASEJ.getString("paciente"), rsASEJ.getString("medio"),
                        rsASEJ.getString("referencia"), rsASEJ.getLong("monto_cent")),
                inicioDeASEJ(desdeASEJ), inicioDeASEJ(hastaASEJ));
    }

    public List<ProduccionFilaASEJ> produccionASEJ(LocalDate desdeASEJ, LocalDate hastaASEJ, Long odontologoIdASEJ) {
        return jdbcTemplateASEJ.query("""
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
                (rsASEJ, iASEJ) -> new ProduccionFilaASEJ(rsASEJ.getLong("odontologo_id"), rsASEJ.getString("odontologo"),
                        rsASEJ.getString("codigo"), rsASEJ.getString("nombre"), rsASEJ.getInt("cantidad"), rsASEJ.getLong("monto")),
                inicioDeASEJ(desdeASEJ), inicioDeASEJ(hastaASEJ), odontologoIdASEJ, odontologoIdASEJ);
    }

    public List<MorosoFilaASEJ> morosidadASEJ(LocalDate hoyASEJ, int limiteASEJ) {
        return jdbcTemplateASEJ.query("""
                SELECT pa.id, pa.numero_historia, %s AS paciente, pa.telefono, COUNT(*) AS cuotas,
                       SUM(c.monto_cent - c.pagado_cent) AS saldo, MIN(c.vencimiento) AS mas_antiguo
                FROM cuota c
                JOIN plan_tratamiento p ON p.id = c.plan_id
                JOIN paciente pa ON pa.id = p.paciente_id
                WHERE p.estado IN %s AND c.vencimiento < ? AND c.pagado_cent < c.monto_cent
                GROUP BY pa.id
                ORDER BY saldo DESC, mas_antiguo
                LIMIT ?""".formatted(NOMBRE_PACIENTEASEJ, PLANES_COBRABLESASEJ),
                (rsASEJ, iASEJ) -> new MorosoFilaASEJ(rsASEJ.getLong("id"), rsASEJ.getString("numero_historia"), rsASEJ.getString("paciente"),
                        rsASEJ.getString("telefono"), rsASEJ.getInt("cuotas"), rsASEJ.getLong("saldo"), rsASEJ.getString("mas_antiguo")),
                hoyASEJ.toString(), limiteASEJ <= 0 ? -1 : limiteASEJ);
    }

    public List<PacienteExaminadoASEJ> pacientesExaminadosASEJ(LocalDate desdeASEJ, LocalDate hastaASEJ) {
        return jdbcTemplateASEJ.query("""
                SELECT pa.id, pa.fecha_nacimiento, MAX(o.cerrado_en) AS ultimo
                FROM odontograma o JOIN paciente pa ON pa.id = o.paciente_id
                WHERE o.estado = 'CERRADO' AND pa.activo = 1
                GROUP BY pa.id
                HAVING ultimo >= ? AND ultimo < ?
                ORDER BY pa.id""",
                (rsASEJ, iASEJ) -> new PacienteExaminadoASEJ(rsASEJ.getLong("id"), rsASEJ.getString("fecha_nacimiento"),
                        rsASEJ.getString("ultimo")),
                inicioDeASEJ(desdeASEJ), inicioDeASEJ(hastaASEJ));
    }
}
