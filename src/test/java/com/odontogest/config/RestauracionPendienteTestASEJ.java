package com.odontogest.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class RestauracionPendienteTestASEJ {

    @TempDir
    Path homeASEJ;

    private void escribirASEJ(String rutaASEJ, String contenidoASEJ) throws IOException {
        Path pASEJ = homeASEJ.resolve(rutaASEJ);
        Files.createDirectories(pASEJ.getParent());
        Files.writeString(pASEJ, contenidoASEJ);
    }

    private List<String> respaldosASEJ() throws IOException {
        try (Stream<Path> sASEJ = Files.list(homeASEJ.resolve("respaldos"))) {
            return sASEJ.map(pASEJ -> pASEJ.getFileName().toString()).sorted().toList();
        }
    }

    @Test
    void sinMarca_noHaceNadaASEJ() throws IOException {
        escribirASEJ("odontogest.db", "actual");
        escribirASEJ("restauracion/odontogest.db", "nueva");
        assertThat(RestauracionPendienteASEJ.aplicarSiExisteASEJ(homeASEJ)).isFalse();
        assertThat(Files.readString(homeASEJ.resolve("odontogest.db"))).isEqualTo("actual");
    }

    @Test
    void conMarca_reemplazaBaseAdjuntosYClave_yConservaLoAnteriorASEJ() throws IOException {
        escribirASEJ("odontogest.db", "actual");
        escribirASEJ("odontogest.db-wal", "wal-viejo");
        escribirASEJ("odontogest.db-shm", "shm");
        escribirASEJ("adjuntos/1/radiografia.png", "vieja");
        escribirASEJ("clave.key", "clave-actual");
        escribirASEJ("restauracion/odontogest.db", "nueva");
        escribirASEJ("restauracion/adjuntos/7/foto.jpg", "restaurada");
        escribirASEJ("restauracion/clave.key", "clave-restaurada");
        escribirASEJ(RestauracionPendienteASEJ.MARCAASEJ, "C:/respaldo.zip");

        assertThat(RestauracionPendienteASEJ.aplicarSiExisteASEJ(homeASEJ)).isTrue();

        assertThat(Files.readString(homeASEJ.resolve("odontogest.db"))).isEqualTo("nueva");
        assertThat(homeASEJ.resolve("odontogest.db-wal")).as("un WAL viejo corrompería la base restaurada").doesNotExist();
        assertThat(homeASEJ.resolve("odontogest.db-shm")).doesNotExist();
        assertThat(Files.readString(homeASEJ.resolve("adjuntos/7/foto.jpg"))).isEqualTo("restaurada");
        assertThat(homeASEJ.resolve("adjuntos/1")).doesNotExist();
        assertThat(Files.readString(homeASEJ.resolve("clave.key"))).isEqualTo("clave-restaurada");
        assertThat(homeASEJ.resolve(RestauracionPendienteASEJ.MARCAASEJ)).doesNotExist();
        assertThat(homeASEJ.resolve("restauracion")).doesNotExist();

        List<String> previosASEJ = respaldosASEJ();
        assertThat(previosASEJ).hasSize(5).allMatch(nASEJ -> nASEJ.startsWith("antes-de-restaurar-"));
        String selloASEJ = previosASEJ.get(0).substring(0, "antes-de-restaurar-yyyyMMdd-HHmmss".length());
        assertThat(Files.readString(homeASEJ.resolve("respaldos/" + selloASEJ + ".db"))).isEqualTo("actual");
        assertThat(Files.readString(homeASEJ.resolve("respaldos/" + selloASEJ + ".db-wal"))).isEqualTo("wal-viejo");
        assertThat(Files.readString(homeASEJ.resolve("respaldos/" + selloASEJ + ".key"))).isEqualTo("clave-actual");
        assertThat(Files.readString(homeASEJ.resolve("respaldos/" + selloASEJ + "-adjuntos/1/radiografia.png"))).isEqualTo("vieja");
    }

    @Test
    void sinBaseActual_soloColocaLaRestauradaASEJ() throws IOException {
        escribirASEJ("restauracion/odontogest.db", "nueva");
        escribirASEJ(RestauracionPendienteASEJ.MARCAASEJ, "");
        assertThat(RestauracionPendienteASEJ.aplicarSiExisteASEJ(homeASEJ)).isTrue();
        assertThat(Files.readString(homeASEJ.resolve("odontogest.db"))).isEqualTo("nueva");
        assertThat(respaldosASEJ()).isEmpty();
    }

    @Test
    void marcaSinBaseRestaurada_seDescartaSinTocarLaActualASEJ() throws IOException {
        escribirASEJ("odontogest.db", "actual");
        escribirASEJ("restauracion/clave.key", "x");
        escribirASEJ(RestauracionPendienteASEJ.MARCAASEJ, "");
        assertThat(RestauracionPendienteASEJ.aplicarSiExisteASEJ(homeASEJ)).isFalse();
        assertThat(Files.readString(homeASEJ.resolve("odontogest.db"))).isEqualTo("actual");
        assertThat(homeASEJ.resolve(RestauracionPendienteASEJ.MARCAASEJ)).doesNotExist();
        assertThat(homeASEJ.resolve("restauracion")).doesNotExist();
    }
}
