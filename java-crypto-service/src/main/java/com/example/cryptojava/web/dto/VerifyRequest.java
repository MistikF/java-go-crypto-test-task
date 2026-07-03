package com.example.cryptojava.web.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class VerifyRequest {

    @NotBlank
    private String signature;

    private String data;

    private boolean detached;
}
