/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio;

import com.odontogest.dominio.odontograma.CadenaIntegridad;
import com.odontogest.dominio.odontograma.OdontogramaCanonico;
import com.odontogest.dominio.odontograma.ResultadoVerificacion;
import com.odontogest.dominio.odontograma.Superficies;
import com.odontogest.modelo.EstadoOdontogramaRegistro;
import com.odontogest.modelo.Hallazgo;
import com.odontogest.modelo.Odontograma;
import com.odontogest.persistencia.repositorio.HallazgoRepository;
import com.odontogest.persistencia.repositorio.OdontogramaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Cadena de integridad de los odontogramas cerrados de un paciente (RF-ODO-05, RF-ODO-11): calcula el hash de
 * cierre y verifica que ningún odontograma firmado cambió después en la base de datos.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IntegridadService {

    private final OdontogramaRepository odontogramaRepository;
    private final HallazgoRepository hallazgoRepository;

    /** Recalcula toda la cadena del paciente e indica el primer odontograma alterado. */
    public ResultadoVerificacion verificar(long pacienteId) {
        List<OdontogramaCanonico> cadena = odontogramaRepository
                .findByPacienteIdAndEstadoOrderByCerradoEnAscIdAsc(pacienteId, EstadoOdontogramaRegistro.CERRADO).stream()
                .map(o -> canonico(o, hallazgoRepository.listarPorOdontograma(o.getId())))
                .toList();
        return CadenaIntegridad.verificar(cadena);
    }

    /** Hash del último odontograma cerrado del paciente, o {@code GENESIS} si aún no tiene. */
    public String hashAnterior(long pacienteId) {
        List<Odontograma> cerrados = odontogramaRepository
                .findByPacienteIdAndEstadoOrderByCerradoEnAscIdAsc(pacienteId, EstadoOdontogramaRegistro.CERRADO);
        return cerrados.isEmpty() ? CadenaIntegridad.GENESIS : cerrados.get(cerrados.size() - 1).getHashCierre();
    }

    /** Hash de cierre del odontograma con sus datos actuales (cerradoEn y cerradoPor ya asignados). */
    public String calcularHash(Odontograma o, String hashAnterior) {
        return CadenaIntegridad.calcular(hashAnterior, canonico(o, hallazgoRepository.listarPorOdontograma(o.getId())));
    }

    static OdontogramaCanonico canonico(Odontograma o, List<Hallazgo> hallazgos) {
        List<OdontogramaCanonico.Linea> lineas = hallazgos.stream()
                .map(h -> new OdontogramaCanonico.Linea(h.getId(), h.getHallazgoTipo().getCodigo(), h.getPiezaFdi(),
                        h.getPiezaFdiFin(), Superficies.aMascara(h.getSuperficies()),
                        h.getEstado() == null ? null : h.getEstado().name(), h.getSigla(), h.getRegistradoEn(),
                        h.getRegistradoPor()))
                .toList();
        return new OdontogramaCanonico(o.getId(), o.getPaciente().getId(), o.getTipo().name(), o.getDenticion().name(),
                o.getCerradoEn(), o.getCerradoPor() == null ? 0 : o.getCerradoPor(), o.getEspecificaciones(), lineas,
                o.getHashAnterior(), o.getHashCierre());
    }
}
