/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.modelo;

import com.odontogest.dominio.odontograma.Superficie;
import com.odontogest.persistencia.conversor.DineroConverter;
import com.odontogest.persistencia.conversor.SuperficiesConverter;
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
import java.util.EnumSet;
import java.util.Set;

/**
 * Ítem del plan de tratamiento (tabla {@code plan_item}): un procedimiento del tarifario, opcionalmente sobre una
 * pieza y sus superficies. La base de datos verifica {@code total = cantidad × precio_unit − descuento}.
 */
@Entity
@Table(name = "plan_item")
@Getter
@Setter
@NoArgsConstructor
public class PlanItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false, updatable = false)
    private PlanTratamiento plan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "procedimiento_id", nullable = false)
    private Procedimiento procedimiento;

    /** Hallazgo que originó la sugerencia ({@code null} si se agregó a mano o es de toda la boca). */
    @Column(name = "hallazgo_id")
    private Long hallazgoId;

    @Column(name = "pieza_fdi")
    private Integer piezaFdi;

    @Convert(converter = SuperficiesConverter.class)
    @Column(name = "superficies", nullable = false)
    private Set<Superficie> superficies = EnumSet.noneOf(Superficie.class);

    /** 1 urgente, 2 operatoria, 3 rehabilitación, 4 mantenimiento. */
    @Column(name = "fase", nullable = false)
    private int fase;

    @Column(name = "cantidad", nullable = false)
    private int cantidad = 1;

    @Convert(converter = DineroConverter.class)
    @Column(name = "precio_unit_cent", nullable = false)
    private BigDecimal precioUnit;

    @Convert(converter = DineroConverter.class)
    @Column(name = "descuento_cent", nullable = false)
    private BigDecimal descuento = BigDecimal.ZERO;

    @Convert(converter = DineroConverter.class)
    @Column(name = "total_cent", nullable = false)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoPlanItem estado = EstadoPlanItem.PENDIENTE;

    /** Atención en la que se realizó. */
    @Column(name = "atencion_id")
    private Long atencionId;

    @Column(name = "realizado_en")
    private LocalDateTime realizadoEn;

    @Column(name = "orden", nullable = false)
    private int orden;
}
