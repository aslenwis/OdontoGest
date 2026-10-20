/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest;

import com.odontogest.config.DirectorioDatos;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Base de las pruebas de integración: levanta el contexto de Spring contra una carpeta de datos
 * temporal, así nunca se toca la BD real de {@code ~/OdontoGest}. Todas las subclases comparten
 * el mismo contexto (y la misma BD SQLite temporal).
 */
@SpringBootTest
public abstract class PruebaIntegracion {

    protected static final Path HOME_PRUEBAS;

    static {
        try {
            Path temporal = Files.createTempDirectory("odontogest-test-");
            System.setProperty(DirectorioDatos.PROPIEDAD, temporal.toString());
            HOME_PRUEBAS = DirectorioDatos.inicializar();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
