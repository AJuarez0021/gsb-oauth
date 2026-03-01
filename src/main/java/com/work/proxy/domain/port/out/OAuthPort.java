package com.work.proxy.domain.port.out;

import com.work.proxy.application.dto.TokenResponse;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

public interface OAuthPort {

    Mono<TokenResponse> requestToken(MultiValueMap<String, String> formData);
}
