package com.odontogest.servicio.dto;

import java.time.LocalDateTime;
import java.util.Locale;

public record RespaldoDtoASEJ(String nombreASEJ, String rutaASEJ, LocalDateTime fechaASEJ, long tamanioBytesASEJ, boolean automaticoASEJ) {

    public String tamanioASEJ() {
        if (tamanioBytesASEJ < 1024) {
            return tamanioBytesASEJ + " B";
        }
        if (tamanioBytesASEJ < 1024 * 1024) {
            return String.format(Locale.ROOT, "%.1f KB", tamanioBytesASEJ / 1024.0);
        }
        return String.format(Locale.ROOT, "%.1f MB", tamanioBytesASEJ / (1024.0 * 1024));
    }
}
