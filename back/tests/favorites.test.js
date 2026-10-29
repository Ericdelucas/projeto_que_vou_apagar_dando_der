import assert from 'node:assert/strict';
import { once } from 'node:events';
import { test } from 'node:test';
import express from 'express';
import jwt from 'jsonwebtoken';
import pool from '../src/config/db.js';
import favoritesRoutes from '../src/routes/favoritesRoutes.js';

test('favoritos autenticados: isolamento, salvar, remover, idempotência e rollback', async (t) => {
  const originalSecret = process.env.JWT_SECRET;
  const secret = 'segredo-exclusivo-para-testes-de-favoritos';
  process.env.JWT_SECRET = secret;
  t.after(() => {
    if (originalSecret === undefined) delete process.env.JWT_SECRET;
    else process.env.JWT_SECRET = originalSecret;
  });
  let stored = new Map();
  let nextId = 1;
  let failInsert = false;
  const events = [];
  const queries = [];
  t.mock.method(pool, 'getConnection', async () => {
    let draft;
    return {
      async beginTransaction() { events.push('begin'); draft = new Map(stored); },
      async commit() { events.push('commit'); stored = draft; },
      async rollback() { events.push('rollback'); },
      release() { events.push('release'); },
      async execute(sql, params) {
        queries.push({ sql, params });
        if (sql.includes('FROM users')) {
          assert.match(sql, /FOR UPDATE/);
          events.push('user-lock');
          return [[7, 8].includes(params[0]) ? [{ id: params[0] }] : []];
        }
        if (sql.includes('SELECT id FROM favorites')) {
          const item = draft.get(params.join(':'));
          return [item ? [item] : []];
        }
        if (sql.includes('FROM products')) {
          assert.match(sql, /is_active = TRUE FOR UPDATE/);
          return [params[0] === 42 ? [{ id: 42 }] : []];
        }
        if (sql.startsWith('INSERT')) {
          if (failInsert) throw new Error('Detalhes internos sensíveis');
          draft.set(params.join(':'), { id: nextId++, userId: params[0], productId: params[1] });
          return [{ affectedRows: 1 }];
        }
        if (sql.startsWith('DELETE')) {
          draft.delete(params.join(':'));
          return [{ affectedRows: 1 }];
        }
        throw new Error(`Consulta inesperada: ${sql}`);
      },
    };
  });
  t.mock.method(pool, 'execute', async (sql, params) => {
    queries.push({ sql, params });
    assert.match(sql, /WHERE f.user_id = \?/);
    return [[...stored.values()].filter(item => item.userId === params[0]).map(item => ({
      favorite_id: item.id, product_id: item.productId, created_at: '2026-10-25T12:00:00Z',
      title: 'Volante', price: '350.90', image_url: 'https://example.com/p.jpg', user_id: 9,
      category: 'Interior', model: 'Mustang', location: 'Palmas', author_name: 'Vendedor',
    }))];
  });

  t.mock.method(pool, 'query', async (sql, params) => {
    assert.match(sql, /SELECT id FROM users WHERE id = \?/);
    return [[7, 8].includes(params[0]) ? [{ id: params[0] }] : []];
  });
  const app = express();
  app.use(express.json());
  app.use('/api/favorites', favoritesRoutes);
  const server = app.listen(0, '127.0.0.1');
  t.after(() => new Promise(resolve => server.close(resolve)));
  await once(server, 'listening');
  const url = `http://127.0.0.1:${server.address().port}/api/favorites`;
  const token = (id, options = {}) => jwt.sign({}, secret, { subject: String(id), expiresIn: '1h', ...options });
  const request = (body, auth = token(7)) => fetch(url, {
    method: body === undefined ? 'GET' : 'POST',
    headers: { 'Content-Type': 'application/json', ...(auth ? { Authorization: `Bearer ${auth}` } : {}) },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
  });

  for (const auth of [null, 'invalido', token(7, { expiresIn: -1 }), token('abc'),
    token(0), token(7, { algorithm: 'HS384' }), jwt.sign({}, 'outro-segredo', { subject: '7' })]) {
    assert.equal((await request(undefined, auth)).status, 401);
    assert.equal((await request({ product_id: 42 }, auth)).status, 401);
  }
  assert.equal(queries.length, 0, 'JWT inválido não deve alcançar o banco');

  const saved = await request({ product_id: 42, user_id: 8, is_favorite: true });
  assert.equal(saved.status, 200);
  assert.deepEqual(await saved.json(), { product_id: 42, is_favorite: true });
  assert.equal(stored.has('7:42'), true);
  assert.equal(stored.has('8:42'), false, 'o usuário vem do JWT, não do corpo');
  assert.deepEqual(events.slice(0, 4), ['begin', 'user-lock', 'commit', 'release']);
  await request({ product_id: 42, is_favorite: true });
  assert.equal(stored.size, 1, 'repetir o estado desejado não deve duplicar ou remover');

  const list = await (await request()).json();
  assert.equal(list.length, 1);
  assert.equal(list[0].product.id, 42);
  assert.equal(list[0].product.price, '350.90');
  assert.deepEqual(list[0].author, { id: 9, name: 'Vendedor' });
  assert.deepEqual(await (await request(undefined, token(8))).json(), []);
  assert.deepEqual(await (await request({ product_id: 42 })).json(), { product_id: 42, is_favorite: false });
  assert.equal(stored.size, 0);
  assert.deepEqual(await (await request({ product_id: 42 })).json(), { product_id: 42, is_favorite: true });
  await request({ product_id: 42, is_favorite: false });
  await request({ product_id: 42, is_favorite: false });
  assert.equal(stored.size, 0);

  const queryCount = queries.length;
  for (const body of [{}, { product_id: '42' }, { product_id: 0 }, { product_id: 1.5 },
    { product_id: 2147483648 }, { product_id: 42, is_favorite: null }]) {
    assert.equal((await request(body)).status, 400);
  }
  assert.equal(queries.length, queryCount);
  assert.equal((await request({ product_id: 99 })).status, 404);
  assert.deepEqual(events.slice(-2), ['rollback', 'release']);
  assert.equal((await request({ product_id: 42 }, token(99))).status, 401);
  assert.deepEqual(events.slice(-2), ['rollback', 'release']);

  failInsert = true;
  t.mock.method(console, 'error', () => {});
  const failed = await request({ product_id: 42 });
  assert.equal(failed.status, 500);
  assert.doesNotMatch(await failed.text(), /sensíveis|internos/);
  assert.equal(stored.size, 0);
  assert.deepEqual(events.slice(-2), ['rollback', 'release']);
});
