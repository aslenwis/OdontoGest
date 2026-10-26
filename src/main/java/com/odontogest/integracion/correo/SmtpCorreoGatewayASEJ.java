package com.odontogest.integracion.correo;

import com.odontogest.excepcion.IntegracionExceptionASEJ;
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

@Component
@Slf4j
public class SmtpCorreoGatewayASEJ implements CorreoGatewayASEJ {

    private static final String ESPERA_MSASEJ = "10000";

    @Override
    public void enviarASEJ(ParametrosSmtpASEJ pASEJ, String destinoASEJ, String asuntoASEJ, String cuerpoASEJ) {
        if (!pASEJ.configuradoASEJ()) {
            throw new IntegracionExceptionASEJ("Configure el servidor SMTP y el correo remitente en Configuración.");
        }
        JavaMailSenderImpl remitenteASEJ = new JavaMailSenderImpl();
        remitenteASEJ.setHost(pASEJ.hostASEJ().trim());
        remitenteASEJ.setPort(pASEJ.puertoASEJ());
        remitenteASEJ.setDefaultEncoding(StandardCharsets.UTF_8.name());
        boolean autenticaASEJ = pASEJ.usuarioASEJ() != null && !pASEJ.usuarioASEJ().isBlank();
        if (autenticaASEJ) {
            remitenteASEJ.setUsername(pASEJ.usuarioASEJ().trim());
            remitenteASEJ.setPassword(pASEJ.passwordASEJ() == null ? "" : pASEJ.passwordASEJ());
        }
        Properties propsASEJ = remitenteASEJ.getJavaMailProperties();
        propsASEJ.put("mail.transport.protocol", "smtp");
        propsASEJ.put("mail.smtp.auth", String.valueOf(autenticaASEJ));
        propsASEJ.put("mail.smtp.starttls.enable", String.valueOf(pASEJ.starttlsASEJ()));
        propsASEJ.put("mail.smtp.starttls.required", String.valueOf(pASEJ.starttlsASEJ()));
        propsASEJ.put("mail.smtp.ssl.enable", String.valueOf(pASEJ.puertoASEJ() == 465));
        propsASEJ.put("mail.smtp.connectiontimeout", ESPERA_MSASEJ);
        propsASEJ.put("mail.smtp.timeout", ESPERA_MSASEJ);
        propsASEJ.put("mail.smtp.writetimeout", ESPERA_MSASEJ);
        try {
            MimeMessage mensajeASEJ = remitenteASEJ.createMimeMessage();
            MimeMessageHelper ayudaASEJ = new MimeMessageHelper(mensajeASEJ, false, StandardCharsets.UTF_8.name());
            ayudaASEJ.setFrom(new InternetAddress(pASEJ.desdeASEJ().trim(), pASEJ.nombreRemitenteASEJ(), StandardCharsets.UTF_8.name()));
            ayudaASEJ.setTo(destinoASEJ.trim());
            ayudaASEJ.setSubject(asuntoASEJ);
            ayudaASEJ.setText(cuerpoASEJ, false);
            remitenteASEJ.send(mensajeASEJ);
        } catch (MailAuthenticationException eASEJ) {
            throw new IntegracionExceptionASEJ("El servidor de correo rechazó el usuario o la contraseña. En Gmail use una "
                    + "contraseña de aplicación.");
        } catch (MailException | MessagingException | UnsupportedEncodingException eASEJ) {
            logASEJ.warn("No se pudo enviar el correo a {}: {}", destinoASEJ, eASEJ.getMessage());
            throw new IntegracionExceptionASEJ("No se pudo enviar el correo: " + causaLegibleASEJ(eASEJ));
        }
    }

    private static String causaLegibleASEJ(Exception eASEJ) {
        Throwable tASEJ = eASEJ;
        while (tASEJ.getCause() != null && tASEJ.getCause() != tASEJ) {
            tASEJ = tASEJ.getCause();
        }
        String mASEJ = tASEJ.getMessage() == null ? tASEJ.getClass().getSimpleName() : tASEJ.getMessage();
        if (tASEJ instanceof UnknownHostException) {
            return "no se encontró el servidor " + mASEJ + " (¿hay internet?).";
        }
        if (tASEJ instanceof ConnectException || tASEJ instanceof SocketTimeoutException) {
            return "no se pudo conectar con el servidor SMTP (" + mASEJ + ").";
        }
        return mASEJ;
    }
}
