package com.example.cryptojava.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.Security;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CryptoServicesTest {

    private static KeyStoreManager keyStoreManager;

    @BeforeAll
    static void setUp() throws Exception {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        KeyStoreProperties properties = new KeyStoreProperties();
        properties.setPath("../certs/crypto/crypto.p12");
        properties.setType("PKCS12");
        properties.setAlias("crypto");
        properties.setPassword("changeit");
        properties.setKeyPassword("changeit");
        keyStoreManager = new KeyStoreManager(properties);
        keyStoreManager.load();
    }

    @Test
    void attachedSignatureRoundTrip() throws Exception {
        Pkcs7Service service = new Pkcs7Service(keyStoreManager);
        byte[] data = "attached payload".getBytes(StandardCharsets.UTF_8);

        byte[] signature = service.sign(data, false);
        Pkcs7Service.VerificationResult result = service.verify(signature, null, false);

        assertTrue(result.isValid());
        assertTrue(result.getSigner().contains("crypto-identity"));
    }

    @Test
    void detachedSignatureRoundTrip() throws Exception {
        Pkcs7Service service = new Pkcs7Service(keyStoreManager);
        byte[] data = "detached payload".getBytes(StandardCharsets.UTF_8);

        byte[] signature = service.sign(data, true);

        assertTrue(service.verify(signature, data, true).isValid());
        assertFalse(service.verify(signature, "tampered".getBytes(StandardCharsets.UTF_8), true).isValid());
    }

    @Test
    void envelopeRoundTrip() throws Exception {
        EnvelopeService service = new EnvelopeService(keyStoreManager);
        byte[] data = "secret message".getBytes(StandardCharsets.UTF_8);

        byte[] envelope = service.encrypt(data);
        byte[] decrypted = service.decrypt(envelope);

        assertArrayEquals(data, decrypted);
    }

    @Test
    void sha256MatchesKnownVector() throws Exception {
        HashService service = new HashService();
        HashService.HashResult result = service.hash("abc".getBytes(StandardCharsets.UTF_8), "SHA-256");

        assertEquals("SHA-256", result.getAlgorithm());
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", result.getValue());
    }
}
