package storage

import (
	"context"
	"database/sql"

	_ "github.com/jackc/pgx/v5/stdlib"
)

const serviceName = "go"

type Store struct {
	db *sql.DB
}

func Open(dsn string) (*Store, error) {
	db, err := sql.Open("pgx", dsn)
	if err != nil {
		return nil, err
	}
	if err := db.Ping(); err != nil {
		return nil, err
	}
	return &Store{db: db}, nil
}

func (s *Store) Close() error {
	return s.db.Close()
}

func (s *Store) Success(ctx context.Context, operation string, input, output []byte, detail string) error {
	return s.save(ctx, operation, input, output, "OK", detail)
}

func (s *Store) Failure(ctx context.Context, operation string, input []byte, detail string) error {
	return s.save(ctx, operation, input, nil, "ERROR", detail)
}

func (s *Store) save(ctx context.Context, operation string, input, output []byte, status, detail string) error {
	_, err := s.db.ExecContext(ctx,
		`INSERT INTO crypto_operation (service, operation, input, output, status, detail)
		 VALUES ($1, $2, $3, $4, $5, $6)`,
		serviceName, operation, input, output, status, detail)
	return err
}
