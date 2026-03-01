package com.work.proxy.application.service;

import com.work.proxy.application.dto.Response;
import com.work.proxy.application.dto.TokenRequest;
import com.work.proxy.application.dto.TokenResponse;
import com.work.proxy.application.port.in.OAuthUseCase;
import com.work.proxy.domain.port.out.OAuthPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
@Validated
public class OAuthService implements OAuthUseCase {

    private final OAuthPort oAuthPort;

    @Override
    public Mono<Response<TokenResponse>> getAccessToken(@Valid TokenRequest request) {
        log.info("Request: {}", request);
        return oAuthPort.requestToken(request).map(Response::new);
    }

}
