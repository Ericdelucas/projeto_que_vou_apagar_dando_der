import bcrypt from 'bcrypt';
import pool from '../config/db.js';

export async function createUser({ name, email, password, role = 'comprador' }) {
  // O hash acontece somente aqui; a senha original nunca é persistida.
  const passwordHash = await bcrypt.hash(password, 12);
  const [result] = await pool.execute(
    'INSERT INTO users (name, email, password, role) VALUES (?, ?, ?, ?)',
    [name, email, passwordHash, role],
  );

  return { id: result.insertId, name, email, role };
}

export async function findByEmail(email) {
  const [rows] = await pool.execute(
    'SELECT id, name, email, password, role FROM users WHERE email = ? LIMIT 1',
    [email],
  );
  return rows[0] ?? null;
}

export async function touchLastActive(userId) {
  await pool.execute('UPDATE users SET last_active_at = CURRENT_TIMESTAMP WHERE id = ?', [userId]);
}
