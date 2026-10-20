/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio;

import com.odontogest.PruebaIntegracion;
import com.odontogest.excepcion.NegocioException;
import com.odontogest.excepcion.PermisoDenegadoException;
import com.odontogest.modelo.Sexo;
import com.odontogest.modelo.TipoConsentimiento;
import com.odontogest.modelo.TipoDocumento;
import com.odontogest.servicio.dto.AnamnesisDto;
import com.odontogest.servicio.dto.PacienteForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Anamnesis (alerta de alergias) y consentimiento de datos personales en PDF (RF-PAC-04, RF-PAC-05).
 */
class AnamnesisYConsentimientoTest extends PruebaIntegracion {

    @Autowired private PacienteService pacienteService;
    @Autowired private AnamnesisService anamnesisService;
    @Autowired private ConsentimientoService consentimientoService;
    @Autowired private JdbcTemplate jdbc;

    private long pacienteId;

    @BeforeEach
    void paciente() {
        iniciarSesion("odontologo");
        pacienteId = pacienteService.guardar(PacienteServiceTest.adulto(dniAleatorio(), "Lucía", "Apaza" + sufijo()));
    }

    private AnamnesisDto anamnesis(String alergias, Map<String, Boolean> enfermedades, String presion, boolean gestante) {
        return new AnamnesisDto(pacienteId, "Apendicectomía 2015", alergias, "Ninguna", enfermedades, "Bruxismo",
                false, gestante, presion, "  Paciente ansiosa  ", null);
    }

    @Test
    void sinAnamnesisDevuelveUnaVacia() {
        AnamnesisDto a = anamnesisService.obtener(pacienteId);
        assertThat(a.alergias()).isNull();
        assertThat(a.actualizadoEn()).isNull();
    }

    @Test
    void guardaYLaFichaMuestraLasAlergias() {
        AnamnesisDto guardada = anamnesisService.guardar(anamnesis("Penicilina\nLátex", Map.of("diabetes", true), "120/80", true));

        assertThat(guardada.enfermedades()).containsEntry("diabetes", true).containsEntry("asma", false)
                .hasSize(AnamnesisService.ENFERMEDADES.size());
        assertThat(guardada.observaciones()).isEqualTo("Paciente ansiosa");
        assertThat(guardada.actualizadoEn()).isNotNull();
        assertThat(pacienteService.obtener(pacienteId).tieneAlergias()).isTrue();
        assertThat(pacienteService.obtener(pacienteId).alergias()).isEqualTo("Penicilina\nLátex");

        String json = jdbc.queryForObject("SELECT enfermedades_json FROM anamnesis WHERE paciente_id = ?", String.class, pacienteId);
        assertThat(json).startsWith("{\"diabetes\":true,\"hipertension\":false");

        anamnesisService.guardar(anamnesis("  ", Map.of(), null, false));
        assertThat(pacienteService.obtener(pacienteId).tieneAlergias()).as("una sola anamnesis por paciente").isFalse();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM anamnesis WHERE paciente_id = ?", Integer.class, pacienteId)).isEqualTo(1);
    }

    @Test
    void validaPresionGestanteYEnfermedades() {
        assertThatThrownBy(() -> anamnesisService.guardar(anamnesis(null, Map.of(), "alta", false)))
                .isInstanceOf(NegocioException.class).hasMessageContaining("120/80");
        assertThatThrownBy(() -> anamnesisService.guardar(anamnesis(null, Map.of("gripe", true), null, false)))
                .isInstanceOf(NegocioException.class).hasMessageContaining("no reconocida");

        long varon = pacienteService.guardar(new PacienteForm(null, TipoDocumento.DNI, dniAleatorio(), "Raúl", "Ticona" + sufijo(),
                null, LocalDate.now().minusYears(50), Sexo.M, null, null, null, null, null, null, null, null, null, true));
        AnamnesisDto gestanteVaron = new AnamnesisDto(varon, null, null, null, Map.of(), null, false, true, null, null, null);
        assertThatThrownBy(() -> anamnesisService.guardar(gestanteVaron))
                .isInstanceOf(NegocioException.class).hasMessageContaining("gestante");
    }

    @Test
    void recepcionNoPuedeEditarLaAnamnesis() {
        iniciarSesion("recepcion");
        assertThatThrownBy(() -> anamnesisService.guardar(anamnesis(null, Map.of(), null, false)))
                .isInstanceOf(PermisoDenegadoException.class).hasMessageContaining("anamnesis");
        assertThat(anamnesisService.obtener(pacienteId)).as("pero sí puede verla").isNotNull();
    }

    @Test
    void generaElPdfDelConsentimientoDeDatos() {
        byte[] pdf = consentimientoService.pdfDatosPersonales(pacienteId);
        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        assertThat(pdf.length).isGreaterThan(1500);
    }

    @Test
    void generaElPdfParaUnMenorConApoderado() {
        long menor = pacienteService.guardar(new PacienteForm(null, TipoDocumento.DNI, dniAleatorio(), "Sofía", "Coaquira" + sufijo(),
                null, LocalDate.now().minusYears(8), Sexo.F, null, null, null, null, null, null,
                "Marta Coaquira", "DNI 41234567", "Madre", true));
        assertThat(consentimientoService.pdfDatosPersonales(menor)).isNotEmpty();
    }

    @Test
    void registraLaFirmaDelConsentimiento() {
        assertThat(pacienteService.obtener(pacienteId).consentimientoDatosEn()).isNull();
        consentimientoService.registrarDatosPersonales(pacienteId, "  Lucía Apaza  ");

        assertThat(consentimientoService.listar(pacienteId)).singleElement()
                .satisfies(c -> {
                    assertThat(c.tipo()).isEqualTo(TipoConsentimiento.DATOS_PERSONALES);
                    assertThat(c.firmadoPor()).isEqualTo("Lucía Apaza");
                });
        assertThat(pacienteService.obtener(pacienteId).consentimientoDatosEn()).isNotNull();
        assertThatThrownBy(() -> consentimientoService.registrarDatosPersonales(pacienteId, " "))
                .isInstanceOf(NegocioException.class);
    }
}
