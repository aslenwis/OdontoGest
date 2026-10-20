/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.odontograma;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Núcleo del odontograma basado en eventos: el estado visible en una fecha es la reducción
 * ordenada de los hallazgos registrados hasta esa fecha.
 * <p>
 * Cada hallazgo ocupa una o varias {@link ClaveCapa}; ante dos hallazgos con la misma clave gana el
 * más reciente (por fecha de registro y, a igual fecha, por id). Los anulados no participan.
 * Complejidad {@code O(n log n)} por el ordenamiento.
 */
public final class ProyectorOdontograma {

    /**
     * @param eventos hallazgos de todos los odontogramas del paciente (cerrados y borrador)
     * @param hasta   instante de corte inclusivo; {@code null} = sin límite
     */
    public EstadoOdontograma proyectar(List<HallazgoEvento> eventos, LocalDateTime hasta) {
        Map<ClaveCapa, HallazgoEvento> vigentes = new LinkedHashMap<>();
        eventos.stream()
                .filter(e -> !e.anulado())
                .filter(e -> hasta == null || !e.registradoEn().isAfter(hasta))
                .sorted(Comparator.comparing(HallazgoEvento::registradoEn).thenComparingLong(HallazgoEvento::id))
                .forEach(e -> claves(e).forEach(k -> vigentes.put(k, e)));
        return EstadoOdontograma.desde(vigentes);
    }

    static List<ClaveCapa> claves(HallazgoEvento e) {
        String capa = e.tipo().grupoExclusion();
        return switch (e.tipo().representacion()) {
            case SUPERFICIE -> e.superficies().stream()
                    .map(s -> new ClaveCapa(e.pieza().fdi(), e.pieza().fdi(), s, capa))
                    .toList();
            case PIEZA -> List.of(new ClaveCapa(e.pieza().fdi(), e.pieza().fdi(), null, capa));
            case ENTRE_PIEZAS, RANGO_PIEZAS -> {
                int a = Math.min(e.pieza().fdi(), e.piezaFin().fdi());
                int b = Math.max(e.pieza().fdi(), e.piezaFin().fdi());
                yield List.of(new ClaveCapa(a, b, null, capa));
            }
        };
    }
}
