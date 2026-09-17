package com.geolink.findme.unit.non_conventionnel;

import com.geolink.findme.entity.OtpPurpose;
import com.geolink.findme.entity.User;
import com.geolink.findme.exception.InvalidOrExpiredTokenException;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires PasswordResetServiceImpl (Cas Non Conventionnels)")
class PasswordResetServiceImplNonConventionnelTest {

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

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(passwordResetService, "otpTtlMinutes", 10);
    }

    @Test
    @DisplayName("Devrait ignorer la demande sans envoyer de mail si l'utilisateur n'existe pas")
    void devrait_ne_rien_faire_si_l_utilisateur_n_existe_pas_lors_de_la_demande() {
        when(userRepository.findByEmail("unknown@geolink.com")).thenReturn(Optional.empty());

        passwordResetService.requestReset("unknown@geolink.com");

        verify(userRepository, times(1)).findByEmail("unknown@geolink.com");
        verify(otpService, never()).generate(any(), any());
        verify(emailService, never()).sendOtpEmail(any(), any(), any(), anyInt(), any());
    }

    @Test
    @DisplayName("Devrait lever InvalidOrExpiredTokenException si l'email est inconnu lors de la réinitialisation")
    void devrait_lever_exception_si_email_inconnu_lors_de_la_reinitialisation() {
        when(userRepository.findByEmail("ghost@geolink.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.resetPassword("ghost@geolink.com", "123456", "NewPass123!"))
                .isInstanceOf(InvalidOrExpiredTokenException.class)
                .hasMessageContaining("invalide ou inexistant");

        verify(otpService, never()).verify(any(), any(), any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Devrait lever InvalidOrExpiredTokenException si le code OTP de réinitialisation est invalide")
    void devrait_lever_exception_si_code_otp_reinitialisation_invalide() {
        User user = User.builder().id(1L).email("user@geolink.com").build();
        when(userRepository.findByEmail("user@geolink.com")).thenReturn(Optional.of(user));
        doThrow(new InvalidOrExpiredTokenException("Code OTP invalide ou expiré"))
                .when(otpService).verify(user, com.geolink.findme.entity.OtpPurpose.PASSWORD_RESET, "WRONG");

        assertThatThrownBy(() -> passwordResetService.resetPassword("user@geolink.com", "WRONG", "NewPass123!"))
                .isInstanceOf(InvalidOrExpiredTokenException.class);

        verify(userRepository, never()).save(any());
        verify(refreshTokenService, never()).revokeAllForUser(any());
    }
}
