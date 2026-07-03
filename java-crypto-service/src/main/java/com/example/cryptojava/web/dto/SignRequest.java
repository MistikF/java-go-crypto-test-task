package com.example.cryptojava.web.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class SignRequest {

    @NotBlank
    private String data;

    private boolean detached;
}
