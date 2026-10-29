import assert from 'node:assert/strict';
import { once } from 'node:events';
import { test } from 'node:test';
import express from 'express';
import jwt from 'jsonwebtoken';
import pool from '../src/config/db.js';
import settingsRoutes from '../src/routes/settingsRoutes.js';

test('configurações: JWT, isolamento, idempotência, exclusão transacional e rollback', async t => {
  const previous = process.env.JWT_SECRET;
  const secret = 'segredo-exclusivo-settings-tests';
  process.env.JWT_SECRET = secret;
  t.after(() => {
    if (previous === undefined) delete process.env.JWT_SECRET;
    else process.env.JWT_SECRET = previous;
  });
  let stored = {
    users: [7, 8, 9], blocks: [], follows: [[7, 8], [8, 7], [8, 9]],
    favorites: [{ user: 7, product: 80 }, { user: 8, product: 70 }, { user: 8, product: 80 }],
    requests: [{ buyer: 7, product: 80 }, { buyer: 8, product: 70 }, { buyer: 8, product: 80 }],
    products: [{ id: 70, user: 7 }, { id: 80, user: 8 }],
    posts: [{ id: 1, user: 7, product: 70 }, { id: 2, user: 8, product: 70 }],
  };
  let connections = 0; let nextId = 1; let failAt = null;
  const events = []; const queries = [];
  t.mock.method(console, 'error', () => {});
  t.mock.method(pool, 'getConnection', async () => {
    connections++;
    let draft;
    return {
      async beginTransaction() { events.push('begin'); draft = structuredClone(stored); },
      async commit() { events.push('commit'); stored = draft; },
      async rollback() { events.push('rollback'); },
      release() { events.push('release'); },
      async execute(sql, params) {
        queries.push({ sql, params });
        if (failAt && sql.startsWith(failAt)) throw new Error('Detalhes internos sensíveis');
        if (sql.includes('SELECT id FROM users')) {
          assert.match(sql, /FOR UPDATE/);
          return [draft.users.filter(id => params.includes(id)).map(id => ({ id }))];
        }
        if (sql.includes('INSERT INTO blocked_users')) {
          if (!draft.blocks.some(b => b.owner === params[0] && b.target === params[1]))
            draft.blocks.push({ id: nextId++, owner: params[0], target: params[1] });
          return [{ affectedRows: 1 }];
        }
        if (sql.includes('FROM blocked_users b JOIN users')) {
          assert.match(sql, /WHERE b.user_id = \?/);
          return [draft.blocks.filter(b => b.owner === params[0] && (params.length === 1 || b.target === params[1]))
            .map(b => ({ id: b.id, blocked_user_id: b.target, created_at: '2026-10-25T12:00:00Z',
              name: `Usuário ${b.target}`, role: 'comprador', avatar_url: null }))];
        }
        if (sql.startsWith('DELETE FROM blocked_users WHERE id')) {
          const before = draft.blocks.length;
          draft.blocks = draft.blocks.filter(b => !(b.id === params[0] && b.owner === params[1]));
          return [{ affectedRows: before - draft.blocks.length }];
        }
        const id = params[0];
        if (sql.startsWith('DELETE FROM blocked_users')) draft.blocks = draft.blocks.filter(b => b.owner !== id && b.target !== id);
        else if (sql.startsWith('DELETE FROM user_follows')) draft.follows = draft.follows.filter(pair => !pair.includes(id));
        else if (sql.startsWith('DELETE FROM favorites')) draft.favorites = draft.favorites.filter(f => f.user !== id);
        else if (sql.startsWith('DELETE FROM product_requests')) draft.requests = draft.requests.filter(r => r.buyer !== id);
        else if (sql.startsWith('DELETE FROM posts')) draft.posts = draft.posts.filter(p => p.user !== id);
        else if (sql.startsWith('DELETE FROM products')) {
          const deleted = draft.products.filter(p => p.user === id).map(p => p.id);
          draft.products = draft.products.filter(p => p.user !== id);
          draft.favorites = draft.favorites.filter(f => !deleted.includes(f.product));
          draft.requests = draft.requests.filter(r => !deleted.includes(r.product));
          draft.posts.forEach(p => { if (deleted.includes(p.product)) p.product = null; });
        } else if (sql.startsWith('DELETE FROM users')) draft.users = draft.users.filter(u => u !== id);
        else throw new Error(`SQL inesperado: ${sql}`);
        return [{ affectedRows: 1 }];
      },
    };
  });
  t.mock.method(pool, 'query', async (sql, params) => {
    assert.match(sql, /SELECT id FROM users WHERE id = \?/);
    return [stored.users.includes(params[0]) ? [{ id: params[0] }] : []];
  });
  const app = express(); app.use(express.json()); app.use('/api/settings', settingsRoutes);
  const server = app.listen(0, '127.0.0.1');
  t.after(() => new Promise(resolve => server.close(resolve)));
  await once(server, 'listening');
  const base = `http://127.0.0.1:${server.address().port}/api/settings`;
  const token = (id = 7, opts = {}) => jwt.sign({}, secret, { subject: String(id), expiresIn: '1h', ...opts });
  const send = (method, path, body, auth = token()) => fetch(base + path, {
    method, headers: { 'Content-Type': 'application/json', ...(auth ? { Authorization: `Bearer ${auth}` } : {}) },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
  });
  for (const auth of [null, 'invalid', token(7, { expiresIn: -1 }), token(0), token('abc'), token(7, { algorithm: 'HS384' })]) {
    for (const [method, path, body] of [['GET', '/blocked'], ['POST', '/block', { blocked_user_id: 8 }],
      ['DELETE', '/block/1'], ['DELETE', '/account']]) assert.equal((await send(method, path, body, auth)).status, 401);
  }
  assert.equal(connections, 0);
  for (const body of [{}, null, { blocked_user_id: '8' }, { blocked_user_id: -1 }, { blocked_user_id: 1.5 },
    { blocked_user_id: 2147483648 }, { blocked_user_id: 7 }]) assert.equal((await send('POST', '/block', body)).status, 400);
  for (const id of ['abc', '-1', '0', '1.5', '2147483648']) assert.equal((await send('DELETE', `/block/${id}`)).status, 400);
  assert.equal(connections, 0, 'Entrada inválida não alcança o banco');
  assert.deepEqual(await (await send('GET', '/blocked')).json(), []);
  const result = await send('POST', '/block', { blocked_user_id: 8, user_id: 9 });
  assert.equal(result.status, 200);
  const block = await result.json();
  assert.equal(block.blocked_user_id, 8); assert.equal(block.user.name, 'Usuário 8');
  assert.equal('email' in block.user, false); assert.equal('password' in block.user, false);
  assert.equal(stored.blocks[0].owner, 7, 'Identidade vem exclusivamente do JWT');
  const repeated = await (await send('POST', '/block', { blocked_user_id: 8 })).json();
  assert.equal(repeated.id, block.id); assert.equal(stored.blocks.length, 1);
  assert.equal((await (await send('GET', '/blocked')).json()).length, 1);
  assert.deepEqual(await (await send('GET', '/blocked', undefined, token(8))).json(), []);
  assert.equal((await send('DELETE', `/block/${block.id}`, undefined, token(8))).status, 404);
  assert.equal(stored.blocks.length, 1);
  assert.equal((await send('DELETE', `/block/${block.id}`)).status, 204);
  assert.equal(stored.blocks.length, 0);
  assert.equal((await send('DELETE', `/block/${block.id}`)).status, 404);
  assert.equal((await send('POST', '/block', { blocked_user_id: 99 })).status, 404);
  assert.equal((await send('GET', '/blocked', undefined, token(99))).status, 401);
  assert.equal((await send('POST', '/block', { blocked_user_id: 8 }, token(99))).status, 401);

  await send('POST', '/block', { blocked_user_id: 8 });
  await send('POST', '/block', { blocked_user_id: 7 }, token(8));
  await send('POST', '/block', { blocked_user_id: 9 }, token(8));
  const snapshot = structuredClone(stored);
  failAt = 'DELETE FROM users';
  const failure = await send('DELETE', '/account', { user_id: 8 });
  assert.equal(failure.status, 500);
  assert.doesNotMatch(await failure.text(), /sensíveis|internos/);
  assert.deepEqual(stored, snapshot, 'Falha no final restaura também os dados associados');
  assert.deepEqual(events.slice(-2), ['rollback', 'release']);
  failAt = null;
  const removed = await send('DELETE', '/account', { user_id: 8 });
  assert.equal(removed.status, 204); assert.equal(await removed.text(), '');
  assert.deepEqual(stored.users, [8, 9]);
  assert.equal(stored.blocks.length, 1); assert.equal(stored.blocks[0].owner, 8); assert.equal(stored.blocks[0].target, 9);
  assert.deepEqual(stored.follows, [[8, 9]]);
  assert.deepEqual(stored.products, [{ id: 80, user: 8 }]);
  assert.deepEqual(stored.posts, [{ id: 2, user: 8, product: null }]);
  assert.deepEqual(stored.favorites, [{ user: 8, product: 80 }]);
  assert.deepEqual(stored.requests, [{ buyer: 8, product: 80 }]);
  assert.deepEqual(events.slice(-2), ['commit', 'release']);
  assert.equal((await send('GET', '/blocked')).status, 401);
  assert.equal((await send('DELETE', '/account')).status, 401);
  assert.equal((await send('POST', '/block', { blocked_user_id: 7 }, token(8))).status, 404);
});
