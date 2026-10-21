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
 * Pago registrado en caja (tabla {@code pago}) con su recibo interno correlativo. No se elimina: se anula con motivo
 * y sus aplicaciones se conservan para trazabilidad.
 */
@Entity
@Table(name = "pago")
@Getter
@Setter
@NoArgsConstructor
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** R-00000001. */
    @Column(name = "numero_recibo", nullable = false, unique = true, updatable = false)
    private String numeroRecibo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false, updatable = false)
    private Paciente paciente;

    /** Plan al que se aplicó todo el pago ({@code null} si se repartió entre varios planes). */
    @Column(name = "plan_id", updatable = false)
    private Long planId;

    @Column(name = "fecha", nullable = false, updatable = false)
    private LocalDateTime fecha;

    @Convert(converter = DineroConverter.class)
    @Column(name = "monto_cent", nullable = false, updatable = false)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(name = "medio", nullable = false, updatable = false)
    private MedioPago medio;

    /** Número de operación, voucher, etc. */
    @Column(name = "referencia", updatable = false)
    private String referencia;

    @Column(name = "registrado_por", nullable = false, updatable = false)
    private Long registradoPor;

    @Column(name = "anulado", nullable = false)
    private boolean anulado;

    @Column(name = "motivo_anulacion")
    private String motivoAnulacion;

    @Column(name = "anulado_en")
    private LocalDateTime anuladoEn;
}
