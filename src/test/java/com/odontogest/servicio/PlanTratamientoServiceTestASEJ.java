package com.odontogest.servicio;

import com.odontogest.PruebaIntegracionASEJ;
import com.odontogest.dominio.finanzas.EstadoCuotaASEJ;
import com.odontogest.dominio.finanzas.FrecuenciaASEJ;
import com.odontogest.dominio.odontograma.EstadoHallazgoASEJ;
import com.odontogest.dominio.odontograma.SuperficieASEJ;
import com.odontogest.dominio.tratamiento.FormaCobroASEJ;
import com.odontogest.excepcion.NegocioExceptionASEJ;
import com.odontogest.excepcion.PermisoDenegadoExceptionASEJ;
import com.odontogest.modelo.CategoriaProcedimientoASEJ;
import com.odontogest.modelo.EstadoPlanASEJ;
import com.odontogest.modelo.EstadoPlanItemASEJ;
import com.odontogest.modelo.SexoASEJ;
import com.odontogest.modelo.TipoConsentimientoASEJ;
import com.odontogest.modelo.TipoDocumentoASEJ;
import com.odontogest.servicio.dto.AtencionFormASEJ;
import com.odontogest.servicio.dto.CronogramaFormASEJ;
import com.odontogest.servicio.dto.CuotaDtoASEJ;
import com.odontogest.servicio.dto.HallazgoTipoDtoASEJ;
import com.odontogest.servicio.dto.ItemAtencionDtoASEJ;
import com.odontogest.servicio.dto.NuevoHallazgoASEJ;
import com.odontogest.servicio.dto.PacienteFormASEJ;
import com.odontogest.servicio.dto.PlanDtoASEJ;
import com.odontogest.servicio.dto.PlanItemDtoASEJ;
import com.odontogest.servicio.dto.PlanItemFormASEJ;
import com.odontogest.servicio.dto.ProcedimientoDtoASEJ;
import com.odontogest.servicio.dto.ProcedimientoFormASEJ;
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

class PlanTratamientoServiceTestASEJ extends PruebaIntegracionASEJ {

    @Autowired private PacienteServiceASEJ pacienteServiceASEJ;
    @Autowired private AtencionServiceASEJ atencionServiceASEJ;
    @Autowired private OdontogramaServiceASEJ odontogramaServiceASEJ;
    @Autowired private CatalogoHallazgoServiceASEJ catalogoServiceASEJ;
    @Autowired private ProcedimientoServiceASEJ procedimientoServiceASEJ;
    @Autowired private ConsentimientoServiceASEJ consentimientoServiceASEJ;
    @Autowired private PlanTratamientoServiceASEJ planServiceASEJ;
    @Autowired private JdbcTemplate jdbcASEJ;

    private Map<String, Long> tiposASEJ;
    private Map<String, Long> procedimientosASEJ;
    private long pacienteIdASEJ;
    private long atencionIdASEJ;

    @BeforeEach
    void prepararASEJ() {
        iniciarSesionASEJ("odontologo");
        tiposASEJ = catalogoServiceASEJ.listarTiposASEJ(true).stream().collect(Collectors.toMap(HallazgoTipoDtoASEJ::codigoASEJ, HallazgoTipoDtoASEJ::idASEJ));
        procedimientosASEJ = procedimientoServiceASEJ.listarASEJ(true).stream()
                .collect(Collectors.toMap(ProcedimientoDtoASEJ::codigoASEJ, ProcedimientoDtoASEJ::idASEJ));
        pacienteIdASEJ = pacienteServiceASEJ.guardarASEJ(new PacienteFormASEJ(null, TipoDocumentoASEJ.DNI, dniAleatorioASEJ(), "Paciente",
                "Plan" + sufijoASEJ(), null, LocalDate.now().minusYears(40), SexoASEJ.M, null, null, null, null, null, null,
                null, null, null, true));
        atencionIdASEJ = atencionServiceASEJ.abrirASEJ(new AtencionFormASEJ(null, pacienteIdASEJ, null, "Dolor", null, null, null, null));
    }

    private void odontogramaDemoFirmadoASEJ() {
        long oASEJ = odontogramaServiceASEJ.obtenerVistaASEJ(pacienteIdASEJ, null).odontogramaIdASEJ();
        registrarASEJ(oASEJ, "CARIES", 36, Set.of(SuperficieASEJ.O), null, "CD");
        registrarASEJ(oASEJ, "CARIES", 46, Set.of(SuperficieASEJ.O, SuperficieASEJ.M), null, "CD");
        registrarASEJ(oASEJ, "TRATAMIENTO_CONDUCTOS", 16, Set.of(), EstadoHallazgoASEJ.MALO, "TC");
        registrarASEJ(oASEJ, "REMANENTE_RADICULAR", 45, Set.of(), null, null);
        registrarASEJ(oASEJ, "RESTAURACION_DEFINITIVA", 26, Set.of(SuperficieASEJ.O), EstadoHallazgoASEJ.BUENO, "R");
        odontogramaServiceASEJ.cerrarASEJ(oASEJ);
    }

    private void registrarASEJ(long oASEJ, String tipoASEJ, int piezaASEJ, Set<SuperficieASEJ> supASEJ, EstadoHallazgoASEJ estadoASEJ, String siglaASEJ) {
        odontogramaServiceASEJ.registrarHallazgoASEJ(new NuevoHallazgoASEJ(oASEJ, tiposASEJ.get(tipoASEJ), piezaASEJ, null, supASEJ, estadoASEJ, siglaASEJ, null));
    }

    private PlanDtoASEJ planSugeridoASEJ() {
        odontogramaDemoFirmadoASEJ();
        return planServiceASEJ.obtenerASEJ(planServiceASEJ.crearASEJ(pacienteIdASEJ, null, true));
    }

    private PlanDtoASEJ aceptadoASEJ() {
        PlanDtoASEJ pASEJ = planSugeridoASEJ();
        planServiceASEJ.cambiarEstadoASEJ(pASEJ.idASEJ(), EstadoPlanASEJ.PROPUESTO);
        return planServiceASEJ.aceptarASEJ(pASEJ.idASEJ(), "Paciente de prueba");
    }

    @Test
    void lasSugerenciasSonCoherentesConElOdontogramaFirmadoASEJ() {
        PlanDtoASEJ pASEJ = planSugeridoASEJ();

        assertThat(pASEJ.estadoASEJ()).isEqualTo(EstadoPlanASEJ.BORRADOR);
        assertThat(pASEJ.odontogramaIdASEJ()).isNotNull();
        assertThat(pASEJ.odontologoNombreASEJ()).isEqualTo("Carlos Alberto Quispe Mamani");
        assertThat(pASEJ.itemsASEJ()).extracting(PlanItemDtoASEJ::faseASEJ, PlanItemDtoASEJ::procedimientoCodigoASEJ, PlanItemDtoASEJ::piezaASEJ)
                .containsExactly(
                        tuple(1, "CONS", null),
                        tuple(1, "ENDO-M", 16),
                        tuple(1, "EXO-RR", 45),
                        tuple(2, "RES-C", 46),
                        tuple(2, "RES-S", 36),
                        tuple(4, "PROF", null));
        assertThat(pASEJ.itemsASEJ()).as("la restauración en buen estado no genera tratamiento")
                .noneMatch(iASEJ -> Integer.valueOf(26).equals(iASEJ.piezaASEJ()));
        assertThat(pASEJ.itemsASEJ()).filteredOn(iASEJ -> iASEJ.piezaASEJ() != null).allMatch(iASEJ -> iASEJ.hallazgoIdASEJ() != null);
        assertThat(pASEJ.itemsASEJ().stream().filter(iASEJ -> "RES-C".equals(iASEJ.procedimientoCodigoASEJ())).findFirst().orElseThrow()
                .superficiesTextoASEJ()).isEqualTo("OM");
        assertThat(pASEJ.subtotalASEJ()).isEqualByComparingTo("1150.00");
        assertThat(pASEJ.totalASEJ()).isEqualByComparingTo("1150.00");

        assertThatThrownBy(() -> planServiceASEJ.sugerirASEJ(pASEJ.idASEJ()))
                .isInstanceOf(NegocioExceptionASEJ.class).hasMessageContaining("ya están en este plan");
    }

    @Test
    void unHallazgoYaTratadoEnOtroPlanNoSeVuelveASugerirASEJ() {
        PlanDtoASEJ primeroASEJ = planSugeridoASEJ();
        PlanDtoASEJ segundoASEJ = planServiceASEJ.obtenerASEJ(planServiceASEJ.crearASEJ(pacienteIdASEJ, null, true));
        assertThat(segundoASEJ.itemsASEJ()).as("solo las reglas generales: los hallazgos ya están en el primer plan")
                .extracting(PlanItemDtoASEJ::procedimientoCodigoASEJ).containsExactly("CONS", "PROF");

        planServiceASEJ.cambiarEstadoASEJ(primeroASEJ.idASEJ(), EstadoPlanASEJ.ANULADO);
        PlanDtoASEJ conSugerenciasASEJ = planServiceASEJ.sugerirASEJ(segundoASEJ.idASEJ());
        assertThat(conSugerenciasASEJ.itemsASEJ()).as("al anular el primer plan, sus hallazgos vuelven a sugerirse")
                .extracting(PlanItemDtoASEJ::procedimientoCodigoASEJ)
                .containsExactly("CONS", "ENDO-M", "EXO-RR", "RES-C", "RES-S", "PROF");
    }

    @Test
    void sinOdontogramaFirmadoNoHaySugerenciasASEJ() {
        assertThatThrownBy(() -> planServiceASEJ.crearASEJ(pacienteIdASEJ, null, true))
                .isInstanceOf(NegocioExceptionASEJ.class).hasMessageContaining("odontograma firmado");
        long planASEJ = planServiceASEJ.crearASEJ(pacienteIdASEJ, null, false);
        assertThat(planServiceASEJ.obtenerASEJ(planASEJ).itemsASEJ()).isEmpty();
    }

    @Test
    void totalesExactosConDescuentosASEJ() {
        long planASEJ = planServiceASEJ.crearASEJ(pacienteIdASEJ, null, false);
        PlanDtoASEJ pASEJ = planServiceASEJ.guardarItemASEJ(new PlanItemFormASEJ(null, planASEJ, procedimientosASEJ.get("CONS"), null, null, 1, 1,
                null, null));
        pASEJ = planServiceASEJ.guardarItemASEJ(new PlanItemFormASEJ(null, planASEJ, procedimientosASEJ.get("RES-C"), 46, Set.of(SuperficieASEJ.O, SuperficieASEJ.M),
                2, 1, new BigDecimal("180.00"), new BigDecimal("30.50")));
        pASEJ = planServiceASEJ.guardarItemASEJ(new PlanItemFormASEJ(null, planASEJ, procedimientosASEJ.get("RX-PA"), null, null, 1, 3, null, null));
        assertThat(pASEJ.subtotalASEJ()).isEqualByComparingTo("274.50");

        pASEJ = planServiceASEJ.actualizarCabeceraASEJ(planASEJ, new BigDecimal("74.50"), "  Pagará en efectivo  ");
        assertThat(pASEJ.descuentoASEJ()).isEqualByComparingTo("74.50");
        assertThat(pASEJ.totalASEJ()).isEqualByComparingTo("200.00");
        assertThat(pASEJ.observacionesASEJ()).isEqualTo("Pagará en efectivo");

        PlanItemDtoASEJ resinaASEJ = pASEJ.itemsASEJ().stream().filter(iASEJ -> "RES-C".equals(iASEJ.procedimientoCodigoASEJ())).findFirst().orElseThrow();
        pASEJ = planServiceASEJ.actualizarDescuentoItemASEJ(resinaASEJ.idASEJ(), BigDecimal.ZERO);
        assertThat(pASEJ.subtotalASEJ()).isEqualByComparingTo("305.00");
        assertThat(pASEJ.totalASEJ()).isEqualByComparingTo("230.50");

        assertThatThrownBy(() -> planServiceASEJ.actualizarDescuentoItemASEJ(resinaASEJ.idASEJ(), new BigDecimal("180.01")))
                .hasMessageContaining("descuento del ítem");
        assertThatThrownBy(() -> planServiceASEJ.actualizarCabeceraASEJ(planASEJ, new BigDecimal("305.01"), null))
                .hasMessageContaining("descuento global");
        assertThatThrownBy(() -> planServiceASEJ.actualizarCabeceraASEJ(planASEJ, new BigDecimal("0.001"), null))
                .hasMessageContaining("2 decimales");

        PlanItemDtoASEJ consASEJ = pASEJ.itemsASEJ().stream().filter(iASEJ -> "CONS".equals(iASEJ.procedimientoCodigoASEJ())).findFirst().orElseThrow();
        pASEJ = planServiceASEJ.eliminarItemASEJ(consASEJ.idASEJ());
        assertThat(pASEJ.totalASEJ()).isEqualByComparingTo("180.50");
        assertThat(jdbcASEJ.queryForObject("SELECT total_cent FROM plan_tratamiento WHERE id = ?", Long.class, planASEJ)).isEqualTo(18050L);

        planServiceASEJ.actualizarCabeceraASEJ(planASEJ, new BigDecimal("100.00"), null);
        PlanItemDtoASEJ resASEJ = pASEJ.itemsASEJ().stream().filter(iASEJ -> "RES-C".equals(iASEJ.procedimientoCodigoASEJ())).findFirst().orElseThrow();
        assertThatThrownBy(() -> planServiceASEJ.eliminarItemASEJ(resASEJ.idASEJ()))
                .as("sin la resina el subtotal (75) no cubre el descuento global").hasMessageContaining("descuento global");
        assertThat(planServiceASEJ.obtenerASEJ(planASEJ).itemsASEJ()).as("la eliminación se revierte").hasSize(2);
    }

    @Test
    void porSuperficieLaCantidadEsElNumeroDeSuperficiesASEJ() {
        long planASEJ = planServiceASEJ.crearASEJ(pacienteIdASEJ, null, false);
        iniciarSesionASEJ("admin");
        long sellanteASEJ = procedimientoServiceASEJ.guardarASEJ(new ProcedimientoFormASEJ(null, "SUP-" + sufijoASEJ().substring(0, 4).toUpperCase(),
                "Resina por superficie", CategoriaProcedimientoASEJ.OPERATORIA, new BigDecimal("70.00"), FormaCobroASEJ.SUPERFICIE, 30, true));
        iniciarSesionASEJ("odontologo");
        PlanDtoASEJ pASEJ = planServiceASEJ.guardarItemASEJ(new PlanItemFormASEJ(null, planASEJ, sellanteASEJ, 11, Set.of(SuperficieASEJ.V, SuperficieASEJ.O, SuperficieASEJ.M),
                2, 1, null, null));
        PlanItemDtoASEJ iASEJ = pASEJ.itemsASEJ().get(0);
        assertThat(iASEJ.cantidadASEJ()).isEqualTo(3);
        assertThat(iASEJ.superficiesTextoASEJ()).isEqualTo("IMV");
        assertThat(iASEJ.totalASEJ()).isEqualByComparingTo("210.00");
        assertThatThrownBy(() -> planServiceASEJ.guardarItemASEJ(new PlanItemFormASEJ(null, planASEJ, sellanteASEJ, null, Set.of(), 2, 1, null, null)))
                .hasMessageContaining("indique la pieza");
        assertThatThrownBy(() -> planServiceASEJ.guardarItemASEJ(new PlanItemFormASEJ(null, planASEJ, procedimientosASEJ.get("CONS"), 19, null, 1, 1, null, null)))
                .hasMessageContaining("19 no es un número FDI");
    }

    @Test
    void transicionesValidasYCondicionesASEJ() {
        long vacioASEJ = planServiceASEJ.crearASEJ(pacienteIdASEJ, null, false);
        assertThatThrownBy(() -> planServiceASEJ.cambiarEstadoASEJ(vacioASEJ, EstadoPlanASEJ.PROPUESTO))
                .hasMessageContaining("al menos un procedimiento");
        assertThatThrownBy(() -> planServiceASEJ.cambiarEstadoASEJ(vacioASEJ, EstadoPlanASEJ.EN_CURSO))
                .hasMessageContaining("no puede pasar a en curso");
        assertThatThrownBy(() -> planServiceASEJ.cambiarEstadoASEJ(vacioASEJ, EstadoPlanASEJ.ACEPTADO))
                .hasMessageContaining("consentimiento");

        PlanDtoASEJ pASEJ = planSugeridoASEJ();
        pASEJ = planServiceASEJ.cambiarEstadoASEJ(pASEJ.idASEJ(), EstadoPlanASEJ.PROPUESTO);
        assertThat(pASEJ.transicionesASEJ()).containsExactlyInAnyOrder(EstadoPlanASEJ.BORRADOR, EstadoPlanASEJ.ACEPTADO, EstadoPlanASEJ.ANULADO);
        long idASEJ = pASEJ.idASEJ();
        assertThatThrownBy(() -> planServiceASEJ.guardarItemASEJ(new PlanItemFormASEJ(null, idASEJ, procedimientosASEJ.get("CONS"), null, null, 1, 1, null, null)))
                .hasMessageContaining("solo se editan los planes en borrador");
        assertThatThrownBy(() -> planServiceASEJ.aceptarASEJ(idASEJ, "  ")).hasMessageContaining("quien firma");

        pASEJ = planServiceASEJ.aceptarASEJ(idASEJ, "Paciente de prueba");
        assertThat(pASEJ.estadoASEJ()).isEqualTo(EstadoPlanASEJ.ACEPTADO);
        assertThat(pASEJ.aceptadoEnASEJ()).isNotNull();
        assertThat(pASEJ.consentimientoFirmadoPorASEJ()).isEqualTo("Paciente de prueba");
        assertThat(consentimientoServiceASEJ.listarASEJ(pacienteIdASEJ)).anyMatch(cASEJ -> cASEJ.tipoASEJ() == TipoConsentimientoASEJ.TRATAMIENTO);
        BigDecimal totalASEJ = pASEJ.totalASEJ();
        assertThat(pASEJ.cuotasASEJ()).as("sin cronograma: cuota única al contado").singleElement().satisfies(cASEJ -> {
            assertThat(cASEJ.numeroASEJ()).isZero();
            assertThat(cASEJ.montoASEJ()).isEqualByComparingTo(totalASEJ);
            assertThat(cASEJ.vencimientoASEJ()).isEqualTo(LocalDate.now());
        });
        assertThatThrownBy(() -> planServiceASEJ.cambiarEstadoASEJ(idASEJ, EstadoPlanASEJ.FINALIZADO)).hasMessageContaining("no puede pasar");
        assertThatThrownBy(() -> planServiceASEJ.cambiarEstadoASEJ(idASEJ, EstadoPlanASEJ.BORRADOR)).hasMessageContaining("no puede pasar");

        planServiceASEJ.cambiarEstadoASEJ(idASEJ, EstadoPlanASEJ.ANULADO);
        assertThatThrownBy(() -> planServiceASEJ.cambiarEstadoASEJ(idASEJ, EstadoPlanASEJ.EN_CURSO)).hasMessageContaining("anulado no puede pasar");
    }

    @Test
    void volverABorradorBorraElCronogramaASEJ() {
        PlanDtoASEJ pASEJ = planSugeridoASEJ();
        planServiceASEJ.cambiarEstadoASEJ(pASEJ.idASEJ(), EstadoPlanASEJ.PROPUESTO);
        pASEJ = planServiceASEJ.generarCronogramaASEJ(new CronogramaFormASEJ(pASEJ.idASEJ(), new BigDecimal("150"), 2, LocalDate.now().plusDays(10),
                FrecuenciaASEJ.MENSUAL));
        assertThat(pASEJ.cuotasASEJ()).hasSize(3);
        pASEJ = planServiceASEJ.cambiarEstadoASEJ(pASEJ.idASEJ(), EstadoPlanASEJ.BORRADOR);
        assertThat(pASEJ.cuotasASEJ()).isEmpty();
        assertThat(pASEJ.editableASEJ()).isTrue();
    }

    @Test
    void cronogramaConRepartoExactoYBloqueadoConPagosASEJ() {
        long planASEJ = planServiceASEJ.crearASEJ(pacienteIdASEJ, null, false);
        planServiceASEJ.guardarItemASEJ(new PlanItemFormASEJ(null, planASEJ, procedimientosASEJ.get("PROF"), null, null, 4, 1, new BigDecimal("1000.00"), null));
        assertThatThrownBy(() -> planServiceASEJ.generarCronogramaASEJ(new CronogramaFormASEJ(planASEJ, null, 3, LocalDate.now(), FrecuenciaASEJ.MENSUAL)))
                .hasMessageContaining("Proponga el plan");
        planServiceASEJ.cambiarEstadoASEJ(planASEJ, EstadoPlanASEJ.PROPUESTO);

        LocalDate lunesASEJ = LocalDate.now().plusWeeks(1).with(DayOfWeek.MONDAY);
        PlanDtoASEJ pASEJ = planServiceASEJ.generarCronogramaASEJ(new CronogramaFormASEJ(planASEJ, null, 3, lunesASEJ, FrecuenciaASEJ.SEMANAL));
        assertThat(pASEJ.cuotasASEJ()).extracting(CuotaDtoASEJ::numeroASEJ, cASEJ -> cASEJ.montoASEJ().toPlainString(), CuotaDtoASEJ::vencimientoASEJ, CuotaDtoASEJ::estadoASEJ)
                .containsExactly(
                        tuple(1, "333.34", lunesASEJ, EstadoCuotaASEJ.PENDIENTE),
                        tuple(2, "333.33", lunesASEJ.plusWeeks(1), EstadoCuotaASEJ.PENDIENTE),
                        tuple(3, "333.33", lunesASEJ.plusWeeks(2), EstadoCuotaASEJ.PENDIENTE));

        pASEJ = planServiceASEJ.generarCronogramaASEJ(new CronogramaFormASEJ(planASEJ, new BigDecimal("400"), 2, lunesASEJ, FrecuenciaASEJ.QUINCENAL));
        assertThat(pASEJ.cuotasASEJ()).extracting(cASEJ -> cASEJ.montoASEJ().toPlainString()).containsExactly("400.00", "300.00", "300.00");
        assertThat(pASEJ.cuotasASEJ().get(0).vencimientoASEJ()).isEqualTo(LocalDate.now());
        assertThatThrownBy(() -> planServiceASEJ.generarCronogramaASEJ(new CronogramaFormASEJ(planASEJ, new BigDecimal("1000"), 2, lunesASEJ, FrecuenciaASEJ.MENSUAL)))
                .hasMessageContaining("inicial cubre el total");
        assertThatThrownBy(() -> planServiceASEJ.generarCronogramaASEJ(new CronogramaFormASEJ(planASEJ, null, 2, LocalDate.now().minusDays(1), FrecuenciaASEJ.MENSUAL)))
                .hasMessageContaining("anterior a hoy");
        assertThatThrownBy(() -> planServiceASEJ.generarCronogramaASEJ(new CronogramaFormASEJ(planASEJ, null, 2, null, null)))
                .hasMessageContaining("primer vencimiento");

        jdbcASEJ.update("UPDATE cuota SET pagado_cent = 10000 WHERE plan_id = ? AND numero = 0", planASEJ);
        PlanDtoASEJ conPagoASEJ = planServiceASEJ.obtenerASEJ(planASEJ);
        assertThat(conPagoASEJ.tienePagosASEJ()).isTrue();
        assertThat(conPagoASEJ.cuotasASEJ().get(0).estadoASEJ()).isEqualTo(EstadoCuotaASEJ.PARCIAL);
        assertThatThrownBy(() -> planServiceASEJ.generarCronogramaASEJ(new CronogramaFormASEJ(planASEJ, null, 3, lunesASEJ, FrecuenciaASEJ.MENSUAL)))
                .hasMessageContaining("ya tiene pagos");
        assertThatThrownBy(() -> planServiceASEJ.cambiarEstadoASEJ(planASEJ, EstadoPlanASEJ.ANULADO)).hasMessageContaining("ya tiene pagos");
        assertThatThrownBy(() -> planServiceASEJ.cambiarEstadoASEJ(planASEJ, EstadoPlanASEJ.BORRADOR)).hasMessageContaining("ya tiene pagos");
    }

    @Test
    void procedimientosRealizadosEnLaAtencionYFinalizacionASEJ() {
        PlanDtoASEJ pASEJ = aceptadoASEJ();
        List<ItemAtencionDtoASEJ> itemsASEJ = planServiceASEJ.itemsParaAtencionASEJ(atencionIdASEJ);
        assertThat(itemsASEJ).hasSize(6).allMatch(ItemAtencionDtoASEJ::editableASEJ).noneMatch(ItemAtencionDtoASEJ::realizadoAquiASEJ);

        long primeroASEJ = itemsASEJ.get(0).itemIdASEJ();
        itemsASEJ = planServiceASEJ.marcarEnAtencionASEJ(atencionIdASEJ, primeroASEJ, true);
        assertThat(itemsASEJ.get(0).realizadoAquiASEJ()).isTrue();
        assertThat(planServiceASEJ.obtenerASEJ(pASEJ.idASEJ()).estadoASEJ()).as("el primer procedimiento inicia el plan").isEqualTo(EstadoPlanASEJ.EN_CURSO);
        assertThatThrownBy(() -> planServiceASEJ.marcarEnAtencionASEJ(atencionIdASEJ, primeroASEJ, true)).hasMessageContaining("ya figura");

        planServiceASEJ.marcarEnAtencionASEJ(atencionIdASEJ, primeroASEJ, false);
        assertThat(planServiceASEJ.obtenerASEJ(pASEJ.idASEJ()).itemsASEJ()).allMatch(iASEJ -> iASEJ.estadoASEJ() == EstadoPlanItemASEJ.PENDIENTE);

        assertThatThrownBy(() -> planServiceASEJ.cambiarEstadoASEJ(pASEJ.idASEJ(), EstadoPlanASEJ.FINALIZADO)).hasMessageContaining("Quedan 6");
        for (ItemAtencionDtoASEJ iASEJ : planServiceASEJ.itemsParaAtencionASEJ(atencionIdASEJ)) {
            planServiceASEJ.marcarEnAtencionASEJ(atencionIdASEJ, iASEJ.itemIdASEJ(), true);
        }
        PlanDtoASEJ finASEJ = planServiceASEJ.cambiarEstadoASEJ(pASEJ.idASEJ(), EstadoPlanASEJ.FINALIZADO);
        assertThat(finASEJ.realizadosASEJ()).isEqualTo(6);
        assertThat(finASEJ.itemsASEJ()).allMatch(iASEJ -> iASEJ.atencionIdASEJ() == atencionIdASEJ && iASEJ.realizadoEnASEJ() != null);
        assertThat(planServiceASEJ.itemsParaAtencionASEJ(atencionIdASEJ)).as("se siguen viendo los realizados aquí, ya sin edición")
                .hasSize(6).allMatch(ItemAtencionDtoASEJ::realizadoAquiASEJ).noneMatch(ItemAtencionDtoASEJ::editableASEJ);

        atencionServiceASEJ.cerrarASEJ(atencionIdASEJ);
        long otraASEJ = atencionServiceASEJ.abrirASEJ(new AtencionFormASEJ(null, pacienteIdASEJ, null, "Control", null, null, null, null));
        assertThat(planServiceASEJ.itemsParaAtencionASEJ(otraASEJ)).isEmpty();
        assertThatThrownBy(() -> planServiceASEJ.marcarEnAtencionASEJ(atencionIdASEJ, primeroASEJ, false)).hasMessageContaining("atención está cerrada");
    }

    @Test
    void recepcionNoEditaPlanesASEJ() {
        PlanDtoASEJ pASEJ = planSugeridoASEJ();
        iniciarSesionASEJ("recepcion");
        assertThat(planServiceASEJ.obtenerASEJ(pASEJ.idASEJ()).itemsASEJ()).isNotEmpty();
        assertThatThrownBy(() -> planServiceASEJ.cambiarEstadoASEJ(pASEJ.idASEJ(), EstadoPlanASEJ.PROPUESTO)).isInstanceOf(PermisoDenegadoExceptionASEJ.class);
        assertThatThrownBy(() -> planServiceASEJ.crearASEJ(pacienteIdASEJ, 1L, false)).isInstanceOf(PermisoDenegadoExceptionASEJ.class);
    }

    @Test
    void presupuestoYConsentimientoEnPdfASEJ() {
        PlanDtoASEJ pASEJ = planSugeridoASEJ();
        planServiceASEJ.cambiarEstadoASEJ(pASEJ.idASEJ(), EstadoPlanASEJ.PROPUESTO);
        planServiceASEJ.generarCronogramaASEJ(new CronogramaFormASEJ(pASEJ.idASEJ(), new BigDecimal("150"), 4, LocalDate.now().plusDays(7), FrecuenciaASEJ.MENSUAL));
        byte[] presupuestoASEJ = planServiceASEJ.pdfPresupuestoASEJ(pASEJ.idASEJ());
        byte[] consentimientoASEJ = planServiceASEJ.pdfConsentimientoASEJ(pASEJ.idASEJ());
        for (byte[] pdfASEJ : List.of(presupuestoASEJ, consentimientoASEJ)) {
            assertThat(new String(pdfASEJ, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
            assertThat(pdfASEJ.length).isGreaterThan(2000);
        }
        long vacioASEJ = planServiceASEJ.crearASEJ(pacienteIdASEJ, null, false);
        assertThatThrownBy(() -> planServiceASEJ.pdfConsentimientoASEJ(vacioASEJ)).hasMessageContaining("no tiene procedimientos");
        assertThat(planServiceASEJ.pdfPresupuestoASEJ(vacioASEJ)).isNotEmpty();
    }
}
