package com.odontogest.servicio;

import com.odontogest.documento.ConsentimientoPdfASEJ;
import com.odontogest.excepcion.NegocioExceptionASEJ;
import com.odontogest.modelo.ConsentimientoASEJ;
import com.odontogest.modelo.PacienteASEJ;
import com.odontogest.modelo.TipoConsentimientoASEJ;
import com.odontogest.persistencia.repositorio.ConsentimientoRepositoryASEJ;
import com.odontogest.persistencia.repositorio.PacienteRepositoryASEJ;
import com.odontogest.seguridad.PermisoASEJ;
import com.odontogest.seguridad.SesionUsuarioASEJ;
import com.odontogest.servicio.dto.ConsentimientoDtoASEJ;
import com.odontogest.servicio.dto.PacienteDetalleASEJ;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ConsentimientoServiceASEJ {

    private final Clock relojASEJ;
    private final ConsentimientoRepositoryASEJ consentimientoRepositoryASEJ;
    private final AuditoriaServiceASEJ auditoriaASEJ;
    private final PacienteRepositoryASEJ pacienteRepositoryASEJ;
    private final PacienteServiceASEJ pacienteServiceASEJ;
    private final ConfiguracionServiceASEJ configuracionServiceASEJ;
    private final ConsentimientoPdfASEJ consentimientoPdfASEJ;
    private final SesionUsuarioASEJ sesionUsuarioASEJ;

    public List<ConsentimientoDtoASEJ> listarASEJ(long pacienteIdASEJ) {
        return consentimientoRepositoryASEJ.findByPacienteASEJIdASEJOrderByAceptadoEnASEJDesc(pacienteIdASEJ).stream()
                .map(cASEJ -> new ConsentimientoDtoASEJ(cASEJ.getIdASEJ(), cASEJ.getTipoASEJ(), cASEJ.getAceptadoEnASEJ(), cASEJ.getFirmadoPorASEJ()))
                .toList();
    }

    public byte[] pdfDatosPersonalesASEJ(long pacienteIdASEJ) {
        PacienteDetalleASEJ pacienteASEJ = pacienteServiceASEJ.obtenerASEJ(pacienteIdASEJ);
        return consentimientoPdfASEJ.datosPersonalesASEJ(configuracionServiceASEJ.datosClinicaASEJ(), pacienteASEJ, LocalDate.now(relojASEJ));
    }

    @Transactional
    public ConsentimientoDtoASEJ registrarDatosPersonalesASEJ(long pacienteIdASEJ, String firmadoPorASEJ) {
        long usuarioIdASEJ = sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.PACIENTE_EDITAR).idASEJ();
        PacienteASEJ pacienteASEJ = pacienteRepositoryASEJ.findById(pacienteIdASEJ)
                .orElseThrow(() -> new NegocioExceptionASEJ("El paciente ya no existe en la base de datos."));
        String firmanteASEJ = ValidadorASEJ.limpioASEJ(firmadoPorASEJ);
        new ValidadorASEJ().requeridoASEJ(firmanteASEJ, "el nombre de quien firma").maximoASEJ(firmanteASEJ, 120, "firmado por")
                .lanzarSiHayErroresASEJ();

        ConsentimientoASEJ cASEJ = new ConsentimientoASEJ();
        cASEJ.setPacienteASEJ(pacienteASEJ);
        cASEJ.setTipoASEJ(TipoConsentimientoASEJ.DATOS_PERSONALES);
        cASEJ.setAceptadoEnASEJ(LocalDateTime.now(relojASEJ));
        cASEJ.setFirmadoPorASEJ(firmanteASEJ);
        cASEJ.setRegistradoPorASEJ(usuarioIdASEJ);
        ConsentimientoASEJ guardadoASEJ = consentimientoRepositoryASEJ.save(cASEJ);
        logASEJ.info("Consentimiento de datos registrado para {}", pacienteASEJ.getNumeroHistoriaASEJ());
        auditoriaASEJ.registrarASEJ("CONSENTIMIENTO_DATOS", "paciente", pacienteASEJ.getIdASEJ(), Map.of("firmadoPor", firmanteASEJ));
        return new ConsentimientoDtoASEJ(guardadoASEJ.getIdASEJ(), guardadoASEJ.getTipoASEJ(), guardadoASEJ.getAceptadoEnASEJ(), guardadoASEJ.getFirmadoPorASEJ());
    }
}
