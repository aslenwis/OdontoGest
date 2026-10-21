/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.integracion;

import com.odontogest.servicio.RecordatorioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Programa el envío de recordatorios (PLAN §5.2.6): un hilo daemon cada 15 minutos, el primero 1 minuto después del
 * arranque. Lo inicia la aplicación de escritorio (las pruebas no). Ningún error detiene la programación.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RecordatorioScheduler {

    private final RecordatorioService recordatorioService;
    private ScheduledExecutorService ejecutor;

    public synchronized void iniciar() {
        if (ejecutor != null) {
            return;
        }
        ejecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "recordatorios");
            t.setDaemon(true);
            return t;
        });
        ejecutor.scheduleWithFixedDelay(this::ciclo, 1, 15, TimeUnit.MINUTES);
    }

    void ciclo() {
        try {
            recordatorioService.enviarPendientes(LocalDateTime.now());
        } catch (Exception e) {
            log.error("Error en el ciclo de recordatorios", e);
        }
    }

    public synchronized void detener() {
        if (ejecutor != null) {
            ejecutor.shutdownNow();
            ejecutor = null;
        }
    }
}
