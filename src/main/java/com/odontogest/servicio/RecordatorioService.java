/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio;

import com.odontogest.excepcion.IntegracionException;
import com.odontogest.integracion.ConectividadService;
import com.odontogest.integracion.correo.CorreoGateway;
import com.odontogest.integracion.correo.ParametrosSmtp;
import com.odontogest.modelo.Cita;
import com.odontogest.modelo.EstadoCita;
import com.odontogest.modelo.NotificacionLog;
import com.odontogest.persistencia.repositorio.CitaRepository;
import com.odontogest.persistencia.repositorio.NotificacionLogRepository;
import com.odontogest.servicio.dto.DatosClinica;
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

/**
 * Recordatorios de citas por correo (RF-AGE-06, PLAN §5.2.6). Lo ejecuta {@code RecordatorioScheduler} cada 15 minutos:
 * <ol>
 *   <li>si {@code recordatorio.habilitado} es falso o falta el SMTP, no hace nada;</li>
 *   <li>si no hay internet, lo intenta en el siguiente ciclo;</li>
 *   <li>busca citas pendientes que empiezan dentro de {@code recordatorio.horas_antes} horas sin recordatorio, de
 *       pacientes con correo que aceptan recordatorios;</li>
 *   <li>envía cada uno, registra {@code notificacion_log} y marca la cita; un fallo no detiene el lote (la cita
 *       fallida se reintenta en el siguiente ciclo).</li>
 * </ol>
 * El envío ocurre <b>fuera</b> de toda transacción: con un pool de una sola conexión, una transacción abierta durante
 * un envío SMTP lento bloquearía al resto de la aplicación. Se lee en una transacción corta y cada resultado se
 * registra en otra.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecordatorioService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.forLanguageTag("es-PE"));
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final CitaRepository citaRepository;
    private final NotificacionLogRepository logRepository;
    private final ConfiguracionService configuracion;
    private final IntegracionService integracion;
    private final ConectividadService conectividad;
    private final CorreoGateway correo;
    private final PlatformTransactionManager transacciones;

    /** Resultado de un ciclo. */
    public record Resultado(int enviados, int fallidos, String motivoOmitido) {

        static Resultado omitido(String motivo) {
            return new Resultado(0, 0, motivo);
        }
    }

    /** Recordatorio listo para enviar, leído dentro de una transacción. */
    private record Pendiente(long citaId, String destino, String cuerpo) {
    }

    public Resultado enviarPendientes(LocalDateTime ahora) {
        if (!configuracion.booleano("recordatorio.habilitado")) {
            return Resultado.omitido("recordatorios deshabilitados");
        }
        ParametrosSmtp smtp = integracion.parametrosSmtp();
        if (!smtp.configurado()) {
            return Resultado.omitido("SMTP sin configurar");
        }
        if (!conectividad.hayInternet()) {
            return Resultado.omitido("sin internet");
        }
        int horas = configuracion.entero("recordatorio.horas_antes");
        DatosClinica clinica = configuracion.datosClinica();
        TransactionTemplate tx = new TransactionTemplate(transacciones);
        List<Pendiente> pendientes = Objects.requireNonNull(tx.execute(estado -> citaRepository
                .pendientesDeRecordatorio(ahora, ahora.plusHours(horas), EnumSet.of(EstadoCita.PROGRAMADA, EstadoCita.CONFIRMADA))
                .stream()
                .map(c -> new Pendiente(c.getId(), c.getPaciente().getEmail().trim(), cuerpo(c, clinica)))
                .toList()));
        int enviados = 0;
        int fallidos = 0;
        LocalDateTime momento = ahora.truncatedTo(ChronoUnit.SECONDS);
        for (Pendiente p : pendientes) {
            String error = null;
            try {
                correo.enviar(smtp, p.destino(), "Recordatorio de su cita – " + clinica.nombre(), p.cuerpo());
                enviados++;
            } catch (IntegracionException e) {
                error = e.getMessage();
                fallidos++;
            }
            String detalle = error;
            tx.executeWithoutResult(estado -> {
                if (detalle == null) {
                    citaRepository.findById(p.citaId()).ifPresent(c -> c.setRecordatorioEnviadoEn(momento));
                }
                logRepository.save(NotificacionLog.de(p.citaId(), p.destino(),
                        detalle == null ? NotificacionLog.Estado.ENVIADO : NotificacionLog.Estado.FALLIDO, detalle, momento));
            });
        }
        if (!pendientes.isEmpty()) {
            log.info("Recordatorios: {} enviado(s), {} fallido(s)", enviados, fallidos);
        }
        return new Resultado(enviados, fallidos, null);
    }

    static String cuerpo(Cita c, DatosClinica clinica) {
        String fecha = FECHA.format(c.getInicio());
        StringBuilder sb = new StringBuilder();
        sb.append("Hola, ").append(c.getPaciente().getNombres()).append(":\n\n")
                .append("Le recordamos su cita el ").append(fecha).append(" a las ").append(HORA.format(c.getInicio()))
                .append(" con C.D. ").append(c.getOdontologo().getNombreCompleto()).append(" (").append(c.getMotivo()).append(").\n\n");
        if (!clinica.direccion().isBlank()) {
            sb.append("Dirección: ").append(clinica.direccion()).append("\n");
        }
        if (!clinica.telefono().isBlank()) {
            sb.append("Si no puede asistir, avísenos al ").append(clinica.telefono()).append(".\n");
        }
        sb.append("\n").append(clinica.nombre());
        return sb.toString();
    }
}
