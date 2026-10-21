/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.seeder;

import com.odontogest.config.RelojAplicacion;
import com.odontogest.dominio.finanzas.Frecuencia;
import com.odontogest.dominio.odontograma.Denticion;
import com.odontogest.dominio.odontograma.EstadoHallazgo;
import com.odontogest.dominio.odontograma.Superficie;
import com.odontogest.modelo.EstadoCita;
import com.odontogest.modelo.EstadoPlan;
import com.odontogest.modelo.EstadoPlanItem;
import com.odontogest.modelo.MedioPago;
import com.odontogest.modelo.Odontologo;
import com.odontogest.modelo.Usuario;
import com.odontogest.persistencia.repositorio.CitaRepository;
import com.odontogest.persistencia.repositorio.UsuarioRepository;
import com.odontogest.seguridad.SesionUsuario;
import com.odontogest.servicio.AtencionService;
import com.odontogest.servicio.CatalogoHallazgoService;
import com.odontogest.servicio.CitaService;
import com.odontogest.servicio.OdontogramaService;
import com.odontogest.servicio.PacienteService;
import com.odontogest.servicio.PagoService;
import com.odontogest.servicio.PlanTratamientoService;
import com.odontogest.servicio.dto.AtencionForm;
import com.odontogest.servicio.dto.CitaForm;
import com.odontogest.servicio.dto.CronogramaForm;
import com.odontogest.servicio.dto.CuotaDto;
import com.odontogest.servicio.dto.CuotaPacienteDto;
import com.odontogest.servicio.dto.HallazgoDto;
import com.odontogest.servicio.dto.HallazgoTipoDto;
import com.odontogest.servicio.dto.NuevoHallazgo;
import com.odontogest.servicio.dto.OdontogramaVista;
import com.odontogest.servicio.dto.PagoForm;
import com.odontogest.servicio.dto.PlanDto;
import com.odontogest.servicio.dto.PlanItemDto;
import com.odontogest.servicio.dto.UsuarioSesion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Historia clínica de demostración (PLAN §3.5): simula unos seis meses de trabajo del consultorio hasta hoy.
 * <ul>
 *   <li>60 pacientes registrados en recepción a lo largo del periodo;</li>
 *   <li>40 primeras consultas desde una cita: odontograma INICIAL firmado con 3 a 12 hallazgos según la edad
 *       (decidua &lt; 6 años, mixta 6–12, permanente &gt; 12), plan generado con el motor de sugerencias,
 *       cronograma, aceptación y pago de la cuota inicial;</li>
 *   <li>15 consultas de EVOLUCIÓN que reemplazan las caries por restauraciones y marcan los procedimientos;</li>
 *   <li>pagos de cuotas en sus vencimientos (algunos pacientes quedan con deuda vencida), aplicados FIFO;</li>
 *   <li>citas de control hasta completar 120 entre hoy −30 y hoy +30 días, sin solapamientos.</li>
 * </ul>
 * Cada paso es un evento con fecha y hora: se ejecutan en orden cronológico, con el {@link RelojAplicacion} fijado en
 * ese momento y <b>a través de los servicios de la aplicación</b> (mismas validaciones, auditoría y hashes que si lo
 * hiciera un usuario). Un paso que un servicio rechaza se omite y se registra en el log. {@code Random} con semilla:
 * los datos son reproducibles.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClinicaDemoSeeder {

    static final int DIAS_HISTORIA = 175;
    static final int PRIMERAS_CONSULTAS = 40;
    static final int EVOLUCIONES = 15;
    static final int CITAS_VENTANA = 120;
    static final int DIAS_VENTANA = 30;
    private static final int CONTROLES = 70;

    private static final int[] MOLARES = {16, 17, 26, 27, 36, 37, 46, 47};
    private static final int[] PREMOLARES = {14, 15, 24, 25, 34, 35, 44, 45};
    private static final int[] ANTERIORES = {11, 12, 13, 21, 22, 23, 31, 32, 33, 41, 42, 43};
    private static final int[] MOLARES_DECIDUOS = {54, 55, 64, 65, 74, 75, 84, 85};
    private static final int[] PRIMEROS_MOLARES = {16, 26, 36, 46};

    private final PacienteSeeder pacienteSeeder;
    private final PacienteService pacienteService;
    private final AtencionService atencionService;
    private final OdontogramaService odontogramaService;
    private final CatalogoHallazgoService catalogoService;
    private final PlanTratamientoService planService;
    private final PagoService pagoService;
    private final CitaService citaService;
    private final CitaRepository citaRepository;
    private final UsuarioRepository usuarioRepository;
    private final SesionUsuario sesionUsuario;
    private final RelojAplicacion reloj;

    /** Resultado para el log y las pruebas. */
    public record Resumen(int pacientes, int primerasConsultas, int evoluciones, int pagos, int citasVentana,
                          int omitidos, String ultimaHistoria) {
    }

    /**
     * Siembra la historia demo hasta {@code ahora}. Deja el reloj en la hora real y la sesión cerrada.
     *
     * @param odontologos el primero es el odontólogo general, luego endodoncia y ortodoncia
     */
    public Resumen sembrar(List<Odontologo> odontologos, long semilla, LocalDateTime ahora) {
        Simulacion s = new Simulacion(odontologos.stream().map(Odontologo::getId).toList(), new Random(semilla),
                ahora.truncatedTo(ChronoUnit.MINUTES));
        try {
            return s.ejecutar();
        } finally {
            reloj.liberar();
            sesionUsuario.cerrar();
        }
    }

    // ------------------------------------------------------------------ simulación

    private final class Simulacion {

        private record Evento(LocalDateTime momento, long orden, Runnable accion) {
        }

        /** Paciente demo ya registrado. */
        private record Registrado(long id, PacienteSeeder.PacienteDemo datos, int odontologo) {
        }

        private final List<Long> odontologos;
        private final Random r;
        private final LocalDateTime ahora;
        private final LocalDate hoy;
        private final PriorityQueue<Evento> cola = new PriorityQueue<>(
                Comparator.comparing(Evento::momento).thenComparingLong(Evento::orden));
        private long orden;
        private final Map<String, Long> tipos;
        private final UsuarioSesion admin;
        private final UsuarioSesion recepcion;

        private final List<Registrado> registrados = new ArrayList<>();
        /** Pacientes con primera consulta hecha (pueden recibir citas de control). */
        private final List<Registrado> atendidos = new ArrayList<>();
        private int primeras;
        private int evoluciones;
        private int evolucionesProgramadas;
        private int pagos;
        private int omitidos;

        Simulacion(List<Long> odontologos, Random r, LocalDateTime ahora) {
            this.odontologos = odontologos;
            this.r = r;
            this.ahora = ahora;
            this.hoy = ahora.toLocalDate();
            this.admin = sesion("admin");
            this.recepcion = sesion("recepcion");
            reloj.fijar(ahora);
            sesionUsuario.iniciar(admin);
            this.tipos = catalogoService.listarTipos(true).stream()
                    .collect(Collectors.toMap(HallazgoTipoDto::codigo, HallazgoTipoDto::id));
        }

        Resumen ejecutar() {
            List<PacienteSeeder.PacienteDemo> fichas = pacienteSeeder.generar(r);
            LocalDate inicio = hoy.minusDays(DIAS_HISTORIA);
            // Las primeras consultas, entre los registrados con al menos dos semanas de anticipación (ya ocurrieron).
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < fichas.size() - 5; i++) {
                indices.add(i);
            }
            Collections.shuffle(indices, r);
            Set<Integer> conConsulta = new HashSet<>(indices.subList(0, PRIMERAS_CONSULTAS));
            for (int i = 0; i < fichas.size(); i++) {
                LocalDate dia = laborable(inicio.plusDays((long) i * (DIAS_HISTORIA - 1) / (fichas.size() - 1)));
                if (!dia.isBefore(hoy)) {
                    dia = laborable(hoy.minusDays(2));
                }
                LocalDateTime momento = dia.atTime(8, 15).plusMinutes(5L * r.nextInt(30));
                PacienteSeeder.PacienteDemo ficha = fichas.get(i);
                boolean consulta = conConsulta.contains(i);
                en(momento, () -> registrar(ficha, consulta));
            }
            for (int i = 0; i < CONTROLES; i++) {
                programarControl();
            }
            while (!cola.isEmpty()) {
                Evento e = cola.poll();
                reloj.fijar(e.momento());
                try {
                    e.accion().run();
                } catch (RuntimeException ex) {
                    omitidos++;
                    if (omitidos <= 5) {
                        log.warn("Datos demo: paso omitido el {}: {}", e.momento(), ex.toString());
                    } else {
                        log.debug("Datos demo: paso omitido el {}: {}", e.momento(), ex.getMessage());
                    }
                }
            }
            reloj.fijar(ahora);
            completarVentana();
            String ultima = registrados.isEmpty() ? ""
                    : pacienteService.obtener(registrados.get(registrados.size() - 1).id()).numeroHistoria();
            return new Resumen(registrados.size(), primeras, evoluciones, pagos, citasEnVentana(), omitidos, ultima);
        }

        // -------------------------------------------------------------- pacientes y primera consulta

        private void registrar(PacienteSeeder.PacienteDemo ficha, boolean conConsulta) {
            como(recepcion);
            long id = pacienteSeeder.registrar(ficha, reloj.instant().atZone(reloj.getZone()).toLocalDate(), r);
            // General ≈55 %, endodoncia ≈25 %, ortodoncia ≈20 % (y la mitad de los menores de 10 años o más).
            int odontologo = ficha.menor() && ficha.edad() >= 10 && r.nextInt(2) == 0 ? 2
                    : r.nextInt(100) < 55 ? 0 : r.nextInt(100) < 55 ? 1 : 2;
            Registrado p = new Registrado(id, ficha, odontologo);
            registrados.add(p);
            como(admin);
            pacienteSeeder.registrarAnamnesis(id, ficha);
            como(recepcion);
            if (!conConsulta) {
                return;
            }
            Optional<LocalDateTime> hueco = citaService.siguienteHueco(odontologos.get(odontologo), 45,
                    momento().plusMinutes(20), id);
            if (hueco.isEmpty()) {
                return;
            }
            long cita = citaService.programar(new CitaForm(null, id, odontologos.get(odontologo), hueco.get(), 45,
                    "Primera consulta: evaluación general", null, null)).id();
            en(hueco.get(), () -> primeraConsulta(p, cita));
        }

        private void primeraConsulta(Registrado p, long cita) {
            como(admin);
            LocalDateTime t = momento();
            long atencion = atencionService.abrirDesdeCita(cita);
            OdontogramaVista v = odontogramaService.obtenerVista(p.id(), null);
            int registradosOk = registrarHallazgos(v.odontogramaId(), v.denticion(), p.datos().edad());
            boolean cariado = odontogramaService.obtenerVista(p.id(), null).historial().stream()
                    .anyMatch(h -> h.codigoTipo().equals("CARIES"));
            atencionService.actualizar(new AtencionForm(atencion, p.id(), null, "Primera consulta: evaluación general",
                    "Examen clínico intraoral y extraoral. " + registradosOk + " hallazgo(s) registrados en el odontograma.",
                    cariado ? "Caries dental (K02.1)." : "Sin caries activas.",
                    "Se explica el plan de tratamiento al paciente.", null));
            fijar(t.plusMinutes(20));
            odontogramaService.cerrar(v.odontogramaId());
            primeras++;

            long plan = planService.crear(p.id(), odontologos.get(p.odontologo()), true);
            planService.cambiarEstado(plan, EstadoPlan.PROPUESTO);
            PlanDto dto = planService.obtener(plan);
            boolean acepta = r.nextInt(100) < 88;
            if (acepta) {
                BigDecimal total = dto.total();
                if (total.compareTo(new BigDecimal("350")) > 0) {
                    BigDecimal inicial = total.multiply(new BigDecimal("0.3")).divide(BigDecimal.TEN, 0, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.TEN);
                    int cuotas = Math.min(5, total.divide(new BigDecimal("400"), 0, RoundingMode.UP).intValue());
                    planService.generarCronograma(new CronogramaForm(plan, inicial, cuotas, t.toLocalDate().plusMonths(1),
                            Frecuencia.MENSUAL));
                }
                dto = planService.aceptar(plan, p.datos().firmante());
                realizar(atencion, dto, i -> i.procedimientoCodigo().equals("CONS"));
            }
            fijar(t.plusMinutes(40));
            atencionService.cerrar(atencion);
            if (!atendidos.contains(p)) {
                atendidos.add(p);
            }
            if (!acepta) {
                return;
            }
            como(recepcion);
            fijar(t.plusMinutes(50));
            pagar(p.id(), false);
            for (CuotaDto c : planService.obtener(plan).cuotas()) {
                if (c.numero() == 0) {
                    continue;
                }
                int azar = r.nextInt(100);
                if (azar < 70) {
                    en(c.vencimiento().minusDays(r.nextInt(4)).atTime(9 + r.nextInt(9), 5 * r.nextInt(12)),
                            () -> {
                                como(recepcion);
                                pagar(p.id(), false);
                            });
                } else if (azar < 80) {
                    en(c.vencimiento().plusDays(3 + r.nextInt(10)).atTime(10 + r.nextInt(8), 5 * r.nextInt(12)),
                            () -> {
                                como(recepcion);
                                pagar(p.id(), true);
                            });
                }
                // El resto no paga esa cuota: quedará vencida (reporte de morosidad).
            }
            boolean antiguo = !t.toLocalDate().isAfter(hoy.minusDays(35));
            if (cariado && antiguo && evolucionesProgramadas < EVOLUCIONES) {
                evolucionesProgramadas++;
                LocalDateTime desde = laborable(t.toLocalDate().plusDays(10 + r.nextInt(20))).atTime(9, 0);
                Optional<LocalDateTime> hueco = citaService.siguienteHueco(odontologos.get(p.odontologo()), 60, desde, p.id());
                if (hueco.isPresent()) {
                    long cita2 = citaService.programar(new CitaForm(null, p.id(), odontologos.get(p.odontologo()),
                            hueco.get(), 60, "Restauraciones según plan de tratamiento", null, null)).id();
                    en(hueco.get(), () -> evolucion(p, plan, cita2));
                }
            }
        }

        // -------------------------------------------------------------- evolución y controles

        private void evolucion(Registrado p, long plan, long cita) {
            como(admin);
            LocalDateTime t = momento();
            long atencion = atencionService.abrirDesdeCita(cita);
            OdontogramaVista v = odontogramaService.obtenerVista(p.id(), null);
            List<HallazgoDto> caries = v.historial().stream()
                    .filter(h -> h.codigoTipo().equals("CARIES") && h.vigente() && !h.anulado()).toList();
            for (HallazgoDto h : caries) {
                intentar(() -> odontogramaService.registrarHallazgo(new NuevoHallazgo(v.odontogramaId(),
                        tipos.get("RESTAURACION_DEFINITIVA"), h.pieza(), null, h.superficies(), EstadoHallazgo.BUENO,
                        h.pieza() > 50 ? "IV" : "R", "Restaurada en esta atención")));
            }
            Set<Long> cariesIds = caries.stream().map(HallazgoDto::id).collect(Collectors.toSet());
            realizar(atencion, planService.obtener(plan),
                    i -> (i.hallazgoId() != null && cariesIds.contains(i.hallazgoId())) || i.procedimientoCodigo().equals("PROF"));
            atencionService.actualizar(new AtencionForm(atencion, p.id(), null, "Restauraciones según plan de tratamiento",
                    "Aislamiento relativo, remoción de caries y restauración.", "Caries tratadas.",
                    "Control en seis meses.", null));
            fijar(t.plusMinutes(40));
            odontogramaService.cerrar(v.odontogramaId());
            fijar(t.plusMinutes(50));
            atencionService.cerrar(atencion);
            evoluciones++;
        }

        /** Cita de control en la ventana de ±30 días, programada unos días antes. */
        private void programarControl() {
            LocalDate dia = laborable(hoy.plusDays(r.nextInt(2 * DIAS_VENTANA + 1) - DIAS_VENTANA));
            LocalDateTime objetivo = dia.atTime(r.nextBoolean() ? 9 : 15, 0).plusMinutes(15L * r.nextInt(14));
            LocalDateTime programacion = dia.minusDays(2 + r.nextInt(12)).atTime(9, 30).plusMinutes(5L * r.nextInt(60));
            if (programacion.isAfter(ahora)) {
                programacion = ahora.minusMinutes(1 + r.nextInt(600));
            }
            en(programacion, () -> {
                if (atendidos.isEmpty()) {
                    return;
                }
                Registrado p = atendidos.get(r.nextInt(atendidos.size()));
                como(recepcion);
                LocalDateTime desde = objetivo.isBefore(momento()) ? momento().plusHours(1) : objetivo;
                Optional<LocalDateTime> hueco = citaService.siguienteHueco(odontologos.get(p.odontologo()), 30, desde, p.id());
                if (hueco.isEmpty() || hueco.get().toLocalDate().isAfter(hoy.plusDays(DIAS_VENTANA))) {
                    return;
                }
                long cita = citaService.programar(new CitaForm(null, p.id(), odontologos.get(p.odontologo()), hueco.get(),
                        30, r.nextBoolean() ? "Control" : "Profilaxis y control", null, null)).id();
                int azar = r.nextInt(100);
                if (hueco.get().isAfter(ahora)) {
                    if (azar < 40) {
                        citaService.cambiarEstado(cita, EstadoCita.CONFIRMADA);
                    }
                } else if (azar < 65) {
                    en(hueco.get(), () -> control(p, cita));
                } else if (azar < 85) {
                    en(hueco.get().plusMinutes(30), () -> {
                        como(recepcion);
                        citaService.cambiarEstado(cita, EstadoCita.NO_ASISTIO);
                    });
                } else {
                    en(momento().plusHours(2), () -> {
                        como(recepcion);
                        citaService.cambiarEstado(cita, EstadoCita.CANCELADA);
                    });
                }
            });
        }

        private void control(Registrado p, long cita) {
            como(admin);
            LocalDateTime t = momento();
            long atencion = atencionService.abrirDesdeCita(cita);
            planService.listarPorPaciente(p.id()).stream()
                    .filter(pl -> pl.estado() == EstadoPlan.ACEPTADO || pl.estado() == EstadoPlan.EN_CURSO)
                    .findFirst()
                    .ifPresent(pl -> realizar(atencion, planService.obtener(pl.id()),
                            i -> i.procedimientoCodigo().equals("PROF") || i.procedimientoCodigo().equals("DEST")));
            atencionService.actualizar(new AtencionForm(atencion, p.id(), null, "Control", "Encías sin signos de inflamación.",
                    "Paciente en mantenimiento.", null, null));
            fijar(t.plusMinutes(25));
            atencionService.cerrar(atencion);   // el odontograma de evolución vacío se descarta
        }

        /** Completa {@value #CITAS_VENTANA} citas entre hoy −30 y hoy +30 con citas futuras programadas hoy. */
        private void completarVentana() {
            como(recepcion);
            List<Registrado> candidatos = atendidos.isEmpty() ? registrados : atendidos;
            for (int intento = 0; intento < 200 && citasEnVentana() < CITAS_VENTANA && !candidatos.isEmpty(); intento++) {
                Registrado p = candidatos.get(r.nextInt(candidatos.size()));
                LocalDateTime desde = laborable(hoy.plusDays(r.nextInt(DIAS_VENTANA))).atTime(r.nextBoolean() ? 9 : 15, 0)
                        .plusMinutes(15L * r.nextInt(14));
                try {
                    Optional<LocalDateTime> hueco = citaService.siguienteHueco(odontologos.get(p.odontologo()), 30,
                            desde.isBefore(ahora) ? ahora : desde, p.id());
                    if (hueco.isPresent() && !hueco.get().toLocalDate().isAfter(hoy.plusDays(DIAS_VENTANA))) {
                        long cita = citaService.programar(new CitaForm(null, p.id(), odontologos.get(p.odontologo()),
                                hueco.get(), 30, "Control", null, null)).id();
                        if (r.nextInt(3) == 0) {
                            citaService.cambiarEstado(cita, EstadoCita.CONFIRMADA);
                        }
                    }
                } catch (RuntimeException ex) {
                    omitidos++;
                    log.debug("Datos demo: cita omitida: {}", ex.getMessage());
                }
            }
        }

        private int citasEnVentana() {
            return citaRepository.listarEntre(hoy.minusDays(DIAS_VENTANA).atStartOfDay(),
                    hoy.plusDays(DIAS_VENTANA + 1L).atStartOfDay(), EnumSet.allOf(EstadoCita.class)).size();
        }

        // -------------------------------------------------------------- odontograma

        /** Registra hallazgos coherentes con la edad y la dentición; devuelve cuántos aceptó el servicio. */
        private int registrarHallazgos(long odontograma, Denticion denticion, int edad) {
            List<NuevoHallazgo> lista = new ArrayList<>();
            Set<Integer> usadas = new HashSet<>();
            if (denticion == Denticion.DECIDUA) {
                int n = 3 + r.nextInt(4);
                for (int k = 0; k < n; k++) {
                    int pieza = libre(MOLARES_DECIDUOS, usadas);
                    lista.add(k == n - 1 && r.nextBoolean()
                            ? hallazgo(odontograma, "PULPOTOMIA", pieza, Set.of(), EstadoHallazgo.BUENO, null)
                            : caries(odontograma, pieza));
                }
            } else if (denticion == Denticion.MIXTA) {
                for (int k = 0; k < 2 + r.nextInt(3); k++) {
                    lista.add(hallazgo(odontograma, "SELLANTE", libre(PRIMEROS_MOLARES, usadas), Set.of(Superficie.O),
                            EstadoHallazgo.BUENO, null));
                }
                for (int k = 0; k < 1 + r.nextInt(3); k++) {
                    lista.add(caries(odontograma, libre(MOLARES_DECIDUOS, usadas)));
                }
                if (edad <= 8) {
                    lista.add(hallazgo(odontograma, "PIEZA_EN_ERUPCION", r.nextBoolean() ? 12 : 22, Set.of(), null, null));
                }
            } else {
                int n = 3 + r.nextInt(10);
                for (int k = 0; k < n; k++) {
                    int azar = r.nextInt(100);
                    int pieza = libre(azar < 50 ? MOLARES : azar < 80 ? PREMOLARES : ANTERIORES, usadas);
                    if (pieza < 0) {
                        continue;
                    }
                    int tipo = r.nextInt(100) - Math.min(30, Math.max(0, edad - 30) / 2);
                    if (tipo >= 45) {
                        lista.add(caries(odontograma, pieza));
                    } else if (tipo >= 20) {
                        lista.add(hallazgo(odontograma, "RESTAURACION_DEFINITIVA", pieza, superficies(pieza),
                                r.nextInt(4) == 0 ? EstadoHallazgo.MALO : EstadoHallazgo.BUENO, r.nextBoolean() ? "AM" : "R"));
                    } else if (tipo >= 8 && esPosterior(pieza)) {
                        lista.add(hallazgo(odontograma, "PIEZA_AUSENTE_DEX", pieza, Set.of(), null, null));
                    } else if (tipo >= 0 && esPosterior(pieza)) {
                        EstadoHallazgo estado = r.nextInt(4) == 0 ? EstadoHallazgo.MALO : EstadoHallazgo.BUENO;
                        lista.add(hallazgo(odontograma, "TRATAMIENTO_CONDUCTOS", pieza, Set.of(), estado, "TC"));
                        lista.add(hallazgo(odontograma, "CORONA", pieza, Set.of(), EstadoHallazgo.BUENO, "CMC"));
                    } else if (edad > 55 && !esPosterior(pieza)) {
                        lista.add(hallazgo(odontograma, "MOVILIDAD", pieza, Set.of(), null, r.nextBoolean() ? "M1" : "M2"));
                    } else {
                        lista.add(hallazgo(odontograma, r.nextBoolean() ? "REMANENTE_RADICULAR" : "FRACTURA", pieza,
                                Set.of(), null, null));
                    }
                }
            }
            int ok = 0;
            for (NuevoHallazgo h : lista) {
                if (ok >= 12) {
                    break;
                }
                if (h != null && intentar(() -> odontogramaService.registrarHallazgo(h))) {
                    ok++;
                }
            }
            // Si el servicio rechazó alguno (p. ej. una combinación que no admite la pieza), se completa hasta 3.
            int[] reserva = denticion == Denticion.PERMANENTE ? PREMOLARES : MOLARES_DECIDUOS;
            for (int intento = 0; ok < 3 && intento < 10; intento++) {
                NuevoHallazgo h = caries(odontograma, libre(reserva, usadas));
                if (h != null && intentar(() -> odontogramaService.registrarHallazgo(h))) {
                    ok++;
                }
            }
            return ok;
        }

        private NuevoHallazgo caries(long odontograma, int pieza) {
            if (pieza < 0) {
                return null;
            }
            String[] siglas = {"CE", "CD", "CD", "CDP"};
            return hallazgo(odontograma, "CARIES", pieza, superficies(pieza), null, siglas[r.nextInt(siglas.length)]);
        }

        private NuevoHallazgo hallazgo(long odontograma, String codigo, int pieza, Set<Superficie> superficies,
                                       EstadoHallazgo estado, String sigla) {
            Long tipo = tipos.get(codigo);
            return tipo == null || pieza < 0 ? null
                    : new NuevoHallazgo(odontograma, tipo, pieza, null, superficies, estado, sigla, null);
        }

        /** Una a tres superficies: oclusal (o incisal) con mesial o distal; en anteriores, proximal o vestibular. */
        private Set<Superficie> superficies(int pieza) {
            boolean anterior = pieza % 10 <= 3;
            int azar = r.nextInt(100);
            if (anterior) {
                return azar < 50 ? Set.of(r.nextBoolean() ? Superficie.M : Superficie.D) : Set.of(Superficie.V);
            }
            if (azar < 50) {
                return Set.of(Superficie.O);
            }
            if (azar < 85) {
                return Set.of(Superficie.O, r.nextBoolean() ? Superficie.M : Superficie.D);
            }
            return Set.of(Superficie.M, Superficie.O, Superficie.D);
        }

        private int libre(int[] piezas, Set<Integer> usadas) {
            for (int intento = 0; intento < 12; intento++) {
                int p = piezas[r.nextInt(piezas.length)];
                if (usadas.add(p)) {
                    return p;
                }
            }
            return -1;
        }

        private static boolean esPosterior(int pieza) {
            return pieza % 10 >= 4;
        }

        // -------------------------------------------------------------- plan y pagos

        private void realizar(long atencion, PlanDto plan, Predicate<PlanItemDto> filtro) {
            if (plan.estado() != EstadoPlan.ACEPTADO && plan.estado() != EstadoPlan.EN_CURSO) {
                return;
            }
            for (PlanItemDto i : plan.items()) {
                if (i.estado() == EstadoPlanItem.PENDIENTE && filtro.test(i)) {
                    intentar(() -> planService.marcarEnAtencion(atencion, i.id(), true));
                }
            }
        }

        /** Paga la siguiente cuota pendiente (FIFO); {@code parcial} paga la mitad. */
        private void pagar(long pacienteId, boolean parcial) {
            List<CuotaPacienteDto> pendientes = pagoService.cuotasPendientes(pacienteId);
            if (pendientes.isEmpty()) {
                return;
            }
            BigDecimal saldo = pendientes.get(0).saldo();
            BigDecimal monto = parcial ? saldo.divide(BigDecimal.TWO, 0, RoundingMode.DOWN).max(BigDecimal.ONE) : saldo;
            int azar = r.nextInt(100);
            MedioPago medio = azar < 45 ? MedioPago.EFECTIVO : azar < 70 ? MedioPago.YAPE : azar < 82 ? MedioPago.TARJETA
                    : azar < 92 ? MedioPago.PLIN : MedioPago.TRANSFERENCIA;
            String referencia = medio == MedioPago.EFECTIVO ? null : "OP-" + (100000 + r.nextInt(900000));
            pagoService.registrar(new PagoForm(pacienteId, monto, medio, referencia));
            pagos++;
        }

        // -------------------------------------------------------------- apoyo

        private void en(LocalDateTime momento, Runnable accion) {
            if (!momento.isAfter(ahora)) {
                cola.add(new Evento(momento, orden++, accion));
            }
        }

        private LocalDateTime momento() {
            return LocalDateTime.now(reloj);
        }

        private void fijar(LocalDateTime t) {
            reloj.fijar(t.isAfter(ahora) ? ahora : t);
        }

        private boolean intentar(Runnable paso) {
            try {
                paso.run();
                return true;
            } catch (RuntimeException ex) {
                log.debug("Datos demo: {}", ex.getMessage());
                return false;
            }
        }

        private void como(UsuarioSesion usuario) {
            sesionUsuario.iniciar(usuario);
        }

        private UsuarioSesion sesion(String username) {
            Usuario u = usuarioRepository.findByUsernameIgnoreCase(username).orElseThrow();
            return new UsuarioSesion(u.getId(), u.getUsername(), u.getNombreCompleto(), u.getRol().name(),
                    u.getOdontologo() == null ? null : u.getOdontologo().getId());
        }

        /** El mismo día o el siguiente hábil (el consultorio no atiende los domingos). */
        private static LocalDate laborable(LocalDate dia) {
            return dia.getDayOfWeek() == DayOfWeek.SUNDAY ? dia.plusDays(1) : dia;
        }
    }
}
