package crypto

import (
	"crypto/x509"

	"github.com/smallstep/pkcs7"
)

func (i *Identity) Encrypt(data []byte) ([]byte, error) {
	pkcs7.ContentEncryptionAlgorithm = pkcs7.EncryptionAlgorithmAES256CBC
	return pkcs7.Encrypt(data, []*x509.Certificate{i.Certificate})
}

func (i *Identity) Decrypt(envelope []byte) ([]byte, error) {
	parsed, err := pkcs7.Parse(envelope)
	if err != nil {
		return nil, err
	}
	return parsed.Decrypt(i.Certificate, i.PrivateKey)
}
