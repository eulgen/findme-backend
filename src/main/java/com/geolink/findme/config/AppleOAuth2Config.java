package com.geolink.findme.config;

import com.geolink.findme.security.AppleClientSecretGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration personnalisée de ClientRegistrationRepository pour injecter
 * dynamiquement le client_secret JWT généré pour Apple Sign-In.
 */
@Configuration
@RequiredArgsConstructor
public class AppleOAuth2Config {

    private final AppleClientSecretGenerator secretGenerator;

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(OAuth2ClientProperties oAuth2ClientProperties) {
        List<ClientRegistration> registrations = new ArrayList<>(
                new OAuth2ClientPropertiesMapper(oAuth2ClientProperties)
                        .asClientRegistrations().values()
        );

        List<ClientRegistration> updatedRegistrations = registrations.stream()
                .map(registration -> {
                    if ("apple".equalsIgnoreCase(registration.getRegistrationId())) {
                        return ClientRegistration.withClientRegistration(registration)
                                .clientSecret(secretGenerator.generate())
                                .build();
                    }
                    return registration;
                })
                .toList();

        return new InMemoryClientRegistrationRepository(updatedRegistrations);
    }
}
