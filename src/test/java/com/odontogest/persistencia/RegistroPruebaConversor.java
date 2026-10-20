/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.persistencia;

import com.odontogest.dominio.odontograma.Superficie;
import com.odontogest.persistencia.conversor.DineroConverter;
import com.odontogest.persistencia.conversor.SuperficiesConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

/**
 * Entidad solo de pruebas que reúne todos los tipos convertidos. Las fechas NO llevan
 * {@code @Convert}: así la prueba verifica que {@code autoApply} funciona.
 */
@Entity
@Table(name = RegistroPruebaConversor.TABLA)
@Getter
@Setter
public class RegistroPruebaConversor {

    static final String TABLA = "prueba_conversor";
    static final String DDL = """
            CREATE TABLE IF NOT EXISTS prueba_conversor (
                id          INTEGER PRIMARY KEY,
                fecha       TEXT,
                fecha_hora  TEXT,
                hora        TEXT,
                monto_cent  INTEGER,
                superficies INTEGER NOT NULL DEFAULT 0 CHECK (superficies BETWEEN 0 AND 31)
            )""";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha")
    private LocalDate fecha;

    @Column(name = "fecha_hora")
    private LocalDateTime fechaHora;

    @Column(name = "hora")
    private LocalTime hora;

    @Convert(converter = DineroConverter.class)
    @Column(name = "monto_cent")
    private BigDecimal monto;

    @Convert(converter = SuperficiesConverter.class)
    @Column(name = "superficies", nullable = false)
    private Set<Superficie> superficies;
}
