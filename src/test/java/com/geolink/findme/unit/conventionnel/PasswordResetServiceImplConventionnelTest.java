package com.geolink.findme.unit.conventionnel;

import com.geolink.findme.entity.OtpPurpose;
import com.geolink.findme.entity.User;
import com.geolink.findme.repository.UserRepository;
import com.geolink.findme.service.emailService.EmailService;
import com.geolink.findme.service.otpService.OtpService;
import com.geolink.findme.service.passwordService.PasswordResetServiceImpl;
import com.geolink.findme.service.passwordService.RefreshTokenService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires PasswordResetServiceImpl (Cas Conventionnels)")
class PasswordResetServiceImplConventionnelTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OtpService otpService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private PasswordResetServiceImpl passwordResetService;

    private User testUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(passwordResetService, "otpTtlMinutes", 10);
        testUser = User.builder()
                .id(1L)
                .email("test@geolink.com")
                .fullName("Jean Dupont")
                .passwordHash("old_pwd")
                .build();
    }

    @Test
    @DisplayName("Devrait générer un OTP et envoyer un email en cas de demande pour un utilisateur existant")
    void devrait_generer_un_otp_et_envoyer_un_mail_si_l_utilisateur_existe() {
        when(userRepository.findByEmail("test@geolink.com")).thenReturn(Optional.of(testUser));
        when(otpService.generate(testUser, OtpPurpose.PASSWORD_RESET)).thenReturn("654321");

        passwordResetService.requestReset("test@geolink.com");

        verify(otpService).generate(testUser, OtpPurpose.PASSWORD_RESET);
        verify(emailService).sendOtpEmail(eq("test@geolink.com"), eq("Jean Dupont"), eq("654321"), eq(10), eq(OtpPurpose.PASSWORD_RESET));
    }

    @Test
    @DisplayName("Devrait réinitialiser le mot de passe et révoquer les jetons avec succès")
    void devrait_reinitialiser_le_mot_de_passe_et_revoquer_les_tokens() {
        when(userRepository.findByEmail("test@geolink.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("NewPass123")).thenReturn("new_hash");

        passwordResetService.resetPassword("test@geolink.com", "654321", "NewPass123");

        verify(otpService).verify(testUser, OtpPurpose.PASSWORD_RESET, "654321");
        assertThat(testUser.getPasswordHash()).isEqualTo("new_hash");
        verify(userRepository).save(testUser);
        verify(refreshTokenService).revokeAllForUser(testUser);
    }
}
