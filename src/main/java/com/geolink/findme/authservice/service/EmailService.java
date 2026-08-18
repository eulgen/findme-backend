package com.geolink.findme.authservice.service;

import com.geolink.findme.authservice.entity.OtpPurpose;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.from:findMe <no-reply@findme.geolink.com>}")
    private String fromEmail;

    @Async
    public void sendOtpEmail(String toEmail, String fullName, String otpCode, int ttlMinutes, OtpPurpose purpose) {
        Context context = new Context();
        context.setVariable("fullName", fullName != null ? fullName : toEmail);
        context.setVariable("otpCode", otpCode);
        context.setVariable("ttlMinutes", ttlMinutes);
        context.setVariable("introText", purpose == OtpPurpose.ACCOUNT_VERIFICATION
                ? "Voici votre code pour vérifier votre compte findMe :"
                : "Voici votre code pour réinitialiser votre mot de passe :");

        String htmlBody = templateEngine.process("email/otp-code", context);
        String subject = purpose == OtpPurpose.ACCOUNT_VERIFICATION
                ? "Vérification de votre compte findMe"
                : "Réinitialisation de votre mot de passe findMe";

        send(toEmail, subject, htmlBody);
    }

    private void send(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Échec d'envoi d'email à {}", to, e);
            throw new RuntimeException("Impossible d'envoyer l'email", e);
        }
    }
}
