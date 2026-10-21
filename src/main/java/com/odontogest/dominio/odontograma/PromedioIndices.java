/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.odontograma;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Acumula los índices CPO-D y ceo-d de cada paciente examinado y calcula los promedios por {@link GrupoEtario}
 * (RF-REP-05). Los promedios se redondean a dos decimales; el nivel OMS se toma del promedio (la OMS clasifica
 * poblaciones, no individuos).
 */
public final class PromedioIndices {

    /**
     * Promedios de un grupo. Los componentes {@code c}, {@code p}, {@code o} son los del CPO-D.
     *
     * @param nivelCpod nivel OMS del CPO-D promedio; {@code null} si el grupo no tiene pacientes
     */
    public record Grupo(GrupoEtario grupo, int pacientes, double cpod, double c, double p, double o, double ceod,
                        NivelSeveridadOms nivelCpod) {
    }

    private static final class Suma {
        int pacientes;
        long cpod;
        long c;
        long p;
        long o;
        long ceod;
    }

    private final Map<GrupoEtario, Suma> sumas = new EnumMap<>(GrupoEtario.class);

    public void agregar(int edad, ResultadoIndice cpod, ResultadoIndice ceod) {
        Objects.requireNonNull(cpod, "cpod");
        Objects.requireNonNull(ceod, "ceod");
        Suma s = sumas.computeIfAbsent(GrupoEtario.de(edad), g -> new Suma());
        s.pacientes++;
        s.cpod += cpod.total();
        s.c += cpod.c();
        s.p += cpod.p();
        s.o += cpod.o();
        s.ceod += ceod.total();
    }

    public int pacientes() {
        return sumas.values().stream().mapToInt(s -> s.pacientes).sum();
    }

    /** Un elemento por grupo etario, en orden de edad, incluidos los grupos sin pacientes. */
    public List<Grupo> resultado() {
        List<Grupo> grupos = new ArrayList<>();
        for (GrupoEtario g : GrupoEtario.values()) {
            Suma s = sumas.get(g);
            if (s == null) {
                grupos.add(new Grupo(g, 0, 0, 0, 0, 0, 0, null));
                continue;
            }
            double cpod = promedio(s.cpod, s.pacientes);
            grupos.add(new Grupo(g, s.pacientes, cpod, promedio(s.c, s.pacientes), promedio(s.p, s.pacientes),
                    promedio(s.o, s.pacientes), promedio(s.ceod, s.pacientes), NivelSeveridadOms.de(cpod)));
        }
        return grupos;
    }

    private static double promedio(long suma, int n) {
        return Math.round(suma * 100.0 / n) / 100.0;
    }
}
