/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.modelo;

import com.odontogest.dominio.odontograma.ConteoIndice;
import com.odontogest.dominio.odontograma.ReglaColor;
import com.odontogest.dominio.odontograma.Representacion;
import com.odontogest.dominio.odontograma.SimboloHallazgo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tipo del catálogo de hallazgos del odontograma (tabla {@code hallazgo_tipo}). Colores, símbolos y capas
 * se configuran aquí, no en el código.
 */
@Entity
@Table(name = "hallazgo_tipo")
@Getter
@Setter
@NoArgsConstructor
public class HallazgoTipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true)
    private String codigo;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    /** Sigla fija que se dibuja en el recuadro de la pieza. */
    @Column(name = "sigla")
    private String sigla;

    /** JSON {@code ["AM","R"]} si el usuario debe elegir la sigla al registrar. */
    @Column(name = "siglas_opciones")
    private String siglasOpciones;

    @Enumerated(EnumType.STRING)
    @Column(name = "representacion", nullable = false)
    private Representacion representacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "simbolo", nullable = false)
    private SimboloHallazgo simbolo;

    @Enumerated(EnumType.STRING)
    @Column(name = "regla_color", nullable = false)
    private ReglaColor reglaColor;

    @Column(name = "grupo_exclusion", nullable = false)
    private String grupoExclusion;

    @Column(name = "oculta_superficies", nullable = false)
    private boolean ocultaSuperficies;

    @Enumerated(EnumType.STRING)
    @Column(name = "conteo_cpod", nullable = false)
    private ConteoIndice conteoCpod = ConteoIndice.NINGUNO;

    @Enumerated(EnumType.STRING)
    @Column(name = "conteo_ceod", nullable = false)
    private ConteoIndice conteoCeod = ConteoIndice.NINGUNO;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Column(name = "orden", nullable = false)
    private int orden;
}
