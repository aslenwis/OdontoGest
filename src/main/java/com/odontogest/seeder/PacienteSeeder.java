/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.seeder;

import com.odontogest.modelo.Sexo;
import com.odontogest.modelo.TipoDocumento;
import com.odontogest.servicio.AnamnesisService;
import com.odontogest.servicio.ConsentimientoService;
import com.odontogest.servicio.PacienteService;
import com.odontogest.servicio.dto.AnamnesisDto;
import com.odontogest.servicio.dto.PacienteForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Pacientes de demostración (PLAN §3.5): nombres peruanos, DNI ficticios que empiezan con 9, edades de 4 a 80 años
 * (10 menores con apoderado), anamnesis y consentimiento de datos personales. Los registra con los mismos servicios
 * que la interfaz, en el momento que fija {@code ClinicaDemoSeeder} en el reloj de la aplicación.
 */
@Component
@RequiredArgsConstructor
public class PacienteSeeder {

    public static final int TOTAL = 60;
    static final int MENORES = 10;

    private static final String[] NOMBRES_F = {"María", "Rosa", "Carmen", "Ana Lucía", "Luz Marina", "Juana", "Elena",
            "Yolanda", "Gladys", "Patricia", "Milagros", "Flor de María", "Silvia", "Lourdes", "Vilma", "Nancy",
            "Ruth", "Sonia", "Delia", "Fiorella", "Valeria", "Camila", "Ximena", "Luciana", "Daniela"};
    private static final String[] NOMBRES_M = {"José", "Juan Carlos", "Luis Alberto", "Jorge", "Pedro", "Víctor",
            "César Augusto", "Miguel Ángel", "Eduardo", "Raúl", "Wilber", "Edwin", "Fredy", "Hernán", "Percy",
            "Ronald", "Walter", "Teófilo", "Alex", "Rodrigo", "Mateo", "Santiago", "Diego", "Thiago", "Sebastián"};
    private static final String[] APELLIDOS = {"Quispe", "Mamani", "Huanca", "Condori", "Flores", "Apaza", "Ccama",
            "Choque", "Ticona", "Pari", "Coaquira", "Calla", "Machaca", "Ramos", "Vilca", "Pacompía", "Cutipa",
            "Chambi", "Arias", "Torres", "Gutiérrez", "Rojas", "Sánchez", "Mendoza", "Paredes", "Velásquez",
            "Supo", "Yupanqui", "Cáceres", "Inofuente"};
    private static final String[] OCUPACIONES = {"Docente", "Comerciante", "Agricultor(a)", "Estudiante",
            "Ama de casa", "Conductor", "Contador(a)", "Enfermero(a)", "Ingeniero(a)", "Artesano(a)", "Jubilado(a)",
            "Técnico(a)", "Abogado(a)", "Policía"};
    private static final String[] JIRONES = {"Jr. Lima", "Jr. Puno", "Av. El Sol", "Jr. Moquegua", "Av. Floral",
            "Jr. Arequipa", "Av. Circunvalación", "Jr. Tacna", "Av. Ferrocarril", "Jr. Deza"};
    private static final String[] ALERGIAS = {"Penicilina", "Látex", "Lidocaína con epinefrina", "Ibuprofeno"};

    private final PacienteService pacienteService;
    private final AnamnesisService anamnesisService;
    private final ConsentimientoService consentimientoService;

    /** Datos de un paciente demo; {@code edad} a la fecha de su registro. */
    public record PacienteDemo(String nombres, String apellidoPaterno, String apellidoMaterno, Sexo sexo, int edad,
                               String dni, String telefono, String email, String ocupacion, String direccion,
                               String apoderado, String alergia, Map<String, Boolean> enfermedades, boolean fuma) {

        public boolean menor() {
            return edad < 18;
        }

        /** Quien firma los consentimientos: el paciente o, si es menor, su apoderado. */
        public String firmante() {
            return menor() ? apoderado : nombres + " " + apellidoPaterno + " " + apellidoMaterno;
        }
    }

    /** Las {@value #TOTAL} fichas, reproducibles con la semilla. Los menores quedan repartidos en la lista. */
    public List<PacienteDemo> generar(Random r) {
        List<PacienteDemo> lista = new ArrayList<>();
        Set<String> dnis = new HashSet<>();
        int[] edadesMenores = {4, 5, 5, 7, 8, 9, 10, 12, 14, 16};
        for (int i = 0; i < TOTAL; i++) {
            boolean menor = i % (TOTAL / MENORES) == 3;
            int edad = menor ? edadesMenores[i / (TOTAL / MENORES)] : 18 + r.nextInt(63);
            Sexo sexo = r.nextBoolean() ? Sexo.F : Sexo.M;
            String nombres = (sexo == Sexo.F ? NOMBRES_F : NOMBRES_M)[r.nextInt(NOMBRES_F.length)];
            String paterno = APELLIDOS[r.nextInt(APELLIDOS.length)];
            String materno = APELLIDOS[r.nextInt(APELLIDOS.length)];
            String dni;
            do {
                dni = "9" + String.format("%07d", r.nextInt(10_000_000));
            } while (!dnis.add(dni));
            String telefono = "9" + String.format("%08d", r.nextInt(100_000_000));
            String email = !menor && r.nextInt(3) == 0
                    ? sinTildes(nombres.split(" ")[0] + "." + paterno).toLowerCase() + (10 + r.nextInt(90)) + "@correo-demo.pe"
                    : null;
            String apoderado = menor ? (r.nextBoolean() ? "Rosa " : "Juan ") + paterno + " " + APELLIDOS[r.nextInt(APELLIDOS.length)]
                    : null;
            Map<String, Boolean> enfermedades = new HashMap<>();
            if (edad > 45 && r.nextInt(3) == 0) {
                enfermedades.put("hipertension", true);
            }
            if (edad > 40 && r.nextInt(6) == 0) {
                enfermedades.put("diabetes", true);
            }
            if (r.nextInt(12) == 0) {
                enfermedades.put("asma", true);
            }
            lista.add(new PacienteDemo(nombres, paterno, materno, sexo, edad, dni, telefono, email,
                    menor ? (edad >= 6 ? "Estudiante" : null) : OCUPACIONES[r.nextInt(OCUPACIONES.length)],
                    JIRONES[r.nextInt(JIRONES.length)] + " " + (100 + r.nextInt(900)) + ", Juliaca", apoderado,
                    r.nextInt(8) == 0 ? ALERGIAS[r.nextInt(ALERGIAS.length)] : null, enfermedades,
                    !menor && r.nextInt(7) == 0));
        }
        return lista;
    }

    /**
     * Registra el paciente (recepción) con su consentimiento de datos. La fecha de nacimiento se calcula para que
     * tenga {@code edad} años el día {@code hoy} (la fecha del reloj de la aplicación).
     *
     * @return id del paciente
     */
    public long registrar(PacienteDemo p, LocalDate hoy, Random r) {
        LocalDate nacimiento = hoy.minusYears(p.edad()).minusDays(1 + r.nextInt(360));
        long id = pacienteService.guardar(new PacienteForm(null, TipoDocumento.DNI, p.dni(), p.nombres(),
                p.apellidoPaterno(), p.apellidoMaterno(), nacimiento, p.sexo(), p.telefono(), p.email(), p.direccion(),
                p.ocupacion(), p.menor() ? null : "Familiar de " + p.nombres().split(" ")[0],
                p.menor() ? null : "9" + String.format("%08d", r.nextInt(100_000_000)),
                p.apoderado(), p.menor() ? "DNI 4" + String.format("%07d", r.nextInt(10_000_000)) : null,
                p.menor() ? (p.apoderado().startsWith("Rosa") ? "Madre" : "Padre") : null, p.email() != null));
        consentimientoService.registrarDatosPersonales(id, p.firmante());
        return id;
    }

    /** Anamnesis del paciente (la registra el odontólogo o el administrador, no recepción). */
    public void registrarAnamnesis(long id, PacienteDemo p) {
        anamnesisService.guardar(new AnamnesisDto(id,
                p.enfermedades().isEmpty() ? "Sin antecedentes de importancia." : "Controles periódicos con su médico.",
                p.alergia(), p.enfermedades().containsKey("hipertension") ? "Losartán 50 mg cada 24 h" : null,
                p.enfermedades(), p.menor() ? "Cepillado dos veces al día" : "Cepillado tres veces al día",
                p.fuma(), false, p.enfermedades().containsKey("hipertension") ? "130/85" : null, null, null));
    }

    private static String sinTildes(String texto) {
        return java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replace(" ", "");
    }
}
