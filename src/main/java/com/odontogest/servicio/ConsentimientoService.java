/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio;

import com.odontogest.documento.ConsentimientoPdf;
import com.odontogest.excepcion.NegocioException;
import com.odontogest.modelo.Consentimiento;
import com.odontogest.modelo.Paciente;
import com.odontogest.modelo.TipoConsentimiento;
import com.odontogest.persistencia.repositorio.ConsentimientoRepository;
import com.odontogest.persistencia.repositorio.PacienteRepository;
import com.odontogest.seguridad.Permiso;
import com.odontogest.seguridad.SesionUsuario;
import com.odontogest.servicio.dto.ConsentimientoDto;
import com.odontogest.servicio.dto.PacienteDetalle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Consentimientos del paciente (RF-PAC-05). Fase 2: consentimiento para el tratamiento de datos personales
 * (Ley N° 29733) imprimible en PDF y registro de su firma. El consentimiento informado de tratamiento se
 * agrega en la Fase 4 junto con el plan.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ConsentimientoService {

    private final ConsentimientoRepository consentimientoRepository;
    private final PacienteRepository pacienteRepository;
    private final PacienteService pacienteService;
    private final ConfiguracionService configuracionService;
    private final ConsentimientoPdf consentimientoPdf;
    private final SesionUsuario sesionUsuario;

    public List<ConsentimientoDto> listar(long pacienteId) {
        return consentimientoRepository.findByPacienteIdOrderByAceptadoEnDesc(pacienteId).stream()
                .map(c -> new ConsentimientoDto(c.getId(), c.getTipo(), c.getAceptadoEn(), c.getFirmadoPor()))
                .toList();
    }

    /** Documento para imprimir y firmar. */
    public byte[] pdfDatosPersonales(long pacienteId) {
        PacienteDetalle paciente = pacienteService.obtener(pacienteId);
        return consentimientoPdf.datosPersonales(configuracionService.datosClinica(), paciente, LocalDate.now());
    }

    /**
     * Registra que el paciente (o su apoderado, si es menor) firmó el consentimiento de datos personales.
     *
     * @param firmadoPor nombre de quien firma
     */
    @Transactional
    public ConsentimientoDto registrarDatosPersonales(long pacienteId, String firmadoPor) {
        long usuarioId = sesionUsuario.exigir(Permiso.PACIENTE_EDITAR).id();
        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new NegocioException("El paciente ya no existe en la base de datos."));
        String firmante = Validador.limpio(firmadoPor);
        new Validador().requerido(firmante, "el nombre de quien firma").maximo(firmante, 120, "firmado por")
                .lanzarSiHayErrores();

        Consentimiento c = new Consentimiento();
        c.setPaciente(paciente);
        c.setTipo(TipoConsentimiento.DATOS_PERSONALES);
        c.setAceptadoEn(LocalDateTime.now());
        c.setFirmadoPor(firmante);
        c.setRegistradoPor(usuarioId);
        Consentimiento guardado = consentimientoRepository.save(c);
        log.info("Consentimiento de datos registrado para {}", paciente.getNumeroHistoria());
        return new ConsentimientoDto(guardado.getId(), guardado.getTipo(), guardado.getAceptadoEn(), guardado.getFirmadoPor());
    }
}
