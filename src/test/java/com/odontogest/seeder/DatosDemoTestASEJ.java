package com.odontogest.seeder;

import com.odontogest.PruebaIntegracionASEJ;
import com.odontogest.config.RelojAplicacionASEJ;
import com.odontogest.seguridad.SesionUsuarioASEJ;
import com.odontogest.servicio.ConfiguracionServiceASEJ;
import com.odontogest.modelo.SexoASEJ;
import com.odontogest.modelo.TipoDocumentoASEJ;
import com.odontogest.servicio.IntegridadServiceASEJ;
import com.odontogest.servicio.PacienteServiceASEJ;
import com.odontogest.servicio.ReporteServiceASEJ;
import com.odontogest.servicio.dto.PacienteFormASEJ;
import com.odontogest.servicio.dto.PacienteResumenASEJ;
import com.odontogest.servicio.dto.ReporteIngresosDtoASEJ;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DatosDemoTestASEJ extends PruebaIntegracionASEJ {

    @DynamicPropertySource
    static void baseDemoASEJ(DynamicPropertyRegistry registroASEJ) {
        String baseASEJ = HOME_PRUEBASASEJ.resolve("demo-" + UUID.randomUUID() + ".db").toString().replace('\\', '/');
        registroASEJ.add("spring.datasource.url", () -> "jdbc:sqlite:" + baseASEJ + "?foreign_keys=true&journal_mode=WAL&busy_timeout=5000");
        registroASEJ.add("app.seeder.datos-demo", () -> "true");
    }

    @Autowired private JdbcTemplate jdbcASEJ;
    @Autowired private IntegridadServiceASEJ integridadServiceASEJ;
    @Autowired private ConfiguracionServiceASEJ configuracionServiceASEJ;
    @Autowired private ReporteServiceASEJ reporteServiceASEJ;
    @Autowired private RelojAplicacionASEJ relojASEJ;
    @Autowired private PacienteServiceASEJ pacienteServiceASEJ;
    @Autowired private SesionUsuarioASEJ sesionASEJ;

    private long contarASEJ(String sqlASEJ, Object... argsASEJ) {
        Long nASEJ = jdbcASEJ.queryForObject(sqlASEJ, Long.class, argsASEJ);
        return nASEJ == null ? 0 : nASEJ;
    }

    @Test
    void pacientesDemoASEJ() {
        assertThat(contarASEJ("SELECT COUNT(*) FROM paciente WHERE numero_historia <= 'HC-000060'")).isEqualTo(60);
        assertThat(contarASEJ("SELECT COUNT(*) FROM paciente WHERE numero_historia <= 'HC-000060' AND numero_documento NOT LIKE '9%'")).isZero();
        assertThat(contarASEJ("SELECT COUNT(*) FROM paciente WHERE apoderado_nombre IS NOT NULL")).isEqualTo(10);
        assertThat(contarASEJ("SELECT COUNT(*) FROM anamnesis")).isEqualTo(60);
        assertThat(contarASEJ("SELECT COUNT(*) FROM consentimiento WHERE tipo = 'DATOS_PERSONALES'")).isEqualTo(60);
        assertThat(configuracionServiceASEJ.textoASEJ(DatabaseSeederASEJ.CLAVE_ULTIMO_HC_DEMOASEJ)).isEqualTo("HC-000060");
        assertThat(contarASEJ("SELECT COUNT(DISTINCT substr(creado_en, 1, 7)) FROM paciente"))
                .as("registrados a lo largo de unos seis meses").isGreaterThanOrEqualTo(6);
    }

    @Test
    void losPacientesDemoSeMarcan_losNuevosNoASEJ() {
        iniciarSesionASEJ("recepcion");
        assertThat(pacienteServiceASEJ.buscarASEJ("", false, 0).contenidoASEJ()).isNotEmpty().allMatch(PacienteResumenASEJ::demoASEJ);
        long nuevoASEJ = pacienteServiceASEJ.guardarASEJ(new PacienteFormASEJ(null, TipoDocumentoASEJ.DNI, dniAleatorioASEJ(), "Paciente",
                "Real" + sufijoASEJ(), null, LocalDate.now().minusYears(30), SexoASEJ.F, null, null, null, null, null, null,
                null, null, null, true));
        assertThat(pacienteServiceASEJ.obtenerASEJ(nuevoASEJ).demoASEJ()).isFalse();
        assertThat(pacienteServiceASEJ.obtenerASEJ(nuevoASEJ).numeroHistoriaASEJ()).isEqualTo("HC-000061");
    }

    @Test
    void odontogramasFirmadosYCadenaIntegraASEJ() {
        assertThat(contarASEJ("SELECT COUNT(*) FROM odontograma WHERE tipo = 'INICIAL' AND estado = 'CERRADO'")).isEqualTo(40);
        assertThat(contarASEJ("SELECT COUNT(*) FROM odontograma WHERE tipo = 'EVOLUCION' AND estado = 'CERRADO'")).isEqualTo(15);
        assertThat(contarASEJ("SELECT COUNT(*) FROM odontograma WHERE estado = 'BORRADOR'")).isZero();
        assertThat(contarASEJ("SELECT COUNT(*) FROM atencion WHERE estado = 'ABIERTA'")).isZero();
        List<Long> conHallazgosASEJ = jdbcASEJ.queryForList("""
                SELECT o.id FROM odontograma o WHERE o.tipo = 'INICIAL'
                AND (SELECT COUNT(*) FROM hallazgo h WHERE h.odontograma_id = o.id) BETWEEN 3 AND 12""", Long.class);
        assertThat(conHallazgosASEJ).as("3 a 12 hallazgos por odontograma inicial").hasSize(40);
        assertThat(contarASEJ("""
                SELECT COUNT(*) FROM hallazgo h JOIN odontograma o ON o.id = h.odontograma_id
                WHERE o.denticion = 'DECIDUA' AND h.pieza_fdi < 50""")).isZero();
        for (Long pacienteASEJ : jdbcASEJ.queryForList("SELECT DISTINCT paciente_id FROM odontograma", Long.class)) {
            assertThat(integridadServiceASEJ.verificarASEJ(pacienteASEJ).integraASEJ()).as("cadena del paciente " + pacienteASEJ).isTrue();
        }
        assertThat(contarASEJ("""
                SELECT COUNT(*) FROM hallazgo h JOIN hallazgo_tipo t ON t.id = h.hallazgo_tipo_id JOIN odontograma o
                ON o.id = h.odontograma_id WHERE o.tipo = 'EVOLUCION' AND t.codigo = 'RESTAURACION_DEFINITIVA'"""))
                .as("la evolución reemplaza caries por restauraciones").isPositive();
    }

    @Test
    void planesPagosYMorosidadASEJ() {
        assertThat(contarASEJ("SELECT COUNT(*) FROM plan_tratamiento")).isEqualTo(40);
        assertThat(contarASEJ("SELECT COUNT(*) FROM plan_tratamiento WHERE estado IN ('ACEPTADO','EN_CURSO','FINALIZADO')"))
                .isGreaterThanOrEqualTo(30);
        assertThat(contarASEJ("SELECT COUNT(*) FROM plan_item WHERE estado = 'REALIZADO'")).isGreaterThan(40);
        assertThat(contarASEJ("SELECT COUNT(*) FROM pago")).isGreaterThan(50);
        assertThat(contarASEJ("SELECT COALESCE(SUM(monto_cent), 0) FROM pago_aplicacion"))
                .as("los pagos aplicados FIFO cuadran con lo pagado en las cuotas")
                .isEqualTo(contarASEJ("SELECT COALESCE(SUM(pagado_cent), 0) FROM cuota"))
                .isEqualTo(contarASEJ("SELECT COALESCE(SUM(monto_cent), 0) FROM pago WHERE anulado = 0"));
        assertThat(contarASEJ("SELECT COUNT(DISTINCT substr(fecha, 1, 7)) FROM pago")).as("ingresos en varios meses")
                .isGreaterThanOrEqualTo(5);
        iniciarSesionASEJ("admin");
        assertThat(reporteServiceASEJ.morosidadASEJ().pacientesASEJ()).as("algunos pacientes quedan con cuotas vencidas").isNotEmpty();
        LocalDate hoyASEJ = LocalDate.now();
        ReporteIngresosDtoASEJ ingresosASEJ = reporteServiceASEJ.ingresosASEJ(hoyASEJ.minusMonths(6), hoyASEJ);
        assertThat(ingresosASEJ.totalASEJ()).isPositive();
    }

    @Test
    void citasEnLaVentanaSinSolapamientosASEJ() {
        LocalDate hoyASEJ = LocalDate.now();
        long ventanaASEJ = contarASEJ("SELECT COUNT(*) FROM cita WHERE inicio >= ? AND inicio < ?",
                hoyASEJ.minusDays(30) + "T00:00:00", hoyASEJ.plusDays(31) + "T00:00:00");
        assertThat(ventanaASEJ).isGreaterThanOrEqualTo(120);
        assertThat(contarASEJ("""
                SELECT COUNT(*) FROM cita a JOIN cita b ON a.id < b.id AND a.odontologo_id = b.odontologo_id
                WHERE a.inicio < b.fin AND b.inicio < a.fin
                  AND a.estado NOT IN ('CANCELADA','NO_ASISTIO') AND b.estado NOT IN ('CANCELADA','NO_ASISTIO')""")).isZero();
        assertThat(contarASEJ("SELECT COUNT(*) FROM cita WHERE inicio > ? AND estado IN ('ATENDIDA','NO_ASISTIO')",
                java.time.LocalDateTime.now().withNano(0).toString())).as("nada futuro figura como atendido").isZero();
        assertThat(contarASEJ("SELECT COUNT(*) FROM cita WHERE estado = 'ATENDIDA'")).isGreaterThanOrEqualTo(55);
        assertThat(contarASEJ("SELECT COUNT(DISTINCT estado) FROM cita")).isGreaterThanOrEqualTo(4);
    }

    @Test
    void alTerminarElRelojYLaSesionQuedanNormalesASEJ() {
        assertThat(relojASEJ.esHoraRealASEJ()).isTrue();
        assertThat(contarASEJ("SELECT COUNT(*) FROM auditoria")).isGreaterThan(500);
        assertThat(contarASEJ("SELECT COUNT(*) FROM auditoria WHERE fecha > ?", java.time.LocalDateTime.now().plusMinutes(1)
                .withNano(0).toString())).as("ningún registro con fecha futura").isZero();
        assertThat(sesionASEJ.estaAutenticadaASEJ()).isFalse();
    }
}
