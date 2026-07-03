package config

import "os"

type Config struct {
	ServerAddr  string
	TLSEnabled  bool
	MTLSEnabled bool
	TLSCert     string
	TLSKey      string
	CACert      string
	CryptoCert  string
	CryptoKey   string
	DBDSN       string
}

func Load() Config {
	return Config{
		ServerAddr:  env("SERVER_ADDR", ":8081"),
		TLSEnabled:  env("TLS_ENABLED", "false") == "true",
		MTLSEnabled: env("MTLS_ENABLED", "false") == "true",
		TLSCert:     env("TLS_CERT", "../certs/tls/server.crt"),
		TLSKey:      env("TLS_KEY", "../certs/tls/server.key"),
		CACert:      env("CA_CERT", "../certs/ca/ca.crt"),
		CryptoCert:  env("CRYPTO_CERT", "../certs/crypto/crypto.crt"),
		CryptoKey:   env("CRYPTO_KEY", "../certs/crypto/crypto.key"),
		DBDSN:       env("DB_DSN", "postgres://crypto:crypto@localhost:5433/crypto?sslmode=disable"),
	}
}

func env(key, fallback string) string {
	if value, ok := os.LookupEnv(key); ok && value != "" {
		return value
	}
	return fallback
}
