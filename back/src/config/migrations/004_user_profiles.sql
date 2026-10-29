-- Executar uma vez em bancos com as migrações anteriores aplicadas.
USE skian_db;
ALTER TABLE users
  ADD COLUMN avatar_url TEXT,
  ADD COLUMN bio VARCHAR(500),
  ADD COLUMN last_active_at TIMESTAMP NULL DEFAULT NULL;

CREATE TABLE IF NOT EXISTS user_follows (
  follower_id INT NOT NULL,
  followed_id INT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (follower_id, followed_id),
  INDEX idx_follows_followed (followed_id),
  CONSTRAINT fk_follow_follower FOREIGN KEY (follower_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_follow_followed FOREIGN KEY (followed_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT chk_follow_self CHECK (follower_id <> followed_id)
);
