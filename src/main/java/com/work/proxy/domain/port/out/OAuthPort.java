package com.work.proxy.domain.port.out;

import com.work.proxy.application.dto.TokenRequest;
import com.work.proxy.application.dto.TokenResponse;
import reactor.core.publisher.Mono;

public interface OAuthPort {

    Mono<TokenResponse> requestToken(TokenRequest request);
}
