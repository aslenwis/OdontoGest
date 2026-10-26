package com.odontogest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class EjecutorConfigASEJ {

    @Bean(name = "ejecutorUi", destroyMethod = "shutdownNow")
    public ExecutorService ejecutorUiASEJ() {
        return Executors.newFixedThreadPool(2, fabricaHilosASEJ("odontogest-bg-"));
    }

    @Bean(name = "planificador", destroyMethod = "shutdownNow")
    public ScheduledExecutorService planificadorASEJ() {
        return Executors.newSingleThreadScheduledExecutor(fabricaHilosASEJ("odontogest-plan-"));
    }

    private static ThreadFactory fabricaHilosASEJ(String prefijoASEJ) {
        AtomicInteger contadorASEJ = new AtomicInteger(1);
        return trabajoASEJ -> {
            Thread hiloASEJ = new Thread(trabajoASEJ, prefijoASEJ + contadorASEJ.getAndIncrement());
            hiloASEJ.setDaemon(true);
            return hiloASEJ;
        };
    }
}
