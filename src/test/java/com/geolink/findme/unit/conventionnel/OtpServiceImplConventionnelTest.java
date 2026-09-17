package com.geolink.findme.unit.conventionnel;

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

class OtpServiceImplConventionnelTest {


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
    void devrait_generer_un_code_otp_a_6_chiffres() {
        when(passwordEncoder.encode(any())).thenReturn("hashed_code");

        String code = otpService.generate(testUser, OtpPurpose.ACCOUNT_VERIFICATION);

        assertThat(code).hasSize(6);
        verify(otpCodeRepository).consumeAllActiveForUserAndPurpose(1L, OtpPurpose.ACCOUNT_VERIFICATION);
        verify(otpCodeRepository).save(any(OtpCode.class));
    }
}
