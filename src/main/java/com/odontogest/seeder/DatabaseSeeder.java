/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.seeder;

import com.odontogest.persistencia.repositorio.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orquestador de los datos de demostración ({@code app.seeder.enabled=true}).
 * <p>
 * Idempotente: si la tabla {@code usuario} ya tiene filas no hace nada. En la Fase 0 solo crea
 * el usuario administrador; odontólogos, pacientes y datos clínicos se agregan en las fases siguientes.
 */
@Component
@Order(3)
@ConditionalOnProperty(name = "app.seeder.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioSeeder usuarioSeeder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarioRepository.count() > 0) {
            return;
        }
        log.info("Base de datos vacía: cargando datos de demostración");
        usuarioSeeder.sembrar();
    }
}
