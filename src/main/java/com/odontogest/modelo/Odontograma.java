/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.modelo;

import com.odontogest.dominio.odontograma.Denticion;
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
 * Odontograma de un paciente (tabla {@code odontograma}). Al cerrarse queda firmado con un hash SHA-256
 * encadenado al odontograma cerrado anterior.
 */
@Entity
@Table(name = "odontograma")
@Getter
@Setter
@NoArgsConstructor
public class Odontograma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "atencion_id", nullable = false)
    private Atencion atencion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, updatable = false)
    private TipoOdontograma tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "denticion", nullable = false)
    private Denticion denticion;

    @Column(name = "especificaciones")
    private String especificaciones;

    @Column(name = "observaciones")
    private String observaciones;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoOdontogramaRegistro estado = EstadoOdontogramaRegistro.BORRADOR;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "cerrado_en")
    private LocalDateTime cerradoEn;

    @Column(name = "cerrado_por")
    private Long cerradoPor;

    /** Hash del odontograma cerrado anterior del paciente, o {@code GENESIS} si es el primero. */
    @Column(name = "hash_anterior")
    private String hashAnterior;

    @Column(name = "hash_cierre")
    private String hashCierre;

    public boolean esBorrador() {
        return estado == EstadoOdontogramaRegistro.BORRADOR;
    }
}
