/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.odontogest.excepcion.NegocioException;
import com.odontogest.modelo.Anamnesis;
import com.odontogest.modelo.Paciente;
import com.odontogest.modelo.Sexo;
import com.odontogest.persistencia.repositorio.AnamnesisRepository;
import com.odontogest.persistencia.repositorio.PacienteRepository;
import com.odontogest.seguridad.Permiso;
import com.odontogest.seguridad.SesionUsuario;
import com.odontogest.servicio.dto.AnamnesisDto;
import com.odontogest.servicio.dto.OpcionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Anamnesis del paciente (RF-PAC-04): antecedentes, alergias, medicación, checklist de enfermedades
 * sistémicas, hábitos y presión arterial. Las alergias se muestran como alerta en la ficha.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnamnesisService {

    /** Checklist de enfermedades sistémicas: clave guardada en el JSON → texto visible. */
    public static final Map<String, String> ENFERMEDADES = enfermedades();

    private static final Pattern PRESION = Pattern.compile("\\d{2,3}/\\d{2,3}");
    private static final Type TIPO_MAPA = new TypeToken<Map<String, Boolean>>() { }.getType();
    private static final Gson GSON = new Gson();

    private final AnamnesisRepository anamnesisRepository;
    private final PacienteRepository pacienteRepository;
    private final SesionUsuario sesionUsuario;

    public List<OpcionDto> enfermedadesDisponibles() {
        return ENFERMEDADES.entrySet().stream().map(e -> new OpcionDto(e.getKey(), e.getValue())).toList();
    }

    /** Anamnesis del paciente; si aún no tiene, devuelve una vacía. */
    public AnamnesisDto obtener(long pacienteId) {
        if (!pacienteRepository.existsById(pacienteId)) {
            throw new NegocioException("El paciente ya no existe en la base de datos.");
        }
        return anamnesisRepository.findByPacienteId(pacienteId)
                .map(a -> new AnamnesisDto(pacienteId, a.getAntecedentesMedicos(), a.getAlergias(),
                        a.getMedicacionActual(), leerEnfermedades(a.getEnfermedadesJson()), a.getHabitos(),
                        a.isFuma(), a.isGestante(), a.getPresionArterial(), a.getObservaciones(), a.getActualizadoEn()))
                .orElseGet(() -> new AnamnesisDto(pacienteId, null, null, null, Map.of(), null,
                        false, false, null, null, null));
    }

    @Transactional
    public AnamnesisDto guardar(AnamnesisDto dto) {
        long usuarioId = sesionUsuario.exigir(Permiso.ANAMNESIS_EDITAR).id();
        Paciente paciente = pacienteRepository.findById(dto.pacienteId())
                .orElseThrow(() -> new NegocioException("El paciente ya no existe en la base de datos."));
        String presion = Validador.limpio(dto.presionArterial());
        new Validador()
                .si(presion != null && !PRESION.matcher(presion).matches(),
                        "La presión arterial debe escribirse como sistólica/diastólica, p. ej. 120/80.")
                .si(dto.gestante() && paciente.getSexo() != Sexo.F, "La condición de gestante solo aplica a pacientes de sexo femenino.")
                .si(!ENFERMEDADES.keySet().containsAll(dto.enfermedades().keySet()), "Enfermedad no reconocida en el checklist.")
                .lanzarSiHayErrores();

        Anamnesis a = anamnesisRepository.findByPacienteId(paciente.getId()).orElseGet(() -> {
            Anamnesis nueva = new Anamnesis();
            nueva.setPaciente(paciente);
            return nueva;
        });
        a.setAntecedentesMedicos(Validador.limpioMultilinea(dto.antecedentesMedicos()));
        a.setAlergias(Validador.limpioMultilinea(dto.alergias()));
        a.setMedicacionActual(Validador.limpioMultilinea(dto.medicacionActual()));
        a.setEnfermedadesJson(escribirEnfermedades(dto.enfermedades()));
        a.setHabitos(Validador.limpioMultilinea(dto.habitos()));
        a.setFuma(dto.fuma());
        a.setGestante(dto.gestante());
        a.setPresionArterial(presion);
        a.setObservaciones(Validador.limpioMultilinea(dto.observaciones()));
        a.setActualizadoPor(usuarioId);
        a.setActualizadoEn(LocalDateTime.now());
        anamnesisRepository.save(a);
        return obtener(paciente.getId());
    }

    private static Map<String, Boolean> leerEnfermedades(String json) {
        Map<String, Boolean> deJson = json == null || json.isBlank() ? null : GSON.fromJson(json, TIPO_MAPA);
        Map<String, Boolean> leidas = deJson == null ? Map.of() : deJson;
        Map<String, Boolean> resultado = new LinkedHashMap<>();
        ENFERMEDADES.keySet().forEach(k -> resultado.put(k, Boolean.TRUE.equals(leidas.get(k))));
        return resultado;
    }

    /** Guarda todas las claves conocidas (true/false) en orden fijo. */
    private static String escribirEnfermedades(Map<String, Boolean> marcadas) {
        Map<String, Boolean> completo = new LinkedHashMap<>();
        ENFERMEDADES.keySet().forEach(k -> completo.put(k, Boolean.TRUE.equals(marcadas.get(k))));
        return GSON.toJson(completo);
    }

    private static Map<String, String> enfermedades() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("diabetes", "Diabetes");
        m.put("hipertension", "Hipertensión arterial");
        m.put("cardiopatia", "Cardiopatía");
        m.put("asma", "Asma");
        m.put("epilepsia", "Epilepsia");
        m.put("hepatitis", "Hepatitis");
        m.put("vih", "VIH");
        m.put("tuberculosis", "Tuberculosis");
        m.put("coagulopatia", "Trastornos de coagulación");
        m.put("renal", "Enfermedad renal");
        m.put("tiroides", "Enfermedad tiroidea");
        m.put("cancer", "Cáncer");
        return Collections.unmodifiableMap(m);
    }
}
