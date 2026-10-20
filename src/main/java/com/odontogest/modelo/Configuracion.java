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
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Parámetro de configuración clave-valor (tabla {@code configuracion}).
 */
@Entity
@Table(name = "configuracion")
@Getter
@Setter
@NoArgsConstructor
public class Configuracion {

    @Id
    @Column(name = "clave")
    private String clave;

    @Column(name = "valor")
    private String valor;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoConfiguracion tipo;

    @Column(name = "grupo", nullable = false)
    private String grupo;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "editable", nullable = false)
    private boolean editable;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;
}
