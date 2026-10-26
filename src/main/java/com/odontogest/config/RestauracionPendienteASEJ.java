package com.odontogest.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.stream.Stream;

public final class RestauracionPendienteASEJ {

    public static final String MARCAASEJ = "odontogest.db.restaurar";
    public static final String CARPETAASEJ = "restauracion";
    public static final String BASEASEJ = "odontogest.db";
    public static final String CLAVEASEJ = "clave.key";
    public static final String PROPIEDAD_APLICADAASEJ = "odontogest.restauracion.aplicada";

    private static final Logger logASEJ = LoggerFactory.getLogger(RestauracionPendienteASEJ.class);
    private static final DateTimeFormatter SELLOASEJ = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private RestauracionPendienteASEJ() {
    }

    public static boolean aplicarSiExisteASEJ(Path homeASEJ) {
        Path marcaASEJ = homeASEJ.resolve(MARCAASEJ);
        if (!Files.exists(marcaASEJ)) {
            return false;
        }
        Path carpetaASEJ = homeASEJ.resolve(CARPETAASEJ);
        Path nuevaASEJ = carpetaASEJ.resolve(BASEASEJ);
        try {
            if (!Files.isRegularFile(nuevaASEJ)) {
                logASEJ.error("Restauración cancelada: falta {}", nuevaASEJ);
                Files.delete(marcaASEJ);
                borrarCarpetaASEJ(carpetaASEJ);
                return false;
            }
            Path respaldosASEJ = Files.createDirectories(homeASEJ.resolve(DirectorioDatosASEJ.RESPALDOSASEJ));
            String previoASEJ = "antes-de-restaurar-" + SELLOASEJ.format(LocalDateTime.now());

            Path actualASEJ = homeASEJ.resolve(BASEASEJ);
            for (String sufijoASEJ : new String[]{"", "-wal", "-shm"}) {
                Path archivoASEJ = homeASEJ.resolve(BASEASEJ + sufijoASEJ);
                if (Files.exists(archivoASEJ)) {
                    Files.move(archivoASEJ, respaldosASEJ.resolve(previoASEJ + ".db" + sufijoASEJ), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            try {
                Files.move(nuevaASEJ, actualASEJ);
            } catch (IOException eASEJ) {
                Path anteriorASEJ = respaldosASEJ.resolve(previoASEJ + ".db");
                if (Files.exists(anteriorASEJ) && !Files.exists(actualASEJ)) {
                    Files.move(anteriorASEJ, actualASEJ);
                    for (String sufijoASEJ : new String[]{"-wal", "-shm"}) {
                        Path archivoASEJ = respaldosASEJ.resolve(previoASEJ + ".db" + sufijoASEJ);
                        if (Files.exists(archivoASEJ)) {
                            Files.move(archivoASEJ, homeASEJ.resolve(BASEASEJ + sufijoASEJ));
                        }
                    }
                }
                throw eASEJ;
            }

            Path adjuntosNuevosASEJ = carpetaASEJ.resolve(DirectorioDatosASEJ.ADJUNTOSASEJ);
            Path adjuntosASEJ = homeASEJ.resolve(DirectorioDatosASEJ.ADJUNTOSASEJ);
            if (Files.isDirectory(adjuntosNuevosASEJ)) {
                if (Files.exists(adjuntosASEJ)) {
                    Files.move(adjuntosASEJ, respaldosASEJ.resolve(previoASEJ + "-adjuntos"));
                }
                Files.move(adjuntosNuevosASEJ, adjuntosASEJ);
            }
            Path claveNuevaASEJ = carpetaASEJ.resolve(CLAVEASEJ);
            if (Files.isRegularFile(claveNuevaASEJ)) {
                Path claveASEJ = homeASEJ.resolve(CLAVEASEJ);
                if (Files.exists(claveASEJ)) {
                    Files.move(claveASEJ, respaldosASEJ.resolve(previoASEJ + ".key"), StandardCopyOption.REPLACE_EXISTING);
                }
                Files.move(claveNuevaASEJ, claveASEJ);
            }
            Files.delete(marcaASEJ);
            borrarCarpetaASEJ(carpetaASEJ);
            logASEJ.info("Respaldo restaurado; la base anterior quedó en {}", respaldosASEJ.resolve(previoASEJ + ".db"));
            return true;
        } catch (IOException eASEJ) {
            throw new UncheckedIOException("No se pudo restaurar el respaldo programado", eASEJ);
        }
    }

    static void borrarCarpetaASEJ(Path carpetaASEJ) throws IOException {
        if (!Files.exists(carpetaASEJ)) {
            return;
        }
        try (Stream<Path> archivosASEJ = Files.walk(carpetaASEJ)) {
            for (Path pASEJ : archivosASEJ.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(pASEJ);
            }
        }
    }
}
