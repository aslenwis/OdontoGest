package com.odontogest.servicio;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.odontogest.dominio.comun.ValidacionesASEJ;
import com.odontogest.excepcion.IntegracionExceptionASEJ;
import com.odontogest.integracion.ConectividadServiceASEJ;
import com.odontogest.integracion.correo.CorreoGatewayASEJ;
import com.odontogest.integracion.correo.ParametrosSmtpASEJ;
import com.odontogest.integracion.dni.ConsultaDniGatewayASEJ;
import com.odontogest.integracion.dni.ParametrosDniASEJ;
import com.odontogest.seguridad.PermisoASEJ;
import com.odontogest.seguridad.SesionUsuarioASEJ;
import com.odontogest.servicio.dto.PersonaDniDtoASEJ;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IntegracionServiceASEJ {

    private final Clock relojASEJ;
    private final ConfiguracionServiceASEJ configuracionASEJ;
    private final ConsultaDniGatewayASEJ dniGatewayASEJ;
    private final CorreoGatewayASEJ correoGatewayASEJ;
    private final ConectividadServiceASEJ conectividadASEJ;
    private final SesionUsuarioASEJ sesionUsuarioASEJ;

    public boolean enLineaASEJ() {
        return conectividadASEJ.ultimoEstadoASEJ();
    }

    public boolean dniHabilitadoASEJ() {
        return configuracionASEJ.booleanoASEJ("api.dni.habilitado") && !configuracionASEJ.textoASEJ("api.dni.url_plantilla").isBlank();
    }

    public Optional<PersonaDniDtoASEJ> consultarDniASEJ(String dniASEJ) {
        sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.PACIENTE_EDITAR);
        if (!dniHabilitadoASEJ()) {
            throw new IntegracionExceptionASEJ("La consulta de DNI no está habilitada (Configuración → API DNI).");
        }
        return consultarASEJ(dniASEJ);
    }

    public Optional<PersonaDniDtoASEJ> probarDniASEJ(String dniASEJ) {
        sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.CONFIGURACION);
        return consultarASEJ(dniASEJ);
    }

    private Optional<PersonaDniDtoASEJ> consultarASEJ(String dniASEJ) {
        String numeroASEJ = dniASEJ == null ? "" : dniASEJ.trim();
        if (!ValidacionesASEJ.esDniASEJ(numeroASEJ)) {
            throw new IntegracionExceptionASEJ("El DNI debe tener 8 dígitos.");
        }
        exigirInternetASEJ("consultar el DNI");
        return dniGatewayASEJ.consultarASEJ(numeroASEJ, parametrosDniASEJ())
                .map(pASEJ -> new PersonaDniDtoASEJ(pASEJ.nombresASEJ(), pASEJ.apellidoPaternoASEJ(), pASEJ.apellidoMaternoASEJ()));
    }

    public void enviarCorreoPruebaASEJ(String destinoASEJ) {
        sesionUsuarioASEJ.exigirASEJ(PermisoASEJ.CONFIGURACION);
        String dASEJ = destinoASEJ == null ? "" : destinoASEJ.trim();
        if (!ValidacionesASEJ.esEmailASEJ(dASEJ)) {
            throw new IntegracionExceptionASEJ("Escriba un correo de destino válido para la prueba.");
        }
        exigirInternetASEJ("enviar el correo");
        String clinicaASEJ = configuracionASEJ.textoASEJ("clinica.nombre");
        correoGatewayASEJ.enviarASEJ(parametrosSmtpASEJ(), dASEJ, "Correo de prueba – " + clinicaASEJ,
                "Este es un correo de prueba enviado desde OdontoGest el "
                        + DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(LocalDateTime.now(relojASEJ))
                        + ".\n\nSi lo recibe, la configuración del servidor de correo es correcta y los recordatorios "
                        + "de citas podrán enviarse.\n\n" + clinicaASEJ);
    }

    ParametrosSmtpASEJ parametrosSmtpASEJ() {
        return new ParametrosSmtpASEJ(configuracionASEJ.textoASEJ("email.smtp.host"), configuracionASEJ.enteroASEJ("email.smtp.puerto"),
                configuracionASEJ.textoASEJ("email.smtp.usuario"), configuracionASEJ.secretoASEJ("email.smtp.password").orElse(""),
                configuracionASEJ.booleanoASEJ("email.smtp.starttls"), configuracionASEJ.textoASEJ("email.remitente"),
                configuracionASEJ.textoASEJ("clinica.nombre"));
    }

    private ParametrosDniASEJ parametrosDniASEJ() {
        Map<String, String> mapeoASEJ = new HashMap<>();
        JsonObject jsonASEJ = configuracionASEJ.jsonASEJ("api.dni.mapeo_json");
        for (Map.Entry<String, JsonElement> eASEJ : jsonASEJ.entrySet()) {
            if (eASEJ.getValue().isJsonPrimitive()) {
                mapeoASEJ.put(eASEJ.getKey(), eASEJ.getValue().getAsString());
            }
        }
        return new ParametrosDniASEJ(configuracionASEJ.textoASEJ("api.dni.url_plantilla"), configuracionASEJ.textoASEJ("api.dni.cabecera_auth"),
                configuracionASEJ.textoASEJ("api.dni.prefijo_token"), configuracionASEJ.secretoASEJ("api.dni.token").orElse(""), mapeoASEJ,
                configuracionASEJ.enteroASEJ("api.timeout_seg"));
    }

    private void exigirInternetASEJ(String accionASEJ) {
        if (!conectividadASEJ.hayInternetASEJ()) {
            throw new IntegracionExceptionASEJ("Sin conexión a internet: no se puede " + accionASEJ + ". El resto del sistema "
                    + "funciona con normalidad.");
        }
    }
}
