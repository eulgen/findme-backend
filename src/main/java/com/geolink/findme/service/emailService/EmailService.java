package com.geolink.findme.service.emailService;

import com.geolink.findme.entity.OtpPurpose;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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

    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    @Async
    public void sendOtpEmail(String toEmail, String fullName, String otpCode, int ttlMinutes, OtpPurpose purpose) {
        Context context = new Context();
        context.setVariable("fullName", fullName != null ? fullName : toEmail);
        context.setVariable("otpCode", otpCode);
        context.setVariable("ttlMinutes", ttlMinutes);

        String encodedEmail = URLEncoder.encode(toEmail, StandardCharsets.UTF_8);
        String actionUrl;
        String actionText;
        String introText;
        String subject;

        if (purpose == OtpPurpose.ACCOUNT_VERIFICATION) {
            introText = "Voici votre code pour vérifier votre compte findMe :";
            subject = "Vérification de votre compte findMe";
            actionText = "Vérifier mon compte";
            actionUrl = frontendBaseUrl + "/auth/verify-account?email=" + encodedEmail + "&code=" + otpCode;
        } else {
            introText = "Voici votre code pour réinitialiser votre mot de passe :";
            subject = "Réinitialisation de votre mot de passe findMe";
            actionText = "Réinitialiser mon mot de passe";
            actionUrl = frontendBaseUrl + "/auth/reset-password?email=" + encodedEmail;
        }

        context.setVariable("introText", introText);
        context.setVariable("actionText", actionText);
        context.setVariable("actionUrl", actionUrl);

        String htmlBody = templateEngine.process("email/otp-code", context);
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
