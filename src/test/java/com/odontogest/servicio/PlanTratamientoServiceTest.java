/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio;

import com.odontogest.PruebaIntegracion;
import com.odontogest.dominio.finanzas.EstadoCuota;
import com.odontogest.dominio.finanzas.Frecuencia;
import com.odontogest.dominio.odontograma.EstadoHallazgo;
import com.odontogest.dominio.odontograma.Superficie;
import com.odontogest.dominio.tratamiento.FormaCobro;
import com.odontogest.excepcion.NegocioException;
import com.odontogest.excepcion.PermisoDenegadoException;
import com.odontogest.modelo.CategoriaProcedimiento;
import com.odontogest.modelo.EstadoPlan;
import com.odontogest.modelo.EstadoPlanItem;
import com.odontogest.modelo.Sexo;
import com.odontogest.modelo.TipoConsentimiento;
import com.odontogest.modelo.TipoDocumento;
import com.odontogest.servicio.dto.AtencionForm;
import com.odontogest.servicio.dto.CronogramaForm;
import com.odontogest.servicio.dto.CuotaDto;
import com.odontogest.servicio.dto.HallazgoTipoDto;
import com.odontogest.servicio.dto.ItemAtencionDto;
import com.odontogest.servicio.dto.NuevoHallazgo;
import com.odontogest.servicio.dto.PacienteForm;
import com.odontogest.servicio.dto.PlanDto;
import com.odontogest.servicio.dto.PlanItemDto;
import com.odontogest.servicio.dto.PlanItemForm;
import com.odontogest.servicio.dto.ProcedimientoDto;
import com.odontogest.servicio.dto.ProcedimientoForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Plan de tratamiento (Fase 4): sugerencias desde el odontograma firmado, totales exactos, transiciones de estado,
 * cronograma bloqueado con pagos, consentimiento de tratamiento, procedimientos realizados en la atención y PDF.
 */
class PlanTratamientoServiceTest extends PruebaIntegracion {

    @Autowired private PacienteService pacienteService;
    @Autowired private AtencionService atencionService;
    @Autowired private OdontogramaService odontogramaService;
    @Autowired private CatalogoHallazgoService catalogoService;
    @Autowired private ProcedimientoService procedimientoService;
    @Autowired private ConsentimientoService consentimientoService;
    @Autowired private PlanTratamientoService planService;
    @Autowired private JdbcTemplate jdbc;

    private Map<String, Long> tipos;
    private Map<String, Long> procedimientos;
    private long pacienteId;
    private long atencionId;

    @BeforeEach
    void preparar() {
        iniciarSesion("odontologo");
        tipos = catalogoService.listarTipos(true).stream().collect(Collectors.toMap(HallazgoTipoDto::codigo, HallazgoTipoDto::id));
        procedimientos = procedimientoService.listar(true).stream()
                .collect(Collectors.toMap(ProcedimientoDto::codigo, ProcedimientoDto::id));
        pacienteId = pacienteService.guardar(new PacienteForm(null, TipoDocumento.DNI, dniAleatorio(), "Paciente",
                "Plan" + sufijo(), null, LocalDate.now().minusYears(40), Sexo.M, null, null, null, null, null, null,
                null, null, null, true));
        atencionId = atencionService.abrir(new AtencionForm(null, pacienteId, null, "Dolor", null, null, null, null));
    }

    /** Odontograma demo: caries de 1 y 2 superficies, conducto en mal estado, remanente y restauración en buen estado. */
    private void odontogramaDemoFirmado() {
        long o = odontogramaService.obtenerVista(pacienteId, null).odontogramaId();
        registrar(o, "CARIES", 36, Set.of(Superficie.O), null, "CD");
        registrar(o, "CARIES", 46, Set.of(Superficie.O, Superficie.M), null, "CD");
        registrar(o, "TRATAMIENTO_CONDUCTOS", 16, Set.of(), EstadoHallazgo.MALO, "TC");
        registrar(o, "REMANENTE_RADICULAR", 45, Set.of(), null, null);
        registrar(o, "RESTAURACION_DEFINITIVA", 26, Set.of(Superficie.O), EstadoHallazgo.BUENO, "R");
        odontogramaService.cerrar(o);
    }

    private void registrar(long o, String tipo, int pieza, Set<Superficie> sup, EstadoHallazgo estado, String sigla) {
        odontogramaService.registrarHallazgo(new NuevoHallazgo(o, tipos.get(tipo), pieza, null, sup, estado, sigla, null));
    }

    private PlanDto planSugerido() {
        odontogramaDemoFirmado();
        return planService.obtener(planService.crear(pacienteId, null, true));
    }

    private PlanDto aceptado() {
        PlanDto p = planSugerido();
        planService.cambiarEstado(p.id(), EstadoPlan.PROPUESTO);
        return planService.aceptar(p.id(), "Paciente de prueba");
    }

    // ------------------------------------------------------------------ sugerencias y totales

    @Test
    void lasSugerenciasSonCoherentesConElOdontogramaFirmado() {
        PlanDto p = planSugerido();

        assertThat(p.estado()).isEqualTo(EstadoPlan.BORRADOR);
        assertThat(p.odontogramaId()).isNotNull();
        assertThat(p.odontologoNombre()).isEqualTo("Carlos Alberto Quispe Mamani");
        assertThat(p.items()).extracting(PlanItemDto::fase, PlanItemDto::procedimientoCodigo, PlanItemDto::pieza)
                .containsExactly(
                        tuple(1, "CONS", null),
                        tuple(1, "ENDO-M", 16),
                        tuple(1, "EXO-RR", 45),
                        tuple(2, "RES-C", 46),
                        tuple(2, "RES-S", 36),
                        tuple(4, "PROF", null));
        assertThat(p.items()).as("la restauración en buen estado no genera tratamiento")
                .noneMatch(i -> Integer.valueOf(26).equals(i.pieza()));
        assertThat(p.items()).filteredOn(i -> i.pieza() != null).allMatch(i -> i.hallazgoId() != null);
        assertThat(p.items().stream().filter(i -> "RES-C".equals(i.procedimientoCodigo())).findFirst().orElseThrow()
                .superficiesTexto()).isEqualTo("OM");
        assertThat(p.subtotal()).isEqualByComparingTo("1150.00");      // 50 + 600 + 120 + 180 + 120 + 80
        assertThat(p.total()).isEqualByComparingTo("1150.00");

        assertThatThrownBy(() -> planService.sugerir(p.id()))
                .isInstanceOf(NegocioException.class).hasMessageContaining("ya están en el plan");
    }

    @Test
    void sinOdontogramaFirmadoNoHaySugerencias() {
        assertThatThrownBy(() -> planService.crear(pacienteId, null, true))
                .isInstanceOf(NegocioException.class).hasMessageContaining("odontograma firmado");
        long plan = planService.crear(pacienteId, null, false);
        assertThat(planService.obtener(plan).items()).isEmpty();
    }

    @Test
    void totalesExactosConDescuentos() {
        long plan = planService.crear(pacienteId, null, false);
        PlanDto p = planService.guardarItem(new PlanItemForm(null, plan, procedimientos.get("CONS"), null, null, 1, 1,
                null, null));
        p = planService.guardarItem(new PlanItemForm(null, plan, procedimientos.get("RES-C"), 46, Set.of(Superficie.O, Superficie.M),
                2, 1, new BigDecimal("180.00"), new BigDecimal("30.50")));
        p = planService.guardarItem(new PlanItemForm(null, plan, procedimientos.get("RX-PA"), null, null, 1, 3, null, null));
        assertThat(p.subtotal()).isEqualByComparingTo("274.50");       // 50 + (180 − 30.50) + 3 × 25

        p = planService.actualizarCabecera(plan, new BigDecimal("74.50"), "  Pagará en efectivo  ");
        assertThat(p.descuento()).isEqualByComparingTo("74.50");
        assertThat(p.total()).isEqualByComparingTo("200.00");
        assertThat(p.observaciones()).isEqualTo("Pagará en efectivo");

        PlanItemDto resina = p.items().stream().filter(i -> "RES-C".equals(i.procedimientoCodigo())).findFirst().orElseThrow();
        p = planService.actualizarDescuentoItem(resina.id(), BigDecimal.ZERO);
        assertThat(p.subtotal()).isEqualByComparingTo("305.00");
        assertThat(p.total()).isEqualByComparingTo("230.50");

        assertThatThrownBy(() -> planService.actualizarDescuentoItem(resina.id(), new BigDecimal("180.01")))
                .hasMessageContaining("descuento del ítem");
        assertThatThrownBy(() -> planService.actualizarCabecera(plan, new BigDecimal("305.01"), null))
                .hasMessageContaining("descuento global");
        assertThatThrownBy(() -> planService.actualizarCabecera(plan, new BigDecimal("0.001"), null))
                .hasMessageContaining("2 decimales");

        PlanItemDto cons = p.items().stream().filter(i -> "CONS".equals(i.procedimientoCodigo())).findFirst().orElseThrow();
        p = planService.eliminarItem(cons.id());
        assertThat(p.total()).isEqualByComparingTo("180.50");                // 255 − 74.50
        assertThat(jdbc.queryForObject("SELECT total_cent FROM plan_tratamiento WHERE id = ?", Long.class, plan)).isEqualTo(18050L);

        planService.actualizarCabecera(plan, new BigDecimal("100.00"), null);
        PlanItemDto res = p.items().stream().filter(i -> "RES-C".equals(i.procedimientoCodigo())).findFirst().orElseThrow();
        assertThatThrownBy(() -> planService.eliminarItem(res.id()))
                .as("sin la resina el subtotal (75) no cubre el descuento global").hasMessageContaining("descuento global");
        assertThat(planService.obtener(plan).items()).as("la eliminación se revierte").hasSize(2);
    }

    @Test
    void porSuperficieLaCantidadEsElNumeroDeSuperficies() {
        long plan = planService.crear(pacienteId, null, false);
        iniciarSesion("admin");
        long sellante = procedimientoService.guardar(new ProcedimientoForm(null, "SUP-" + sufijo().substring(0, 4).toUpperCase(),
                "Resina por superficie", CategoriaProcedimiento.OPERATORIA, new BigDecimal("70.00"), FormaCobro.SUPERFICIE, 30, true));
        iniciarSesion("odontologo");
        PlanDto p = planService.guardarItem(new PlanItemForm(null, plan, sellante, 11, Set.of(Superficie.V, Superficie.O, Superficie.M),
                2, 1, null, null));
        PlanItemDto i = p.items().get(0);
        assertThat(i.cantidad()).isEqualTo(3);
        assertThat(i.superficiesTexto()).isEqualTo("IMV");
        assertThat(i.total()).isEqualByComparingTo("210.00");
        assertThatThrownBy(() -> planService.guardarItem(new PlanItemForm(null, plan, sellante, null, Set.of(), 2, 1, null, null)))
                .hasMessageContaining("indique la pieza");
        assertThatThrownBy(() -> planService.guardarItem(new PlanItemForm(null, plan, procedimientos.get("CONS"), 19, null, 1, 1, null, null)))
                .hasMessageContaining("19 no es un número FDI");
    }

    // ------------------------------------------------------------------ estados

    @Test
    void transicionesValidasYCondiciones() {
        long vacio = planService.crear(pacienteId, null, false);
        assertThatThrownBy(() -> planService.cambiarEstado(vacio, EstadoPlan.PROPUESTO))
                .hasMessageContaining("al menos un procedimiento");
        assertThatThrownBy(() -> planService.cambiarEstado(vacio, EstadoPlan.EN_CURSO))
                .hasMessageContaining("no puede pasar a en curso");
        assertThatThrownBy(() -> planService.cambiarEstado(vacio, EstadoPlan.ACEPTADO))
                .hasMessageContaining("consentimiento");

        PlanDto p = planSugerido();
        p = planService.cambiarEstado(p.id(), EstadoPlan.PROPUESTO);
        assertThat(p.transiciones()).containsExactlyInAnyOrder(EstadoPlan.BORRADOR, EstadoPlan.ACEPTADO, EstadoPlan.ANULADO);
        long id = p.id();
        assertThatThrownBy(() -> planService.guardarItem(new PlanItemForm(null, id, procedimientos.get("CONS"), null, null, 1, 1, null, null)))
                .hasMessageContaining("solo se editan los planes en borrador");
        assertThatThrownBy(() -> planService.aceptar(id, "  ")).hasMessageContaining("quien firma");

        p = planService.aceptar(id, "Paciente de prueba");
        assertThat(p.estado()).isEqualTo(EstadoPlan.ACEPTADO);
        assertThat(p.aceptadoEn()).isNotNull();
        assertThat(p.consentimientoFirmadoPor()).isEqualTo("Paciente de prueba");
        assertThat(consentimientoService.listar(pacienteId)).anyMatch(c -> c.tipo() == TipoConsentimiento.TRATAMIENTO);
        BigDecimal total = p.total();
        assertThat(p.cuotas()).as("sin cronograma: cuota única al contado").singleElement().satisfies(c -> {
            assertThat(c.numero()).isZero();
            assertThat(c.monto()).isEqualByComparingTo(total);
            assertThat(c.vencimiento()).isEqualTo(LocalDate.now());
        });
        assertThatThrownBy(() -> planService.cambiarEstado(id, EstadoPlan.FINALIZADO)).hasMessageContaining("no puede pasar");
        assertThatThrownBy(() -> planService.cambiarEstado(id, EstadoPlan.BORRADOR)).hasMessageContaining("no puede pasar");

        planService.cambiarEstado(id, EstadoPlan.ANULADO);
        assertThatThrownBy(() -> planService.cambiarEstado(id, EstadoPlan.EN_CURSO)).hasMessageContaining("anulado no puede pasar");
    }

    @Test
    void volverABorradorBorraElCronograma() {
        PlanDto p = planSugerido();
        planService.cambiarEstado(p.id(), EstadoPlan.PROPUESTO);
        p = planService.generarCronograma(new CronogramaForm(p.id(), new BigDecimal("150"), 2, LocalDate.now().plusDays(10),
                Frecuencia.MENSUAL));
        assertThat(p.cuotas()).hasSize(3);
        p = planService.cambiarEstado(p.id(), EstadoPlan.BORRADOR);
        assertThat(p.cuotas()).isEmpty();
        assertThat(p.editable()).isTrue();
    }

    // ------------------------------------------------------------------ cronograma

    @Test
    void cronogramaConRepartoExactoYBloqueadoConPagos() {
        long plan = planService.crear(pacienteId, null, false);
        planService.guardarItem(new PlanItemForm(null, plan, procedimientos.get("PROF"), null, null, 4, 1, new BigDecimal("1000.00"), null));
        assertThatThrownBy(() -> planService.generarCronograma(new CronogramaForm(plan, null, 3, LocalDate.now(), Frecuencia.MENSUAL)))
                .hasMessageContaining("Proponga el plan");
        planService.cambiarEstado(plan, EstadoPlan.PROPUESTO);

        LocalDate lunes = LocalDate.now().plusWeeks(1).with(DayOfWeek.MONDAY);
        PlanDto p = planService.generarCronograma(new CronogramaForm(plan, null, 3, lunes, Frecuencia.SEMANAL));
        assertThat(p.cuotas()).extracting(CuotaDto::numero, c -> c.monto().toPlainString(), CuotaDto::vencimiento, CuotaDto::estado)
                .containsExactly(
                        tuple(1, "333.34", lunes, EstadoCuota.PENDIENTE),
                        tuple(2, "333.33", lunes.plusWeeks(1), EstadoCuota.PENDIENTE),
                        tuple(3, "333.33", lunes.plusWeeks(2), EstadoCuota.PENDIENTE));

        p = planService.generarCronograma(new CronogramaForm(plan, new BigDecimal("400"), 2, lunes, Frecuencia.QUINCENAL));
        assertThat(p.cuotas()).extracting(c -> c.monto().toPlainString()).containsExactly("400.00", "300.00", "300.00");
        assertThat(p.cuotas().get(0).vencimiento()).isEqualTo(LocalDate.now());
        assertThatThrownBy(() -> planService.generarCronograma(new CronogramaForm(plan, new BigDecimal("1000"), 2, lunes, Frecuencia.MENSUAL)))
                .hasMessageContaining("inicial cubre el total");
        assertThatThrownBy(() -> planService.generarCronograma(new CronogramaForm(plan, null, 2, LocalDate.now().minusDays(1), Frecuencia.MENSUAL)))
                .hasMessageContaining("anterior a hoy");
        assertThatThrownBy(() -> planService.generarCronograma(new CronogramaForm(plan, null, 2, null, null)))
                .hasMessageContaining("primer vencimiento");

        // La caja (Fase 5) registra un pago parcial en la cuota inicial: el cronograma queda bloqueado.
        jdbc.update("UPDATE cuota SET pagado_cent = 10000 WHERE plan_id = ? AND numero = 0", plan);
        PlanDto conPago = planService.obtener(plan);
        assertThat(conPago.tienePagos()).isTrue();
        assertThat(conPago.cuotas().get(0).estado()).isEqualTo(EstadoCuota.PARCIAL);
        assertThatThrownBy(() -> planService.generarCronograma(new CronogramaForm(plan, null, 3, lunes, Frecuencia.MENSUAL)))
                .hasMessageContaining("ya tiene pagos");
        assertThatThrownBy(() -> planService.cambiarEstado(plan, EstadoPlan.ANULADO)).hasMessageContaining("ya tiene pagos");
        assertThatThrownBy(() -> planService.cambiarEstado(plan, EstadoPlan.BORRADOR)).hasMessageContaining("ya tiene pagos");
    }

    // ------------------------------------------------------------------ atención

    @Test
    void procedimientosRealizadosEnLaAtencionYFinalizacion() {
        PlanDto p = aceptado();
        List<ItemAtencionDto> items = planService.itemsParaAtencion(atencionId);
        assertThat(items).hasSize(6).allMatch(ItemAtencionDto::editable).noneMatch(ItemAtencionDto::realizadoAqui);

        long primero = items.get(0).itemId();
        items = planService.marcarEnAtencion(atencionId, primero, true);
        assertThat(items.get(0).realizadoAqui()).isTrue();
        assertThat(planService.obtener(p.id()).estado()).as("el primer procedimiento inicia el plan").isEqualTo(EstadoPlan.EN_CURSO);
        assertThatThrownBy(() -> planService.marcarEnAtencion(atencionId, primero, true)).hasMessageContaining("ya figura");

        planService.marcarEnAtencion(atencionId, primero, false);
        assertThat(planService.obtener(p.id()).items()).allMatch(i -> i.estado() == EstadoPlanItem.PENDIENTE);

        assertThatThrownBy(() -> planService.cambiarEstado(p.id(), EstadoPlan.FINALIZADO)).hasMessageContaining("Quedan 6");
        for (ItemAtencionDto i : planService.itemsParaAtencion(atencionId)) {
            planService.marcarEnAtencion(atencionId, i.itemId(), true);
        }
        PlanDto fin = planService.cambiarEstado(p.id(), EstadoPlan.FINALIZADO);
        assertThat(fin.realizados()).isEqualTo(6);
        assertThat(fin.items()).allMatch(i -> i.atencionId() == atencionId && i.realizadoEn() != null);
        assertThat(planService.itemsParaAtencion(atencionId)).as("se siguen viendo los realizados aquí, ya sin edición")
                .hasSize(6).allMatch(ItemAtencionDto::realizadoAqui).noneMatch(ItemAtencionDto::editable);

        // Otra atención ya no los ve: están realizados en la anterior.
        atencionService.cerrar(atencionId);
        long otra = atencionService.abrir(new AtencionForm(null, pacienteId, null, "Control", null, null, null, null));
        assertThat(planService.itemsParaAtencion(otra)).isEmpty();
        assertThatThrownBy(() -> planService.marcarEnAtencion(atencionId, primero, false)).hasMessageContaining("atención está cerrada");
    }

    // ------------------------------------------------------------------ permisos y PDF

    @Test
    void recepcionNoEditaPlanes() {
        PlanDto p = planSugerido();
        iniciarSesion("recepcion");
        assertThat(planService.obtener(p.id()).items()).isNotEmpty();
        assertThatThrownBy(() -> planService.cambiarEstado(p.id(), EstadoPlan.PROPUESTO)).isInstanceOf(PermisoDenegadoException.class);
        assertThatThrownBy(() -> planService.crear(pacienteId, 1L, false)).isInstanceOf(PermisoDenegadoException.class);
    }

    @Test
    void presupuestoYConsentimientoEnPdf() {
        PlanDto p = planSugerido();
        planService.cambiarEstado(p.id(), EstadoPlan.PROPUESTO);
        planService.generarCronograma(new CronogramaForm(p.id(), new BigDecimal("150"), 4, LocalDate.now().plusDays(7), Frecuencia.MENSUAL));
        byte[] presupuesto = planService.pdfPresupuesto(p.id());
        byte[] consentimiento = planService.pdfConsentimiento(p.id());
        for (byte[] pdf : List.of(presupuesto, consentimiento)) {
            assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
            assertThat(pdf.length).isGreaterThan(2000);
        }
        long vacio = planService.crear(pacienteId, null, false);
        assertThatThrownBy(() -> planService.pdfConsentimiento(vacio)).hasMessageContaining("no tiene procedimientos");
        assertThat(planService.pdfPresupuesto(vacio)).isNotEmpty();
    }
}
