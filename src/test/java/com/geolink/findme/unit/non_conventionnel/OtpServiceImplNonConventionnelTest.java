package com.geolink.findme.unit.non_conventionnel;

import com.geolink.findme.entity.OtpCode;
import com.geolink.findme.entity.OtpPurpose;
import com.geolink.findme.entity.User;
import com.geolink.findme.exception.InvalidOrExpiredTokenException;
import com.geolink.findme.repository.OtpCodeRepository;
import com.geolink.findme.service.otpService.OtpServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)

class OtpServiceImplNonConventionnelTest {


    @Mock
    private OtpCodeRepository otpCodeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private OtpServiceImpl otpService;

    private User testUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(otpService, "ttlMinutes", 10);
        testUser = User.builder()
                .id(1L)
                .email("test@geolink.com")
                .fullName("Jean Dupont")
                .build();
    }

    @Test
    void devrait_verifier_un_code_otp_valide() {
        OtpCode otpCode = new OtpCode();
        otpCode.setExpiresAt(Instant.now().plus(5, ChronoUnit.MINUTES));
        otpCode.setCodeHash("hashed_code");
        otpCode.setAttemptCount(0);
        otpCode.setConsumed(false);

        when(otpCodeRepository.findFirstByUserIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(1L, OtpPurpose.ACCOUNT_VERIFICATION))
                .thenReturn(Optional.of(otpCode));
        when(passwordEncoder.matches("123456", "hashed_code")).thenReturn(true);

        otpService.verify(testUser, OtpPurpose.ACCOUNT_VERIFICATION, "123456");

        assertThat(otpCode.isConsumed()).isTrue();
        verify(otpCodeRepository).save(otpCode);
    }

    @Test
    void devrait_lever_une_exception_si_code_invalide_ou_expire() {
        when(otpCodeRepository.findFirstByUserIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(1L, OtpPurpose.ACCOUNT_VERIFICATION))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> otpService.verify(testUser, OtpPurpose.ACCOUNT_VERIFICATION, "123456"))
                .isInstanceOf(InvalidOrExpiredTokenException.class)
                .hasMessageContaining("Aucun code OTP actif");
    }

    @Test
    void shouldBlockAfterFiveFailedAttempts() {
        OtpCode otpCode = new OtpCode();
        otpCode.setExpiresAt(Instant.now().plus(5, ChronoUnit.MINUTES));
        otpCode.setAttemptCount(5);

        when(otpCodeRepository.findFirstByUserIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(1L, OtpPurpose.ACCOUNT_VERIFICATION))
                .thenReturn(Optional.of(otpCode));

        assertThatThrownBy(() -> otpService.verify(testUser, OtpPurpose.ACCOUNT_VERIFICATION, "123456"))
                .isInstanceOf(InvalidOrExpiredTokenException.class)
                .hasMessageContaining("Nombre maximal de tentatives atteint");
    }
}
