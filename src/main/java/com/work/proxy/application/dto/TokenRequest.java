package com.work.proxy.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
@Schema(description = "OAuth token request form parameters")
public class TokenRequest {
    @Schema(name = "grant_type", description = "OAuth grant type", requiredMode = Schema.RequiredMode.REQUIRED, example = "")
    @NotBlank(message = "grant_type is required")
    private String grantType;

    @Schema(name = "client_id", description = "Client ID", requiredMode = Schema.RequiredMode.REQUIRED,example = "")
    @NotBlank(message = "client_id is required")
    private String clientId;

    @Schema(name = "client_secret", description = "Client secret", requiredMode = Schema.RequiredMode.REQUIRED, example = "")
    @NotBlank(message = "client_secret is required")
    @ToString.Exclude
    private String clientSecret;

    @Schema(name = "username", description = "Username", requiredMode = Schema.RequiredMode.REQUIRED, example = "")
    @NotBlank(message = "username is required")
    private String userName;

    @Schema(name = "password", description = "Password", requiredMode = Schema.RequiredMode.REQUIRED, example = "")
    @NotBlank(message = "password is required")
    @ToString.Exclude
    private String password;

    @Schema(name = "scope", description = "Requested token scope", requiredMode = Schema.RequiredMode.REQUIRED, example = "")
    @NotBlank(message = "scope is required")
    private String scope;


}
