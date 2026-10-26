package com.odontogest.servicio;

import com.odontogest.excepcion.IntegracionExceptionASEJ;
import com.odontogest.integracion.ConectividadServiceASEJ;
import com.odontogest.integracion.correo.CorreoGatewayASEJ;
import com.odontogest.integracion.correo.ParametrosSmtpASEJ;
import com.odontogest.modelo.CitaASEJ;
import com.odontogest.modelo.EstadoCitaASEJ;
import com.odontogest.modelo.NotificacionLogASEJ;
import com.odontogest.persistencia.repositorio.CitaRepositoryASEJ;
import com.odontogest.persistencia.repositorio.NotificacionLogRepositoryASEJ;
import com.odontogest.servicio.dto.DatosClinicaASEJ;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecordatorioServiceASEJ {

    private static final DateTimeFormatter FECHAASEJ = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.forLanguageTag("es-PE"));
    private static final DateTimeFormatter HORAASEJ = DateTimeFormatter.ofPattern("HH:mm");

    private final CitaRepositoryASEJ citaRepositoryASEJ;
    private final NotificacionLogRepositoryASEJ logRepositoryASEJ;
    private final ConfiguracionServiceASEJ configuracionASEJ;
    private final IntegracionServiceASEJ integracionASEJ;
    private final ConectividadServiceASEJ conectividadASEJ;
    private final CorreoGatewayASEJ correoASEJ;
    private final PlatformTransactionManager transaccionesASEJ;

    public record ResultadoASEJ(int enviadosASEJ, int fallidosASEJ, String motivoOmitidoASEJ) {

        static ResultadoASEJ omitidoASEJ(String motivoASEJ) {
            return new ResultadoASEJ(0, 0, motivoASEJ);
        }
    }

    private record PendienteASEJ(long citaIdASEJ, String destinoASEJ, String cuerpoASEJ) {
    }

    public ResultadoASEJ enviarPendientesASEJ(LocalDateTime ahoraASEJ) {
        if (!configuracionASEJ.booleanoASEJ("recordatorio.habilitado")) {
            return ResultadoASEJ.omitidoASEJ("recordatorios deshabilitados");
        }
        ParametrosSmtpASEJ smtpASEJ = integracionASEJ.parametrosSmtpASEJ();
        if (!smtpASEJ.configuradoASEJ()) {
            return ResultadoASEJ.omitidoASEJ("SMTP sin configurar");
        }
        if (!conectividadASEJ.hayInternetASEJ()) {
            return ResultadoASEJ.omitidoASEJ("sin internet");
        }
        int horasASEJ = configuracionASEJ.enteroASEJ("recordatorio.horas_antes");
        DatosClinicaASEJ clinicaASEJ = configuracionASEJ.datosClinicaASEJ();
        TransactionTemplate txASEJ = new TransactionTemplate(transaccionesASEJ);
        List<PendienteASEJ> pendientesASEJ = Objects.requireNonNull(txASEJ.execute(estadoASEJ -> citaRepositoryASEJ
                .pendientesDeRecordatorioASEJ(ahoraASEJ, ahoraASEJ.plusHours(horasASEJ), EnumSet.of(EstadoCitaASEJ.PROGRAMADA, EstadoCitaASEJ.CONFIRMADA))
                .stream()
                .map(cASEJ -> new PendienteASEJ(cASEJ.getIdASEJ(), cASEJ.getPacienteASEJ().getEmailASEJ().trim(), cuerpoASEJ(cASEJ, clinicaASEJ)))
                .toList()));
        int enviadosASEJ = 0;
        int fallidosASEJ = 0;
        LocalDateTime momentoASEJ = ahoraASEJ.truncatedTo(ChronoUnit.SECONDS);
        for (PendienteASEJ pASEJ : pendientesASEJ) {
            String errorASEJ = null;
            try {
                correoASEJ.enviarASEJ(smtpASEJ, pASEJ.destinoASEJ(), "Recordatorio de su cita – " + clinicaASEJ.nombreASEJ(), pASEJ.cuerpoASEJ());
                enviadosASEJ++;
            } catch (IntegracionExceptionASEJ eASEJ) {
                errorASEJ = eASEJ.getMessage();
                fallidosASEJ++;
            }
            String detalleASEJ = errorASEJ;
            txASEJ.executeWithoutResult(estadoASEJ -> {
                if (detalleASEJ == null) {
                    citaRepositoryASEJ.findById(pASEJ.citaIdASEJ()).ifPresent(cASEJ -> cASEJ.setRecordatorioEnviadoEnASEJ(momentoASEJ));
                }
                logRepositoryASEJ.save(NotificacionLogASEJ.deASEJ(pASEJ.citaIdASEJ(), pASEJ.destinoASEJ(),
                        detalleASEJ == null ? NotificacionLogASEJ.EstadoASEJ.ENVIADO : NotificacionLogASEJ.EstadoASEJ.FALLIDO, detalleASEJ, momentoASEJ));
            });
        }
        if (!pendientesASEJ.isEmpty()) {
            logASEJ.info("Recordatorios: {} enviado(s), {} fallido(s)", enviadosASEJ, fallidosASEJ);
        }
        return new ResultadoASEJ(enviadosASEJ, fallidosASEJ, null);
    }

    static String cuerpoASEJ(CitaASEJ cASEJ, DatosClinicaASEJ clinicaASEJ) {
        String fechaASEJ = FECHAASEJ.format(cASEJ.getInicioASEJ());
        StringBuilder sbASEJ = new StringBuilder();
        sbASEJ.append("Hola, ").append(cASEJ.getPacienteASEJ().getNombresASEJ()).append(":\n\n")
                .append("Le recordamos su cita el ").append(fechaASEJ).append(" a las ").append(HORAASEJ.format(cASEJ.getInicioASEJ()))
                .append(" con C.D. ").append(cASEJ.getOdontologoASEJ().getNombreCompletoASEJ()).append(" (").append(cASEJ.getMotivoASEJ()).append(").\n\n");
        if (!clinicaASEJ.direccionASEJ().isBlank()) {
            sbASEJ.append("Dirección: ").append(clinicaASEJ.direccionASEJ()).append("\n");
        }
        if (!clinicaASEJ.telefonoASEJ().isBlank()) {
            sbASEJ.append("Si no puede asistir, avísenos al ").append(clinicaASEJ.telefonoASEJ()).append(".\n");
        }
        sbASEJ.append("\n").append(clinicaASEJ.nombreASEJ());
        return sbASEJ.toString();
    }
}
