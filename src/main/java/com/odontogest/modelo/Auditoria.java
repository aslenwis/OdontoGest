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
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

/**
 * Registro de auditoría (tabla {@code auditoria}, RNF-11): quién hizo qué, sobre qué entidad y cuándo. Inmutable.
 */
@Entity
@Immutable
@Table(name = "auditoria")
@Getter
@NoArgsConstructor
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** {@code null} si la acción no tiene usuario (por ejemplo, un intento de ingreso fallido). */
    @Column(name = "usuario_id")
    private Long usuarioId;

    /** LOGIN, HALLAZGO_CREAR, ODONTOGRAMA_CERRAR, PAGO_REGISTRAR… */
    @Column(name = "accion", nullable = false)
    private String accion;

    @Column(name = "entidad")
    private String entidad;

    @Column(name = "entidad_id")
    private Long entidadId;

    /** JSON con los datos relevantes (nunca contraseñas ni secretos). */
    @Column(name = "detalle")
    private String detalle;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    public static Auditoria de(Long usuarioId, String accion, String entidad, Long entidadId, String detalle,
                               LocalDateTime fecha) {
        Auditoria a = new Auditoria();
        a.usuarioId = usuarioId;
        a.accion = accion;
        a.entidad = entidad;
        a.entidadId = entidadId;
        a.detalle = detalle;
        a.fecha = fecha;
        return a;
    }
}
