package com.example.cryptojava.web;

import com.example.cryptojava.crypto.HashService;
import com.example.cryptojava.persistence.OperationLogger;
import com.example.cryptojava.web.dto.HashRequest;
import com.example.cryptojava.web.dto.HashResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@RestController
@RequestMapping("/api/v1/hash")
public class HashController {

    private final HashService hashService;
    private final OperationLogger operationLogger;

    public HashController(HashService hashService, OperationLogger operationLogger) {
        this.hashService = hashService;
        this.operationLogger = operationLogger;
    }

    @PostMapping
    public HashResponse hash(@Valid @RequestBody HashRequest request) throws Exception {
        byte[] data = Base64.getDecoder().decode(request.getData());
        HashService.HashResult result = hashService.hash(data, request.getAlgorithm());
        operationLogger.success("HASH", data, result.getValue().getBytes(StandardCharsets.UTF_8), result.getAlgorithm());
        return new HashResponse(result.getAlgorithm(), result.getValue());
    }
}
