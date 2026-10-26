package com.odontogest.integracion;

import com.odontogest.servicio.RecordatorioServiceASEJ;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecordatorioSchedulerASEJ {

    private final RecordatorioServiceASEJ recordatorioServiceASEJ;
    private ScheduledExecutorService ejecutorASEJ;

    public synchronized void iniciarASEJ() {
        if (ejecutorASEJ != null) {
            return;
        }
        ejecutorASEJ = Executors.newSingleThreadScheduledExecutor(rASEJ -> {
            Thread tASEJ = new Thread(rASEJ, "recordatorios");
            tASEJ.setDaemon(true);
            return tASEJ;
        });
        ejecutorASEJ.scheduleWithFixedDelay(this::cicloASEJ, 1, 15, TimeUnit.MINUTES);
    }

    void cicloASEJ() {
        try {
            recordatorioServiceASEJ.enviarPendientesASEJ(LocalDateTime.now());
        } catch (Exception eASEJ) {
            logASEJ.error("Error en el ciclo de recordatorios", eASEJ);
        }
    }

    public synchronized void detenerASEJ() {
        if (ejecutorASEJ != null) {
            ejecutorASEJ.shutdownNow();
            ejecutorASEJ = null;
        }
    }
}
