package com.odontogest.integracion;

import com.odontogest.evento.ConectividadCambiadaASEJ;
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

@Component
@Slf4j
public class ConectividadServiceASEJ {

    private static final List<URI> DESTINOSASEJ = List.of(
            URI.create("https://www.gstatic.com/generate_204"),
            URI.create("https://1.1.1.1/"));
    private static final Duration ESPERAASEJ = Duration.ofSeconds(2);
    private static final Duration VIGENCIAASEJ = Duration.ofSeconds(30);

    private final ApplicationEventPublisher eventosASEJ;
    private final HttpClient clienteASEJ = HttpClient.newBuilder().connectTimeout(ESPERAASEJ)
            .followRedirects(HttpClient.Redirect.NEVER).build();
    private volatile Boolean ultimoASEJ;
    private volatile Instant verificadoEnASEJ = Instant.EPOCH;
    private ScheduledExecutorService monitorASEJ;

    public ConectividadServiceASEJ(ApplicationEventPublisher eventosASEJ) {
        this.eventosASEJ = eventosASEJ;
    }

    public boolean hayInternetASEJ() {
        Boolean uASEJ = ultimoASEJ;
        if (uASEJ != null && Duration.between(verificadoEnASEJ, Instant.now()).compareTo(VIGENCIAASEJ) < 0) {
            return uASEJ;
        }
        return verificarASEJ();
    }

    public boolean ultimoEstadoASEJ() {
        Boolean uASEJ = ultimoASEJ;
        return uASEJ == null || uASEJ;
    }

    public synchronized boolean verificarASEJ() {
        boolean enLineaASEJ = DESTINOSASEJ.stream().anyMatch(this::respondeASEJ);
        Boolean anteriorASEJ = ultimoASEJ;
        ultimoASEJ = enLineaASEJ;
        verificadoEnASEJ = Instant.now();
        if (anteriorASEJ == null || anteriorASEJ != enLineaASEJ) {
            logASEJ.info("Conectividad a internet: {}", enLineaASEJ ? "disponible" : "sin conexión");
            eventosASEJ.publishEvent(new ConectividadCambiadaASEJ(enLineaASEJ));
        }
        return enLineaASEJ;
    }

    private boolean respondeASEJ(URI destinoASEJ) {
        try {
            HttpRequest peticionASEJ = HttpRequest.newBuilder(destinoASEJ).timeout(ESPERAASEJ)
                    .method("HEAD", HttpRequest.BodyPublishers.noBody()).build();
            clienteASEJ.send(peticionASEJ, HttpResponse.BodyHandlers.discarding());
            return true;
        } catch (InterruptedException eASEJ) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception eASEJ) {
            return false;
        }
    }

    public synchronized void iniciarMonitoreoASEJ() {
        if (monitorASEJ != null) {
            return;
        }
        monitorASEJ = Executors.newSingleThreadScheduledExecutor(rASEJ -> {
            Thread tASEJ = new Thread(rASEJ, "conectividad");
            tASEJ.setDaemon(true);
            return tASEJ;
        });
        monitorASEJ.scheduleWithFixedDelay(this::verificarASEJ, 0, 60, TimeUnit.SECONDS);
    }

    public synchronized void detenerASEJ() {
        if (monitorASEJ != null) {
            monitorASEJ.shutdownNow();
            monitorASEJ = null;
        }
    }
}
