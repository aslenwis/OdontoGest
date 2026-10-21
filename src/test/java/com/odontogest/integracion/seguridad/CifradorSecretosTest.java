/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.integracion.seguridad;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AES-256-GCM con clave local: ida y vuelta, IV aleatorio, formato {@code enc:v1:} y rechazo con otra clave.
 */
class CifradorSecretosTest {

    @TempDir
    Path carpeta;

    @Test
    void cifraYDescifraConLaClaveCreadaLaPrimeraVez() throws Exception {
        CifradorSecretos c = new CifradorSecretos(carpeta.resolve("clave.key"));
        String cifrado = c.cifrar("contraseña-ñandú-123");
        assertThat(cifrado).startsWith("enc:v1:");
        assertThat(CifradorSecretos.esCifrado(cifrado)).isTrue();
        assertThat(c.descifrar(cifrado)).isEqualTo("contraseña-ñandú-123");
        assertThat(c.cifrar("contraseña-ñandú-123")).as("IV aleatorio").isNotEqualTo(cifrado);
        assertThat(Base64.getDecoder().decode(Files.readString(carpeta.resolve("clave.key")).trim())).hasSize(32);

        CifradorSecretos otraInstancia = new CifradorSecretos(carpeta.resolve("clave.key"));
        assertThat(otraInstancia.descifrar(cifrado)).as("la clave persiste en el archivo").isEqualTo("contraseña-ñandú-123");
    }

    @Test
    void otraClaveOUnValorAlteradoNoSeDescifran() {
        CifradorSecretos a = new CifradorSecretos(carpeta.resolve("a.key"));
        CifradorSecretos b = new CifradorSecretos(carpeta.resolve("b.key"));
        String cifrado = a.cifrar("secreto");
        assertThatThrownBy(() -> b.descifrar(cifrado)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("clave local");
        String alterado = cifrado.substring(0, cifrado.length() - 4) + "AAAA";
        assertThatThrownBy(() -> a.descifrar(alterado)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> a.descifrar("texto plano")).isInstanceOf(IllegalArgumentException.class);
    }
}
