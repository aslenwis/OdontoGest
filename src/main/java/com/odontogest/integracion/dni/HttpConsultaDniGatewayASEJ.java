package com.odontogest.integracion.dni;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.odontogest.excepcion.IntegracionExceptionASEJ;
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

@Component
@Slf4j
public class HttpConsultaDniGatewayASEJ implements ConsultaDniGatewayASEJ {

    @Override
    public Optional<DatosPersonaASEJ> consultarASEJ(String dniASEJ, ParametrosDniASEJ pASEJ) {
        if (dniASEJ == null || !dniASEJ.matches("\\d{8}")) {
            throw new IntegracionExceptionASEJ("El DNI debe tener 8 dígitos.");
        }
        if (pASEJ.urlPlantillaASEJ() == null || pASEJ.urlPlantillaASEJ().isBlank()) {
            throw new IntegracionExceptionASEJ("Configure la URL de la API de DNI en Configuración.");
        }
        URI uriASEJ;
        try {
            uriASEJ = URI.create(pASEJ.urlPlantillaASEJ().replace("{numero}", dniASEJ));
        } catch (IllegalArgumentException eASEJ) {
            throw new IntegracionExceptionASEJ("La URL de la API de DNI no es válida.");
        }
        Duration esperaASEJ = Duration.ofSeconds(Math.max(1, pASEJ.timeoutSegASEJ()));
        HttpRequest.Builder peticionASEJ = HttpRequest.newBuilder(uriASEJ).timeout(esperaASEJ).GET()
                .header("Accept", "application/json");
        if (pASEJ.cabeceraAuthASEJ() != null && !pASEJ.cabeceraAuthASEJ().isBlank() && pASEJ.tokenASEJ() != null && !pASEJ.tokenASEJ().isBlank()) {
            peticionASEJ.header(pASEJ.cabeceraAuthASEJ().trim(), (pASEJ.prefijoTokenASEJ() == null ? "" : pASEJ.prefijoTokenASEJ()) + pASEJ.tokenASEJ());
        }
        HttpClient clienteASEJ = HttpClient.newBuilder().connectTimeout(esperaASEJ).followRedirects(HttpClient.Redirect.NORMAL).build();
        HttpResponse<String> respuestaASEJ;
        try {
            respuestaASEJ = clienteASEJ.send(peticionASEJ.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (HttpTimeoutException eASEJ) {
            throw new IntegracionExceptionASEJ("La API de DNI no respondió a tiempo (" + esperaASEJ.toSeconds() + " s). Ingrese los datos a mano.");
        } catch (InterruptedException eASEJ) {
            Thread.currentThread().interrupt();
            throw new IntegracionExceptionASEJ("Se interrumpió la consulta del DNI.");
        } catch (IOException eASEJ) {
            throw new IntegracionExceptionASEJ("No se pudo conectar con la API de DNI. Verifique la conexión a internet y la URL.");
        }
        int estadoASEJ = respuestaASEJ.statusCode();
        if (estadoASEJ == 404) {
            return Optional.empty();
        }
        if (estadoASEJ == 401 || estadoASEJ == 403) {
            throw new IntegracionExceptionASEJ("La API de DNI rechazó el token (HTTP " + estadoASEJ + "). Revíselo en Configuración.");
        }
        if (estadoASEJ == 429) {
            throw new IntegracionExceptionASEJ("Se superó el límite de consultas de la API de DNI. Intente más tarde.");
        }
        if (estadoASEJ < 200 || estadoASEJ >= 300) {
            throw new IntegracionExceptionASEJ("La API de DNI respondió con un error (HTTP " + estadoASEJ + ").");
        }
        return leerASEJ(respuestaASEJ.body(), pASEJ);
    }

    static Optional<DatosPersonaASEJ> leerASEJ(String cuerpoASEJ, ParametrosDniASEJ pASEJ) {
        JsonObject jsonASEJ;
        try {
            jsonASEJ = JsonParser.parseString(cuerpoASEJ).getAsJsonObject();
        } catch (JsonParseException | IllegalStateException eASEJ) {
            throw new IntegracionExceptionASEJ("La API de DNI devolvió una respuesta que no es JSON.");
        }
        String nombresASEJ = campoASEJ(jsonASEJ, pASEJ.mapeoASEJ().getOrDefault("nombres", "nombres"));
        String paternoASEJ = campoASEJ(jsonASEJ, pASEJ.mapeoASEJ().getOrDefault("apellidoPaterno", "apellidoPaterno"));
        String maternoASEJ = campoASEJ(jsonASEJ, pASEJ.mapeoASEJ().getOrDefault("apellidoMaterno", "apellidoMaterno"));
        if (nombresASEJ == null && paternoASEJ == null) {
            return Optional.empty();
        }
        return Optional.of(new DatosPersonaASEJ(tituloASEJ(nombresASEJ), tituloASEJ(paternoASEJ), tituloASEJ(maternoASEJ)));
    }

    static String campoASEJ(JsonObject jsonASEJ, String rutaASEJ) {
        JsonElement actualASEJ = jsonASEJ;
        for (String parteASEJ : rutaASEJ.split("\\.")) {
            if (actualASEJ == null || !actualASEJ.isJsonObject()) {
                return null;
            }
            actualASEJ = actualASEJ.getAsJsonObject().get(parteASEJ);
        }
        if (actualASEJ == null || actualASEJ.isJsonNull() || !actualASEJ.isJsonPrimitive()) {
            return null;
        }
        String sASEJ = actualASEJ.getAsString().trim();
        return sASEJ.isEmpty() ? null : sASEJ;
    }

    static String tituloASEJ(String sASEJ) {
        if (sASEJ == null) {
            return null;
        }
        StringBuilder sbASEJ = new StringBuilder(sASEJ.length());
        boolean inicioASEJ = true;
        for (char cASEJ : sASEJ.toLowerCase(Locale.forLanguageTag("es-PE")).toCharArray()) {
            sbASEJ.append(inicioASEJ ? Character.toUpperCase(cASEJ) : cASEJ);
            inicioASEJ = cASEJ == ' ' || cASEJ == '-';
        }
        return sbASEJ.toString();
    }
}
