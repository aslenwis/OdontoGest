/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Intercambio de archivos al arrancar con una restauración programada (PLAN §5.4): lo anterior se conserva en
 * {@code respaldos/} y nada queda a medias.
 */
class RestauracionPendienteTest {

    @TempDir
    Path home;

    private void escribir(String ruta, String contenido) throws IOException {
        Path p = home.resolve(ruta);
        Files.createDirectories(p.getParent());
        Files.writeString(p, contenido);
    }

    private List<String> respaldos() throws IOException {
        try (Stream<Path> s = Files.list(home.resolve("respaldos"))) {
            return s.map(p -> p.getFileName().toString()).sorted().toList();
        }
    }

    @Test
    void sinMarca_noHaceNada() throws IOException {
        escribir("odontogest.db", "actual");
        escribir("restauracion/odontogest.db", "nueva");
        assertThat(RestauracionPendiente.aplicarSiExiste(home)).isFalse();
        assertThat(Files.readString(home.resolve("odontogest.db"))).isEqualTo("actual");
    }

    @Test
    void conMarca_reemplazaBaseAdjuntosYClave_yConservaLoAnterior() throws IOException {
        escribir("odontogest.db", "actual");
        escribir("odontogest.db-wal", "wal-viejo");
        escribir("odontogest.db-shm", "shm");
        escribir("adjuntos/1/radiografia.png", "vieja");
        escribir("clave.key", "clave-actual");
        escribir("restauracion/odontogest.db", "nueva");
        escribir("restauracion/adjuntos/7/foto.jpg", "restaurada");
        escribir("restauracion/clave.key", "clave-restaurada");
        escribir(RestauracionPendiente.MARCA, "C:/respaldo.zip");

        assertThat(RestauracionPendiente.aplicarSiExiste(home)).isTrue();

        assertThat(Files.readString(home.resolve("odontogest.db"))).isEqualTo("nueva");
        assertThat(home.resolve("odontogest.db-wal")).as("un WAL viejo corrompería la base restaurada").doesNotExist();
        assertThat(home.resolve("odontogest.db-shm")).doesNotExist();
        assertThat(Files.readString(home.resolve("adjuntos/7/foto.jpg"))).isEqualTo("restaurada");
        assertThat(home.resolve("adjuntos/1")).doesNotExist();
        assertThat(Files.readString(home.resolve("clave.key"))).isEqualTo("clave-restaurada");
        assertThat(home.resolve(RestauracionPendiente.MARCA)).doesNotExist();
        assertThat(home.resolve("restauracion")).doesNotExist();

        List<String> previos = respaldos();
        assertThat(previos).hasSize(5).allMatch(n -> n.startsWith("antes-de-restaurar-"));
        String sello = previos.get(0).substring(0, "antes-de-restaurar-yyyyMMdd-HHmmss".length());
        assertThat(Files.readString(home.resolve("respaldos/" + sello + ".db"))).isEqualTo("actual");
        assertThat(Files.readString(home.resolve("respaldos/" + sello + ".db-wal"))).isEqualTo("wal-viejo");
        assertThat(Files.readString(home.resolve("respaldos/" + sello + ".key"))).isEqualTo("clave-actual");
        assertThat(Files.readString(home.resolve("respaldos/" + sello + "-adjuntos/1/radiografia.png"))).isEqualTo("vieja");
    }

    @Test
    void sinBaseActual_soloColocaLaRestaurada() throws IOException {
        escribir("restauracion/odontogest.db", "nueva");
        escribir(RestauracionPendiente.MARCA, "");
        assertThat(RestauracionPendiente.aplicarSiExiste(home)).isTrue();
        assertThat(Files.readString(home.resolve("odontogest.db"))).isEqualTo("nueva");
        assertThat(respaldos()).isEmpty();
    }

    @Test
    void marcaSinBaseRestaurada_seDescartaSinTocarLaActual() throws IOException {
        escribir("odontogest.db", "actual");
        escribir("restauracion/clave.key", "x");
        escribir(RestauracionPendiente.MARCA, "");
        assertThat(RestauracionPendiente.aplicarSiExiste(home)).isFalse();
        assertThat(Files.readString(home.resolve("odontogest.db"))).isEqualTo("actual");
        assertThat(home.resolve(RestauracionPendiente.MARCA)).doesNotExist();
        assertThat(home.resolve("restauracion")).doesNotExist();
    }
}
