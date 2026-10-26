package com.odontogest.integracion.seguridad;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CifradorSecretosTestASEJ {

    @TempDir
    Path carpetaASEJ;

    @Test
    void cifraYDescifraConLaClaveCreadaLaPrimeraVezASEJ() throws Exception {
        CifradorSecretosASEJ cASEJ = new CifradorSecretosASEJ(carpetaASEJ.resolve("clave.key"));
        String cifradoASEJ = cASEJ.cifrarASEJ("contraseña-ñandú-123");
        assertThat(cifradoASEJ).startsWith("enc:v1:");
        assertThat(CifradorSecretosASEJ.esCifradoASEJ(cifradoASEJ)).isTrue();
        assertThat(cASEJ.descifrarASEJ(cifradoASEJ)).isEqualTo("contraseña-ñandú-123");
        assertThat(cASEJ.cifrarASEJ("contraseña-ñandú-123")).as("IV aleatorio").isNotEqualTo(cifradoASEJ);
        assertThat(Base64.getDecoder().decode(Files.readString(carpetaASEJ.resolve("clave.key")).trim())).hasSize(32);

        CifradorSecretosASEJ otraInstanciaASEJ = new CifradorSecretosASEJ(carpetaASEJ.resolve("clave.key"));
        assertThat(otraInstanciaASEJ.descifrarASEJ(cifradoASEJ)).as("la clave persiste en el archivo").isEqualTo("contraseña-ñandú-123");
    }

    @Test
    void otraClaveOUnValorAlteradoNoSeDescifranASEJ() {
        CifradorSecretosASEJ aASEJ = new CifradorSecretosASEJ(carpetaASEJ.resolve("a.key"));
        CifradorSecretosASEJ bASEJ = new CifradorSecretosASEJ(carpetaASEJ.resolve("b.key"));
        String cifradoASEJ = aASEJ.cifrarASEJ("secreto");
        assertThatThrownBy(() -> bASEJ.descifrarASEJ(cifradoASEJ)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("clave local");
        String alteradoASEJ = cifradoASEJ.substring(0, cifradoASEJ.length() - 4) + "AAAA";
        assertThatThrownBy(() -> aASEJ.descifrarASEJ(alteradoASEJ)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> aASEJ.descifrarASEJ("texto plano")).isInstanceOf(IllegalArgumentException.class);
    }
}
