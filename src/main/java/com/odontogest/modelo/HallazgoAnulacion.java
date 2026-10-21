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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

/**
 * Anulación trazable de un hallazgo de un odontograma cerrado (tabla {@code hallazgo_anulacion}). El hallazgo
 * sigue en el historial marcado como anulado; la anulación no altera el hash de cierre.
 */
@Entity
@Immutable
@Table(name = "hallazgo_anulacion")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HallazgoAnulacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hallazgo_id", nullable = false, unique = true)
    private Hallazgo hallazgo;

    @Column(name = "motivo", nullable = false)
    private String motivo;

    @Column(name = "anulado_por", nullable = false)
    private Long anuladoPor;

    @Column(name = "anulado_en", nullable = false)
    private LocalDateTime anuladoEn;

    public static HallazgoAnulacion de(Hallazgo hallazgo, String motivo, long anuladoPor, LocalDateTime anuladoEn) {
        HallazgoAnulacion a = new HallazgoAnulacion();
        a.hallazgo = hallazgo;
        a.motivo = motivo;
        a.anuladoPor = anuladoPor;
        a.anuladoEn = anuladoEn;
        return a;
    }
}
