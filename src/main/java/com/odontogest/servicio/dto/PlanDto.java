/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import com.odontogest.modelo.EstadoPlan;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Plan de tratamiento completo: cabecera, ítems, cronograma y consentimiento (pestaña Plan y PDF de presupuesto).
 *
 * @param odontogramaId            odontograma firmado que originó las sugerencias ({@code null} si se armó a mano)
 * @param consentimientoFirmadoPor quien firmó el consentimiento informado de tratamiento ({@code null} si aún no)
 * @param transiciones             estados a los que puede pasar según su estado actual; las demás condiciones
 *                                 (ítems, pagos) las valida el servicio al ejecutar la transición
 */
public record PlanDto(long id, long pacienteId, long odontologoId, String odontologoNombre, String odontologoCop,
                      Long odontogramaId, LocalDateTime fecha, EstadoPlan estado, BigDecimal subtotal,
                      BigDecimal descuento, BigDecimal total, String observaciones, LocalDateTime aceptadoEn,
                      List<PlanItemDto> items, List<CuotaDto> cuotas, String consentimientoFirmadoPor,
                      LocalDateTime consentimientoEn, Set<EstadoPlan> transiciones) {

    public PlanDto {
        items = List.copyOf(items);
        cuotas = List.copyOf(cuotas);
        transiciones = Set.copyOf(transiciones);
    }

    /** Total en céntimos (para el monto en letras). */
    public long totalCent() {
        return total.setScale(2).movePointRight(2).longValueExact();
    }

    public boolean editable() {
        return estado.editable();
    }

    public BigDecimal pagado() {
        return cuotas.stream().map(CuotaDto::pagado).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean tienePagos() {
        return pagado().signum() > 0;
    }

    public long realizados() {
        return items.stream().filter(PlanItemDto::realizado).count();
    }

    public String etiqueta() {
        return new PlanResumen(id, fecha, estado, total, odontologoNombre).etiqueta();
    }
}
