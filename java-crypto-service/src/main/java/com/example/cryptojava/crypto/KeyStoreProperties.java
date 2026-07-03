package com.example.cryptojava.crypto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.keystore")
public class KeyStoreProperties {

    private String path;
    private String type = "PKCS12";
    private String alias;
    private String password;
    private String keyPassword;

    public char[] storePassword() {
        return password == null ? new char[0] : password.toCharArray();
    }

    public char[] entryPassword() {
        String effective = keyPassword != null ? keyPassword : password;
        return effective == null ? new char[0] : effective.toCharArray();
    }
}
