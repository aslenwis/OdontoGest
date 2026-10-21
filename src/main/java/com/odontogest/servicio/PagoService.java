/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio;

import com.odontogest.documento.ReciboPdf;
import com.odontogest.dominio.comun.ReglaDominioException;
import com.odontogest.dominio.finanzas.Aplicacion;
import com.odontogest.dominio.finanzas.AsignadorPagosFifo;
import com.odontogest.dominio.finanzas.EstadoCuota;
import com.odontogest.dominio.finanzas.SaldoCuota;
import com.odontogest.evento.PagoRegistrado;
import com.odontogest.excepcion.NegocioException;
import com.odontogest.modelo.Cuota;
import com.odontogest.modelo.EstadoPlan;
import com.odontogest.modelo.MedioPago;
import com.odontogest.modelo.Paciente;
import com.odontogest.modelo.Pago;
import com.odontogest.modelo.PagoAplicacion;
import com.odontogest.modelo.PlanTratamiento;
import com.odontogest.modelo.Usuario;
import com.odontogest.persistencia.conversor.DineroConverter;
import com.odontogest.persistencia.repositorio.CuotaRepository;
import com.odontogest.persistencia.repositorio.PacienteRepository;
import com.odontogest.persistencia.repositorio.PagoAplicacionRepository;
import com.odontogest.persistencia.repositorio.PagoRepository;
import com.odontogest.persistencia.repositorio.UsuarioRepository;
import com.odontogest.seguridad.Permiso;
import com.odontogest.seguridad.SesionUsuario;
import com.odontogest.servicio.dto.AplicacionDto;
import com.odontogest.servicio.dto.CuotaPacienteDto;
import com.odontogest.servicio.dto.EstadoCuentaDto;
import com.odontogest.servicio.dto.PagoDto;
import com.odontogest.servicio.dto.PagoForm;
import com.odontogest.servicio.dto.ReciboDocumento;
import com.odontogest.servicio.dto.ResumenCajaDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Caja (RF-CAJ-01 a RF-CAJ-05): cobros repartidos en orden FIFO entre las cuotas pendientes del paciente, recibo
 * interno con correlativo sin saltos, anulación con motivo, estado de cuenta y caja del día.
 * <ul>
 *   <li>Se cobran las cuotas de planes ACEPTADOS, EN CURSO o FINALIZADOS (los propuestos aún no se cobran).</li>
 *   <li>{@link #registrar(PagoForm)} consume el correlativo, inserta el pago y sus aplicaciones y suma lo pagado en
 *       cada cuota en una sola transacción; el primer pago pasa el plan de ACEPTADO a EN CURSO.</li>
 *   <li>{@link #anular(long, String)} marca el pago, resta sus aplicaciones de las cuotas y conserva las filas de
 *       {@code pago_aplicacion}.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PagoService {

    public static final int LARGO_MINIMO_MOTIVO = 10;
    /** Estados del plan cuyas cuotas se cobran. */
    static final Set<EstadoPlan> COBRABLES = EnumSet.of(EstadoPlan.ACEPTADO, EstadoPlan.EN_CURSO, EstadoPlan.FINALIZADO);
    private static final BigDecimal MONTO_MAXIMO = new BigDecimal("999999.99");

    private final PagoRepository pagoRepository;
    private final AuditoriaService auditoria;
    private final PagoAplicacionRepository aplicacionRepository;
    private final CuotaRepository cuotaRepository;
    private final PacienteRepository pacienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final CorrelativoService correlativoService;
    private final PacienteService pacienteService;
    private final ConfiguracionService configuracionService;
    private final ReciboPdf reciboPdf;
    private final SesionUsuario sesionUsuario;
    private final ApplicationEventPublisher eventos;

    private final AsignadorPagosFifo asignador = new AsignadorPagosFifo();

    // ------------------------------------------------------------------ consultas

    /** Cuotas con saldo de los planes cobrables del paciente, en orden FIFO. */
    public List<CuotaPacienteDto> cuotasPendientes(long pacienteId) {
        LocalDate hoy = LocalDate.now();
        return cuotaRepository.listarPorPaciente(pacienteId, COBRABLES).stream()
                .filter(c -> c.getPagado().compareTo(c.getMonto()) < 0)
                .map(c -> dto(c, hoy))
                .toList();
    }

    /**
     * Reparto FIFO del monto sin registrar nada (vista previa de la caja).
     *
     * @throws NegocioException si el monto no es válido o excede el saldo pendiente
     */
    public List<AplicacionDto> vistaPrevia(long pacienteId, BigDecimal monto) {
        validarMonto(monto);
        List<Cuota> pendientes = pendientes(pacienteId);
        Map<Long, Cuota> porId = pendientes.stream().collect(Collectors.toMap(Cuota::getId, Function.identity()));
        return asignar(monto, pendientes).stream()
                .map(a -> {
                    Cuota c = porId.get(a.cuotaId());
                    return new AplicacionDto(c.getId(), c.getPlan().getId(), c.getNumero(), c.getVencimiento(),
                            c.getMonto().subtract(c.getPagado()), DineroConverter.desdeCentimos(a.montoCent()));
                })
                .toList();
    }

    /** Estado de cuenta del paciente (RF-CAJ-04). */
    public EstadoCuentaDto estadoCuenta(long pacienteId) {
        LocalDate hoy = LocalDate.now();
        List<CuotaPacienteDto> cuotas = cuotaRepository.listarPorPaciente(pacienteId, COBRABLES).stream()
                .map(c -> dto(c, hoy)).toList();
        BigDecimal total = suma(cuotas.stream().map(CuotaPacienteDto::monto).toList());
        BigDecimal pagado = suma(cuotas.stream().map(CuotaPacienteDto::pagado).toList());
        BigDecimal vencido = suma(cuotas.stream().filter(c -> c.estado() == EstadoCuota.VENCIDA)
                .map(CuotaPacienteDto::saldo).toList());
        return new EstadoCuentaDto(pacienteId, total, pagado, total.subtract(pagado), vencido, cuotas,
                dtos(pagoRepository.listarPorPaciente(pacienteId)));
    }

    /** Saldo pendiente del paciente (cabecera de la ficha). */
    public BigDecimal saldo(long pacienteId) {
        return suma(cuotaRepository.listarPorPaciente(pacienteId, COBRABLES).stream()
                .map(c -> c.getMonto().subtract(c.getPagado())).toList());
    }

    /** Pagos de un día y totales por medio de pago, sin los anulados (RF-CAJ-05). */
    public ResumenCajaDto cajaDelDia(LocalDate fecha) {
        List<PagoDto> pagos = dtos(pagoRepository.listarEntre(fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay()));
        Map<MedioPago, BigDecimal> porMedio = new EnumMap<>(MedioPago.class);
        for (MedioPago m : MedioPago.values()) {
            porMedio.put(m, BigDecimal.ZERO.setScale(2));
        }
        for (PagoDto p : pagos) {
            if (!p.anulado()) {
                porMedio.merge(p.medio(), p.monto(), BigDecimal::add);
            }
        }
        return new ResumenCajaDto(fecha, pagos, porMedio, suma(List.copyOf(porMedio.values())));
    }

    public PagoDto obtener(long pagoId) {
        return dtos(List.of(buscar(pagoId))).get(0);
    }

    /** Recibo interno en PDF (RF-CAJ-02). */
    public byte[] pdfRecibo(long pagoId) {
        PagoDto pago = obtener(pagoId);
        return reciboPdf.generar(new ReciboDocumento(configuracionService.datosClinica(),
                pacienteService.obtener(pago.pacienteId()), pago, saldo(pago.pacienteId())));
    }

    // ------------------------------------------------------------------ escritura

    /** Registra el cobro y lo aplica FIFO a las cuotas pendientes (RF-CAJ-01). */
    @Transactional
    public PagoDto registrar(PagoForm form) {
        long usuarioId = sesionUsuario.exigir(Permiso.PAGO_REGISTRAR).id();
        Paciente paciente = pacienteRepository.findById(form.pacienteId())
                .orElseThrow(() -> new NegocioException("El paciente ya no existe en la base de datos."));
        validarMonto(form.monto());
        String referencia = Validador.limpio(form.referencia());
        new Validador()
                .requerido(form.medio(), "el medio de pago")
                .maximo(referencia, 60, "referencia")
                .lanzarSiHayErrores();

        List<Cuota> pendientes = pendientes(paciente.getId());
        if (pendientes.isEmpty()) {
            throw new NegocioException("El paciente no tiene cuotas pendientes de pago.");
        }
        List<Aplicacion> aplicaciones = asignar(form.monto(), pendientes);
        Map<Long, Cuota> porId = pendientes.stream().collect(Collectors.toMap(Cuota::getId, Function.identity()));
        Set<Long> planes = aplicaciones.stream().map(a -> porId.get(a.cuotaId()).getPlan().getId())
                .collect(Collectors.toSet());

        Pago pago = new Pago();
        pago.setNumeroRecibo(correlativoService.siguienteRecibo());
        pago.setPaciente(paciente);
        pago.setPlanId(planes.size() == 1 ? planes.iterator().next() : null);
        pago.setFecha(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        pago.setMonto(form.monto());
        pago.setMedio(form.medio());
        pago.setReferencia(referencia);
        pago.setRegistradoPor(usuarioId);
        Pago guardado = pagoRepository.save(pago);

        for (Aplicacion a : aplicaciones) {
            Cuota cuota = porId.get(a.cuotaId());
            BigDecimal monto = DineroConverter.desdeCentimos(a.montoCent());
            PagoAplicacion pa = new PagoAplicacion();
            pa.setPago(guardado);
            pa.setCuota(cuota);
            pa.setMonto(monto);
            aplicacionRepository.save(pa);
            cuota.setPagado(cuota.getPagado().add(monto));
            PlanTratamiento plan = cuota.getPlan();
            if (plan.getEstado() == EstadoPlan.ACEPTADO) {
                plan.setEstado(EstadoPlan.EN_CURSO);
            }
        }
        aplicacionRepository.flush();
        log.info("Pago {} de S/ {} registrado para {}", guardado.getNumeroRecibo(), form.monto(), paciente.getNumeroHistoria());
        auditoria.registrar("PAGO_REGISTRAR", "pago", guardado.getId(), Map.of("recibo", guardado.getNumeroRecibo(),
                "monto", form.monto().toPlainString(), "medio", form.medio().name()));
        eventos.publishEvent(new PagoRegistrado(paciente.getId(), guardado.getId(), guardado.getNumeroRecibo(), false));
        return obtener(guardado.getId());
    }

    /** Anula un pago con motivo y revierte lo aplicado en las cuotas (RF-CAJ-03). */
    @Transactional
    public PagoDto anular(long pagoId, String motivo) {
        sesionUsuario.exigir(Permiso.PAGO_ANULAR);
        Pago pago = buscar(pagoId);
        if (pago.isAnulado()) {
            throw new NegocioException("El pago " + pago.getNumeroRecibo() + " ya está anulado.");
        }
        String m = Validador.limpioMultilinea(motivo);
        new Validador()
                .requerido(m, "el motivo de la anulación")
                .si(m != null && m.length() < LARGO_MINIMO_MOTIVO,
                        "Describa el motivo con al menos " + LARGO_MINIMO_MOTIVO + " caracteres.")
                .maximo(m, 300, "motivo")
                .lanzarSiHayErrores();
        for (PagoAplicacion a : aplicacionRepository.listarPorPagos(List.of(pago.getId()))) {
            Cuota cuota = a.getCuota();
            BigDecimal pagado = cuota.getPagado().subtract(a.getMonto());
            if (pagado.signum() < 0) {
                throw new IllegalStateException("Lo pagado de la cuota " + cuota.getId() + " quedaría negativo.");
            }
            cuota.setPagado(pagado);
        }
        pago.setAnulado(true);
        pago.setMotivoAnulacion(m);
        pago.setAnuladoEn(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        cuotaRepository.flush();
        log.info("Pago {} anulado: {}", pago.getNumeroRecibo(), m);
        auditoria.registrar("PAGO_ANULAR", "pago", pago.getId(), Map.of("recibo", pago.getNumeroRecibo(), "motivo", m));
        eventos.publishEvent(new PagoRegistrado(pago.getPaciente().getId(), pago.getId(), pago.getNumeroRecibo(), true));
        return obtener(pago.getId());
    }

    // ------------------------------------------------------------------ apoyo

    private List<Cuota> pendientes(long pacienteId) {
        return cuotaRepository.listarPorPaciente(pacienteId, COBRABLES).stream()
                .filter(c -> c.getPagado().compareTo(c.getMonto()) < 0)
                .toList();
    }

    private List<Aplicacion> asignar(BigDecimal monto, List<Cuota> pendientes) {
        List<SaldoCuota> saldos = pendientes.stream()
                .map(c -> new SaldoCuota(c.getId(), c.getVencimiento(), c.getNumero(),
                        DineroConverter.aCentimos(c.getMonto().subtract(c.getPagado()))))
                .toList();
        try {
            return asignador.asignar(DineroConverter.aCentimos(monto), saldos);
        } catch (ReglaDominioException e) {
            throw new NegocioException(pendientes.isEmpty() ? "El paciente no tiene cuotas pendientes de pago."
                    : e.getMessage());
        }
    }

    private static void validarMonto(BigDecimal monto) {
        if (monto == null) {
            throw new NegocioException("Ingrese el monto a cobrar.");
        }
        if (monto.signum() <= 0 || monto.compareTo(MONTO_MAXIMO) > 0) {
            throw new NegocioException("El monto debe ser mayor que cero y menor que 1 000 000.");
        }
        if (monto.stripTrailingZeros().scale() > 2) {
            throw new NegocioException("El monto admite como máximo 2 decimales.");
        }
    }

    private Pago buscar(long id) {
        return pagoRepository.findById(id)
                .orElseThrow(() -> new NegocioException("El pago ya no existe en la base de datos."));
    }

    private List<PagoDto> dtos(List<Pago> pagos) {
        if (pagos.isEmpty()) {
            return List.of();
        }
        Map<Long, List<AplicacionDto>> aplicaciones = new HashMap<>();
        for (PagoAplicacion a : aplicacionRepository.listarPorPagos(pagos.stream().map(Pago::getId).toList())) {
            Cuota c = a.getCuota();
            aplicaciones.computeIfAbsent(a.getPago().getId(), k -> new ArrayList<>())
                    .add(new AplicacionDto(c.getId(), c.getPlan().getId(), c.getNumero(), c.getVencimiento(), null, a.getMonto()));
        }
        Map<Long, String> usuarios = usuarioRepository.findAll().stream()
                .collect(Collectors.toMap(Usuario::getId, Usuario::getNombreCompleto));
        return pagos.stream()
                .map(p -> new PagoDto(p.getId(), p.getNumeroRecibo(), p.getPaciente().getId(),
                        p.getPaciente().getNombreCompleto(), p.getPaciente().getNumeroHistoria(), p.getFecha(), p.getMonto(),
                        p.getMedio(), p.getReferencia(), usuarios.getOrDefault(p.getRegistradoPor(), "—"), p.isAnulado(),
                        p.getMotivoAnulacion(), p.getAnuladoEn(), aplicaciones.getOrDefault(p.getId(), List.of())))
                .toList();
    }

    private static CuotaPacienteDto dto(Cuota c, LocalDate hoy) {
        return new CuotaPacienteDto(c.getId(), c.getPlan().getId(), c.getNumero(), c.getVencimiento(), c.getMonto(),
                c.getPagado(), EstadoCuota.de(DineroConverter.aCentimos(c.getMonto()), DineroConverter.aCentimos(c.getPagado()),
                c.getVencimiento(), hoy));
    }

    private static BigDecimal suma(List<BigDecimal> montos) {
        return montos.stream().reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
    }
}
