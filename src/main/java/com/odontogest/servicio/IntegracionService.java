/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.odontogest.dominio.comun.Validaciones;
import com.odontogest.excepcion.IntegracionException;
import com.odontogest.integracion.ConectividadService;
import com.odontogest.integracion.correo.CorreoGateway;
import com.odontogest.integracion.correo.ParametrosSmtp;
import com.odontogest.integracion.dni.ConsultaDniGateway;
import com.odontogest.integracion.dni.ParametrosDni;
import com.odontogest.seguridad.Permiso;
import com.odontogest.seguridad.SesionUsuario;
import com.odontogest.servicio.dto.PersonaDniDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Integraciones opcionales (RNF-03: la aplicación funciona sin ellas): consulta de DNI (RF-PAC-06) y correo de
 * prueba (RF-CFG-03). Arma los parámetros desde la configuración (los secretos descifrados solo aquí) y avisa sin
 * bloquear cuando no hay internet o falta configurar algo.
 */
@Service
@RequiredArgsConstructor
public class IntegracionService {

    private final Clock reloj;
    private final ConfiguracionService configuracion;
    private final ConsultaDniGateway dniGateway;
    private final CorreoGateway correoGateway;
    private final ConectividadService conectividad;
    private final SesionUsuario sesionUsuario;

    /** Último estado conocido de la conexión, sin consultar la red. */
    public boolean enLinea() {
        return conectividad.ultimoEstado();
    }

    /** La consulta de DNI está habilitada y tiene URL: el botón "Consultar DNI" se activa. */
    public boolean dniHabilitado() {
        return configuracion.booleano("api.dni.habilitado") && !configuracion.texto("api.dni.url_plantilla").isBlank();
    }

    /** Completa nombres desde la API de DNI (formulario del paciente). */
    public Optional<PersonaDniDto> consultarDni(String dni) {
        sesionUsuario.exigir(Permiso.PACIENTE_EDITAR);
        if (!dniHabilitado()) {
            throw new IntegracionException("La consulta de DNI no está habilitada (Configuración → API DNI).");
        }
        return consultar(dni);
    }

    /** "Probar conexión" en Configuración: usa la configuración guardada aunque la consulta esté deshabilitada. */
    public Optional<PersonaDniDto> probarDni(String dni) {
        sesionUsuario.exigir(Permiso.CONFIGURACION);
        return consultar(dni);
    }

    private Optional<PersonaDniDto> consultar(String dni) {
        String numero = dni == null ? "" : dni.trim();
        if (!Validaciones.esDni(numero)) {
            throw new IntegracionException("El DNI debe tener 8 dígitos.");
        }
        exigirInternet("consultar el DNI");
        return dniGateway.consultar(numero, parametrosDni())
                .map(p -> new PersonaDniDto(p.nombres(), p.apellidoPaterno(), p.apellidoMaterno()));
    }

    /** Envía un correo de prueba con la configuración SMTP guardada. */
    public void enviarCorreoPrueba(String destino) {
        sesionUsuario.exigir(Permiso.CONFIGURACION);
        String d = destino == null ? "" : destino.trim();
        if (!Validaciones.esEmail(d)) {
            throw new IntegracionException("Escriba un correo de destino válido para la prueba.");
        }
        exigirInternet("enviar el correo");
        String clinica = configuracion.texto("clinica.nombre");
        correoGateway.enviar(parametrosSmtp(), d, "Correo de prueba – " + clinica,
                "Este es un correo de prueba enviado desde OdontoGest el "
                        + DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(LocalDateTime.now(reloj))
                        + ".\n\nSi lo recibe, la configuración del servidor de correo es correcta y los recordatorios "
                        + "de citas podrán enviarse.\n\n" + clinica);
    }

    ParametrosSmtp parametrosSmtp() {
        return new ParametrosSmtp(configuracion.texto("email.smtp.host"), configuracion.entero("email.smtp.puerto"),
                configuracion.texto("email.smtp.usuario"), configuracion.secreto("email.smtp.password").orElse(""),
                configuracion.booleano("email.smtp.starttls"), configuracion.texto("email.remitente"),
                configuracion.texto("clinica.nombre"));
    }

    private ParametrosDni parametrosDni() {
        Map<String, String> mapeo = new HashMap<>();
        JsonObject json = configuracion.json("api.dni.mapeo_json");
        for (Map.Entry<String, JsonElement> e : json.entrySet()) {
            if (e.getValue().isJsonPrimitive()) {
                mapeo.put(e.getKey(), e.getValue().getAsString());
            }
        }
        return new ParametrosDni(configuracion.texto("api.dni.url_plantilla"), configuracion.texto("api.dni.cabecera_auth"),
                configuracion.texto("api.dni.prefijo_token"), configuracion.secreto("api.dni.token").orElse(""), mapeo,
                configuracion.entero("api.timeout_seg"));
    }

    private void exigirInternet(String accion) {
        if (!conectividad.hayInternet()) {
            throw new IntegracionException("Sin conexión a internet: no se puede " + accion + ". El resto del sistema "
                    + "funciona con normalidad.");
        }
    }
}
