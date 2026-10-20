/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.odontograma;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static com.odontogest.dominio.odontograma.ConteoIndice.C;
import static com.odontogest.dominio.odontograma.ConteoIndice.E;
import static com.odontogest.dominio.odontograma.ConteoIndice.NINGUNO;
import static com.odontogest.dominio.odontograma.ConteoIndice.O;
import static com.odontogest.dominio.odontograma.ConteoIndice.P;

/**
 * Tipos de hallazgo del catálogo inicial (PLAN §3.5) y fábrica de eventos para las pruebas del dominio.
 */
public final class CatalogoPrueba {

    public static final TipoHallazgoDef CARIES = tipo(1, "CARIES", Representacion.SUPERFICIE, SimboloHallazgo.RELLENO,
            ReglaColor.SIEMPRE_ROJO, "SUPERFICIE", false, C, C);
    public static final TipoHallazgoDef RESTAURACION = new TipoHallazgoDef(2, "RESTAURACION_DEFINITIVA",
            "Restauración definitiva", null, List.of("AM", "R", "IV", "IM", "IE"), Representacion.SUPERFICIE,
            SimboloHallazgo.RELLENO, ReglaColor.POR_ESTADO, "SUPERFICIE", false, O, O);
    public static final TipoHallazgoDef RESTAURACION_TEMPORAL = tipo(3, "RESTAURACION_TEMPORAL",
            Representacion.SUPERFICIE, SimboloHallazgo.CONTORNO_SUPERFICIE, ReglaColor.SIEMPRE_ROJO, "SUPERFICIE", false, O, O);
    public static final TipoHallazgoDef SELLANTE = tipo(4, "SELLANTE", Representacion.SUPERFICIE, SimboloHallazgo.TRAZO_FOSAS,
            ReglaColor.POR_ESTADO, "SUPERFICIE_SELLANTE", false, NINGUNO, NINGUNO);
    public static final TipoHallazgoDef PIEZA_AUSENTE = tipo(5, "PIEZA_AUSENTE", Representacion.PIEZA, SimboloHallazgo.ASPA,
            ReglaColor.SIEMPRE_AZUL, "ESTADO_PIEZA", true, P, E);
    public static final TipoHallazgoDef REMANENTE_RADICULAR = tipo(6, "REMANENTE_RADICULAR", Representacion.PIEZA,
            SimboloHallazgo.SOLO_SIGLA, ReglaColor.SIEMPRE_ROJO, "ESTADO_PIEZA", false, C, E);
    public static final TipoHallazgoDef IMPLANTE = tipo(7, "IMPLANTE", Representacion.PIEZA, SimboloHallazgo.SOLO_SIGLA,
            ReglaColor.POR_ESTADO, "ESTADO_PIEZA", false, P, NINGUNO);
    public static final TipoHallazgoDef CORONA_DEFINITIVA = tipo(9, "CORONA_DEFINITIVA", Representacion.PIEZA,
            SimboloHallazgo.CUADRADO_CORONA, ReglaColor.POR_ESTADO, "CORONA", false, O, O);
    public static final TipoHallazgoDef CORONA_TEMPORAL = tipo(10, "CORONA_TEMPORAL", Representacion.PIEZA,
            SimboloHallazgo.CUADRADO_CORONA, ReglaColor.SIEMPRE_ROJO, "CORONA", false, O, O);
    public static final TipoHallazgoDef TRATAMIENTO_PULPAR = tipo(11, "TRATAMIENTO_PULPAR", Representacion.PIEZA,
            SimboloHallazgo.LINEA_RAIZ, ReglaColor.POR_ESTADO, "PULPAR", false, NINGUNO, NINGUNO);
    public static final TipoHallazgoDef FRACTURA = tipo(12, "FRACTURA", Representacion.PIEZA, SimboloHallazgo.LINEA_FRACTURA,
            ReglaColor.SIEMPRE_ROJO, "FRACTURA", false, NINGUNO, NINGUNO);
    public static final TipoHallazgoDef MOVILIDAD = tipo(16, "MOVILIDAD", Representacion.PIEZA, SimboloHallazgo.SOLO_SIGLA,
            ReglaColor.SIEMPRE_ROJO, "MOVILIDAD", false, NINGUNO, NINGUNO);
    public static final TipoHallazgoDef DIASTEMA = tipo(17, "DIASTEMA", Representacion.ENTRE_PIEZAS, SimboloHallazgo.PARENTESIS,
            ReglaColor.SIEMPRE_AZUL, "DIASTEMA", false, NINGUNO, NINGUNO);
    public static final TipoHallazgoDef PROTESIS_FIJA = tipo(18, "PROTESIS_FIJA", Representacion.RANGO_PIEZAS, SimboloHallazgo.PUENTE,
            ReglaColor.POR_ESTADO, "PROTESIS_FIJA", false, NINGUNO, NINGUNO);
    public static final TipoHallazgoDef PIEZA_NO_ERUPCIONADA = tipo(19, "PIEZA_AUSENTE_DNE", Representacion.PIEZA, SimboloHallazgo.ASPA,
            ReglaColor.SIEMPRE_AZUL, "ESTADO_PIEZA", true, NINGUNO, NINGUNO);
    public static final TipoHallazgoDef EDENTULO_TOTAL = tipo(40, "EDENTULO_TOTAL", Representacion.RANGO_PIEZAS,
            SimboloHallazgo.LINEA_CORONAS, ReglaColor.SIEMPRE_AZUL, "EDENTULO", true, P, E);

    /** Fecha base: los eventos de prueba se ordenan por minutos a partir de aquí. */
    public static final LocalDateTime T0 = LocalDateTime.of(2026, 3, 2, 9, 0);

    private static final AtomicLong IDS = new AtomicLong(1000);

    private CatalogoPrueba() {
    }

    private static TipoHallazgoDef tipo(long id, String codigo, Representacion r, SimboloHallazgo simbolo, ReglaColor color,
                                        String capa, boolean oculta, ConteoIndice cpod, ConteoIndice ceod) {
        return new TipoHallazgoDef(id, codigo, codigo, null, List.of(), r, simbolo, color, capa, oculta, cpod, ceod);
    }

    /** Hallazgo de superficie registrado {@code minuto} minutos después de {@link #T0}. */
    public static HallazgoEvento sup(TipoHallazgoDef tipo, int fdi, int minuto, EstadoHallazgo estado, Superficie... s) {
        return new HallazgoEvento(IDS.incrementAndGet(), 1, tipo, PiezaDental.de(fdi), null,
                EnumSet.copyOf(List.of(s)), estado, null, null, T0.plusMinutes(minuto), 1, false);
    }

    public static HallazgoEvento sup(TipoHallazgoDef tipo, int fdi, int minuto, Superficie... s) {
        return sup(tipo, fdi, minuto, null, s);
    }

    /** Hallazgo de pieza. */
    public static HallazgoEvento pieza(TipoHallazgoDef tipo, int fdi, int minuto, EstadoHallazgo estado) {
        return new HallazgoEvento(IDS.incrementAndGet(), 1, tipo, PiezaDental.de(fdi), null,
                EnumSet.noneOf(Superficie.class), estado, null, null, T0.plusMinutes(minuto), 1, false);
    }

    public static HallazgoEvento pieza(TipoHallazgoDef tipo, int fdi, int minuto) {
        return pieza(tipo, fdi, minuto, null);
    }

    /** Hallazgo entre piezas o de rango. */
    public static HallazgoEvento rango(TipoHallazgoDef tipo, int desde, int hasta, int minuto, EstadoHallazgo estado) {
        return new HallazgoEvento(IDS.incrementAndGet(), 1, tipo, PiezaDental.de(desde), PiezaDental.de(hasta),
                EnumSet.noneOf(Superficie.class), estado, null, null, T0.plusMinutes(minuto), 1, false);
    }

    /** Copia del evento marcada como anulada. */
    public static HallazgoEvento anulado(HallazgoEvento e) {
        return new HallazgoEvento(e.id(), e.odontogramaId(), e.tipo(), e.pieza(), e.piezaFin(), e.superficies(),
                e.estado(), e.sigla(), e.nota(), e.registradoEn(), e.registradoPor(), true);
    }

    /** Proyección sin límite de fecha. */
    public static EstadoOdontograma estado(HallazgoEvento... eventos) {
        return new ProyectorOdontograma().proyectar(List.of(eventos), null);
    }
}
