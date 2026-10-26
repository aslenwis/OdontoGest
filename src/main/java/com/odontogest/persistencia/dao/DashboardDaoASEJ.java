package com.odontogest.persistencia.dao;

import com.odontogest.persistencia.conversor.LocalDateTimeConverterASEJ;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class DashboardDaoASEJ {

    static final String PLANES_COBRABLESASEJ = "('ACEPTADO','EN_CURSO','FINALIZADO')";

    public record MontoMesASEJ(String mesASEJ, long montoCentASEJ) {
    }

    public record VencidasASEJ(int cuotasASEJ, int pacientesASEJ, long saldoCentASEJ) {
    }

    private final JdbcTemplate jdbcTemplateASEJ;

    public int citasDelDiaASEJ(LocalDate diaASEJ, Long odontologoIdASEJ) {
        Integer nASEJ = jdbcTemplateASEJ.queryForObject("""
                SELECT COUNT(*) FROM cita
                WHERE inicio >= ? AND inicio < ? AND estado <> 'CANCELADA' AND (? IS NULL OR odontologo_id = ?)""",
                Integer.class, inicioDeASEJ(diaASEJ), inicioDeASEJ(diaASEJ.plusDays(1)), odontologoIdASEJ, odontologoIdASEJ);
        return nASEJ == null ? 0 : nASEJ;
    }

    public long ingresosASEJ(LocalDate desdeASEJ, LocalDate hastaASEJ) {
        Long totalASEJ = jdbcTemplateASEJ.queryForObject(
                "SELECT COALESCE(SUM(monto_cent), 0) FROM pago WHERE anulado = 0 AND fecha >= ? AND fecha < ?",
                Long.class, inicioDeASEJ(desdeASEJ), inicioDeASEJ(hastaASEJ));
        return totalASEJ == null ? 0 : totalASEJ;
    }

    public List<MontoMesASEJ> ingresosPorMesASEJ(LocalDate desdeASEJ, LocalDate hastaASEJ) {
        return jdbcTemplateASEJ.query("""
                SELECT substr(fecha, 1, 7) AS mes, SUM(monto_cent) AS monto FROM pago
                WHERE anulado = 0 AND fecha >= ? AND fecha < ?
                GROUP BY mes ORDER BY mes""",
                (rsASEJ, iASEJ) -> new MontoMesASEJ(rsASEJ.getString("mes"), rsASEJ.getLong("monto")), inicioDeASEJ(desdeASEJ), inicioDeASEJ(hastaASEJ));
    }

    public List<MontoMesASEJ> produccionPorMesASEJ(long odontologoIdASEJ, LocalDate desdeASEJ, LocalDate hastaASEJ) {
        return jdbcTemplateASEJ.query("""
                SELECT substr(i.realizado_en, 1, 7) AS mes, SUM(i.total_cent) AS monto
                FROM plan_item i
                JOIN plan_tratamiento p ON p.id = i.plan_id
                LEFT JOIN atencion a ON a.id = i.atencion_id
                WHERE i.estado = 'REALIZADO' AND p.estado <> 'ANULADO'
                  AND i.realizado_en >= ? AND i.realizado_en < ?
                  AND COALESCE(a.odontologo_id, p.odontologo_id) = ?
                GROUP BY mes ORDER BY mes""",
                (rsASEJ, iASEJ) -> new MontoMesASEJ(rsASEJ.getString("mes"), rsASEJ.getLong("monto")),
                inicioDeASEJ(desdeASEJ), inicioDeASEJ(hastaASEJ), odontologoIdASEJ);
    }

    public int pacientesNuevosASEJ(LocalDate desdeASEJ, LocalDate hastaASEJ) {
        Integer nASEJ = jdbcTemplateASEJ.queryForObject("SELECT COUNT(*) FROM paciente WHERE creado_en >= ? AND creado_en < ?",
                Integer.class, inicioDeASEJ(desdeASEJ), inicioDeASEJ(hastaASEJ));
        return nASEJ == null ? 0 : nASEJ;
    }

    public VencidasASEJ vencidasASEJ(LocalDate hoyASEJ) {
        return jdbcTemplateASEJ.queryForObject("""
                SELECT COUNT(*) AS cuotas, COUNT(DISTINCT p.paciente_id) AS pacientes,
                       COALESCE(SUM(c.monto_cent - c.pagado_cent), 0) AS saldo
                FROM cuota c JOIN plan_tratamiento p ON p.id = c.plan_id
                WHERE p.estado IN """ + PLANES_COBRABLESASEJ + " AND c.vencimiento < ? AND c.pagado_cent < c.monto_cent",
                (rsASEJ, iASEJ) -> new VencidasASEJ(rsASEJ.getInt("cuotas"), rsASEJ.getInt("pacientes"), rsASEJ.getLong("saldo")),
                hoyASEJ.toString());
    }

    static String inicioDeASEJ(LocalDate diaASEJ) {
        return LocalDateTimeConverterASEJ.aTextoASEJ(diaASEJ.atStartOfDay());
    }
}
