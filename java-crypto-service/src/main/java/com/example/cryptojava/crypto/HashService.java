package com.example.cryptojava.crypto;

import org.springframework.stereotype.Service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class HashService {

    private static final int PBKDF2_ITERATIONS = 120_000;
    private static final int PBKDF2_KEY_LENGTH = 256;
    private static final int PBKDF2_SALT_BYTES = 16;

    private final SecureRandom secureRandom = new SecureRandom();

    public HashResult hash(byte[] data, String algorithm) throws Exception {
        String normalized = normalize(algorithm);
        if ("PBKDF2".equals(normalized)) {
            return pbkdf2(data);
        }
        MessageDigest digest = MessageDigest.getInstance(normalized);
        return new HashResult(normalized, toHex(digest.digest(data)));
    }

    private HashResult pbkdf2(byte[] data) throws Exception {
        byte[] salt = new byte[PBKDF2_SALT_BYTES];
        secureRandom.nextBytes(salt);

        char[] password = new String(data, StandardCharsets.UTF_8).toCharArray();
        PBEKeySpec spec = new PBEKeySpec(password, salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH);
        byte[] derived = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();

        String encoded = "pbkdf2$" + PBKDF2_ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(derived);
        return new HashResult("PBKDF2WithHmacSHA256", encoded);
    }

    private String normalize(String algorithm) {
        if (algorithm == null || algorithm.isBlank()) {
            return "SHA-256";
        }
        String upper = algorithm.trim().toUpperCase();
        switch (upper) {
            case "SHA256":
            case "SHA-256":
                return "SHA-256";
            case "SHA512":
            case "SHA-512":
                return "SHA-512";
            case "PBKDF2":
            case "PBKDF2WITHHMACSHA256":
                return "PBKDF2";
            default:
                throw new IllegalArgumentException("Unsupported hash algorithm: " + algorithm);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(Character.forDigit((b >> 4) & 0xF, 16));
            builder.append(Character.forDigit(b & 0xF, 16));
        }
        return builder.toString();
    }

    public static final class HashResult {
        private final String algorithm;
        private final String value;

        public HashResult(String algorithm, String value) {
            this.algorithm = algorithm;
            this.value = value;
        }

        public String getAlgorithm() {
            return algorithm;
        }

        public String getValue() {
            return value;
        }
    }
}
