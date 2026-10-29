import assert from 'node:assert/strict';
import { test } from 'node:test';
import { setImmediate } from 'node:timers/promises';
import { createMemoryDatabase } from '../src/config/memoryDatabase.js';

test('SQLite: transações isolam consultas concorrentes, rollback, constraints e cascatas', async () => {
  const db = await createMemoryDatabase({ seed: false });
  try {
    const [{ insertId: first }] = await db.execute("INSERT INTO users (name, email) VALUES (?, ?)", ['Ana', 'ana@example.com']);
    const [{ insertId: second }] = await db.execute("INSERT INTO users (name, email) VALUES (?, ?)", ['Bruno', 'bruno@example.com']);
    await assert.rejects(db.execute('INSERT INTO users (email) VALUES (?)', ['ANA@example.com']), { code: 'ER_DUP_ENTRY' });
    await assert.rejects(db.execute('INSERT INTO blocked_users (user_id, blocked_user_id) VALUES (?, ?)', [first, first]));
    await assert.rejects(db.execute('INSERT INTO blocked_users (user_id, blocked_user_id) VALUES (?, ?)', [first, 999]));
    const connection = await db.getConnection();
    await connection.beginTransaction();
    await connection.execute('UPDATE users SET name = ? WHERE id = ?', ['Temporário', first]);
    let completed = false;
    const pending = db.query('SELECT name FROM users WHERE id = ?', [first]).then(result => { completed = true; return result; });
    await setImmediate();
    assert.equal(completed, false, 'Outra requisição espera a transação terminar');
    await connection.rollback(); connection.release();
    assert.equal((await pending)[0][0].name, 'Ana');
    const [{ insertId: product }] = await db.execute('INSERT INTO products (title, price, user_id) VALUES (?, ?, ?)', ['Peça', '99.90', first]);
    await db.execute('INSERT INTO favorites (user_id, product_id) VALUES (?, ?)', [second, product]);
    await db.execute('INSERT INTO posts (user_id, product_id) VALUES (?, ?)', [second, product]);
    await db.execute('DELETE FROM products WHERE user_id = ?', [first]);
    assert.deepEqual((await db.query('SELECT id FROM favorites'))[0], []);
    assert.equal((await db.query('SELECT product_id FROM posts'))[0][0].product_id, null);
  } finally { await db.end(); }
});
