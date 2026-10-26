package com.odontogest.servicio;

import com.odontogest.documento.ConsentimientoPdfASEJ;
import com.odontogest.documento.PresupuestoPdfASEJ;
import com.odontogest.dominio.comun.ReglaDominioExceptionASEJ;
import com.odontogest.dominio.finanzas.CalculadoraTotalesASEJ;
import com.odontogest.dominio.finanzas.CuotaProgramadaASEJ;
import com.odontogest.dominio.finanzas.EstadoCuotaASEJ;
import com.odontogest.dominio.finanzas.GeneradorCronogramaASEJ;
import com.odontogest.dominio.odontograma.PiezaDentalASEJ;
import com.odontogest.dominio.odontograma.SuperficieASEJ;
import com.odontogest.dominio.tratamiento.FormaCobroASEJ;
import com.odontogest.dominio.tratamiento.ItemSugeridoASEJ;
import com.odontogest.dominio.tratamiento.MotorSugerenciasASEJ;
import com.odontogest.dominio.tratamiento.ProcedimientoDefASEJ;
import com.odontogest.dominio.tratamiento.ReglaSugerenciaDefASEJ;
import com.odontogest.excepcion.NegocioExceptionASEJ;
import com.odontogest.modelo.AtencionASEJ;
import com.odontogest.modelo.ConsentimientoASEJ;
import com.odontogest.modelo.CuotaASEJ;
import com.odontogest.modelo.EstadoAtencionASEJ;
import com.odontogest.modelo.EstadoPlanASEJ;
import com.odontogest.modelo.EstadoPlanItemASEJ;
import com.odontogest.modelo.OdontologoASEJ;
import com.odontogest.modelo.PacienteASEJ;
import com.odontogest.modelo.PlanItemASEJ;
import com.odontogest.modelo.PlanTratamientoASEJ;
import com.odontogest.modelo.ProcedimientoASEJ;
import com.odontogest.modelo.ReglaSugerenciaASEJ;
import com.odontogest.modelo.RolASEJ;
import com.odontogest.modelo.TipoConsentimientoASEJ;
import com.odontogest.persistencia.conversor.DineroConverterASEJ;
import com.odontogest.persistencia.repositorio.AtencionRepositoryASEJ;
import com.odontogest.persistencia.repositorio.ConsentimientoRepositoryASEJ;
import com.odontogest.persistencia.repositorio.CuotaRepositoryASEJ;
import com.odontogest.persistencia.repositorio.OdontologoRepositoryASEJ;
import com.odontogest.persistencia.repositorio.PacienteRepositoryASEJ;
import com.odontogest.persistencia.repositorio.PlanItemRepositoryASEJ;
import com.odontogest.persistencia.repositorio.PlanTratamientoRepositoryASEJ;
import com.odontogest.persistencia.repositorio.ProcedimientoRepositoryASEJ;
import com.odontogest.persistencia.repositorio.ReglaSugerenciaRepositoryASEJ;
import com.odontogest.seguridad.PermisoASEJ;
import com.odontogest.seguridad.SesionUsuarioASEJ;
import com.odontogest.servicio.dto.CronogramaFormASEJ;
import com.odontogest.servicio.dto.CuotaDtoASEJ;
import com.odontogest.servicio.dto.ItemAtencionDtoASEJ;
import com.odontogest.servicio.dto.PacienteDetalleASEJ;
import com.odontogest.servicio.dto.PlanDtoASEJ;
import com.odontogest.servicio.dto.PlanItemDtoASEJ;
import com.odontogest.servicio.dto.PlanItemFormASEJ;
import com.odontogest.servicio.dto.PlanResumenASEJ;
import com.odontogest.servicio.dto.PresupuestoDocumentoASEJ;
import com.odontogest.servicio.dto.UsuarioSesionASEJ;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PlanTratamientoServiceASEJ {

    private static final BigDecimal MONTO_MAXIMOASEJ = new BigDecimal("999999.99");
    private static final int MAXIMO_CUOTASASEJ = 60;

    private final Clock relojASEJ;
    private final PlanTratamientoRepositoryASEJ planRepositoryASEJ;
    private final AuditoriaServiceASEJ auditoriaASEJ;
    private final PlanItemRepositoryASEJ itemRepositoryASEJ;
    private final CuotaRepositoryASEJ cuotaRepositoryASEJ;
    private final ProcedimientoRepositoryASEJ procedimientoRepositoryASEJ;
    private final ReglaSugerenciaRepositoryASEJ reglaRepositoryASEJ;
    private final PacienteRepositoryASEJ pacienteRepositoryASEJ;
    private final OdontologoRepositoryASEJ odontologoRepositoryASEJ;
    private final AtencionRepositoryASEJ atencionRepositoryASEJ;
    private final ConsentimientoRepositoryASEJ consentimientoRepositoryASEJ;
    private final OdontogramaServiceASEJ odontogramaServiceASEJ;
    private final PacienteServiceASEJ pacienteServiceASEJ;
    private final ConfiguracionServiceASEJ configuracionServiceASEJ;
    private final PresupuestoPdfASEJ presupuestoPdfASEJ;
    private final ConsentimientoPdfASEJ consentimientoPdfASEJ;
    private final SesionUsuarioASEJ sesionUsuarioASEJ;

    private final CalculadoraTotalesASEJ calculadoraASEJ = new CalculadoraTotalesASEJ();
    private final GeneradorCronogramaASEJ generadorASEJ = new GeneradorCronogramaASEJ();
    private final MotorSugerenciasASEJ motorASEJ = new MotorSugerenciasASEJ();

    public List<PlanResumenASEJ> listarPorPacienteASEJ(long pacienteIdASEJ) {
        return planRepositoryASEJ.listarPorPacienteASEJ(pacienteIdASEJ).stream()
                .map(pASEJ -> new PlanResumenASEJ(pASEJ.getIdASEJ(), pASEJ.getFechaASEJ(), pASEJ.getEstadoASEJ(), pASEJ.getTotalASEJ(),
                        pASEJ.getOdontologoASEJ().getNombreCompletoASEJ()))
                .toList();
    }

    public PlanDtoASEJ obtenerASEJ(long planIdASEJ) {
        return dtoASEJ(buscarASEJ(planIdASEJ));
    }

    public List<ItemAtencionDtoASEJ> itemsParaAtencionASEJ(long atencionIdASEJ) {
        AtencionASEJ atencionASEJ = atencionRepositoryASEJ.findById(atencionIdASEJ)
                .orElseThrow(() -> new NegocioExceptionASEJ("La atención ya no existe en la base de datos."));
        List<PlanTratamientoASEJ> planesASEJ = planRepositoryASEJ.findByPacienteASEJIdASEJAndEstadoASEJIn(atencionASEJ.getPacienteASEJ().getIdASEJ(),
                EnumSet.of(EstadoPlanASEJ.ACEPTADO, EstadoPlanASEJ.EN_CURSO, EstadoPlanASEJ.FINALIZADO));
        if (planesASEJ.isEmpty()) {
            return List.of();
        }
        boolean abiertaASEJ = atencionASEJ.getEstadoASEJ() == EstadoAtencionASEJ.ABIERTA;
        return itemRepositoryASEJ.listarPorPlanesASEJ(planesASEJ.stream().map(PlanTratamientoASEJ::getIdASEJ).toList()).stream()
                .filter(iASEJ -> iASEJ.getEstadoASEJ() != EstadoPlanItemASEJ.CANCELADO)
                .filter(iASEJ -> atencionIdASEJ == Objects.requireNonNullElse(iASEJ.getAtencionIdASEJ(), -1L)
                        || (iASEJ.getEstadoASEJ() != EstadoPlanItemASEJ.REALIZADO && iASEJ.getPlanASEJ().getEstadoASEJ().enEjecucionASEJ()))
                .map(iASEJ -> {
                    PlanItemDtoASEJ dASEJ = dtoASEJ(iASEJ);
                    boolean aquiASEJ = iASEJ.getEstadoASEJ() == EstadoPlanItemASEJ.REALIZADO && iASEJ.getAtencionIdASEJ() != null
                            && iASEJ.getAtencionIdASEJ() == atencionIdASEJ;
                    return new ItemAtencionDtoASEJ(iASEJ.getIdASEJ(), iASEJ.getPlanASEJ().getIdASEJ(),
                            "Plan N° " + iASEJ.getPlanASEJ().getIdASEJ() + " · " + dASEJ.descripcionASEJ(), PlanItemDtoASEJ.nombreFaseASEJ(iASEJ.getFaseASEJ()),
                            aquiASEJ, abiertaASEJ && iASEJ.getPlanASEJ().getEstadoASEJ().enEjecucionASEJ());
                })
                .toList();
    }

    public byte[] pdfPresupuestoASEJ(long planIdASEJ) {
        return presupuestoPdfASEJ.generarASEJ(documentoASEJ(planIdASEJ));
    }

    public byte[] pdfConsentimientoASEJ(long planIdASEJ) {
        PresupuestoDocumentoASEJ docASEJ = documentoASEJ(planIdASEJ);
        if (docASEJ.planASEJ().itemsASEJ().isEmpty()) {
            throw new NegocioExceptionASEJ("El plan no tiene procedimientos.");
        }
        return consentimientoPdfASEJ.tratamientoASEJ(docASEJ, LocalDate.now(relojASEJ));
    }

    @Transactional
    public long crearASEJ(long pacienteIdASEJ, Long odontologoIdASEJ, boolean sugerirASEJ) {
        UsuarioSesionASEJ usuarioASEJ = sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.PLAN_EDITAR);
        PacienteASEJ pacienteASEJ = pacienteRepositoryASEJ.findById(pacienteIdASEJ)
                .orElseThrow(() -> new NegocioExceptionASEJ("El paciente ya no existe en la base de datos."));
        if (!pacienteASEJ.isActivoASEJ()) {
            throw new NegocioExceptionASEJ("El paciente está inactivo: reactívelo antes de crear un plan.");
        }
        Long elegidoASEJ = RolASEJ.valueOf(usuarioASEJ.rolASEJ()) == RolASEJ.ODONTOLOGO ? usuarioASEJ.odontologoIdASEJ() : odontologoIdASEJ;
        new ValidadorASEJ().requeridoASEJ(elegidoASEJ, "el odontólogo responsable").lanzarSiHayErroresASEJ();
        OdontologoASEJ odontologoASEJ = odontologoRepositoryASEJ.findById(elegidoASEJ)
                .orElseThrow(() -> new NegocioExceptionASEJ("El odontólogo ya no existe en la base de datos."));
        if (!odontologoASEJ.isActivoASEJ()) {
            throw new NegocioExceptionASEJ("El odontólogo " + odontologoASEJ.getNombreCompletoASEJ() + " está inactivo.");
        }
        PlanTratamientoASEJ pASEJ = new PlanTratamientoASEJ();
        pASEJ.setPacienteASEJ(pacienteASEJ);
        pASEJ.setOdontologoASEJ(odontologoASEJ);
        pASEJ.setFechaASEJ(ahoraASEJ());
        pASEJ.setEstadoASEJ(EstadoPlanASEJ.BORRADOR);
        PlanTratamientoASEJ guardadoASEJ = planRepositoryASEJ.save(pASEJ);
        if (sugerirASEJ) {
            agregarSugerenciasASEJ(guardadoASEJ);
        }
        logASEJ.info("Plan {} creado para {}", guardadoASEJ.getIdASEJ(), pacienteASEJ.getNumeroHistoriaASEJ());
        auditoriaASEJ.registrarASEJ("PLAN_CREAR", "plan_tratamiento", guardadoASEJ.getIdASEJ(), null);
        return guardadoASEJ.getIdASEJ();
    }

    @Transactional
    public PlanDtoASEJ sugerirASEJ(long planIdASEJ) {
        sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.PLAN_EDITAR);
        PlanTratamientoASEJ pASEJ = editableASEJ(planIdASEJ);
        int agregadosASEJ = agregarSugerenciasASEJ(pASEJ);
        if (agregadosASEJ == 0) {
            throw new NegocioExceptionASEJ("No hay sugerencias nuevas: los procedimientos sugeridos ya están en este plan "
                    + "o los hallazgos ya tienen tratamiento en otro plan.");
        }
        return dtoASEJ(pASEJ);
    }

    @Transactional
    public PlanDtoASEJ actualizarCabeceraASEJ(long planIdASEJ, BigDecimal descuentoGlobalASEJ, String observacionesASEJ) {
        sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.PLAN_EDITAR);
        PlanTratamientoASEJ pASEJ = buscarASEJ(planIdASEJ);
        String obsASEJ = ValidadorASEJ.limpioMultilineaASEJ(observacionesASEJ);
        new ValidadorASEJ().maximoASEJ(obsASEJ, 1000, "observaciones").lanzarSiHayErroresASEJ();
        BigDecimal descuentoASEJ = descuentoGlobalASEJ == null ? BigDecimal.ZERO : descuentoGlobalASEJ;
        if (descuentoASEJ.compareTo(pASEJ.getDescuentoASEJ()) != 0) {
            exigirEditableASEJ(pASEJ);
            validarMontoASEJ(descuentoASEJ, "El descuento global");
            pASEJ.setDescuentoASEJ(descuentoASEJ);
            recalcularASEJ(pASEJ);
        }
        pASEJ.setObservacionesASEJ(obsASEJ);
        return dtoASEJ(pASEJ);
    }

    @Transactional
    public PlanDtoASEJ cambiarEstadoASEJ(long planIdASEJ, EstadoPlanASEJ destinoASEJ) {
        sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.PLAN_EDITAR);
        PlanTratamientoASEJ pASEJ = buscarASEJ(planIdASEJ);
        if (destinoASEJ == EstadoPlanASEJ.ACEPTADO) {
            throw new NegocioExceptionASEJ("Para aceptar el plan registre el consentimiento de tratamiento.");
        }
        exigirTransicionASEJ(pASEJ, destinoASEJ);
        List<PlanItemASEJ> itemsASEJ = itemRepositoryASEJ.listarPorPlanASEJ(pASEJ.getIdASEJ());
        List<CuotaASEJ> cuotasASEJ = cuotaRepositoryASEJ.findByPlanASEJIdASEJOrderByNumeroASEJAsc(pASEJ.getIdASEJ());
        switch (destinoASEJ) {
            case PROPUESTO -> {
                if (itemsASEJ.stream().noneMatch(iASEJ -> iASEJ.getEstadoASEJ() != EstadoPlanItemASEJ.CANCELADO)) {
                    throw new NegocioExceptionASEJ("Agregue al menos un procedimiento antes de proponer el plan.");
                }
                if (pASEJ.getTotalASEJ().signum() <= 0) {
                    throw new NegocioExceptionASEJ("El total del plan debe ser mayor que cero.");
                }
            }
            case BORRADOR -> {
                exigirSinPagosASEJ(cuotasASEJ, "volver a editar el plan");
                borrarCuotasASEJ(cuotasASEJ);
            }
            case FINALIZADO -> {
                long pendientesASEJ = itemsASEJ.stream()
                        .filter(iASEJ -> iASEJ.getEstadoASEJ() != EstadoPlanItemASEJ.CANCELADO && iASEJ.getEstadoASEJ() != EstadoPlanItemASEJ.REALIZADO)
                        .count();
                if (pendientesASEJ > 0) {
                    throw new NegocioExceptionASEJ("Quedan " + pendientesASEJ + " procedimiento(s) sin realizar: márquelos en la "
                            + "atención antes de finalizar el plan.");
                }
            }
            case ANULADO -> exigirSinPagosASEJ(cuotasASEJ, "anular el plan");
            default -> {
            }
        }
        pASEJ.setEstadoASEJ(destinoASEJ);
        logASEJ.info("Plan {} → {}", pASEJ.getIdASEJ(), destinoASEJ);
        auditoriaASEJ.registrarASEJ("PLAN_ESTADO", "plan_tratamiento", pASEJ.getIdASEJ(), Map.of("estado", destinoASEJ.name()));
        return dtoASEJ(pASEJ);
    }

    @Transactional
    public PlanDtoASEJ aceptarASEJ(long planIdASEJ, String firmadoPorASEJ) {
        long usuarioIdASEJ = sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.PLAN_EDITAR).idASEJ();
        PlanTratamientoASEJ pASEJ = buscarASEJ(planIdASEJ);
        exigirTransicionASEJ(pASEJ, EstadoPlanASEJ.ACEPTADO);
        String firmanteASEJ = ValidadorASEJ.limpioASEJ(firmadoPorASEJ);
        new ValidadorASEJ().requeridoASEJ(firmanteASEJ, "el nombre de quien firma el consentimiento")
                .maximoASEJ(firmanteASEJ, 120, "firmado por").lanzarSiHayErroresASEJ();
        LocalDateTime ahoraASEJ = ahoraASEJ();
        ConsentimientoASEJ cASEJ = new ConsentimientoASEJ();
        cASEJ.setPacienteASEJ(pASEJ.getPacienteASEJ());
        cASEJ.setTipoASEJ(TipoConsentimientoASEJ.TRATAMIENTO);
        cASEJ.setPlanIdASEJ(pASEJ.getIdASEJ());
        cASEJ.setAceptadoEnASEJ(ahoraASEJ);
        cASEJ.setFirmadoPorASEJ(firmanteASEJ);
        cASEJ.setRegistradoPorASEJ(usuarioIdASEJ);
        consentimientoRepositoryASEJ.save(cASEJ);
        if (cuotaRepositoryASEJ.findByPlanASEJIdASEJOrderByNumeroASEJAsc(pASEJ.getIdASEJ()).isEmpty()) {
            long totalASEJ = DineroConverterASEJ.aCentimosASEJ(pASEJ.getTotalASEJ());
            guardarCuotasASEJ(pASEJ, generadorASEJ.generarASEJ(totalASEJ, totalASEJ, 0, null, null, LocalDate.now(relojASEJ)));
        }
        pASEJ.setEstadoASEJ(EstadoPlanASEJ.ACEPTADO);
        pASEJ.setAceptadoEnASEJ(ahoraASEJ);
        logASEJ.info("Plan {} aceptado; consentimiento firmado por {}", pASEJ.getIdASEJ(), firmanteASEJ);
        auditoriaASEJ.registrarASEJ("PLAN_ACEPTAR", "plan_tratamiento", pASEJ.getIdASEJ(), Map.of("firmadoPor", firmanteASEJ));
        return dtoASEJ(pASEJ);
    }

    @Transactional
    public PlanDtoASEJ guardarItemASEJ(PlanItemFormASEJ formASEJ) {
        sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.PLAN_EDITAR);
        PlanTratamientoASEJ pASEJ = editableASEJ(formASEJ.planIdASEJ());
        new ValidadorASEJ()
                .requeridoASEJ(formASEJ.procedimientoIdASEJ(), "el procedimiento")
                .siASEJ(formASEJ.faseASEJ() < 1 || formASEJ.faseASEJ() > 4, "La fase debe estar entre 1 y 4.")
                .siASEJ(formASEJ.piezaFdiASEJ() != null && !PiezaDentalASEJ.esValidaASEJ(formASEJ.piezaFdiASEJ()),
                        "La pieza " + formASEJ.piezaFdiASEJ() + " no es un número FDI válido.")
                .lanzarSiHayErroresASEJ();
        ProcedimientoASEJ procASEJ = procedimientoRepositoryASEJ.findById(formASEJ.procedimientoIdASEJ())
                .orElseThrow(() -> new NegocioExceptionASEJ("El procedimiento ya no existe en la base de datos."));
        PlanItemASEJ itemASEJ;
        if (formASEJ.idASEJ() == null) {
            if (!procASEJ.isActivoASEJ()) {
                throw new NegocioExceptionASEJ("El procedimiento " + procASEJ.getCodigoASEJ() + " está inactivo en el tarifario.");
            }
            itemASEJ = new PlanItemASEJ();
            itemASEJ.setPlanASEJ(pASEJ);
            itemASEJ.setOrdenASEJ(siguienteOrdenASEJ(pASEJ.getIdASEJ()));
        } else {
            itemASEJ = itemDelPlanASEJ(formASEJ.idASEJ(), pASEJ);
        }
        Set<SuperficieASEJ> superficiesASEJ = formASEJ.superficiesASEJ();
        int cantidadASEJ = formASEJ.cantidadASEJ();
        if (procASEJ.getFormaCobroASEJ() == FormaCobroASEJ.SUPERFICIE) {
            new ValidadorASEJ()
                    .siASEJ(formASEJ.piezaFdiASEJ() == null, "El procedimiento se cobra por superficie: indique la pieza.")
                    .siASEJ(superficiesASEJ.isEmpty(), "El procedimiento se cobra por superficie: marque al menos una.")
                    .lanzarSiHayErroresASEJ();
            cantidadASEJ = superficiesASEJ.size();
        } else if (formASEJ.piezaFdiASEJ() == null) {
            superficiesASEJ = Set.of();
        }
        BigDecimal precioASEJ = formASEJ.precioUnitASEJ() == null ? procASEJ.getPrecioASEJ() : formASEJ.precioUnitASEJ();
        BigDecimal descuentoASEJ = formASEJ.descuentoASEJ() == null ? BigDecimal.ZERO : formASEJ.descuentoASEJ();
        validarMontoASEJ(precioASEJ, "El precio");
        validarMontoASEJ(descuentoASEJ, "El descuento");
        if (cantidadASEJ < 1 || cantidadASEJ > 99) {
            throw new NegocioExceptionASEJ("La cantidad debe estar entre 1 y 99.");
        }
        BigDecimal totalASEJ = DineroConverterASEJ.desdeCentimosASEJ(totalItemASEJ(cantidadASEJ, precioASEJ, descuentoASEJ));

        itemASEJ.setProcedimientoASEJ(procASEJ);
        itemASEJ.setPiezaFdiASEJ(formASEJ.piezaFdiASEJ());
        itemASEJ.setSuperficiesASEJ(superficiesASEJ.isEmpty() ? EnumSet.noneOf(SuperficieASEJ.class) : EnumSet.copyOf(superficiesASEJ));
        itemASEJ.setFaseASEJ(formASEJ.faseASEJ());
        itemASEJ.setCantidadASEJ(cantidadASEJ);
        itemASEJ.setPrecioUnitASEJ(precioASEJ);
        itemASEJ.setDescuentoASEJ(descuentoASEJ);
        itemASEJ.setTotalASEJ(totalASEJ);
        itemRepositoryASEJ.save(itemASEJ);
        recalcularASEJ(pASEJ);
        return dtoASEJ(pASEJ);
    }

    @Transactional
    public PlanDtoASEJ actualizarDescuentoItemASEJ(long itemIdASEJ, BigDecimal descuentoASEJ) {
        sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.PLAN_EDITAR);
        PlanItemASEJ itemASEJ = itemRepositoryASEJ.findById(itemIdASEJ)
                .orElseThrow(() -> new NegocioExceptionASEJ("El ítem ya no existe en la base de datos."));
        PlanTratamientoASEJ pASEJ = itemASEJ.getPlanASEJ();
        exigirEditableASEJ(pASEJ);
        BigDecimal dASEJ = descuentoASEJ == null ? BigDecimal.ZERO : descuentoASEJ;
        validarMontoASEJ(dASEJ, "El descuento");
        itemASEJ.setDescuentoASEJ(dASEJ);
        itemASEJ.setTotalASEJ(DineroConverterASEJ.desdeCentimosASEJ(totalItemASEJ(itemASEJ.getCantidadASEJ(), itemASEJ.getPrecioUnitASEJ(), dASEJ)));
        recalcularASEJ(pASEJ);
        return dtoASEJ(pASEJ);
    }

    @Transactional
    public PlanDtoASEJ eliminarItemASEJ(long itemIdASEJ) {
        sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.PLAN_EDITAR);
        PlanItemASEJ itemASEJ = itemRepositoryASEJ.findById(itemIdASEJ)
                .orElseThrow(() -> new NegocioExceptionASEJ("El ítem ya no existe en la base de datos."));
        PlanTratamientoASEJ pASEJ = itemASEJ.getPlanASEJ();
        exigirEditableASEJ(pASEJ);
        itemRepositoryASEJ.delete(itemASEJ);
        itemRepositoryASEJ.flush();
        recalcularASEJ(pASEJ);
        return dtoASEJ(pASEJ);
    }

    @Transactional
    public PlanDtoASEJ generarCronogramaASEJ(CronogramaFormASEJ formASEJ) {
        sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.PLAN_EDITAR);
        PlanTratamientoASEJ pASEJ = buscarASEJ(formASEJ.planIdASEJ());
        if (!pASEJ.getEstadoASEJ().admiteCronogramaASEJ()) {
            throw new NegocioExceptionASEJ(pASEJ.getEstadoASEJ() == EstadoPlanASEJ.BORRADOR
                    ? "Proponga el plan antes de generar el cronograma: así el total ya no cambia."
                    : "El plan está " + pASEJ.getEstadoASEJ().etiquetaASEJ().toLowerCase() + ": no admite cambios de cronograma.");
        }
        List<CuotaASEJ> actualesASEJ = cuotaRepositoryASEJ.findByPlanASEJIdASEJOrderByNumeroASEJAsc(pASEJ.getIdASEJ());
        exigirSinPagosASEJ(actualesASEJ, "regenerar el cronograma");
        BigDecimal inicialASEJ = formASEJ.inicialASEJ() == null ? BigDecimal.ZERO : formASEJ.inicialASEJ();
        validarMontoASEJ(inicialASEJ, "La cuota inicial");
        if (formASEJ.numeroCuotasASEJ() > MAXIMO_CUOTASASEJ) {
            throw new NegocioExceptionASEJ("El cronograma admite como máximo " + MAXIMO_CUOTASASEJ + " cuotas.");
        }
        List<CuotaProgramadaASEJ> programadasASEJ;
        try {
            programadasASEJ = generadorASEJ.generarASEJ(DineroConverterASEJ.aCentimosASEJ(pASEJ.getTotalASEJ()), DineroConverterASEJ.aCentimosASEJ(inicialASEJ),
                    formASEJ.numeroCuotasASEJ(), formASEJ.primerVencimientoASEJ(), formASEJ.frecuenciaASEJ(), LocalDate.now(relojASEJ));
        } catch (ReglaDominioExceptionASEJ eASEJ) {
            throw new NegocioExceptionASEJ(eASEJ.getMessage());
        } catch (NullPointerException eASEJ) {
            throw new NegocioExceptionASEJ("Indique la fecha del primer vencimiento y la frecuencia.");
        }
        borrarCuotasASEJ(actualesASEJ);
        guardarCuotasASEJ(pASEJ, programadasASEJ);
        return dtoASEJ(pASEJ);
    }

    @Transactional
    public List<ItemAtencionDtoASEJ> marcarEnAtencionASEJ(long atencionIdASEJ, long itemIdASEJ, boolean realizadoASEJ) {
        sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.ATENCION_GESTIONAR);
        AtencionASEJ atencionASEJ = atencionRepositoryASEJ.findById(atencionIdASEJ)
                .orElseThrow(() -> new NegocioExceptionASEJ("La atención ya no existe en la base de datos."));
        if (atencionASEJ.getEstadoASEJ() != EstadoAtencionASEJ.ABIERTA) {
            throw new NegocioExceptionASEJ("La atención está cerrada: no se pueden marcar procedimientos.");
        }
        PlanItemASEJ itemASEJ = itemRepositoryASEJ.findById(itemIdASEJ)
                .orElseThrow(() -> new NegocioExceptionASEJ("El ítem ya no existe en la base de datos."));
        PlanTratamientoASEJ pASEJ = itemASEJ.getPlanASEJ();
        if (!pASEJ.getPacienteASEJ().getIdASEJ().equals(atencionASEJ.getPacienteASEJ().getIdASEJ())) {
            throw new NegocioExceptionASEJ("El procedimiento pertenece al plan de otro paciente.");
        }
        if (!pASEJ.getEstadoASEJ().enEjecucionASEJ()) {
            throw new NegocioExceptionASEJ("El plan está " + pASEJ.getEstadoASEJ().etiquetaASEJ().toLowerCase()
                    + ": solo se marcan procedimientos de planes aceptados o en curso.");
        }
        if (realizadoASEJ) {
            if (itemASEJ.getEstadoASEJ() == EstadoPlanItemASEJ.REALIZADO) {
                throw new NegocioExceptionASEJ("El procedimiento ya figura como realizado.");
            }
            itemASEJ.setEstadoASEJ(EstadoPlanItemASEJ.REALIZADO);
            itemASEJ.setAtencionIdASEJ(atencionIdASEJ);
            itemASEJ.setRealizadoEnASEJ(ahoraASEJ());
            if (pASEJ.getEstadoASEJ() == EstadoPlanASEJ.ACEPTADO) {
                pASEJ.setEstadoASEJ(EstadoPlanASEJ.EN_CURSO);
            }
        } else {
            if (itemASEJ.getEstadoASEJ() != EstadoPlanItemASEJ.REALIZADO || !Objects.equals(itemASEJ.getAtencionIdASEJ(), atencionIdASEJ)) {
                throw new NegocioExceptionASEJ("Solo se puede desmarcar un procedimiento realizado en esta misma atención.");
            }
            itemASEJ.setEstadoASEJ(EstadoPlanItemASEJ.PENDIENTE);
            itemASEJ.setAtencionIdASEJ(null);
            itemASEJ.setRealizadoEnASEJ(null);
        }
        itemRepositoryASEJ.flush();
        return itemsParaAtencionASEJ(atencionIdASEJ);
    }

    private int agregarSugerenciasASEJ(PlanTratamientoASEJ pASEJ) {
        OdontogramaServiceASEJ.EstadoFirmadoASEJ firmadoASEJ = odontogramaServiceASEJ.estadoFirmadoASEJ(pASEJ.getPacienteASEJ().getIdASEJ())
                .orElseThrow(() -> new NegocioExceptionASEJ("El paciente no tiene un odontograma firmado: "
                        + "cierre y firme el odontograma antes de pedir sugerencias."));
        List<ReglaSugerenciaASEJ> reglasASEJ = reglaRepositoryASEJ.listarConRelacionesASEJ().stream()
                .filter(rASEJ -> rASEJ.isActivoASEJ() && rASEJ.getProcedimientoASEJ().isActivoASEJ())
                .toList();
        Map<Long, ProcedimientoDefASEJ> tarifarioASEJ = reglasASEJ.stream().map(ReglaSugerenciaASEJ::getProcedimientoASEJ).distinct()
                .collect(Collectors.toMap(ProcedimientoASEJ::getIdASEJ, PlanTratamientoServiceASEJ::definicionASEJ, (aASEJ, bASEJ) -> aASEJ));
        List<ItemSugeridoASEJ> sugeridosASEJ = motorASEJ.sugerirASEJ(firmadoASEJ.estadoASEJ(),
                reglasASEJ.stream().map(PlanTratamientoServiceASEJ::definicionASEJ).toList(), tarifarioASEJ);

        List<PlanItemASEJ> existentesASEJ = itemRepositoryASEJ.listarPorPlanASEJ(pASEJ.getIdASEJ());
        Set<String> enPlanASEJ = new HashSet<>();
        for (PlanItemASEJ iASEJ : existentesASEJ) {
            if (iASEJ.getEstadoASEJ() != EstadoPlanItemASEJ.CANCELADO) {
                enPlanASEJ.add(iASEJ.getProcedimientoASEJ().getIdASEJ() + "@" + iASEJ.getPiezaFdiASEJ());
            }
        }
        Map<Long, ProcedimientoASEJ> procedimientosASEJ = reglasASEJ.stream().map(ReglaSugerenciaASEJ::getProcedimientoASEJ).distinct()
                .collect(Collectors.toMap(ProcedimientoASEJ::getIdASEJ, Function.identity(), (aASEJ, bASEJ) -> aASEJ));
        int ordenASEJ = siguienteOrdenASEJ(pASEJ.getIdASEJ());
        Set<Long> tratadosEnOtrosPlanesASEJ = new HashSet<>(
                itemRepositoryASEJ.hallazgosEnOtrosPlanesASEJ(pASEJ.getPacienteASEJ().getIdASEJ(), pASEJ.getIdASEJ()));
        int agregadosASEJ = 0;
        for (ItemSugeridoASEJ sASEJ : sugeridosASEJ) {
            if (sASEJ.origenHallazgoIdASEJ() != null && tratadosEnOtrosPlanesASEJ.contains(sASEJ.origenHallazgoIdASEJ())) {
                continue;
            }
            if (!enPlanASEJ.add(sASEJ.procedimientoIdASEJ() + "@" + sASEJ.piezaFdiASEJ())) {
                continue;
            }
            PlanItemASEJ itemASEJ = new PlanItemASEJ();
            itemASEJ.setPlanASEJ(pASEJ);
            itemASEJ.setProcedimientoASEJ(procedimientosASEJ.get(sASEJ.procedimientoIdASEJ()));
            itemASEJ.setHallazgoIdASEJ(sASEJ.origenHallazgoIdASEJ());
            itemASEJ.setPiezaFdiASEJ(sASEJ.piezaFdiASEJ());
            itemASEJ.setSuperficiesASEJ(sASEJ.superficiesASEJ().isEmpty() ? EnumSet.noneOf(SuperficieASEJ.class) : EnumSet.copyOf(sASEJ.superficiesASEJ()));
            itemASEJ.setFaseASEJ(sASEJ.faseASEJ());
            itemASEJ.setCantidadASEJ(sASEJ.cantidadASEJ());
            itemASEJ.setPrecioUnitASEJ(DineroConverterASEJ.desdeCentimosASEJ(sASEJ.precioUnitCentASEJ()));
            itemASEJ.setDescuentoASEJ(BigDecimal.ZERO);
            itemASEJ.setTotalASEJ(DineroConverterASEJ.desdeCentimosASEJ(sASEJ.totalCentASEJ()));
            itemASEJ.setOrdenASEJ(ordenASEJ++);
            itemRepositoryASEJ.save(itemASEJ);
            agregadosASEJ++;
        }
        pASEJ.setOdontogramaIdASEJ(firmadoASEJ.odontogramaIdASEJ());
        itemRepositoryASEJ.flush();
        recalcularASEJ(pASEJ);
        logASEJ.info("Plan {}: {} procedimiento(s) sugerido(s) desde el odontograma {}", pASEJ.getIdASEJ(), agregadosASEJ,
                firmadoASEJ.odontogramaIdASEJ());
        return agregadosASEJ;
    }

    private void recalcularASEJ(PlanTratamientoASEJ pASEJ) {
        List<CalculadoraTotalesASEJ.LineaASEJ> lineasASEJ = itemRepositoryASEJ.listarPorPlanASEJ(pASEJ.getIdASEJ()).stream()
                .map(iASEJ -> new CalculadoraTotalesASEJ.LineaASEJ(iASEJ.getCantidadASEJ(), DineroConverterASEJ.aCentimosASEJ(iASEJ.getPrecioUnitASEJ()),
                        DineroConverterASEJ.aCentimosASEJ(iASEJ.getDescuentoASEJ()), iASEJ.getEstadoASEJ() == EstadoPlanItemASEJ.CANCELADO))
                .toList();
        CalculadoraTotalesASEJ.TotalesASEJ tASEJ;
        try {
            tASEJ = calculadoraASEJ.calcularASEJ(lineasASEJ, DineroConverterASEJ.aCentimosASEJ(pASEJ.getDescuentoASEJ()));
        } catch (ReglaDominioExceptionASEJ eASEJ) {
            throw new NegocioExceptionASEJ(eASEJ.getMessage());
        }
        pASEJ.setSubtotalASEJ(DineroConverterASEJ.desdeCentimosASEJ(tASEJ.subtotalCentASEJ()));
        pASEJ.setTotalASEJ(DineroConverterASEJ.desdeCentimosASEJ(tASEJ.totalCentASEJ()));
    }

    private long totalItemASEJ(int cantidadASEJ, BigDecimal precioASEJ, BigDecimal descuentoASEJ) {
        try {
            return calculadoraASEJ.totalItemASEJ(cantidadASEJ, DineroConverterASEJ.aCentimosASEJ(precioASEJ), DineroConverterASEJ.aCentimosASEJ(descuentoASEJ));
        } catch (ReglaDominioExceptionASEJ eASEJ) {
            throw new NegocioExceptionASEJ(eASEJ.getMessage());
        }
    }

    private void guardarCuotasASEJ(PlanTratamientoASEJ pASEJ, List<CuotaProgramadaASEJ> programadasASEJ) {
        for (CuotaProgramadaASEJ cASEJ : programadasASEJ) {
            CuotaASEJ cuotaASEJ = new CuotaASEJ();
            cuotaASEJ.setPlanASEJ(pASEJ);
            cuotaASEJ.setNumeroASEJ(cASEJ.numeroASEJ());
            cuotaASEJ.setVencimientoASEJ(cASEJ.vencimientoASEJ());
            cuotaASEJ.setMontoASEJ(DineroConverterASEJ.desdeCentimosASEJ(cASEJ.montoCentASEJ()));
            cuotaASEJ.setPagadoASEJ(BigDecimal.ZERO);
            cuotaRepositoryASEJ.save(cuotaASEJ);
        }
        cuotaRepositoryASEJ.flush();
    }

    private void borrarCuotasASEJ(List<CuotaASEJ> cuotasASEJ) {
        if (!cuotasASEJ.isEmpty()) {
            cuotaRepositoryASEJ.deleteAll(cuotasASEJ);
            cuotaRepositoryASEJ.flush();
        }
    }

    private static void exigirSinPagosASEJ(List<CuotaASEJ> cuotasASEJ, String accionASEJ) {
        if (cuotasASEJ.stream().anyMatch(cASEJ -> cASEJ.getPagadoASEJ().signum() > 0)) {
            throw new NegocioExceptionASEJ("El plan ya tiene pagos registrados: no se puede " + accionASEJ + ".");
        }
    }

    private static void exigirTransicionASEJ(PlanTratamientoASEJ pASEJ, EstadoPlanASEJ destinoASEJ) {
        if (!pASEJ.getEstadoASEJ().puedePasarAASEJ(destinoASEJ)) {
            throw new NegocioExceptionASEJ("Un plan " + pASEJ.getEstadoASEJ().etiquetaASEJ().toLowerCase() + " no puede pasar a "
                    + destinoASEJ.etiquetaASEJ().toLowerCase() + ".");
        }
    }

    private static void exigirEditableASEJ(PlanTratamientoASEJ pASEJ) {
        if (!pASEJ.getEstadoASEJ().editableASEJ()) {
            throw new NegocioExceptionASEJ("El plan está " + pASEJ.getEstadoASEJ().etiquetaASEJ().toLowerCase()
                    + ": solo se editan los planes en borrador.");
        }
    }

    private static void validarMontoASEJ(BigDecimal montoASEJ, String campoASEJ) {
        if (montoASEJ.signum() < 0 || montoASEJ.compareTo(MONTO_MAXIMOASEJ) > 0) {
            throw new NegocioExceptionASEJ(campoASEJ + " debe estar entre 0 y 999 999.99.");
        }
        if (montoASEJ.stripTrailingZeros().scale() > 2) {
            throw new NegocioExceptionASEJ(campoASEJ + " admite como máximo 2 decimales.");
        }
    }

    private PlanTratamientoASEJ buscarASEJ(long idASEJ) {
        return planRepositoryASEJ.findById(idASEJ)
                .orElseThrow(() -> new NegocioExceptionASEJ("El plan ya no existe en la base de datos."));
    }

    private PlanTratamientoASEJ editableASEJ(long idASEJ) {
        PlanTratamientoASEJ pASEJ = buscarASEJ(idASEJ);
        exigirEditableASEJ(pASEJ);
        return pASEJ;
    }

    private PlanItemASEJ itemDelPlanASEJ(long itemIdASEJ, PlanTratamientoASEJ pASEJ) {
        PlanItemASEJ itemASEJ = itemRepositoryASEJ.findById(itemIdASEJ)
                .orElseThrow(() -> new NegocioExceptionASEJ("El ítem ya no existe en la base de datos."));
        if (!itemASEJ.getPlanASEJ().getIdASEJ().equals(pASEJ.getIdASEJ())) {
            throw new NegocioExceptionASEJ("El ítem no pertenece a este plan.");
        }
        return itemASEJ;
    }

    private int siguienteOrdenASEJ(long planIdASEJ) {
        return itemRepositoryASEJ.listarPorPlanASEJ(planIdASEJ).stream().mapToInt(PlanItemASEJ::getOrdenASEJ).max().orElse(0) + 1;
    }

    private PresupuestoDocumentoASEJ documentoASEJ(long planIdASEJ) {
        PlanDtoASEJ planASEJ = obtenerASEJ(planIdASEJ);
        PacienteDetalleASEJ pacienteASEJ = pacienteServiceASEJ.obtenerASEJ(planASEJ.pacienteIdASEJ());
        return new PresupuestoDocumentoASEJ(configuracionServiceASEJ.datosClinicaASEJ(), pacienteASEJ, planASEJ);
    }

    private PlanDtoASEJ dtoASEJ(PlanTratamientoASEJ pASEJ) {
        List<PlanItemDtoASEJ> itemsASEJ = new ArrayList<>(itemRepositoryASEJ.listarPorPlanASEJ(pASEJ.getIdASEJ()).stream()
                .map(PlanTratamientoServiceASEJ::dtoASEJ).toList());
        itemsASEJ.sort(PlanTratamientoServiceASEJ.ORDEN_ITEMSASEJ);
        LocalDate hoyASEJ = LocalDate.now(relojASEJ);
        List<CuotaDtoASEJ> cuotasASEJ = cuotaRepositoryASEJ.findByPlanASEJIdASEJOrderByNumeroASEJAsc(pASEJ.getIdASEJ()).stream()
                .map(cASEJ -> new CuotaDtoASEJ(cASEJ.getIdASEJ(), cASEJ.getNumeroASEJ(), cASEJ.getVencimientoASEJ(), cASEJ.getMontoASEJ(), cASEJ.getPagadoASEJ(),
                        EstadoCuotaASEJ.deASEJ(DineroConverterASEJ.aCentimosASEJ(cASEJ.getMontoASEJ()), DineroConverterASEJ.aCentimosASEJ(cASEJ.getPagadoASEJ()),
                                cASEJ.getVencimientoASEJ(), hoyASEJ)))
                .toList();
        var consentimientoASEJ = consentimientoRepositoryASEJ.findFirstByPlanIdASEJAndTipoASEJOrderByAceptadoEnASEJDesc(pASEJ.getIdASEJ(),
                TipoConsentimientoASEJ.TRATAMIENTO);
        OdontologoASEJ oASEJ = pASEJ.getOdontologoASEJ();
        return new PlanDtoASEJ(pASEJ.getIdASEJ(), pASEJ.getPacienteASEJ().getIdASEJ(), oASEJ.getIdASEJ(), oASEJ.getNombreCompletoASEJ(), oASEJ.getCopASEJ(),
                pASEJ.getOdontogramaIdASEJ(), pASEJ.getFechaASEJ(), pASEJ.getEstadoASEJ(), pASEJ.getSubtotalASEJ(), pASEJ.getDescuentoASEJ(), pASEJ.getTotalASEJ(),
                pASEJ.getObservacionesASEJ(), pASEJ.getAceptadoEnASEJ(), itemsASEJ, cuotasASEJ,
                consentimientoASEJ.map(ConsentimientoASEJ::getFirmadoPorASEJ).orElse(null),
                consentimientoASEJ.map(ConsentimientoASEJ::getAceptadoEnASEJ).orElse(null), pASEJ.getEstadoASEJ().siguientesASEJ());
    }

    private static final Comparator<PlanItemDtoASEJ> ORDEN_ITEMSASEJ = Comparator
            .comparingInt(PlanItemDtoASEJ::faseASEJ)
            .thenComparingInt(iASEJ -> iASEJ.piezaASEJ() == null ? -1 : PiezaDentalASEJ.deASEJ(iASEJ.piezaASEJ()).ordenDibujoASEJ())
            .thenComparingLong(PlanItemDtoASEJ::idASEJ);

    private static PlanItemDtoASEJ dtoASEJ(PlanItemASEJ iASEJ) {
        ProcedimientoASEJ procASEJ = iASEJ.getProcedimientoASEJ();
        return new PlanItemDtoASEJ(iASEJ.getIdASEJ(), iASEJ.getPlanASEJ().getIdASEJ(), procASEJ.getIdASEJ(), procASEJ.getCodigoASEJ(), procASEJ.getNombreASEJ(),
                procASEJ.getFormaCobroASEJ(), iASEJ.getHallazgoIdASEJ(), iASEJ.getPiezaFdiASEJ(), iASEJ.getSuperficiesASEJ(), iASEJ.getFaseASEJ(), iASEJ.getCantidadASEJ(),
                iASEJ.getPrecioUnitASEJ(), iASEJ.getDescuentoASEJ(), iASEJ.getTotalASEJ(), iASEJ.getEstadoASEJ(), iASEJ.getAtencionIdASEJ(), iASEJ.getRealizadoEnASEJ());
    }

    private static ProcedimientoDefASEJ definicionASEJ(ProcedimientoASEJ pASEJ) {
        return new ProcedimientoDefASEJ(pASEJ.getIdASEJ(), pASEJ.getCodigoASEJ(), pASEJ.getNombreASEJ(), DineroConverterASEJ.aCentimosASEJ(pASEJ.getPrecioASEJ()),
                pASEJ.getFormaCobroASEJ());
    }

    private static ReglaSugerenciaDefASEJ definicionASEJ(ReglaSugerenciaASEJ rASEJ) {
        return new ReglaSugerenciaDefASEJ(rASEJ.getIdASEJ(), rASEJ.getHallazgoTipoASEJ() == null ? null : rASEJ.getHallazgoTipoASEJ().getIdASEJ(),
                rASEJ.getEstadoASEJ(), rASEJ.getTipoRadicularASEJ(), rASEJ.getMinSuperficiesASEJ(), rASEJ.getMaxSuperficiesASEJ(),
                rASEJ.getProcedimientoASEJ().getIdASEJ(), rASEJ.getFaseASEJ(), rASEJ.getPrioridadASEJ());
    }

    private LocalDateTime ahoraASEJ() {
        return LocalDateTime.now(relojASEJ).truncatedTo(ChronoUnit.SECONDS);
    }
}
