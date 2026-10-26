package com.odontogest.integracion.dni;

import com.odontogest.excepcion.IntegracionExceptionASEJ;
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

class HttpConsultaDniGatewayTestASEJ {

    private final HttpConsultaDniGatewayASEJ gatewayASEJ = new HttpConsultaDniGatewayASEJ();
    private HttpServer servidorASEJ;
    private final AtomicReference<String> autorizacionASEJ = new AtomicReference<>();

    @BeforeEach
    void iniciarASEJ() throws IOException {
        servidorASEJ = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidorASEJ.createContext("/v1/dni/", intercambioASEJ -> {
            autorizacionASEJ.set(intercambioASEJ.getRequestHeaders().getFirst("Authorization"));
            String dniASEJ = intercambioASEJ.getRequestURI().getPath().substring("/v1/dni/".length());
            int estadoASEJ;
            String cuerpoASEJ;
            switch (dniASEJ) {
                case "12345678" -> {
                    estadoASEJ = 200;
                    cuerpoASEJ = "{\"success\":true,\"data\":{\"nombres\":\"ANA MARÍA\",\"apellido_paterno\":\"QUISPE\","
                            + "\"apellido_materno\":\"DE LA CRUZ\"}}";
                }
                case "00000000" -> {
                    estadoASEJ = 404;
                    cuerpoASEJ = "{\"message\":\"no encontrado\"}";
                }
                case "11111111" -> {
                    estadoASEJ = 401;
                    cuerpoASEJ = "{}";
                }
                case "22222222" -> {
                    estadoASEJ = 200;
                    cuerpoASEJ = "<html>error</html>";
                }
                case "33333333" -> {
                    try {
                        Thread.sleep(2500);
                    } catch (InterruptedException eASEJ) {
                        Thread.currentThread().interrupt();
                    }
                    estadoASEJ = 200;
                    cuerpoASEJ = "{}";
                }
                default -> {
                    estadoASEJ = 500;
                    cuerpoASEJ = "{}";
                }
            }
            byte[] bytesASEJ = cuerpoASEJ.getBytes(StandardCharsets.UTF_8);
            intercambioASEJ.sendResponseHeaders(estadoASEJ, bytesASEJ.length);
            try (OutputStream outASEJ = intercambioASEJ.getResponseBody()) {
                outASEJ.write(bytesASEJ);
            }
        });
        servidorASEJ.start();
    }

    @AfterEach
    void detenerASEJ() {
        servidorASEJ.stop(0);
    }

    private ParametrosDniASEJ parametrosASEJ(int timeoutASEJ) {
        return new ParametrosDniASEJ("http://127.0.0.1:" + servidorASEJ.getAddress().getPort() + "/v1/dni/{numero}", "Authorization",
                "Bearer ", "tk-123", Map.of("nombres", "data.nombres", "apellidoPaterno", "data.apellido_paterno",
                "apellidoMaterno", "data.apellido_materno"), timeoutASEJ);
    }

    @Test
    void leeLaRespuestaConElMapeoYEnviaElTokenASEJ() {
        DatosPersonaASEJ pASEJ = gatewayASEJ.consultarASEJ("12345678", parametrosASEJ(5)).orElseThrow();
        assertThat(pASEJ.nombresASEJ()).isEqualTo("Ana María");
        assertThat(pASEJ.apellidoPaternoASEJ()).isEqualTo("Quispe");
        assertThat(pASEJ.apellidoMaternoASEJ()).isEqualTo("De La Cruz");
        assertThat(autorizacionASEJ.get()).isEqualTo("Bearer tk-123");
    }

    @Test
    void noEncontradoYErroresASEJ() {
        assertThat(gatewayASEJ.consultarASEJ("00000000", parametrosASEJ(5))).isEmpty();
        assertThatThrownBy(() -> gatewayASEJ.consultarASEJ("11111111", parametrosASEJ(5)))
                .isInstanceOf(IntegracionExceptionASEJ.class).hasMessageContaining("rechazó el token");
        assertThatThrownBy(() -> gatewayASEJ.consultarASEJ("22222222", parametrosASEJ(5))).hasMessageContaining("no es JSON");
        assertThatThrownBy(() -> gatewayASEJ.consultarASEJ("44444444", parametrosASEJ(5))).hasMessageContaining("HTTP 500");
        assertThatThrownBy(() -> gatewayASEJ.consultarASEJ("33333333", parametrosASEJ(1))).hasMessageContaining("no respondió a tiempo");
        assertThatThrownBy(() -> gatewayASEJ.consultarASEJ("1234", parametrosASEJ(5))).hasMessageContaining("8 dígitos");
    }

    @Test
    void sinServidorAvisaSinColgarseASEJ() {
        int puertoASEJ = servidorASEJ.getAddress().getPort();
        servidorASEJ.stop(0);
        ParametrosDniASEJ pASEJ = new ParametrosDniASEJ("http://127.0.0.1:" + puertoASEJ + "/v1/dni/{numero}", "", "", "", Map.of(), 2);
        assertThatThrownBy(() -> gatewayASEJ.consultarASEJ("12345678", pASEJ)).isInstanceOf(IntegracionExceptionASEJ.class)
                .hasMessageContaining("No se pudo conectar");
    }
}
