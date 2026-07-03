package crypto

import (
	"errors"

	"github.com/smallstep/pkcs7"
)

type VerificationResult struct {
	Valid  bool
	Signer string
}

func (i *Identity) Sign(data []byte, detached bool) ([]byte, error) {
	signedData, err := pkcs7.NewSignedData(data)
	if err != nil {
		return nil, err
	}
	signedData.SetDigestAlgorithm(pkcs7.OIDDigestAlgorithmSHA256)

	if err := signedData.AddSigner(i.Certificate, i.PrivateKey, pkcs7.SignerInfoConfig{}); err != nil {
		return nil, err
	}
	if detached {
		signedData.Detach()
	}
	return signedData.Finish()
}

func (i *Identity) Verify(signature, original []byte, detached bool) (VerificationResult, error) {
	parsed, err := pkcs7.Parse(signature)
	if err != nil {
		return VerificationResult{}, err
	}
	if detached {
		if len(original) == 0 {
			return VerificationResult{}, errors.New("detached verification requires the original data")
		}
		parsed.Content = original
	}
	if err := parsed.Verify(); err != nil {
		return VerificationResult{Valid: false}, nil
	}
	signer := parsed.GetOnlySigner()
	subject := ""
	if signer != nil {
		subject = signer.Subject.String()
	}
	return VerificationResult{Valid: true, Signer: subject}, nil
}
