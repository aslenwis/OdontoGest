/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio;

import com.odontogest.documento.ConsentimientoPdf;
import com.odontogest.documento.PresupuestoPdf;
import com.odontogest.dominio.comun.ReglaDominioException;
import com.odontogest.dominio.finanzas.CalculadoraTotales;
import com.odontogest.dominio.finanzas.CuotaProgramada;
import com.odontogest.dominio.finanzas.EstadoCuota;
import com.odontogest.dominio.finanzas.GeneradorCronograma;
import com.odontogest.dominio.odontograma.PiezaDental;
import com.odontogest.dominio.odontograma.Superficie;
import com.odontogest.dominio.tratamiento.FormaCobro;
import com.odontogest.dominio.tratamiento.ItemSugerido;
import com.odontogest.dominio.tratamiento.MotorSugerencias;
import com.odontogest.dominio.tratamiento.ProcedimientoDef;
import com.odontogest.dominio.tratamiento.ReglaSugerenciaDef;
import com.odontogest.excepcion.NegocioException;
import com.odontogest.modelo.Atencion;
import com.odontogest.modelo.Consentimiento;
import com.odontogest.modelo.Cuota;
import com.odontogest.modelo.EstadoAtencion;
import com.odontogest.modelo.EstadoPlan;
import com.odontogest.modelo.EstadoPlanItem;
import com.odontogest.modelo.Odontologo;
import com.odontogest.modelo.Paciente;
import com.odontogest.modelo.PlanItem;
import com.odontogest.modelo.PlanTratamiento;
import com.odontogest.modelo.Procedimiento;
import com.odontogest.modelo.ReglaSugerencia;
import com.odontogest.modelo.Rol;
import com.odontogest.modelo.TipoConsentimiento;
import com.odontogest.persistencia.conversor.DineroConverter;
import com.odontogest.persistencia.repositorio.AtencionRepository;
import com.odontogest.persistencia.repositorio.ConsentimientoRepository;
import com.odontogest.persistencia.repositorio.CuotaRepository;
import com.odontogest.persistencia.repositorio.OdontologoRepository;
import com.odontogest.persistencia.repositorio.PacienteRepository;
import com.odontogest.persistencia.repositorio.PlanItemRepository;
import com.odontogest.persistencia.repositorio.PlanTratamientoRepository;
import com.odontogest.persistencia.repositorio.ProcedimientoRepository;
import com.odontogest.persistencia.repositorio.ReglaSugerenciaRepository;
import com.odontogest.seguridad.Permiso;
import com.odontogest.seguridad.SesionUsuario;
import com.odontogest.servicio.dto.CronogramaForm;
import com.odontogest.servicio.dto.CuotaDto;
import com.odontogest.servicio.dto.ItemAtencionDto;
import com.odontogest.servicio.dto.PacienteDetalle;
import com.odontogest.servicio.dto.PlanDto;
import com.odontogest.servicio.dto.PlanItemDto;
import com.odontogest.servicio.dto.PlanItemForm;
import com.odontogest.servicio.dto.PlanResumen;
import com.odontogest.servicio.dto.PresupuestoDocumento;
import com.odontogest.servicio.dto.UsuarioSesion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

/**
 * Plan de tratamiento y presupuesto (RF-PLN-01 a RF-PLN-06) y procedimientos realizados en la atención (RF-ATE-02).
 * <ul>
 *   <li>Ítems, precios y descuentos solo se editan en BORRADOR; los totales se recalculan con
 *       {@link CalculadoraTotales} en cada cambio.</li>
 *   <li>Las sugerencias salen del último odontograma <b>firmado</b> ({@link MotorSugerencias}); no se repite un
 *       procedimiento que ya está en el plan para la misma pieza, ni se propone un hallazgo que ya tiene tratamiento
 *       en otro plan no anulado (aunque siga en el odontograma porque no se registró la evolución).</li>
 *   <li>Transiciones: ver {@link EstadoPlan}. Aceptar registra el consentimiento informado de tratamiento; si el plan
 *       no tiene cronograma se crea una cuota única (pago al contado) para que la caja pueda cobrarlo.</li>
 *   <li>El cronograma ({@link GeneradorCronograma}) se puede regenerar mientras ninguna cuota tenga pagos.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PlanTratamientoService {

    private static final BigDecimal MONTO_MAXIMO = new BigDecimal("999999.99");
    private static final int MAXIMO_CUOTAS = 60;

    private final PlanTratamientoRepository planRepository;
    private final PlanItemRepository itemRepository;
    private final CuotaRepository cuotaRepository;
    private final ProcedimientoRepository procedimientoRepository;
    private final ReglaSugerenciaRepository reglaRepository;
    private final PacienteRepository pacienteRepository;
    private final OdontologoRepository odontologoRepository;
    private final AtencionRepository atencionRepository;
    private final ConsentimientoRepository consentimientoRepository;
    private final OdontogramaService odontogramaService;
    private final PacienteService pacienteService;
    private final ConfiguracionService configuracionService;
    private final PresupuestoPdf presupuestoPdf;
    private final ConsentimientoPdf consentimientoPdf;
    private final SesionUsuario sesionUsuario;

    private final CalculadoraTotales calculadora = new CalculadoraTotales();
    private final GeneradorCronograma generador = new GeneradorCronograma();
    private final MotorSugerencias motor = new MotorSugerencias();

    // ------------------------------------------------------------------ consultas

    public List<PlanResumen> listarPorPaciente(long pacienteId) {
        return planRepository.listarPorPaciente(pacienteId).stream()
                .map(p -> new PlanResumen(p.getId(), p.getFecha(), p.getEstado(), p.getTotal(),
                        p.getOdontologo().getNombreCompleto()))
                .toList();
    }

    public PlanDto obtener(long planId) {
        return dto(buscar(planId));
    }

    /**
     * Procedimientos que se pueden marcar en una atención: los pendientes de los planes aceptados o en curso del
     * paciente y los que ya se marcaron en esta atención.
     */
    public List<ItemAtencionDto> itemsParaAtencion(long atencionId) {
        Atencion atencion = atencionRepository.findById(atencionId)
                .orElseThrow(() -> new NegocioException("La atención ya no existe en la base de datos."));
        List<PlanTratamiento> planes = planRepository.findByPacienteIdAndEstadoIn(atencion.getPaciente().getId(),
                EnumSet.of(EstadoPlan.ACEPTADO, EstadoPlan.EN_CURSO, EstadoPlan.FINALIZADO));
        if (planes.isEmpty()) {
            return List.of();
        }
        boolean abierta = atencion.getEstado() == EstadoAtencion.ABIERTA;
        return itemRepository.listarPorPlanes(planes.stream().map(PlanTratamiento::getId).toList()).stream()
                .filter(i -> i.getEstado() != EstadoPlanItem.CANCELADO)
                .filter(i -> atencionId == Objects.requireNonNullElse(i.getAtencionId(), -1L)
                        || (i.getEstado() != EstadoPlanItem.REALIZADO && i.getPlan().getEstado().enEjecucion()))
                .map(i -> {
                    PlanItemDto d = dto(i);
                    boolean aqui = i.getEstado() == EstadoPlanItem.REALIZADO && i.getAtencionId() != null
                            && i.getAtencionId() == atencionId;
                    return new ItemAtencionDto(i.getId(), i.getPlan().getId(),
                            "Plan N° " + i.getPlan().getId() + " · " + d.descripcion(), PlanItemDto.nombreFase(i.getFase()),
                            aqui, abierta && i.getPlan().getEstado().enEjecucion());
                })
                .toList();
    }

    public byte[] pdfPresupuesto(long planId) {
        return presupuestoPdf.generar(documento(planId));
    }

    /** Consentimiento informado de tratamiento para imprimir y firmar (RF-PAC-05). */
    public byte[] pdfConsentimiento(long planId) {
        PresupuestoDocumento doc = documento(planId);
        if (doc.plan().items().isEmpty()) {
            throw new NegocioException("El plan no tiene procedimientos.");
        }
        return consentimientoPdf.tratamiento(doc, LocalDate.now());
    }

    // ------------------------------------------------------------------ plan

    /**
     * Crea un plan en borrador.
     *
     * @param odontologoId se ignora si el usuario es odontólogo (el plan es suyo)
     * @param sugerir      agrega de inmediato las sugerencias del último odontograma firmado
     * @return id del plan
     */
    @Transactional
    public long crear(long pacienteId, Long odontologoId, boolean sugerir) {
        UsuarioSesion usuario = sesionUsuario.exigir(Permiso.PLAN_EDITAR);
        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new NegocioException("El paciente ya no existe en la base de datos."));
        if (!paciente.isActivo()) {
            throw new NegocioException("El paciente está inactivo: reactívelo antes de crear un plan.");
        }
        Long elegido = Rol.valueOf(usuario.rol()) == Rol.ODONTOLOGO ? usuario.odontologoId() : odontologoId;
        new Validador().requerido(elegido, "el odontólogo responsable").lanzarSiHayErrores();
        Odontologo odontologo = odontologoRepository.findById(elegido)
                .orElseThrow(() -> new NegocioException("El odontólogo ya no existe en la base de datos."));
        if (!odontologo.isActivo()) {
            throw new NegocioException("El odontólogo " + odontologo.getNombreCompleto() + " está inactivo.");
        }
        PlanTratamiento p = new PlanTratamiento();
        p.setPaciente(paciente);
        p.setOdontologo(odontologo);
        p.setFecha(ahora());
        p.setEstado(EstadoPlan.BORRADOR);
        PlanTratamiento guardado = planRepository.save(p);
        if (sugerir) {
            agregarSugerencias(guardado);
        }
        log.info("Plan {} creado para {}", guardado.getId(), paciente.getNumeroHistoria());
        return guardado.getId();
    }

    /** Agrega al borrador los procedimientos sugeridos por el motor de reglas (RF-PLN-01). */
    @Transactional
    public PlanDto sugerir(long planId) {
        sesionUsuario.exigir(Permiso.PLAN_EDITAR);
        PlanTratamiento p = editable(planId);
        int agregados = agregarSugerencias(p);
        if (agregados == 0) {
            throw new NegocioException("No hay sugerencias nuevas: los procedimientos sugeridos ya están en este plan "
                    + "o los hallazgos ya tienen tratamiento en otro plan.");
        }
        return dto(p);
    }

    /** Descuento global y observaciones (RF-PLN-03). */
    @Transactional
    public PlanDto actualizarCabecera(long planId, BigDecimal descuentoGlobal, String observaciones) {
        sesionUsuario.exigir(Permiso.PLAN_EDITAR);
        PlanTratamiento p = buscar(planId);
        String obs = Validador.limpioMultilinea(observaciones);
        new Validador().maximo(obs, 1000, "observaciones").lanzarSiHayErrores();
        BigDecimal descuento = descuentoGlobal == null ? BigDecimal.ZERO : descuentoGlobal;
        if (descuento.compareTo(p.getDescuento()) != 0) {
            exigirEditable(p);
            validarMonto(descuento, "El descuento global");
            p.setDescuento(descuento);
            recalcular(p);
        }
        p.setObservaciones(obs);
        return dto(p);
    }

    /**
     * Cambia el estado del plan (RF-PLN-04). Para pasar a ACEPTADO use {@link #aceptar(long, String)}.
     */
    @Transactional
    public PlanDto cambiarEstado(long planId, EstadoPlan destino) {
        sesionUsuario.exigir(Permiso.PLAN_EDITAR);
        PlanTratamiento p = buscar(planId);
        if (destino == EstadoPlan.ACEPTADO) {
            throw new NegocioException("Para aceptar el plan registre el consentimiento de tratamiento.");
        }
        exigirTransicion(p, destino);
        List<PlanItem> items = itemRepository.listarPorPlan(p.getId());
        List<Cuota> cuotas = cuotaRepository.findByPlanIdOrderByNumeroAsc(p.getId());
        switch (destino) {
            case PROPUESTO -> {
                if (items.stream().noneMatch(i -> i.getEstado() != EstadoPlanItem.CANCELADO)) {
                    throw new NegocioException("Agregue al menos un procedimiento antes de proponer el plan.");
                }
                if (p.getTotal().signum() <= 0) {
                    throw new NegocioException("El total del plan debe ser mayor que cero.");
                }
            }
            case BORRADOR -> {
                exigirSinPagos(cuotas, "volver a editar el plan");
                borrarCuotas(cuotas);
            }
            case FINALIZADO -> {
                long pendientes = items.stream()
                        .filter(i -> i.getEstado() != EstadoPlanItem.CANCELADO && i.getEstado() != EstadoPlanItem.REALIZADO)
                        .count();
                if (pendientes > 0) {
                    throw new NegocioException("Quedan " + pendientes + " procedimiento(s) sin realizar: márquelos en la "
                            + "atención antes de finalizar el plan.");
                }
            }
            case ANULADO -> exigirSinPagos(cuotas, "anular el plan");
            default -> {
            }
        }
        p.setEstado(destino);
        log.info("Plan {} → {}", p.getId(), destino);
        return dto(p);
    }

    /**
     * El paciente acepta el plan propuesto: registra el consentimiento informado de tratamiento y, si no hay
     * cronograma, crea una cuota única con vencimiento hoy.
     *
     * @param firmadoPor paciente o apoderado que firma el consentimiento
     */
    @Transactional
    public PlanDto aceptar(long planId, String firmadoPor) {
        long usuarioId = sesionUsuario.exigir(Permiso.PLAN_EDITAR).id();
        PlanTratamiento p = buscar(planId);
        exigirTransicion(p, EstadoPlan.ACEPTADO);
        String firmante = Validador.limpio(firmadoPor);
        new Validador().requerido(firmante, "el nombre de quien firma el consentimiento")
                .maximo(firmante, 120, "firmado por").lanzarSiHayErrores();
        LocalDateTime ahora = ahora();
        Consentimiento c = new Consentimiento();
        c.setPaciente(p.getPaciente());
        c.setTipo(TipoConsentimiento.TRATAMIENTO);
        c.setPlanId(p.getId());
        c.setAceptadoEn(ahora);
        c.setFirmadoPor(firmante);
        c.setRegistradoPor(usuarioId);
        consentimientoRepository.save(c);
        if (cuotaRepository.findByPlanIdOrderByNumeroAsc(p.getId()).isEmpty()) {
            long total = DineroConverter.aCentimos(p.getTotal());
            guardarCuotas(p, generador.generar(total, total, 0, null, null, LocalDate.now()));
        }
        p.setEstado(EstadoPlan.ACEPTADO);
        p.setAceptadoEn(ahora);
        log.info("Plan {} aceptado; consentimiento firmado por {}", p.getId(), firmante);
        return dto(p);
    }

    // ------------------------------------------------------------------ ítems

    /** Agrega o modifica un ítem del borrador (RF-PLN-02). */
    @Transactional
    public PlanDto guardarItem(PlanItemForm form) {
        sesionUsuario.exigir(Permiso.PLAN_EDITAR);
        PlanTratamiento p = editable(form.planId());
        new Validador()
                .requerido(form.procedimientoId(), "el procedimiento")
                .si(form.fase() < 1 || form.fase() > 4, "La fase debe estar entre 1 y 4.")
                .si(form.piezaFdi() != null && !PiezaDental.esValida(form.piezaFdi()),
                        "La pieza " + form.piezaFdi() + " no es un número FDI válido.")
                .lanzarSiHayErrores();
        Procedimiento proc = procedimientoRepository.findById(form.procedimientoId())
                .orElseThrow(() -> new NegocioException("El procedimiento ya no existe en la base de datos."));
        PlanItem item;
        if (form.id() == null) {
            if (!proc.isActivo()) {
                throw new NegocioException("El procedimiento " + proc.getCodigo() + " está inactivo en el tarifario.");
            }
            item = new PlanItem();
            item.setPlan(p);
            item.setOrden(siguienteOrden(p.getId()));
        } else {
            item = itemDelPlan(form.id(), p);
        }
        Set<Superficie> superficies = form.superficies();
        int cantidad = form.cantidad();
        if (proc.getFormaCobro() == FormaCobro.SUPERFICIE) {
            new Validador()
                    .si(form.piezaFdi() == null, "El procedimiento se cobra por superficie: indique la pieza.")
                    .si(superficies.isEmpty(), "El procedimiento se cobra por superficie: marque al menos una.")
                    .lanzarSiHayErrores();
            cantidad = superficies.size();
        } else if (form.piezaFdi() == null) {
            superficies = Set.of();
        }
        BigDecimal precio = form.precioUnit() == null ? proc.getPrecio() : form.precioUnit();
        BigDecimal descuento = form.descuento() == null ? BigDecimal.ZERO : form.descuento();
        validarMonto(precio, "El precio");
        validarMonto(descuento, "El descuento");
        if (cantidad < 1 || cantidad > 99) {
            throw new NegocioException("La cantidad debe estar entre 1 y 99.");
        }
        BigDecimal total = DineroConverter.desdeCentimos(totalItem(cantidad, precio, descuento));

        item.setProcedimiento(proc);
        item.setPiezaFdi(form.piezaFdi());
        item.setSuperficies(superficies.isEmpty() ? EnumSet.noneOf(Superficie.class) : EnumSet.copyOf(superficies));
        item.setFase(form.fase());
        item.setCantidad(cantidad);
        item.setPrecioUnit(precio);
        item.setDescuento(descuento);
        item.setTotal(total);
        itemRepository.save(item);
        recalcular(p);
        return dto(p);
    }

    /** Cambia solo el descuento de un ítem (edición en la tabla). */
    @Transactional
    public PlanDto actualizarDescuentoItem(long itemId, BigDecimal descuento) {
        sesionUsuario.exigir(Permiso.PLAN_EDITAR);
        PlanItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NegocioException("El ítem ya no existe en la base de datos."));
        PlanTratamiento p = item.getPlan();
        exigirEditable(p);
        BigDecimal d = descuento == null ? BigDecimal.ZERO : descuento;
        validarMonto(d, "El descuento");
        item.setDescuento(d);
        item.setTotal(DineroConverter.desdeCentimos(totalItem(item.getCantidad(), item.getPrecioUnit(), d)));
        recalcular(p);
        return dto(p);
    }

    /** Quita un ítem del borrador. */
    @Transactional
    public PlanDto eliminarItem(long itemId) {
        sesionUsuario.exigir(Permiso.PLAN_EDITAR);
        PlanItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NegocioException("El ítem ya no existe en la base de datos."));
        PlanTratamiento p = item.getPlan();
        exigirEditable(p);
        itemRepository.delete(item);
        itemRepository.flush();
        recalcular(p);
        return dto(p);
    }

    // ------------------------------------------------------------------ cronograma

    /** Genera (o regenera) el cronograma de cuotas (RF-PLN-05); bloqueado si alguna cuota tiene pagos. */
    @Transactional
    public PlanDto generarCronograma(CronogramaForm form) {
        sesionUsuario.exigir(Permiso.PLAN_EDITAR);
        PlanTratamiento p = buscar(form.planId());
        if (!p.getEstado().admiteCronograma()) {
            throw new NegocioException(p.getEstado() == EstadoPlan.BORRADOR
                    ? "Proponga el plan antes de generar el cronograma: así el total ya no cambia."
                    : "El plan está " + p.getEstado().etiqueta().toLowerCase() + ": no admite cambios de cronograma.");
        }
        List<Cuota> actuales = cuotaRepository.findByPlanIdOrderByNumeroAsc(p.getId());
        exigirSinPagos(actuales, "regenerar el cronograma");
        BigDecimal inicial = form.inicial() == null ? BigDecimal.ZERO : form.inicial();
        validarMonto(inicial, "La cuota inicial");
        if (form.numeroCuotas() > MAXIMO_CUOTAS) {
            throw new NegocioException("El cronograma admite como máximo " + MAXIMO_CUOTAS + " cuotas.");
        }
        List<CuotaProgramada> programadas;
        try {
            programadas = generador.generar(DineroConverter.aCentimos(p.getTotal()), DineroConverter.aCentimos(inicial),
                    form.numeroCuotas(), form.primerVencimiento(), form.frecuencia(), LocalDate.now());
        } catch (ReglaDominioException e) {
            throw new NegocioException(e.getMessage());
        } catch (NullPointerException e) {
            throw new NegocioException("Indique la fecha del primer vencimiento y la frecuencia.");
        }
        borrarCuotas(actuales);
        guardarCuotas(p, programadas);
        return dto(p);
    }

    // ------------------------------------------------------------------ atención

    /**
     * Marca (o desmarca) un procedimiento del plan como realizado en la atención (RF-ATE-02). Marcar el primero
     * pasa el plan de ACEPTADO a EN_CURSO.
     */
    @Transactional
    public List<ItemAtencionDto> marcarEnAtencion(long atencionId, long itemId, boolean realizado) {
        sesionUsuario.exigir(Permiso.ATENCION_GESTIONAR);
        Atencion atencion = atencionRepository.findById(atencionId)
                .orElseThrow(() -> new NegocioException("La atención ya no existe en la base de datos."));
        if (atencion.getEstado() != EstadoAtencion.ABIERTA) {
            throw new NegocioException("La atención está cerrada: no se pueden marcar procedimientos.");
        }
        PlanItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NegocioException("El ítem ya no existe en la base de datos."));
        PlanTratamiento p = item.getPlan();
        if (!p.getPaciente().getId().equals(atencion.getPaciente().getId())) {
            throw new NegocioException("El procedimiento pertenece al plan de otro paciente.");
        }
        if (!p.getEstado().enEjecucion()) {
            throw new NegocioException("El plan está " + p.getEstado().etiqueta().toLowerCase()
                    + ": solo se marcan procedimientos de planes aceptados o en curso.");
        }
        if (realizado) {
            if (item.getEstado() == EstadoPlanItem.REALIZADO) {
                throw new NegocioException("El procedimiento ya figura como realizado.");
            }
            item.setEstado(EstadoPlanItem.REALIZADO);
            item.setAtencionId(atencionId);
            item.setRealizadoEn(ahora());
            if (p.getEstado() == EstadoPlan.ACEPTADO) {
                p.setEstado(EstadoPlan.EN_CURSO);
            }
        } else {
            if (item.getEstado() != EstadoPlanItem.REALIZADO || !Objects.equals(item.getAtencionId(), atencionId)) {
                throw new NegocioException("Solo se puede desmarcar un procedimiento realizado en esta misma atención.");
            }
            item.setEstado(EstadoPlanItem.PENDIENTE);
            item.setAtencionId(null);
            item.setRealizadoEn(null);
        }
        itemRepository.flush();
        return itemsParaAtencion(atencionId);
    }

    // ------------------------------------------------------------------ apoyo

    private int agregarSugerencias(PlanTratamiento p) {
        OdontogramaService.EstadoFirmado firmado = odontogramaService.estadoFirmado(p.getPaciente().getId())
                .orElseThrow(() -> new NegocioException("El paciente no tiene un odontograma firmado: "
                        + "cierre y firme el odontograma antes de pedir sugerencias."));
        List<ReglaSugerencia> reglas = reglaRepository.listarConRelaciones().stream()
                .filter(r -> r.isActivo() && r.getProcedimiento().isActivo())
                .toList();
        Map<Long, ProcedimientoDef> tarifario = reglas.stream().map(ReglaSugerencia::getProcedimiento).distinct()
                .collect(Collectors.toMap(Procedimiento::getId, PlanTratamientoService::definicion, (a, b) -> a));
        List<ItemSugerido> sugeridos = motor.sugerir(firmado.estado(),
                reglas.stream().map(PlanTratamientoService::definicion).toList(), tarifario);

        List<PlanItem> existentes = itemRepository.listarPorPlan(p.getId());
        Set<String> enPlan = new HashSet<>();
        for (PlanItem i : existentes) {
            if (i.getEstado() != EstadoPlanItem.CANCELADO) {
                enPlan.add(i.getProcedimiento().getId() + "@" + i.getPiezaFdi());
            }
        }
        Map<Long, Procedimiento> procedimientos = reglas.stream().map(ReglaSugerencia::getProcedimiento).distinct()
                .collect(Collectors.toMap(Procedimiento::getId, Function.identity(), (a, b) -> a));
        int orden = siguienteOrden(p.getId());
        Set<Long> tratadosEnOtrosPlanes = new HashSet<>(
                itemRepository.hallazgosEnOtrosPlanes(p.getPaciente().getId(), p.getId()));
        int agregados = 0;
        for (ItemSugerido s : sugeridos) {
            if (s.origenHallazgoId() != null && tratadosEnOtrosPlanes.contains(s.origenHallazgoId())) {
                continue;           // ya planificado o realizado en otro plan
            }
            if (!enPlan.add(s.procedimientoId() + "@" + s.piezaFdi())) {
                continue;
            }
            PlanItem item = new PlanItem();
            item.setPlan(p);
            item.setProcedimiento(procedimientos.get(s.procedimientoId()));
            item.setHallazgoId(s.origenHallazgoId());
            item.setPiezaFdi(s.piezaFdi());
            item.setSuperficies(s.superficies().isEmpty() ? EnumSet.noneOf(Superficie.class) : EnumSet.copyOf(s.superficies()));
            item.setFase(s.fase());
            item.setCantidad(s.cantidad());
            item.setPrecioUnit(DineroConverter.desdeCentimos(s.precioUnitCent()));
            item.setDescuento(BigDecimal.ZERO);
            item.setTotal(DineroConverter.desdeCentimos(s.totalCent()));
            item.setOrden(orden++);
            itemRepository.save(item);
            agregados++;
        }
        p.setOdontogramaId(firmado.odontogramaId());
        itemRepository.flush();
        recalcular(p);
        log.info("Plan {}: {} procedimiento(s) sugerido(s) desde el odontograma {}", p.getId(), agregados,
                firmado.odontogramaId());
        return agregados;
    }

    /** Recalcula subtotal y total; el descuento global no puede superar el nuevo subtotal. */
    private void recalcular(PlanTratamiento p) {
        List<CalculadoraTotales.Linea> lineas = itemRepository.listarPorPlan(p.getId()).stream()
                .map(i -> new CalculadoraTotales.Linea(i.getCantidad(), DineroConverter.aCentimos(i.getPrecioUnit()),
                        DineroConverter.aCentimos(i.getDescuento()), i.getEstado() == EstadoPlanItem.CANCELADO))
                .toList();
        CalculadoraTotales.Totales t;
        try {
            t = calculadora.calcular(lineas, DineroConverter.aCentimos(p.getDescuento()));
        } catch (ReglaDominioException e) {
            throw new NegocioException(e.getMessage());
        }
        p.setSubtotal(DineroConverter.desdeCentimos(t.subtotalCent()));
        p.setTotal(DineroConverter.desdeCentimos(t.totalCent()));
    }

    private long totalItem(int cantidad, BigDecimal precio, BigDecimal descuento) {
        try {
            return calculadora.totalItem(cantidad, DineroConverter.aCentimos(precio), DineroConverter.aCentimos(descuento));
        } catch (ReglaDominioException e) {
            throw new NegocioException(e.getMessage());
        }
    }

    private void guardarCuotas(PlanTratamiento p, List<CuotaProgramada> programadas) {
        for (CuotaProgramada c : programadas) {
            Cuota cuota = new Cuota();
            cuota.setPlan(p);
            cuota.setNumero(c.numero());
            cuota.setVencimiento(c.vencimiento());
            cuota.setMonto(DineroConverter.desdeCentimos(c.montoCent()));
            cuota.setPagado(BigDecimal.ZERO);
            cuotaRepository.save(cuota);
        }
        cuotaRepository.flush();
    }

    /** Borra las cuotas y sincroniza antes de insertar otras (Hibernate inserta antes de borrar). */
    private void borrarCuotas(List<Cuota> cuotas) {
        if (!cuotas.isEmpty()) {
            cuotaRepository.deleteAll(cuotas);
            cuotaRepository.flush();
        }
    }

    private static void exigirSinPagos(List<Cuota> cuotas, String accion) {
        if (cuotas.stream().anyMatch(c -> c.getPagado().signum() > 0)) {
            throw new NegocioException("El plan ya tiene pagos registrados: no se puede " + accion + ".");
        }
    }

    private static void exigirTransicion(PlanTratamiento p, EstadoPlan destino) {
        if (!p.getEstado().puedePasarA(destino)) {
            throw new NegocioException("Un plan " + p.getEstado().etiqueta().toLowerCase() + " no puede pasar a "
                    + destino.etiqueta().toLowerCase() + ".");
        }
    }

    private static void exigirEditable(PlanTratamiento p) {
        if (!p.getEstado().editable()) {
            throw new NegocioException("El plan está " + p.getEstado().etiqueta().toLowerCase()
                    + ": solo se editan los planes en borrador.");
        }
    }

    private static void validarMonto(BigDecimal monto, String campo) {
        if (monto.signum() < 0 || monto.compareTo(MONTO_MAXIMO) > 0) {
            throw new NegocioException(campo + " debe estar entre 0 y 999 999.99.");
        }
        if (monto.stripTrailingZeros().scale() > 2) {
            throw new NegocioException(campo + " admite como máximo 2 decimales.");
        }
    }

    private PlanTratamiento buscar(long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new NegocioException("El plan ya no existe en la base de datos."));
    }

    private PlanTratamiento editable(long id) {
        PlanTratamiento p = buscar(id);
        exigirEditable(p);
        return p;
    }

    private PlanItem itemDelPlan(long itemId, PlanTratamiento p) {
        PlanItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NegocioException("El ítem ya no existe en la base de datos."));
        if (!item.getPlan().getId().equals(p.getId())) {
            throw new NegocioException("El ítem no pertenece a este plan.");
        }
        return item;
    }

    private int siguienteOrden(long planId) {
        return itemRepository.listarPorPlan(planId).stream().mapToInt(PlanItem::getOrden).max().orElse(0) + 1;
    }

    private PresupuestoDocumento documento(long planId) {
        PlanDto plan = obtener(planId);
        PacienteDetalle paciente = pacienteService.obtener(plan.pacienteId());
        return new PresupuestoDocumento(configuracionService.datosClinica(), paciente, plan);
    }

    private PlanDto dto(PlanTratamiento p) {
        List<PlanItemDto> items = new ArrayList<>(itemRepository.listarPorPlan(p.getId()).stream()
                .map(PlanTratamientoService::dto).toList());
        items.sort(PlanTratamientoService.ORDEN_ITEMS);
        LocalDate hoy = LocalDate.now();
        List<CuotaDto> cuotas = cuotaRepository.findByPlanIdOrderByNumeroAsc(p.getId()).stream()
                .map(c -> new CuotaDto(c.getId(), c.getNumero(), c.getVencimiento(), c.getMonto(), c.getPagado(),
                        EstadoCuota.de(DineroConverter.aCentimos(c.getMonto()), DineroConverter.aCentimos(c.getPagado()),
                                c.getVencimiento(), hoy)))
                .toList();
        var consentimiento = consentimientoRepository.findFirstByPlanIdAndTipoOrderByAceptadoEnDesc(p.getId(),
                TipoConsentimiento.TRATAMIENTO);
        Odontologo o = p.getOdontologo();
        return new PlanDto(p.getId(), p.getPaciente().getId(), o.getId(), o.getNombreCompleto(), o.getCop(),
                p.getOdontogramaId(), p.getFecha(), p.getEstado(), p.getSubtotal(), p.getDescuento(), p.getTotal(),
                p.getObservaciones(), p.getAceptadoEn(), items, cuotas,
                consentimiento.map(Consentimiento::getFirmadoPor).orElse(null),
                consentimiento.map(Consentimiento::getAceptadoEn).orElse(null), p.getEstado().siguientes());
    }

    /** Fase y luego pieza en orden de dibujo (los ítems sin pieza primero), como el motor de sugerencias. */
    private static final Comparator<PlanItemDto> ORDEN_ITEMS = Comparator
            .comparingInt(PlanItemDto::fase)
            .thenComparingInt(i -> i.pieza() == null ? -1 : PiezaDental.de(i.pieza()).ordenDibujo())
            .thenComparingLong(PlanItemDto::id);

    private static PlanItemDto dto(PlanItem i) {
        Procedimiento proc = i.getProcedimiento();
        return new PlanItemDto(i.getId(), i.getPlan().getId(), proc.getId(), proc.getCodigo(), proc.getNombre(),
                proc.getFormaCobro(), i.getHallazgoId(), i.getPiezaFdi(), i.getSuperficies(), i.getFase(), i.getCantidad(),
                i.getPrecioUnit(), i.getDescuento(), i.getTotal(), i.getEstado(), i.getAtencionId(), i.getRealizadoEn());
    }

    private static ProcedimientoDef definicion(Procedimiento p) {
        return new ProcedimientoDef(p.getId(), p.getCodigo(), p.getNombre(), DineroConverter.aCentimos(p.getPrecio()),
                p.getFormaCobro());
    }

    private static ReglaSugerenciaDef definicion(ReglaSugerencia r) {
        return new ReglaSugerenciaDef(r.getId(), r.getHallazgoTipo() == null ? null : r.getHallazgoTipo().getId(),
                r.getEstado(), r.getTipoRadicular(), r.getMinSuperficies(), r.getMaxSuperficies(),
                r.getProcedimiento().getId(), r.getFase(), r.getPrioridad());
    }

    private static LocalDateTime ahora() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
