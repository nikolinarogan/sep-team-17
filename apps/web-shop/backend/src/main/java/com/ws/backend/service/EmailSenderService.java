package com.ws.backend.service;

import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

@Service
public class EmailSenderService {

    private final JavaMailSender mailSender;

    public EmailSenderService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends an HTML email with a clickable activation link.
     *
     * @param to Recipient email
     * @param activationLink The full activation URL
     * @param linkText Text to display instead of showing the full URL
     */
    public void sendActivationEmail(String to, String activationLink, String linkText) {
        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setSubject("Activate your PKI account");

            // HTML email content
            String htmlMsg = "<p>Welcome to PKI System!</p>"
                    + "<p>Click the link below to activate your account:</p>"
                    + "<a href=\"" + activationLink + "\">" + linkText + "</a>"
                    + "<p>This link will expire in 24 hours.</p>";

            helper.setText(htmlMsg, true); // true = HTML content

            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to send email to " + to, e);
        }
    }
}
