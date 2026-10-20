/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.agenda;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Busca el siguiente hueco libre de una duración dada por barrido de intervalos.
 * <pre>
 * para cada día d desde fecha(desde) hasta fecha(desde) + diasMaximos:
 *     para cada bloque B del horario de ese día (por hora de inicio):
 *         cursor = max(B.inicio, redondearArriba(desde, paso))
 *         para cada ocupado o que se solapa con B, por inicio:
 *             si o.inicio − cursor ≥ duracion → [cursor, cursor + duracion)
 *             cursor = max(cursor, redondearArriba(o.fin, paso))
 *         si B.fin − cursor ≥ duracion → [cursor, cursor + duracion)
 * </pre>
 * Complejidad {@code O(D · k log k)} con {@code k} citas por día.
 */
public final class BuscadorHuecos {

    public Optional<Intervalo> siguienteHueco(LocalDateTime desde, Duration duracion, Duration paso,
                                              List<BloqueHorario> horario, List<Intervalo> ocupados, int diasMaximos) {
        if (duracion.isZero() || duracion.isNegative() || paso.isZero() || paso.isNegative()) {
            throw new IllegalArgumentException("La duración y el paso deben ser positivos");
        }
        LocalDateTime minimo = redondearArriba(desde, paso);
        LocalDate primerDia = desde.toLocalDate();
        List<BloqueHorario> bloques = horario.stream().sorted(Comparator.comparing(BloqueHorario::inicio)).toList();

        for (int i = 0; i <= diasMaximos; i++) {
            LocalDate dia = primerDia.plusDays(i);
            for (BloqueHorario bloque : bloques) {
                if (bloque.dia() != dia.getDayOfWeek()) {
                    continue;
                }
                Intervalo b = bloque.en(dia);
                LocalDateTime cursor = max(b.inicio(), minimo);
                List<Intervalo> delBloque = ocupados.stream()
                        .filter(o -> o.solapa(b))
                        .sorted(Comparator.comparing(Intervalo::inicio))
                        .toList();
                for (Intervalo o : delBloque) {
                    if (!Duration.between(cursor, o.inicio()).minus(duracion).isNegative()) {
                        return Optional.of(Intervalo.de(cursor, duracion));
                    }
                    cursor = max(cursor, redondearArriba(o.fin(), paso));
                }
                if (!Duration.between(cursor, b.fin()).minus(duracion).isNegative()) {
                    return Optional.of(Intervalo.de(cursor, duracion));
                }
            }
        }
        return Optional.empty();
    }

    /** Redondea hacia arriba al múltiplo de {@code paso} contado desde la medianoche. */
    static LocalDateTime redondearArriba(LocalDateTime t, Duration paso) {
        LocalDateTime medianoche = t.toLocalDate().atStartOfDay();
        long nanos = Duration.between(medianoche, t).toNanos();
        long pasoNanos = paso.toNanos();
        long redondeado = ((nanos + pasoNanos - 1) / pasoNanos) * pasoNanos;
        return medianoche.plusNanos(redondeado);
    }

    private static LocalDateTime max(LocalDateTime a, LocalDateTime b) {
        return a.isAfter(b) ? a : b;
    }
}
