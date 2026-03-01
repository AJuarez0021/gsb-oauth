package com.work.proxy.application.service;

import com.work.proxy.application.dto.TokenRequest;
import com.work.proxy.application.dto.TokenResponse;
import com.work.proxy.domain.exception.ServiceException;
import com.work.proxy.domain.port.out.OAuthPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuthServiceTest {

    @Mock
    private OAuthPort oAuthPort;

    private OAuthService oAuthService;

    @BeforeEach
    void setUp() {
        oAuthService = new OAuthService(oAuthPort);
    }

    @Test
    void shouldGetAccessTokenSuccessfully() {


        TokenResponse expectedResponse = TokenResponse.builder()
                .accessToken("test-token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .build();


        when(oAuthPort.requestToken(any())).thenReturn(Mono.just(expectedResponse));

        TokenRequest request = createRequest();

        StepVerifier.create(oAuthService.getAccessToken(request))
                .assertNext(response -> {
                    assertEquals("test-token", response.getData().getAccessToken());
                    assertEquals("Bearer", response.getData().getTokenType());
                    assertEquals(3600L, response.getData().getExpiresIn());
                })
                .verifyComplete();

        verify(oAuthPort).requestToken(any());

    }


    private  TokenRequest createRequest() {
        return TokenRequest.builder()
                .scope("read")
                .grantType("password")
                .clientSecret("test-secret")
                .clientId("test-client")
                .userName("user")
                .password("123")
                .build();
    }

    @Test
    void shouldPropagateErrorFromPort() {
        when(oAuthPort.requestToken(any()))
                .thenReturn(Mono.error(new ServiceException(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable")));

        TokenRequest request = createRequest();

        StepVerifier.create(oAuthService.getAccessToken(request))
                .expectErrorMatches(throwable ->
                        throwable instanceof ServiceException &&
                        ((ServiceException) throwable).getStatusCode().value() == 503)
                .verify();
    }

    @Test
    void shouldHandlePasswordGrant() {


        TokenResponse expectedResponse = TokenResponse.builder()
                .accessToken("user-token")
                .refreshToken("refresh-token")
                .tokenType("Bearer")
                .expiresIn(7200L)
                .build();


        when(oAuthPort.requestToken(any())).thenReturn(Mono.just(expectedResponse));

        TokenRequest request = createRequest();

        StepVerifier.create(oAuthService.getAccessToken(request))
                .assertNext(response -> {
                    assertEquals("user-token", response.getData().getAccessToken());
                    assertEquals("refresh-token", response.getData().getRefreshToken());
                })
                .verifyComplete();
    }
}
