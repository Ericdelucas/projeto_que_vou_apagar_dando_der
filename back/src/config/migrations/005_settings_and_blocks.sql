-- Aplicar após 004_user_profiles.sql. Também presente no schema de instalações novas.
USE skian_db;

CREATE TABLE IF NOT EXISTS blocked_users (
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
