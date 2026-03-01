package com.work.proxy.infrastructure.adapter.out.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.work.proxy.domain.exception.ServiceException;
import com.work.proxy.infrastructure.config.OAuthProperties;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class OAuthAdapterTest {

    private MockWebServer mockWebServer;
    private OAuthAdapter oAuthAdapter;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        OAuthProperties oAuthProperties = new OAuthProperties();
        oAuthProperties.setUrl(mockWebServer.url("/oauth/token").toString());
        oAuthProperties.setAttempts(1);

        WebClient oauthWebClient = WebClient.builder().build();
        oAuthAdapter = new OAuthAdapter(oauthWebClient, oAuthProperties, new ObjectMapper());
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void shouldRequestTokenSuccessfully() throws InterruptedException {
        String tokenResponse = """
                {
                    "access_token": "test-access-token",
                    "token_type": "Bearer",
                    "expires_in": 3600
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(tokenResponse)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");
        formData.add("client_id", "test-client");
        formData.add("client_secret", "test-secret");

        StepVerifier.create(oAuthAdapter.requestToken(formData))
                .assertNext(response -> {
                    assertTrue(response.getAccessToken().contains("test-access-token"));
                    assertTrue(response.getTokenType().contains("Bearer"));
                })
                .verifyComplete();

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("/oauth/token", request.getPath());
    }

    @Test
    void shouldReturnRawJsonResponse() {
        String tokenResponse = """
                {
                    "access_token": "token123",
                    "refresh_token": "custom_value"
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(tokenResponse)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");

        StepVerifier.create(oAuthAdapter.requestToken(formData))
                .assertNext(response -> {
                    assertTrue(response.getAccessToken().contains("token123"));
                    assertTrue(response.getRefreshToken().contains("custom_value"));
                })
                .verifyComplete();
    }

    @Test
    void shouldThrowServiceExceptionOnUnauthorized() {
        String errorResponse = """
                {
                    "error": "invalid_client",
                    "error_description": "Client authentication failed"
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(401)
                .setBody(errorResponse)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");

        StepVerifier.create(oAuthAdapter.requestToken(formData))
                .expectErrorMatches(throwable ->
                        throwable instanceof ServiceException &&
                        ((ServiceException) throwable).getStatusCode().value() == 401 &&
                        "invalid_client".equals(throwable.getMessage()))
                .verify();
    }

    @Test
    void shouldThrowServiceExceptionOnServerError() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("{\"error\": \"server_error\"}")
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");

        StepVerifier.create(oAuthAdapter.requestToken(formData))
                .expectErrorMatches(throwable ->
                        throwable instanceof ServiceException &&
                        ((ServiceException) throwable).getStatusCode().value() == 500 &&
                        "server_error".equals(throwable.getMessage()))
                .verify();
    }

    @Test
    void shouldFallbackToDefaultMessageWhenNoErrorField() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(400)
                .setBody("{\"message\": \"bad request\"}")
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");

        StepVerifier.create(oAuthAdapter.requestToken(formData))
                .expectErrorMatches(throwable ->
                        throwable instanceof ServiceException &&
                        ((ServiceException) throwable).getStatusCode().value() == 400 &&
                        "Could not get response from oauth service".equals(throwable.getMessage()))
                .verify();
    }

    @Test
    void shouldThrowServiceExceptionWhenServiceIsUnavailable() throws IOException {
        mockWebServer.shutdown();
        mockWebServer = new MockWebServer(); // permite que tearDown haga shutdown sin error

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");

        StepVerifier.create(oAuthAdapter.requestToken(formData))
                .expectErrorMatches(throwable ->
                        throwable instanceof ServiceException &&
                        ((ServiceException) throwable).getStatusCode().value() == 503 &&
                        "Service Unavailable".equals(throwable.getMessage()))
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void shouldIncludeFormDataInRequest() throws InterruptedException {
        String tokenResponse = """
                {
                    "access_token": "test-token"
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(tokenResponse)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");
        formData.add("client_id", "test-client-id");
        formData.add("client_secret", "test-client-secret");
        formData.add("scope", "read write");

        StepVerifier.create(oAuthAdapter.requestToken(formData))
                .expectNextCount(1)
                .verifyComplete();

        RecordedRequest request = mockWebServer.takeRequest();
        String body = request.getBody().readUtf8();
        assertTrue(body.contains("grant_type=client_credentials"));
        assertTrue(body.contains("client_id=test-client-id"));
        assertTrue(body.contains("client_secret=test-client-secret"));
    }

    @Test
    void shouldThrowServiceExceptionOnTimeout() {
        OAuthProperties shortTimeoutProperties = new OAuthProperties();
        shortTimeoutProperties.setUrl(mockWebServer.url("/oauth/token").toString());
        shortTimeoutProperties.setAttempts(1);
        shortTimeoutProperties.setTimeout(200L);

        OAuthAdapter adapterWithShortTimeout = new OAuthAdapter(
                WebClient.builder().build(), shortTimeoutProperties, new ObjectMapper());

        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"access_token\": \"token\"}")
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBodyDelay(2, TimeUnit.SECONDS));
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"access_token\": \"token\"}")
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBodyDelay(2, TimeUnit.SECONDS));

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");

        StepVerifier.create(adapterWithShortTimeout.requestToken(formData))
                .expectErrorMatches(throwable ->
                        throwable instanceof ServiceException &&
                        ((ServiceException) throwable).getStatusCode().value() == 503)
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void shouldReturnEmptyTokenWhenResponseBodyIsEmpty() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{}")
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");

        StepVerifier.create(oAuthAdapter.requestToken(formData))
                .assertNext(response -> {
                    assertNull(response.getAccessToken());
                    assertNull(response.getTokenType());
                    assertNull(response.getExpiresIn());
                })
                .verifyComplete();
    }

    @Test
    void shouldPropagateErrorOnMalformedJson() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("not valid json {{{")
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");

        StepVerifier.create(oAuthAdapter.requestToken(formData))
                .expectError()
                .verify(Duration.ofSeconds(5));
    }

    @Test
    void shouldUseCorrectContentType() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{}")
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");

        StepVerifier.create(oAuthAdapter.requestToken(formData))
                .expectNextCount(1)
                .verifyComplete();

        RecordedRequest request = mockWebServer.takeRequest();
        assertTrue(request.getHeader(HttpHeaders.CONTENT_TYPE)
                .contains(MediaType.APPLICATION_FORM_URLENCODED_VALUE));
    }
}
