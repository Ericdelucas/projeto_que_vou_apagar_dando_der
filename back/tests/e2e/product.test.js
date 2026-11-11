import assert from 'node:assert/strict';
import { once } from 'node:events';
import { readFile } from 'node:fs/promises';
import { test } from 'node:test';
import mysql from 'mysql2/promise';
import app from '../../src/app.js';
import pool from '../../src/config/db.js';
import { demoImages } from '../../src/config/demoMedia.js';

// Nunca executar a suíte destrutiva contra o banco da aplicação.
const database = process.env.DB_NAME || '';
if (process.env.SKIPPECAS_E2E !== '1' || !/^skippecas_e2e_[a-z0-9_]+$/.test(database)) {
  throw new Error('E2E exige SKIPPECAS_E2E=1 e DB_NAME=skippecas_e2e_<identificador>; usa e remove apenas esse banco isolado.');
}

test('MVP real: schema → autenticação → vendas/feed/marketplace → favoritos/perfis → bloqueios → exclusão e revogação', async t => {
  const admin = await mysql.createConnection({
    host: process.env.DB_HOST || '127.0.0.1', port: Number(process.env.DB_PORT || 3306),
    user: process.env.DB_USER || 'root', password: process.env.DB_PASSWORD ?? '',
    multipleStatements: true,
  });
  let server; let created = false;
  t.after(async () => {
    if (server) await new Promise(resolve => server.close(resolve));
    await pool.end();
    if (created) await admin.query(`DROP DATABASE \`${database}\``);
    await admin.end();
  });
  const [existing] = await admin.execute('SELECT SCHEMA_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = ?', [database]);
  assert.equal(existing.length, 0, 'O banco E2E já existe; recuse sobrescrever qualquer dado.');
  const schema = (await readFile(new URL('../../src/config/migrations/000_full_schema.sql', import.meta.url), 'utf8'))
    .replaceAll('skian_db', database);
  await admin.query(schema); created = true;
  await admin.query(schema); // Bootstrap repetível.
  const incremental = (await readFile(new URL('../../src/config/migrations/005_settings_and_blocks.sql', import.meta.url), 'utf8'))
    .replaceAll('skian_db', database);
  await admin.query(incremental); await admin.query(incremental);
  server = app.listen(0, '127.0.0.1'); await once(server, 'listening');
  const base = `http://127.0.0.1:${server.address().port}`;
  const request = (method, path, token, body) => fetch(base + path, {
    method, signal: AbortSignal.timeout(15000),
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
  });
  const json = async (response, expected = 200) => {
    const text = await response.text(); assert.equal(response.status, expected, text);
    return text ? JSON.parse(text) : null;
  };
  const health = await json(await request('GET', '/health'));
  assert.equal(health.status, 'ok'); assert.equal(Object.keys(health.modules).length, 7);

  const account = async (name, role) => {
    const credentials = { name, email: `${name.toLowerCase()}@example.com`, password: 'teste-seguro-123', role };
    const registered = await json(await request('POST', '/api/auth/register', null, credentials), 201);
    const login = await json(await request('POST', '/api/auth/login', null, credentials));
    assert.equal(login.user.id, registered.user.id); assert.ok(login.token);
    return { ...login.user, token: login.token, credentials };
  };
  const seller = await account('Vendedor', 'vendedor');
  const buyer = await account('Comprador', 'comprador');
  const other = await account('Outro', 'vendedor');
  const product = async owner => json(await request('POST', '/api/seller/products', owner.token, {
    title: 'Volante Mustang', price: '350.90', category: 'Interior', model: 'Mustang',
    image_url: demoImages.steeringWheel, location: 'Palmas',
  }), 201);
  const first = await product(seller); const second = await product(other);
  const forbidden = await request('POST', '/api/seller/products', buyer.token, {});
  assert.equal(forbidden.status, 403);
  const mine = await json(await request('GET', '/api/seller/products', seller.token));
  assert.deepEqual(mine.map(p => p.id), [first.id]);
  const filtered = await json(await request('GET', '/api/marketplace?model=Mustang&category=Interior&min_price=300&max_price=400&location=Palmas'));
  assert.equal(filtered.length, 2);
  assert.deepEqual(await json(await request('GET', '/api/marketplace?min_price=500')), []);
  const filters = await json(await request('GET', '/api/marketplace/filters'));
  assert.deepEqual(filters.models, ['Mustang']);
  await pool.execute('INSERT INTO posts (user_id, product_id, description) VALUES (?, ?, ?), (?, ?, ?)',
    [seller.id, first.id, 'Publicação própria', buyer.id, first.id, 'Publicação de terceiro']);
  const feed = await json(await request('GET', '/api/feed')); assert.equal(feed.length, 2);
  assert.ok(feed.every(p => p.product.id === first.id));
  assert.equal((await json(await request('GET', `/api/products/${first.id}`))).id, first.id);
  const save = async (who, productId) => json(await request('POST', '/api/favorites', who.token,
    { product_id: productId, is_favorite: true }));
  await save(buyer, first.id); await save(buyer, first.id); await save(buyer, second.id); await save(seller, second.id);
  assert.equal((await json(await request('GET', '/api/favorites', buyer.token))).length, 2);
  const profile = await json(await request('GET', '/api/users/me', seller.token));
  assert.equal(profile.posts_count, 1); assert.equal(profile.is_owner, true);
  const updated = await json(await request('PUT', '/api/users/profile', seller.token, { bio: 'Loja de peças' }));
  assert.equal(updated.bio, 'Loja de peças');
  const search = await json(await request('GET', '/api/users/search?q=Vendedor&filter=vendedor', buyer.token));
  assert.deepEqual(search.map(u => u.id), [seller.id]);
  assert.equal('email' in search[0], false);
  assert.equal('email' in await json(await request('GET', `/api/users/profile/${seller.id}`, buyer.token)), false);

  const block = async (who, id) => json(await request('POST', '/api/settings/block', who.token, { blocked_user_id: id }));
  const [blocked, repeat] = await Promise.all([block(seller, buyer.id), block(seller, buyer.id)]);
  assert.equal(blocked.id, repeat.id, 'Bloqueios concorrentes são idempotentes');
  assert.equal((await json(await request('GET', '/api/settings/blocked', seller.token))).length, 1);
  assert.deepEqual(await json(await request('GET', '/api/settings/blocked', buyer.token)), []);
  assert.equal((await request('DELETE', `/api/settings/block/${blocked.id}`, buyer.token)).status, 404);
  await json(await request('DELETE', `/api/settings/block/${blocked.id}`, seller.token), 204);
  assert.equal((await request('POST', '/api/settings/block', seller.token, { blocked_user_id: seller.id })).status, 400);
  assert.equal((await request('POST', '/api/settings/block', seller.token, { blocked_user_id: 2147483647 })).status, 404);
  await assert.rejects(pool.execute('INSERT INTO blocked_users (user_id, blocked_user_id) VALUES (?, ?)', [seller.id, seller.id]));
  await assert.rejects(pool.execute('INSERT INTO blocked_users (user_id, blocked_user_id) VALUES (?, ?)', [seller.id, 2147483647]));
  await block(seller, buyer.id); await block(other, seller.id); await block(buyer, other.id);
  await pool.execute('INSERT INTO user_follows (follower_id, followed_id) VALUES (?, ?), (?, ?), (?, ?)',
    [seller.id, buyer.id, buyer.id, seller.id, buyer.id, other.id]);
  await pool.execute('INSERT INTO product_requests (product_id, buyer_id) VALUES (?, ?), (?, ?), (?, ?)',
    [second.id, seller.id, first.id, buyer.id, second.id, buyer.id]);
  const counts = async () => {
    const result = {};
    for (const table of ['users', 'posts', 'products', 'favorites', 'product_requests', 'user_follows', 'blocked_users']) {
      const [[row]] = await pool.query(`SELECT COUNT(*) AS total FROM ${table}`); result[table] = row.total;
    }
    return result;
  };
  const before = await counts();
  await pool.query(`CREATE TRIGGER e2e_refuse_deletion BEFORE DELETE ON users FOR EACH ROW
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'falha de teste'`);
  assert.equal((await request('DELETE', '/api/settings/account', seller.token)).status, 500);
  assert.deepEqual(await counts(), before, 'Rollback restaura todas as tabelas após falha real do banco');
  assert.equal((await request('GET', '/api/users/me', seller.token)).status, 200);
  await pool.query('DROP TRIGGER e2e_refuse_deletion');
  await json(await request('DELETE', '/api/settings/account', seller.token, { user_id: other.id }), 204);
  assert.deepEqual(await counts(), { users: 2, posts: 1, products: 1, favorites: 1,
    product_requests: 1, user_follows: 1, blocked_users: 1 });
  const [[remainingPost]] = await pool.query('SELECT user_id, product_id FROM posts');
  assert.equal(remainingPost.user_id, buyer.id); assert.equal(remainingPost.product_id, null);
  for (const path of ['/api/users/me', '/api/users/search', '/api/favorites', '/api/seller/products', '/api/settings/blocked']) {
    assert.equal((await request('GET', path, seller.token)).status, 401, `${path}: token de conta excluída`);
  }
  assert.equal((await request('DELETE', '/api/settings/account', seller.token)).status, 401);
  assert.equal((await request('POST', '/api/auth/login', null, seller.credentials)).status, 401);
  assert.equal((await request('GET', '/api/users/me', other.token)).status, 200);
  assert.deepEqual((await json(await request('GET', '/api/favorites', buyer.token))).map(f => f.product_id), [second.id]);
  assert.equal((await json(await request('GET', '/health'))).status, 'ok');
});
