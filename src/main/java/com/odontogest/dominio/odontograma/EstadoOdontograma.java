/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.odontograma;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Estado proyectado e inmutable del odontograma: qué hallazgo ocupa cada {@link ClaveCapa}.
 */
public final class EstadoOdontograma {

    private static final EstadoOdontograma VACIO = new EstadoOdontograma(Map.of(), Map.of(), List.of());

    private final Map<ClaveCapa, HallazgoEvento> vigentes;
    private final Map<Integer, EstadoPieza> piezas;
    private final List<HallazgoEvento> rangos;

    private EstadoOdontograma(Map<ClaveCapa, HallazgoEvento> vigentes, Map<Integer, EstadoPieza> piezas,
                              List<HallazgoEvento> rangos) {
        this.vigentes = vigentes;
        this.piezas = piezas;
        this.rangos = rangos;
    }

    public static EstadoOdontograma vacio() {
        return VACIO;
    }

    /** Construye el estado a partir del mapa clave → hallazgo vigente. */
    public static EstadoOdontograma desde(Map<ClaveCapa, HallazgoEvento> vigentes) {
        Map<PiezaDental, Set<HallazgoEvento>> porPieza = new TreeMap<>();
        Set<HallazgoEvento> rangos = new LinkedHashSet<>();
        vigentes.forEach((clave, h) -> {
            if (h.esDeRango()) {
                rangos.add(h);
            } else {
                porPieza.computeIfAbsent(h.pieza(), p -> new LinkedHashSet<>()).add(h);
            }
        });

        Comparator<HallazgoEvento> cronologico = Comparator.comparing(HallazgoEvento::registradoEn)
                .thenComparingLong(HallazgoEvento::id);
        Map<Integer, EstadoPieza> piezas = new LinkedHashMap<>();
        porPieza.forEach((pieza, hallazgos) -> {
            List<HallazgoEvento> lista = new ArrayList<>(hallazgos);
            lista.sort(cronologico);
            boolean ocultas = lista.stream().anyMatch(h -> h.tipo().ocultaSuperficies());
            piezas.put(pieza.fdi(), new EstadoPieza(pieza, lista, ocultas));
        });
        List<HallazgoEvento> listaRangos = new ArrayList<>(rangos);
        listaRangos.sort(cronologico);

        return new EstadoOdontograma(
                Collections.unmodifiableMap(new LinkedHashMap<>(vigentes)),
                Collections.unmodifiableMap(piezas),
                List.copyOf(listaRangos));
    }

    /** Estado por pieza (fdi → estado), en orden de dibujo; solo piezas con hallazgos vigentes. */
    public Map<Integer, EstadoPieza> piezas() {
        return piezas;
    }

    public Optional<EstadoPieza> pieza(int fdi) {
        return Optional.ofNullable(piezas.get(fdi));
    }

    /** Hallazgos vigentes entre piezas o sobre rangos. */
    public List<HallazgoEvento> rangos() {
        return rangos;
    }

    public Set<ClaveCapa> claves() {
        return vigentes.keySet();
    }

    /** Mapa completo clave → hallazgo vigente. */
    public Map<ClaveCapa, HallazgoEvento> vigentes() {
        return vigentes;
    }

    /** Hallazgos de rango vigentes que incluyen la pieza (p. ej. edéntulo total 18–28 cubre la 14). */
    public List<HallazgoEvento> rangosQueCubren(int fdi) {
        PiezaDental pieza = PiezaDental.de(fdi);
        return rangos.stream()
                .filter(r -> PiezaDental.rango(r.pieza(), r.piezaFin()).contains(pieza))
                .toList();
    }

    /** La pieza no muestra superficies: por un hallazgo propio (pieza ausente) o por un rango (edéntulo total). */
    public boolean superficiesOcultas(int fdi) {
        return pieza(fdi).map(EstadoPieza::superficiesOcultas).orElse(false)
                || rangosQueCubren(fdi).stream().anyMatch(r -> r.tipo().ocultaSuperficies());
    }

    /**
     * Hallazgos que se dibujan y cuentan para la pieza (índices y sugerencias): los propios, sin los de
     * superficie si la pieza las oculta, más los rangos que la cubren.
     */
    public List<HallazgoEvento> hallazgosVisibles(int fdi) {
        boolean ocultas = superficiesOcultas(fdi);
        List<HallazgoEvento> visibles = new ArrayList<>();
        pieza(fdi).ifPresent(p -> p.hallazgos().stream()
                .filter(h -> !(ocultas && h.esDeSuperficie()))
                .forEach(visibles::add));
        visibles.addAll(rangosQueCubren(fdi));
        return List.copyOf(visibles);
    }

    public Optional<HallazgoEvento> enSuperficie(int fdi, Superficie superficie, String capa) {
        return Optional.ofNullable(vigentes.get(new ClaveCapa(fdi, fdi, superficie, capa)));
    }

    /** Todos los hallazgos vigentes sin repetir (uno de 3 superficies ocupa 3 claves). */
    public List<HallazgoEvento> hallazgosVigentes() {
        return List.copyOf(new LinkedHashSet<>(vigentes.values()));
    }

    public boolean estaVacio() {
        return vigentes.isEmpty();
    }
}
