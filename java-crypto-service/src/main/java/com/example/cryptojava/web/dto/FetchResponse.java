package com.example.cryptojava.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FetchResponse {

    private String contentType;
    private String sha256;
    private String document;
}
