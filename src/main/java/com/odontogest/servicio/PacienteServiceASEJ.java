package com.odontogest.servicio;

import com.odontogest.dominio.comun.ValidacionesASEJ;
import com.odontogest.excepcion.NegocioExceptionASEJ;
import com.odontogest.modelo.AnamnesisASEJ;
import com.odontogest.modelo.ConsentimientoASEJ;
import com.odontogest.modelo.PacienteASEJ;
import com.odontogest.modelo.TipoConsentimientoASEJ;
import com.odontogest.modelo.TipoDocumentoASEJ;
import com.odontogest.persistencia.repositorio.AnamnesisRepositoryASEJ;
import com.odontogest.persistencia.repositorio.ConsentimientoRepositoryASEJ;
import com.odontogest.persistencia.repositorio.PacienteRepositoryASEJ;
import com.odontogest.seguridad.PermisoASEJ;
import com.odontogest.seguridad.SesionUsuarioASEJ;
import com.odontogest.servicio.dto.PaginaASEJ;
import com.odontogest.servicio.dto.PacienteDetalleASEJ;
import com.odontogest.servicio.dto.PacienteFormASEJ;
import com.odontogest.servicio.dto.PacienteResumenASEJ;
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

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PacienteServiceASEJ {

    public static final int TAMANIO_PAGINAASEJ = 15;
    private static final int EDAD_MAXIMAASEJ = 120;

    public static final String CLAVE_ULTIMO_HC_DEMOASEJ = "seeder.ultimo_hc_demo";

    private final Clock relojASEJ;
    private final ConfiguracionServiceASEJ configuracionServiceASEJ;
    private final PacienteRepositoryASEJ pacienteRepositoryASEJ;
    private final AuditoriaServiceASEJ auditoriaASEJ;
    private final AnamnesisRepositoryASEJ anamnesisRepositoryASEJ;
    private final ConsentimientoRepositoryASEJ consentimientoRepositoryASEJ;
    private final CorrelativoServiceASEJ correlativoServiceASEJ;
    private final SesionUsuarioASEJ sesionUsuarioASEJ;

    public PaginaASEJ<PacienteResumenASEJ> buscarASEJ(String filtroASEJ, boolean incluirInactivosASEJ, int paginaASEJ) {
        PageRequest solicitudASEJ = PageRequest.of(Math.max(0, paginaASEJ), TAMANIO_PAGINAASEJ,
                Sort.by("apellidoPaternoASEJ", "apellidoMaternoASEJ", "nombresASEJ", "idASEJ"));
        Page<PacienteASEJ> resultadoASEJ = pacienteRepositoryASEJ.findAll(especificacionASEJ(filtroASEJ, incluirInactivosASEJ), solicitudASEJ);
        LocalDate hoyASEJ = LocalDate.now(relojASEJ);
        String ultimoDemoASEJ = ultimoDemoASEJ();
        List<PacienteResumenASEJ> filasASEJ = resultadoASEJ.getContent().stream().map(pASEJ -> resumenASEJ(pASEJ, hoyASEJ, ultimoDemoASEJ)).toList();
        return new PaginaASEJ<>(filasASEJ, resultadoASEJ.getNumber(), TAMANIO_PAGINAASEJ, resultadoASEJ.getTotalElements());
    }

    public PacienteDetalleASEJ obtenerASEJ(long idASEJ) {
        PacienteASEJ pASEJ = buscarPacienteASEJ(idASEJ);
        String alergiasASEJ = anamnesisRepositoryASEJ.findByPacienteASEJIdASEJ(idASEJ).map(AnamnesisASEJ::getAlergiasASEJ).orElse(null);
        LocalDateTime consentimientoASEJ = consentimientoRepositoryASEJ
                .findFirstByPacienteASEJIdASEJAndTipoASEJOrderByAceptadoEnASEJDesc(idASEJ, TipoConsentimientoASEJ.DATOS_PERSONALES)
                .map(ConsentimientoASEJ::getAceptadoEnASEJ).orElse(null);
        return detalleASEJ(pASEJ, alergiasASEJ, consentimientoASEJ);
    }

    @Transactional
    public long guardarASEJ(PacienteFormASEJ formASEJ) {
        sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.PACIENTE_EDITAR);
        PacienteFormASEJ fASEJ = normalizarASEJ(formASEJ);
        validarASEJ(fASEJ);

        PacienteASEJ pASEJ = fASEJ.idASEJ() == null ? new PacienteASEJ() : buscarPacienteASEJ(fASEJ.idASEJ());
        LocalDateTime ahoraASEJ = LocalDateTime.now(relojASEJ);
        if (pASEJ.getIdASEJ() == null) {
            pASEJ.setNumeroHistoriaASEJ(correlativoServiceASEJ.siguienteHistoriaClinicaASEJ());
            pASEJ.setCreadoEnASEJ(ahoraASEJ);
            pASEJ.setActivoASEJ(true);
        }
        pASEJ.setTipoDocumentoASEJ(fASEJ.tipoDocumentoASEJ());
        pASEJ.setNumeroDocumentoASEJ(fASEJ.numeroDocumentoASEJ());
        pASEJ.setNombresASEJ(fASEJ.nombresASEJ());
        pASEJ.setApellidoPaternoASEJ(fASEJ.apellidoPaternoASEJ());
        pASEJ.setApellidoMaternoASEJ(fASEJ.apellidoMaternoASEJ());
        pASEJ.setFechaNacimientoASEJ(fASEJ.fechaNacimientoASEJ());
        pASEJ.setSexoASEJ(fASEJ.sexoASEJ());
        pASEJ.setTelefonoASEJ(fASEJ.telefonoASEJ());
        pASEJ.setEmailASEJ(fASEJ.emailASEJ());
        pASEJ.setDireccionASEJ(fASEJ.direccionASEJ());
        pASEJ.setOcupacionASEJ(fASEJ.ocupacionASEJ());
        pASEJ.setContactoEmergenciaNombreASEJ(fASEJ.contactoEmergenciaNombreASEJ());
        pASEJ.setContactoEmergenciaTelefonoASEJ(fASEJ.contactoEmergenciaTelefonoASEJ());
        boolean menorASEJ = ValidacionesASEJ.esMenorDeEdadASEJ(fASEJ.fechaNacimientoASEJ(), LocalDate.now(relojASEJ));
        pASEJ.setApoderadoNombreASEJ(menorASEJ ? fASEJ.apoderadoNombreASEJ() : null);
        pASEJ.setApoderadoDocumentoASEJ(menorASEJ ? fASEJ.apoderadoDocumentoASEJ() : null);
        pASEJ.setApoderadoParentescoASEJ(menorASEJ ? fASEJ.apoderadoParentescoASEJ() : null);
        pASEJ.setAceptaRecordatoriosASEJ(fASEJ.aceptaRecordatoriosASEJ());
        pASEJ.setActualizadoEnASEJ(ahoraASEJ);

        PacienteASEJ guardadoASEJ = pacienteRepositoryASEJ.save(pASEJ);
        logASEJ.info("Paciente {} {}", fASEJ.idASEJ() == null ? "registrado" : "actualizado", guardadoASEJ.getNumeroHistoriaASEJ());
        auditoriaASEJ.registrarASEJ(fASEJ.idASEJ() == null ? "PACIENTE_CREAR" : "PACIENTE_EDITAR", "paciente", guardadoASEJ.getIdASEJ(),
                Map.of("historia", guardadoASEJ.getNumeroHistoriaASEJ()));
        return guardadoASEJ.getIdASEJ();
    }

    @Transactional
    public void cambiarEstadoASEJ(long idASEJ, boolean activoASEJ) {
        sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.PACIENTE_EDITAR);
        PacienteASEJ pASEJ = buscarPacienteASEJ(idASEJ);
        pASEJ.setActivoASEJ(activoASEJ);
        auditoriaASEJ.registrarASEJ(activoASEJ ? "PACIENTE_REACTIVAR" : "PACIENTE_DESACTIVAR", "paciente", idASEJ, null);
        pASEJ.setActualizadoEnASEJ(LocalDateTime.now(relojASEJ));
    }

    private PacienteASEJ buscarPacienteASEJ(long idASEJ) {
        return pacienteRepositoryASEJ.findById(idASEJ)
                .orElseThrow(() -> new NegocioExceptionASEJ("El paciente ya no existe en la base de datos."));
    }

    private static Specification<PacienteASEJ> especificacionASEJ(String filtroASEJ, boolean incluirInactivosASEJ) {
        return (rootASEJ, consultaASEJ, cbASEJ) -> {
            List<Predicate> condicionesASEJ = new ArrayList<>();
            if (!incluirInactivosASEJ) {
                condicionesASEJ.add(cbASEJ.isTrue(rootASEJ.get("activoASEJ")));
            }
            String limpioASEJ = ValidadorASEJ.limpioASEJ(filtroASEJ);
            if (limpioASEJ != null) {
                List<Expression<String>> camposASEJ = List.of(
                        cbASEJ.lower(rootASEJ.get("nombresASEJ")), cbASEJ.lower(rootASEJ.get("apellidoPaternoASEJ")),
                        cbASEJ.lower(rootASEJ.get("apellidoMaternoASEJ")), cbASEJ.lower(rootASEJ.get("numeroDocumentoASEJ")),
                        cbASEJ.lower(rootASEJ.get("numeroHistoriaASEJ")));
                for (String palabraASEJ : limpioASEJ.toLowerCase(Locale.ROOT).split(" ")) {
                    String patronASEJ = "%" + palabraASEJ.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                    condicionesASEJ.add(cbASEJ.or(camposASEJ.stream().map(cASEJ -> cbASEJ.like(cASEJ, patronASEJ, '\\')).toArray(Predicate[]::new)));
                }
            }
            return cbASEJ.and(condicionesASEJ.toArray(Predicate[]::new));
        };
    }

    private static PacienteFormASEJ normalizarASEJ(PacienteFormASEJ fASEJ) {
        TipoDocumentoASEJ tipoASEJ = fASEJ.tipoDocumentoASEJ();
        String numeroASEJ = ValidadorASEJ.limpioASEJ(fASEJ.numeroDocumentoASEJ());
        if (tipoASEJ == TipoDocumentoASEJ.SIN_DOCUMENTO) {
            numeroASEJ = null;
        } else if (numeroASEJ != null) {
            numeroASEJ = numeroASEJ.replace(" ", "").toUpperCase(Locale.ROOT);
        }
        String emailASEJ = ValidadorASEJ.limpioASEJ(fASEJ.emailASEJ());
        return new PacienteFormASEJ(fASEJ.idASEJ(), tipoASEJ, numeroASEJ,
                ValidadorASEJ.limpioASEJ(fASEJ.nombresASEJ()), ValidadorASEJ.limpioASEJ(fASEJ.apellidoPaternoASEJ()), ValidadorASEJ.limpioASEJ(fASEJ.apellidoMaternoASEJ()),
                fASEJ.fechaNacimientoASEJ(), fASEJ.sexoASEJ(), ValidadorASEJ.limpioASEJ(fASEJ.telefonoASEJ()),
                emailASEJ == null ? null : emailASEJ.toLowerCase(Locale.ROOT),
                ValidadorASEJ.limpioASEJ(fASEJ.direccionASEJ()), ValidadorASEJ.limpioASEJ(fASEJ.ocupacionASEJ()),
                ValidadorASEJ.limpioASEJ(fASEJ.contactoEmergenciaNombreASEJ()), ValidadorASEJ.limpioASEJ(fASEJ.contactoEmergenciaTelefonoASEJ()),
                ValidadorASEJ.limpioASEJ(fASEJ.apoderadoNombreASEJ()), ValidadorASEJ.limpioASEJ(fASEJ.apoderadoDocumentoASEJ()),
                ValidadorASEJ.limpioASEJ(fASEJ.apoderadoParentescoASEJ()), fASEJ.aceptaRecordatoriosASEJ());
    }

    private void validarASEJ(PacienteFormASEJ fASEJ) {
        LocalDate hoyASEJ = LocalDate.now(relojASEJ);
        ValidadorASEJ vASEJ = new ValidadorASEJ()
                .requeridoASEJ(fASEJ.nombresASEJ(), "los nombres")
                .requeridoASEJ(fASEJ.apellidoPaternoASEJ(), "el apellido paterno")
                .requeridoASEJ(fASEJ.tipoDocumentoASEJ(), "el tipo de documento")
                .requeridoASEJ(fASEJ.fechaNacimientoASEJ(), "la fecha de nacimiento")
                .requeridoASEJ(fASEJ.sexoASEJ(), "el sexo")
                .maximoASEJ(fASEJ.nombresASEJ(), 80, "nombres")
                .maximoASEJ(fASEJ.apellidoPaternoASEJ(), 60, "apellido paterno")
                .maximoASEJ(fASEJ.apellidoMaternoASEJ(), 60, "apellido materno")
                .maximoASEJ(fASEJ.direccionASEJ(), 200, "dirección");

        if (fASEJ.tipoDocumentoASEJ() != null && fASEJ.tipoDocumentoASEJ() != TipoDocumentoASEJ.SIN_DOCUMENTO) {
            String nASEJ = fASEJ.numeroDocumentoASEJ();
            vASEJ.requeridoASEJ(nASEJ, "el número de documento");
            if (nASEJ != null) {
                switch (fASEJ.tipoDocumentoASEJ()) {
                    case DNI -> vASEJ.siASEJ(!ValidacionesASEJ.esDniASEJ(nASEJ), "El DNI debe tener 8 dígitos.");
                    case CE -> vASEJ.siASEJ(!ValidacionesASEJ.esCarneExtranjeriaASEJ(nASEJ), "El carné de extranjería debe tener de 9 a 12 caracteres alfanuméricos.");
                    case PASAPORTE -> vASEJ.siASEJ(!ValidacionesASEJ.esPasaporteASEJ(nASEJ), "El pasaporte debe tener de 6 a 12 caracteres alfanuméricos.");
                    default -> { }
                }
            }
        }
        if (fASEJ.fechaNacimientoASEJ() != null) {
            vASEJ.siASEJ(fASEJ.fechaNacimientoASEJ().isAfter(hoyASEJ), "La fecha de nacimiento no puede ser futura.");
            vASEJ.siASEJ(fASEJ.fechaNacimientoASEJ().isBefore(hoyASEJ.minusYears(EDAD_MAXIMAASEJ)), "Revise la fecha de nacimiento (más de " + EDAD_MAXIMAASEJ + " años).");
        }
        vASEJ.siASEJ(fASEJ.emailASEJ() != null && !ValidacionesASEJ.esEmailASEJ(fASEJ.emailASEJ()), "El correo electrónico no es válido.");
        vASEJ.siASEJ(fASEJ.telefonoASEJ() != null && !ValidacionesASEJ.esTelefonoASEJ(fASEJ.telefonoASEJ()), "El teléfono no es válido.");
        vASEJ.siASEJ(fASEJ.contactoEmergenciaTelefonoASEJ() != null && !ValidacionesASEJ.esTelefonoASEJ(fASEJ.contactoEmergenciaTelefonoASEJ()),
                "El teléfono del contacto de emergencia no es válido.");

        boolean menorASEJ = fASEJ.fechaNacimientoASEJ() != null && !fASEJ.fechaNacimientoASEJ().isAfter(hoyASEJ)
                && ValidacionesASEJ.esMenorDeEdadASEJ(fASEJ.fechaNacimientoASEJ(), hoyASEJ);
        if (menorASEJ) {
            vASEJ.requeridoASEJ(fASEJ.apoderadoNombreASEJ(), "el nombre del apoderado (el paciente es menor de edad)");
            vASEJ.requeridoASEJ(fASEJ.apoderadoDocumentoASEJ(), "el documento del apoderado");
            vASEJ.requeridoASEJ(fASEJ.apoderadoParentescoASEJ(), "el parentesco del apoderado");
        }
        vASEJ.lanzarSiHayErroresASEJ();

        if (fASEJ.numeroDocumentoASEJ() != null) {
            pacienteRepositoryASEJ.findByTipoDocumentoASEJAndNumeroDocumentoASEJ(fASEJ.tipoDocumentoASEJ(), fASEJ.numeroDocumentoASEJ())
                    .filter(otroASEJ -> !otroASEJ.getIdASEJ().equals(fASEJ.idASEJ()))
                    .ifPresent(otroASEJ -> {
                        throw new NegocioExceptionASEJ("Ya existe un paciente con " + fASEJ.tipoDocumentoASEJ().name() + " "
                                + fASEJ.numeroDocumentoASEJ() + " (" + otroASEJ.getNumeroHistoriaASEJ() + ").");
                    });
        }
    }

    private static PacienteResumenASEJ resumenASEJ(PacienteASEJ pASEJ, LocalDate hoyASEJ, String ultimoDemoASEJ) {
        return new PacienteResumenASEJ(pASEJ.getIdASEJ(), pASEJ.getNumeroHistoriaASEJ(), documentoASEJ(pASEJ), pASEJ.getNombreCompletoASEJ(),
                ValidacionesASEJ.edadASEJ(pASEJ.getFechaNacimientoASEJ(), hoyASEJ), pASEJ.getTelefonoASEJ(), pASEJ.isActivoASEJ(),
                esDemoASEJ(pASEJ.getNumeroHistoriaASEJ(), ultimoDemoASEJ));
    }

    static boolean esDemoASEJ(String numeroHistoriaASEJ, String ultimoDemoASEJ) {
        return ultimoDemoASEJ != null && !ultimoDemoASEJ.isBlank() && numeroHistoriaASEJ != null
                && numeroHistoriaASEJ.length() == ultimoDemoASEJ.length() && numeroHistoriaASEJ.compareTo(ultimoDemoASEJ) <= 0;
    }

    private String ultimoDemoASEJ() {
        return configuracionServiceASEJ.textoASEJ(CLAVE_ULTIMO_HC_DEMOASEJ);
    }

    private static String documentoASEJ(PacienteASEJ pASEJ) {
        return pASEJ.getTipoDocumentoASEJ() == TipoDocumentoASEJ.SIN_DOCUMENTO
                ? "Sin documento" : pASEJ.getTipoDocumentoASEJ().name() + " " + pASEJ.getNumeroDocumentoASEJ();
    }

    private PacienteDetalleASEJ detalleASEJ(PacienteASEJ pASEJ, String alergiasASEJ, LocalDateTime consentimientoASEJ) {
        return new PacienteDetalleASEJ(pASEJ.getIdASEJ(), pASEJ.getNumeroHistoriaASEJ(), pASEJ.getTipoDocumentoASEJ(), pASEJ.getNumeroDocumentoASEJ(),
                pASEJ.getNombresASEJ(), pASEJ.getApellidoPaternoASEJ(), pASEJ.getApellidoMaternoASEJ(), pASEJ.getFechaNacimientoASEJ(), pASEJ.getSexoASEJ(),
                pASEJ.getTelefonoASEJ(), pASEJ.getEmailASEJ(), pASEJ.getDireccionASEJ(), pASEJ.getOcupacionASEJ(),
                pASEJ.getContactoEmergenciaNombreASEJ(), pASEJ.getContactoEmergenciaTelefonoASEJ(),
                pASEJ.getApoderadoNombreASEJ(), pASEJ.getApoderadoDocumentoASEJ(), pASEJ.getApoderadoParentescoASEJ(),
                pASEJ.isAceptaRecordatoriosASEJ(), pASEJ.isActivoASEJ(), ValidacionesASEJ.edadASEJ(pASEJ.getFechaNacimientoASEJ(), LocalDate.now(relojASEJ)),
                alergiasASEJ, consentimientoASEJ, esDemoASEJ(pASEJ.getNumeroHistoriaASEJ(), ultimoDemoASEJ()));
    }
}
