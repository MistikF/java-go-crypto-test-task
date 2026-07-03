package httpapi

import (
	"net/http"

	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"
)

func (a *API) Router() http.Handler {
	router := chi.NewRouter()
	router.Use(middleware.RequestID)
	router.Use(middleware.Recoverer)

	router.Route("/api/v1", func(r chi.Router) {
		r.Post("/signatures/sign", a.sign)
		r.Post("/signatures/verify", a.verify)
		r.Post("/encryption/encrypt", a.encrypt)
		r.Post("/encryption/decrypt", a.decrypt)
		r.Post("/hash", a.hash)
		r.Post("/documents/fetch", a.fetch)
	})

	return router
}
