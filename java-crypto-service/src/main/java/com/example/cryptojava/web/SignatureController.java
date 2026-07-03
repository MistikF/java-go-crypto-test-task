package com.example.cryptojava.web;

import com.example.cryptojava.crypto.Pkcs7Service;
import com.example.cryptojava.persistence.OperationLogger;
import com.example.cryptojava.web.dto.SignRequest;
import com.example.cryptojava.web.dto.SignResponse;
import com.example.cryptojava.web.dto.VerifyRequest;
import com.example.cryptojava.web.dto.VerifyResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Base64;

@RestController
@RequestMapping("/api/v1/signatures")
public class SignatureController {

    private final Pkcs7Service pkcs7Service;
    private final OperationLogger operationLogger;

    public SignatureController(Pkcs7Service pkcs7Service, OperationLogger operationLogger) {
        this.pkcs7Service = pkcs7Service;
        this.operationLogger = operationLogger;
    }

    @PostMapping("/sign")
    public SignResponse sign(@Valid @RequestBody SignRequest request) throws Exception {
        byte[] data = Base64.getDecoder().decode(request.getData());
        byte[] signature = pkcs7Service.sign(data, request.isDetached());
        operationLogger.success("SIGN", data, signature, request.isDetached() ? "detached" : "attached");
        return new SignResponse(Base64.getEncoder().encodeToString(signature));
    }

    @PostMapping("/verify")
    public VerifyResponse verify(@Valid @RequestBody VerifyRequest request) throws Exception {
        byte[] signature = Base64.getDecoder().decode(request.getSignature());
        byte[] data = request.getData() == null ? new byte[0] : Base64.getDecoder().decode(request.getData());

        Pkcs7Service.VerificationResult result = pkcs7Service.verify(signature, data, request.isDetached());
        operationLogger.success("VERIFY", signature, null,
                "valid=" + result.isValid() + (result.getSigner() != null ? "; signer=" + result.getSigner() : ""));
        return new VerifyResponse(result.isValid(), result.getSigner());
    }
}
