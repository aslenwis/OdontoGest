/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.integracion.dni;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.odontogest.excepcion.IntegracionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

/**
 * Consulta de DNI por HTTP (PLAN §5.2.7): {@code java.net.http.HttpClient} con el tiempo de espera configurado, URL
 * con {@code {numero}} reemplazado, cabecera {@code cabecera_auth: prefijo + token} y respuesta JSON leída con Gson
 * según el mapeo configurado. HTTP 404 → vacío; otros errores → {@link IntegracionException} con un mensaje para el
 * usuario (nunca se registra el token).
 */
@Component
@Slf4j
public class HttpConsultaDniGateway implements ConsultaDniGateway {

    @Override
    public Optional<DatosPersona> consultar(String dni, ParametrosDni p) {
        if (dni == null || !dni.matches("\\d{8}")) {
            throw new IntegracionException("El DNI debe tener 8 dígitos.");
        }
        if (p.urlPlantilla() == null || p.urlPlantilla().isBlank()) {
            throw new IntegracionException("Configure la URL de la API de DNI en Configuración.");
        }
        URI uri;
        try {
            uri = URI.create(p.urlPlantilla().replace("{numero}", dni));
        } catch (IllegalArgumentException e) {
            throw new IntegracionException("La URL de la API de DNI no es válida.");
        }
        Duration espera = Duration.ofSeconds(Math.max(1, p.timeoutSeg()));
        HttpRequest.Builder peticion = HttpRequest.newBuilder(uri).timeout(espera).GET()
                .header("Accept", "application/json");
        if (p.cabeceraAuth() != null && !p.cabeceraAuth().isBlank() && p.token() != null && !p.token().isBlank()) {
            peticion.header(p.cabeceraAuth().trim(), (p.prefijoToken() == null ? "" : p.prefijoToken()) + p.token());
        }
        HttpClient cliente = HttpClient.newBuilder().connectTimeout(espera).followRedirects(HttpClient.Redirect.NORMAL).build();
        HttpResponse<String> respuesta;
        try {
            respuesta = cliente.send(peticion.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (HttpTimeoutException e) {
            throw new IntegracionException("La API de DNI no respondió a tiempo (" + espera.toSeconds() + " s). Ingrese los datos a mano.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IntegracionException("Se interrumpió la consulta del DNI.");
        } catch (IOException e) {
            throw new IntegracionException("No se pudo conectar con la API de DNI. Verifique la conexión a internet y la URL.");
        }
        int estado = respuesta.statusCode();
        if (estado == 404) {
            return Optional.empty();
        }
        if (estado == 401 || estado == 403) {
            throw new IntegracionException("La API de DNI rechazó el token (HTTP " + estado + "). Revíselo en Configuración.");
        }
        if (estado == 429) {
            throw new IntegracionException("Se superó el límite de consultas de la API de DNI. Intente más tarde.");
        }
        if (estado < 200 || estado >= 300) {
            throw new IntegracionException("La API de DNI respondió con un error (HTTP " + estado + ").");
        }
        return leer(respuesta.body(), p);
    }

    static Optional<DatosPersona> leer(String cuerpo, ParametrosDni p) {
        JsonObject json;
        try {
            json = JsonParser.parseString(cuerpo).getAsJsonObject();
        } catch (JsonParseException | IllegalStateException e) {
            throw new IntegracionException("La API de DNI devolvió una respuesta que no es JSON.");
        }
        String nombres = campo(json, p.mapeo().getOrDefault("nombres", "nombres"));
        String paterno = campo(json, p.mapeo().getOrDefault("apellidoPaterno", "apellidoPaterno"));
        String materno = campo(json, p.mapeo().getOrDefault("apellidoMaterno", "apellidoMaterno"));
        if (nombres == null && paterno == null) {
            // Algunos proveedores responden 200 con un cuerpo vacío o con un indicador de "no encontrado".
            return Optional.empty();
        }
        return Optional.of(new DatosPersona(titulo(nombres), titulo(paterno), titulo(materno)));
    }

    /** Valor de texto en una ruta con puntos ({@code data.nombres}); {@code null} si no existe o está vacío. */
    static String campo(JsonObject json, String ruta) {
        JsonElement actual = json;
        for (String parte : ruta.split("\\.")) {
            if (actual == null || !actual.isJsonObject()) {
                return null;
            }
            actual = actual.getAsJsonObject().get(parte);
        }
        if (actual == null || actual.isJsonNull() || !actual.isJsonPrimitive()) {
            return null;
        }
        String s = actual.getAsString().trim();
        return s.isEmpty() ? null : s;
    }

    /** "QUISPE MAMANI" → "Quispe Mamani" (las APIs suelen devolver mayúsculas). */
    static String titulo(String s) {
        if (s == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(s.length());
        boolean inicio = true;
        for (char c : s.toLowerCase(Locale.forLanguageTag("es-PE")).toCharArray()) {
            sb.append(inicio ? Character.toUpperCase(c) : c);
            inicio = c == ' ' || c == '-';
        }
        return sb.toString();
    }
}
