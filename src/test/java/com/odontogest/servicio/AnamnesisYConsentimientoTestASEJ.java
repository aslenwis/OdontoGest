package com.odontogest.servicio;

import com.odontogest.PruebaIntegracionASEJ;
import com.odontogest.excepcion.NegocioExceptionASEJ;
import com.odontogest.excepcion.PermisoDenegadoExceptionASEJ;
import com.odontogest.modelo.SexoASEJ;
import com.odontogest.modelo.TipoConsentimientoASEJ;
import com.odontogest.modelo.TipoDocumentoASEJ;
import com.odontogest.servicio.dto.AnamnesisDtoASEJ;
import com.odontogest.servicio.dto.PacienteFormASEJ;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnamnesisYConsentimientoTestASEJ extends PruebaIntegracionASEJ {

    @Autowired private PacienteServiceASEJ pacienteServiceASEJ;
    @Autowired private AnamnesisServiceASEJ anamnesisServiceASEJ;
    @Autowired private ConsentimientoServiceASEJ consentimientoServiceASEJ;
    @Autowired private JdbcTemplate jdbcASEJ;

    private long pacienteIdASEJ;

    @BeforeEach
    void pacienteASEJ() {
        iniciarSesionASEJ("odontologo");
        pacienteIdASEJ = pacienteServiceASEJ.guardarASEJ(PacienteServiceTestASEJ.adultoASEJ(dniAleatorioASEJ(), "Lucía", "Apaza" + sufijoASEJ()));
    }

    private AnamnesisDtoASEJ anamnesisASEJ(String alergiasASEJ, Map<String, Boolean> enfermedadesASEJ, String presionASEJ, boolean gestanteASEJ) {
        return new AnamnesisDtoASEJ(pacienteIdASEJ, "Apendicectomía 2015", alergiasASEJ, "Ninguna", enfermedadesASEJ, "Bruxismo",
                false, gestanteASEJ, presionASEJ, "  Paciente ansiosa  ", null);
    }

    @Test
    void sinAnamnesisDevuelveUnaVaciaASEJ() {
        AnamnesisDtoASEJ aASEJ = anamnesisServiceASEJ.obtenerASEJ(pacienteIdASEJ);
        assertThat(aASEJ.alergiasASEJ()).isNull();
        assertThat(aASEJ.actualizadoEnASEJ()).isNull();
    }

    @Test
    void guardaYLaFichaMuestraLasAlergiasASEJ() {
        AnamnesisDtoASEJ guardadaASEJ = anamnesisServiceASEJ.guardarASEJ(anamnesisASEJ("Penicilina\nLátex", Map.of("diabetes", true), "120/80", true));

        assertThat(guardadaASEJ.enfermedadesASEJ()).containsEntry("diabetes", true).containsEntry("asma", false)
                .hasSize(AnamnesisServiceASEJ.ENFERMEDADESASEJ.size());
        assertThat(guardadaASEJ.observacionesASEJ()).isEqualTo("Paciente ansiosa");
        assertThat(guardadaASEJ.actualizadoEnASEJ()).isNotNull();
        assertThat(pacienteServiceASEJ.obtenerASEJ(pacienteIdASEJ).tieneAlergiasASEJ()).isTrue();
        assertThat(pacienteServiceASEJ.obtenerASEJ(pacienteIdASEJ).alergiasASEJ()).isEqualTo("Penicilina\nLátex");

        String jsonASEJ = jdbcASEJ.queryForObject("SELECT enfermedades_json FROM anamnesis WHERE paciente_id = ?", String.class, pacienteIdASEJ);
        assertThat(jsonASEJ).startsWith("{\"diabetes\":true,\"hipertension\":false");

        anamnesisServiceASEJ.guardarASEJ(anamnesisASEJ("  ", Map.of(), null, false));
        assertThat(pacienteServiceASEJ.obtenerASEJ(pacienteIdASEJ).tieneAlergiasASEJ()).as("una sola anamnesis por paciente").isFalse();
        assertThat(jdbcASEJ.queryForObject("SELECT COUNT(*) FROM anamnesis WHERE paciente_id = ?", Integer.class, pacienteIdASEJ)).isEqualTo(1);
    }

    @Test
    void validaPresionGestanteYEnfermedadesASEJ() {
        assertThatThrownBy(() -> anamnesisServiceASEJ.guardarASEJ(anamnesisASEJ(null, Map.of(), "alta", false)))
                .isInstanceOf(NegocioExceptionASEJ.class).hasMessageContaining("120/80");
        assertThatThrownBy(() -> anamnesisServiceASEJ.guardarASEJ(anamnesisASEJ(null, Map.of("gripe", true), null, false)))
                .isInstanceOf(NegocioExceptionASEJ.class).hasMessageContaining("no reconocida");

        long varonASEJ = pacienteServiceASEJ.guardarASEJ(new PacienteFormASEJ(null, TipoDocumentoASEJ.DNI, dniAleatorioASEJ(), "Raúl", "Ticona" + sufijoASEJ(),
                null, LocalDate.now().minusYears(50), SexoASEJ.M, null, null, null, null, null, null, null, null, null, true));
        AnamnesisDtoASEJ gestanteVaronASEJ = new AnamnesisDtoASEJ(varonASEJ, null, null, null, Map.of(), null, false, true, null, null, null);
        assertThatThrownBy(() -> anamnesisServiceASEJ.guardarASEJ(gestanteVaronASEJ))
                .isInstanceOf(NegocioExceptionASEJ.class).hasMessageContaining("gestante");
    }

    @Test
    void recepcionNoPuedeEditarLaAnamnesisASEJ() {
        iniciarSesionASEJ("recepcion");
        assertThatThrownBy(() -> anamnesisServiceASEJ.guardarASEJ(anamnesisASEJ(null, Map.of(), null, false)))
                .isInstanceOf(PermisoDenegadoExceptionASEJ.class).hasMessageContaining("anamnesis");
        assertThat(anamnesisServiceASEJ.obtenerASEJ(pacienteIdASEJ)).as("pero sí puede verla").isNotNull();
    }

    @Test
    void generaElPdfDelConsentimientoDeDatosASEJ() {
        byte[] pdfASEJ = consentimientoServiceASEJ.pdfDatosPersonalesASEJ(pacienteIdASEJ);
        assertThat(new String(pdfASEJ, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        assertThat(pdfASEJ.length).isGreaterThan(1500);
    }

    @Test
    void generaElPdfParaUnMenorConApoderadoASEJ() {
        long menorASEJ = pacienteServiceASEJ.guardarASEJ(new PacienteFormASEJ(null, TipoDocumentoASEJ.DNI, dniAleatorioASEJ(), "Sofía", "Coaquira" + sufijoASEJ(),
                null, LocalDate.now().minusYears(8), SexoASEJ.F, null, null, null, null, null, null,
                "Marta Coaquira", "DNI 41234567", "Madre", true));
        assertThat(consentimientoServiceASEJ.pdfDatosPersonalesASEJ(menorASEJ)).isNotEmpty();
    }

    @Test
    void registraLaFirmaDelConsentimientoASEJ() {
        assertThat(pacienteServiceASEJ.obtenerASEJ(pacienteIdASEJ).consentimientoDatosEnASEJ()).isNull();
        consentimientoServiceASEJ.registrarDatosPersonalesASEJ(pacienteIdASEJ, "  Lucía Apaza  ");

        assertThat(consentimientoServiceASEJ.listarASEJ(pacienteIdASEJ)).singleElement()
                .satisfies(cASEJ -> {
                    assertThat(cASEJ.tipoASEJ()).isEqualTo(TipoConsentimientoASEJ.DATOS_PERSONALES);
                    assertThat(cASEJ.firmadoPorASEJ()).isEqualTo("Lucía Apaza");
                });
        assertThat(pacienteServiceASEJ.obtenerASEJ(pacienteIdASEJ).consentimientoDatosEnASEJ()).isNotNull();
        assertThatThrownBy(() -> consentimientoServiceASEJ.registrarDatosPersonalesASEJ(pacienteIdASEJ, " "))
                .isInstanceOf(NegocioExceptionASEJ.class);
    }
}
