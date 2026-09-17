package com.geolink.findme.unit.conventionnel;

import com.geolink.findme.entity.OtpPurpose;
import com.geolink.findme.service.emailService.EmailService;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)

class EmailServiceTest {


    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding("UTF-8");
        templateEngine.setTemplateResolver(templateResolver);

        emailService = new EmailService(mailSender, templateEngine);
        ReflectionTestUtils.setField(emailService, "fromEmail", "findMe <no-reply@findme.geolink.com>");
        ReflectionTestUtils.setField(emailService, "frontendBaseUrl", "http://localhost:3000");

        lenient().when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    @DisplayName("Devrait envoyer un email de vérification avec lien action de confirmation de compte")
    void devrait_envoyer_email_verification_avec_lien_action() {
        // Act
        emailService.sendOtpEmail("test@geolink.com", "Jean Dupont", "123456", 10, OtpPurpose.ACCOUNT_VERIFICATION);

        // Assert
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Devrait envoyer un email de réinitialisation de mot de passe avec lien action")
    void devrait_envoyer_email_reinitialisation_avec_lien_action() {
        // Act
        emailService.sendOtpEmail("user@geolink.com", "Alice", "654321", 10, OtpPurpose.PASSWORD_RESET);

        // Assert
        verify(mailSender).send(any(MimeMessage.class));
    }
}
