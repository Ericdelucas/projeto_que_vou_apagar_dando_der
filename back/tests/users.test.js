import assert from 'node:assert/strict';
import { once } from 'node:events';
import { test } from 'node:test';
import express from 'express';
import jwt from 'jsonwebtoken';
import pool from '../src/config/db.js';
import userRoutes from '../src/routes/userRoutes.js';

test('usuários: busca parametrizada, perfil público/privado, métricas e edição própria', async (t) => {
  const previous = process.env.JWT_SECRET;
  const secret = 'segredo-para-testes-de-perfil';
  process.env.JWT_SECRET = secret;
  t.after(() => {
    if (previous === undefined) delete process.env.JWT_SECRET;
    else process.env.JWT_SECRET = previous;
  });
  const users = new Map([7, 8].map(id => [id, {
    id, name: `Usuário ${id}`, email: `u${id}@example.com`, role: 'comprador',
    avatar_url: null, bio: null, last_active_at: null,
    posts_count: '2', followers_count: '3', following_count: '4',
  }]));
  let duplicate = false;
  const queries = [];
  const execute = t.mock.method(pool, 'execute', async (sql, params) => {
    queries.push({ sql, params });
    if (sql.includes('LIKE')) return [[{ id: 7, name: 'Usuário 7', role: 'comprador', avatar_url: null,
      last_active_at: null, activity_status: 'inativo' }]];
    if (sql.includes('FROM users u')) {
      assert.match(sql, /COUNT\(\*\) FROM user_follows WHERE followed_id = u.id/);
      return [users.has(params[0]) ? [users.get(params[0])] : []];
    }
    if (sql.includes('UNION ALL')) {
      assert.match(sql, /NOT EXISTS/);
      assert.deepEqual(params, [params[0], params[0], params[0]]);
      return [[{ media_id: 'post:1', kind: 'post', sort_id: 1, post_id: 1, product_id: 42,
        title: 'Volante', image_url: 'https://example.com/p.jpg', video_url: 'https://example.com/p.mp4' }]];
    }
    if (sql.startsWith('UPDATE')) {
      if (duplicate) throw Object.assign(new Error('Duplicate'), { code: 'ER_DUP_ENTRY' });
      const user = users.get(params.at(-1));
      if (!user) return [{ affectedRows: 0 }];
      const fields = sql.slice(sql.indexOf('SET ') + 4, sql.indexOf(', last_active_at')).split(', ');
      fields.forEach((field, index) => { user[field.split(' = ')[0]] = params[index]; });
      return [{ affectedRows: 1 }];
    }
    if (sql.startsWith('SELECT id')) return [users.has(params[0]) ? [{ id: params[0] }] : []];
    throw new Error(`Consulta inesperada: ${sql}`);
  });
  t.mock.method(pool, 'query', async (sql, params) => {
    assert.match(sql, /SELECT id FROM users WHERE id = \?/);
    return [users.has(params[0]) ? [{ id: params[0] }] : []];
  });
  const app = express(); app.use(express.json()); app.use('/api/users', userRoutes);
  const server = app.listen(0, '127.0.0.1');
  t.after(() => new Promise(resolve => server.close(resolve)));
  await once(server, 'listening');
  const base = `http://127.0.0.1:${server.address().port}/api/users`;
  const token = jwt.sign({}, secret, { subject: '7', expiresIn: '1h' });
  const send = (path, body, auth = token) => fetch(`${base}${path}`, {
    method: body === undefined ? 'GET' : 'PUT',
    headers: { 'Content-Type': 'application/json', ...(auth ? { Authorization: `Bearer ${auth}` } : {}) },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
  });
  for (const path of ['/search', '/me', '/profile/8']) assert.equal((await send(path, undefined, null)).status, 401);
  assert.equal((await send('/profile', { name: 'Ana' }, null)).status, 401);
  assert.equal(queries.length, 0);
  const result = await send(`/search?${new URLSearchParams({ q: 'Ana%_!', filter: 'vendedor' })}`);
  assert.equal(result.status, 200);
  assert.deepEqual(queries.at(-1).params, ['%Ana!%!_!!%', '%Ana!%!_!!%', 'vendedor']);
  assert.match(queries.at(-1).sql, /role = \?/);
  assert.doesNotMatch(await result.text(), /email|password/);
  for (const path of ['/search?q=a&q=b', '/search?filter=admin', '/profile/abc', '/profile/-1', '/profile/2147483648']) {
    assert.equal((await send(path)).status, 400);
  }
  const own = await (await send('/me')).json();
  assert.equal(own.email, 'u7@example.com');
  assert.equal(own.is_owner, true);
  assert.equal(own.posts_count, 2);
  assert.equal(own.followers_count, 3);
  assert.equal(own.following_count, 4);
  assert.equal(own.media[0].sort_id, undefined);
  const other = await (await send('/profile/8')).json();
  assert.equal(other.is_owner, false);
  assert.equal('email' in other, false);
  assert.equal('password' in other, false);
  assert.equal((await send('/profile/99')).status, 404);
  for (const body of [{}, { user_id: 8, name: 'Ana' }, { role: 'vendedor' }, { password: 'teste' },
    { name: '' }, { email: 'invalido' }, { bio: 'x'.repeat(501) }, { avatar_url: 'file:///tmp/avatar' }]) {
    assert.equal((await send('/profile', body)).status, 400);
  }
  const updated = await send('/profile', { name: ' Ana ', email: ' ANA@example.com ', bio: '', avatar_url: '' });
  assert.equal(updated.status, 200);
  const saved = await updated.json();
  assert.equal(saved.name, 'Ana'); assert.equal(saved.email, 'ana@example.com');
  assert.equal(saved.avatar_url, null); assert.equal(saved.bio, null);
  const update = queries.findLast(item => item.sql.startsWith('UPDATE'));
  assert.match(update.sql, /WHERE id = \?/);
  assert.equal(update.params.at(-1), 7);
  assert.equal(users.get(8).name, 'Usuário 8');
  duplicate = true;
  assert.equal((await send('/profile', { email: 'existente@example.com' })).status, 409);
  t.mock.method(console, 'error', () => {});
  execute.mock.mockImplementation(async () => { throw new Error('Detalhes internos sensíveis'); });
  for (const [path, body] of [['/search', undefined], ['/me', undefined], ['/profile', { name: 'Ana' }]]) {
    const failure = await send(path, body);
    assert.equal(failure.status, 500);
    assert.doesNotMatch(await failure.text(), /internos|sensíveis/);
  }
});
