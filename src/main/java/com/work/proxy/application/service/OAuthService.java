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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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
        return oAuthPort.requestToken(toFormData(request)).map(Response::new);
    }

    private static MultiValueMap<String, String> toFormData(TokenRequest request) {
        log.info("Request_ {}", request);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.set("client_id", request.getClientId());
        form.set("client_secret", request.getClientSecret());
        form.set("grant_type", request.getGrantType());
        form.set("username", request.getUserName());
        form.set("password", request.getPassword());
        form.set("scope", request.getScope());
        return form;
    }

}
