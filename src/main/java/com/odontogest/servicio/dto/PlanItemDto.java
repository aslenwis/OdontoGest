/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import com.odontogest.dominio.odontograma.PiezaDental;
import com.odontogest.dominio.odontograma.Superficie;
import com.odontogest.dominio.tratamiento.FormaCobro;
import com.odontogest.modelo.EstadoPlanItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Ítem de un plan de tratamiento.
 *
 * @param hallazgoId hallazgo que originó la sugerencia ({@code null} si se agregó a mano)
 * @param atencionId atención en la que se realizó ({@code null} si aún no se realiza)
 */
public record PlanItemDto(long id, long planId, long procedimientoId, String procedimientoCodigo, String procedimientoNombre,
                          FormaCobro formaCobro, Long hallazgoId, Integer pieza, Set<Superficie> superficies, int fase,
                          int cantidad, BigDecimal precioUnit, BigDecimal descuento, BigDecimal total,
                          EstadoPlanItem estado, Long atencionId, LocalDateTime realizadoEn) {

    public PlanItemDto {
        superficies = Set.copyOf(superficies);
    }

    /** Nombre de la fase del plan (RF-PLN-02). */
    public static String nombreFase(int fase) {
        return switch (fase) {
            case 1 -> "Fase 1 · Urgente";
            case 2 -> "Fase 2 · Operatoria";
            case 3 -> "Fase 3 · Rehabilitación";
            case 4 -> "Fase 4 · Mantenimiento";
            default -> "Fase " + fase;
        };
    }

    /** Superficies con la sigla que corresponde a la pieza, p. ej. "MOD" o "VI"; vacío si no hay. */
    public String superficiesTexto() {
        if (superficies.isEmpty() || pieza == null) {
            return "";
        }
        PiezaDental p = PiezaDental.de(pieza);
        return superficies.stream().sorted().map(s -> s.etiqueta(p)).collect(Collectors.joining());
    }

    /** Por ejemplo "Resina compuesta · pieza 36 MO". */
    public String descripcion() {
        StringBuilder sb = new StringBuilder(procedimientoNombre);
        if (pieza != null) {
            sb.append(" · pieza ").append(pieza);
            String sup = superficiesTexto();
            if (!sup.isEmpty()) {
                sb.append(' ').append(sup);
            }
        }
        if (cantidad > 1 && formaCobro != FormaCobro.SUPERFICIE) {
            sb.append(" ×").append(cantidad);
        }
        return sb.toString();
    }

    public boolean realizado() {
        return estado == EstadoPlanItem.REALIZADO;
    }
}
