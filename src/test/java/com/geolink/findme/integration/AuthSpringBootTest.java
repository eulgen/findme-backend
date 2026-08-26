package com.geolink.findme.integration;

import com.geolink.findme.dto.request.RefreshRequestDTO;
import com.geolink.findme.dto.request.SignInRequestDTO;
import com.geolink.findme.dto.request.SignUpRequestDTO;
import com.geolink.findme.dto.response.AuthResponseDTO;
import com.geolink.findme.dto.response.UserProfileDTO;
import com.geolink.findme.entity.Role;
import com.geolink.findme.entity.User;
import com.geolink.findme.repository.RoleRepository;
import com.geolink.findme.repository.UserRepository;
import com.geolink.findme.service.emailService.EmailService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:authdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS authservice",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "securite.jwt.secret=dGhpc19pc19hX3Zlcnlfc2VjdXJlX2xvbmdfc2VjcmV0X2tleV9mb3Jfand0X3NpZ25pbmdfYW5kX3ZlcmlmaWNhdGlvbl8xMjM0NTY3ODkw",
        "securite.jwt.duree-acces-minutes=15",
        "securite.jwt.duree-rafraichissement-jours=7",
        "DB_URL=jdbc:h2:mem:authdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS authservice",
        "DB_USER=sa",
        "DB_PASSWORD=",
        "spring.security.oauth2.client.registration.google.client-id=mock-client-id",
        "spring.security.oauth2.client.registration.google.client-secret=mock-client-secret",
        "spring.security.oauth2.client.registration.apple.client-id=com.geolink.findme.client",
        "spring.security.oauth2.client.registration.apple.client-secret=mock-client-secret"
})
class AuthSpringBootTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @MockitoBean
    private EmailService emailService;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        if (roleRepository.findByName("USER").isEmpty()) {
            roleRepository.save(Role.builder().name("USER").description("Utilisateur standard").build());
            roleRepository.save(Role.builder().name("ADMIN").description("Administrateur").build());
            roleRepository.save(Role.builder().name("SUPPORT_AGENT").description("Agent support").build());
        }
    }

    @Test
    void fullAuthFlow_Success() {
        // 1. Sign Up
        SignUpRequestDTO signUpReq = SignUpRequestDTO.builder()
                .email("flow.user@geolink.com")
                .password("SecurePass123")
                .fullName("Flow User")
                .build();

        ResponseEntity<UserProfileDTO> signUpResp = restTemplate.postForEntity(
                baseUrl + "/api/auth/signup",
                signUpReq,
                UserProfileDTO.class
        );

        assertThat(signUpResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(signUpResp.getBody()).isNotNull();
        assertThat(signUpResp.getBody().getEmail()).isEqualTo("flow.user@geolink.com");

        // Activer manuellement le compte pour simuler la validation OTP
        User savedUser = userRepository.findByEmail("flow.user@geolink.com").orElseThrow();
        savedUser.setAccountVerified(true);
        userRepository.saveAndFlush(savedUser);

        // 2. Sign In
        SignInRequestDTO signInReq = SignInRequestDTO.builder()
                .email("flow.user@geolink.com")
                .password("SecurePass123")
                .build();

        ResponseEntity<AuthResponseDTO> signInResp = restTemplate.postForEntity(
                baseUrl + "/api/auth/signin",
                signInReq,
                AuthResponseDTO.class
        );

        assertThat(signInResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(signInResp.getBody()).isNotNull();
        String accessToken = signInResp.getBody().getAccessToken();
        String refreshToken = signInResp.getBody().getRefreshToken();
        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();

        // 3. GET /api/users/me (Authentifié avec le token JWT Bearer)
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> meEntity = new HttpEntity<>(headers);

        ResponseEntity<UserProfileDTO> meResp = restTemplate.exchange(
                baseUrl + "/api/users/me",
                HttpMethod.GET,
                meEntity,
                UserProfileDTO.class
        );

        assertThat(meResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(meResp.getBody()).isNotNull();
        assertThat(meResp.getBody().getFullName()).isEqualTo("Flow User");

        // 4. Refresh Token (Rotation)
        RefreshRequestDTO refreshReq = RefreshRequestDTO.builder()
                .refreshToken(refreshToken)
                .build();

        ResponseEntity<AuthResponseDTO> refreshResp = restTemplate.postForEntity(
                baseUrl + "/api/auth/refresh",
                refreshReq,
                AuthResponseDTO.class
        );

        assertThat(refreshResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshResp.getBody()).isNotNull();
        assertThat(refreshResp.getBody().getAccessToken()).isNotBlank();
        assertThat(refreshResp.getBody().getRefreshToken()).isNotEqualTo(refreshToken);

        // 5. Logout
        RefreshRequestDTO logoutReq = RefreshRequestDTO.builder()
                .refreshToken(refreshResp.getBody().getRefreshToken())
                .build();

        ResponseEntity<Void> logoutResp = restTemplate.postForEntity(
                baseUrl + "/api/auth/logout",
                logoutReq,
                Void.class
        );

        assertThat(logoutResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
}
