package com.work.proxy.infrastructure.adapter.out.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.work.proxy.application.dto.TokenResponse;
import com.work.proxy.domain.exception.ServiceException;
import com.work.proxy.domain.port.out.OAuthPort;
import com.work.proxy.infrastructure.config.OAuthProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuthAdapter implements OAuthPort {

    private final WebClient oauthWebClient;
    private final OAuthProperties oAuthProperties;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<TokenResponse> requestToken(MultiValueMap<String, String> formData) {

        return oauthWebClient.post()
                .uri(oAuthProperties.getUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::mapClientResponse)
                .bodyToMono(TokenResponse.class)
                .timeout(Duration.ofMillis(oAuthProperties.getTimeout()))
                .retryWhen(buildRetrySpec())
                .onErrorMap(this::isConnectionError, ex -> mapWebClientRequestException())
                .doOnSuccess(response -> log.info("OAuth response received"))
                .doOnError(ex -> log.error("OAuth request failed — {}: {}", ex.getClass().getSimpleName(), ex.getMessage()));

    }

    private boolean isConnectionError(Throwable ex) {
        return ex instanceof WebClientRequestException ||
                ex instanceof TimeoutException ||
                (Exceptions.isRetryExhausted(ex) &&
                        (ex.getCause() instanceof WebClientRequestException ||
                         ex.getCause() instanceof TimeoutException));
    }

    private ServiceException mapWebClientRequestException() {
        log.debug("OAuth service unavailable after retries");
        return new ServiceException(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable");
    }

    private Mono<? extends Throwable> mapClientResponse(ClientResponse response) {
        HttpStatusCode status = response.statusCode();
        return response.bodyToMono(String.class)
                .map(body -> extractServiceException(status, body))
                .defaultIfEmpty(new ServiceException(status, "Error getting response from oauth service"));
    }

    private ServiceException extractServiceException(HttpStatusCode status, String body) {
        try {
            JsonNode node = objectMapper.readTree(body);
            String error = node.path("error").asText();
            String message = error.isBlank() ? "Could not get response from oauth service" : error;
            log.error("OAuth error response: {} - {}", status, message);
            return new ServiceException(status, message);
        } catch (Exception e) {
            log.error("OAuth error response: {} - could not parse body", status, e);
            return new ServiceException(status, "Could not parse body");
        }
    }


    private Retry buildRetrySpec() {
        return Retry.backoff(oAuthProperties.getAttempts(), Duration.ofMillis(500))
                .maxBackoff(Duration.ofSeconds(5))
                .filter(ex -> ex instanceof TimeoutException || ex instanceof WebClientRequestException)
                .doBeforeRetry(signal -> log.debug("Retrying OAuth request, attempt: {}", signal.totalRetries() + 1));
    }


}
