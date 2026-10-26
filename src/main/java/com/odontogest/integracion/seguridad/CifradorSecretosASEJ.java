package com.odontogest.integracion.seguridad;

import com.odontogest.config.DirectorioDatosASEJ;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class CifradorSecretosASEJ {

    public static final String PREFIJOASEJ = "enc:v1:";
    public static final String ARCHIVO_CLAVEASEJ = "clave.key";
    private static final String ALGORITMOASEJ = "AES/GCM/NoPadding";
    private static final int BYTES_CLAVEASEJ = 32;
    private static final int BYTES_IVASEJ = 12;
    private static final int BITS_ETIQUETAASEJ = 128;

    private final SecureRandom aleatorioASEJ = new SecureRandom();
    private final Path archivoClaveASEJ;
    private volatile SecretKey claveASEJ;

    public CifradorSecretosASEJ() {
        this(DirectorioDatosASEJ.homeASEJ().resolve(ARCHIVO_CLAVEASEJ));
    }

    public CifradorSecretosASEJ(Path archivoClaveASEJ) {
        this.archivoClaveASEJ = archivoClaveASEJ;
    }

    public static boolean esCifradoASEJ(String valorASEJ) {
        return valorASEJ != null && valorASEJ.startsWith(PREFIJOASEJ);
    }

    public String cifrarASEJ(String textoPlanoASEJ) {
        try {
            byte[] ivASEJ = new byte[BYTES_IVASEJ];
            aleatorioASEJ.nextBytes(ivASEJ);
            Cipher cASEJ = Cipher.getInstance(ALGORITMOASEJ);
            cASEJ.init(Cipher.ENCRYPT_MODE, claveASEJ(), new GCMParameterSpec(BITS_ETIQUETAASEJ, ivASEJ));
            byte[] cifradoASEJ = cASEJ.doFinal(textoPlanoASEJ.getBytes(StandardCharsets.UTF_8));
            byte[] todoASEJ = ByteBuffer.allocate(ivASEJ.length + cifradoASEJ.length).put(ivASEJ).put(cifradoASEJ).array();
            return PREFIJOASEJ + Base64.getEncoder().encodeToString(todoASEJ);
        } catch (GeneralSecurityException eASEJ) {
            throw new IllegalStateException("No se pudo cifrar el secreto", eASEJ);
        }
    }

    public String descifrarASEJ(String valorCifradoASEJ) {
        if (!esCifradoASEJ(valorCifradoASEJ)) {
            throw new IllegalArgumentException("El valor no está cifrado con " + PREFIJOASEJ);
        }
        try {
            byte[] todoASEJ = Base64.getDecoder().decode(valorCifradoASEJ.substring(PREFIJOASEJ.length()));
            if (todoASEJ.length <= BYTES_IVASEJ) {
                throw new IllegalStateException("Secreto cifrado incompleto");
            }
            Cipher cASEJ = Cipher.getInstance(ALGORITMOASEJ);
            cASEJ.init(Cipher.DECRYPT_MODE, claveASEJ(), new GCMParameterSpec(BITS_ETIQUETAASEJ, todoASEJ, 0, BYTES_IVASEJ));
            byte[] planoASEJ = cASEJ.doFinal(todoASEJ, BYTES_IVASEJ, todoASEJ.length - BYTES_IVASEJ);
            return new String(planoASEJ, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException eASEJ) {
            throw new IllegalStateException("No se pudo descifrar el secreto: la clave local (" + ARCHIVO_CLAVEASEJ
                    + ") no corresponde a esta base de datos. Vuelva a escribir el valor en Configuración.", eASEJ);
        }
    }

    private SecretKey claveASEJ() {
        SecretKey kASEJ = claveASEJ;
        if (kASEJ == null) {
            synchronized (this) {
                kASEJ = claveASEJ;
                if (kASEJ == null) {
                    kASEJ = new SecretKeySpec(leerOCrearASEJ(), "AES");
                    claveASEJ = kASEJ;
                }
            }
        }
        return kASEJ;
    }

    private byte[] leerOCrearASEJ() {
        try {
            if (Files.exists(archivoClaveASEJ)) {
                byte[] leidaASEJ = Base64.getDecoder().decode(Files.readString(archivoClaveASEJ, StandardCharsets.US_ASCII).trim());
                if (leidaASEJ.length != BYTES_CLAVEASEJ) {
                    throw new IllegalStateException("El archivo " + archivoClaveASEJ + " no contiene una clave AES-256 válida");
                }
                return leidaASEJ;
            }
            byte[] nuevaASEJ = new byte[BYTES_CLAVEASEJ];
            aleatorioASEJ.nextBytes(nuevaASEJ);
            Files.createDirectories(archivoClaveASEJ.getParent());
            Files.writeString(archivoClaveASEJ, Base64.getEncoder().encodeToString(nuevaASEJ), StandardCharsets.US_ASCII);
            return nuevaASEJ;
        } catch (IOException eASEJ) {
            throw new UncheckedIOException("No se pudo leer ni crear la clave de cifrado " + archivoClaveASEJ, eASEJ);
        }
    }
}
