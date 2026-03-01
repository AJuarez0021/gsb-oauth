package com.work.proxy.infrastructure.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "oauth.client")
public class OAuthProperties {

    private String url;

    private Integer attempts = 3;

    private Long timeout = 15000L;

}
