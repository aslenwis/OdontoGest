/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.persistencia.repositorio;

import com.odontogest.modelo.PagoAplicacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

/**
 * Acceso a las aplicaciones de pagos a cuotas (tabla {@code pago_aplicacion}).
 */
public interface PagoAplicacionRepository extends JpaRepository<PagoAplicacion, Long> {

    @Query("""
            SELECT a FROM PagoAplicacion a JOIN FETCH a.cuota c JOIN FETCH c.plan
            WHERE a.pago.id IN :pagoIds ORDER BY a.pago.id, c.vencimiento, c.numero""")
    List<PagoAplicacion> listarPorPagos(Collection<Long> pagoIds);
}
