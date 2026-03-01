package com.work.proxy.application.port.in;

import com.work.proxy.application.dto.Response;
import com.work.proxy.application.dto.TokenRequest;
import com.work.proxy.application.dto.TokenResponse;
import jakarta.validation.Valid;
import reactor.core.publisher.Mono;

public interface OAuthUseCase {

    Mono<Response<TokenResponse>> getAccessToken(@Valid TokenRequest request);
}
