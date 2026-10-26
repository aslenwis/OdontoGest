package com.odontogest.servicio;

import com.odontogest.documento.ReciboPdfASEJ;
import com.odontogest.dominio.comun.ReglaDominioExceptionASEJ;
import com.odontogest.dominio.finanzas.AplicacionASEJ;
import com.odontogest.dominio.finanzas.AsignadorPagosFifoASEJ;
import com.odontogest.dominio.finanzas.EstadoCuotaASEJ;
import com.odontogest.dominio.finanzas.SaldoCuotaASEJ;
import com.odontogest.evento.PagoRegistradoASEJ;
import com.odontogest.excepcion.NegocioExceptionASEJ;
import com.odontogest.modelo.CuotaASEJ;
import com.odontogest.modelo.EstadoPlanASEJ;
import com.odontogest.modelo.MedioPagoASEJ;
import com.odontogest.modelo.PacienteASEJ;
import com.odontogest.modelo.PagoASEJ;
import com.odontogest.modelo.PagoAplicacionASEJ;
import com.odontogest.modelo.PlanTratamientoASEJ;
import com.odontogest.modelo.UsuarioASEJ;
import com.odontogest.persistencia.conversor.DineroConverterASEJ;
import com.odontogest.persistencia.repositorio.CuotaRepositoryASEJ;
import com.odontogest.persistencia.repositorio.PacienteRepositoryASEJ;
import com.odontogest.persistencia.repositorio.PagoAplicacionRepositoryASEJ;
import com.odontogest.persistencia.repositorio.PagoRepositoryASEJ;
import com.odontogest.persistencia.repositorio.UsuarioRepositoryASEJ;
import com.odontogest.seguridad.PermisoASEJ;
import com.odontogest.seguridad.SesionUsuarioASEJ;
import com.odontogest.servicio.dto.AplicacionDtoASEJ;
import com.odontogest.servicio.dto.CuotaPacienteDtoASEJ;
import com.odontogest.servicio.dto.EstadoCuentaDtoASEJ;
import com.odontogest.servicio.dto.PagoDtoASEJ;
import com.odontogest.servicio.dto.PagoFormASEJ;
import com.odontogest.servicio.dto.ReciboDocumentoASEJ;
import com.odontogest.servicio.dto.ResumenCajaDtoASEJ;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
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

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PagoServiceASEJ {

    public static final int LARGO_MINIMO_MOTIVOASEJ = 10;
    static final Set<EstadoPlanASEJ> COBRABLESASEJ = EnumSet.of(EstadoPlanASEJ.ACEPTADO, EstadoPlanASEJ.EN_CURSO, EstadoPlanASEJ.FINALIZADO);
    private static final BigDecimal MONTO_MAXIMOASEJ = new BigDecimal("999999.99");

    private final Clock relojASEJ;
    private final PagoRepositoryASEJ pagoRepositoryASEJ;
    private final AuditoriaServiceASEJ auditoriaASEJ;
    private final PagoAplicacionRepositoryASEJ aplicacionRepositoryASEJ;
    private final CuotaRepositoryASEJ cuotaRepositoryASEJ;
    private final PacienteRepositoryASEJ pacienteRepositoryASEJ;
    private final UsuarioRepositoryASEJ usuarioRepositoryASEJ;
    private final CorrelativoServiceASEJ correlativoServiceASEJ;
    private final PacienteServiceASEJ pacienteServiceASEJ;
    private final ConfiguracionServiceASEJ configuracionServiceASEJ;
    private final ReciboPdfASEJ reciboPdfASEJ;
    private final SesionUsuarioASEJ sesionUsuarioASEJ;
    private final ApplicationEventPublisher eventosASEJ;

    private final AsignadorPagosFifoASEJ asignadorASEJ = new AsignadorPagosFifoASEJ();

    public List<CuotaPacienteDtoASEJ> cuotasPendientesASEJ(long pacienteIdASEJ) {
        LocalDate hoyASEJ = LocalDate.now(relojASEJ);
        return cuotaRepositoryASEJ.listarPorPacienteASEJ(pacienteIdASEJ, COBRABLESASEJ).stream()
                .filter(cASEJ -> cASEJ.getPagadoASEJ().compareTo(cASEJ.getMontoASEJ()) < 0)
                .map(cASEJ -> dtoASEJ(cASEJ, hoyASEJ))
                .toList();
    }

    public List<AplicacionDtoASEJ> vistaPreviaASEJ(long pacienteIdASEJ, BigDecimal montoASEJ) {
        validarMontoASEJ(montoASEJ);
        List<CuotaASEJ> pendientesASEJ = pendientesASEJ(pacienteIdASEJ);
        Map<Long, CuotaASEJ> porIdASEJ = pendientesASEJ.stream().collect(Collectors.toMap(CuotaASEJ::getIdASEJ, Function.identity()));
        return asignarASEJ(montoASEJ, pendientesASEJ).stream()
                .map(aASEJ -> {
                    CuotaASEJ cASEJ = porIdASEJ.get(aASEJ.cuotaIdASEJ());
                    return new AplicacionDtoASEJ(cASEJ.getIdASEJ(), cASEJ.getPlanASEJ().getIdASEJ(), cASEJ.getNumeroASEJ(), cASEJ.getVencimientoASEJ(),
                            cASEJ.getMontoASEJ().subtract(cASEJ.getPagadoASEJ()), DineroConverterASEJ.desdeCentimosASEJ(aASEJ.montoCentASEJ()));
                })
                .toList();
    }

    public EstadoCuentaDtoASEJ estadoCuentaASEJ(long pacienteIdASEJ) {
        LocalDate hoyASEJ = LocalDate.now(relojASEJ);
        List<CuotaPacienteDtoASEJ> cuotasASEJ = cuotaRepositoryASEJ.listarPorPacienteASEJ(pacienteIdASEJ, COBRABLESASEJ).stream()
                .map(cASEJ -> dtoASEJ(cASEJ, hoyASEJ)).toList();
        BigDecimal totalASEJ = sumaASEJ(cuotasASEJ.stream().map(CuotaPacienteDtoASEJ::montoASEJ).toList());
        BigDecimal pagadoASEJ = sumaASEJ(cuotasASEJ.stream().map(CuotaPacienteDtoASEJ::pagadoASEJ).toList());
        BigDecimal vencidoASEJ = sumaASEJ(cuotasASEJ.stream().filter(cASEJ -> cASEJ.estadoASEJ() == EstadoCuotaASEJ.VENCIDA)
                .map(CuotaPacienteDtoASEJ::saldoASEJ).toList());
        return new EstadoCuentaDtoASEJ(pacienteIdASEJ, totalASEJ, pagadoASEJ, totalASEJ.subtract(pagadoASEJ), vencidoASEJ, cuotasASEJ,
                dtosASEJ(pagoRepositoryASEJ.listarPorPacienteASEJ(pacienteIdASEJ)));
    }

    public BigDecimal saldoASEJ(long pacienteIdASEJ) {
        return sumaASEJ(cuotaRepositoryASEJ.listarPorPacienteASEJ(pacienteIdASEJ, COBRABLESASEJ).stream()
                .map(cASEJ -> cASEJ.getMontoASEJ().subtract(cASEJ.getPagadoASEJ())).toList());
    }

    public ResumenCajaDtoASEJ cajaDelDiaASEJ(LocalDate fechaASEJ) {
        List<PagoDtoASEJ> pagosASEJ = dtosASEJ(pagoRepositoryASEJ.listarEntreASEJ(fechaASEJ.atStartOfDay(), fechaASEJ.plusDays(1).atStartOfDay()));
        Map<MedioPagoASEJ, BigDecimal> porMedioASEJ = new EnumMap<>(MedioPagoASEJ.class);
        for (MedioPagoASEJ mASEJ : MedioPagoASEJ.values()) {
            porMedioASEJ.put(mASEJ, BigDecimal.ZERO.setScale(2));
        }
        for (PagoDtoASEJ pASEJ : pagosASEJ) {
            if (!pASEJ.anuladoASEJ()) {
                porMedioASEJ.merge(pASEJ.medioASEJ(), pASEJ.montoASEJ(), BigDecimal::add);
            }
        }
        return new ResumenCajaDtoASEJ(fechaASEJ, pagosASEJ, porMedioASEJ, sumaASEJ(List.copyOf(porMedioASEJ.values())));
    }

    public PagoDtoASEJ obtenerASEJ(long pagoIdASEJ) {
        return dtosASEJ(List.of(buscarASEJ(pagoIdASEJ))).get(0);
    }

    public byte[] pdfReciboASEJ(long pagoIdASEJ) {
        PagoDtoASEJ pagoASEJ = obtenerASEJ(pagoIdASEJ);
        return reciboPdfASEJ.generarASEJ(new ReciboDocumentoASEJ(configuracionServiceASEJ.datosClinicaASEJ(),
                pacienteServiceASEJ.obtenerASEJ(pagoASEJ.pacienteIdASEJ()), pagoASEJ, saldoASEJ(pagoASEJ.pacienteIdASEJ())));
    }

    @Transactional
    public PagoDtoASEJ registrarASEJ(PagoFormASEJ formASEJ) {
        long usuarioIdASEJ = sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.PAGO_REGISTRAR).idASEJ();
        PacienteASEJ pacienteASEJ = pacienteRepositoryASEJ.findById(formASEJ.pacienteIdASEJ())
                .orElseThrow(() -> new NegocioExceptionASEJ("El paciente ya no existe en la base de datos."));
        validarMontoASEJ(formASEJ.montoASEJ());
        String referenciaASEJ = ValidadorASEJ.limpioASEJ(formASEJ.referenciaASEJ());
        new ValidadorASEJ()
                .requeridoASEJ(formASEJ.medioASEJ(), "el medio de pago")
                .maximoASEJ(referenciaASEJ, 60, "referencia")
                .lanzarSiHayErroresASEJ();

        List<CuotaASEJ> pendientesASEJ = pendientesASEJ(pacienteASEJ.getIdASEJ());
        if (pendientesASEJ.isEmpty()) {
            throw new NegocioExceptionASEJ("El paciente no tiene cuotas pendientes de pago.");
        }
        List<AplicacionASEJ> aplicacionesASEJ = asignarASEJ(formASEJ.montoASEJ(), pendientesASEJ);
        Map<Long, CuotaASEJ> porIdASEJ = pendientesASEJ.stream().collect(Collectors.toMap(CuotaASEJ::getIdASEJ, Function.identity()));
        Set<Long> planesASEJ = aplicacionesASEJ.stream().map(aASEJ -> porIdASEJ.get(aASEJ.cuotaIdASEJ()).getPlanASEJ().getIdASEJ())
                .collect(Collectors.toSet());

        PagoASEJ pagoASEJ = new PagoASEJ();
        pagoASEJ.setNumeroReciboASEJ(correlativoServiceASEJ.siguienteReciboASEJ());
        pagoASEJ.setPacienteASEJ(pacienteASEJ);
        pagoASEJ.setPlanIdASEJ(planesASEJ.size() == 1 ? planesASEJ.iterator().next() : null);
        pagoASEJ.setFechaASEJ(LocalDateTime.now(relojASEJ).truncatedTo(ChronoUnit.SECONDS));
        pagoASEJ.setMontoASEJ(formASEJ.montoASEJ());
        pagoASEJ.setMedioASEJ(formASEJ.medioASEJ());
        pagoASEJ.setReferenciaASEJ(referenciaASEJ);
        pagoASEJ.setRegistradoPorASEJ(usuarioIdASEJ);
        PagoASEJ guardadoASEJ = pagoRepositoryASEJ.save(pagoASEJ);

        for (AplicacionASEJ aASEJ : aplicacionesASEJ) {
            CuotaASEJ cuotaASEJ = porIdASEJ.get(aASEJ.cuotaIdASEJ());
            BigDecimal montoASEJ = DineroConverterASEJ.desdeCentimosASEJ(aASEJ.montoCentASEJ());
            PagoAplicacionASEJ paASEJ = new PagoAplicacionASEJ();
            paASEJ.setPagoASEJ(guardadoASEJ);
            paASEJ.setCuotaASEJ(cuotaASEJ);
            paASEJ.setMontoASEJ(montoASEJ);
            aplicacionRepositoryASEJ.save(paASEJ);
            cuotaASEJ.setPagadoASEJ(cuotaASEJ.getPagadoASEJ().add(montoASEJ));
            PlanTratamientoASEJ planASEJ = cuotaASEJ.getPlanASEJ();
            if (planASEJ.getEstadoASEJ() == EstadoPlanASEJ.ACEPTADO) {
                planASEJ.setEstadoASEJ(EstadoPlanASEJ.EN_CURSO);
            }
        }
        aplicacionRepositoryASEJ.flush();
        logASEJ.info("Pago {} de S/ {} registrado para {}", guardadoASEJ.getNumeroReciboASEJ(), formASEJ.montoASEJ(), pacienteASEJ.getNumeroHistoriaASEJ());
        auditoriaASEJ.registrarASEJ("PAGO_REGISTRAR", "pago", guardadoASEJ.getIdASEJ(), Map.of("recibo", guardadoASEJ.getNumeroReciboASEJ(),
                "monto", formASEJ.montoASEJ().toPlainString(), "medio", formASEJ.medioASEJ().name()));
        eventosASEJ.publishEvent(new PagoRegistradoASEJ(pacienteASEJ.getIdASEJ(), guardadoASEJ.getIdASEJ(), guardadoASEJ.getNumeroReciboASEJ(), false));
        return obtenerASEJ(guardadoASEJ.getIdASEJ());
    }

    @Transactional
    public PagoDtoASEJ anularASEJ(long pagoIdASEJ, String motivoASEJ) {
        sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.PAGO_ANULAR);
        PagoASEJ pagoASEJ = buscarASEJ(pagoIdASEJ);
        if (pagoASEJ.isAnuladoASEJ()) {
            throw new NegocioExceptionASEJ("El pago " + pagoASEJ.getNumeroReciboASEJ() + " ya está anulado.");
        }
        String mASEJ = ValidadorASEJ.limpioMultilineaASEJ(motivoASEJ);
        new ValidadorASEJ()
                .requeridoASEJ(mASEJ, "el motivo de la anulación")
                .siASEJ(mASEJ != null && mASEJ.length() < LARGO_MINIMO_MOTIVOASEJ,
                        "Describa el motivo con al menos " + LARGO_MINIMO_MOTIVOASEJ + " caracteres.")
                .maximoASEJ(mASEJ, 300, "motivo")
                .lanzarSiHayErroresASEJ();
        for (PagoAplicacionASEJ aASEJ : aplicacionRepositoryASEJ.listarPorPagosASEJ(List.of(pagoASEJ.getIdASEJ()))) {
            CuotaASEJ cuotaASEJ = aASEJ.getCuotaASEJ();
            BigDecimal pagadoASEJ = cuotaASEJ.getPagadoASEJ().subtract(aASEJ.getMontoASEJ());
            if (pagadoASEJ.signum() < 0) {
                throw new IllegalStateException("Lo pagado de la cuota " + cuotaASEJ.getIdASEJ() + " quedaría negativo.");
            }
            cuotaASEJ.setPagadoASEJ(pagadoASEJ);
        }
        pagoASEJ.setAnuladoASEJ(true);
        pagoASEJ.setMotivoAnulacionASEJ(mASEJ);
        pagoASEJ.setAnuladoEnASEJ(LocalDateTime.now(relojASEJ).truncatedTo(ChronoUnit.SECONDS));
        cuotaRepositoryASEJ.flush();
        logASEJ.info("Pago {} anulado: {}", pagoASEJ.getNumeroReciboASEJ(), mASEJ);
        auditoriaASEJ.registrarASEJ("PAGO_ANULAR", "pago", pagoASEJ.getIdASEJ(), Map.of("recibo", pagoASEJ.getNumeroReciboASEJ(), "motivo", mASEJ));
        eventosASEJ.publishEvent(new PagoRegistradoASEJ(pagoASEJ.getPacienteASEJ().getIdASEJ(), pagoASEJ.getIdASEJ(), pagoASEJ.getNumeroReciboASEJ(), true));
        return obtenerASEJ(pagoASEJ.getIdASEJ());
    }

    private List<CuotaASEJ> pendientesASEJ(long pacienteIdASEJ) {
        return cuotaRepositoryASEJ.listarPorPacienteASEJ(pacienteIdASEJ, COBRABLESASEJ).stream()
                .filter(cASEJ -> cASEJ.getPagadoASEJ().compareTo(cASEJ.getMontoASEJ()) < 0)
                .toList();
    }

    private List<AplicacionASEJ> asignarASEJ(BigDecimal montoASEJ, List<CuotaASEJ> pendientesASEJ) {
        List<SaldoCuotaASEJ> saldosASEJ = pendientesASEJ.stream()
                .map(cASEJ -> new SaldoCuotaASEJ(cASEJ.getIdASEJ(), cASEJ.getVencimientoASEJ(), cASEJ.getNumeroASEJ(),
                        DineroConverterASEJ.aCentimosASEJ(cASEJ.getMontoASEJ().subtract(cASEJ.getPagadoASEJ()))))
                .toList();
        try {
            return asignadorASEJ.asignarASEJ(DineroConverterASEJ.aCentimosASEJ(montoASEJ), saldosASEJ);
        } catch (ReglaDominioExceptionASEJ eASEJ) {
            throw new NegocioExceptionASEJ(pendientesASEJ.isEmpty() ? "El paciente no tiene cuotas pendientes de pago."
                    : eASEJ.getMessage());
        }
    }

    private static void validarMontoASEJ(BigDecimal montoASEJ) {
        if (montoASEJ == null) {
            throw new NegocioExceptionASEJ("Ingrese el monto a cobrar.");
        }
        if (montoASEJ.signum() <= 0 || montoASEJ.compareTo(MONTO_MAXIMOASEJ) > 0) {
            throw new NegocioExceptionASEJ("El monto debe ser mayor que cero y menor que 1 000 000.");
        }
        if (montoASEJ.stripTrailingZeros().scale() > 2) {
            throw new NegocioExceptionASEJ("El monto admite como máximo 2 decimales.");
        }
    }

    private PagoASEJ buscarASEJ(long idASEJ) {
        return pagoRepositoryASEJ.findById(idASEJ)
                .orElseThrow(() -> new NegocioExceptionASEJ("El pago ya no existe en la base de datos."));
    }

    private List<PagoDtoASEJ> dtosASEJ(List<PagoASEJ> pagosASEJ) {
        if (pagosASEJ.isEmpty()) {
            return List.of();
        }
        Map<Long, List<AplicacionDtoASEJ>> aplicacionesASEJ = new HashMap<>();
        for (PagoAplicacionASEJ aASEJ : aplicacionRepositoryASEJ.listarPorPagosASEJ(pagosASEJ.stream().map(PagoASEJ::getIdASEJ).toList())) {
            CuotaASEJ cASEJ = aASEJ.getCuotaASEJ();
            aplicacionesASEJ.computeIfAbsent(aASEJ.getPagoASEJ().getIdASEJ(), kASEJ -> new ArrayList<>())
                    .add(new AplicacionDtoASEJ(cASEJ.getIdASEJ(), cASEJ.getPlanASEJ().getIdASEJ(), cASEJ.getNumeroASEJ(), cASEJ.getVencimientoASEJ(), null, aASEJ.getMontoASEJ()));
        }
        Map<Long, String> usuariosASEJ = usuarioRepositoryASEJ.findAll().stream()
                .collect(Collectors.toMap(UsuarioASEJ::getIdASEJ, UsuarioASEJ::getNombreCompletoASEJ));
        return pagosASEJ.stream()
                .map(pASEJ -> new PagoDtoASEJ(pASEJ.getIdASEJ(), pASEJ.getNumeroReciboASEJ(), pASEJ.getPacienteASEJ().getIdASEJ(),
                        pASEJ.getPacienteASEJ().getNombreCompletoASEJ(), pASEJ.getPacienteASEJ().getNumeroHistoriaASEJ(), pASEJ.getFechaASEJ(), pASEJ.getMontoASEJ(),
                        pASEJ.getMedioASEJ(), pASEJ.getReferenciaASEJ(), usuariosASEJ.getOrDefault(pASEJ.getRegistradoPorASEJ(), "—"), pASEJ.isAnuladoASEJ(),
                        pASEJ.getMotivoAnulacionASEJ(), pASEJ.getAnuladoEnASEJ(), aplicacionesASEJ.getOrDefault(pASEJ.getIdASEJ(), List.of())))
                .toList();
    }

    private static CuotaPacienteDtoASEJ dtoASEJ(CuotaASEJ cASEJ, LocalDate hoyASEJ) {
        return new CuotaPacienteDtoASEJ(cASEJ.getIdASEJ(), cASEJ.getPlanASEJ().getIdASEJ(), cASEJ.getNumeroASEJ(), cASEJ.getVencimientoASEJ(), cASEJ.getMontoASEJ(),
                cASEJ.getPagadoASEJ(), EstadoCuotaASEJ.deASEJ(DineroConverterASEJ.aCentimosASEJ(cASEJ.getMontoASEJ()), DineroConverterASEJ.aCentimosASEJ(cASEJ.getPagadoASEJ()),
                cASEJ.getVencimientoASEJ(), hoyASEJ));
    }

    private static BigDecimal sumaASEJ(List<BigDecimal> montosASEJ) {
        return montosASEJ.stream().reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
    }
}
