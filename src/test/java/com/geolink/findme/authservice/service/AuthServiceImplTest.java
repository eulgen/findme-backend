package com.geolink.findme.authservice.service;

import com.geolink.findme.authservice.dto.mapper.UserMapper;
import com.geolink.findme.authservice.dto.request.RefreshRequestDTO;
import com.geolink.findme.authservice.dto.request.SignInRequestDTO;
import com.geolink.findme.authservice.dto.request.SignUpRequestDTO;
import com.geolink.findme.authservice.dto.response.AuthResponseDTO;
import com.geolink.findme.authservice.dto.response.UserProfileDTO;
import com.geolink.findme.authservice.entity.AccountStatus;
import com.geolink.findme.authservice.entity.OtpPurpose;
import com.geolink.findme.authservice.entity.RefreshToken;
import com.geolink.findme.authservice.entity.Role;
import com.geolink.findme.authservice.entity.User;
import com.geolink.findme.authservice.exception.EmailAlreadyUsedException;
import com.geolink.findme.authservice.exception.InvalidCredentialsException;
import com.geolink.findme.authservice.repository.RoleRepository;
import com.geolink.findme.authservice.repository.UserRepository;
import com.geolink.findme.authservice.security.JwtService;
import com.geolink.findme.authservice.security.UserPrincipal;
import com.geolink.findme.authservice.service.auth.AuthServiceImpl;
import com.geolink.findme.authservice.service.passwordService.RefreshTokenService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private JwtService jwtService;

    @Spy
    private UserMapper userMapper = new UserMapper();

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OtpService otpService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private Role userRole;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "otpTtlMinutes", 10);
        userRole = Role.builder().id(1L).name("USER").build();
        testUser = User.builder()
                .id(1L)
                .email("test@geolink.com")
                .passwordHash("encoded_pwd")
                .firstName("Jean")
                .lastName("Dupont")
                .status(AccountStatus.ACTIVE)
                .roles(Set.of(userRole))
                .build();
        testUser.setAccountVerified(true);
    }

    @Test
    void devrait_inscrire_un_nouvel_utilisateur_et_envoyer_otp() {
        SignUpRequestDTO request = SignUpRequestDTO.builder()
                .email("test@geolink.com")
                .password("Pass1234")
                .firstName("Jean")
                .lastName("Dupont")
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded_pwd");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(otpService.generate(any(User.class), eq(OtpPurpose.ACCOUNT_VERIFICATION))).thenReturn("123456");

        UserProfileDTO result = authService.signUp(request);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@geolink.com");
        verify(emailService).sendOtpEmail(eq("test@geolink.com"), eq("Jean Dupont"), eq("123456"), eq(10), eq(OtpPurpose.ACCOUNT_VERIFICATION));
    }

    @Test
    void devrait_lever_une_exception_si_l_email_est_deja_utilise_lors_de_l_inscription() {
        SignUpRequestDTO request = SignUpRequestDTO.builder()
                .email("test@geolink.com")
                .password("Pass1234")
                .firstName("Jean")
                .lastName("Dupont")
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.signUp(request))
                .isInstanceOf(EmailAlreadyUsedException.class)
                .hasMessageContaining("test@geolink.com");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void devrait_connecter_un_utilisateur_verifie_et_retourner_les_tokens() {
        SignInRequestDTO request = SignInRequestDTO.builder()
                .email("test@geolink.com")
                .password("Pass1234")
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Pass1234", "encoded_pwd")).thenReturn(true);
        when(jwtService.generateAccessToken(any(UserPrincipal.class))).thenReturn("access_token");
        when(refreshTokenService.createRefreshToken(testUser)).thenReturn("refresh_token");
        when(jwtService.getAccessExpirationInSeconds()).thenReturn(900L);

        AuthResponseDTO response = authService.signIn(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access_token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh_token");
    }

    @Test
    void devrait_refuser_connexion_si_compte_non_verifie() {
        testUser.setAccountVerified(false);
        SignInRequestDTO request = SignInRequestDTO.builder()
                .email("test@geolink.com")
                .password("Pass1234")
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Pass1234", "encoded_pwd")).thenReturn(true);

        assertThatThrownBy(() -> authService.signIn(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Veuillez vérifier votre compte");
    }

    @Test
    void devrait_revoquer_le_token_spécifique_lors_du_logout_with_token() {
        authService.logoutWithToken("raw_refresh_token");

        verify(refreshTokenService).revokeToken("raw_refresh_token");
    }
}
