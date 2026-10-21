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
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import java.time.LocalDateTime;

/**
 * Plan de tratamiento y presupuesto del paciente (tabla {@code plan_tratamiento}). Los totales se recalculan con
 * {@code CalculadoraTotales} cada vez que cambian los ítems o el descuento global; la base de datos verifica
 * {@code total = subtotal − descuento}.
 */
@Entity
@Table(name = "plan_tratamiento")
@Getter
@Setter
@NoArgsConstructor
public class PlanTratamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false, updatable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "odontologo_id", nullable = false)
    private Odontologo odontologo;

    /** Odontograma firmado que originó las sugerencias ({@code null} si el plan se armó a mano). */
    @Column(name = "odontograma_id")
    private Long odontogramaId;

    @Column(name = "fecha", nullable = false, updatable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoPlan estado = EstadoPlan.BORRADOR;

    @Convert(converter = DineroConverter.class)
    @Column(name = "subtotal_cent", nullable = false)
    private BigDecimal subtotal = BigDecimal.ZERO;

    /** Descuento global (se resta del subtotal). */
    @Convert(converter = DineroConverter.class)
    @Column(name = "descuento_cent", nullable = false)
    private BigDecimal descuento = BigDecimal.ZERO;

    @Convert(converter = DineroConverter.class)
    @Column(name = "total_cent", nullable = false)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "observaciones")
    private String observaciones;

    @Column(name = "aceptado_en")
    private LocalDateTime aceptadoEn;
}
