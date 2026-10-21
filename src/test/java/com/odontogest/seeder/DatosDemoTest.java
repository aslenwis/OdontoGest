/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.seeder;

import com.odontogest.PruebaIntegracion;
import com.odontogest.config.RelojAplicacion;
import com.odontogest.seguridad.SesionUsuario;
import com.odontogest.servicio.ConfiguracionService;
import com.odontogest.modelo.Sexo;
import com.odontogest.modelo.TipoDocumento;
import com.odontogest.servicio.IntegridadService;
import com.odontogest.servicio.PacienteService;
import com.odontogest.servicio.ReporteService;
import com.odontogest.servicio.dto.PacienteForm;
import com.odontogest.servicio.dto.PacienteResumen;
import com.odontogest.servicio.dto.ReporteIngresosDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Datos de demostración completos (PLAN §3.5) sobre una base propia: la siembra corre al levantar este contexto,
 * igual que en el primer arranque de la aplicación.
 */
class DatosDemoTest extends PruebaIntegracion {

    @DynamicPropertySource
    static void baseDemo(DynamicPropertyRegistry registro) {
        String base = HOME_PRUEBAS.resolve("demo-" + UUID.randomUUID() + ".db").toString().replace('\\', '/');
        registro.add("spring.datasource.url", () -> "jdbc:sqlite:" + base + "?foreign_keys=true&journal_mode=WAL&busy_timeout=5000");
        registro.add("app.seeder.datos-demo", () -> "true");
    }

    @Autowired private JdbcTemplate jdbc;
    @Autowired private IntegridadService integridadService;
    @Autowired private ConfiguracionService configuracionService;
    @Autowired private ReporteService reporteService;
    @Autowired private RelojAplicacion reloj;
    @Autowired private PacienteService pacienteService;
    @Autowired private SesionUsuario sesion;

    private long contar(String sql, Object... args) {
        Long n = jdbc.queryForObject(sql, Long.class, args);
        return n == null ? 0 : n;
    }

    @Test
    void pacientesDemo() {
        assertThat(contar("SELECT COUNT(*) FROM paciente WHERE numero_historia <= 'HC-000060'")).isEqualTo(60);
        assertThat(contar("SELECT COUNT(*) FROM paciente WHERE numero_documento NOT LIKE '9%'")).isZero();
        assertThat(contar("SELECT COUNT(*) FROM paciente WHERE apoderado_nombre IS NOT NULL")).isEqualTo(10);
        assertThat(contar("SELECT COUNT(*) FROM anamnesis")).isEqualTo(60);
        assertThat(contar("SELECT COUNT(*) FROM consentimiento WHERE tipo = 'DATOS_PERSONALES'")).isEqualTo(60);
        assertThat(configuracionService.texto(DatabaseSeeder.CLAVE_ULTIMO_HC_DEMO)).isEqualTo("HC-000060");
        assertThat(contar("SELECT COUNT(DISTINCT substr(creado_en, 1, 7)) FROM paciente"))
                .as("registrados a lo largo de unos seis meses").isGreaterThanOrEqualTo(6);
    }

    @Test
    void losPacientesDemoSeMarcan_losNuevosNo() {
        iniciarSesion("recepcion");
        assertThat(pacienteService.buscar("", false, 0).contenido()).isNotEmpty().allMatch(PacienteResumen::demo);
        long nuevo = pacienteService.guardar(new PacienteForm(null, TipoDocumento.DNI, dniAleatorio(), "Paciente",
                "Real" + sufijo(), null, LocalDate.now().minusYears(30), Sexo.F, null, null, null, null, null, null,
                null, null, null, true));
        assertThat(pacienteService.obtener(nuevo).demo()).isFalse();
        assertThat(pacienteService.obtener(nuevo).numeroHistoria()).isEqualTo("HC-000061");
    }

    @Test
    void odontogramasFirmadosYCadenaIntegra() {
        assertThat(contar("SELECT COUNT(*) FROM odontograma WHERE tipo = 'INICIAL' AND estado = 'CERRADO'")).isEqualTo(40);
        assertThat(contar("SELECT COUNT(*) FROM odontograma WHERE tipo = 'EVOLUCION' AND estado = 'CERRADO'")).isEqualTo(15);
        assertThat(contar("SELECT COUNT(*) FROM odontograma WHERE estado = 'BORRADOR'")).isZero();
        assertThat(contar("SELECT COUNT(*) FROM atencion WHERE estado = 'ABIERTA'")).isZero();
        List<Long> conHallazgos = jdbc.queryForList("""
                SELECT o.id FROM odontograma o WHERE o.tipo = 'INICIAL'
                AND (SELECT COUNT(*) FROM hallazgo h WHERE h.odontograma_id = o.id) BETWEEN 3 AND 12""", Long.class);
        assertThat(conHallazgos).as("3 a 12 hallazgos por odontograma inicial").hasSize(40);
        // Dentición según la edad: nada de piezas permanentes (11–48) en una dentición decidua.
        assertThat(contar("""
                SELECT COUNT(*) FROM hallazgo h JOIN odontograma o ON o.id = h.odontograma_id
                WHERE o.denticion = 'DECIDUA' AND h.pieza_fdi < 50""")).isZero();
        for (Long paciente : jdbc.queryForList("SELECT DISTINCT paciente_id FROM odontograma", Long.class)) {
            assertThat(integridadService.verificar(paciente).integra()).as("cadena del paciente " + paciente).isTrue();
        }
        assertThat(contar("""
                SELECT COUNT(*) FROM hallazgo h JOIN hallazgo_tipo t ON t.id = h.hallazgo_tipo_id JOIN odontograma o
                ON o.id = h.odontograma_id WHERE o.tipo = 'EVOLUCION' AND t.codigo = 'RESTAURACION_DEFINITIVA'"""))
                .as("la evolución reemplaza caries por restauraciones").isPositive();
    }

    @Test
    void planesPagosYMorosidad() {
        assertThat(contar("SELECT COUNT(*) FROM plan_tratamiento")).isEqualTo(40);
        assertThat(contar("SELECT COUNT(*) FROM plan_tratamiento WHERE estado IN ('ACEPTADO','EN_CURSO','FINALIZADO')"))
                .isGreaterThanOrEqualTo(30);
        assertThat(contar("SELECT COUNT(*) FROM plan_item WHERE estado = 'REALIZADO'")).isGreaterThan(40);
        assertThat(contar("SELECT COUNT(*) FROM pago")).isGreaterThan(50);
        assertThat(contar("SELECT COALESCE(SUM(monto_cent), 0) FROM pago_aplicacion"))
                .as("los pagos aplicados FIFO cuadran con lo pagado en las cuotas")
                .isEqualTo(contar("SELECT COALESCE(SUM(pagado_cent), 0) FROM cuota"))
                .isEqualTo(contar("SELECT COALESCE(SUM(monto_cent), 0) FROM pago WHERE anulado = 0"));
        assertThat(contar("SELECT COUNT(DISTINCT substr(fecha, 1, 7)) FROM pago")).as("ingresos en varios meses")
                .isGreaterThanOrEqualTo(5);
        iniciarSesion("admin");
        assertThat(reporteService.morosidad().pacientes()).as("algunos pacientes quedan con cuotas vencidas").isNotEmpty();
        LocalDate hoy = LocalDate.now();
        ReporteIngresosDto ingresos = reporteService.ingresos(hoy.minusMonths(6), hoy);
        assertThat(ingresos.total()).isPositive();
    }

    @Test
    void citasEnLaVentanaSinSolapamientos() {
        LocalDate hoy = LocalDate.now();
        long ventana = contar("SELECT COUNT(*) FROM cita WHERE inicio >= ? AND inicio < ?",
                hoy.minusDays(30) + "T00:00:00", hoy.plusDays(31) + "T00:00:00");
        assertThat(ventana).isGreaterThanOrEqualTo(120);
        assertThat(contar("""
                SELECT COUNT(*) FROM cita a JOIN cita b ON a.id < b.id AND a.odontologo_id = b.odontologo_id
                WHERE a.inicio < b.fin AND b.inicio < a.fin
                  AND a.estado NOT IN ('CANCELADA','NO_ASISTIO') AND b.estado NOT IN ('CANCELADA','NO_ASISTIO')""")).isZero();
        assertThat(contar("SELECT COUNT(*) FROM cita WHERE inicio > ? AND estado IN ('ATENDIDA','NO_ASISTIO')",
                java.time.LocalDateTime.now().withNano(0).toString())).as("nada futuro figura como atendido").isZero();
        assertThat(contar("SELECT COUNT(*) FROM cita WHERE estado = 'ATENDIDA'")).isGreaterThanOrEqualTo(55);
        assertThat(contar("SELECT COUNT(DISTINCT estado) FROM cita")).isGreaterThanOrEqualTo(4);
    }

    @Test
    void alTerminarElRelojYLaSesionQuedanNormales() {
        assertThat(reloj.esHoraReal()).isTrue();
        assertThat(contar("SELECT COUNT(*) FROM auditoria")).isGreaterThan(500);
        assertThat(contar("SELECT COUNT(*) FROM auditoria WHERE fecha > ?", java.time.LocalDateTime.now().plusMinutes(1)
                .withNano(0).toString())).as("ningún registro con fecha futura").isZero();
        assertThat(sesion.estaAutenticada()).isFalse();
    }
}
