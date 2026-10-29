-- Executar uma vez em bancos criados antes do módulo de vendedor.
USE skian_db;

ALTER TABLE products ADD COLUMN video_url TEXT;

CREATE TABLE IF NOT EXISTS product_requests (
  id INT AUTO_INCREMENT PRIMARY KEY,
  product_id INT NOT NULL,
  buyer_id INT NOT NULL,
  status ENUM('pendente', 'aceita', 'recusada') NOT NULL DEFAULT 'pendente',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_product_requests_status (product_id, status),
  CONSTRAINT fk_requests_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
  CONSTRAINT fk_requests_buyer FOREIGN KEY (buyer_id) REFERENCES users(id) ON DELETE CASCADE
);
