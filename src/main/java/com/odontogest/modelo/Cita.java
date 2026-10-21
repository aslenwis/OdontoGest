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
 * Cita de la agenda (tabla {@code cita}): intervalo semiabierto {@code [inicio, fin)} de un paciente con un
 * odontólogo. La base de datos exige {@code fin > inicio}; los solapamientos los impide {@code CitaService}.
 */
@Entity
@Table(name = "cita")
@Getter
@Setter
@NoArgsConstructor
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "odontologo_id", nullable = false)
    private Odontologo odontologo;

    @Column(name = "inicio", nullable = false)
    private LocalDateTime inicio;

    @Column(name = "fin", nullable = false)
    private LocalDateTime fin;

    @Column(name = "motivo", nullable = false)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoCita estado = EstadoCita.PROGRAMADA;

    /** Procedimiento del plan que se hará en la cita (opcional). */
    @Column(name = "plan_item_id")
    private Long planItemId;

    @Column(name = "notas")
    private String notas;

    /** Lo fija el envío de recordatorios (Fase 7). */
    @Column(name = "recordatorio_enviado_en")
    private LocalDateTime recordatorioEnviadoEn;

    @Column(name = "creado_por", nullable = false, updatable = false)
    private Long creadoPor;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;
}
