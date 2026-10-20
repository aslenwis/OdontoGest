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
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Paciente con su historia clínica (tabla {@code paciente}).
 */
@Entity
@Table(name = "paciente")
@Getter
@Setter
@NoArgsConstructor
public class Paciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Correlativo {@code HC-000001}; no cambia nunca. */
    @Column(name = "numero_historia", nullable = false, unique = true, updatable = false)
    private String numeroHistoria;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false)
    private TipoDocumento tipoDocumento;

    @Column(name = "numero_documento")
    private String numeroDocumento;

    @Column(name = "nombres", nullable = false)
    private String nombres;

    @Column(name = "apellido_paterno", nullable = false)
    private String apellidoPaterno;

    @Column(name = "apellido_materno")
    private String apellidoMaterno;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Enumerated(EnumType.STRING)
    @Column(name = "sexo", nullable = false)
    private Sexo sexo;

    @Column(name = "telefono")
    private String telefono;

    @Column(name = "email")
    private String email;

    @Column(name = "direccion")
    private String direccion;

    @Column(name = "ocupacion")
    private String ocupacion;

    @Column(name = "contacto_emergencia_nombre")
    private String contactoEmergenciaNombre;

    @Column(name = "contacto_emergencia_telefono")
    private String contactoEmergenciaTelefono;

    @Column(name = "apoderado_nombre")
    private String apoderadoNombre;

    @Column(name = "apoderado_documento")
    private String apoderadoDocumento;

    @Column(name = "apoderado_parentesco")
    private String apoderadoParentesco;

    @Column(name = "acepta_recordatorios", nullable = false)
    private boolean aceptaRecordatorios = true;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    /** "APELLIDO_P APELLIDO_M, NOMBRES" como se ordena en los listados. */
    public String getNombreCompleto() {
        String apellidos = apellidoMaterno == null || apellidoMaterno.isBlank()
                ? apellidoPaterno : apellidoPaterno + " " + apellidoMaterno;
        return apellidos + ", " + nombres;
    }
}
