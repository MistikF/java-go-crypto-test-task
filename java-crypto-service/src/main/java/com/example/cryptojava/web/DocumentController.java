package com.example.cryptojava.web;

import com.example.cryptojava.persistence.OperationLogger;
import com.example.cryptojava.remote.SecureDocumentFetcher;
import com.example.cryptojava.web.dto.FetchRequest;
import com.example.cryptojava.web.dto.FetchResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final SecureDocumentFetcher fetcher;
    private final OperationLogger operationLogger;

    public DocumentController(SecureDocumentFetcher fetcher, OperationLogger operationLogger) {
        this.fetcher = fetcher;
        this.operationLogger = operationLogger;
    }

    @PostMapping("/fetch")
    public FetchResponse fetch(@Valid @RequestBody FetchRequest request) throws Exception {
        SecureDocumentFetcher.FetchedDocument document = fetcher.fetch(request.getUrl());
        operationLogger.success("FETCH", request.getUrl().getBytes(StandardCharsets.UTF_8), document.getRaw(),
                "sha256=" + document.getSha256() + "; contentType=" + document.getContentType());
        return new FetchResponse(document.getContentType(), document.getSha256(), document.getDocumentBase64());
    }
}
