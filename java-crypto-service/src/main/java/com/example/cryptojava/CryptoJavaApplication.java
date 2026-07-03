package com.example.cryptojava;

import com.example.cryptojava.crypto.KeyStoreProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(KeyStoreProperties.class)
public class CryptoJavaApplication {

    public static void main(String[] args) {
        SpringApplication.run(CryptoJavaApplication.class, args);
    }
}
