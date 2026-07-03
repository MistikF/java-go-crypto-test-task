package remote

import (
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"time"
)

type Document struct {
	ContentType string
	SHA256      string
	Raw         []byte
}

type Fetcher struct {
	client *http.Client
}

func NewFetcher() *Fetcher {
	return &Fetcher{client: &http.Client{Timeout: 30 * time.Second}}
}

func (f *Fetcher) Fetch(rawURL string) (*Document, error) {
	parsed, err := url.Parse(rawURL)
	if err != nil {
		return nil, err
	}
	if parsed.Scheme != "https" {
		return nil, fmt.Errorf("only HTTPS URLs are allowed")
	}

	response, err := f.client.Get(rawURL)
	if err != nil {
		return nil, err
	}
	defer response.Body.Close()

	if response.StatusCode/100 != 2 {
		return nil, fmt.Errorf("remote resource returned HTTP %d", response.StatusCode)
	}

	body, err := io.ReadAll(response.Body)
	if err != nil {
		return nil, err
	}

	sum := sha256.Sum256(body)
	contentType := response.Header.Get("Content-Type")
	if contentType == "" {
		contentType = "application/octet-stream"
	}
	return &Document{ContentType: contentType, SHA256: hex.EncodeToString(sum[:]), Raw: body}, nil
}
