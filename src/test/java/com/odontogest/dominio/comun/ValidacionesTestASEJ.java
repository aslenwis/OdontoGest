package com.odontogest.dominio.comun;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidacionesTestASEJ {

    @Test
    void dniASEJ() {
        assertThat(ValidacionesASEJ.esDniASEJ("71234567")).isTrue();
        assertThat(ValidacionesASEJ.esDniASEJ("7123456")).isFalse();
        assertThat(ValidacionesASEJ.esDniASEJ("712345678")).isFalse();
        assertThat(ValidacionesASEJ.esDniASEJ("7123456A")).isFalse();
        assertThat(ValidacionesASEJ.esDniASEJ(null)).isFalse();
    }

    @Test
    void carneDeExtranjeriaYPasaporteASEJ() {
        assertThat(ValidacionesASEJ.esCarneExtranjeriaASEJ("001234567")).isTrue();
        assertThat(ValidacionesASEJ.esCarneExtranjeriaASEJ("12345")).isFalse();
        assertThat(ValidacionesASEJ.esPasaporteASEJ("AB123456")).isTrue();
        assertThat(ValidacionesASEJ.esPasaporteASEJ("AB-123")).isFalse();
        assertThat(ValidacionesASEJ.esPasaporteASEJ(null)).isFalse();
        assertThat(ValidacionesASEJ.esCarneExtranjeriaASEJ(null)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"paciente@gmail.com", "a.b+c@clinica.com.pe", " x@y.pe "})
    void correosValidosASEJ(String emailASEJ) {
        assertThat(ValidacionesASEJ.esEmailASEJ(emailASEJ)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "sin-arroba", "a@b", "a@.pe", "@clinica.pe", "a b@c.pe"})
    void correosInvalidosASEJ(String emailASEJ) {
        assertThat(ValidacionesASEJ.esEmailASEJ(emailASEJ)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"987654321", "987 654 321", "+51 987654321", "01 4567890", "(054) 123456", "054-123456"})
    void telefonosValidosASEJ(String telefonoASEJ) {
        assertThat(ValidacionesASEJ.esTelefonoASEJ(telefonoASEJ)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"12", "98765432a", "887654321000", "abc"})
    void telefonosInvalidosASEJ(String telefonoASEJ) {
        assertThat(ValidacionesASEJ.esTelefonoASEJ(telefonoASEJ)).isFalse();
    }

    @Test
    void edadYMayoriaDeEdadASEJ() {
        LocalDate hoyASEJ = LocalDate.of(2026, 3, 2);
        assertThat(ValidacionesASEJ.edadASEJ(LocalDate.of(2008, 3, 2), hoyASEJ)).isEqualTo(18);
        assertThat(ValidacionesASEJ.esMenorDeEdadASEJ(LocalDate.of(2008, 3, 2), hoyASEJ)).isFalse();
        assertThat(ValidacionesASEJ.esMenorDeEdadASEJ(LocalDate.of(2008, 3, 3), hoyASEJ)).isTrue();
        assertThatThrownBy(() -> ValidacionesASEJ.edadASEJ(hoyASEJ.plusDays(1), hoyASEJ)).isInstanceOf(IllegalArgumentException.class);
        assertThat(ValidacionesASEJ.esTelefonoASEJ(null)).isFalse();
        assertThat(ValidacionesASEJ.esEmailASEJ(null)).isFalse();
    }
}
