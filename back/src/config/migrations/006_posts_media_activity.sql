-- Aplicar uma vez em bancos MySQL existentes antes de iniciar a API atualizada.
ALTER TABLE posts ADD COLUMN image_url TEXT,
  ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
