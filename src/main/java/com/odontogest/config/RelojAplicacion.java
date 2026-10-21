/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.config;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Reloj de la aplicación: los servicios toman la fecha y hora de aquí ({@code LocalDateTime.now(reloj)}) y no del
 * sistema directamente.
 * <p>
 * En uso normal da la hora real. El seeder de datos demo lo fija en momentos del pasado para registrar atenciones,
 * odontogramas, pagos y citas con fechas realistas <b>a través de los mismos servicios</b> que la interfaz: así se
 * cumplen las validaciones (p. ej. no programar citas en el pasado) y los hashes de los odontogramas son válidos.
 * Las pruebas también lo usan para fijar "hoy".
 */
@Component
public class RelojAplicacion extends Clock {

    private final ZoneId zona;
    /** Momento fijado; {@code null} = hora real. */
    private volatile LocalDateTime fijado;

    public RelojAplicacion() {
        this(ZoneId.systemDefault());
    }

    RelojAplicacion(ZoneId zona) {
        this.zona = zona;
    }

    /** Detiene el reloj en {@code momento} hasta {@link #liberar()} o el siguiente {@code fijar}. */
    public void fijar(LocalDateTime momento) {
        this.fijado = momento;
    }

    /** Vuelve a la hora real. */
    public void liberar() {
        this.fijado = null;
    }

    public boolean esHoraReal() {
        return fijado == null;
    }

    @Override
    public ZoneId getZone() {
        return zona;
    }

    @Override
    public Clock withZone(ZoneId otraZona) {
        if (otraZona.equals(zona)) {
            return this;
        }
        LocalDateTime f = fijado;
        return f == null ? Clock.system(otraZona) : Clock.fixed(f.atZone(zona).toInstant(), otraZona);
    }

    @Override
    public Instant instant() {
        LocalDateTime f = fijado;
        return f == null ? Instant.now() : f.atZone(zona).toInstant();
    }
}
