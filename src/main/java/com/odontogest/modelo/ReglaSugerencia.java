/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.modelo;

import com.odontogest.dominio.odontograma.EstadoHallazgo;
import com.odontogest.dominio.odontograma.TipoRadicular;
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

/**
 * Regla del motor de sugerencias hallazgo → procedimiento (tabla {@code regla_sugerencia}).
 */
@Entity
@Table(name = "regla_sugerencia")
@Getter
@Setter
@NoArgsConstructor
public class ReglaSugerencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** {@code null} = regla general (toda la boca). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hallazgo_tipo_id")
    private HallazgoTipo hallazgoTipo;

    /** {@code null} = cualquier estado. */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    private EstadoHallazgo estado;

    /** {@code null} = cualquier pieza. */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_radicular")
    private TipoRadicular tipoRadicular;

    @Column(name = "min_superficies", nullable = false)
    private int minSuperficies;

    @Column(name = "max_superficies", nullable = false)
    private int maxSuperficies = 5;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "procedimiento_id", nullable = false)
    private Procedimiento procedimiento;

    @Column(name = "fase", nullable = false)
    private int fase;

    /** Menor = se evalúa primero. */
    @Column(name = "prioridad", nullable = false)
    private int prioridad = 100;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;
}
