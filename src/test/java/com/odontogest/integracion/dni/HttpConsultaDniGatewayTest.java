/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.integracion.dni;

import com.odontogest.excepcion.IntegracionException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Consulta de DNI contra un servidor HTTP local (sin internet): mapeo configurable, cabecera de autenticación,
 * 404 → vacío y errores con mensajes para el usuario.
 */
class HttpConsultaDniGatewayTest {

    private final HttpConsultaDniGateway gateway = new HttpConsultaDniGateway();
    private HttpServer servidor;
    private final AtomicReference<String> autorizacion = new AtomicReference<>();

    @BeforeEach
    void iniciar() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/v1/dni/", intercambio -> {
            autorizacion.set(intercambio.getRequestHeaders().getFirst("Authorization"));
            String dni = intercambio.getRequestURI().getPath().substring("/v1/dni/".length());
            int estado;
            String cuerpo;
            switch (dni) {
                case "12345678" -> {
                    estado = 200;
                    cuerpo = "{\"success\":true,\"data\":{\"nombres\":\"ANA MARÍA\",\"apellido_paterno\":\"QUISPE\","
                            + "\"apellido_materno\":\"DE LA CRUZ\"}}";
                }
                case "00000000" -> {
                    estado = 404;
                    cuerpo = "{\"message\":\"no encontrado\"}";
                }
                case "11111111" -> {
                    estado = 401;
                    cuerpo = "{}";
                }
                case "22222222" -> {
                    estado = 200;
                    cuerpo = "<html>error</html>";
                }
                case "33333333" -> {
                    try {
                        Thread.sleep(2500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    estado = 200;
                    cuerpo = "{}";
                }
                default -> {
                    estado = 500;
                    cuerpo = "{}";
                }
            }
            byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
            intercambio.sendResponseHeaders(estado, bytes.length);
            try (OutputStream out = intercambio.getResponseBody()) {
                out.write(bytes);
            }
        });
        servidor.start();
    }

    @AfterEach
    void detener() {
        servidor.stop(0);
    }

    private ParametrosDni parametros(int timeout) {
        return new ParametrosDni("http://127.0.0.1:" + servidor.getAddress().getPort() + "/v1/dni/{numero}", "Authorization",
                "Bearer ", "tk-123", Map.of("nombres", "data.nombres", "apellidoPaterno", "data.apellido_paterno",
                "apellidoMaterno", "data.apellido_materno"), timeout);
    }

    @Test
    void leeLaRespuestaConElMapeoYEnviaElToken() {
        DatosPersona p = gateway.consultar("12345678", parametros(5)).orElseThrow();
        assertThat(p.nombres()).isEqualTo("Ana María");
        assertThat(p.apellidoPaterno()).isEqualTo("Quispe");
        assertThat(p.apellidoMaterno()).isEqualTo("De La Cruz");
        assertThat(autorizacion.get()).isEqualTo("Bearer tk-123");
    }

    @Test
    void noEncontradoYErrores() {
        assertThat(gateway.consultar("00000000", parametros(5))).isEmpty();
        assertThatThrownBy(() -> gateway.consultar("11111111", parametros(5)))
                .isInstanceOf(IntegracionException.class).hasMessageContaining("rechazó el token");
        assertThatThrownBy(() -> gateway.consultar("22222222", parametros(5))).hasMessageContaining("no es JSON");
        assertThatThrownBy(() -> gateway.consultar("44444444", parametros(5))).hasMessageContaining("HTTP 500");
        assertThatThrownBy(() -> gateway.consultar("33333333", parametros(1))).hasMessageContaining("no respondió a tiempo");
        assertThatThrownBy(() -> gateway.consultar("1234", parametros(5))).hasMessageContaining("8 dígitos");
    }

    @Test
    void sinServidorAvisaSinColgarse() {
        int puerto = servidor.getAddress().getPort();
        servidor.stop(0);
        ParametrosDni p = new ParametrosDni("http://127.0.0.1:" + puerto + "/v1/dni/{numero}", "", "", "", Map.of(), 2);
        assertThatThrownBy(() -> gateway.consultar("12345678", p)).isInstanceOf(IntegracionException.class)
                .hasMessageContaining("No se pudo conectar");
    }
}
