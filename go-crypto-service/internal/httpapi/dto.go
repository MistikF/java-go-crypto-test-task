package httpapi

type signRequest struct {
	Data     string `json:"data"`
	Detached bool   `json:"detached"`
}

type signResponse struct {
	Signature string `json:"signature"`
}

type verifyRequest struct {
	Signature string `json:"signature"`
	Data      string `json:"data"`
	Detached  bool   `json:"detached"`
}

type verifyResponse struct {
	Valid  bool   `json:"valid"`
	Signer string `json:"signer"`
}

type encryptRequest struct {
	Data string `json:"data"`
}

type encryptResponse struct {
	Envelope string `json:"envelope"`
}

type decryptRequest struct {
	Envelope string `json:"envelope"`
}

type decryptResponse struct {
	Data string `json:"data"`
}

type hashRequest struct {
	Data      string `json:"data"`
	Algorithm string `json:"algorithm"`
}

type hashResponse struct {
	Algorithm string `json:"algorithm"`
	Hash      string `json:"hash"`
}

type fetchRequest struct {
	URL string `json:"url"`
}

type fetchResponse struct {
	ContentType string `json:"contentType"`
	SHA256      string `json:"sha256"`
	Document    string `json:"document"`
}

type errorResponse struct {
	Error string `json:"error"`
}
