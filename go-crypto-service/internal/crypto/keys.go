package crypto

import (
	"crypto"
	"crypto/x509"
	"encoding/pem"
	"errors"
	"fmt"
	"os"
)

type Identity struct {
	Certificate *x509.Certificate
	PrivateKey  crypto.PrivateKey
}

func LoadIdentity(certPath, keyPath string) (*Identity, error) {
	certificate, err := loadCertificate(certPath)
	if err != nil {
		return nil, err
	}
	privateKey, err := loadPrivateKey(keyPath)
	if err != nil {
		return nil, err
	}
	return &Identity{Certificate: certificate, PrivateKey: privateKey}, nil
}

func loadCertificate(path string) (*x509.Certificate, error) {
	block, err := readPem(path)
	if err != nil {
		return nil, err
	}
	return x509.ParseCertificate(block.Bytes)
}

func loadPrivateKey(path string) (crypto.PrivateKey, error) {
	block, err := readPem(path)
	if err != nil {
		return nil, err
	}
	if key, err := x509.ParsePKCS8PrivateKey(block.Bytes); err == nil {
		return key, nil
	}
	if key, err := x509.ParsePKCS1PrivateKey(block.Bytes); err == nil {
		return key, nil
	}
	return nil, fmt.Errorf("unsupported private key format in %s", path)
}

func readPem(path string) (*pem.Block, error) {
	raw, err := os.ReadFile(path)
	if err != nil {
		return nil, err
	}
	block, _ := pem.Decode(raw)
	if block == nil {
		return nil, errors.New("no PEM block found in " + path)
	}
	return block, nil
}
