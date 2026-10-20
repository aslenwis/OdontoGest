/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.seeder;

import com.odontogest.modelo.TipoConfiguracion;
import com.odontogest.persistencia.conversor.LocalDateTimeConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static com.odontogest.modelo.TipoConfiguracion.BOOLEANO;
import static com.odontogest.modelo.TipoConfiguracion.ENTERO;
import static com.odontogest.modelo.TipoConfiguracion.JSON;
import static com.odontogest.modelo.TipoConfiguracion.SECRETO;
import static com.odontogest.modelo.TipoConfiguracion.TEXTO;

/**
 * Inserta las claves de configuración por defecto (PLAN §3.4) y los correlativos {@code HC} y
 * {@code RECIBO}. Se ejecuta siempre; con {@code INSERT OR IGNORE} nunca pisa valores del usuario.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class ConfiguracionSeeder implements ApplicationRunner {

    private record ClaveDefecto(String clave, TipoConfiguracion tipo, String grupo, String valor,
                                boolean editable, String descripcion) {
    }

    private static final List<ClaveDefecto> CLAVES = List.of(
            new ClaveDefecto("esquema.version", ENTERO, "sistema", "1", false, "Versión del esquema de base de datos"),

            new ClaveDefecto("clinica.nombre", TEXTO, "clinica", "Consultorio Odontológico Demo", true, "Nombre de la clínica"),
            new ClaveDefecto("clinica.ruc", TEXTO, "clinica", "20000000001", true, "RUC (ficticio en la demo)"),
            new ClaveDefecto("clinica.direccion", TEXTO, "clinica", "Av. Demostración 123, Lima", true, "Dirección"),
            new ClaveDefecto("clinica.telefono", TEXTO, "clinica", "(01) 000-0000", true, "Teléfono"),
            new ClaveDefecto("clinica.email", TEXTO, "clinica", "contacto@consultorio-demo.pe", true, "Correo de contacto"),
            new ClaveDefecto("clinica.logo_ruta", TEXTO, "clinica", "", true, "Ruta del logo (vacío = logo por defecto)"),

            new ClaveDefecto("agenda.duracion_cita_min", ENTERO, "agenda", "30", true, "Duración por defecto de una cita (min)"),
            new ClaveDefecto("agenda.intervalo_min", ENTERO, "agenda", "15", true, "Intervalo de la grilla de agenda (min)"),
            new ClaveDefecto("agenda.dias_busqueda_hueco", ENTERO, "agenda", "30", true, "Días máximos para buscar el siguiente hueco"),

            new ClaveDefecto("recordatorio.habilitado", BOOLEANO, "recordatorio", "false", true, "Enviar recordatorios por correo"),
            new ClaveDefecto("recordatorio.horas_antes", ENTERO, "recordatorio", "24", true, "Horas de anticipación del recordatorio"),

            new ClaveDefecto("email.smtp.host", TEXTO, "email", "smtp.gmail.com", true, "Servidor SMTP"),
            new ClaveDefecto("email.smtp.puerto", ENTERO, "email", "587", true, "Puerto SMTP"),
            new ClaveDefecto("email.smtp.usuario", TEXTO, "email", "", true, "Usuario SMTP"),
            new ClaveDefecto("email.remitente", TEXTO, "email", "", true, "Correo remitente"),
            new ClaveDefecto("email.smtp.password", SECRETO, "email", "", true, "Contraseña SMTP (cifrada)"),
            new ClaveDefecto("email.smtp.starttls", BOOLEANO, "email", "true", true, "Usar STARTTLS"),

            new ClaveDefecto("api.dni.habilitado", BOOLEANO, "api_dni", "false", true, "Consultar DNI en API externa"),
            new ClaveDefecto("api.dni.url_plantilla", TEXTO, "api_dni", "", true, "URL con {numero}, p. ej. https://<proveedor>/v1/dni/{numero}"),
            new ClaveDefecto("api.dni.cabecera_auth", TEXTO, "api_dni", "Authorization", true, "Cabecera de autenticación"),
            new ClaveDefecto("api.dni.prefijo_token", TEXTO, "api_dni", "Bearer ", true, "Prefijo del token"),
            new ClaveDefecto("api.dni.token", SECRETO, "api_dni", "", true, "Token de la API (cifrado)"),
            new ClaveDefecto("api.dni.mapeo_json", JSON, "api_dni",
                    "{\"nombres\":\"nombres\",\"apellidoPaterno\":\"apellidoPaterno\",\"apellidoMaterno\":\"apellidoMaterno\"}",
                    true, "Nombres de los campos JSON de la respuesta"),
            new ClaveDefecto("api.timeout_seg", ENTERO, "api_dni", "6", true, "Tiempo máximo de espera (s)"),

            new ClaveDefecto("respaldo.automatico_al_cerrar", BOOLEANO, "respaldo", "true", true, "Respaldo automático al cerrar"),
            new ClaveDefecto("respaldo.mantener", ENTERO, "respaldo", "10", true, "Respaldos automáticos a conservar"),

            new ClaveDefecto("odontograma.excluir_terceros_molares_cpod", BOOLEANO, "odontograma", "true", true,
                    "Excluir terceras molares del CPO-D"),

            new ClaveDefecto("ui.tema", TEXTO, "ui", "CLARO", true, "Tema de la interfaz (CLARO u OSCURO)"));

    private static final List<String> SERIES_CORRELATIVO = List.of("HC", "RECIBO");

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String ahora = LocalDateTimeConverter.aTexto(LocalDateTime.now());
        int[] insertadas = jdbcTemplate.batchUpdate("""
                        INSERT OR IGNORE INTO configuracion (clave, valor, tipo, grupo, descripcion, editable, actualizado_en)
                        VALUES (?, ?, ?, ?, ?, ?, ?)""",
                CLAVES, CLAVES.size(), (ps, c) -> {
                    ps.setString(1, c.clave());
                    ps.setString(2, c.valor());
                    ps.setString(3, c.tipo().name());
                    ps.setString(4, c.grupo());
                    ps.setString(5, c.descripcion());
                    ps.setInt(6, c.editable() ? 1 : 0);
                    ps.setString(7, ahora);
                })[0];
        for (String serie : SERIES_CORRELATIVO) {
            jdbcTemplate.update("INSERT OR IGNORE INTO correlativo (serie, ultimo) VALUES (?, 0)", serie);
        }
        long nuevas = Arrays.stream(insertadas).filter(n -> n > 0).count();
        if (nuevas > 0) {
            log.info("Configuración por defecto: {} clave(s) nueva(s)", nuevas);
        }
    }
}
