package crypto

import (
	"bytes"
	"testing"
)

const (
	certPath = "../../../certs/crypto/crypto.crt"
	keyPath  = "../../../certs/crypto/crypto.key"
)

func loadTestIdentity(t *testing.T) *Identity {
	t.Helper()
	identity, err := LoadIdentity(certPath, keyPath)
	if err != nil {
		t.Fatalf("load identity: %v", err)
	}
	return identity
}

func TestAttachedSignatureRoundTrip(t *testing.T) {
	identity := loadTestIdentity(t)
	data := []byte("attached payload")

	signature, err := identity.Sign(data, false)
	if err != nil {
		t.Fatalf("sign: %v", err)
	}
	result, err := identity.Verify(signature, nil, false)
	if err != nil {
		t.Fatalf("verify: %v", err)
	}
	if !result.Valid {
		t.Fatalf("expected valid signature")
	}
}

func TestDetachedSignatureRoundTrip(t *testing.T) {
	identity := loadTestIdentity(t)
	data := []byte("detached payload")

	signature, err := identity.Sign(data, true)
	if err != nil {
		t.Fatalf("sign: %v", err)
	}
	valid, err := identity.Verify(signature, data, true)
	if err != nil {
		t.Fatalf("verify: %v", err)
	}
	if !valid.Valid {
		t.Fatalf("expected valid detached signature")
	}
	tampered, err := identity.Verify(signature, []byte("tampered"), true)
	if err != nil {
		t.Fatalf("verify tampered: %v", err)
	}
	if tampered.Valid {
		t.Fatalf("expected invalid signature for tampered data")
	}
}

func TestEnvelopeRoundTrip(t *testing.T) {
	identity := loadTestIdentity(t)
	data := []byte("secret message")

	envelope, err := identity.Encrypt(data)
	if err != nil {
		t.Fatalf("encrypt: %v", err)
	}
	decrypted, err := identity.Decrypt(envelope)
	if err != nil {
		t.Fatalf("decrypt: %v", err)
	}
	if !bytes.Equal(data, decrypted) {
		t.Fatalf("decrypted mismatch: %q", decrypted)
	}
}

func TestSHA256KnownVector(t *testing.T) {
	result, err := Hash([]byte("abc"), "SHA-256")
	if err != nil {
		t.Fatalf("hash: %v", err)
	}
	const expected = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
	if result.Value != expected {
		t.Fatalf("expected %s, got %s", expected, result.Value)
	}
}
