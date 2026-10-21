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
 * Atención (evolución clínica) de un paciente con un odontólogo (tabla {@code atencion}).
 */
@Entity
@Table(name = "atencion")
@Getter
@Setter
@NoArgsConstructor
public class Atencion {

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

    /** Fase 6: pasa a {@code @OneToOne Cita}. */
    @Column(name = "cita_id")
    private Long citaId;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "motivo_consulta", nullable = false)
    private String motivoConsulta;

    @Column(name = "examen_clinico")
    private String examenClinico;

    @Column(name = "diagnostico")
    private String diagnostico;

    @Column(name = "observaciones")
    private String observaciones;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoAtencion estado = EstadoAtencion.ABIERTA;

    @Column(name = "cerrada_en")
    private LocalDateTime cerradaEn;
}
