package com.ws.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailSenderService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    public EmailSenderService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendMfaEmail(String to, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress != null && !fromAddress.isBlank() ? fromAddress : "noreply@webshop.local");
            helper.setTo(to);
            helper.setSubject("Your Web Shop Verification Code");
            helper.setText(buildMfaEmailBody(code), true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send MFA email: " + e.getMessage(), e);
        }
    }

    private String buildMfaEmailBody(String code) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"><style>
            body{font-family:system-ui,sans-serif;line-height:1.6;color:#333;margin:0;padding:20px}
            .container{max-width:480px;margin:0 auto}
            .card{background:#fff;border-radius:8px;box-shadow:0 2px 8px rgba(0,0,0,0.08);padding:32px}
            .code{font-size:28px;font-weight:700;letter-spacing:6px;color:#1a1a2e;background:#f4f4f5;padding:16px 24px;border-radius:6px;text-align:center;margin:24px 0}
            </style></head>
            <body><div class="container"><div class="card">
            <h2>Your Verification Code</h2>
            <p>Use the following code to complete your sign-in. This code expires in 5 minutes.</p>
            <div class="code">%s</div>
            <p>If you did not request this code, please ignore this email.</p>
            </div></div></body></html>
            """.formatted(code);
    }

    public void sendActivationEmail(String to, String activationLink, String linkText) {
        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setSubject("Activate your account");

            String htmlMsg = "<p>Welcome to RentCar Web Shop!</p>"
                    + "<p>Click the link below to activate your account:</p>"
                    + "<a href=\"" + activationLink + "\">" + linkText + "</a>"
                    + "<p>This link will expire in 24 hours.</p>";

            helper.setText(htmlMsg, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to send email to " + to, e);
        }
    }
}
