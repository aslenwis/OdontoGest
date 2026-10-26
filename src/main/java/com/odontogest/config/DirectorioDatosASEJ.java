package com.odontogest.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DirectorioDatosASEJ {

    public static final String PROPIEDADASEJ = "odontogest.home";
    public static final String ADJUNTOSASEJ = "adjuntos";
    public static final String RESPALDOSASEJ = "respaldos";
    public static final String LOGSASEJ = "logs";

    private DirectorioDatosASEJ() {
    }

    public static Path inicializarASEJ() {
        String configuradaASEJ = System.getProperty(PROPIEDADASEJ);
        Path homeASEJ = (configuradaASEJ == null || configuradaASEJ.isBlank())
                ? Path.of(System.getProperty("user.home"), "OdontoGest")
                : Path.of(configuradaASEJ);
        homeASEJ = homeASEJ.toAbsolutePath().normalize();
        try {
            Files.createDirectories(homeASEJ);
            Files.createDirectories(homeASEJ.resolve(ADJUNTOSASEJ));
            Files.createDirectories(homeASEJ.resolve(RESPALDOSASEJ));
            Files.createDirectories(homeASEJ.resolve(LOGSASEJ));
        } catch (IOException eASEJ) {
            throw new UncheckedIOException("No se pudo crear la carpeta de datos " + homeASEJ, eASEJ);
        }
        System.setProperty(PROPIEDADASEJ, homeASEJ.toString().replace('\\', '/'));
        return homeASEJ;
    }

    public static Path homeASEJ() {
        String valorASEJ = System.getProperty(PROPIEDADASEJ);
        if (valorASEJ == null) {
            throw new IllegalStateException("DirectorioDatos.inicializar() no se ha ejecutado");
        }
        return Path.of(valorASEJ);
    }
}
