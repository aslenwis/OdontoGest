/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.modelo;

import com.odontogest.dominio.tratamiento.FormaCobro;
import com.odontogest.persistencia.conversor.DineroConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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

import java.math.BigDecimal;

/**
 * Procedimiento del tarifario (tabla {@code procedimiento}).
 */
@Entity
@Table(name = "procedimiento")
@Getter
@Setter
@NoArgsConstructor
public class Procedimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true)
    private String codigo;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", nullable = false)
    private CategoriaProcedimiento categoria;

    @Convert(converter = DineroConverter.class)
    @Column(name = "precio_cent", nullable = false)
    private BigDecimal precio;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_cobro", nullable = false)
    private FormaCobro formaCobro;

    @Column(name = "duracion_min", nullable = false)
    private int duracionMin = 30;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;
}
