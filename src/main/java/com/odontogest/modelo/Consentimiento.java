/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.modelo;

import jakarta.persistence.Column;
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

import java.time.LocalDateTime;

/**
 * Registro de un consentimiento firmado (tabla {@code consentimiento}): de datos personales (Ley N° 29733)
 * o de tratamiento (Fase 4, vinculado a un plan).
 */
@Entity
@Table(name = "consentimiento")
@Getter
@Setter
@NoArgsConstructor
public class Consentimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoConsentimiento tipo;

    /** Fase 4: pasa a {@code @ManyToOne PlanTratamiento}. */
    @Column(name = "plan_id")
    private Long planId;

    @Column(name = "aceptado_en", nullable = false)
    private LocalDateTime aceptadoEn;

    /** Paciente o apoderado que firma. */
    @Column(name = "firmado_por", nullable = false)
    private String firmadoPor;

    @Column(name = "registrado_por", nullable = false)
    private Long registradoPor;
}
