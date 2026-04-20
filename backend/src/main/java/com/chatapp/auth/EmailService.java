package com.chatapp.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String from;
    private final String frontendUrl;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.mail.from}") String from,
                        @Value("${app.frontend.url}") String frontendUrl) {
        this.mailSender = mailSender;
        this.from = from;
        this.frontendUrl = frontendUrl;
    }

    public void sendPasswordResetEmail(String toEmail, String token) {
        String link = frontendUrl + "/reset-password?token=" + token;
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(toEmail);
        msg.setSubject("Password Reset Request");
        msg.setText("Click the link below to reset your password (expires in 1 hour):\n\n" + link
                + "\n\nIf you did not request a password reset, ignore this email.");
        mailSender.send(msg);
    }
}
