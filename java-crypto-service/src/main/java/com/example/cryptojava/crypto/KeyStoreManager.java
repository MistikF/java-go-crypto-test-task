package com.example.cryptojava.crypto;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

@Component
public class KeyStoreManager {

    private final KeyStoreProperties properties;
    private X509Certificate certificate;
    private PrivateKey privateKey;

    public KeyStoreManager(KeyStoreProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void load() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(properties.getType());
        try (InputStream in = Files.newInputStream(Path.of(properties.getPath()))) {
            keyStore.load(in, properties.storePassword());
        }

        String alias = resolveAlias(keyStore);
        this.certificate = (X509Certificate) keyStore.getCertificate(alias);
        this.privateKey = (PrivateKey) keyStore.getKey(alias, properties.entryPassword());

        if (certificate == null || privateKey == null) {
            throw new IllegalStateException("Keystore entry '" + alias + "' is missing a certificate or private key");
        }
    }

    private String resolveAlias(KeyStore keyStore) throws Exception {
        if (properties.getAlias() != null && keyStore.containsAlias(properties.getAlias())) {
            return properties.getAlias();
        }
        return keyStore.aliases().nextElement();
    }

    public X509Certificate getCertificate() {
        return certificate;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }
}
