package com.work.proxy.infrastructure.adapter.in.rest;

import com.work.proxy.application.dto.Response;
import com.work.proxy.application.dto.TokenRequest;
import com.work.proxy.application.dto.TokenResponse;
import com.work.proxy.application.dto.TokenResponseEnvelope;
import com.work.proxy.application.port.in.OAuthUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import com.work.proxy.application.dto.Error;

@Slf4j
@RestController
@RequestMapping("/api/v1/oauth")
@RequiredArgsConstructor
@Tag(name = "OAuth", description = "BFF OAuth endpoints")
public class OAuthController {

    private final OAuthUseCase oAuthUseCase;

    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Obtain OAuth token",
            description = "It obtains an access token from the OAuth server and transforms it for the frontend"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    mediaType = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                    schema = @Schema(implementation = TokenRequest.class)))
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Token successfully obtained",
                    content = @Content(schema = @Schema(implementation = TokenResponseEnvelope.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request parameters",
                    content = @Content(schema = @Schema(implementation = Error.class))),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials",
                    content = @Content(schema = @Schema(implementation = Error.class))),
            @ApiResponse(
                    responseCode = "500",
                    description = "General Error",
                    content = @Content(schema = @Schema(implementation = Error.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "OAuth service not available",
                    content = @Content(schema = @Schema(implementation = Error.class))
            )
    })
    public Mono<Response<TokenResponse>> getToken(ServerWebExchange exchange) {
        log.debug("Received OAuth token request");
        return exchange.getFormData()
                .flatMap(formData -> oAuthUseCase.getAccessToken(TokenRequest.builder()
                        .grantType(formData.getFirst("grant_type"))
                        .clientId(formData.getFirst("client_id"))
                        .clientSecret(formData.getFirst("client_secret"))
                        .userName(formData.getFirst("username"))
                        .password(formData.getFirst("password"))
                        .scope(formData.getFirst("scope"))
                        .build()));
    }
}
