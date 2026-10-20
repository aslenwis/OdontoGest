/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.finanzas;

import com.odontogest.dominio.comun.ReglaDominioException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Aplicación de pagos a cuotas en orden FIFO.
 */
class AsignadorPagosFifoTest {

    private static final LocalDate ENERO = LocalDate.of(2026, 1, 15);
    private static final LocalDate FEBRERO = LocalDate.of(2026, 2, 15);
    private static final LocalDate MARZO = LocalDate.of(2026, 3, 15);

    private final AsignadorPagosFifo asignador = new AsignadorPagosFifo();

    private final List<SaldoCuota> pendientes = List.of(
            new SaldoCuota(30, MARZO, 3, 10000),
            new SaldoCuota(10, ENERO, 1, 4000),            // parcialmente pagada antes
            new SaldoCuota(20, FEBRERO, 2, 10000));

    @Test
    void pagaPrimeroLaCuotaMasAntigua() {
        assertThat(asignador.asignar(3000, pendientes)).containsExactly(new Aplicacion(10, 3000));
    }

    @Test
    void unPagoGrandeCubreVariasCuotasEnOrden() {
        assertThat(asignador.asignar(19000, pendientes)).containsExactly(
                new Aplicacion(10, 4000), new Aplicacion(20, 10000), new Aplicacion(30, 5000));
    }

    @Test
    void pagoExactoDelSaldoTotal() {
        List<Aplicacion> aplicaciones = asignador.asignar(24000, pendientes);
        assertThat(aplicaciones).hasSize(3);
        assertThat(aplicaciones.stream().mapToLong(Aplicacion::montoCent).sum()).isEqualTo(24000);
    }

    @Test
    void aIgualVencimientoVaPrimeroElMenorNumero() {
        List<SaldoCuota> mismoDia = List.of(new SaldoCuota(2, ENERO, 1, 500), new SaldoCuota(1, ENERO, 0, 500));
        assertThat(asignador.asignar(700, mismoDia)).containsExactly(new Aplicacion(1, 500), new Aplicacion(2, 200));
    }

    @Test
    void ignoraCuotasSinSaldo() {
        List<SaldoCuota> conPagada = List.of(new SaldoCuota(1, ENERO, 1, 0), new SaldoCuota(2, FEBRERO, 2, 100));
        assertThat(asignador.asignar(100, conPagada)).containsExactly(new Aplicacion(2, 100));
    }

    @Test
    void rechazaMontoMayorQueElSaldo() {
        assertThatThrownBy(() -> asignador.asignar(24050, pendientes))
                .isInstanceOf(ReglaDominioException.class)
                .hasMessage("El monto excede el saldo pendiente en S/ 0.50.");
        assertThatThrownBy(() -> asignador.asignar(100, List.of()))
                .hasMessageContaining("S/ 1.00");
    }

    @Test
    void rechazaMontoNoPositivo() {
        assertThatThrownBy(() -> asignador.asignar(0, pendientes)).isInstanceOf(ReglaDominioException.class);
        assertThatThrownBy(() -> asignador.asignar(-5, pendientes)).isInstanceOf(ReglaDominioException.class);
    }
}
