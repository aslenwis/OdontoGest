/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.integracion;

import com.odontogest.evento.ConectividadCambiada;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * ¿Hay internet? (RNF-03): petición HEAD con 2 s de espera a direcciones públicas muy estables; basta que una
 * responda. El resultado se guarda 30 s para no consultar en cada uso.
 * <p>
 * {@link #iniciarMonitoreo()} (lo llama la aplicación al arrancar, no las pruebas) revisa cada minuto y publica
 * {@link ConectividadCambiada} cuando el estado cambia, para que la interfaz muestre u oculte el aviso.
 */
@Component
@Slf4j
public class ConectividadService {

    private static final List<URI> DESTINOS = List.of(
            URI.create("https://www.gstatic.com/generate_204"),
            URI.create("https://1.1.1.1/"));
    private static final Duration ESPERA = Duration.ofSeconds(2);
    private static final Duration VIGENCIA = Duration.ofSeconds(30);

    private final ApplicationEventPublisher eventos;
    private final HttpClient cliente = HttpClient.newBuilder().connectTimeout(ESPERA)
            .followRedirects(HttpClient.Redirect.NEVER).build();
    private volatile Boolean ultimo;
    private volatile Instant verificadoEn = Instant.EPOCH;
    private ScheduledExecutorService monitor;

    public ConectividadService(ApplicationEventPublisher eventos) {
        this.eventos = eventos;
    }

    public boolean hayInternet() {
        Boolean u = ultimo;
        if (u != null && Duration.between(verificadoEn, Instant.now()).compareTo(VIGENCIA) < 0) {
            return u;
        }
        return verificar();
    }

    /** Último estado conocido sin consultar la red ({@code true} mientras no se haya verificado). */
    public boolean ultimoEstado() {
        Boolean u = ultimo;
        return u == null || u;
    }

    /** Consulta la red ahora y publica el cambio si lo hubo. */
    public synchronized boolean verificar() {
        boolean enLinea = DESTINOS.stream().anyMatch(this::responde);
        Boolean anterior = ultimo;
        ultimo = enLinea;
        verificadoEn = Instant.now();
        if (anterior == null || anterior != enLinea) {
            log.info("Conectividad a internet: {}", enLinea ? "disponible" : "sin conexión");
            eventos.publishEvent(new ConectividadCambiada(enLinea));
        }
        return enLinea;
    }

    private boolean responde(URI destino) {
        try {
            HttpRequest peticion = HttpRequest.newBuilder(destino).timeout(ESPERA)
                    .method("HEAD", HttpRequest.BodyPublishers.noBody()).build();
            cliente.send(peticion, HttpResponse.BodyHandlers.discarding());
            return true;                                      // cualquier respuesta HTTP indica conexión
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public synchronized void iniciarMonitoreo() {
        if (monitor != null) {
            return;
        }
        monitor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "conectividad");
            t.setDaemon(true);
            return t;
        });
        monitor.scheduleWithFixedDelay(this::verificar, 0, 60, TimeUnit.SECONDS);
    }

    public synchronized void detener() {
        if (monitor != null) {
            monitor.shutdownNow();
            monitor = null;
        }
    }
}
