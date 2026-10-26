package com.odontogest.config;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class RelojAplicacionASEJ extends Clock {

    private final ZoneId zonaASEJ;
    private volatile LocalDateTime fijadoASEJ;

    public RelojAplicacionASEJ() {
        this(ZoneId.systemDefault());
    }

    RelojAplicacionASEJ(ZoneId zonaASEJ) {
        this.zonaASEJ = zonaASEJ;
    }

    public void fijarASEJ(LocalDateTime momentoASEJ) {
        this.fijadoASEJ = momentoASEJ;
    }

    public void liberarASEJ() {
        this.fijadoASEJ = null;
    }

    public boolean esHoraRealASEJ() {
        return fijadoASEJ == null;
    }

    @Override
    public ZoneId getZone() {
        return zonaASEJ;
    }

    @Override
    public Clock withZone(ZoneId otraZonaASEJ) {
        if (otraZonaASEJ.equals(zonaASEJ)) {
            return this;
        }
        LocalDateTime fASEJ = fijadoASEJ;
        return fASEJ == null ? Clock.system(otraZonaASEJ) : Clock.fixed(fASEJ.atZone(zonaASEJ).toInstant(), otraZonaASEJ);
    }

    @Override
    public Instant instant() {
        LocalDateTime fASEJ = fijadoASEJ;
        return fASEJ == null ? Instant.now() : fASEJ.atZone(zonaASEJ).toInstant();
    }
}
