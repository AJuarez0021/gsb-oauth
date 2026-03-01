package com.work.proxy.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "webclient")
public class WebClientProperties {

    private int maxConnections = 500;

    private int connectionTimeout = 5000;

    private int readTimeout = 10000;

    private int writeTimeout = 10000;
}
