/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.odontograma;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Cambios entre dos estados del odontograma (p. ej. inicial frente a actual).
 *
 * @param agregadas    claves vigentes en el objetivo que no existían en la base
 * @param eliminadas   claves de la base que ya no están vigentes en el objetivo
 * @param modificadas  misma clave ocupada por otro hallazgo (id distinto)
 */
public record DiferenciaOdontograma(Set<ClaveCapa> agregadas, Set<ClaveCapa> eliminadas, Set<ClaveCapa> modificadas) {

    public DiferenciaOdontograma {
        agregadas = Set.copyOf(agregadas);
        eliminadas = Set.copyOf(eliminadas);
        modificadas = Set.copyOf(modificadas);
    }

    public static DiferenciaOdontograma entre(EstadoOdontograma base, EstadoOdontograma objetivo) {
        Map<ClaveCapa, HallazgoEvento> b = base.vigentes();
        Map<ClaveCapa, HallazgoEvento> o = objetivo.vigentes();
        Set<ClaveCapa> agregadas = new LinkedHashSet<>();
        Set<ClaveCapa> modificadas = new LinkedHashSet<>();
        o.forEach((clave, hallazgo) -> {
            HallazgoEvento anterior = b.get(clave);
            if (anterior == null) {
                agregadas.add(clave);
            } else if (anterior.id() != hallazgo.id()) {
                modificadas.add(clave);
            }
        });
        Set<ClaveCapa> eliminadas = new LinkedHashSet<>(b.keySet());
        eliminadas.removeAll(o.keySet());
        return new DiferenciaOdontograma(agregadas, eliminadas, modificadas);
    }

    public boolean sinCambios() {
        return agregadas.isEmpty() && eliminadas.isEmpty() && modificadas.isEmpty();
    }

    /** Piezas con algún cambio (para los rangos, ambas piezas extremo), ordenadas por número FDI. */
    public Set<Integer> piezasAfectadas() {
        Set<Integer> piezas = new TreeSet<>();
        for (Set<ClaveCapa> grupo : List.of(agregadas, eliminadas, modificadas)) {
            for (ClaveCapa c : grupo) {
                piezas.add(c.piezaInicio());
                piezas.add(c.piezaFin());
            }
        }
        return piezas;
    }
}
