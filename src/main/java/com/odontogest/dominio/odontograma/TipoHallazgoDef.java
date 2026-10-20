/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.odontograma;

import java.util.List;
import java.util.Objects;

/**
 * Definición de un tipo de hallazgo del catálogo {@code hallazgo_tipo}. Colores, símbolos y capas
 * salen de aquí: el código no tiene reglas clínicas "quemadas".
 *
 * @param siglasOpciones   siglas entre las que el usuario debe elegir (vacía si no aplica)
 * @param simbolo          nombre del símbolo de dibujo (columna {@code simbolo}, p. ej. {@code ASPA})
 * @param grupoExclusion   capa: un hallazgo nuevo de la misma capa reemplaza al anterior
 * @param ocultaSuperficies la pieza deja de mostrar sus superficies (p. ej. pieza ausente)
 */
public record TipoHallazgoDef(
        long id, String codigo, String nombre, String sigla, List<String> siglasOpciones,
        Representacion representacion, String simbolo, ReglaColor reglaColor, String grupoExclusion,
        boolean ocultaSuperficies, ConteoIndice conteoCpod, ConteoIndice conteoCeod) {

    public TipoHallazgoDef {
        Objects.requireNonNull(codigo, "codigo");
        Objects.requireNonNull(representacion, "representacion");
        Objects.requireNonNull(reglaColor, "reglaColor");
        Objects.requireNonNull(grupoExclusion, "grupoExclusion");
        siglasOpciones = siglasOpciones == null ? List.of() : List.copyOf(siglasOpciones);
        conteoCpod = conteoCpod == null ? ConteoIndice.NINGUNO : conteoCpod;
        conteoCeod = conteoCeod == null ? ConteoIndice.NINGUNO : conteoCeod;
        if (conteoCpod == ConteoIndice.E) {
            throw new IllegalArgumentException("El CPO-D no tiene componente E: " + codigo);
        }
        if (conteoCeod == ConteoIndice.P) {
            throw new IllegalArgumentException("El ceo-d no tiene componente P: " + codigo);
        }
    }

    /** Indica si el usuario debe elegir el estado bueno/malo al registrar. */
    public boolean usaEstado() {
        return reglaColor == ReglaColor.POR_ESTADO;
    }
}
