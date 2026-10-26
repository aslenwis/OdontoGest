package com.odontogest.persistencia;

import com.odontogest.PruebaIntegracionASEJ;
import com.odontogest.dominio.odontograma.SuperficieASEJ;
import com.odontogest.modelo.RolASEJ;
import com.odontogest.modelo.UsuarioASEJ;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class ConversoresIntegracionTestASEJ extends PruebaIntegracionASEJ {

    @PersistenceContext
    private EntityManager emASEJ;

    @Autowired
    private JdbcTemplate jdbcASEJ;

    @BeforeEach
    void crearTablaDePruebaASEJ() {
        jdbcASEJ.execute(RegistroPruebaConversorASEJ.DDLASEJ);
    }

    @Test
    void guardaFechasDineroYSuperficiesConElFormatoEsperadoASEJ() {
        RegistroPruebaConversorASEJ rASEJ = new RegistroPruebaConversorASEJ();
        rASEJ.setFechaASEJ(LocalDate.of(2026, 3, 9));
        rASEJ.setFechaHoraASEJ(LocalDateTime.of(2026, 3, 9, 8, 5, 7, 123_456_789));
        rASEJ.setHoraASEJ(LocalTime.of(9, 30));
        rASEJ.setMontoASEJ(new BigDecimal("1234.50"));
        rASEJ.setSuperficiesASEJ(EnumSet.of(SuperficieASEJ.M, SuperficieASEJ.O, SuperficieASEJ.D));
        emASEJ.persist(rASEJ);
        emASEJ.flush();

        Map<String, Object> filaASEJ = jdbcASEJ.queryForMap("""
                SELECT fecha, typeof(fecha) AS t_fecha, fecha_hora, typeof(fecha_hora) AS t_fecha_hora,
                       hora, typeof(hora) AS t_hora, monto_cent, typeof(monto_cent) AS t_monto,
                       superficies, typeof(superficies) AS t_superficies
                FROM prueba_conversor WHERE id = ?""", rASEJ.getIdASEJ());

        assertThat(filaASEJ.get("fecha")).isEqualTo("2026-03-09");
        assertThat(filaASEJ.get("t_fecha")).isEqualTo("text");
        assertThat(filaASEJ.get("fecha_hora")).isEqualTo("2026-03-09T08:05:07");
        assertThat(filaASEJ.get("t_fecha_hora")).isEqualTo("text");
        assertThat(filaASEJ.get("hora")).isEqualTo("09:30");
        assertThat(filaASEJ.get("t_hora")).isEqualTo("text");
        assertThat(((Number) filaASEJ.get("monto_cent")).longValue()).isEqualTo(123450L);
        assertThat(filaASEJ.get("t_monto")).isEqualTo("integer");
        assertThat(((Number) filaASEJ.get("superficies")).intValue()).isEqualTo(7);
        assertThat(filaASEJ.get("t_superficies")).isEqualTo("integer");

        emASEJ.clear();
        RegistroPruebaConversorASEJ leidoASEJ = emASEJ.find(RegistroPruebaConversorASEJ.class, rASEJ.getIdASEJ());
        assertThat(leidoASEJ.getFechaASEJ()).isEqualTo(LocalDate.of(2026, 3, 9));
        assertThat(leidoASEJ.getFechaHoraASEJ()).isEqualTo(LocalDateTime.of(2026, 3, 9, 8, 5, 7));
        assertThat(leidoASEJ.getHoraASEJ()).isEqualTo(LocalTime.of(9, 30));
        assertThat(leidoASEJ.getMontoASEJ()).isEqualByComparingTo("1234.50");
        assertThat(leidoASEJ.getMontoASEJ().scale()).isEqualTo(2);
        assertThat(leidoASEJ.getSuperficiesASEJ()).containsExactlyInAnyOrder(SuperficieASEJ.M, SuperficieASEJ.O, SuperficieASEJ.D);
    }

    @Test
    void guardaNulosYConjuntoVacioASEJ() {
        RegistroPruebaConversorASEJ rASEJ = new RegistroPruebaConversorASEJ();
        rASEJ.setSuperficiesASEJ(EnumSet.noneOf(SuperficieASEJ.class));
        emASEJ.persist(rASEJ);
        emASEJ.flush();

        Map<String, Object> filaASEJ = jdbcASEJ.queryForMap(
                "SELECT fecha, fecha_hora, hora, monto_cent, superficies FROM prueba_conversor WHERE id = ?", rASEJ.getIdASEJ());
        assertThat(filaASEJ.get("fecha")).isNull();
        assertThat(filaASEJ.get("fecha_hora")).isNull();
        assertThat(filaASEJ.get("hora")).isNull();
        assertThat(filaASEJ.get("monto_cent")).isNull();
        assertThat(((Number) filaASEJ.get("superficies")).intValue()).isZero();

        emASEJ.clear();
        assertThat(emASEJ.find(RegistroPruebaConversorASEJ.class, rASEJ.getIdASEJ()).getSuperficiesASEJ()).isEmpty();
    }

    @Test
    void entidadRealUsuarioGuardaFechaHoraComoTextoYBooleanoComoEnteroASEJ() {
        UsuarioASEJ uASEJ = new UsuarioASEJ();
        uASEJ.setUsernameASEJ("prueba.conversor");
        uASEJ.setPasswordHashASEJ("x");
        uASEJ.setNombreCompletoASEJ("Prueba Conversor");
        uASEJ.setRolASEJ(RolASEJ.RECEPCION);
        uASEJ.setCreadoEnASEJ(LocalDateTime.of(2026, 1, 2, 3, 4, 5));
        emASEJ.persist(uASEJ);
        emASEJ.flush();

        Map<String, Object> filaASEJ = jdbcASEJ.queryForMap(
                "SELECT creado_en, typeof(creado_en) AS t, activo, typeof(activo) AS t_activo, rol FROM usuario WHERE id = ?",
                uASEJ.getIdASEJ());
        assertThat(filaASEJ.get("creado_en")).isEqualTo("2026-01-02T03:04:05");
        assertThat(filaASEJ.get("t")).isEqualTo("text");
        assertThat(((Number) filaASEJ.get("activo")).intValue()).isEqualTo(1);
        assertThat(filaASEJ.get("t_activo")).isEqualTo("integer");
        assertThat(filaASEJ.get("rol")).isEqualTo("RECEPCION");
    }
}
