package com.ws.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendActivationEmail(String to, String activationToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String activationUrl = frontendUrl + "/activate?token=" + activationToken;

            helper.setTo(to);
            helper.setSubject("Activate Your Account");
            helper.setText(buildActivationEmailBody(activationUrl), true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send activation email", e);
        }
    }

    private String buildActivationEmailBody(String activationUrl) {
        return "<html><body>" +
                "<h2>Welcome! Please activate your account</h2>" +
                "<p>Click the link below to activate your account:</p>" +
                "<a href=\"" + activationUrl + "\">Activate Account</a>" +
                "<p>Or copy and paste this URL into your browser:</p>" +
                "<p>" + activationUrl + "</p>" +
                "<p>This link will expire in 24 hours.</p>" +
                "</body></html>";
    }
}
