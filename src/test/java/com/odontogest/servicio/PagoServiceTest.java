/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.odontogest.PruebaIntegracion;
import com.odontogest.dominio.finanzas.EstadoCuota;
import com.odontogest.dominio.finanzas.Frecuencia;
import com.odontogest.excepcion.NegocioException;
import com.odontogest.excepcion.PermisoDenegadoException;
import com.odontogest.modelo.EstadoPlan;
import com.odontogest.modelo.MedioPago;
import com.odontogest.modelo.Sexo;
import com.odontogest.modelo.TipoDocumento;
import com.odontogest.servicio.dto.AplicacionDto;
import com.odontogest.servicio.dto.CronogramaForm;
import com.odontogest.servicio.dto.CuotaPacienteDto;
import com.odontogest.servicio.dto.EstadoCuentaDto;
import com.odontogest.servicio.dto.PacienteForm;
import com.odontogest.servicio.dto.PagoDto;
import com.odontogest.servicio.dto.PagoForm;
import com.odontogest.servicio.dto.PlanItemForm;
import com.odontogest.servicio.dto.ProcedimientoDto;
import com.odontogest.servicio.dto.ResumenCajaDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Caja (Fase 5): vista previa FIFO, saldos tras pagar y anular, recibos correlativos sin saltos, estado de cuenta,
 * caja del día y recibo en PDF.
 */
class PagoServiceTest extends PruebaIntegracion {

    @Autowired private PacienteService pacienteService;
    @Autowired private ProcedimientoService procedimientoService;
    @Autowired private PlanTratamientoService planService;
    @Autowired private PagoService pagoService;
    @Autowired private JdbcTemplate jdbc;

    private long pacienteId;
    private long planId;

    @BeforeEach
    void preparar() {
        pacienteId = nuevoPaciente();
        planId = planAceptado(pacienteId, "1000.00", "100.00", 3);
        iniciarSesion("recepcion");
    }

    private long nuevoPaciente() {
        iniciarSesion("odontologo");
        return pacienteService.guardar(new PacienteForm(null, TipoDocumento.DNI, dniAleatorio(), "Paciente",
                "Caja" + sufijo(), null, LocalDate.now().minusYears(30), Sexo.F, null, null, null, null, null, null,
                null, null, null, true));
    }

    /** Plan aceptado con un procedimiento del precio indicado y cronograma: inicial (hoy) + n cuotas mensuales. */
    private long planAceptado(long paciente, String precio, String inicial, int cuotas) {
        iniciarSesion("odontologo");
        long prof = procedimientoService.listar(false).stream().filter(p -> p.codigo().equals("PROF"))
                .map(ProcedimientoDto::id).findFirst().orElseThrow();
        long plan = planService.crear(paciente, null, false);
        planService.guardarItem(new PlanItemForm(null, plan, prof, null, null, 4, 1, new BigDecimal(precio), null));
        planService.cambiarEstado(plan, EstadoPlan.PROPUESTO);
        planService.generarCronograma(new CronogramaForm(plan, new BigDecimal(inicial), cuotas,
                LocalDate.now().plusMonths(1), Frecuencia.MENSUAL));
        planService.aceptar(plan, "Paciente de prueba");
        return plan;
    }

    private PagoDto pagar(String monto, MedioPago medio) {
        return pagoService.registrar(new PagoForm(pacienteId, new BigDecimal(monto), medio, null));
    }

    private static long correlativo(PagoDto p) {
        return Long.parseLong(p.numeroRecibo().substring(2));
    }

    // ------------------------------------------------------------------ vista previa y registro

    @Test
    void vistaPreviaFifoSinRegistrarNada() {
        List<AplicacionDto> previa = pagoService.vistaPrevia(pacienteId, new BigDecimal("450.00"));
        assertThat(previa).extracting(AplicacionDto::numeroCuota, a -> a.monto().toPlainString(), a -> a.saldoDespues().toPlainString())
                .containsExactly(tuple(0, "100.00", "0.00"), tuple(1, "300.00", "0.00"), tuple(2, "50.00", "250.00"));
        assertThat(pagoService.saldo(pacienteId)).isEqualByComparingTo("1000.00");

        assertThatThrownBy(() -> pagoService.vistaPrevia(pacienteId, new BigDecimal("1000.01")))
                .isInstanceOf(NegocioException.class).hasMessageContaining("excede el saldo pendiente en S/ 0.01");
        assertThatThrownBy(() -> pagoService.vistaPrevia(pacienteId, BigDecimal.ZERO)).hasMessageContaining("mayor que cero");
        assertThatThrownBy(() -> pagoService.vistaPrevia(pacienteId, new BigDecimal("1.001"))).hasMessageContaining("2 decimales");
    }

    @Test
    void registrarAplicaFifoActualizaSaldosEIniciaElPlan() {
        PagoDto p = pagar("450.00", MedioPago.EFECTIVO);

        assertThat(p.numeroRecibo()).matches("R-\\d{8}");
        assertThat(p.registradoPor()).isNotEqualTo("—");
        assertThat(p.aplicaciones()).extracting(AplicacionDto::numeroCuota, a -> a.monto().toPlainString())
                .containsExactly(tuple(0, "100.00"), tuple(1, "300.00"), tuple(2, "50.00"));
        assertThat(jdbc.queryForObject("SELECT plan_id FROM pago WHERE id = ?", Long.class, p.id())).isEqualTo(planId);
        assertThat(planService.obtener(planId).estado()).as("el primer pago inicia el plan").isEqualTo(EstadoPlan.EN_CURSO);

        List<CuotaPacienteDto> pendientes = pagoService.cuotasPendientes(pacienteId);
        assertThat(pendientes).extracting(CuotaPacienteDto::numero, c -> c.saldo().toPlainString(), CuotaPacienteDto::estado)
                .containsExactly(tuple(2, "250.00", EstadoCuota.PARCIAL), tuple(3, "300.00", EstadoCuota.PENDIENTE));
        assertThat(pagoService.saldo(pacienteId)).isEqualByComparingTo("550.00");

        PagoDto resto = pagoService.registrar(new PagoForm(pacienteId, new BigDecimal("550"), MedioPago.YAPE, " 0012345 "));
        assertThat(resto.referencia()).isEqualTo("0012345");
        assertThat(pagoService.saldo(pacienteId)).isEqualByComparingTo("0.00");
        assertThat(pagoService.cuotasPendientes(pacienteId)).isEmpty();
        assertThatThrownBy(() -> pagar("1.00", MedioPago.EFECTIVO)).hasMessageContaining("no tiene cuotas pendientes");
    }

    @Test
    void recibosCorrelativosSinSaltosAunqueFalleUnCobro() {
        PagoDto a = pagar("10.00", MedioPago.EFECTIVO);
        assertThatThrownBy(() -> pagar("5000.00", MedioPago.EFECTIVO)).hasMessageContaining("excede");
        assertThatThrownBy(() -> pagoService.registrar(new PagoForm(pacienteId, new BigDecimal("5"), null, null)))
                .hasMessageContaining("medio de pago");
        PagoDto b = pagar("20.00", MedioPago.TARJETA);
        PagoDto c = pagar("30.00", MedioPago.PLIN);
        assertThat(List.of(correlativo(a), correlativo(b), correlativo(c)))
                .containsExactly(correlativo(a), correlativo(a) + 1, correlativo(a) + 2);
    }

    @Test
    void lasCuotasDePlanesNoAceptadosNoSeCobran() {
        long otro = nuevoPaciente();
        iniciarSesion("odontologo");
        long prof = procedimientoService.listar(false).stream().filter(p -> p.codigo().equals("PROF"))
                .map(ProcedimientoDto::id).findFirst().orElseThrow();
        long plan = planService.crear(otro, null, false);
        planService.guardarItem(new PlanItemForm(null, plan, prof, null, null, 4, 1, null, null));
        planService.cambiarEstado(plan, EstadoPlan.PROPUESTO);
        planService.generarCronograma(new CronogramaForm(plan, null, 2, LocalDate.now().plusDays(5), Frecuencia.MENSUAL));
        iniciarSesion("recepcion");
        assertThat(pagoService.cuotasPendientes(otro)).isEmpty();
        assertThatThrownBy(() -> pagoService.registrar(new PagoForm(otro, new BigDecimal("10"), MedioPago.EFECTIVO, null)))
                .hasMessageContaining("no tiene cuotas pendientes");
    }

    @Test
    void unPagoPuedeCubrirCuotasDeVariosPlanes() {
        long segundo = planAceptado(pacienteId, "200.00", "0", 1);
        iniciarSesion("recepcion");
        // FIFO: inicial del primer plan (hoy) y luego las cuotas que vencen en un mes, primero las del plan más antiguo.
        PagoDto p = pagar("700.00", MedioPago.TRANSFERENCIA);
        assertThat(p.aplicaciones()).extracting(AplicacionDto::planId, AplicacionDto::numeroCuota)
                .containsExactly(tuple(planId, 0), tuple(planId, 1), tuple(segundo, 1), tuple(planId, 2));
        assertThat(jdbc.queryForObject("SELECT plan_id FROM pago WHERE id = ?", Long.class, p.id()))
                .as("repartido entre planes").isNull();
        assertThat(planService.obtener(segundo).estado()).isEqualTo(EstadoPlan.EN_CURSO);
    }

    // ------------------------------------------------------------------ anulación

    @Test
    void anularRevierteLasCuotasYConservaLasAplicaciones() {
        PagoDto p = pagar("450.00", MedioPago.EFECTIVO);
        assertThatThrownBy(() -> pagoService.anular(p.id(), "Error de digitación"))
                .as("recepción no anula").isInstanceOf(PermisoDenegadoException.class);

        iniciarSesion("admin");
        assertThatThrownBy(() -> pagoService.anular(p.id(), "corto")).hasMessageContaining("al menos 10");
        PagoDto anulado = pagoService.anular(p.id(), "  Error de digitación del monto  ");
        assertThat(anulado.anulado()).isTrue();
        assertThat(anulado.motivoAnulacion()).isEqualTo("Error de digitación del monto");
        assertThat(anulado.anuladoEn()).isNotNull();
        assertThat(anulado.aplicaciones()).as("se conservan para trazabilidad").hasSize(3);
        assertThat(pagoService.saldo(pacienteId)).isEqualByComparingTo("1000.00");
        assertThat(pagoService.cuotasPendientes(pacienteId)).hasSize(4).allMatch(c -> c.pagado().signum() == 0);
        assertThatThrownBy(() -> pagoService.anular(p.id(), "Otra vez el mismo pago")).hasMessageContaining("ya está anulado");

        // El plan quedó sin pagos efectivos: el cronograma se puede regenerar de nuevo.
        iniciarSesion("odontologo");
        assertThat(planService.obtener(planId).tienePagos()).isFalse();
    }

    // ------------------------------------------------------------------ consultas

    @Test
    void estadoDeCuentaConVencidas() {
        jdbc.update("UPDATE cuota SET vencimiento = ? WHERE plan_id = ? AND numero = 1",
                LocalDate.now().minusDays(3).toString(), planId);
        pagar("150.00", MedioPago.EFECTIVO);                 // FIFO: va primero a la cuota 1, que venció hace 3 días

        EstadoCuentaDto e = pagoService.estadoCuenta(pacienteId);
        assertThat(e.total()).isEqualByComparingTo("1000.00");
        assertThat(e.pagado()).isEqualByComparingTo("150.00");
        assertThat(e.saldo()).isEqualByComparingTo("850.00");
        assertThat(e.vencido()).isEqualByComparingTo("150.00");
        assertThat(e.cuotas()).extracting(CuotaPacienteDto::numero, c -> c.saldo().toPlainString(), CuotaPacienteDto::estado)
                .containsExactly(tuple(1, "150.00", EstadoCuota.VENCIDA), tuple(0, "100.00", EstadoCuota.PENDIENTE),
                        tuple(2, "300.00", EstadoCuota.PENDIENTE), tuple(3, "300.00", EstadoCuota.PENDIENTE));
        assertThat(e.pagos()).hasSize(1);
    }

    @Test
    void cajaDelDiaSumaPorMedioSinLosAnulados() {
        ResumenCajaDto antes = pagoService.cajaDelDia(LocalDate.now());
        pagar("100.00", MedioPago.EFECTIVO);
        pagar("50.50", MedioPago.YAPE);
        PagoDto anulado = pagar("20.00", MedioPago.EFECTIVO);
        iniciarSesion("admin");
        pagoService.anular(anulado.id(), "Cobro duplicado por error");

        ResumenCajaDto hoy = pagoService.cajaDelDia(LocalDate.now());
        assertThat(hoy.pagos()).hasSize(antes.pagos().size() + 3);
        assertThat(hoy.porMedio().get(MedioPago.EFECTIVO).subtract(antes.porMedio().get(MedioPago.EFECTIVO)))
                .isEqualByComparingTo("100.00");
        assertThat(hoy.porMedio().get(MedioPago.YAPE).subtract(antes.porMedio().get(MedioPago.YAPE)))
                .isEqualByComparingTo("50.50");
        assertThat(hoy.total().subtract(antes.total())).isEqualByComparingTo("150.50");
        assertThat(pagoService.cajaDelDia(LocalDate.now().minusDays(400)).pagos()).isEmpty();
    }

    @Test
    void reciboEnPdfConMontoEnLetrasYMarcaDeAnulado() throws Exception {
        PagoDto p = pagar("1000.00", MedioPago.EFECTIVO);
        String texto = new PdfTextExtractor(new PdfReader(pagoService.pdfRecibo(p.id()))).getTextFromPage(1);
        assertThat(texto).contains(p.numeroRecibo(), "MIL CON 00/100 SOLES", "Efectivo", "Plan N° " + planId + " · Inicial")
                .doesNotContain("ANULADO");

        iniciarSesion("admin");
        pagoService.anular(p.id(), "Pago registrado al paciente equivocado");
        String anulado = new PdfTextExtractor(new PdfReader(pagoService.pdfRecibo(p.id()))).getTextFromPage(1);
        assertThat(anulado).contains("ANULADO", "Pago registrado al paciente equivocado");
    }
}
