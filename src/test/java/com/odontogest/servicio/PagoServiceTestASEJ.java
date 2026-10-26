package com.odontogest.servicio;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.odontogest.PruebaIntegracionASEJ;
import com.odontogest.dominio.finanzas.EstadoCuotaASEJ;
import com.odontogest.dominio.finanzas.FrecuenciaASEJ;
import com.odontogest.excepcion.NegocioExceptionASEJ;
import com.odontogest.excepcion.PermisoDenegadoExceptionASEJ;
import com.odontogest.modelo.EstadoPlanASEJ;
import com.odontogest.modelo.MedioPagoASEJ;
import com.odontogest.modelo.SexoASEJ;
import com.odontogest.modelo.TipoDocumentoASEJ;
import com.odontogest.servicio.dto.AplicacionDtoASEJ;
import com.odontogest.servicio.dto.CronogramaFormASEJ;
import com.odontogest.servicio.dto.CuotaPacienteDtoASEJ;
import com.odontogest.servicio.dto.EstadoCuentaDtoASEJ;
import com.odontogest.servicio.dto.PacienteFormASEJ;
import com.odontogest.servicio.dto.PagoDtoASEJ;
import com.odontogest.servicio.dto.PagoFormASEJ;
import com.odontogest.servicio.dto.PlanItemFormASEJ;
import com.odontogest.servicio.dto.ProcedimientoDtoASEJ;
import com.odontogest.servicio.dto.ResumenCajaDtoASEJ;
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

class PagoServiceTestASEJ extends PruebaIntegracionASEJ {

    @Autowired private PacienteServiceASEJ pacienteServiceASEJ;
    @Autowired private ProcedimientoServiceASEJ procedimientoServiceASEJ;
    @Autowired private PlanTratamientoServiceASEJ planServiceASEJ;
    @Autowired private PagoServiceASEJ pagoServiceASEJ;
    @Autowired private JdbcTemplate jdbcASEJ;

    private long pacienteIdASEJ;
    private long planIdASEJ;

    @BeforeEach
    void prepararASEJ() {
        pacienteIdASEJ = nuevoPacienteASEJ();
        planIdASEJ = planAceptadoASEJ(pacienteIdASEJ, "1000.00", "100.00", 3);
        iniciarSesionASEJ("recepcion");
    }

    private long nuevoPacienteASEJ() {
        iniciarSesionASEJ("odontologo");
        return pacienteServiceASEJ.guardarASEJ(new PacienteFormASEJ(null, TipoDocumentoASEJ.DNI, dniAleatorioASEJ(), "Paciente",
                "Caja" + sufijoASEJ(), null, LocalDate.now().minusYears(30), SexoASEJ.F, null, null, null, null, null, null,
                null, null, null, true));
    }

    private long planAceptadoASEJ(long pacienteASEJ, String precioASEJ, String inicialASEJ, int cuotasASEJ) {
        iniciarSesionASEJ("odontologo");
        long profASEJ = procedimientoServiceASEJ.listarASEJ(false).stream().filter(pASEJ -> pASEJ.codigoASEJ().equals("PROF"))
                .map(ProcedimientoDtoASEJ::idASEJ).findFirst().orElseThrow();
        long planASEJ = planServiceASEJ.crearASEJ(pacienteASEJ, null, false);
        planServiceASEJ.guardarItemASEJ(new PlanItemFormASEJ(null, planASEJ, profASEJ, null, null, 4, 1, new BigDecimal(precioASEJ), null));
        planServiceASEJ.cambiarEstadoASEJ(planASEJ, EstadoPlanASEJ.PROPUESTO);
        planServiceASEJ.generarCronogramaASEJ(new CronogramaFormASEJ(planASEJ, new BigDecimal(inicialASEJ), cuotasASEJ,
                LocalDate.now().plusMonths(1), FrecuenciaASEJ.MENSUAL));
        planServiceASEJ.aceptarASEJ(planASEJ, "Paciente de prueba");
        return planASEJ;
    }

    private PagoDtoASEJ pagarASEJ(String montoASEJ, MedioPagoASEJ medioASEJ) {
        return pagoServiceASEJ.registrarASEJ(new PagoFormASEJ(pacienteIdASEJ, new BigDecimal(montoASEJ), medioASEJ, null));
    }

    private static long correlativoASEJ(PagoDtoASEJ pASEJ) {
        return Long.parseLong(pASEJ.numeroReciboASEJ().substring(2));
    }

    @Test
    void vistaPreviaFifoSinRegistrarNadaASEJ() {
        List<AplicacionDtoASEJ> previaASEJ = pagoServiceASEJ.vistaPreviaASEJ(pacienteIdASEJ, new BigDecimal("450.00"));
        assertThat(previaASEJ).extracting(AplicacionDtoASEJ::numeroCuotaASEJ, aASEJ -> aASEJ.montoASEJ().toPlainString(), aASEJ -> aASEJ.saldoDespuesASEJ().toPlainString())
                .containsExactly(tuple(0, "100.00", "0.00"), tuple(1, "300.00", "0.00"), tuple(2, "50.00", "250.00"));
        assertThat(pagoServiceASEJ.saldoASEJ(pacienteIdASEJ)).isEqualByComparingTo("1000.00");

        assertThatThrownBy(() -> pagoServiceASEJ.vistaPreviaASEJ(pacienteIdASEJ, new BigDecimal("1000.01")))
                .isInstanceOf(NegocioExceptionASEJ.class).hasMessageContaining("excede el saldo pendiente en S/ 0.01");
        assertThatThrownBy(() -> pagoServiceASEJ.vistaPreviaASEJ(pacienteIdASEJ, BigDecimal.ZERO)).hasMessageContaining("mayor que cero");
        assertThatThrownBy(() -> pagoServiceASEJ.vistaPreviaASEJ(pacienteIdASEJ, new BigDecimal("1.001"))).hasMessageContaining("2 decimales");
    }

    @Test
    void registrarAplicaFifoActualizaSaldosEIniciaElPlanASEJ() {
        PagoDtoASEJ pASEJ = pagarASEJ("450.00", MedioPagoASEJ.EFECTIVO);

        assertThat(pASEJ.numeroReciboASEJ()).matches("R-\\d{8}");
        assertThat(pASEJ.registradoPorASEJ()).isNotEqualTo("—");
        assertThat(pASEJ.aplicacionesASEJ()).extracting(AplicacionDtoASEJ::numeroCuotaASEJ, aASEJ -> aASEJ.montoASEJ().toPlainString())
                .containsExactly(tuple(0, "100.00"), tuple(1, "300.00"), tuple(2, "50.00"));
        assertThat(jdbcASEJ.queryForObject("SELECT plan_id FROM pago WHERE id = ?", Long.class, pASEJ.idASEJ())).isEqualTo(planIdASEJ);
        assertThat(planServiceASEJ.obtenerASEJ(planIdASEJ).estadoASEJ()).as("el primer pago inicia el plan").isEqualTo(EstadoPlanASEJ.EN_CURSO);

        List<CuotaPacienteDtoASEJ> pendientesASEJ = pagoServiceASEJ.cuotasPendientesASEJ(pacienteIdASEJ);
        assertThat(pendientesASEJ).extracting(CuotaPacienteDtoASEJ::numeroASEJ, cASEJ -> cASEJ.saldoASEJ().toPlainString(), CuotaPacienteDtoASEJ::estadoASEJ)
                .containsExactly(tuple(2, "250.00", EstadoCuotaASEJ.PARCIAL), tuple(3, "300.00", EstadoCuotaASEJ.PENDIENTE));
        assertThat(pagoServiceASEJ.saldoASEJ(pacienteIdASEJ)).isEqualByComparingTo("550.00");

        PagoDtoASEJ restoASEJ = pagoServiceASEJ.registrarASEJ(new PagoFormASEJ(pacienteIdASEJ, new BigDecimal("550"), MedioPagoASEJ.YAPE, " 0012345 "));
        assertThat(restoASEJ.referenciaASEJ()).isEqualTo("0012345");
        assertThat(pagoServiceASEJ.saldoASEJ(pacienteIdASEJ)).isEqualByComparingTo("0.00");
        assertThat(pagoServiceASEJ.cuotasPendientesASEJ(pacienteIdASEJ)).isEmpty();
        assertThatThrownBy(() -> pagarASEJ("1.00", MedioPagoASEJ.EFECTIVO)).hasMessageContaining("no tiene cuotas pendientes");
    }

    @Test
    void recibosCorrelativosSinSaltosAunqueFalleUnCobroASEJ() {
        PagoDtoASEJ aASEJ = pagarASEJ("10.00", MedioPagoASEJ.EFECTIVO);
        assertThatThrownBy(() -> pagarASEJ("5000.00", MedioPagoASEJ.EFECTIVO)).hasMessageContaining("excede");
        assertThatThrownBy(() -> pagoServiceASEJ.registrarASEJ(new PagoFormASEJ(pacienteIdASEJ, new BigDecimal("5"), null, null)))
                .hasMessageContaining("medio de pago");
        PagoDtoASEJ bASEJ = pagarASEJ("20.00", MedioPagoASEJ.TARJETA);
        PagoDtoASEJ cASEJ = pagarASEJ("30.00", MedioPagoASEJ.PLIN);
        assertThat(List.of(correlativoASEJ(aASEJ), correlativoASEJ(bASEJ), correlativoASEJ(cASEJ)))
                .containsExactly(correlativoASEJ(aASEJ), correlativoASEJ(aASEJ) + 1, correlativoASEJ(aASEJ) + 2);
    }

    @Test
    void lasCuotasDePlanesNoAceptadosNoSeCobranASEJ() {
        long otroASEJ = nuevoPacienteASEJ();
        iniciarSesionASEJ("odontologo");
        long profASEJ = procedimientoServiceASEJ.listarASEJ(false).stream().filter(pASEJ -> pASEJ.codigoASEJ().equals("PROF"))
                .map(ProcedimientoDtoASEJ::idASEJ).findFirst().orElseThrow();
        long planASEJ = planServiceASEJ.crearASEJ(otroASEJ, null, false);
        planServiceASEJ.guardarItemASEJ(new PlanItemFormASEJ(null, planASEJ, profASEJ, null, null, 4, 1, null, null));
        planServiceASEJ.cambiarEstadoASEJ(planASEJ, EstadoPlanASEJ.PROPUESTO);
        planServiceASEJ.generarCronogramaASEJ(new CronogramaFormASEJ(planASEJ, null, 2, LocalDate.now().plusDays(5), FrecuenciaASEJ.MENSUAL));
        iniciarSesionASEJ("recepcion");
        assertThat(pagoServiceASEJ.cuotasPendientesASEJ(otroASEJ)).isEmpty();
        assertThatThrownBy(() -> pagoServiceASEJ.registrarASEJ(new PagoFormASEJ(otroASEJ, new BigDecimal("10"), MedioPagoASEJ.EFECTIVO, null)))
                .hasMessageContaining("no tiene cuotas pendientes");
    }

    @Test
    void unPagoPuedeCubrirCuotasDeVariosPlanesASEJ() {
        long segundoASEJ = planAceptadoASEJ(pacienteIdASEJ, "200.00", "0", 1);
        iniciarSesionASEJ("recepcion");
        PagoDtoASEJ pASEJ = pagarASEJ("700.00", MedioPagoASEJ.TRANSFERENCIA);
        assertThat(pASEJ.aplicacionesASEJ()).extracting(AplicacionDtoASEJ::planIdASEJ, AplicacionDtoASEJ::numeroCuotaASEJ)
                .containsExactly(tuple(planIdASEJ, 0), tuple(planIdASEJ, 1), tuple(segundoASEJ, 1), tuple(planIdASEJ, 2));
        assertThat(jdbcASEJ.queryForObject("SELECT plan_id FROM pago WHERE id = ?", Long.class, pASEJ.idASEJ()))
                .as("repartido entre planes").isNull();
        assertThat(planServiceASEJ.obtenerASEJ(segundoASEJ).estadoASEJ()).isEqualTo(EstadoPlanASEJ.EN_CURSO);
    }

    @Test
    void anularRevierteLasCuotasYConservaLasAplicacionesASEJ() {
        PagoDtoASEJ pASEJ = pagarASEJ("450.00", MedioPagoASEJ.EFECTIVO);
        assertThatThrownBy(() -> pagoServiceASEJ.anularASEJ(pASEJ.idASEJ(), "Error de digitación"))
                .as("recepción no anula").isInstanceOf(PermisoDenegadoExceptionASEJ.class);

        iniciarSesionASEJ("admin");
        assertThatThrownBy(() -> pagoServiceASEJ.anularASEJ(pASEJ.idASEJ(), "corto")).hasMessageContaining("al menos 10");
        PagoDtoASEJ anuladoASEJ = pagoServiceASEJ.anularASEJ(pASEJ.idASEJ(), "  Error de digitación del monto  ");
        assertThat(anuladoASEJ.anuladoASEJ()).isTrue();
        assertThat(anuladoASEJ.motivoAnulacionASEJ()).isEqualTo("Error de digitación del monto");
        assertThat(anuladoASEJ.anuladoEnASEJ()).isNotNull();
        assertThat(anuladoASEJ.aplicacionesASEJ()).as("se conservan para trazabilidad").hasSize(3);
        assertThat(pagoServiceASEJ.saldoASEJ(pacienteIdASEJ)).isEqualByComparingTo("1000.00");
        assertThat(pagoServiceASEJ.cuotasPendientesASEJ(pacienteIdASEJ)).hasSize(4).allMatch(cASEJ -> cASEJ.pagadoASEJ().signum() == 0);
        assertThatThrownBy(() -> pagoServiceASEJ.anularASEJ(pASEJ.idASEJ(), "Otra vez el mismo pago")).hasMessageContaining("ya está anulado");

        iniciarSesionASEJ("odontologo");
        assertThat(planServiceASEJ.obtenerASEJ(planIdASEJ).tienePagosASEJ()).isFalse();
    }

    @Test
    void estadoDeCuentaConVencidasASEJ() {
        jdbcASEJ.update("UPDATE cuota SET vencimiento = ? WHERE plan_id = ? AND numero = 1",
                LocalDate.now().minusDays(3).toString(), planIdASEJ);
        pagarASEJ("150.00", MedioPagoASEJ.EFECTIVO);

        EstadoCuentaDtoASEJ eASEJ = pagoServiceASEJ.estadoCuentaASEJ(pacienteIdASEJ);
        assertThat(eASEJ.totalASEJ()).isEqualByComparingTo("1000.00");
        assertThat(eASEJ.pagadoASEJ()).isEqualByComparingTo("150.00");
        assertThat(eASEJ.saldoASEJ()).isEqualByComparingTo("850.00");
        assertThat(eASEJ.vencidoASEJ()).isEqualByComparingTo("150.00");
        assertThat(eASEJ.cuotasASEJ()).extracting(CuotaPacienteDtoASEJ::numeroASEJ, cASEJ -> cASEJ.saldoASEJ().toPlainString(), CuotaPacienteDtoASEJ::estadoASEJ)
                .containsExactly(tuple(1, "150.00", EstadoCuotaASEJ.VENCIDA), tuple(0, "100.00", EstadoCuotaASEJ.PENDIENTE),
                        tuple(2, "300.00", EstadoCuotaASEJ.PENDIENTE), tuple(3, "300.00", EstadoCuotaASEJ.PENDIENTE));
        assertThat(eASEJ.pagosASEJ()).hasSize(1);
    }

    @Test
    void cajaDelDiaSumaPorMedioSinLosAnuladosASEJ() {
        ResumenCajaDtoASEJ antesASEJ = pagoServiceASEJ.cajaDelDiaASEJ(LocalDate.now());
        pagarASEJ("100.00", MedioPagoASEJ.EFECTIVO);
        pagarASEJ("50.50", MedioPagoASEJ.YAPE);
        PagoDtoASEJ anuladoASEJ = pagarASEJ("20.00", MedioPagoASEJ.EFECTIVO);
        iniciarSesionASEJ("admin");
        pagoServiceASEJ.anularASEJ(anuladoASEJ.idASEJ(), "Cobro duplicado por error");

        ResumenCajaDtoASEJ hoyASEJ = pagoServiceASEJ.cajaDelDiaASEJ(LocalDate.now());
        assertThat(hoyASEJ.pagosASEJ()).hasSize(antesASEJ.pagosASEJ().size() + 3);
        assertThat(hoyASEJ.porMedioASEJ().get(MedioPagoASEJ.EFECTIVO).subtract(antesASEJ.porMedioASEJ().get(MedioPagoASEJ.EFECTIVO)))
                .isEqualByComparingTo("100.00");
        assertThat(hoyASEJ.porMedioASEJ().get(MedioPagoASEJ.YAPE).subtract(antesASEJ.porMedioASEJ().get(MedioPagoASEJ.YAPE)))
                .isEqualByComparingTo("50.50");
        assertThat(hoyASEJ.totalASEJ().subtract(antesASEJ.totalASEJ())).isEqualByComparingTo("150.50");
        assertThat(pagoServiceASEJ.cajaDelDiaASEJ(LocalDate.now().minusDays(400)).pagosASEJ()).isEmpty();
    }

    @Test
    void reciboEnPdfConMontoEnLetrasYMarcaDeAnuladoASEJ() throws Exception {
        PagoDtoASEJ pASEJ = pagarASEJ("1000.00", MedioPagoASEJ.EFECTIVO);
        String textoASEJ = new PdfTextExtractor(new PdfReader(pagoServiceASEJ.pdfReciboASEJ(pASEJ.idASEJ()))).getTextFromPage(1);
        assertThat(textoASEJ).contains(pASEJ.numeroReciboASEJ(), "MIL CON 00/100 SOLES", "Efectivo", "Plan N° " + planIdASEJ + " · Inicial")
                .doesNotContain("ANULADO");

        iniciarSesionASEJ("admin");
        pagoServiceASEJ.anularASEJ(pASEJ.idASEJ(), "Pago registrado al paciente equivocado");
        String anuladoASEJ = new PdfTextExtractor(new PdfReader(pagoServiceASEJ.pdfReciboASEJ(pASEJ.idASEJ()))).getTextFromPage(1);
        assertThat(anuladoASEJ).contains("ANULADO", "Pago registrado al paciente equivocado");
    }
}
