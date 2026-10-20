/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.tratamiento;

import com.odontogest.dominio.odontograma.EstadoOdontograma;
import com.odontogest.dominio.odontograma.HallazgoEvento;
import com.odontogest.dominio.odontograma.PiezaDental;
import com.odontogest.dominio.odontograma.Superficie;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Genera ítems de plan de tratamiento a partir de los hallazgos vigentes y reglas configurables.
 * <ol>
 *   <li>Cada regla general produce un ítem sin pieza.</li>
 *   <li>Cada hallazgo vigente toma la primera regla candidata por prioridad (tipo, estado, tipo radicular
 *       y número de superficies); sin regla, no genera ítem (p. ej. restauración en buen estado).</li>
 *   <li>Se desduplica por (procedimiento, pieza).</li>
 *   <li>Precio: por superficie = precio × |superficies|; por pieza, arcada, boca o sesión = precio.</li>
 *   <li>Orden: fase y luego pieza en orden de dibujo (los ítems sin pieza primero).</li>
 * </ol>
 */
public final class MotorSugerencias {

    /**
     * @param reglas     reglas activas (se ordenan aquí por prioridad)
     * @param tarifario  procedimientos por id
     */
    public List<ItemSugerido> sugerir(EstadoOdontograma estado, List<ReglaSugerenciaDef> reglas,
                                      Map<Long, ProcedimientoDef> tarifario) {
        List<ReglaSugerenciaDef> ordenadas = reglas.stream()
                .sorted(Comparator.comparingInt(ReglaSugerenciaDef::prioridad).thenComparingLong(ReglaSugerenciaDef::id))
                .toList();

        List<ItemSugerido> items = new ArrayList<>();
        for (ReglaSugerenciaDef regla : ordenadas) {
            if (regla.esGeneral()) {
                items.add(item(regla, procedimiento(tarifario, regla), null));
            }
        }
        for (HallazgoEvento h : hallazgosConsiderados(estado)) {
            Optional<ReglaSugerenciaDef> regla = ordenadas.stream().filter(r -> r.aplicaA(h)).findFirst();
            regla.ifPresent(r -> items.add(item(r, procedimiento(tarifario, r), h)));
        }

        Set<String> vistos = new HashSet<>();
        List<ItemSugerido> unicos = new ArrayList<>();
        for (ItemSugerido i : items) {
            if (vistos.add(i.procedimientoId() + "@" + i.piezaFdi())) {
                unicos.add(i);
            }
        }
        unicos.sort(Comparator.comparingInt(ItemSugerido::fase)
                .thenComparingInt(i -> i.piezaFdi() == null ? -1 : PiezaDental.de(i.piezaFdi()).ordenDibujo()));
        return List.copyOf(unicos);
    }

    /** Hallazgos visibles de cada pieza más los de rango, sin repetir y en orden de registro. */
    private static List<HallazgoEvento> hallazgosConsiderados(EstadoOdontograma estado) {
        Set<HallazgoEvento> hallazgos = new LinkedHashSet<>();
        for (int fdi : estado.piezas().keySet()) {
            hallazgos.addAll(estado.hallazgosVisibles(fdi));
        }
        hallazgos.addAll(estado.rangos());
        List<HallazgoEvento> lista = new ArrayList<>(hallazgos);
        lista.sort(Comparator.comparing(HallazgoEvento::registradoEn).thenComparingLong(HallazgoEvento::id));
        return lista;
    }

    private static ProcedimientoDef procedimiento(Map<Long, ProcedimientoDef> tarifario, ReglaSugerenciaDef regla) {
        return Objects.requireNonNull(tarifario.get(regla.procedimientoId()),
                () -> "La regla " + regla.id() + " apunta a un procedimiento inexistente: " + regla.procedimientoId());
    }

    private static ItemSugerido item(ReglaSugerenciaDef regla, ProcedimientoDef proc, HallazgoEvento h) {
        Integer pieza = h == null ? null : h.pieza().fdi();
        Set<Superficie> superficies = h == null ? Set.of() : h.superficies();
        int cantidad = proc.formaCobro() == FormaCobro.SUPERFICIE ? Math.max(1, superficies.size()) : 1;
        return new ItemSugerido(proc.id(), pieza, superficies, regla.fase(), cantidad, proc.precioCent(),
                h == null ? null : h.id());
    }
}
