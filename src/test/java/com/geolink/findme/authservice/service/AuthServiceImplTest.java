package com.geolink.findme.authservice.service;

import com.geolink.findme.authservice.dto.mapper.UserMapper;
import com.geolink.findme.authservice.dto.request.RefreshRequestDTO;
import com.geolink.findme.authservice.dto.request.SignInRequestDTO;
import com.geolink.findme.authservice.dto.request.SignUpRequestDTO;
import com.geolink.findme.authservice.dto.response.AuthResponseDTO;
import com.geolink.findme.authservice.dto.response.UserProfileDTO;
import com.geolink.findme.authservice.entity.AccountStatus;
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

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private Role userRole;

    @BeforeEach
    void setUp() {
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
    }

    @Test
    void devrait_inscrire_un_nouvel_utilisateur_avec_succes() {
        // Given
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

        // When
        UserProfileDTO result = authService.signUp(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@geolink.com");
        verify(userRepository, times(1)).existsByEmail("test@geolink.com");
        verify(roleRepository, times(1)).findByName("USER");
        verify(passwordEncoder, times(1)).encode("Pass1234");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void devrait_lever_une_exception_si_l_email_est_deja_utilise_lors_de_l_inscription() {
        // Given
        SignUpRequestDTO request = SignUpRequestDTO.builder()
                .email("test@geolink.com")
                .password("Pass1234")
                .firstName("Jean")
                .lastName("Dupont")
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.signUp(request))
                .isInstanceOf(EmailAlreadyUsedException.class)
                .hasMessageContaining("test@geolink.com");

        verify(userRepository, times(1)).existsByEmail("test@geolink.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void devrait_connecter_un_utilisateur_et_retourner_les_tokens() {
        // Given
        SignInRequestDTO request = SignInRequestDTO.builder()
                .email("test@geolink.com")
                .password("Pass1234")
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Pass1234", "encoded_pwd")).thenReturn(true);
        when(jwtService.generateAccessToken(any(UserPrincipal.class))).thenReturn("access_token");
        when(refreshTokenService.createRefreshToken(testUser)).thenReturn("refresh_token");
        when(jwtService.getAccessExpirationInSeconds()).thenReturn(900L);

        // When
        AuthResponseDTO response = authService.signIn(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access_token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh_token");
        assertThat(response.getExpiresIn()).isEqualTo(900L);
        verify(userRepository, times(1)).findByEmail("test@geolink.com");
        verify(passwordEncoder, times(1)).matches("Pass1234", "encoded_pwd");
        verify(jwtService, times(1)).generateAccessToken(any(UserPrincipal.class));
        verify(refreshTokenService, times(1)).createRefreshToken(testUser);
    }

    @Test
    void devrait_lever_une_exception_si_le_mot_de_passe_est_incorrect() {
        // Given
        SignInRequestDTO request = SignInRequestDTO.builder()
                .email("test@geolink.com")
                .password("WrongPwd")
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("WrongPwd", "encoded_pwd")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.signIn(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Email ou mot de passe incorrect");

        verify(userRepository, times(1)).findByEmail("test@geolink.com");
        verify(passwordEncoder, times(1)).matches("WrongPwd", "encoded_pwd");
        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    void devrait_rafraichir_les_tokens_avec_succes() {
        // Given
        RefreshRequestDTO request = RefreshRequestDTO.builder()
                .refreshToken("old_refresh")
                .build();

        RefreshToken tokenEntity = RefreshToken.builder()
                .user(testUser)
                .build();

        when(refreshTokenService.verifyAndRotate("old_refresh")).thenReturn(tokenEntity);
        when(refreshTokenService.createRefreshToken(testUser)).thenReturn("new_refresh");
        when(jwtService.generateAccessToken(any(UserPrincipal.class))).thenReturn("new_access");
        when(jwtService.getAccessExpirationInSeconds()).thenReturn(900L);

        // When
        AuthResponseDTO response = authService.refresh(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("new_access");
        assertThat(response.getRefreshToken()).isEqualTo("new_refresh");
        verify(refreshTokenService, times(1)).verifyAndRotate("old_refresh");
        verify(refreshTokenService, times(1)).createRefreshToken(testUser);
        verify(jwtService, times(1)).generateAccessToken(any(UserPrincipal.class));
    }

    @Test
    void devrait_revoquer_tous_les_tokens_lors_de_la_deconnexion() {
        // Given
        when(userRepository.findByEmail("test@geolink.com")).thenReturn(Optional.of(testUser));

        // When
        authService.logout("test@geolink.com");

        // Then
        verify(userRepository, times(1)).findByEmail("test@geolink.com");
        verify(refreshTokenService, times(1)).revokeAllForUser(testUser);
    }
}
