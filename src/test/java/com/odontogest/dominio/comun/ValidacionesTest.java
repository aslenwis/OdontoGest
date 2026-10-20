/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.comun;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Formatos de documento, correo y teléfono; mayoría de edad.
 */
class ValidacionesTest {

    @Test
    void dni() {
        assertThat(Validaciones.esDni("71234567")).isTrue();
        assertThat(Validaciones.esDni("7123456")).isFalse();
        assertThat(Validaciones.esDni("712345678")).isFalse();
        assertThat(Validaciones.esDni("7123456A")).isFalse();
        assertThat(Validaciones.esDni(null)).isFalse();
    }

    @Test
    void carneDeExtranjeriaYPasaporte() {
        assertThat(Validaciones.esCarneExtranjeria("001234567")).isTrue();
        assertThat(Validaciones.esCarneExtranjeria("12345")).isFalse();
        assertThat(Validaciones.esPasaporte("AB123456")).isTrue();
        assertThat(Validaciones.esPasaporte("AB-123")).isFalse();
        assertThat(Validaciones.esPasaporte(null)).isFalse();
        assertThat(Validaciones.esCarneExtranjeria(null)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"paciente@gmail.com", "a.b+c@clinica.com.pe", " x@y.pe "})
    void correosValidos(String email) {
        assertThat(Validaciones.esEmail(email)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "sin-arroba", "a@b", "a@.pe", "@clinica.pe", "a b@c.pe"})
    void correosInvalidos(String email) {
        assertThat(Validaciones.esEmail(email)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"987654321", "987 654 321", "+51 987654321", "01 4567890", "(054) 123456", "054-123456"})
    void telefonosValidos(String telefono) {
        assertThat(Validaciones.esTelefono(telefono)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"12", "98765432a", "887654321000", "abc"})
    void telefonosInvalidos(String telefono) {
        assertThat(Validaciones.esTelefono(telefono)).isFalse();
    }

    @Test
    void edadYMayoriaDeEdad() {
        LocalDate hoy = LocalDate.of(2026, 3, 2);
        assertThat(Validaciones.edad(LocalDate.of(2008, 3, 2), hoy)).isEqualTo(18);
        assertThat(Validaciones.esMenorDeEdad(LocalDate.of(2008, 3, 2), hoy)).isFalse();
        assertThat(Validaciones.esMenorDeEdad(LocalDate.of(2008, 3, 3), hoy)).isTrue();
        assertThatThrownBy(() -> Validaciones.edad(hoy.plusDays(1), hoy)).isInstanceOf(IllegalArgumentException.class);
        assertThat(Validaciones.esTelefono(null)).isFalse();
        assertThat(Validaciones.esEmail(null)).isFalse();
    }
}
