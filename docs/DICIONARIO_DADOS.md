# Dicionário de dados derivado do schema

Fonte: `back/src/config/migrations/000_full_schema.sql`. Regenerável com `python3 scripts/gerar_modelos.py`. Os trechos SQL preservam tipos, índices, restrições e ações referenciais exatamente como definidos.

## users

```sql
CREATE TABLE users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255),
  email VARCHAR(255) UNIQUE,
  password VARCHAR(255),
  role ENUM('comprador', 'vendedor') DEFAULT 'comprador',
  avatar_url TEXT,
  bio VARCHAR(500),
  last_active_at TIMESTAMP NULL DEFAULT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## products

```sql
CREATE TABLE products (
  id INT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  price DECIMAL(10, 2) NOT NULL,
  image_url TEXT,
  video_url TEXT,
  user_id INT NOT NULL,
  category VARCHAR(255),
  model VARCHAR(255),
  location VARCHAR(255),
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  INDEX idx_products_active (is_active, id),
  CONSTRAINT fk_products_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT chk_products_price CHECK (price >= 0)
);
```

## posts

```sql
CREATE TABLE posts (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  product_id INT NULL,
  video_url TEXT,
  image_url TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  description TEXT,
  likes_count INT NOT NULL DEFAULT 0,
  comments_count INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_posts_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_posts_product FOREIGN KEY (product_id) REFERENCES products(id)
    ON DELETE SET NULL,
  CONSTRAINT chk_posts_likes CHECK (likes_count >= 0),
  CONSTRAINT chk_posts_comments CHECK (comments_count >= 0)
);
```

## favorites

```sql
CREATE TABLE favorites (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  product_id INT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_favorites_user_product UNIQUE (user_id, product_id),
  CONSTRAINT fk_favorites_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_favorites_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);
```

## product_requests

```sql
CREATE TABLE product_requests (
  id INT AUTO_INCREMENT PRIMARY KEY,
  product_id INT NOT NULL,
  buyer_id INT NOT NULL,
  status ENUM('pendente', 'aceita', 'recusada') NOT NULL DEFAULT 'pendente',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_product_requests_status (product_id, status),
  CONSTRAINT fk_requests_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
  CONSTRAINT fk_requests_buyer FOREIGN KEY (buyer_id) REFERENCES users(id) ON DELETE CASCADE
);
```

## user_follows

```sql
CREATE TABLE user_follows (
  follower_id INT NOT NULL,
  followed_id INT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (follower_id, followed_id),
  INDEX idx_follows_followed (followed_id),
  CONSTRAINT fk_follow_follower FOREIGN KEY (follower_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_follow_followed FOREIGN KEY (followed_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT chk_follow_self CHECK (follower_id <> followed_id)
);
```

## blocked_users

```sql
CREATE TABLE blocked_users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  blocked_user_id INT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_blocked_users_pair UNIQUE (user_id, blocked_user_id),
  INDEX idx_blocked_users_target (blocked_user_id),
  CONSTRAINT fk_blocked_users_owner FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_blocked_users_target FOREIGN KEY (blocked_user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT chk_blocked_users_self CHECK (user_id <> blocked_user_id)
);
```
