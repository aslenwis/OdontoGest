/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio;

import com.odontogest.config.DirectorioDatos;
import com.odontogest.config.RestauracionPendiente;
import com.odontogest.servicio.dto.InformacionSistemaDto;
import com.odontogest.servicio.dto.IntegridadBaseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Información del sistema para "Acerca de" y verificación de integridad de la base (PLAN §6.1). No exige sesión:
 * "Acerca de" también se abre desde la pantalla de inicio de sesión y solo lee.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SistemaService {

    public static final String NOMBRE = "OdontoGest";
    public static final String DESCRIPCION = "Sistema de gestión de consultorio odontológico con odontograma interactivo";
    public static final List<String> AUTORES = List.of("Apaza Mamani Ruben", "Apaza Suca Elwin junior",
            "Inofuente Trujillo David Alex");
    public static final String CURSO = "Ingeniería de Software";
    public static final String UNIVERSIDAD = "Universidad Nacional de Juliaca";
    public static final int ANIO = 2026;
    public static final String AVISO = "Programa de ordenador (soporte lógico) protegido por el Decreto Legislativo"
            + " N.° 822, Ley sobre el Derecho de Autor. © " + ANIO + " los autores. Todos los derechos reservados."
            + " Prohibida su reproducción, distribución o modificación sin autorización.";
    private static final int MAXIMO_PROBLEMAS = 100;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectProvider<BuildProperties> buildProperties;

    public InformacionSistemaDto informacion() {
        BuildProperties build = buildProperties.getIfAvailable();
        Path home = DirectorioDatos.home().toAbsolutePath();
        return new InformacionSistemaDto(NOMBRE, DESCRIPCION, build == null ? "desarrollo" : build.getVersion(),
                build == null || build.getTime() == null ? null
                        : LocalDateTime.ofInstant(build.getTime(), ZoneId.systemDefault()),
                AUTORES, CURSO, UNIVERSIDAD, ANIO, AVISO,
                System.getProperty("java.vendor") + " " + System.getProperty("java.version"),
                home.toString(), home.resolve(RestauracionPendiente.BASE).toString(),
                home.resolve(DirectorioDatos.LOGS).toString(), home.resolve(DirectorioDatos.RESPALDOS).toString());
    }

    /** {@code PRAGMA integrity_check} (estructura de páginas e índices) y {@code PRAGMA foreign_key_check}. */
    public IntegridadBaseDto verificarIntegridad() {
        List<String> mensajes = jdbcTemplate.queryForList("PRAGMA integrity_check(" + MAXIMO_PROBLEMAS + ")", String.class);
        List<String> problemas = mensajes.stream().filter(m -> !"ok".equalsIgnoreCase(m)).toList();
        int foraneas = jdbcTemplate.queryForList("PRAGMA foreign_key_check").size();
        long tamanio;
        try {
            tamanio = Files.size(DirectorioDatos.home().resolve(RestauracionPendiente.BASE));
        } catch (IOException e) {
            tamanio = -1;
        }
        if (!problemas.isEmpty() || foraneas > 0) {
            log.error("Verificación de integridad: {} problema(s), {} clave(s) foránea(s) rota(s): {}", problemas.size(),
                    foraneas, problemas);
        } else {
            log.info("Verificación de integridad: correcta");
        }
        return new IntegridadBaseDto(problemas, foraneas, tamanio);
    }
}
