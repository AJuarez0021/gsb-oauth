package com.work.proxy.application.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class Error {
    private String error;
    private int status;
    private Instant timestamp;
}
