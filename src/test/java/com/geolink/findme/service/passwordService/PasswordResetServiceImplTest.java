package com.geolink.findme.service.passwordService;

import com.geolink.findme.entity.OtpPurpose;
import com.geolink.findme.entity.User;
import com.geolink.findme.repository.UserRepository;
import com.geolink.findme.service.emailService.EmailService;
import com.geolink.findme.service.otpService.OtpService;

import org.junit.jupiter.api.BeforeEach;
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
class PasswordResetServiceImplTest {

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
    void devrait_generer_un_otp_et_envoyer_un_mail_si_l_utilisateur_existe() {
        when(userRepository.findByEmail("test@geolink.com")).thenReturn(Optional.of(testUser));
        when(otpService.generate(testUser, OtpPurpose.PASSWORD_RESET)).thenReturn("654321");

        passwordResetService.requestReset("test@geolink.com");

        verify(otpService).generate(testUser, OtpPurpose.PASSWORD_RESET);
        verify(emailService).sendOtpEmail(eq("test@geolink.com"), eq("Jean Dupont"), eq("654321"), eq(10), eq(OtpPurpose.PASSWORD_RESET));
    }

    @Test
    void devrait_ne_rien_faire_si_l_utilisateur_n_existe_pas_lors_de_la_demande() {
        when(userRepository.findByEmail("unknown@geolink.com")).thenReturn(Optional.empty());

        passwordResetService.requestReset("unknown@geolink.com");

        verify(userRepository, times(1)).findByEmail("unknown@geolink.com");
        verify(otpService, never()).generate(any(), any());
        verify(emailService, never()).sendOtpEmail(any(), any(), any(), anyInt(), any());
    }

    @Test
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
