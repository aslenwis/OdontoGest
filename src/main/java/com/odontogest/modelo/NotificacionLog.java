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
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

/**
 * Registro de cada intento de recordatorio por correo (tabla {@code notificacion_log}, RF-AGE-06).
 */
@Entity
@Immutable
@Table(name = "notificacion_log")
@Getter
@Setter
@NoArgsConstructor
public class NotificacionLog {

    /** Valores de {@code estado}. */
    public enum Estado {
        ENVIADO, FALLIDO, OMITIDO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "cita_id")
    private Long citaId;

    @Column(name = "canal", nullable = false)
    private String canal = "EMAIL";

    @Column(name = "destino", nullable = false)
    private String destino;

    @Column(name = "estado", nullable = false)
    @Enumerated(EnumType.STRING)
    private Estado estado;

    @Column(name = "detalle")
    private String detalle;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    public static NotificacionLog de(Long citaId, String destino, Estado estado, String detalle, LocalDateTime fecha) {
        NotificacionLog n = new NotificacionLog();
        n.citaId = citaId;
        n.destino = destino;
        n.estado = estado;
        n.detalle = detalle == null || detalle.length() <= 500 ? detalle : detalle.substring(0, 500);
        n.fecha = fecha;
        return n;
    }
}
