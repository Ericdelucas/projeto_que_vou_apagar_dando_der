PRAGMA foreign_keys = ON;
CREATE TABLE users (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT, email TEXT UNIQUE COLLATE NOCASE, password TEXT,
  role TEXT NOT NULL DEFAULT 'comprador' CHECK (role IN ('comprador', 'vendedor')),
  avatar_url TEXT, bio TEXT, last_active_at TEXT,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE products (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  title TEXT NOT NULL, price TEXT NOT NULL CHECK (CAST(price AS REAL) >= 0),
  image_url TEXT, video_url TEXT, user_id INTEGER NOT NULL REFERENCES users(id),
  category TEXT, model TEXT, location TEXT, is_active INTEGER NOT NULL DEFAULT 1
);
CREATE INDEX idx_products_active ON products(is_active, id);
CREATE TABLE posts (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id INTEGER NOT NULL REFERENCES users(id),
  product_id INTEGER REFERENCES products(id) ON DELETE SET NULL,
  video_url TEXT, image_url TEXT, created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, description TEXT,
  likes_count INTEGER NOT NULL DEFAULT 0 CHECK (likes_count >= 0),
  comments_count INTEGER NOT NULL DEFAULT 0 CHECK (comments_count >= 0)
);
CREATE TABLE favorites (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  product_id INTEGER NOT NULL REFERENCES products(id) ON DELETE CASCADE,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(user_id, product_id)
);
CREATE TABLE product_requests (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  product_id INTEGER NOT NULL REFERENCES products(id) ON DELETE CASCADE,
  buyer_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  status TEXT NOT NULL DEFAULT 'pendente' CHECK (status IN ('pendente', 'aceita', 'recusada')),
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_requests_status ON product_requests(product_id, status);
CREATE TABLE user_follows (
  follower_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  followed_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(follower_id, followed_id), CHECK(follower_id <> followed_id)
);
CREATE INDEX idx_follows_followed ON user_follows(followed_id);
CREATE TABLE blocked_users (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  blocked_user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(user_id, blocked_user_id), CHECK(user_id <> blocked_user_id)
);
CREATE INDEX idx_blocked_target ON blocked_users(blocked_user_id);
