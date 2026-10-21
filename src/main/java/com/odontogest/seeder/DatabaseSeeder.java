/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.seeder;

import com.odontogest.config.RelojAplicacion;
import com.odontogest.modelo.Odontologo;
import com.odontogest.persistencia.repositorio.UsuarioRepository;
import com.odontogest.servicio.ConfiguracionService;
import com.odontogest.servicio.PacienteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Orquestador de los datos de demostración ({@code app.seeder.enabled=true}, PLAN §3.5).
 * <ol>
 *   <li>Odontólogos con su horario y usuarios demo, en una transacción.</li>
 *   <li>Si {@code app.seeder.datos-demo=true}: pacientes e historia clínica de unos seis meses
 *       ({@link ClinicaDemoSeeder}). Sin transacción envolvente: cada servicio confirma la suya, como en la
 *       interfaz. Al terminar guarda {@code seeder.ultimo_hc_demo} para marcar a los pacientes demo.</li>
 * </ol>
 * Idempotente: si la tabla {@code usuario} ya tiene filas no hace nada. Un error en los datos demo se registra en
 * el log pero no impide abrir la aplicación.
 */
@Component
@Order(3)
@ConditionalOnProperty(name = "app.seeder.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements ApplicationRunner {

    public static final String CLAVE_ULTIMO_HC_DEMO = PacienteService.CLAVE_ULTIMO_HC_DEMO;

    private final UsuarioRepository usuarioRepository;
    private final UsuarioSeeder usuarioSeeder;
    private final OdontologoSeeder odontologoSeeder;
    private final ClinicaDemoSeeder clinicaDemoSeeder;
    private final ConfiguracionService configuracionService;
    private final RelojAplicacion reloj;
    private final PlatformTransactionManager transacciones;

    @Value("${app.seeder.datos-demo:true}")
    private boolean datosDemo;
    @Value("${app.seeder.semilla:2026}")
    private long semilla;

    @Override
    public void run(ApplicationArguments args) {
        if (usuarioRepository.count() > 0) {
            return;
        }
        LocalDateTime ahora = LocalDateTime.now(reloj);
        log.info("Base de datos vacía: cargando datos de demostración");
        // Los usuarios existen desde antes de la historia demo (su fecha de creación es coherente con ella).
        reloj.fijar(ahora.minusDays(ClinicaDemoSeeder.DIAS_HISTORIA + 1L).withHour(8).withMinute(0));
        List<Odontologo> odontologos;
        try {
            odontologos = new TransactionTemplate(transacciones).execute(s -> {
                List<Odontologo> creados = odontologoSeeder.sembrar();
                usuarioSeeder.sembrar(creados.get(0));
                return creados;
            });
        } finally {
            reloj.liberar();
        }
        if (!datosDemo || odontologos == null) {
            return;
        }
        long inicio = System.currentTimeMillis();
        try {
            ClinicaDemoSeeder.Resumen r = clinicaDemoSeeder.sembrar(odontologos, semilla, ahora);
            configuracionService.guardarSistema(CLAVE_ULTIMO_HC_DEMO, r.ultimaHistoria());
            log.info("Datos demo en {} s: {} pacientes, {} primeras consultas, {} evoluciones, {} pagos, {} citas en ±30 días"
                            + " ({} pasos omitidos)", (System.currentTimeMillis() - inicio) / 1000, r.pacientes(),
                    r.primerasConsultas(), r.evoluciones(), r.pagos(), r.citasVentana(), r.omitidos());
        } catch (RuntimeException e) {
            log.error("No se pudieron cargar todos los datos de demostración", e);
        }
    }
}
