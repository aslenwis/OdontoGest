/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Odontólogo que atiende en la clínica (tabla {@code odontologo}).
 */
@Entity
@Table(name = "odontologo")
@Getter
@Setter
@NoArgsConstructor
public class Odontologo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "nombres", nullable = false)
    private String nombres;

    @Column(name = "apellidos", nullable = false)
    private String apellidos;

    /** N° de colegiatura del Colegio Odontológico del Perú. */
    @Column(name = "cop", nullable = false, unique = true)
    private String cop;

    /** Registro nacional de especialidad. */
    @Column(name = "rne")
    private String rne;

    @Column(name = "especialidad", nullable = false)
    private String especialidad;

    @Column(name = "telefono")
    private String telefono;

    @Column(name = "email")
    private String email;

    @Column(name = "color_agenda", nullable = false)
    private String colorAgenda;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    public String getNombreCompleto() {
        return nombres + " " + apellidos;
    }
}
