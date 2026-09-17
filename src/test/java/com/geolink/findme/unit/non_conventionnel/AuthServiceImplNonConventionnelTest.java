package com.geolink.findme.unit.non_conventionnel;

import com.geolink.findme.dto.mapper.UserMapper;
import com.geolink.findme.dto.request.SignInRequestDTO;
import com.geolink.findme.dto.request.SignUpRequestDTO;
import com.geolink.findme.dto.request.RefreshRequestDTO;
import com.geolink.findme.entity.AccountStatus;
import com.geolink.findme.entity.OtpPurpose;
import com.geolink.findme.entity.Role;
import com.geolink.findme.entity.User;
import com.geolink.findme.exception.AddressNotFoundException;
import com.geolink.findme.exception.EmailAlreadyUsedException;
import com.geolink.findme.exception.InvalidCredentialsException;
import com.geolink.findme.exception.InvalidOrExpiredTokenException;
import com.geolink.findme.repository.AddressRepository;
import com.geolink.findme.repository.RoleRepository;
import com.geolink.findme.repository.UserRepository;
import com.geolink.findme.security.JwtService;
import com.geolink.findme.service.auth.AuthServiceImpl;
import com.geolink.findme.service.emailService.EmailService;
import com.geolink.findme.service.otpService.OtpService;
import com.geolink.findme.service.passwordService.RefreshTokenService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires AuthServiceImpl (Cas Non Conventionnels)")
class AuthServiceImplNonConventionnelTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private AddressRepository addressRepository;

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
                .fullName("Jean Dupont")
                .status(AccountStatus.ACTIVE)
                .roles(Set.of(userRole))
                .build();
        testUser.setAccountVerified(true);
    }

    @Test
    @DisplayName("Devrait lever EmailAlreadyUsedException si l'email existe déjà lors de l'inscription")
    void devrait_lever_une_exception_si_l_email_est_deja_utilise_lors_de_l_inscription() {
        SignUpRequestDTO request = SignUpRequestDTO.builder()
                .email("test@geolink.com")
                .password("Pass1234")
                .fullName("Jean Dupont")
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.signUp(request))
                .isInstanceOf(EmailAlreadyUsedException.class)
                .hasMessageContaining("test@geolink.com");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Devrait lever AddressNotFoundException si le code d'adresse fourni est introuvable lors de l'inscription")
    void devrait_lever_exception_si_code_adresse_inexistant_lors_de_l_inscription() {
        SignUpRequestDTO request = SignUpRequestDTO.builder()
                .email("new@geolink.com")
                .password("Pass1234")
                .fullName("New User")
                .addressCode("ADR-INVALID")
                .build();

        when(userRepository.existsByEmail("new@geolink.com")).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("Pass1234")).thenReturn("encoded_pwd");
        when(addressRepository.findByAddressCodeIgnoreCase("ADR-INVALID")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.signUp(request))
                .isInstanceOf(AddressNotFoundException.class)
                .hasMessageContaining("ADR-INVALID");
    }

    @Test
    @DisplayName("Devrait refuser la connexion si le compte n'est pas encore vérifié")
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
    @DisplayName("Devrait refuser la connexion si le mot de passe est incorrect")
    void devrait_refuser_connexion_si_mot_de_passe_incorrect() {
        SignInRequestDTO request = SignInRequestDTO.builder()
                .email("test@geolink.com")
                .password("WrongPass")
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("WrongPass", "encoded_pwd")).thenReturn(false);

        assertThatThrownBy(() -> authService.signIn(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Email ou mot de passe incorrect");
    }

    @Test
    @DisplayName("Devrait refuser la connexion si l'email n'existe pas")
    void devrait_refuser_connexion_si_email_inexistant() {
        SignInRequestDTO request = SignInRequestDTO.builder()
                .email("unknown@geolink.com")
                .password("Pass1234")
                .build();

        when(userRepository.findByEmail("unknown@geolink.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.signIn(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Email ou mot de passe incorrect");
    }

    @Test
    @DisplayName("Devrait refuser la connexion si le compte est inactif/suspendu")
    void devrait_refuser_connexion_si_compte_inactif() {
        testUser.setStatus(AccountStatus.INACTIVE);
        testUser.setAccountVerified(true);
        SignInRequestDTO request = SignInRequestDTO.builder()
                .email("test@geolink.com")
                .password("Pass1234")
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Pass1234", "encoded_pwd")).thenReturn(true);

        assertThatThrownBy(() -> authService.signIn(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Compte désactivé ou inactif");
    }

    @Test
    @DisplayName("Devrait lever InvalidOrExpiredTokenException si l'utilisateur est inconnu lors de la vérification OTP")
    void devrait_lever_exception_si_verification_otp_pour_utilisateur_inconnu() {
        when(userRepository.findByEmail("unknown@geolink.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verifyAccount("unknown@geolink.com", "123456"))
                .isInstanceOf(InvalidOrExpiredTokenException.class)
                .hasMessageContaining("Utilisateur introuvable");
    }

    @Test
    @DisplayName("Devrait lever InvalidOrExpiredTokenException si l'utilisateur tente de renvoyer un OTP sur un compte déjà vérifié")
    void devrait_lever_exception_si_renvoi_otp_sur_compte_deja_verifie() {
        testUser.setAccountVerified(true);
        when(userRepository.findByEmail("test@geolink.com")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authService.resendVerificationOtp("test@geolink.com"))
                .isInstanceOf(InvalidOrExpiredTokenException.class)
                .hasMessageContaining("Le compte est déjà vérifié");
    }

    @Test
    @DisplayName("Devrait lever IllegalStateException si le rôle USER est absent de la base lors de l'inscription")
    void devrait_lever_exception_si_role_user_absent_en_base() {
        SignUpRequestDTO request = SignUpRequestDTO.builder()
                .email("new@geolink.com")
                .password("Pass1234")
                .fullName("New User")
                .build();

        when(userRepository.existsByEmail("new@geolink.com")).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.signUp(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Le rôle USER de référence est introuvable en base");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Devrait lever InvalidOrExpiredTokenException si le code OTP est invalide lors de la vérification du compte")
    void devrait_lever_exception_si_code_otp_invalide_lors_de_la_verification_compte() {
        when(userRepository.findByEmail("test@geolink.com")).thenReturn(Optional.of(testUser));
        doThrow(new InvalidOrExpiredTokenException("Code OTP invalide"))
                .when(otpService).verify(testUser, OtpPurpose.ACCOUNT_VERIFICATION, "BADCODE");

        assertThatThrownBy(() -> authService.verifyAccount("test@geolink.com", "BADCODE"))
                .isInstanceOf(InvalidOrExpiredTokenException.class)
                .hasMessageContaining("Code OTP invalide");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Devrait propagager InvalidOrExpiredTokenException si le refresh token est invalide lors du refresh")
    void devrait_propager_exception_si_refresh_token_invalide() {
        RefreshRequestDTO request = RefreshRequestDTO.builder()
                .refreshToken("invalid_token")
                .build();

        when(refreshTokenService.verifyAndRotate("invalid_token"))
                .thenThrow(new InvalidOrExpiredTokenException("Token invalide"));

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(InvalidOrExpiredTokenException.class)
                .hasMessageContaining("Token invalide");
    }
}
