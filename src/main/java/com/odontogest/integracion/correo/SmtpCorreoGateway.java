/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.integracion.correo;

import com.odontogest.excepcion.IntegracionException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Envío por SMTP (PLAN §5.2.6): crea un {@link JavaMailSenderImpl} con la configuración actual en cada envío, así
 * un cambio en Configuración rige de inmediato sin reiniciar. Tiempos de espera cortos para no colgar la interfaz.
 */
@Component
@Slf4j
public class SmtpCorreoGateway implements CorreoGateway {

    private static final String ESPERA_MS = "10000";

    @Override
    public void enviar(ParametrosSmtp p, String destino, String asunto, String cuerpo) {
        if (!p.configurado()) {
            throw new IntegracionException("Configure el servidor SMTP y el correo remitente en Configuración.");
        }
        JavaMailSenderImpl remitente = new JavaMailSenderImpl();
        remitente.setHost(p.host().trim());
        remitente.setPort(p.puerto());
        remitente.setDefaultEncoding(StandardCharsets.UTF_8.name());
        boolean autentica = p.usuario() != null && !p.usuario().isBlank();
        if (autentica) {
            remitente.setUsername(p.usuario().trim());
            remitente.setPassword(p.password() == null ? "" : p.password());
        }
        Properties props = remitente.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", String.valueOf(autentica));
        props.put("mail.smtp.starttls.enable", String.valueOf(p.starttls()));
        props.put("mail.smtp.starttls.required", String.valueOf(p.starttls()));
        props.put("mail.smtp.ssl.enable", String.valueOf(p.puerto() == 465));
        props.put("mail.smtp.connectiontimeout", ESPERA_MS);
        props.put("mail.smtp.timeout", ESPERA_MS);
        props.put("mail.smtp.writetimeout", ESPERA_MS);
        try {
            MimeMessage mensaje = remitente.createMimeMessage();
            MimeMessageHelper ayuda = new MimeMessageHelper(mensaje, false, StandardCharsets.UTF_8.name());
            ayuda.setFrom(new InternetAddress(p.desde().trim(), p.nombreRemitente(), StandardCharsets.UTF_8.name()));
            ayuda.setTo(destino.trim());
            ayuda.setSubject(asunto);
            ayuda.setText(cuerpo, false);
            remitente.send(mensaje);
        } catch (MailAuthenticationException e) {
            throw new IntegracionException("El servidor de correo rechazó el usuario o la contraseña. En Gmail use una "
                    + "contraseña de aplicación.");
        } catch (MailException | MessagingException | UnsupportedEncodingException e) {
            log.warn("No se pudo enviar el correo a {}: {}", destino, e.getMessage());
            throw new IntegracionException("No se pudo enviar el correo: " + causaLegible(e));
        }
    }

    private static String causaLegible(Exception e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        String m = t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
        if (t instanceof UnknownHostException) {
            return "no se encontró el servidor " + m + " (¿hay internet?).";
        }
        if (t instanceof ConnectException || t instanceof SocketTimeoutException) {
            return "no se pudo conectar con el servidor SMTP (" + m + ").";
        }
        return m;
    }
}
