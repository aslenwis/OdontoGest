/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Hilos de fondo de la aplicación. Todos son <i>daemon</i> para no impedir el cierre de la JVM.
 */
@Configuration
public class EjecutorConfig {

    /** Ejecuta las tareas lanzadas desde la UI ({@code TareaUi}): BD, archivos y red. */
    @Bean(name = "ejecutorUi", destroyMethod = "shutdownNow")
    public ExecutorService ejecutorUi() {
        return Executors.newFixedThreadPool(2, fabricaHilos("odontogest-bg-"));
    }

    /** Tareas periódicas (recordatorios, Fase 7). */
    @Bean(name = "planificador", destroyMethod = "shutdownNow")
    public ScheduledExecutorService planificador() {
        return Executors.newSingleThreadScheduledExecutor(fabricaHilos("odontogest-plan-"));
    }

    private static ThreadFactory fabricaHilos(String prefijo) {
        AtomicInteger contador = new AtomicInteger(1);
        return trabajo -> {
            Thread hilo = new Thread(trabajo, prefijo + contador.getAndIncrement());
            hilo.setDaemon(true);
            return hilo;
        };
    }
}
