/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Anamnesis (antecedentes médicos) de un paciente, una por paciente (tabla {@code anamnesis}).
 */
@Entity
@Table(name = "anamnesis")
@Getter
@Setter
@NoArgsConstructor
public class Anamnesis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false, unique = true)
    private Paciente paciente;

    @Column(name = "antecedentes_medicos")
    private String antecedentesMedicos;

    @Column(name = "alergias")
    private String alergias;

    @Column(name = "medicacion_actual")
    private String medicacionActual;

    /** JSON {@code {"diabetes":true,...}} con el checklist de enfermedades sistémicas. */
    @Column(name = "enfermedades_json", nullable = false)
    private String enfermedadesJson = "{}";

    @Column(name = "habitos")
    private String habitos;

    @Column(name = "fuma", nullable = false)
    private boolean fuma;

    @Column(name = "gestante", nullable = false)
    private boolean gestante;

    @Column(name = "presion_arterial")
    private String presionArterial;

    @Column(name = "observaciones")
    private String observaciones;

    @Column(name = "actualizado_por")
    private Long actualizadoPor;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;
}
