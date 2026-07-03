package com.example.cryptojava.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VerifyResponse {

    private boolean valid;
    private String signer;
}
