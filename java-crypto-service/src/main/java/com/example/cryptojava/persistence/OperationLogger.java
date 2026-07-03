package com.example.cryptojava.persistence;

import org.springframework.stereotype.Component;

@Component
public class OperationLogger {

    private static final String SERVICE_NAME = "java";

    private final CryptoOperationRepository repository;

    public OperationLogger(CryptoOperationRepository repository) {
        this.repository = repository;
    }

    public void success(String operation, byte[] input, byte[] output, String detail) {
        repository.save(new CryptoOperation(SERVICE_NAME, operation, input, output, "OK", detail));
    }

    public void failure(String operation, byte[] input, String detail) {
        repository.save(new CryptoOperation(SERVICE_NAME, operation, input, null, "ERROR", detail));
    }
}
