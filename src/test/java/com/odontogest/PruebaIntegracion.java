/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest;

import com.odontogest.config.DirectorioDatos;
import com.odontogest.modelo.Usuario;
import com.odontogest.persistencia.repositorio.UsuarioRepository;
import com.odontogest.seguridad.SesionUsuario;
import com.odontogest.servicio.dto.UsuarioSesion;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Base de las pruebas de integración: levanta el contexto de Spring contra una carpeta de datos
 * temporal, así nunca se toca la BD real de {@code ~/OdontoGest}. Todas las subclases comparten
 * el mismo contexto (y la misma BD SQLite temporal, con los datos demo del seeder).
 */
@SpringBootTest(properties = "app.seeder.datos-demo=false")
public abstract class PruebaIntegracion {

    protected static final Path HOME_PRUEBAS;

    static {
        try {
            Path temporal = Files.createTempDirectory("odontogest-test-");
            System.setProperty(DirectorioDatos.PROPIEDAD, temporal.toString());
            HOME_PRUEBAS = DirectorioDatos.inicializar();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Autowired private SesionUsuario sesionDePrueba;
    @Autowired private UsuarioRepository usuariosDePrueba;

    /** Abre la sesión sin pasar por BCrypt (más rápido); usuarios demo: admin, odontologo, recepcion. */
    protected UsuarioSesion iniciarSesion(String username) {
        Usuario u = usuariosDePrueba.findByUsernameIgnoreCase(username).orElseThrow();
        UsuarioSesion s = new UsuarioSesion(u.getId(), u.getUsername(), u.getNombreCompleto(), u.getRol().name(),
                u.getOdontologo() == null ? null : u.getOdontologo().getId());
        sesionDePrueba.iniciar(s);
        return s;
    }

    @AfterEach
    void cerrarSesionDePrueba() {
        sesionDePrueba.cerrar();
    }

    /** DNI de 8 dígitos aleatorio (empieza con 7 para no chocar con los demo, que empiezan con 9). */
    protected static String dniAleatorio() {
        return "7" + String.format("%07d", ThreadLocalRandom.current().nextInt(10_000_000));
    }

    /** Sufijo alfabético para nombres únicos (la búsqueda trabaja con palabras). */
    protected static String sufijo() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            sb.append((char) ('a' + ThreadLocalRandom.current().nextInt(26)));
        }
        return sb.toString();
    }
}
