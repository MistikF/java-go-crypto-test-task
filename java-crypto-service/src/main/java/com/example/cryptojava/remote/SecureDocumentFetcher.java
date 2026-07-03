package com.example.cryptojava.remote;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;

@Service
public class SecureDocumentFetcher {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public FetchedDocument fetch(String url) throws Exception {
        URI uri = URI.create(url);
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("Only HTTPS URLs are allowed");
        }

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException("Remote resource returned HTTP " + response.statusCode());
        }

        byte[] body = response.body();
        String contentType = response.headers().firstValue("content-type").orElse("application/octet-stream");
        String sha256 = sha256Hex(body);
        return new FetchedDocument(contentType, sha256, Base64.getEncoder().encodeToString(body), body);
    }

    private String sha256Hex(byte[] data) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
        StringBuilder builder = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            builder.append(Character.forDigit((b >> 4) & 0xF, 16));
            builder.append(Character.forDigit(b & 0xF, 16));
        }
        return builder.toString();
    }

    public static final class FetchedDocument {
        private final String contentType;
        private final String sha256;
        private final String documentBase64;
        private final byte[] raw;

        public FetchedDocument(String contentType, String sha256, String documentBase64, byte[] raw) {
            this.contentType = contentType;
            this.sha256 = sha256;
            this.documentBase64 = documentBase64;
            this.raw = raw;
        }

        public String getContentType() {
            return contentType;
        }

        public String getSha256() {
            return sha256;
        }

        public String getDocumentBase64() {
            return documentBase64;
        }

        public byte[] getRaw() {
            return raw;
        }
    }
}
