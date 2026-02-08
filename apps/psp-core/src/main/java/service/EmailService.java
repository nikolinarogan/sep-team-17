package service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendMfaEmail(String to, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress != null && !fromAddress.isBlank() ? fromAddress : "noreply@psp.local");
            helper.setTo(to);
            helper.setSubject("Your PSP Verification Code");
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
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 20px; }
                    .container { max-width: 480px; margin: 0 auto; }
                    .card { background: #ffffff; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.08); padding: 32px; }
                    .logo { font-size: 20px; font-weight: 700; color: #1a1a2e; margin-bottom: 24px; }
                    h1 { font-size: 18px; font-weight: 600; color: #1a1a2e; margin: 0 0 16px 0; }
                    p { margin: 0 0 16px 0; color: #555; }
                    .code { font-size: 28px; font-weight: 700; letter-spacing: 6px; color: #1a1a2e; background: #f4f4f5; padding: 16px 24px; border-radius: 6px; text-align: center; margin: 24px 0; }
                    .footer { font-size: 12px; color: #888; margin-top: 24px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="card">
                        <div class="logo">PSP Admin Portal</div>
                        <h1>Your Verification Code</h1>
                        <p>Use the following code to complete your sign-in. This code expires in 5 minutes.</p>
                        <div class="code">%s</div>
                        <p>If you did not request this code, please ignore this email and ensure your account is secure.</p>
                        <div class="footer">This is an automated message. Please do not reply.</div>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(code);
    }
}
