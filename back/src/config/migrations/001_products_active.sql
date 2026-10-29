-- Executar uma vez em bancos que já possuíam a tabela products antes do Marketplace.
USE skian_db;

ALTER TABLE products
  ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE,
  ADD INDEX idx_products_active (is_active, id);
