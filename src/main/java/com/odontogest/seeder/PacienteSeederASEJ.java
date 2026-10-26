package com.odontogest.seeder;

import com.odontogest.modelo.SexoASEJ;
import com.odontogest.modelo.TipoDocumentoASEJ;
import com.odontogest.servicio.AnamnesisServiceASEJ;
import com.odontogest.servicio.ConsentimientoServiceASEJ;
import com.odontogest.servicio.PacienteServiceASEJ;
import com.odontogest.servicio.dto.AnamnesisDtoASEJ;
import com.odontogest.servicio.dto.PacienteFormASEJ;
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

@Component
@RequiredArgsConstructor
public class PacienteSeederASEJ {

    public static final int TOTALASEJ = 60;
    static final int MENORESASEJ = 10;

    private static final String[] NOMBRES_FASEJ = {"María", "Rosa", "Carmen", "Ana Lucía", "Luz Marina", "Juana", "Elena",
            "Yolanda", "Gladys", "Patricia", "Milagros", "Flor de María", "Silvia", "Lourdes", "Vilma", "Nancy",
            "Ruth", "Sonia", "Delia", "Fiorella", "Valeria", "Camila", "Ximena", "Luciana", "Daniela"};
    private static final String[] NOMBRES_MASEJ = {"José", "Juan Carlos", "Luis Alberto", "Jorge", "Pedro", "Víctor",
            "César Augusto", "Miguel Ángel", "Eduardo", "Raúl", "Wilber", "Edwin", "Fredy", "Hernán", "Percy",
            "Ronald", "Walter", "Teófilo", "Alex", "Rodrigo", "Mateo", "Santiago", "Diego", "Thiago", "Sebastián"};
    private static final String[] APELLIDOSASEJ = {"Quispe", "Mamani", "Huanca", "Condori", "Flores", "Apaza", "Ccama",
            "Choque", "Ticona", "Pari", "Coaquira", "Calla", "Machaca", "Ramos", "Vilca", "Pacompía", "Cutipa",
            "Chambi", "Arias", "Torres", "Gutiérrez", "Rojas", "Sánchez", "Mendoza", "Paredes", "Velásquez",
            "Supo", "Yupanqui", "Cáceres", "Inofuente"};
    private static final String[] OCUPACIONESASEJ = {"Docente", "Comerciante", "Agricultor(a)", "Estudiante",
            "Ama de casa", "Conductor", "Contador(a)", "Enfermero(a)", "Ingeniero(a)", "Artesano(a)", "Jubilado(a)",
            "Técnico(a)", "Abogado(a)", "Policía"};
    private static final String[] JIRONESASEJ = {"Jr. Lima", "Jr. Puno", "Av. El Sol", "Jr. Moquegua", "Av. Floral",
            "Jr. Arequipa", "Av. Circunvalación", "Jr. Tacna", "Av. Ferrocarril", "Jr. Deza"};
    private static final String[] ALERGIASASEJ = {"Penicilina", "Látex", "Lidocaína con epinefrina", "Ibuprofeno"};

    private final PacienteServiceASEJ pacienteServiceASEJ;
    private final AnamnesisServiceASEJ anamnesisServiceASEJ;
    private final ConsentimientoServiceASEJ consentimientoServiceASEJ;

    public record PacienteDemoASEJ(String nombresASEJ, String apellidoPaternoASEJ, String apellidoMaternoASEJ, SexoASEJ sexoASEJ, int edadASEJ,
                               String dniASEJ, String telefonoASEJ, String emailASEJ, String ocupacionASEJ, String direccionASEJ,
                               String apoderadoASEJ, String alergiaASEJ, Map<String, Boolean> enfermedadesASEJ, boolean fumaASEJ) {

        public boolean menorASEJ() {
            return edadASEJ < 18;
        }

        public String firmanteASEJ() {
            return menorASEJ() ? apoderadoASEJ : nombresASEJ + " " + apellidoPaternoASEJ + " " + apellidoMaternoASEJ;
        }
    }

    public List<PacienteDemoASEJ> generarASEJ(Random rASEJ) {
        List<PacienteDemoASEJ> listaASEJ = new ArrayList<>();
        Set<String> dnisASEJ = new HashSet<>();
        int[] edadesMenoresASEJ = {4, 5, 5, 7, 8, 9, 10, 12, 14, 16};
        for (int iASEJ = 0; iASEJ < TOTALASEJ; iASEJ++) {
            boolean menorASEJ = iASEJ % (TOTALASEJ / MENORESASEJ) == 3;
            int edadASEJ = menorASEJ ? edadesMenoresASEJ[iASEJ / (TOTALASEJ / MENORESASEJ)] : 18 + rASEJ.nextInt(63);
            SexoASEJ sexoASEJ = rASEJ.nextBoolean() ? SexoASEJ.F : SexoASEJ.M;
            String nombresASEJ = (sexoASEJ == SexoASEJ.F ? NOMBRES_FASEJ : NOMBRES_MASEJ)[rASEJ.nextInt(NOMBRES_FASEJ.length)];
            String paternoASEJ = APELLIDOSASEJ[rASEJ.nextInt(APELLIDOSASEJ.length)];
            String maternoASEJ = APELLIDOSASEJ[rASEJ.nextInt(APELLIDOSASEJ.length)];
            String dniASEJ;
            do {
                dniASEJ = "9" + String.format("%07d", rASEJ.nextInt(10_000_000));
            } while (!dnisASEJ.add(dniASEJ));
            String telefonoASEJ = "9" + String.format("%08d", rASEJ.nextInt(100_000_000));
            String emailASEJ = !menorASEJ && rASEJ.nextInt(3) == 0
                    ? sinTildesASEJ(nombresASEJ.split(" ")[0] + "." + paternoASEJ).toLowerCase() + (10 + rASEJ.nextInt(90)) + "@correo-demo.pe"
                    : null;
            String apoderadoASEJ = menorASEJ ? (rASEJ.nextBoolean() ? "Rosa " : "Juan ") + paternoASEJ + " " + APELLIDOSASEJ[rASEJ.nextInt(APELLIDOSASEJ.length)]
                    : null;
            Map<String, Boolean> enfermedadesASEJ = new HashMap<>();
            if (edadASEJ > 45 && rASEJ.nextInt(3) == 0) {
                enfermedadesASEJ.put("hipertension", true);
            }
            if (edadASEJ > 40 && rASEJ.nextInt(6) == 0) {
                enfermedadesASEJ.put("diabetes", true);
            }
            if (rASEJ.nextInt(12) == 0) {
                enfermedadesASEJ.put("asma", true);
            }
            listaASEJ.add(new PacienteDemoASEJ(nombresASEJ, paternoASEJ, maternoASEJ, sexoASEJ, edadASEJ, dniASEJ, telefonoASEJ, emailASEJ,
                    menorASEJ ? (edadASEJ >= 6 ? "Estudiante" : null) : OCUPACIONESASEJ[rASEJ.nextInt(OCUPACIONESASEJ.length)],
                    JIRONESASEJ[rASEJ.nextInt(JIRONESASEJ.length)] + " " + (100 + rASEJ.nextInt(900)) + ", Juliaca", apoderadoASEJ,
                    rASEJ.nextInt(8) == 0 ? ALERGIASASEJ[rASEJ.nextInt(ALERGIASASEJ.length)] : null, enfermedadesASEJ,
                    !menorASEJ && rASEJ.nextInt(7) == 0));
        }
        return listaASEJ;
    }

    public long registrarASEJ(PacienteDemoASEJ pASEJ, LocalDate hoyASEJ, Random rASEJ) {
        LocalDate nacimientoASEJ = hoyASEJ.minusYears(pASEJ.edadASEJ()).minusDays(1 + rASEJ.nextInt(360));
        long idASEJ = pacienteServiceASEJ.guardarASEJ(new PacienteFormASEJ(null, TipoDocumentoASEJ.DNI, pASEJ.dniASEJ(), pASEJ.nombresASEJ(),
                pASEJ.apellidoPaternoASEJ(), pASEJ.apellidoMaternoASEJ(), nacimientoASEJ, pASEJ.sexoASEJ(), pASEJ.telefonoASEJ(), pASEJ.emailASEJ(), pASEJ.direccionASEJ(),
                pASEJ.ocupacionASEJ(), pASEJ.menorASEJ() ? null : "Familiar de " + pASEJ.nombresASEJ().split(" ")[0],
                pASEJ.menorASEJ() ? null : "9" + String.format("%08d", rASEJ.nextInt(100_000_000)),
                pASEJ.apoderadoASEJ(), pASEJ.menorASEJ() ? "DNI 4" + String.format("%07d", rASEJ.nextInt(10_000_000)) : null,
                pASEJ.menorASEJ() ? (pASEJ.apoderadoASEJ().startsWith("Rosa") ? "Madre" : "Padre") : null, pASEJ.emailASEJ() != null));
        consentimientoServiceASEJ.registrarDatosPersonalesASEJ(idASEJ, pASEJ.firmanteASEJ());
        return idASEJ;
    }

    public void registrarAnamnesisASEJ(long idASEJ, PacienteDemoASEJ pASEJ) {
        anamnesisServiceASEJ.guardarASEJ(new AnamnesisDtoASEJ(idASEJ,
                pASEJ.enfermedadesASEJ().isEmpty() ? "Sin antecedentes de importancia." : "Controles periódicos con su médico.",
                pASEJ.alergiaASEJ(), pASEJ.enfermedadesASEJ().containsKey("hipertension") ? "Losartán 50 mg cada 24 h" : null,
                pASEJ.enfermedadesASEJ(), pASEJ.menorASEJ() ? "Cepillado dos veces al día" : "Cepillado tres veces al día",
                pASEJ.fumaASEJ(), false, pASEJ.enfermedadesASEJ().containsKey("hipertension") ? "130/85" : null, null, null));
    }

    private static String sinTildesASEJ(String textoASEJ) {
        return java.text.Normalizer.normalize(textoASEJ, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replace(" ", "");
    }
}
