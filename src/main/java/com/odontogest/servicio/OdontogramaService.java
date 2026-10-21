/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio;

import com.odontogest.dominio.comun.Validaciones;
import com.odontogest.dominio.odontograma.CalculadoraIndices;
import com.odontogest.dominio.odontograma.Denticion;
import com.odontogest.dominio.odontograma.DiferenciaOdontograma;
import com.odontogest.dominio.odontograma.EstadoHallazgo;
import com.odontogest.dominio.odontograma.EstadoOdontograma;
import com.odontogest.dominio.odontograma.HallazgoEvento;
import com.odontogest.dominio.odontograma.PiezaDental;
import com.odontogest.dominio.odontograma.ProyectorOdontograma;
import com.odontogest.dominio.odontograma.ReglaColor;
import com.odontogest.dominio.odontograma.Representacion;
import com.odontogest.dominio.odontograma.Superficie;
import com.odontogest.dominio.odontograma.TipoHallazgoDef;
import com.odontogest.documento.FichaOdontologicaPdf;
import com.odontogest.evento.OdontogramaCerrado;
import com.odontogest.excepcion.NegocioException;
import com.odontogest.modelo.Atencion;
import com.odontogest.modelo.EstadoAtencion;
import com.odontogest.modelo.EstadoOdontogramaRegistro;
import com.odontogest.modelo.Hallazgo;
import com.odontogest.modelo.HallazgoAnulacion;
import com.odontogest.modelo.HallazgoTipo;
import com.odontogest.modelo.Odontograma;
import com.odontogest.modelo.TipoOdontograma;
import com.odontogest.modelo.Usuario;
import com.odontogest.persistencia.repositorio.AtencionRepository;
import com.odontogest.persistencia.repositorio.HallazgoAnulacionRepository;
import com.odontogest.persistencia.repositorio.HallazgoRepository;
import com.odontogest.persistencia.repositorio.HallazgoTipoRepository;
import com.odontogest.persistencia.repositorio.OdontogramaRepository;
import com.odontogest.persistencia.repositorio.UsuarioRepository;
import com.odontogest.seguridad.Permiso;
import com.odontogest.seguridad.SesionUsuario;
import com.odontogest.servicio.dto.DiferenciaVista;
import com.odontogest.servicio.dto.FichaOdontologica;
import com.odontogest.servicio.dto.HallazgoDto;
import com.odontogest.servicio.dto.IndicesVista;
import com.odontogest.servicio.dto.NuevoHallazgo;
import com.odontogest.servicio.dto.OdontogramaResumen;
import com.odontogest.servicio.dto.OdontogramaVista;
import com.odontogest.servicio.dto.PacienteDetalle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

/**
 * Odontograma basado en eventos (RF-ODO-01 a RF-ODO-10).
 * <p>
 * Ciclo de vida: cada atención abre un odontograma en BORRADOR (INICIAL si el paciente no tiene uno, si no
 * EVOLUCION; como máximo un borrador por paciente). En el borrador se registran y eliminan hallazgos; al cerrarlo
 * queda firmado con un hash SHA-256 encadenado y solo admite anulaciones con motivo. El estado de la boca en una
 * fecha es la proyección de todos los hallazgos del paciente registrados hasta esa fecha.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OdontogramaService {

    public static final int LARGO_MINIMO_MOTIVO = 10;

    private final OdontogramaRepository odontogramaRepository;
    private final AuditoriaService auditoria;
    private final HallazgoRepository hallazgoRepository;
    private final HallazgoAnulacionRepository anulacionRepository;
    private final HallazgoTipoRepository hallazgoTipoRepository;
    private final AtencionRepository atencionRepository;
    private final UsuarioRepository usuarioRepository;
    private final CatalogoHallazgoService catalogoService;
    private final ConfiguracionService configuracionService;
    private final IntegridadService integridadService;
    private final PacienteService pacienteService;
    private final FichaOdontologicaPdf fichaPdf;
    private final SesionUsuario sesionUsuario;
    private final ApplicationEventPublisher eventos;

    private final ProyectorOdontograma proyector = new ProyectorOdontograma();
    private final CalculadoraIndices calculadora = new CalculadoraIndices();

    // ------------------------------------------------------------------ consultas

    /** Odontogramas del paciente en orden de creación (línea de tiempo). */
    public List<OdontogramaResumen> listarPorPaciente(long pacienteId) {
        return odontogramaRepository.findByPacienteIdOrderByCreadoEnAscIdAsc(pacienteId).stream()
                .map(o -> new OdontogramaResumen(o.getId(), o.getAtencion().getId(), o.getTipo(), o.getEstado(),
                        o.getDenticion(), o.getCreadoEn(), o.getCerradoEn(), hallazgoRepository.countByOdontogramaId(o.getId())))
                .toList();
    }

    /**
     * Estado de la boca en un momento.
     *
     * @param hasta {@code null} = ahora (incluye el borrador); si no, el odontograma mostrado es el último cerrado
     *              hasta ese momento y una anulación solo se aplica si ocurrió antes de él
     */
    public OdontogramaVista obtenerVista(long pacienteId, LocalDateTime hasta) {
        Datos d = cargar(pacienteId);
        Optional<Odontograma> mostrado = odontogramaMostrado(d.odontogramas(), hasta);
        List<HallazgoEvento> eventos = d.eventos(hasta);
        EstadoOdontograma estado = proyector.proyectar(eventos, hasta);
        Set<Long> vigentes = estado.hallazgosVigentes().stream().map(HallazgoEvento::id).collect(Collectors.toSet());
        IndicesVista indices = indices(estado);

        List<HallazgoDto> historial = d.hallazgos().stream()
                .filter(h -> Datos.registradoHasta(h, hasta))
                .map(h -> dto(h, d, hasta, vigentes))
                .toList();

        if (mostrado.isEmpty()) {
            int edad = pacienteService.obtener(pacienteId).edad();
            return new OdontogramaVista(pacienteId, null, null, null, null, Denticion.sugerida(edad), null, null,
                    null, null, null, false, hasta, estado, historial, indices);
        }
        Odontograma o = mostrado.get();
        return new OdontogramaVista(pacienteId, o.getId(), o.getAtencion().getId(), o.getTipo(), o.getEstado(),
                o.getDenticion(), o.getEspecificaciones(), o.getObservaciones(), o.getCreadoEn(), o.getCerradoEn(),
                o.getHashCierre(), o.getAtencion().getEstado() == EstadoAtencion.ABIERTA, hasta, estado, historial, indices);
    }

    /** Diferencias entre dos momentos ({@code objetivo == null} = ahora), p. ej. inicial frente a actual (RF-ODO-07). */
    public DiferenciaVista comparar(long pacienteId, LocalDateTime base, LocalDateTime objetivo) {
        Datos d = cargar(pacienteId);
        EstadoOdontograma b = proyector.proyectar(d.eventos(base), base);
        EstadoOdontograma o = proyector.proyectar(d.eventos(objetivo), objetivo);
        return new DiferenciaVista(base, objetivo, DiferenciaOdontograma.entre(b, o));
    }

    public IndicesVista calcularIndices(long pacienteId) {
        Datos d = cargar(pacienteId);
        return indices(proyector.proyectar(d.eventos(null), null));
    }

    /** Último odontograma firmado del paciente y el estado de la boca que resulta de lo firmado. */
    record EstadoFirmado(long odontogramaId, EstadoOdontograma estado) {
    }

    /**
     * Estado para el motor de sugerencias del plan: solo hallazgos de odontogramas cerrados (los del borrador aún
     * pueden eliminarse) con las anulaciones registradas hasta ahora.
     */
    Optional<EstadoFirmado> estadoFirmado(long pacienteId) {
        Datos d = cargar(pacienteId);
        Optional<Odontograma> ultimo = d.odontogramas().stream().filter(o -> !o.esBorrador())
                .max(Comparator.comparing(Odontograma::getCerradoEn).thenComparing(Odontograma::getId));
        if (ultimo.isEmpty()) {
            return Optional.empty();
        }
        List<HallazgoEvento> firmados = d.hallazgos().stream()
                .filter(h -> !h.getOdontograma().esBorrador())
                .map(h -> evento(h, d.tipos().get(h.getHallazgoTipo().getId()), d.anuladoHasta(h.getId(), null)))
                .toList();
        return Optional.of(new EstadoFirmado(ultimo.get().getId(), proyector.proyectar(firmados, null)));
    }

    /** Índices de lo firmado (sin el borrador) para el reporte epidemiológico; vacío si no hay nada firmado. */
    Optional<IndicesVista> indicesFirmados(long pacienteId) {
        return estadoFirmado(pacienteId).map(e -> indices(e.estado()));
    }

    /** Ficha odontológica en PDF (RF-ODO-10); la imagen del odontograma la dibuja la UI. */
    public byte[] pdfFicha(long pacienteId, LocalDateTime hasta, byte[] imagenPng) {
        OdontogramaVista vista = obtenerVista(pacienteId, hasta);
        if (vista.odontogramaId() == null) {
            throw new NegocioException("El paciente aún no tiene odontograma.");
        }
        PacienteDetalle paciente = pacienteService.obtener(pacienteId);
        Atencion atencion = atencionRepository.findById(vista.atencionId()).orElseThrow();
        return fichaPdf.generar(new FichaOdontologica(configuracionService.datosClinica(), paciente, vista,
                atencion.getOdontologo().getNombreCompleto(), atencion.getOdontologo().getCop(), imagenPng));
    }

    // ------------------------------------------------------------------ ciclo de vida

    /**
     * Abre el odontograma en borrador de una atención: INICIAL si el paciente aún no tiene uno, si no EVOLUCION.
     *
     * @param denticion {@code null} = la habitual para la edad del paciente
     */
    @Transactional
    public long abrirBorrador(long atencionId, Denticion denticion) {
        sesionUsuario.exigir(Permiso.ODONTOGRAMA_EDITAR);
        Atencion atencion = atencionRepository.findById(atencionId)
                .orElseThrow(() -> new NegocioException("La atención ya no existe en la base de datos."));
        if (atencion.getEstado() != EstadoAtencion.ABIERTA) {
            throw new NegocioException("La atención está cerrada: abra una nueva atención para registrar una evolución.");
        }
        long pacienteId = atencion.getPaciente().getId();
        if (odontogramaRepository.findFirstByPacienteIdAndEstado(pacienteId, EstadoOdontogramaRegistro.BORRADOR).isPresent()) {
            throw new NegocioException("El paciente ya tiene un odontograma en borrador: ciérrelo antes de abrir otro.");
        }
        Odontograma o = new Odontograma();
        o.setPaciente(atencion.getPaciente());
        o.setAtencion(atencion);
        o.setTipo(odontogramaRepository.existsByPacienteIdAndTipo(pacienteId, TipoOdontograma.INICIAL)
                ? TipoOdontograma.EVOLUCION : TipoOdontograma.INICIAL);
        o.setDenticion(denticion != null ? denticion
                : Denticion.sugerida(Validaciones.edad(atencion.getPaciente().getFechaNacimiento(), LocalDate.now())));
        o.setEstado(EstadoOdontogramaRegistro.BORRADOR);
        o.setCreadoEn(ahora());
        return odontogramaRepository.save(o).getId();
    }

    /** Cambia la dentición del borrador si todos sus hallazgos siguen siendo válidos. */
    @Transactional
    public OdontogramaVista cambiarDenticion(long odontogramaId, Denticion denticion) {
        sesionUsuario.exigir(Permiso.ODONTOGRAMA_EDITAR);
        Odontograma o = borradorEditable(odontogramaId);
        boolean incompatibles = hallazgoRepository.listarPorOdontograma(o.getId()).stream()
                .anyMatch(h -> !denticion.admite(PiezaDental.de(h.getPiezaFdi()))
                        || (h.getPiezaFdiFin() != null && !denticion.admite(PiezaDental.de(h.getPiezaFdiFin()))));
        if (incompatibles) {
            throw new NegocioException("Hay hallazgos registrados en piezas que no pertenecen a la dentición elegida.");
        }
        o.setDenticion(denticion);
        return obtenerVista(o.getPaciente().getId(), null);
    }

    @Transactional
    public OdontogramaVista registrarHallazgo(NuevoHallazgo cmd) {
        long usuarioId = sesionUsuario.exigir(Permiso.ODONTOGRAMA_EDITAR).id();
        Odontograma o = borradorEditable(cmd.odontogramaId());
        HallazgoTipo tipo = hallazgoTipoRepository.findById(cmd.hallazgoTipoId())
                .orElseThrow(() -> new NegocioException("El tipo de hallazgo ya no existe en el catálogo."));
        if (!tipo.isActivo()) {
            throw new NegocioException("El hallazgo " + tipo.getNombre() + " está inactivo en el catálogo.");
        }
        long pacienteId = o.getPaciente().getId();
        Validado v = validar(cmd, tipo, o.getDenticion(), estadoActual(pacienteId));

        auditoria.registrar("HALLAZGO_CREAR", "odontograma", o.getId(), Map.of("tipo", tipo.getCodigo(), "pieza", cmd.piezaFdi()));
        hallazgoRepository.save(Hallazgo.registrar(o, tipo, cmd.piezaFdi(), v.piezaFin(), v.superficies(), v.estado(),
                v.sigla(), v.nota(), usuarioId, ahora()));
        return obtenerVista(pacienteId, null);
    }

    /** En borrador el hallazgo aún no está firmado: se puede eliminar físicamente. */
    @Transactional
    public OdontogramaVista eliminarHallazgoBorrador(long hallazgoId) {
        sesionUsuario.exigir(Permiso.ODONTOGRAMA_EDITAR);
        Hallazgo h = buscarHallazgo(hallazgoId);
        Odontograma o = h.getOdontograma();
        if (!o.esBorrador()) {
            throw new NegocioException("El odontograma ya está cerrado: el hallazgo no se puede eliminar, solo anular.");
        }
        borradorEditable(o.getId());
        hallazgoRepository.delete(h);
        log.info("Hallazgo {} eliminado del borrador {}", hallazgoId, o.getId());
        auditoria.registrar("HALLAZGO_ELIMINAR", "hallazgo", hallazgoId, Map.of("odontograma", o.getId()));
        return obtenerVista(o.getPaciente().getId(), null);
    }

    /** Corrige un hallazgo de un odontograma cerrado sin alterarlo (NTS 188, 5.6). */
    @Transactional
    public OdontogramaVista anularHallazgo(long hallazgoId, String motivo) {
        long usuarioId = sesionUsuario.exigir(Permiso.ODONTOGRAMA_EDITAR).id();
        Hallazgo h = buscarHallazgo(hallazgoId);
        if (h.getOdontograma().esBorrador()) {
            throw new NegocioException("El odontograma está en borrador: elimine el hallazgo en lugar de anularlo.");
        }
        if (anulacionRepository.existsByHallazgoId(hallazgoId)) {
            throw new NegocioException("El hallazgo ya está anulado.");
        }
        String m = Validador.limpio(motivo);
        if (m == null || m.length() < LARGO_MINIMO_MOTIVO) {
            throw new NegocioException("Indique el motivo de la anulación (al menos " + LARGO_MINIMO_MOTIVO + " caracteres).");
        }
        anulacionRepository.save(HallazgoAnulacion.de(h, m, usuarioId, ahora()));
        log.info("Hallazgo {} anulado: {}", hallazgoId, m);
        auditoria.registrar("HALLAZGO_ANULAR", "hallazgo", hallazgoId, Map.of("motivo", m));
        return obtenerVista(h.getOdontograma().getPaciente().getId(), null);
    }

    @Transactional
    public OdontogramaVista actualizarEspecificaciones(long odontogramaId, String especificaciones, String observaciones) {
        sesionUsuario.exigir(Permiso.ODONTOGRAMA_EDITAR);
        Odontograma o = borradorEditable(odontogramaId);
        String esp = Validador.limpioMultilinea(especificaciones);
        String obs = Validador.limpioMultilinea(observaciones);
        new Validador().maximo(esp, 2000, "especificaciones").maximo(obs, 2000, "observaciones").lanzarSiHayErrores();
        o.setEspecificaciones(esp);
        o.setObservaciones(obs);
        return obtenerVista(o.getPaciente().getId(), null);
    }

    /** Cierra (firma) el borrador: queda inmutable con su hash encadenado (RF-ODO-05). */
    @Transactional
    public OdontogramaVista cerrar(long odontogramaId) {
        long usuarioId = sesionUsuario.exigir(Permiso.ODONTOGRAMA_EDITAR).id();
        Odontograma o = borradorEditable(odontogramaId);
        long pacienteId = o.getPaciente().getId();
        String anterior = integridadService.hashAnterior(pacienteId);
        o.setCerradoEn(ahora());
        o.setCerradoPor(usuarioId);
        o.setHashAnterior(anterior);
        o.setHashCierre(integridadService.calcularHash(o, anterior));
        o.setEstado(EstadoOdontogramaRegistro.CERRADO);
        odontogramaRepository.save(o);
        log.info("Odontograma {} cerrado con hash {}", o.getId(), o.getHashCierre());
        auditoria.registrar("ODONTOGRAMA_CERRAR", "odontograma", o.getId(), Map.of("hash", o.getHashCierre()));
        eventos.publishEvent(new OdontogramaCerrado(pacienteId, o.getId(), o.getHashCierre()));
        return obtenerVista(pacienteId, null);
    }

    // ------------------------------------------------------------------ validación (PLAN §5.1.3)

    private record Validado(Integer piezaFin, Set<Superficie> superficies, EstadoHallazgo estado,
                            String sigla, String nota) {
    }

    private Validado validar(NuevoHallazgo cmd, HallazgoTipo tipo, Denticion denticion, EstadoOdontograma estado) {
        if (!PiezaDental.esValida(cmd.piezaFdi())) {
            throw new NegocioException("La pieza " + cmd.piezaFdi() + " no existe en el sistema FDI.");
        }
        PiezaDental pieza = PiezaDental.de(cmd.piezaFdi());
        Validador v = new Validador()
                .si(!denticion.admite(pieza), "La pieza " + pieza + " no pertenece a la dentición del odontograma.");
        Representacion r = tipo.getRepresentacion();
        Integer piezaFin = null;
        Set<Superficie> superficies = EnumSet.noneOf(Superficie.class);
        switch (r) {
            case SUPERFICIE -> {
                v.si(cmd.superficies().isEmpty(), "Seleccione al menos una superficie de la pieza " + pieza + ".");
                v.si(estado.superficiesOcultas(pieza.fdi()),
                        "La pieza " + pieza + " figura como ausente: no se pueden registrar hallazgos en sus superficies.");
                superficies = cmd.superficies().isEmpty() ? superficies : EnumSet.copyOf(cmd.superficies());
            }
            case PIEZA -> v.si(!cmd.superficies().isEmpty(), "El hallazgo " + tipo.getNombre() + " se registra por pieza, sin superficies.");
            case ENTRE_PIEZAS, RANGO_PIEZAS -> {
                if (cmd.piezaFdiFin() == null || !PiezaDental.esValida(cmd.piezaFdiFin())) {
                    v.si(true, "Seleccione la segunda pieza del hallazgo " + tipo.getNombre() + ".");
                } else {
                    PiezaDental fin = PiezaDental.de(cmd.piezaFdiFin());
                    piezaFin = fin.fdi();
                    v.si(fin.equals(pieza), "Seleccione dos piezas distintas.")
                            .si(!denticion.admite(fin), "La pieza " + fin + " no pertenece a la dentición del odontograma.")
                            .si(!pieza.mismaFila(fin), "Las piezas " + pieza + " y " + fin + " deben estar en la misma arcada y fila.")
                            .si(r == Representacion.ENTRE_PIEZAS && pieza.mismaFila(fin) && !pieza.esContiguaA(fin),
                                    "El hallazgo " + tipo.getNombre() + " se registra entre dos piezas contiguas.");
                }
                v.si(!cmd.superficies().isEmpty(), "El hallazgo " + tipo.getNombre() + " no se registra por superficie.");
            }
        }
        EstadoHallazgo est = null;
        if (tipo.getReglaColor() == ReglaColor.POR_ESTADO) {
            v.requerido(cmd.estado(), "el estado (bueno o malo)");
            est = cmd.estado();
        }
        List<String> opciones = CatalogoHallazgoService.opciones(tipo.getSiglasOpciones());
        String sigla = null;
        if (!opciones.isEmpty()) {
            sigla = cmd.sigla() == null ? null : cmd.sigla().trim().toUpperCase(Locale.ROOT);
            v.si(sigla == null || !opciones.contains(sigla),
                    "Elija la sigla del hallazgo " + tipo.getNombre() + ": " + String.join(", ", opciones) + ".");
        }
        String nota = Validador.limpio(cmd.nota());
        v.maximo(nota, 200, "nota");
        v.lanzarSiHayErrores();
        return new Validado(piezaFin, superficies, est, sigla, nota);
    }

    // ------------------------------------------------------------------ apoyo

    /** Hallazgos, anulaciones, catálogo y odontogramas del paciente, leídos una sola vez. */
    private record Datos(List<Odontograma> odontogramas, List<Hallazgo> hallazgos, Map<Long, HallazgoAnulacion> anulaciones,
                         Map<Long, TipoHallazgoDef> tipos, Map<Long, String> usuarios) {

        boolean anuladoHasta(long hallazgoId, LocalDateTime hasta) {
            HallazgoAnulacion a = anulaciones.get(hallazgoId);
            return a != null && (hasta == null || !a.getAnuladoEn().isAfter(hasta));
        }

        List<HallazgoEvento> eventos(LocalDateTime hasta) {
            return hallazgos.stream()
                    .filter(h -> registradoHasta(h, hasta))
                    .map(h -> evento(h, tipos.get(h.getHallazgoTipo().getId()), anuladoHasta(h.getId(), hasta)))
                    .toList();
        }

        /**
         * En el estado actual entran todos (incluido el borrador); en un momento pasado, solo los de odontogramas
         * firmados hasta entonces: el pasado es exactamente lo que se firmó.
         */
        static boolean registradoHasta(Hallazgo h, LocalDateTime hasta) {
            if (hasta == null) {
                return true;
            }
            Odontograma o = h.getOdontograma();
            return !o.esBorrador() && !o.getCerradoEn().isAfter(hasta);
        }
    }

    private Datos cargar(long pacienteId) {
        List<Hallazgo> hallazgos = hallazgoRepository.listarPorPaciente(pacienteId);
        Map<Long, HallazgoAnulacion> anulaciones = anulacionRepository.listarPorPaciente(pacienteId).stream()
                .collect(Collectors.toMap(a -> a.getHallazgo().getId(), Function.identity()));
        Map<Long, TipoHallazgoDef> tipos = catalogoService.definiciones().stream()
                .collect(Collectors.toMap(TipoHallazgoDef::id, Function.identity()));
        Map<Long, String> usuarios = new HashMap<>();
        for (Usuario u : usuarioRepository.findAll()) {
            usuarios.put(u.getId(), u.getNombreCompleto());
        }
        return new Datos(odontogramaRepository.findByPacienteIdOrderByCreadoEnAscIdAsc(pacienteId), hallazgos,
                anulaciones, tipos, usuarios);
    }

    private static Optional<Odontograma> odontogramaMostrado(List<Odontograma> odontogramas, LocalDateTime hasta) {
        if (hasta == null) {
            Optional<Odontograma> borrador = odontogramas.stream().filter(Odontograma::esBorrador).findFirst();
            if (borrador.isPresent()) {
                return borrador;
            }
        }
        return odontogramas.stream()
                .filter(o -> !o.esBorrador() && (hasta == null || !o.getCerradoEn().isAfter(hasta)))
                .max(Comparator.comparing(Odontograma::getCerradoEn).thenComparing(Odontograma::getId));
    }

    private EstadoOdontograma estadoActual(long pacienteId) {
        return proyector.proyectar(cargar(pacienteId).eventos(null), null);
    }

    private IndicesVista indices(EstadoOdontograma estado) {
        boolean excluir = configuracionService.booleano("odontograma.excluir_terceros_molares_cpod");
        return new IndicesVista(calculadora.cpod(estado, excluir), calculadora.ceod(estado), excluir);
    }

    private Odontograma borradorEditable(long odontogramaId) {
        Odontograma o = odontogramaRepository.findById(odontogramaId)
                .orElseThrow(() -> new NegocioException("El odontograma ya no existe en la base de datos."));
        if (!o.esBorrador()) {
            throw new NegocioException("El odontograma está cerrado y firmado: no se puede modificar.");
        }
        if (o.getAtencion().getEstado() != EstadoAtencion.ABIERTA) {
            throw new NegocioException("La atención del odontograma está cerrada.");
        }
        return o;
    }

    private Hallazgo buscarHallazgo(long id) {
        return hallazgoRepository.findById(id)
                .orElseThrow(() -> new NegocioException("El hallazgo ya no existe en la base de datos."));
    }

    static HallazgoEvento evento(Hallazgo h, TipoHallazgoDef tipo, boolean anulado) {
        Set<Superficie> sup = h.getSuperficies();
        return new HallazgoEvento(h.getId(), h.getOdontograma().getId(), tipo, PiezaDental.de(h.getPiezaFdi()),
                h.getPiezaFdiFin() == null ? null : PiezaDental.de(h.getPiezaFdiFin()),
                sup.isEmpty() ? EnumSet.noneOf(Superficie.class) : EnumSet.copyOf(sup), h.getEstado(),
                h.getSigla(), h.getNota(), h.getRegistradoEn(), h.getRegistradoPor(), anulado);
    }

    private static HallazgoDto dto(Hallazgo h, Datos d, LocalDateTime hasta, Set<Long> vigentes) {
        TipoHallazgoDef tipo = d.tipos().get(h.getHallazgoTipo().getId());
        HallazgoEvento e = evento(h, tipo, false);
        boolean anulado = d.anuladoHasta(h.getId(), hasta);
        HallazgoAnulacion a = anulado ? d.anulaciones().get(h.getId()) : null;
        boolean eliminable = hasta == null && h.getOdontograma().esBorrador()
                && h.getOdontograma().getAtencion().getEstado() == EstadoAtencion.ABIERTA;
        return new HallazgoDto(h.getId(), h.getOdontograma().getId(), tipo.id(), tipo.codigo(), tipo.nombre(),
                tipo.representacion(), h.getPiezaFdi(), h.getPiezaFdiFin(), h.getSuperficies(), h.getEstado(),
                h.getSigla() != null ? h.getSigla() : tipo.sigla(), h.getNota(), h.getRegistradoEn(),
                d.usuarios().getOrDefault(h.getRegistradoPor(), "—"), e.color(), anulado,
                a == null ? null : a.getMotivo(), a == null ? null : a.getAnuladoEn(), vigentes.contains(h.getId()), eliminable);
    }

    /** Fechas guardadas al segundo: lo mismo que entra en el hash es lo que se lee después. */
    private static LocalDateTime ahora() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
