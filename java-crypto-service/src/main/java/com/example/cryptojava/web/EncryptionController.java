package com.example.cryptojava.web;

import com.example.cryptojava.crypto.EnvelopeService;
import com.example.cryptojava.persistence.OperationLogger;
import com.example.cryptojava.web.dto.DecryptRequest;
import com.example.cryptojava.web.dto.DecryptResponse;
import com.example.cryptojava.web.dto.EncryptRequest;
import com.example.cryptojava.web.dto.EncryptResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Base64;

@RestController
@RequestMapping("/api/v1/encryption")
public class EncryptionController {

    private final EnvelopeService envelopeService;
    private final OperationLogger operationLogger;

    public EncryptionController(EnvelopeService envelopeService, OperationLogger operationLogger) {
        this.envelopeService = envelopeService;
        this.operationLogger = operationLogger;
    }

    @PostMapping("/encrypt")
    public EncryptResponse encrypt(@Valid @RequestBody EncryptRequest request) throws Exception {
        byte[] data = Base64.getDecoder().decode(request.getData());
        byte[] envelope = envelopeService.encrypt(data);
        operationLogger.success("ENCRYPT", data, envelope, "CMS EnvelopedData AES-256-CBC + RSA");
        return new EncryptResponse(Base64.getEncoder().encodeToString(envelope));
    }

    @PostMapping("/decrypt")
    public DecryptResponse decrypt(@Valid @RequestBody DecryptRequest request) throws Exception {
        byte[] envelope = Base64.getDecoder().decode(request.getEnvelope());
        byte[] data = envelopeService.decrypt(envelope);
        operationLogger.success("DECRYPT", envelope, data, "CMS EnvelopedData");
        return new DecryptResponse(Base64.getEncoder().encodeToString(data));
    }
}
