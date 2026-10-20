/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.persistencia;

import com.odontogest.PruebaIntegracion;
import com.odontogest.dominio.odontograma.Superficie;
import com.odontogest.modelo.Rol;
import com.odontogest.modelo.Usuario;
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

/**
 * Guarda y lee fechas, dinero y superficies contra SQLite real y verifica el texto/entero exacto
 * almacenado (PLAN §3.3, riesgo §7.3 sobre {@code autoApply} con {@code java.time}).
 */
@Transactional
class ConversoresIntegracionTest extends PruebaIntegracion {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void crearTablaDePrueba() {
        jdbc.execute(RegistroPruebaConversor.DDL);
    }

    @Test
    void guardaFechasDineroYSuperficiesConElFormatoEsperado() {
        RegistroPruebaConversor r = new RegistroPruebaConversor();
        r.setFecha(LocalDate.of(2026, 3, 9));
        r.setFechaHora(LocalDateTime.of(2026, 3, 9, 8, 5, 7, 123_456_789));
        r.setHora(LocalTime.of(9, 30));
        r.setMonto(new BigDecimal("1234.50"));
        r.setSuperficies(EnumSet.of(Superficie.M, Superficie.O, Superficie.D));
        em.persist(r);
        em.flush();

        Map<String, Object> fila = jdbc.queryForMap("""
                SELECT fecha, typeof(fecha) AS t_fecha, fecha_hora, typeof(fecha_hora) AS t_fecha_hora,
                       hora, typeof(hora) AS t_hora, monto_cent, typeof(monto_cent) AS t_monto,
                       superficies, typeof(superficies) AS t_superficies
                FROM prueba_conversor WHERE id = ?""", r.getId());

        assertThat(fila.get("fecha")).isEqualTo("2026-03-09");
        assertThat(fila.get("t_fecha")).isEqualTo("text");
        assertThat(fila.get("fecha_hora")).isEqualTo("2026-03-09T08:05:07");
        assertThat(fila.get("t_fecha_hora")).isEqualTo("text");
        assertThat(fila.get("hora")).isEqualTo("09:30");
        assertThat(fila.get("t_hora")).isEqualTo("text");
        assertThat(((Number) fila.get("monto_cent")).longValue()).isEqualTo(123450L);
        assertThat(fila.get("t_monto")).isEqualTo("integer");
        assertThat(((Number) fila.get("superficies")).intValue()).isEqualTo(7);   // M|O|D
        assertThat(fila.get("t_superficies")).isEqualTo("integer");

        em.clear();
        RegistroPruebaConversor leido = em.find(RegistroPruebaConversor.class, r.getId());
        assertThat(leido.getFecha()).isEqualTo(LocalDate.of(2026, 3, 9));
        assertThat(leido.getFechaHora()).isEqualTo(LocalDateTime.of(2026, 3, 9, 8, 5, 7));
        assertThat(leido.getHora()).isEqualTo(LocalTime.of(9, 30));
        assertThat(leido.getMonto()).isEqualByComparingTo("1234.50");
        assertThat(leido.getMonto().scale()).isEqualTo(2);
        assertThat(leido.getSuperficies()).containsExactlyInAnyOrder(Superficie.M, Superficie.O, Superficie.D);
    }

    @Test
    void guardaNulosYConjuntoVacio() {
        RegistroPruebaConversor r = new RegistroPruebaConversor();
        r.setSuperficies(EnumSet.noneOf(Superficie.class));
        em.persist(r);
        em.flush();

        Map<String, Object> fila = jdbc.queryForMap(
                "SELECT fecha, fecha_hora, hora, monto_cent, superficies FROM prueba_conversor WHERE id = ?", r.getId());
        assertThat(fila.get("fecha")).isNull();
        assertThat(fila.get("fecha_hora")).isNull();
        assertThat(fila.get("hora")).isNull();
        assertThat(fila.get("monto_cent")).isNull();
        assertThat(((Number) fila.get("superficies")).intValue()).isZero();

        em.clear();
        assertThat(em.find(RegistroPruebaConversor.class, r.getId()).getSuperficies()).isEmpty();
    }

    @Test
    void entidadRealUsuarioGuardaFechaHoraComoTextoYBooleanoComoEntero() {
        Usuario u = new Usuario();
        u.setUsername("prueba.conversor");
        u.setPasswordHash("x");
        u.setNombreCompleto("Prueba Conversor");
        u.setRol(Rol.RECEPCION);
        u.setCreadoEn(LocalDateTime.of(2026, 1, 2, 3, 4, 5));
        em.persist(u);
        em.flush();

        Map<String, Object> fila = jdbc.queryForMap(
                "SELECT creado_en, typeof(creado_en) AS t, activo, typeof(activo) AS t_activo, rol FROM usuario WHERE id = ?",
                u.getId());
        assertThat(fila.get("creado_en")).isEqualTo("2026-01-02T03:04:05");
        assertThat(fila.get("t")).isEqualTo("text");
        assertThat(((Number) fila.get("activo")).intValue()).isEqualTo(1);
        assertThat(fila.get("t_activo")).isEqualTo("integer");
        assertThat(fila.get("rol")).isEqualTo("RECEPCION");
    }
}
