/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio;

import com.odontogest.dominio.comun.Validaciones;
import com.odontogest.excepcion.NegocioException;
import com.odontogest.modelo.Anamnesis;
import com.odontogest.modelo.Consentimiento;
import com.odontogest.modelo.Paciente;
import com.odontogest.modelo.TipoConsentimiento;
import com.odontogest.modelo.TipoDocumento;
import com.odontogest.persistencia.repositorio.AnamnesisRepository;
import com.odontogest.persistencia.repositorio.ConsentimientoRepository;
import com.odontogest.persistencia.repositorio.PacienteRepository;
import com.odontogest.seguridad.Permiso;
import com.odontogest.seguridad.SesionUsuario;
import com.odontogest.servicio.dto.Pagina;
import com.odontogest.servicio.dto.PacienteDetalle;
import com.odontogest.servicio.dto.PacienteForm;
import com.odontogest.servicio.dto.PacienteResumen;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Pacientes e historia clínica (RF-PAC-01 a RF-PAC-03): búsqueda paginada, ficha y registro con número
 * de historia correlativo. Los pacientes no se eliminan: se desactivan (sus datos clínicos se conservan).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PacienteService {

    public static final int TAMANIO_PAGINA = 15;
    private static final int EDAD_MAXIMA = 120;

    /** Última historia creada por el seeder demo (PLAN §3.5); la escribe {@code DatabaseSeeder}. */
    public static final String CLAVE_ULTIMO_HC_DEMO = "seeder.ultimo_hc_demo";

    private final Clock reloj;
    private final ConfiguracionService configuracionService;
    private final PacienteRepository pacienteRepository;
    private final AuditoriaService auditoria;
    private final AnamnesisRepository anamnesisRepository;
    private final ConsentimientoRepository consentimientoRepository;
    private final CorrelativoService correlativoService;
    private final SesionUsuario sesionUsuario;

    /**
     * Busca por nombre, apellidos, documento o N° de historia. Cada palabra del filtro debe aparecer en
     * alguno de esos campos, así "perez juan" encuentra a "Juan Pérez" sin importar el orden.
     */
    public Pagina<PacienteResumen> buscar(String filtro, boolean incluirInactivos, int pagina) {
        PageRequest solicitud = PageRequest.of(Math.max(0, pagina), TAMANIO_PAGINA,
                Sort.by("apellidoPaterno", "apellidoMaterno", "nombres", "id"));
        Page<Paciente> resultado = pacienteRepository.findAll(especificacion(filtro, incluirInactivos), solicitud);
        LocalDate hoy = LocalDate.now(reloj);
        String ultimoDemo = ultimoDemo();
        List<PacienteResumen> filas = resultado.getContent().stream().map(p -> resumen(p, hoy, ultimoDemo)).toList();
        return new Pagina<>(filas, resultado.getNumber(), TAMANIO_PAGINA, resultado.getTotalElements());
    }

    public PacienteDetalle obtener(long id) {
        Paciente p = buscarPaciente(id);
        String alergias = anamnesisRepository.findByPacienteId(id).map(Anamnesis::getAlergias).orElse(null);
        LocalDateTime consentimiento = consentimientoRepository
                .findFirstByPacienteIdAndTipoOrderByAceptadoEnDesc(id, TipoConsentimiento.DATOS_PERSONALES)
                .map(Consentimiento::getAceptadoEn).orElse(null);
        return detalle(p, alergias, consentimiento);
    }

    /** Registra o actualiza un paciente; devuelve su id. */
    @Transactional
    public long guardar(PacienteForm form) {
        sesionUsuario.exigir(Permiso.PACIENTE_EDITAR);
        PacienteForm f = normalizar(form);
        validar(f);

        Paciente p = f.id() == null ? new Paciente() : buscarPaciente(f.id());
        LocalDateTime ahora = LocalDateTime.now(reloj);
        if (p.getId() == null) {
            p.setNumeroHistoria(correlativoService.siguienteHistoriaClinica());
            p.setCreadoEn(ahora);
            p.setActivo(true);
        }
        p.setTipoDocumento(f.tipoDocumento());
        p.setNumeroDocumento(f.numeroDocumento());
        p.setNombres(f.nombres());
        p.setApellidoPaterno(f.apellidoPaterno());
        p.setApellidoMaterno(f.apellidoMaterno());
        p.setFechaNacimiento(f.fechaNacimiento());
        p.setSexo(f.sexo());
        p.setTelefono(f.telefono());
        p.setEmail(f.email());
        p.setDireccion(f.direccion());
        p.setOcupacion(f.ocupacion());
        p.setContactoEmergenciaNombre(f.contactoEmergenciaNombre());
        p.setContactoEmergenciaTelefono(f.contactoEmergenciaTelefono());
        boolean menor = Validaciones.esMenorDeEdad(f.fechaNacimiento(), LocalDate.now(reloj));
        p.setApoderadoNombre(menor ? f.apoderadoNombre() : null);
        p.setApoderadoDocumento(menor ? f.apoderadoDocumento() : null);
        p.setApoderadoParentesco(menor ? f.apoderadoParentesco() : null);
        p.setAceptaRecordatorios(f.aceptaRecordatorios());
        p.setActualizadoEn(ahora);

        Paciente guardado = pacienteRepository.save(p);
        log.info("Paciente {} {}", f.id() == null ? "registrado" : "actualizado", guardado.getNumeroHistoria());
        auditoria.registrar(f.id() == null ? "PACIENTE_CREAR" : "PACIENTE_EDITAR", "paciente", guardado.getId(),
                Map.of("historia", guardado.getNumeroHistoria()));
        return guardado.getId();
    }

    @Transactional
    public void cambiarEstado(long id, boolean activo) {
        sesionUsuario.exigir(Permiso.PACIENTE_EDITAR);
        Paciente p = buscarPaciente(id);
        p.setActivo(activo);
        auditoria.registrar(activo ? "PACIENTE_REACTIVAR" : "PACIENTE_DESACTIVAR", "paciente", id, null);
        p.setActualizadoEn(LocalDateTime.now(reloj));
    }

    private Paciente buscarPaciente(long id) {
        return pacienteRepository.findById(id)
                .orElseThrow(() -> new NegocioException("El paciente ya no existe en la base de datos."));
    }

    private static Specification<Paciente> especificacion(String filtro, boolean incluirInactivos) {
        return (root, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (!incluirInactivos) {
                condiciones.add(cb.isTrue(root.get("activo")));
            }
            String limpio = Validador.limpio(filtro);
            if (limpio != null) {
                List<Expression<String>> campos = List.of(
                        cb.lower(root.get("nombres")), cb.lower(root.get("apellidoPaterno")),
                        cb.lower(root.get("apellidoMaterno")), cb.lower(root.get("numeroDocumento")),
                        cb.lower(root.get("numeroHistoria")));
                for (String palabra : limpio.toLowerCase(Locale.ROOT).split(" ")) {
                    String patron = "%" + palabra.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                    condiciones.add(cb.or(campos.stream().map(c -> cb.like(c, patron, '\\')).toArray(Predicate[]::new)));
                }
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
    }

    private static PacienteForm normalizar(PacienteForm f) {
        TipoDocumento tipo = f.tipoDocumento();
        String numero = Validador.limpio(f.numeroDocumento());
        if (tipo == TipoDocumento.SIN_DOCUMENTO) {
            numero = null;
        } else if (numero != null) {
            numero = numero.replace(" ", "").toUpperCase(Locale.ROOT);
        }
        String email = Validador.limpio(f.email());
        return new PacienteForm(f.id(), tipo, numero,
                Validador.limpio(f.nombres()), Validador.limpio(f.apellidoPaterno()), Validador.limpio(f.apellidoMaterno()),
                f.fechaNacimiento(), f.sexo(), Validador.limpio(f.telefono()),
                email == null ? null : email.toLowerCase(Locale.ROOT),
                Validador.limpio(f.direccion()), Validador.limpio(f.ocupacion()),
                Validador.limpio(f.contactoEmergenciaNombre()), Validador.limpio(f.contactoEmergenciaTelefono()),
                Validador.limpio(f.apoderadoNombre()), Validador.limpio(f.apoderadoDocumento()),
                Validador.limpio(f.apoderadoParentesco()), f.aceptaRecordatorios());
    }

    private void validar(PacienteForm f) {
        LocalDate hoy = LocalDate.now(reloj);
        Validador v = new Validador()
                .requerido(f.nombres(), "los nombres")
                .requerido(f.apellidoPaterno(), "el apellido paterno")
                .requerido(f.tipoDocumento(), "el tipo de documento")
                .requerido(f.fechaNacimiento(), "la fecha de nacimiento")
                .requerido(f.sexo(), "el sexo")
                .maximo(f.nombres(), 80, "nombres")
                .maximo(f.apellidoPaterno(), 60, "apellido paterno")
                .maximo(f.apellidoMaterno(), 60, "apellido materno")
                .maximo(f.direccion(), 200, "dirección");

        if (f.tipoDocumento() != null && f.tipoDocumento() != TipoDocumento.SIN_DOCUMENTO) {
            String n = f.numeroDocumento();
            v.requerido(n, "el número de documento");
            if (n != null) {
                switch (f.tipoDocumento()) {
                    case DNI -> v.si(!Validaciones.esDni(n), "El DNI debe tener 8 dígitos.");
                    case CE -> v.si(!Validaciones.esCarneExtranjeria(n), "El carné de extranjería debe tener de 9 a 12 caracteres alfanuméricos.");
                    case PASAPORTE -> v.si(!Validaciones.esPasaporte(n), "El pasaporte debe tener de 6 a 12 caracteres alfanuméricos.");
                    default -> { }
                }
            }
        }
        if (f.fechaNacimiento() != null) {
            v.si(f.fechaNacimiento().isAfter(hoy), "La fecha de nacimiento no puede ser futura.");
            v.si(f.fechaNacimiento().isBefore(hoy.minusYears(EDAD_MAXIMA)), "Revise la fecha de nacimiento (más de " + EDAD_MAXIMA + " años).");
        }
        v.si(f.email() != null && !Validaciones.esEmail(f.email()), "El correo electrónico no es válido.");
        v.si(f.telefono() != null && !Validaciones.esTelefono(f.telefono()), "El teléfono no es válido.");
        v.si(f.contactoEmergenciaTelefono() != null && !Validaciones.esTelefono(f.contactoEmergenciaTelefono()),
                "El teléfono del contacto de emergencia no es válido.");

        boolean menor = f.fechaNacimiento() != null && !f.fechaNacimiento().isAfter(hoy)
                && Validaciones.esMenorDeEdad(f.fechaNacimiento(), hoy);
        if (menor) {
            v.requerido(f.apoderadoNombre(), "el nombre del apoderado (el paciente es menor de edad)");
            v.requerido(f.apoderadoDocumento(), "el documento del apoderado");
            v.requerido(f.apoderadoParentesco(), "el parentesco del apoderado");
        }
        v.lanzarSiHayErrores();

        if (f.numeroDocumento() != null) {
            pacienteRepository.findByTipoDocumentoAndNumeroDocumento(f.tipoDocumento(), f.numeroDocumento())
                    .filter(otro -> !otro.getId().equals(f.id()))
                    .ifPresent(otro -> {
                        throw new NegocioException("Ya existe un paciente con " + f.tipoDocumento().name() + " "
                                + f.numeroDocumento() + " (" + otro.getNumeroHistoria() + ").");
                    });
        }
    }

    private static PacienteResumen resumen(Paciente p, LocalDate hoy, String ultimoDemo) {
        return new PacienteResumen(p.getId(), p.getNumeroHistoria(), documento(p), p.getNombreCompleto(),
                Validaciones.edad(p.getFechaNacimiento(), hoy), p.getTelefono(), p.isActivo(),
                esDemo(p.getNumeroHistoria(), ultimoDemo));
    }

    /**
     * Paciente creado por el seeder de demostración: su historia no pasa de {@code seeder.ultimo_hc_demo}
     * (las historias tienen el mismo largo, {@code HC-000001}, así que se comparan como texto).
     */
    static boolean esDemo(String numeroHistoria, String ultimoDemo) {
        return ultimoDemo != null && !ultimoDemo.isBlank() && numeroHistoria != null
                && numeroHistoria.length() == ultimoDemo.length() && numeroHistoria.compareTo(ultimoDemo) <= 0;
    }

    private String ultimoDemo() {
        return configuracionService.texto(CLAVE_ULTIMO_HC_DEMO);
    }

    private static String documento(Paciente p) {
        return p.getTipoDocumento() == TipoDocumento.SIN_DOCUMENTO
                ? "Sin documento" : p.getTipoDocumento().name() + " " + p.getNumeroDocumento();
    }

    private PacienteDetalle detalle(Paciente p, String alergias, LocalDateTime consentimiento) {
        return new PacienteDetalle(p.getId(), p.getNumeroHistoria(), p.getTipoDocumento(), p.getNumeroDocumento(),
                p.getNombres(), p.getApellidoPaterno(), p.getApellidoMaterno(), p.getFechaNacimiento(), p.getSexo(),
                p.getTelefono(), p.getEmail(), p.getDireccion(), p.getOcupacion(),
                p.getContactoEmergenciaNombre(), p.getContactoEmergenciaTelefono(),
                p.getApoderadoNombre(), p.getApoderadoDocumento(), p.getApoderadoParentesco(),
                p.isAceptaRecordatorios(), p.isActivo(), Validaciones.edad(p.getFechaNacimiento(), LocalDate.now(reloj)),
                alergias, consentimiento, esDemo(p.getNumeroHistoria(), ultimoDemo()));
    }
}
