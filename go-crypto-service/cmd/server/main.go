package main

import (
	"crypto/tls"
	"crypto/x509"
	"log"
	"net/http"
	"os"

	"github.com/example/go-crypto-service/internal/config"
	"github.com/example/go-crypto-service/internal/crypto"
	"github.com/example/go-crypto-service/internal/httpapi"
	"github.com/example/go-crypto-service/internal/remote"
	"github.com/example/go-crypto-service/internal/storage"
)

func main() {
	cfg := config.Load()

	identity, err := crypto.LoadIdentity(cfg.CryptoCert, cfg.CryptoKey)
	if err != nil {
		log.Fatalf("load crypto identity: %v", err)
	}

	store, err := storage.Open(cfg.DBDSN)
	if err != nil {
		log.Fatalf("open database: %v", err)
	}
	defer store.Close()

	api := httpapi.New(identity, remote.NewFetcher(), store)
	server := &http.Server{Addr: cfg.ServerAddr, Handler: api.Router()}

	if cfg.TLSEnabled {
		tlsConfig, err := buildTLSConfig(cfg)
		if err != nil {
			log.Fatalf("configure TLS: %v", err)
		}
		server.TLSConfig = tlsConfig
		log.Printf("go-crypto-service listening on %s (https)", cfg.ServerAddr)
		log.Fatal(server.ListenAndServeTLS(cfg.TLSCert, cfg.TLSKey))
	}

	log.Printf("go-crypto-service listening on %s (http)", cfg.ServerAddr)
	log.Fatal(server.ListenAndServe())
}

func buildTLSConfig(cfg config.Config) (*tls.Config, error) {
	tlsConfig := &tls.Config{MinVersion: tls.VersionTLS12}
	if !cfg.MTLSEnabled {
		return tlsConfig, nil
	}
	caPem, err := os.ReadFile(cfg.CACert)
	if err != nil {
		return nil, err
	}
	pool := x509.NewCertPool()
	if !pool.AppendCertsFromPEM(caPem) {
		return nil, err
	}
	tlsConfig.ClientCAs = pool
	tlsConfig.ClientAuth = tls.RequireAndVerifyClientCert
	return tlsConfig, nil
}
