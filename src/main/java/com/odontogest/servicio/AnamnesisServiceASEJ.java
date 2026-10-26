package com.odontogest.servicio;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.odontogest.excepcion.NegocioExceptionASEJ;
import com.odontogest.modelo.AnamnesisASEJ;
import com.odontogest.modelo.PacienteASEJ;
import com.odontogest.modelo.SexoASEJ;
import com.odontogest.persistencia.repositorio.AnamnesisRepositoryASEJ;
import com.odontogest.persistencia.repositorio.PacienteRepositoryASEJ;
import com.odontogest.seguridad.PermisoASEJ;
import com.odontogest.seguridad.SesionUsuarioASEJ;
import com.odontogest.servicio.dto.AnamnesisDtoASEJ;
import com.odontogest.servicio.dto.OpcionDtoASEJ;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Type;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnamnesisServiceASEJ {

    public static final Map<String, String> ENFERMEDADESASEJ = enfermedadesASEJ();

    private static final Pattern PRESIONASEJ = Pattern.compile("\\d{2,3}/\\d{2,3}");
    private static final Type TIPO_MAPAASEJ = new TypeToken<Map<String, Boolean>>() { }.getType();
    private static final Gson GSONASEJ = new Gson();

    private final Clock relojASEJ;
    private final AnamnesisRepositoryASEJ anamnesisRepositoryASEJ;
    private final PacienteRepositoryASEJ pacienteRepositoryASEJ;
    private final SesionUsuarioASEJ sesionUsuarioASEJ;

    public List<OpcionDtoASEJ> enfermedadesDisponiblesASEJ() {
        return ENFERMEDADESASEJ.entrySet().stream().map(eASEJ -> new OpcionDtoASEJ(eASEJ.getKey(), eASEJ.getValue())).toList();
    }

    public AnamnesisDtoASEJ obtenerASEJ(long pacienteIdASEJ) {
        if (!pacienteRepositoryASEJ.existsById(pacienteIdASEJ)) {
            throw new NegocioExceptionASEJ("El paciente ya no existe en la base de datos.");
        }
        return anamnesisRepositoryASEJ.findByPacienteASEJIdASEJ(pacienteIdASEJ)
                .map(aASEJ -> new AnamnesisDtoASEJ(pacienteIdASEJ, aASEJ.getAntecedentesMedicosASEJ(), aASEJ.getAlergiasASEJ(),
                        aASEJ.getMedicacionActualASEJ(), leerEnfermedadesASEJ(aASEJ.getEnfermedadesJsonASEJ()), aASEJ.getHabitosASEJ(),
                        aASEJ.isFumaASEJ(), aASEJ.isGestanteASEJ(), aASEJ.getPresionArterialASEJ(), aASEJ.getObservacionesASEJ(), aASEJ.getActualizadoEnASEJ()))
                .orElseGet(() -> new AnamnesisDtoASEJ(pacienteIdASEJ, null, null, null, Map.of(), null,
                        false, false, null, null, null));
    }

    @Transactional
    public AnamnesisDtoASEJ guardarASEJ(AnamnesisDtoASEJ dtoASEJ) {
        long usuarioIdASEJ = sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.ANAMNESIS_EDITAR).idASEJ();
        PacienteASEJ pacienteASEJ = pacienteRepositoryASEJ.findById(dtoASEJ.pacienteIdASEJ())
                .orElseThrow(() -> new NegocioExceptionASEJ("El paciente ya no existe en la base de datos."));
        String presionASEJ = ValidadorASEJ.limpioASEJ(dtoASEJ.presionArterialASEJ());
        new ValidadorASEJ()
                .siASEJ(presionASEJ != null && !PRESIONASEJ.matcher(presionASEJ).matches(),
                        "La presión arterial debe escribirse como sistólica/diastólica, p. ej. 120/80.")
                .siASEJ(dtoASEJ.gestanteASEJ() && pacienteASEJ.getSexoASEJ() != SexoASEJ.F, "La condición de gestante solo aplica a pacientes de sexo femenino.")
                .siASEJ(!ENFERMEDADESASEJ.keySet().containsAll(dtoASEJ.enfermedadesASEJ().keySet()), "Enfermedad no reconocida en el checklist.")
                .lanzarSiHayErroresASEJ();

        AnamnesisASEJ aASEJ = anamnesisRepositoryASEJ.findByPacienteASEJIdASEJ(pacienteASEJ.getIdASEJ()).orElseGet(() -> {
            AnamnesisASEJ nuevaASEJ = new AnamnesisASEJ();
            nuevaASEJ.setPacienteASEJ(pacienteASEJ);
            return nuevaASEJ;
        });
        aASEJ.setAntecedentesMedicosASEJ(ValidadorASEJ.limpioMultilineaASEJ(dtoASEJ.antecedentesMedicosASEJ()));
        aASEJ.setAlergiasASEJ(ValidadorASEJ.limpioMultilineaASEJ(dtoASEJ.alergiasASEJ()));
        aASEJ.setMedicacionActualASEJ(ValidadorASEJ.limpioMultilineaASEJ(dtoASEJ.medicacionActualASEJ()));
        aASEJ.setEnfermedadesJsonASEJ(escribirEnfermedadesASEJ(dtoASEJ.enfermedadesASEJ()));
        aASEJ.setHabitosASEJ(ValidadorASEJ.limpioMultilineaASEJ(dtoASEJ.habitosASEJ()));
        aASEJ.setFumaASEJ(dtoASEJ.fumaASEJ());
        aASEJ.setGestanteASEJ(dtoASEJ.gestanteASEJ());
        aASEJ.setPresionArterialASEJ(presionASEJ);
        aASEJ.setObservacionesASEJ(ValidadorASEJ.limpioMultilineaASEJ(dtoASEJ.observacionesASEJ()));
        aASEJ.setActualizadoPorASEJ(usuarioIdASEJ);
        aASEJ.setActualizadoEnASEJ(LocalDateTime.now(relojASEJ));
        anamnesisRepositoryASEJ.save(aASEJ);
        return obtenerASEJ(pacienteASEJ.getIdASEJ());
    }

    private static Map<String, Boolean> leerEnfermedadesASEJ(String jsonASEJ) {
        Map<String, Boolean> deJsonASEJ = jsonASEJ == null || jsonASEJ.isBlank() ? null : GSONASEJ.fromJson(jsonASEJ, TIPO_MAPAASEJ);
        Map<String, Boolean> leidasASEJ = deJsonASEJ == null ? Map.of() : deJsonASEJ;
        Map<String, Boolean> resultadoASEJ = new LinkedHashMap<>();
        ENFERMEDADESASEJ.keySet().forEach(kASEJ -> resultadoASEJ.put(kASEJ, Boolean.TRUE.equals(leidasASEJ.get(kASEJ))));
        return resultadoASEJ;
    }

    private static String escribirEnfermedadesASEJ(Map<String, Boolean> marcadasASEJ) {
        Map<String, Boolean> completoASEJ = new LinkedHashMap<>();
        ENFERMEDADESASEJ.keySet().forEach(kASEJ -> completoASEJ.put(kASEJ, Boolean.TRUE.equals(marcadasASEJ.get(kASEJ))));
        return GSONASEJ.toJson(completoASEJ);
    }

    private static Map<String, String> enfermedadesASEJ() {
        Map<String, String> mASEJ = new LinkedHashMap<>();
        mASEJ.put("diabetes", "Diabetes");
        mASEJ.put("hipertension", "Hipertensión arterial");
        mASEJ.put("cardiopatia", "Cardiopatía");
        mASEJ.put("asma", "Asma");
        mASEJ.put("epilepsia", "Epilepsia");
        mASEJ.put("hepatitis", "Hepatitis");
        mASEJ.put("vih", "VIH");
        mASEJ.put("tuberculosis", "Tuberculosis");
        mASEJ.put("coagulopatia", "Trastornos de coagulación");
        mASEJ.put("renal", "Enfermedad renal");
        mASEJ.put("tiroides", "Enfermedad tiroidea");
        mASEJ.put("cancer", "Cáncer");
        return Collections.unmodifiableMap(mASEJ);
    }
}
