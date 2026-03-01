package com.work.proxy.application.service;

import com.work.proxy.application.dto.TokenRequest;
import com.work.proxy.application.port.in.OAuthUseCase;
import com.work.proxy.domain.port.out.OAuthPort;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class OAuthServiceValidationTest {

    @MockitoBean
    private OAuthPort oAuthPort;

    @Autowired
    private OAuthUseCase oAuthService;

    @Test
    void shouldThrowConstraintViolationWhenGrantTypeIsBlank() {
        TokenRequest request = TokenRequest.builder()
                .grantType("")
                .clientId("client")
                .clientSecret("secret")
                .userName("user")
                .password("pass")
                .scope("read")
                .build();

        assertThrows(ConstraintViolationException.class,
                () -> oAuthService.getAccessToken(request));
    }

    @Test
    void shouldThrowConstraintViolationWhenPasswordIsBlank() {
        TokenRequest request = TokenRequest.builder()
                .grantType("password")
                .clientId("client")
                .clientSecret("secret")
                .userName("user")
                .password("")
                .scope("read")
                .build();

        assertThrows(ConstraintViolationException.class,
                () -> oAuthService.getAccessToken(request));
    }

    @Test
    void shouldThrowConstraintViolationWhenClientIdIsNull() {
        TokenRequest request = TokenRequest.builder()
                .grantType("password")
                .clientId(null)
                .clientSecret("secret")
                .userName("user")
                .password("pass")
                .scope("read")
                .build();

        assertThrows(ConstraintViolationException.class,
                () -> oAuthService.getAccessToken(request));
    }

    @Test
    void shouldIncludeFieldNameInViolationMessage() {
        TokenRequest request = TokenRequest.builder()
                .grantType(null)
                .clientId("client")
                .clientSecret("secret")
                .userName("user")
                .password("pass")
                .scope("read")
                .build();

        ConstraintViolationException ex = assertThrows(ConstraintViolationException.class,
                () -> oAuthService.getAccessToken(request));

        assertTrue(ex.getConstraintViolations().stream()
                .anyMatch(v -> v.getMessage().contains("grant_type is required")));
    }
}
