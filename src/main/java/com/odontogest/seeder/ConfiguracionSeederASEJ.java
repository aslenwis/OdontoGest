package com.odontogest.seeder;

import com.odontogest.modelo.TipoConfiguracionASEJ;
import com.odontogest.persistencia.conversor.LocalDateTimeConverterASEJ;
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

import static com.odontogest.modelo.TipoConfiguracionASEJ.BOOLEANO;
import static com.odontogest.modelo.TipoConfiguracionASEJ.ENTERO;
import static com.odontogest.modelo.TipoConfiguracionASEJ.JSON;
import static com.odontogest.modelo.TipoConfiguracionASEJ.SECRETO;
import static com.odontogest.modelo.TipoConfiguracionASEJ.TEXTO;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class ConfiguracionSeederASEJ implements ApplicationRunner {

    private record ClaveDefectoASEJ(String claveASEJ, TipoConfiguracionASEJ tipoASEJ, String grupoASEJ, String valorASEJ,
                                boolean editableASEJ, String descripcionASEJ) {
    }

    private static final List<ClaveDefectoASEJ> CLAVESASEJ = List.of(
            new ClaveDefectoASEJ("esquema.version", ENTERO, "sistema", "1", false, "Versión del esquema de base de datos"),
            new ClaveDefectoASEJ("seeder.ultimo_hc_demo", TEXTO, "sistema", "", false,
                    "Última historia clínica creada por el seeder demo (los pacientes hasta ella se marcan como demo)"),

            new ClaveDefectoASEJ("clinica.nombre", TEXTO, "clinica", "Consultorio Odontológico Demo", true, "Nombre de la clínica"),
            new ClaveDefectoASEJ("clinica.ruc", TEXTO, "clinica", "20000000001", true, "RUC (ficticio en la demo)"),
            new ClaveDefectoASEJ("clinica.direccion", TEXTO, "clinica", "Av. Demostración 123, Lima", true, "Dirección"),
            new ClaveDefectoASEJ("clinica.telefono", TEXTO, "clinica", "(01) 000-0000", true, "Teléfono"),
            new ClaveDefectoASEJ("clinica.email", TEXTO, "clinica", "contacto@consultorio-demo.pe", true, "Correo de contacto"),
            new ClaveDefectoASEJ("clinica.logo_ruta", TEXTO, "clinica", "", true, "Ruta del logo (vacío = logo por defecto)"),

            new ClaveDefectoASEJ("agenda.duracion_cita_min", ENTERO, "agenda", "30", true, "Duración por defecto de una cita (min)"),
            new ClaveDefectoASEJ("agenda.intervalo_min", ENTERO, "agenda", "15", true, "Intervalo de la grilla de agenda (min)"),
            new ClaveDefectoASEJ("agenda.dias_busqueda_hueco", ENTERO, "agenda", "30", true, "Días máximos para buscar el siguiente hueco"),

            new ClaveDefectoASEJ("recordatorio.habilitado", BOOLEANO, "recordatorio", "false", true, "Enviar recordatorios por correo"),
            new ClaveDefectoASEJ("recordatorio.horas_antes", ENTERO, "recordatorio", "24", true, "Horas de anticipación del recordatorio"),

            new ClaveDefectoASEJ("email.smtp.host", TEXTO, "email", "smtp.gmail.com", true, "Servidor SMTP"),
            new ClaveDefectoASEJ("email.smtp.puerto", ENTERO, "email", "587", true, "Puerto SMTP"),
            new ClaveDefectoASEJ("email.smtp.usuario", TEXTO, "email", "", true, "Usuario SMTP"),
            new ClaveDefectoASEJ("email.remitente", TEXTO, "email", "", true, "Correo remitente"),
            new ClaveDefectoASEJ("email.smtp.password", SECRETO, "email", "", true, "Contraseña SMTP (cifrada)"),
            new ClaveDefectoASEJ("email.smtp.starttls", BOOLEANO, "email", "true", true, "Usar STARTTLS"),

            new ClaveDefectoASEJ("api.dni.habilitado", BOOLEANO, "api_dni", "false", true, "Consultar DNI en API externa"),
            new ClaveDefectoASEJ("api.dni.url_plantilla", TEXTO, "api_dni", "", true, "URL con {numero}, p. ej. https://<proveedor>/v1/dni/{numero}"),
            new ClaveDefectoASEJ("api.dni.cabecera_auth", TEXTO, "api_dni", "Authorization", true, "Cabecera de autenticación"),
            new ClaveDefectoASEJ("api.dni.prefijo_token", TEXTO, "api_dni", "Bearer ", true, "Prefijo del token"),
            new ClaveDefectoASEJ("api.dni.token", SECRETO, "api_dni", "", true, "Token de la API (cifrado)"),
            new ClaveDefectoASEJ("api.dni.mapeo_json", JSON, "api_dni",
                    "{\"nombres\":\"nombres\",\"apellidoPaterno\":\"apellidoPaterno\",\"apellidoMaterno\":\"apellidoMaterno\"}",
                    true, "Nombres de los campos JSON de la respuesta"),
            new ClaveDefectoASEJ("api.timeout_seg", ENTERO, "api_dni", "6", true, "Tiempo máximo de espera (s)"),

            new ClaveDefectoASEJ("respaldo.automatico_al_cerrar", BOOLEANO, "respaldo", "true", true, "Respaldo automático al cerrar"),
            new ClaveDefectoASEJ("respaldo.mantener", ENTERO, "respaldo", "10", true, "Respaldos automáticos a conservar"),

            new ClaveDefectoASEJ("odontograma.excluir_terceros_molares_cpod", BOOLEANO, "odontograma", "true", true,
                    "Excluir terceras molares del CPO-D"),

            new ClaveDefectoASEJ("ui.tema", TEXTO, "ui", "CLARO", true, "Tema de la interfaz (CLARO u OSCURO)"));

    private static final List<String> SERIES_CORRELATIVOASEJ = List.of("HC", "RECIBO");

    private final JdbcTemplate jdbcTemplateASEJ;

    @Override
    @Transactional
    public void run(ApplicationArguments argsASEJ) {
        String ahoraASEJ = LocalDateTimeConverterASEJ.aTextoASEJ(LocalDateTime.now());
        int[] insertadasASEJ = jdbcTemplateASEJ.batchUpdate("""
                        INSERT OR IGNORE INTO configuracion (clave, valor, tipo, grupo, descripcion, editable, actualizado_en)
                        VALUES (?, ?, ?, ?, ?, ?, ?)""",
                CLAVESASEJ, CLAVESASEJ.size(), (psASEJ, cASEJ) -> {
                    psASEJ.setString(1, cASEJ.claveASEJ());
                    psASEJ.setString(2, cASEJ.valorASEJ());
                    psASEJ.setString(3, cASEJ.tipoASEJ().name());
                    psASEJ.setString(4, cASEJ.grupoASEJ());
                    psASEJ.setString(5, cASEJ.descripcionASEJ());
                    psASEJ.setInt(6, cASEJ.editableASEJ() ? 1 : 0);
                    psASEJ.setString(7, ahoraASEJ);
                })[0];
        for (String serieASEJ : SERIES_CORRELATIVOASEJ) {
            jdbcTemplateASEJ.update("INSERT OR IGNORE INTO correlativo (serie, ultimo) VALUES (?, 0)", serieASEJ);
        }
        long nuevasASEJ = Arrays.stream(insertadasASEJ).filter(nASEJ -> nASEJ > 0).count();
        if (nuevasASEJ > 0) {
            logASEJ.info("Configuración por defecto: {} clave(s) nueva(s)", nuevasASEJ);
        }
    }
}
