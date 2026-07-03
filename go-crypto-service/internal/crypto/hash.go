package crypto

import (
	"crypto/rand"
	"crypto/sha256"
	"crypto/sha512"
	"encoding/base64"
	"encoding/hex"
	"fmt"
	"strings"

	"golang.org/x/crypto/pbkdf2"
)

const (
	pbkdf2Iterations = 120000
	pbkdf2KeyLength  = 32
	pbkdf2SaltBytes  = 16
)

type HashResult struct {
	Algorithm string
	Value     string
}

func Hash(data []byte, algorithm string) (HashResult, error) {
	switch normalize(algorithm) {
	case "SHA-256":
		sum := sha256.Sum256(data)
		return HashResult{Algorithm: "SHA-256", Value: hex.EncodeToString(sum[:])}, nil
	case "SHA-512":
		sum := sha512.Sum512(data)
		return HashResult{Algorithm: "SHA-512", Value: hex.EncodeToString(sum[:])}, nil
	case "PBKDF2":
		return pbkdf2Hash(data)
	default:
		return HashResult{}, fmt.Errorf("unsupported hash algorithm: %s", algorithm)
	}
}

func pbkdf2Hash(data []byte) (HashResult, error) {
	salt := make([]byte, pbkdf2SaltBytes)
	if _, err := rand.Read(salt); err != nil {
		return HashResult{}, err
	}
	derived := pbkdf2.Key(data, salt, pbkdf2Iterations, pbkdf2KeyLength, sha256.New)
	encoded := fmt.Sprintf("pbkdf2$%d$%s$%s", pbkdf2Iterations,
		base64.StdEncoding.EncodeToString(salt),
		base64.StdEncoding.EncodeToString(derived))
	return HashResult{Algorithm: "PBKDF2WithHmacSHA256", Value: encoded}, nil
}

func normalize(algorithm string) string {
	switch strings.ToUpper(strings.TrimSpace(algorithm)) {
	case "", "SHA256", "SHA-256":
		return "SHA-256"
	case "SHA512", "SHA-512":
		return "SHA-512"
	case "PBKDF2", "PBKDF2WITHHMACSHA256":
		return "PBKDF2"
	default:
		return algorithm
	}
}
