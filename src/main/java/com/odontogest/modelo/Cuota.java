/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.modelo;

import com.odontogest.persistencia.conversor.DineroConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Cuota del cronograma de pagos de un plan (tabla {@code cuota}). El estado (pendiente, parcial, vencida, pagada)
 * se deriva con {@code EstadoCuota}; {@code pagado} lo actualiza la caja (Fase 5).
 */
@Entity
@Table(name = "cuota")
@Getter
@Setter
@NoArgsConstructor
public class Cuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false, updatable = false)
    private PlanTratamiento plan;

    /** 0 = cuota inicial. */
    @Column(name = "numero", nullable = false)
    private int numero;

    @Column(name = "vencimiento", nullable = false)
    private LocalDate vencimiento;

    @Convert(converter = DineroConverter.class)
    @Column(name = "monto_cent", nullable = false)
    private BigDecimal monto;

    @Convert(converter = DineroConverter.class)
    @Column(name = "pagado_cent", nullable = false)
    private BigDecimal pagado = BigDecimal.ZERO;
}
