package com.odontogest.servicio;

import com.odontogest.dominio.comun.ValidacionesASEJ;
import com.odontogest.dominio.odontograma.CalculadoraIndicesASEJ;
import com.odontogest.dominio.odontograma.DenticionASEJ;
import com.odontogest.dominio.odontograma.DiferenciaOdontogramaASEJ;
import com.odontogest.dominio.odontograma.EstadoHallazgoASEJ;
import com.odontogest.dominio.odontograma.EstadoOdontogramaASEJ;
import com.odontogest.dominio.odontograma.HallazgoEventoASEJ;
import com.odontogest.dominio.odontograma.PiezaDentalASEJ;
import com.odontogest.dominio.odontograma.ProyectorOdontogramaASEJ;
import com.odontogest.dominio.odontograma.ReglaColorASEJ;
import com.odontogest.dominio.odontograma.RepresentacionASEJ;
import com.odontogest.dominio.odontograma.SuperficieASEJ;
import com.odontogest.dominio.odontograma.TipoHallazgoDefASEJ;
import com.odontogest.documento.FichaOdontologicaPdfASEJ;
import com.odontogest.evento.OdontogramaCerradoASEJ;
import com.odontogest.excepcion.NegocioExceptionASEJ;
import com.odontogest.modelo.AtencionASEJ;
import com.odontogest.modelo.EstadoAtencionASEJ;
import com.odontogest.modelo.EstadoOdontogramaRegistroASEJ;
import com.odontogest.modelo.HallazgoASEJ;
import com.odontogest.modelo.HallazgoAnulacionASEJ;
import com.odontogest.modelo.HallazgoTipoASEJ;
import com.odontogest.modelo.OdontogramaASEJ;
import com.odontogest.modelo.TipoOdontogramaASEJ;
import com.odontogest.modelo.UsuarioASEJ;
import com.odontogest.persistencia.repositorio.AtencionRepositoryASEJ;
import com.odontogest.persistencia.repositorio.HallazgoAnulacionRepositoryASEJ;
import com.odontogest.persistencia.repositorio.HallazgoRepositoryASEJ;
import com.odontogest.persistencia.repositorio.HallazgoTipoRepositoryASEJ;
import com.odontogest.persistencia.repositorio.OdontogramaRepositoryASEJ;
import com.odontogest.persistencia.repositorio.UsuarioRepositoryASEJ;
import com.odontogest.seguridad.PermisoASEJ;
import com.odontogest.seguridad.SesionUsuarioASEJ;
import com.odontogest.servicio.dto.DiferenciaVistaASEJ;
import com.odontogest.servicio.dto.FichaOdontologicaASEJ;
import com.odontogest.servicio.dto.HallazgoDtoASEJ;
import com.odontogest.servicio.dto.IndicesVistaASEJ;
import com.odontogest.servicio.dto.NuevoHallazgoASEJ;
import com.odontogest.servicio.dto.OdontogramaResumenASEJ;
import com.odontogest.servicio.dto.OdontogramaVistaASEJ;
import com.odontogest.servicio.dto.PacienteDetalleASEJ;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OdontogramaServiceASEJ {

    public static final int LARGO_MINIMO_MOTIVOASEJ = 10;

    private final Clock relojASEJ;
    private final OdontogramaRepositoryASEJ odontogramaRepositoryASEJ;
    private final AuditoriaServiceASEJ auditoriaASEJ;
    private final HallazgoRepositoryASEJ hallazgoRepositoryASEJ;
    private final HallazgoAnulacionRepositoryASEJ anulacionRepositoryASEJ;
    private final HallazgoTipoRepositoryASEJ hallazgoTipoRepositoryASEJ;
    private final AtencionRepositoryASEJ atencionRepositoryASEJ;
    private final UsuarioRepositoryASEJ usuarioRepositoryASEJ;
    private final CatalogoHallazgoServiceASEJ catalogoServiceASEJ;
    private final ConfiguracionServiceASEJ configuracionServiceASEJ;
    private final IntegridadServiceASEJ integridadServiceASEJ;
    private final PacienteServiceASEJ pacienteServiceASEJ;
    private final FichaOdontologicaPdfASEJ fichaPdfASEJ;
    private final SesionUsuarioASEJ sesionUsuarioASEJ;
    private final ApplicationEventPublisher eventosASEJ;

    private final ProyectorOdontogramaASEJ proyectorASEJ = new ProyectorOdontogramaASEJ();
    private final CalculadoraIndicesASEJ calculadoraASEJ = new CalculadoraIndicesASEJ();

    public List<OdontogramaResumenASEJ> listarPorPacienteASEJ(long pacienteIdASEJ) {
        return odontogramaRepositoryASEJ.findByPacienteASEJIdASEJOrderByCreadoEnASEJAscIdASEJAsc(pacienteIdASEJ).stream()
                .map(oASEJ -> new OdontogramaResumenASEJ(oASEJ.getIdASEJ(), oASEJ.getAtencionASEJ().getIdASEJ(), oASEJ.getTipoASEJ(), oASEJ.getEstadoASEJ(),
                        oASEJ.getDenticionASEJ(), oASEJ.getCreadoEnASEJ(), oASEJ.getCerradoEnASEJ(), hallazgoRepositoryASEJ.countByOdontogramaASEJIdASEJ(oASEJ.getIdASEJ())))
                .toList();
    }

    public OdontogramaVistaASEJ obtenerVistaASEJ(long pacienteIdASEJ, LocalDateTime hastaASEJ) {
        DatosASEJ dASEJ = cargarASEJ(pacienteIdASEJ);
        Optional<OdontogramaASEJ> mostradoASEJ = odontogramaMostradoASEJ(dASEJ.odontogramasASEJ(), hastaASEJ);
        List<HallazgoEventoASEJ> eventosASEJ = dASEJ.eventosASEJ(hastaASEJ);
        EstadoOdontogramaASEJ estadoASEJ = proyectorASEJ.proyectarASEJ(eventosASEJ, hastaASEJ);
        Set<Long> vigentesASEJ = estadoASEJ.hallazgosVigentesASEJ().stream().map(HallazgoEventoASEJ::idASEJ).collect(Collectors.toSet());
        IndicesVistaASEJ indicesASEJ = indicesASEJ(estadoASEJ);

        List<HallazgoDtoASEJ> historialASEJ = dASEJ.hallazgosASEJ().stream()
                .filter(hASEJ -> DatosASEJ.registradoHastaASEJ(hASEJ, hastaASEJ))
                .map(hASEJ -> dtoASEJ(hASEJ, dASEJ, hastaASEJ, vigentesASEJ))
                .toList();

        if (mostradoASEJ.isEmpty()) {
            int edadASEJ = pacienteServiceASEJ.obtenerASEJ(pacienteIdASEJ).edadASEJ();
            return new OdontogramaVistaASEJ(pacienteIdASEJ, null, null, null, null, DenticionASEJ.sugeridaASEJ(edadASEJ), null, null,
                    null, null, null, false, hastaASEJ, estadoASEJ, historialASEJ, indicesASEJ);
        }
        OdontogramaASEJ oASEJ = mostradoASEJ.get();
        return new OdontogramaVistaASEJ(pacienteIdASEJ, oASEJ.getIdASEJ(), oASEJ.getAtencionASEJ().getIdASEJ(), oASEJ.getTipoASEJ(), oASEJ.getEstadoASEJ(),
                oASEJ.getDenticionASEJ(), oASEJ.getEspecificacionesASEJ(), oASEJ.getObservacionesASEJ(), oASEJ.getCreadoEnASEJ(), oASEJ.getCerradoEnASEJ(),
                oASEJ.getHashCierreASEJ(), oASEJ.getAtencionASEJ().getEstadoASEJ() == EstadoAtencionASEJ.ABIERTA, hastaASEJ, estadoASEJ, historialASEJ, indicesASEJ);
    }

    public DiferenciaVistaASEJ compararASEJ(long pacienteIdASEJ, LocalDateTime baseASEJ, LocalDateTime objetivoASEJ) {
        DatosASEJ dASEJ = cargarASEJ(pacienteIdASEJ);
        EstadoOdontogramaASEJ bASEJ = proyectorASEJ.proyectarASEJ(dASEJ.eventosASEJ(baseASEJ), baseASEJ);
        EstadoOdontogramaASEJ oASEJ = proyectorASEJ.proyectarASEJ(dASEJ.eventosASEJ(objetivoASEJ), objetivoASEJ);
        return new DiferenciaVistaASEJ(baseASEJ, objetivoASEJ, DiferenciaOdontogramaASEJ.entreASEJ(bASEJ, oASEJ));
    }

    public IndicesVistaASEJ calcularIndicesASEJ(long pacienteIdASEJ) {
        DatosASEJ dASEJ = cargarASEJ(pacienteIdASEJ);
        return indicesASEJ(proyectorASEJ.proyectarASEJ(dASEJ.eventosASEJ(null), null));
    }

    record EstadoFirmadoASEJ(long odontogramaIdASEJ, EstadoOdontogramaASEJ estadoASEJ) {
    }

    Optional<EstadoFirmadoASEJ> estadoFirmadoASEJ(long pacienteIdASEJ) {
        DatosASEJ dASEJ = cargarASEJ(pacienteIdASEJ);
        Optional<OdontogramaASEJ> ultimoASEJ = dASEJ.odontogramasASEJ().stream().filter(oASEJ -> !oASEJ.esBorradorASEJ())
                .max(Comparator.comparing(OdontogramaASEJ::getCerradoEnASEJ).thenComparing(OdontogramaASEJ::getIdASEJ));
        if (ultimoASEJ.isEmpty()) {
            return Optional.empty();
        }
        List<HallazgoEventoASEJ> firmadosASEJ = dASEJ.hallazgosASEJ().stream()
                .filter(hASEJ -> !hASEJ.getOdontogramaASEJ().esBorradorASEJ())
                .map(hASEJ -> eventoASEJ(hASEJ, dASEJ.tiposASEJ().get(hASEJ.getHallazgoTipoASEJ().getIdASEJ()), dASEJ.anuladoHastaASEJ(hASEJ.getIdASEJ(), null)))
                .toList();
        return Optional.of(new EstadoFirmadoASEJ(ultimoASEJ.get().getIdASEJ(), proyectorASEJ.proyectarASEJ(firmadosASEJ, null)));
    }

    Optional<IndicesVistaASEJ> indicesFirmadosASEJ(long pacienteIdASEJ) {
        return estadoFirmadoASEJ(pacienteIdASEJ).map(eASEJ -> indicesASEJ(eASEJ.estadoASEJ()));
    }

    public byte[] pdfFichaASEJ(long pacienteIdASEJ, LocalDateTime hastaASEJ, byte[] imagenPngASEJ) {
        OdontogramaVistaASEJ vistaASEJ = obtenerVistaASEJ(pacienteIdASEJ, hastaASEJ);
        if (vistaASEJ.odontogramaIdASEJ() == null) {
            throw new NegocioExceptionASEJ("El paciente aún no tiene odontograma.");
        }
        PacienteDetalleASEJ pacienteASEJ = pacienteServiceASEJ.obtenerASEJ(pacienteIdASEJ);
        AtencionASEJ atencionASEJ = atencionRepositoryASEJ.findById(vistaASEJ.atencionIdASEJ()).orElseThrow();
        return fichaPdfASEJ.generarASEJ(new FichaOdontologicaASEJ(configuracionServiceASEJ.datosClinicaASEJ(), pacienteASEJ, vistaASEJ,
                atencionASEJ.getOdontologoASEJ().getNombreCompletoASEJ(), atencionASEJ.getOdontologoASEJ().getCopASEJ(), imagenPngASEJ));
    }

    @Transactional
    public long abrirBorradorASEJ(long atencionIdASEJ, DenticionASEJ denticionASEJ) {
        sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.ODONTOGRAMA_EDITAR);
        AtencionASEJ atencionASEJ = atencionRepositoryASEJ.findById(atencionIdASEJ)
                .orElseThrow(() -> new NegocioExceptionASEJ("La atención ya no existe en la base de datos."));
        if (atencionASEJ.getEstadoASEJ() != EstadoAtencionASEJ.ABIERTA) {
            throw new NegocioExceptionASEJ("La atención está cerrada: abra una nueva atención para registrar una evolución.");
        }
        long pacienteIdASEJ = atencionASEJ.getPacienteASEJ().getIdASEJ();
        if (odontogramaRepositoryASEJ.findFirstByPacienteASEJIdASEJAndEstadoASEJ(pacienteIdASEJ, EstadoOdontogramaRegistroASEJ.BORRADOR).isPresent()) {
            throw new NegocioExceptionASEJ("El paciente ya tiene un odontograma en borrador: ciérrelo antes de abrir otro.");
        }
        OdontogramaASEJ oASEJ = new OdontogramaASEJ();
        oASEJ.setPacienteASEJ(atencionASEJ.getPacienteASEJ());
        oASEJ.setAtencionASEJ(atencionASEJ);
        oASEJ.setTipoASEJ(odontogramaRepositoryASEJ.existsByPacienteASEJIdASEJAndTipoASEJ(pacienteIdASEJ, TipoOdontogramaASEJ.INICIAL)
                ? TipoOdontogramaASEJ.EVOLUCION : TipoOdontogramaASEJ.INICIAL);
        oASEJ.setDenticionASEJ(denticionASEJ != null ? denticionASEJ
                : DenticionASEJ.sugeridaASEJ(ValidacionesASEJ.edadASEJ(atencionASEJ.getPacienteASEJ().getFechaNacimientoASEJ(), LocalDate.now(relojASEJ))));
        oASEJ.setEstadoASEJ(EstadoOdontogramaRegistroASEJ.BORRADOR);
        oASEJ.setCreadoEnASEJ(ahoraASEJ());
        return odontogramaRepositoryASEJ.save(oASEJ).getIdASEJ();
    }

    @Transactional
    public OdontogramaVistaASEJ cambiarDenticionASEJ(long odontogramaIdASEJ, DenticionASEJ denticionASEJ) {
        sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.ODONTOGRAMA_EDITAR);
        OdontogramaASEJ oASEJ = borradorEditableASEJ(odontogramaIdASEJ);
        boolean incompatiblesASEJ = hallazgoRepositoryASEJ.listarPorOdontogramaASEJ(oASEJ.getIdASEJ()).stream()
                .anyMatch(hASEJ -> !denticionASEJ.admiteASEJ(PiezaDentalASEJ.deASEJ(hASEJ.getPiezaFdiASEJ()))
                        || (hASEJ.getPiezaFdiFinASEJ() != null && !denticionASEJ.admiteASEJ(PiezaDentalASEJ.deASEJ(hASEJ.getPiezaFdiFinASEJ()))));
        if (incompatiblesASEJ) {
            throw new NegocioExceptionASEJ("Hay hallazgos registrados en piezas que no pertenecen a la dentición elegida.");
        }
        oASEJ.setDenticionASEJ(denticionASEJ);
        return obtenerVistaASEJ(oASEJ.getPacienteASEJ().getIdASEJ(), null);
    }

    @Transactional
    public OdontogramaVistaASEJ registrarHallazgoASEJ(NuevoHallazgoASEJ cmdASEJ) {
        long usuarioIdASEJ = sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.ODONTOGRAMA_EDITAR).idASEJ();
        OdontogramaASEJ oASEJ = borradorEditableASEJ(cmdASEJ.odontogramaIdASEJ());
        HallazgoTipoASEJ tipoASEJ = hallazgoTipoRepositoryASEJ.findById(cmdASEJ.hallazgoTipoIdASEJ())
                .orElseThrow(() -> new NegocioExceptionASEJ("El tipo de hallazgo ya no existe en el catálogo."));
        if (!tipoASEJ.isActivoASEJ()) {
            throw new NegocioExceptionASEJ("El hallazgo " + tipoASEJ.getNombreASEJ() + " está inactivo en el catálogo.");
        }
        long pacienteIdASEJ = oASEJ.getPacienteASEJ().getIdASEJ();
        ValidadoASEJ vASEJ = validarASEJ(cmdASEJ, tipoASEJ, oASEJ.getDenticionASEJ(), estadoActualASEJ(pacienteIdASEJ));

        auditoriaASEJ.registrarASEJ("HALLAZGO_CREAR", "odontograma", oASEJ.getIdASEJ(), Map.of("tipo", tipoASEJ.getCodigoASEJ(), "pieza", cmdASEJ.piezaFdiASEJ()));
        hallazgoRepositoryASEJ.save(HallazgoASEJ.registrarASEJ(oASEJ, tipoASEJ, cmdASEJ.piezaFdiASEJ(), vASEJ.piezaFinASEJ(), vASEJ.superficiesASEJ(), vASEJ.estadoASEJ(),
                vASEJ.siglaASEJ(), vASEJ.notaASEJ(), usuarioIdASEJ, ahoraASEJ()));
        return obtenerVistaASEJ(pacienteIdASEJ, null);
    }

    @Transactional
    public OdontogramaVistaASEJ eliminarHallazgoBorradorASEJ(long hallazgoIdASEJ) {
        sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.ODONTOGRAMA_EDITAR);
        HallazgoASEJ hASEJ = buscarHallazgoASEJ(hallazgoIdASEJ);
        OdontogramaASEJ oASEJ = hASEJ.getOdontogramaASEJ();
        if (!oASEJ.esBorradorASEJ()) {
            throw new NegocioExceptionASEJ("El odontograma ya está cerrado: el hallazgo no se puede eliminar, solo anular.");
        }
        borradorEditableASEJ(oASEJ.getIdASEJ());
        hallazgoRepositoryASEJ.delete(hASEJ);
        logASEJ.info("Hallazgo {} eliminado del borrador {}", hallazgoIdASEJ, oASEJ.getIdASEJ());
        auditoriaASEJ.registrarASEJ("HALLAZGO_ELIMINAR", "hallazgo", hallazgoIdASEJ, Map.of("odontograma", oASEJ.getIdASEJ()));
        return obtenerVistaASEJ(oASEJ.getPacienteASEJ().getIdASEJ(), null);
    }

    @Transactional
    public OdontogramaVistaASEJ anularHallazgoASEJ(long hallazgoIdASEJ, String motivoASEJ) {
        long usuarioIdASEJ = sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.ODONTOGRAMA_EDITAR).idASEJ();
        HallazgoASEJ hASEJ = buscarHallazgoASEJ(hallazgoIdASEJ);
        if (hASEJ.getOdontogramaASEJ().esBorradorASEJ()) {
            throw new NegocioExceptionASEJ("El odontograma está en borrador: elimine el hallazgo en lugar de anularlo.");
        }
        if (anulacionRepositoryASEJ.existsByHallazgoASEJIdASEJ(hallazgoIdASEJ)) {
            throw new NegocioExceptionASEJ("El hallazgo ya está anulado.");
        }
        String mASEJ = ValidadorASEJ.limpioASEJ(motivoASEJ);
        if (mASEJ == null || mASEJ.length() < LARGO_MINIMO_MOTIVOASEJ) {
            throw new NegocioExceptionASEJ("Indique el motivo de la anulación (al menos " + LARGO_MINIMO_MOTIVOASEJ + " caracteres).");
        }
        anulacionRepositoryASEJ.save(HallazgoAnulacionASEJ.deASEJ(hASEJ, mASEJ, usuarioIdASEJ, ahoraASEJ()));
        logASEJ.info("Hallazgo {} anulado: {}", hallazgoIdASEJ, mASEJ);
        auditoriaASEJ.registrarASEJ("HALLAZGO_ANULAR", "hallazgo", hallazgoIdASEJ, Map.of("motivo", mASEJ));
        return obtenerVistaASEJ(hASEJ.getOdontogramaASEJ().getPacienteASEJ().getIdASEJ(), null);
    }

    @Transactional
    public OdontogramaVistaASEJ actualizarEspecificacionesASEJ(long odontogramaIdASEJ, String especificacionesASEJ, String observacionesASEJ) {
        sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.ODONTOGRAMA_EDITAR);
        OdontogramaASEJ oASEJ = borradorEditableASEJ(odontogramaIdASEJ);
        String espASEJ = ValidadorASEJ.limpioMultilineaASEJ(especificacionesASEJ);
        String obsASEJ = ValidadorASEJ.limpioMultilineaASEJ(observacionesASEJ);
        new ValidadorASEJ().maximoASEJ(espASEJ, 2000, "especificaciones").maximoASEJ(obsASEJ, 2000, "observaciones").lanzarSiHayErroresASEJ();
        oASEJ.setEspecificacionesASEJ(espASEJ);
        oASEJ.setObservacionesASEJ(obsASEJ);
        return obtenerVistaASEJ(oASEJ.getPacienteASEJ().getIdASEJ(), null);
    }

    @Transactional
    public OdontogramaVistaASEJ cerrarASEJ(long odontogramaIdASEJ) {
        long usuarioIdASEJ = sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.ODONTOGRAMA_EDITAR).idASEJ();
        OdontogramaASEJ oASEJ = borradorEditableASEJ(odontogramaIdASEJ);
        long pacienteIdASEJ = oASEJ.getPacienteASEJ().getIdASEJ();
        String anteriorASEJ = integridadServiceASEJ.hashAnteriorASEJ(pacienteIdASEJ);
        oASEJ.setCerradoEnASEJ(ahoraASEJ());
        oASEJ.setCerradoPorASEJ(usuarioIdASEJ);
        oASEJ.setHashAnteriorASEJ(anteriorASEJ);
        oASEJ.setHashCierreASEJ(integridadServiceASEJ.calcularHashASEJ(oASEJ, anteriorASEJ));
        oASEJ.setEstadoASEJ(EstadoOdontogramaRegistroASEJ.CERRADO);
        odontogramaRepositoryASEJ.save(oASEJ);
        logASEJ.info("Odontograma {} cerrado con hash {}", oASEJ.getIdASEJ(), oASEJ.getHashCierreASEJ());
        auditoriaASEJ.registrarASEJ("ODONTOGRAMA_CERRAR", "odontograma", oASEJ.getIdASEJ(), Map.of("hash", oASEJ.getHashCierreASEJ()));
        eventosASEJ.publishEvent(new OdontogramaCerradoASEJ(pacienteIdASEJ, oASEJ.getIdASEJ(), oASEJ.getHashCierreASEJ()));
        return obtenerVistaASEJ(pacienteIdASEJ, null);
    }

    private record ValidadoASEJ(Integer piezaFinASEJ, Set<SuperficieASEJ> superficiesASEJ, EstadoHallazgoASEJ estadoASEJ,
                            String siglaASEJ, String notaASEJ) {
    }

    private ValidadoASEJ validarASEJ(NuevoHallazgoASEJ cmdASEJ, HallazgoTipoASEJ tipoASEJ, DenticionASEJ denticionASEJ, EstadoOdontogramaASEJ estadoASEJ) {
        if (!PiezaDentalASEJ.esValidaASEJ(cmdASEJ.piezaFdiASEJ())) {
            throw new NegocioExceptionASEJ("La pieza " + cmdASEJ.piezaFdiASEJ() + " no existe en el sistema FDI.");
        }
        PiezaDentalASEJ piezaASEJ = PiezaDentalASEJ.deASEJ(cmdASEJ.piezaFdiASEJ());
        ValidadorASEJ vASEJ = new ValidadorASEJ()
                .siASEJ(!denticionASEJ.admiteASEJ(piezaASEJ), "La pieza " + piezaASEJ + " no pertenece a la dentición del odontograma.");
        RepresentacionASEJ rASEJ = tipoASEJ.getRepresentacionASEJ();
        Integer piezaFinASEJ = null;
        Set<SuperficieASEJ> superficiesASEJ = EnumSet.noneOf(SuperficieASEJ.class);
        switch (rASEJ) {
            case SUPERFICIE -> {
                vASEJ.siASEJ(cmdASEJ.superficiesASEJ().isEmpty(), "Seleccione al menos una superficie de la pieza " + piezaASEJ + ".");
                vASEJ.siASEJ(estadoASEJ.superficiesOcultasASEJ(piezaASEJ.fdiASEJ()),
                        "La pieza " + piezaASEJ + " figura como ausente: no se pueden registrar hallazgos en sus superficies.");
                superficiesASEJ = cmdASEJ.superficiesASEJ().isEmpty() ? superficiesASEJ : EnumSet.copyOf(cmdASEJ.superficiesASEJ());
            }
            case PIEZA -> vASEJ.siASEJ(!cmdASEJ.superficiesASEJ().isEmpty(), "El hallazgo " + tipoASEJ.getNombreASEJ() + " se registra por pieza, sin superficies.");
            case ENTRE_PIEZAS, RANGO_PIEZAS -> {
                if (cmdASEJ.piezaFdiFinASEJ() == null || !PiezaDentalASEJ.esValidaASEJ(cmdASEJ.piezaFdiFinASEJ())) {
                    vASEJ.siASEJ(true, "Seleccione la segunda pieza del hallazgo " + tipoASEJ.getNombreASEJ() + ".");
                } else {
                    PiezaDentalASEJ finASEJ = PiezaDentalASEJ.deASEJ(cmdASEJ.piezaFdiFinASEJ());
                    piezaFinASEJ = finASEJ.fdiASEJ();
                    vASEJ.siASEJ(finASEJ.equals(piezaASEJ), "Seleccione dos piezas distintas.")
                            .siASEJ(!denticionASEJ.admiteASEJ(finASEJ), "La pieza " + finASEJ + " no pertenece a la dentición del odontograma.")
                            .siASEJ(!piezaASEJ.mismaFilaASEJ(finASEJ), "Las piezas " + piezaASEJ + " y " + finASEJ + " deben estar en la misma arcada y fila.")
                            .siASEJ(rASEJ == RepresentacionASEJ.ENTRE_PIEZAS && piezaASEJ.mismaFilaASEJ(finASEJ) && !piezaASEJ.esContiguaAASEJ(finASEJ),
                                    "El hallazgo " + tipoASEJ.getNombreASEJ() + " se registra entre dos piezas contiguas.");
                }
                vASEJ.siASEJ(!cmdASEJ.superficiesASEJ().isEmpty(), "El hallazgo " + tipoASEJ.getNombreASEJ() + " no se registra por superficie.");
            }
        }
        EstadoHallazgoASEJ estASEJ = null;
        if (tipoASEJ.getReglaColorASEJ() == ReglaColorASEJ.POR_ESTADO) {
            vASEJ.requeridoASEJ(cmdASEJ.estadoASEJ(), "el estado (bueno o malo)");
            estASEJ = cmdASEJ.estadoASEJ();
        }
        List<String> opcionesASEJ = CatalogoHallazgoServiceASEJ.opcionesASEJ(tipoASEJ.getSiglasOpcionesASEJ());
        String siglaASEJ = null;
        if (!opcionesASEJ.isEmpty()) {
            siglaASEJ = cmdASEJ.siglaASEJ() == null ? null : cmdASEJ.siglaASEJ().trim().toUpperCase(Locale.ROOT);
            vASEJ.siASEJ(siglaASEJ == null || !opcionesASEJ.contains(siglaASEJ),
                    "Elija la sigla del hallazgo " + tipoASEJ.getNombreASEJ() + ": " + String.join(", ", opcionesASEJ) + ".");
        }
        String notaASEJ = ValidadorASEJ.limpioASEJ(cmdASEJ.notaASEJ());
        vASEJ.maximoASEJ(notaASEJ, 200, "nota");
        vASEJ.lanzarSiHayErroresASEJ();
        return new ValidadoASEJ(piezaFinASEJ, superficiesASEJ, estASEJ, siglaASEJ, notaASEJ);
    }

    private record DatosASEJ(List<OdontogramaASEJ> odontogramasASEJ, List<HallazgoASEJ> hallazgosASEJ, Map<Long, HallazgoAnulacionASEJ> anulacionesASEJ,
                         Map<Long, TipoHallazgoDefASEJ> tiposASEJ, Map<Long, String> usuariosASEJ) {

        boolean anuladoHastaASEJ(long hallazgoIdASEJ, LocalDateTime hastaASEJ) {
            HallazgoAnulacionASEJ aASEJ = anulacionesASEJ.get(hallazgoIdASEJ);
            return aASEJ != null && (hastaASEJ == null || !aASEJ.getAnuladoEnASEJ().isAfter(hastaASEJ));
        }

        List<HallazgoEventoASEJ> eventosASEJ(LocalDateTime hastaASEJ) {
            return hallazgosASEJ.stream()
                    .filter(hASEJ -> registradoHastaASEJ(hASEJ, hastaASEJ))
                    .map(hASEJ -> eventoASEJ(hASEJ, tiposASEJ.get(hASEJ.getHallazgoTipoASEJ().getIdASEJ()), anuladoHastaASEJ(hASEJ.getIdASEJ(), hastaASEJ)))
                    .toList();
        }

        static boolean registradoHastaASEJ(HallazgoASEJ hASEJ, LocalDateTime hastaASEJ) {
            if (hastaASEJ == null) {
                return true;
            }
            OdontogramaASEJ oASEJ = hASEJ.getOdontogramaASEJ();
            return !oASEJ.esBorradorASEJ() && !oASEJ.getCerradoEnASEJ().isAfter(hastaASEJ);
        }
    }

    private DatosASEJ cargarASEJ(long pacienteIdASEJ) {
        List<HallazgoASEJ> hallazgosASEJ = hallazgoRepositoryASEJ.listarPorPacienteASEJ(pacienteIdASEJ);
        Map<Long, HallazgoAnulacionASEJ> anulacionesASEJ = anulacionRepositoryASEJ.listarPorPacienteASEJ(pacienteIdASEJ).stream()
                .collect(Collectors.toMap(aASEJ -> aASEJ.getHallazgoASEJ().getIdASEJ(), Function.identity()));
        Map<Long, TipoHallazgoDefASEJ> tiposASEJ = catalogoServiceASEJ.definicionesASEJ().stream()
                .collect(Collectors.toMap(TipoHallazgoDefASEJ::idASEJ, Function.identity()));
        Map<Long, String> usuariosASEJ = new HashMap<>();
        for (UsuarioASEJ uASEJ : usuarioRepositoryASEJ.findAll()) {
            usuariosASEJ.put(uASEJ.getIdASEJ(), uASEJ.getNombreCompletoASEJ());
        }
        return new DatosASEJ(odontogramaRepositoryASEJ.findByPacienteASEJIdASEJOrderByCreadoEnASEJAscIdASEJAsc(pacienteIdASEJ), hallazgosASEJ,
                anulacionesASEJ, tiposASEJ, usuariosASEJ);
    }

    private static Optional<OdontogramaASEJ> odontogramaMostradoASEJ(List<OdontogramaASEJ> odontogramasASEJ, LocalDateTime hastaASEJ) {
        if (hastaASEJ == null) {
            Optional<OdontogramaASEJ> borradorASEJ = odontogramasASEJ.stream().filter(OdontogramaASEJ::esBorradorASEJ).findFirst();
            if (borradorASEJ.isPresent()) {
                return borradorASEJ;
            }
        }
        return odontogramasASEJ.stream()
                .filter(oASEJ -> !oASEJ.esBorradorASEJ() && (hastaASEJ == null || !oASEJ.getCerradoEnASEJ().isAfter(hastaASEJ)))
                .max(Comparator.comparing(OdontogramaASEJ::getCerradoEnASEJ).thenComparing(OdontogramaASEJ::getIdASEJ));
    }

    private EstadoOdontogramaASEJ estadoActualASEJ(long pacienteIdASEJ) {
        return proyectorASEJ.proyectarASEJ(cargarASEJ(pacienteIdASEJ).eventosASEJ(null), null);
    }

    private IndicesVistaASEJ indicesASEJ(EstadoOdontogramaASEJ estadoASEJ) {
        boolean excluirASEJ = configuracionServiceASEJ.booleanoASEJ("odontograma.excluir_terceros_molares_cpod");
        return new IndicesVistaASEJ(calculadoraASEJ.cpodASEJ(estadoASEJ, excluirASEJ), calculadoraASEJ.ceodASEJ(estadoASEJ), excluirASEJ);
    }

    private OdontogramaASEJ borradorEditableASEJ(long odontogramaIdASEJ) {
        OdontogramaASEJ oASEJ = odontogramaRepositoryASEJ.findById(odontogramaIdASEJ)
                .orElseThrow(() -> new NegocioExceptionASEJ("El odontograma ya no existe en la base de datos."));
        if (!oASEJ.esBorradorASEJ()) {
            throw new NegocioExceptionASEJ("El odontograma está cerrado y firmado: no se puede modificar.");
        }
        if (oASEJ.getAtencionASEJ().getEstadoASEJ() != EstadoAtencionASEJ.ABIERTA) {
            throw new NegocioExceptionASEJ("La atención del odontograma está cerrada.");
        }
        return oASEJ;
    }

    private HallazgoASEJ buscarHallazgoASEJ(long idASEJ) {
        return hallazgoRepositoryASEJ.findById(idASEJ)
                .orElseThrow(() -> new NegocioExceptionASEJ("El hallazgo ya no existe en la base de datos."));
    }

    static HallazgoEventoASEJ eventoASEJ(HallazgoASEJ hASEJ, TipoHallazgoDefASEJ tipoASEJ, boolean anuladoASEJ) {
        Set<SuperficieASEJ> supASEJ = hASEJ.getSuperficiesASEJ();
        return new HallazgoEventoASEJ(hASEJ.getIdASEJ(), hASEJ.getOdontogramaASEJ().getIdASEJ(), tipoASEJ, PiezaDentalASEJ.deASEJ(hASEJ.getPiezaFdiASEJ()),
                hASEJ.getPiezaFdiFinASEJ() == null ? null : PiezaDentalASEJ.deASEJ(hASEJ.getPiezaFdiFinASEJ()),
                supASEJ.isEmpty() ? EnumSet.noneOf(SuperficieASEJ.class) : EnumSet.copyOf(supASEJ), hASEJ.getEstadoASEJ(),
                hASEJ.getSiglaASEJ(), hASEJ.getNotaASEJ(), hASEJ.getRegistradoEnASEJ(), hASEJ.getRegistradoPorASEJ(), anuladoASEJ);
    }

    private static HallazgoDtoASEJ dtoASEJ(HallazgoASEJ hASEJ, DatosASEJ dASEJ, LocalDateTime hastaASEJ, Set<Long> vigentesASEJ) {
        TipoHallazgoDefASEJ tipoASEJ = dASEJ.tiposASEJ().get(hASEJ.getHallazgoTipoASEJ().getIdASEJ());
        HallazgoEventoASEJ eASEJ = eventoASEJ(hASEJ, tipoASEJ, false);
        boolean anuladoASEJ = dASEJ.anuladoHastaASEJ(hASEJ.getIdASEJ(), hastaASEJ);
        HallazgoAnulacionASEJ aASEJ = anuladoASEJ ? dASEJ.anulacionesASEJ().get(hASEJ.getIdASEJ()) : null;
        boolean eliminableASEJ = hastaASEJ == null && hASEJ.getOdontogramaASEJ().esBorradorASEJ()
                && hASEJ.getOdontogramaASEJ().getAtencionASEJ().getEstadoASEJ() == EstadoAtencionASEJ.ABIERTA;
        return new HallazgoDtoASEJ(hASEJ.getIdASEJ(), hASEJ.getOdontogramaASEJ().getIdASEJ(), tipoASEJ.idASEJ(), tipoASEJ.codigoASEJ(), tipoASEJ.nombreASEJ(),
                tipoASEJ.representacionASEJ(), hASEJ.getPiezaFdiASEJ(), hASEJ.getPiezaFdiFinASEJ(), hASEJ.getSuperficiesASEJ(), hASEJ.getEstadoASEJ(),
                hASEJ.getSiglaASEJ() != null ? hASEJ.getSiglaASEJ() : tipoASEJ.siglaASEJ(), hASEJ.getNotaASEJ(), hASEJ.getRegistradoEnASEJ(),
                dASEJ.usuariosASEJ().getOrDefault(hASEJ.getRegistradoPorASEJ(), "—"), eASEJ.colorASEJ(), anuladoASEJ,
                aASEJ == null ? null : aASEJ.getMotivoASEJ(), aASEJ == null ? null : aASEJ.getAnuladoEnASEJ(), vigentesASEJ.contains(hASEJ.getIdASEJ()), eliminableASEJ);
    }

    private LocalDateTime ahoraASEJ() {
        return LocalDateTime.now(relojASEJ).truncatedTo(ChronoUnit.SECONDS);
    }
}
