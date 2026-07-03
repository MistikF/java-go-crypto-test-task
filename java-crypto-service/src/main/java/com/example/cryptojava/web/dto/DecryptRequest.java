package com.example.cryptojava.web.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class DecryptRequest {

    @NotBlank
    private String envelope;
}
