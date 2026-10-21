/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.integracion.seguridad;

import com.odontogest.config.DirectorioDatos;
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

/**
 * Cifra los secretos de la tabla {@code configuracion} (token de la API de DNI, contraseña SMTP) con AES-256-GCM.
 * <p>
 * La clave de 256 bits se genera con {@link SecureRandom} la primera vez y se guarda en
 * {@code ${odontogest.home}/clave.key}. Formato del valor guardado: {@code enc:v1:} + Base64(IV de 12 bytes ‖ texto
 * cifrado + etiqueta de 128 bits).
 * <p>
 * Limitación (para el informe): la clave vive en el mismo equipo; protege contra quien copie solo la base de datos,
 * no contra quien tenga acceso a toda la carpeta de datos.
 */
@Component
public class CifradorSecretos {

    public static final String PREFIJO = "enc:v1:";
    public static final String ARCHIVO_CLAVE = "clave.key";
    private static final String ALGORITMO = "AES/GCM/NoPadding";
    private static final int BYTES_CLAVE = 32;
    private static final int BYTES_IV = 12;
    private static final int BITS_ETIQUETA = 128;

    private final SecureRandom aleatorio = new SecureRandom();
    private final Path archivoClave;
    private volatile SecretKey clave;

    public CifradorSecretos() {
        this(DirectorioDatos.home().resolve(ARCHIVO_CLAVE));
    }

    /** Para pruebas: clave en otro archivo. */
    public CifradorSecretos(Path archivoClave) {
        this.archivoClave = archivoClave;
    }

    public static boolean esCifrado(String valor) {
        return valor != null && valor.startsWith(PREFIJO);
    }

    public String cifrar(String textoPlano) {
        try {
            byte[] iv = new byte[BYTES_IV];
            aleatorio.nextBytes(iv);
            Cipher c = Cipher.getInstance(ALGORITMO);
            c.init(Cipher.ENCRYPT_MODE, clave(), new GCMParameterSpec(BITS_ETIQUETA, iv));
            byte[] cifrado = c.doFinal(textoPlano.getBytes(StandardCharsets.UTF_8));
            byte[] todo = ByteBuffer.allocate(iv.length + cifrado.length).put(iv).put(cifrado).array();
            return PREFIJO + Base64.getEncoder().encodeToString(todo);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo cifrar el secreto", e);
        }
    }

    /**
     * @throws IllegalStateException si el valor no tiene el formato esperado o se cifró con otra clave (por ejemplo,
     *                               si se restauró una base de datos sin su {@code clave.key})
     */
    public String descifrar(String valorCifrado) {
        if (!esCifrado(valorCifrado)) {
            throw new IllegalArgumentException("El valor no está cifrado con " + PREFIJO);
        }
        try {
            byte[] todo = Base64.getDecoder().decode(valorCifrado.substring(PREFIJO.length()));
            if (todo.length <= BYTES_IV) {
                throw new IllegalStateException("Secreto cifrado incompleto");
            }
            Cipher c = Cipher.getInstance(ALGORITMO);
            c.init(Cipher.DECRYPT_MODE, clave(), new GCMParameterSpec(BITS_ETIQUETA, todo, 0, BYTES_IV));
            byte[] plano = c.doFinal(todo, BYTES_IV, todo.length - BYTES_IV);
            return new String(plano, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("No se pudo descifrar el secreto: la clave local (" + ARCHIVO_CLAVE
                    + ") no corresponde a esta base de datos. Vuelva a escribir el valor en Configuración.", e);
        }
    }

    /** Lee la clave o la crea la primera vez. */
    private SecretKey clave() {
        SecretKey k = clave;
        if (k == null) {
            synchronized (this) {
                k = clave;
                if (k == null) {
                    k = new SecretKeySpec(leerOCrear(), "AES");
                    clave = k;
                }
            }
        }
        return k;
    }

    private byte[] leerOCrear() {
        try {
            if (Files.exists(archivoClave)) {
                byte[] leida = Base64.getDecoder().decode(Files.readString(archivoClave, StandardCharsets.US_ASCII).trim());
                if (leida.length != BYTES_CLAVE) {
                    throw new IllegalStateException("El archivo " + archivoClave + " no contiene una clave AES-256 válida");
                }
                return leida;
            }
            byte[] nueva = new byte[BYTES_CLAVE];
            aleatorio.nextBytes(nueva);
            Files.createDirectories(archivoClave.getParent());
            Files.writeString(archivoClave, Base64.getEncoder().encodeToString(nueva), StandardCharsets.US_ASCII);
            return nueva;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer ni crear la clave de cifrado " + archivoClave, e);
        }
    }
}
