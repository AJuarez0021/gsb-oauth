package com.work.proxy.infrastructure.adapter.in.rest;

import com.work.proxy.application.dto.TokenResponse;
import com.work.proxy.application.service.OAuthService;
import com.work.proxy.domain.exception.ServiceException;
import com.work.proxy.domain.port.out.OAuthPort;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import com.work.proxy.application.dto.TokenRequest;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = OAuthController.class)
@Import(OAuthService.class)
class OAuthControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private OAuthPort oAuthPort;

    private static BodyInserters.FormInserter<String> fullForm(String grantType) {
        return BodyInserters.fromFormData("grant_type", grantType)
                .with("client_id", "test-client")
                .with("client_secret", "test-secret")
                .with("username", "user")
                .with("password", "pass")
                .with("scope", "read");
    }

    @Test
    void shouldGetTokenSuccessfully() {
        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken("test-token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .build();
        when(oAuthPort.requestToken(any())).thenReturn(Mono.just(tokenResponse));

        webTestClient.post()
                .uri("/api/v1/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(fullForm("client_credentials"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.access_token").isEqualTo("test-token")
                .jsonPath("$.data.token_type").isEqualTo("Bearer")
                .jsonPath("$.data.expires_in").isEqualTo(3600);
    }

    @Test
    void shouldHandleServiceUnavailable() {
        when(oAuthPort.requestToken(any()))
                .thenReturn(Mono.error(new ServiceException(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable")));

        webTestClient.post()
                .uri("/api/v1/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(fullForm("client_credentials"))
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.status").isEqualTo(503)
                .jsonPath("$.error").isEqualTo("Service Unavailable");
    }

    @Test
    void shouldHandleTimeout() {
        when(oAuthPort.requestToken(any()))
                .thenReturn(Mono.error(new ServiceException(HttpStatus.SERVICE_UNAVAILABLE, "Timeout en OAuth")));

        webTestClient.post()
                .uri("/api/v1/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(fullForm("client_credentials"))
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.error").isEqualTo("Timeout en OAuth");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldPassFormDataToUseCase() {
        TokenResponse tokenResponse = TokenResponse.builder().accessToken("token").build();
        when(oAuthPort.requestToken(any())).thenReturn(Mono.just(tokenResponse));

        webTestClient.post()
                .uri("/api/v1/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "password")
                        .with("client_id", "my-client")
                        .with("client_secret", "my-secret")
                        .with("username", "user")
                        .with("password", "pass")
                        .with("scope", "read"))
                .exchange()
                .expectStatus().isOk();

        ArgumentCaptor<TokenRequest> captor = ArgumentCaptor.forClass(TokenRequest.class);
        verify(oAuthPort).requestToken(captor.capture());
        TokenRequest captured = captor.getValue();
        assertEquals("password",  captured.getGrantType());
        assertEquals("user",      captured.getUserName());
        assertEquals("pass",      captured.getPassword());
        assertEquals("my-client", captured.getClientId());
    }

    @Test
    void shouldReturnTokenWithScope() {
        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken("token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .scope("read write")
                .build();
        when(oAuthPort.requestToken(any())).thenReturn(Mono.just(tokenResponse));

        webTestClient.post()
                .uri("/api/v1/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(fullForm("client_credentials"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.scope").isEqualTo("read write");
    }

    @Test
    void shouldReturnTokenWithRefreshToken() {
        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .build();
        when(oAuthPort.requestToken(any())).thenReturn(Mono.just(tokenResponse));

        webTestClient.post()
                .uri("/api/v1/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(fullForm("password"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.access_token").isEqualTo("access-token")
                .jsonPath("$.data.refresh_token").isEqualTo("refresh-token");
    }

    @Test
    void shouldReturn400WhenGrantTypeIsMissing() {
        webTestClient.post()
                .uri("/api/v1/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("client_id", "test"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.error").isNotEmpty();
    }

    @Test
    void shouldReturn400WhenMultipleRequiredFieldsAreMissing() {
        webTestClient.post()
                .uri("/api/v1/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "password"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.error").value(error ->
                        assertTrue(error.toString().contains("is required")));
    }

    @Test
    void shouldReturn400WhenFieldsAreBlank() {
        webTestClient.post()
                .uri("/api/v1/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "")
                        .with("client_id", "")
                        .with("client_secret", "")
                        .with("username", "")
                        .with("password", "")
                        .with("scope", ""))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.error").isNotEmpty();
    }

    @Test
    void shouldHandleUnauthorized() {
        when(oAuthPort.requestToken(any()))
                .thenReturn(Mono.error(new ServiceException(HttpStatus.UNAUTHORIZED, "Invalid credentials")));

        webTestClient.post()
                .uri("/api/v1/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(fullForm("client_credentials"))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.error").isEqualTo("Invalid credentials");
    }
}
