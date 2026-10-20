/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.odontograma;

import java.util.List;
import java.util.function.Function;

/**
 * Índices epidemiológicos CPO-D (permanentes) y ceo-d (deciduas) calculados desde el estado vigente.
 * <p>
 * Cada pieza aporta como máximo 1 a un solo componente, con precedencia:
 * <ol>
 *   <li>algún hallazgo que cuenta como C → cariada;</li>
 *   <li>si no, algún hallazgo que cuenta como O y es ROJO (restauración en mal estado) → cariada;</li>
 *   <li>si no, algún hallazgo P (CPO-D) / E (ceo-d) → perdida / extraída;</li>
 *   <li>si no, algún hallazgo O → obturada.</li>
 * </ol>
 * Se usan los hallazgos visibles de la pieza ({@link EstadoOdontograma#hallazgosVisibles(int)}): una pieza
 * ausente no cuenta como cariada por una caries registrada antes de su extracción, y los hallazgos de rango
 * (p. ej. edéntulo total) cuentan en cada pieza que cubren.
 */
public final class CalculadoraIndices {

    public ResultadoIndice cpod(EstadoOdontograma estado, boolean excluirTercerasMolares) {
        List<PiezaDental> universo = PiezaDental.permanentes().stream()
                .filter(p -> !(excluirTercerasMolares && p.esTerceraMolar()))
                .toList();
        return calcular(estado, universo, TipoHallazgoDef::conteoCpod, ConteoIndice.P);
    }

    public ResultadoIndice ceod(EstadoOdontograma estado) {
        return calcular(estado, PiezaDental.deciduas(), TipoHallazgoDef::conteoCeod, ConteoIndice.E);
    }

    private ResultadoIndice calcular(EstadoOdontograma estado, List<PiezaDental> universo,
                                     Function<TipoHallazgoDef, ConteoIndice> conteo, ConteoIndice perdida) {
        int c = 0;
        int p = 0;
        int o = 0;
        for (PiezaDental pieza : universo) {
            List<HallazgoEvento> h = estado.hallazgosVisibles(pieza.fdi());
            if (h.stream().anyMatch(x -> conteo.apply(x.tipo()) == ConteoIndice.C)) {
                c++;
            } else if (h.stream().anyMatch(x -> conteo.apply(x.tipo()) == ConteoIndice.O && x.color() == ColorHallazgo.ROJO)) {
                c++;
            } else if (h.stream().anyMatch(x -> conteo.apply(x.tipo()) == perdida)) {
                p++;
            } else if (h.stream().anyMatch(x -> conteo.apply(x.tipo()) == ConteoIndice.O)) {
                o++;
            }
        }
        int total = c + p + o;
        return new ResultadoIndice(c, p, o, total, universo.size(), NivelSeveridadOms.de(total));
    }
}
