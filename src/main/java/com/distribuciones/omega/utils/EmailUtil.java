package com.distribuciones.omega.utils;

import javax.mail.*;
import javax.mail.internet.*;

import io.github.cdimascio.dotenv.Dotenv;

import java.util.Properties;

/**
 * Utilidad para enviar correos electrónicos
 */
public class EmailUtil {
    // Configuración del servidor SMTP de Gmail
    private static final Dotenv dotenv = Dotenv.configure()
                                           .directory(".")
                                           .ignoreIfMissing()
                                           .load();
    private static final String EMAIL = dotenv.get("EMAIL");
    private static final String APP_PASS = dotenv.get("APP_PASS");
    
    private static final String HOST = "smtp.gmail.com";
    private static final int PORT = 587;
    private static final String USERNAME = EMAIL; 
    private static final String PASSWORD = APP_PASS; 
    private static final String FROM_NAME = "Distribuciones Omega";
    
    /**
     * Envía un correo electrónico de alerta
     * @param to Dirección de correo del destinatario
     * @param subject Asunto del correo
     * @param body Cuerpo del mensaje
     * @return true si el envío fue exitoso
     */
    public static boolean sendEmail(String to, String subject, String body) {
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", HOST);
        properties.put("mail.smtp.port", PORT);
        
        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USERNAME, PASSWORD);
            }
        });
        
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(USERNAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(body);
            
            Transport.send(message);
            return true;
        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        }
    }
}