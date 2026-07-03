package httpapi

import (
	"encoding/base64"
	"encoding/json"
	"net/http"

	"github.com/example/go-crypto-service/internal/crypto"
	"github.com/example/go-crypto-service/internal/remote"
	"github.com/example/go-crypto-service/internal/storage"
)

type API struct {
	identity *crypto.Identity
	fetcher  *remote.Fetcher
	store    *storage.Store
}

func New(identity *crypto.Identity, fetcher *remote.Fetcher, store *storage.Store) *API {
	return &API{identity: identity, fetcher: fetcher, store: store}
}

func (a *API) sign(w http.ResponseWriter, r *http.Request) {
	var req signRequest
	if !decode(w, r, &req) {
		return
	}
	data, err := base64.StdEncoding.DecodeString(req.Data)
	if err != nil {
		writeError(w, http.StatusBadRequest, "invalid base64 data")
		return
	}
	signature, err := a.identity.Sign(data, req.Detached)
	if err != nil {
		a.store.Failure(r.Context(), "SIGN", data, err.Error())
		writeError(w, http.StatusInternalServerError, err.Error())
		return
	}
	mode := "attached"
	if req.Detached {
		mode = "detached"
	}
	a.store.Success(r.Context(), "SIGN", data, signature, mode)
	writeJSON(w, http.StatusOK, signResponse{Signature: base64.StdEncoding.EncodeToString(signature)})
}

func (a *API) verify(w http.ResponseWriter, r *http.Request) {
	var req verifyRequest
	if !decode(w, r, &req) {
		return
	}
	signature, err := base64.StdEncoding.DecodeString(req.Signature)
	if err != nil {
		writeError(w, http.StatusBadRequest, "invalid base64 signature")
		return
	}
	var data []byte
	if req.Data != "" {
		if data, err = base64.StdEncoding.DecodeString(req.Data); err != nil {
			writeError(w, http.StatusBadRequest, "invalid base64 data")
			return
		}
	}
	result, err := a.identity.Verify(signature, data, req.Detached)
	if err != nil {
		writeError(w, http.StatusBadRequest, err.Error())
		return
	}
	a.store.Success(r.Context(), "VERIFY", signature, nil, "valid="+boolText(result.Valid))
	writeJSON(w, http.StatusOK, verifyResponse{Valid: result.Valid, Signer: result.Signer})
}

func (a *API) encrypt(w http.ResponseWriter, r *http.Request) {
	var req encryptRequest
	if !decode(w, r, &req) {
		return
	}
	data, err := base64.StdEncoding.DecodeString(req.Data)
	if err != nil {
		writeError(w, http.StatusBadRequest, "invalid base64 data")
		return
	}
	envelope, err := a.identity.Encrypt(data)
	if err != nil {
		a.store.Failure(r.Context(), "ENCRYPT", data, err.Error())
		writeError(w, http.StatusInternalServerError, err.Error())
		return
	}
	a.store.Success(r.Context(), "ENCRYPT", data, envelope, "CMS EnvelopedData AES-256-CBC + RSA")
	writeJSON(w, http.StatusOK, encryptResponse{Envelope: base64.StdEncoding.EncodeToString(envelope)})
}

func (a *API) decrypt(w http.ResponseWriter, r *http.Request) {
	var req decryptRequest
	if !decode(w, r, &req) {
		return
	}
	envelope, err := base64.StdEncoding.DecodeString(req.Envelope)
	if err != nil {
		writeError(w, http.StatusBadRequest, "invalid base64 envelope")
		return
	}
	data, err := a.identity.Decrypt(envelope)
	if err != nil {
		a.store.Failure(r.Context(), "DECRYPT", envelope, err.Error())
		writeError(w, http.StatusInternalServerError, err.Error())
		return
	}
	a.store.Success(r.Context(), "DECRYPT", envelope, data, "CMS EnvelopedData")
	writeJSON(w, http.StatusOK, decryptResponse{Data: base64.StdEncoding.EncodeToString(data)})
}

func (a *API) hash(w http.ResponseWriter, r *http.Request) {
	var req hashRequest
	if !decode(w, r, &req) {
		return
	}
	data, err := base64.StdEncoding.DecodeString(req.Data)
	if err != nil {
		writeError(w, http.StatusBadRequest, "invalid base64 data")
		return
	}
	result, err := crypto.Hash(data, req.Algorithm)
	if err != nil {
		writeError(w, http.StatusBadRequest, err.Error())
		return
	}
	a.store.Success(r.Context(), "HASH", data, []byte(result.Value), result.Algorithm)
	writeJSON(w, http.StatusOK, hashResponse{Algorithm: result.Algorithm, Hash: result.Value})
}

func (a *API) fetch(w http.ResponseWriter, r *http.Request) {
	var req fetchRequest
	if !decode(w, r, &req) {
		return
	}
	document, err := a.fetcher.Fetch(req.URL)
	if err != nil {
		a.store.Failure(r.Context(), "FETCH", []byte(req.URL), err.Error())
		writeError(w, http.StatusBadRequest, err.Error())
		return
	}
	a.store.Success(r.Context(), "FETCH", []byte(req.URL), document.Raw, "sha256="+document.SHA256+"; contentType="+document.ContentType)
	writeJSON(w, http.StatusOK, fetchResponse{
		ContentType: document.ContentType,
		SHA256:      document.SHA256,
		Document:    base64.StdEncoding.EncodeToString(document.Raw),
	})
}

func decode(w http.ResponseWriter, r *http.Request, target any) bool {
	if err := json.NewDecoder(r.Body).Decode(target); err != nil {
		writeError(w, http.StatusBadRequest, "invalid JSON body")
		return false
	}
	return true
}

func writeJSON(w http.ResponseWriter, status int, body any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(body)
}

func writeError(w http.ResponseWriter, status int, message string) {
	writeJSON(w, status, errorResponse{Error: message})
}

func boolText(value bool) string {
	if value {
		return "true"
	}
	return "false"
}
