/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
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

/**
 * Aplica una restauración de respaldo programada (PLAN §5.4). Se ejecuta en {@code Launcher}, antes de levantar
 * Spring, porque la base no se puede reemplazar mientras está abierta.
 * <p>
 * {@code RespaldoService.programarRestauracion} deja los archivos validados en {@code restauracion/} y la marca
 * {@code odontogest.db.restaurar}. Al arrancar, si existe la marca:
 * <ol>
 *   <li>la base actual (con sus archivos {@code -wal}/{@code -shm}), {@code adjuntos/} y {@code clave.key} se mueven
 *       a {@code respaldos/antes-de-restaurar-<fecha>...}: nada se borra;</li>
 *   <li>los archivos restaurados ocupan su lugar;</li>
 *   <li>se eliminan la marca y la carpeta {@code restauracion/}.</li>
 * </ol>
 * Un {@code -wal} viejo junto a la base restaurada la corrompería al abrirla; por eso se mueve con la base anterior.
 */
public final class RestauracionPendiente {

    public static final String MARCA = "odontogest.db.restaurar";
    public static final String CARPETA = "restauracion";
    public static final String BASE = "odontogest.db";
    public static final String CLAVE = "clave.key";
    /**
     * Propiedad de sistema que deja {@code Launcher} cuando se aplicó una restauración: este arranque ocurre antes de
     * que Spring configure el log en archivo, así que el aviso y el registro se hacen después.
     */
    public static final String PROPIEDAD_APLICADA = "odontogest.restauracion.aplicada";

    private static final Logger log = LoggerFactory.getLogger(RestauracionPendiente.class);
    private static final DateTimeFormatter SELLO = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private RestauracionPendiente() {
    }

    /**
     * @return {@code true} si se restauró un respaldo
     * @throws UncheckedIOException si falla a mitad de camino (la marca se conserva para reintentar)
     */
    public static boolean aplicarSiExiste(Path home) {
        Path marca = home.resolve(MARCA);
        if (!Files.exists(marca)) {
            return false;
        }
        Path carpeta = home.resolve(CARPETA);
        Path nueva = carpeta.resolve(BASE);
        try {
            if (!Files.isRegularFile(nueva)) {
                log.error("Restauración cancelada: falta {}", nueva);
                Files.delete(marca);
                borrarCarpeta(carpeta);
                return false;
            }
            Path respaldos = Files.createDirectories(home.resolve(DirectorioDatos.RESPALDOS));
            String previo = "antes-de-restaurar-" + SELLO.format(LocalDateTime.now());

            Path actual = home.resolve(BASE);
            for (String sufijo : new String[]{"", "-wal", "-shm"}) {
                Path archivo = home.resolve(BASE + sufijo);
                if (Files.exists(archivo)) {
                    Files.move(archivo, respaldos.resolve(previo + ".db" + sufijo), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            try {
                Files.move(nueva, actual);
            } catch (IOException e) {
                // Sin base en su lugar Spring crearía una vacía: se devuelve la anterior antes de fallar.
                Path anterior = respaldos.resolve(previo + ".db");
                if (Files.exists(anterior) && !Files.exists(actual)) {
                    Files.move(anterior, actual);
                    for (String sufijo : new String[]{"-wal", "-shm"}) {
                        Path archivo = respaldos.resolve(previo + ".db" + sufijo);
                        if (Files.exists(archivo)) {
                            Files.move(archivo, home.resolve(BASE + sufijo));
                        }
                    }
                }
                throw e;
            }

            Path adjuntosNuevos = carpeta.resolve(DirectorioDatos.ADJUNTOS);
            Path adjuntos = home.resolve(DirectorioDatos.ADJUNTOS);
            if (Files.isDirectory(adjuntosNuevos)) {
                if (Files.exists(adjuntos)) {
                    Files.move(adjuntos, respaldos.resolve(previo + "-adjuntos"));
                }
                Files.move(adjuntosNuevos, adjuntos);
            }
            Path claveNueva = carpeta.resolve(CLAVE);
            if (Files.isRegularFile(claveNueva)) {
                Path clave = home.resolve(CLAVE);
                if (Files.exists(clave)) {
                    Files.move(clave, respaldos.resolve(previo + ".key"), StandardCopyOption.REPLACE_EXISTING);
                }
                Files.move(claveNueva, clave);
            }
            Files.delete(marca);
            borrarCarpeta(carpeta);
            log.info("Respaldo restaurado; la base anterior quedó en {}", respaldos.resolve(previo + ".db"));
            return true;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo restaurar el respaldo programado", e);
        }
    }

    static void borrarCarpeta(Path carpeta) throws IOException {
        if (!Files.exists(carpeta)) {
            return;
        }
        try (Stream<Path> archivos = Files.walk(carpeta)) {
            for (Path p : archivos.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(p);
            }
        }
    }
}
