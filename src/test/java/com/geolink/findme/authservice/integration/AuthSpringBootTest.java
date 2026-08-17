package com.geolink.findme.authservice.integration;

import com.geolink.findme.authservice.dto.request.RefreshRequestDTO;
import com.geolink.findme.authservice.dto.request.SignInRequestDTO;
import com.geolink.findme.authservice.dto.request.SignUpRequestDTO;
import com.geolink.findme.authservice.dto.response.AuthResponseDTO;
import com.geolink.findme.authservice.dto.response.UserProfileDTO;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:authdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.default_schema=authservice",
        "spring.flyway.schemas=authservice",
        "spring.flyway.default-schema=authservice",
        "spring.flyway.create-schemas=true"
})
class AuthSpringBootTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
    }

    @Test
    void fullAuthFlow_Success() {
        // 1. Sign Up
        SignUpRequestDTO signUpReq = SignUpRequestDTO.builder()
                .email("flow.user@geolink.com")
                .password("SecurePass123")
                .firstName("Flow")
                .lastName("User")
                .build();

        ResponseEntity<UserProfileDTO> signUpResp = restTemplate.postForEntity(
                baseUrl + "/api/auth/signup",
                signUpReq,
                UserProfileDTO.class
        );

        assertThat(signUpResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(signUpResp.getBody()).isNotNull();
        assertThat(signUpResp.getBody().getEmail()).isEqualTo("flow.user@geolink.com");
        assertThat(signUpResp.getBody().getRole()).isEqualTo("USER");

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

        // 3. GET /api/users/me (Authenticated)
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
        assertThat(meResp.getBody().getFirstName()).isEqualTo("Flow");

        // 4. Refresh Token
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
        ResponseEntity<Void> logoutResp = restTemplate.exchange(
                baseUrl + "/api/auth/logout",
                HttpMethod.POST,
                meEntity,
                Void.class
        );

        assertThat(logoutResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
}
