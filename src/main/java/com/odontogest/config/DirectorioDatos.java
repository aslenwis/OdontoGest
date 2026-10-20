/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Resuelve y crea la carpeta de datos de la aplicación ({@code ${odontogest.home}}).
 * <p>
 * Se ejecuta antes de Spring: fija la propiedad de sistema {@value #PROPIEDAD} que usan
 * {@code application.properties} (BD y log). Por defecto es {@code ${user.home}/OdontoGest};
 * se sobrescribe con {@code -Dodontogest.home=...}. Nunca se escribe en la carpeta de instalación.
 */
public final class DirectorioDatos {

    public static final String PROPIEDAD = "odontogest.home";
    public static final String ADJUNTOS = "adjuntos";
    public static final String RESPALDOS = "respaldos";
    public static final String LOGS = "logs";

    private DirectorioDatos() {
    }

    /**
     * Crea la carpeta de datos y sus subcarpetas si no existen (idempotente).
     *
     * @return ruta absoluta de la carpeta de datos
     */
    public static Path inicializar() {
        String configurada = System.getProperty(PROPIEDAD);
        Path home = (configurada == null || configurada.isBlank())
                ? Path.of(System.getProperty("user.home"), "OdontoGest")
                : Path.of(configurada);
        home = home.toAbsolutePath().normalize();
        try {
            Files.createDirectories(home);
            Files.createDirectories(home.resolve(ADJUNTOS));
            Files.createDirectories(home.resolve(RESPALDOS));
            Files.createDirectories(home.resolve(LOGS));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo crear la carpeta de datos " + home, e);
        }
        // Barras normales: la ruta se interpola dentro de la URL JDBC de SQLite.
        System.setProperty(PROPIEDAD, home.toString().replace('\\', '/'));
        return home;
    }

    /** Carpeta de datos ya inicializada. */
    public static Path home() {
        String valor = System.getProperty(PROPIEDAD);
        if (valor == null) {
            throw new IllegalStateException("DirectorioDatos.inicializar() no se ha ejecutado");
        }
        return Path.of(valor);
    }
}
