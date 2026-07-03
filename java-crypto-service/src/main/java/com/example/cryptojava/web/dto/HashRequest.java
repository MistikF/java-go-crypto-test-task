package com.example.cryptojava.web.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class HashRequest {

    @NotBlank
    private String data;

    private String algorithm;
}
