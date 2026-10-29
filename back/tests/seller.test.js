import assert from 'node:assert/strict';
import { once } from 'node:events';
import { test } from 'node:test';
import express from 'express';
import jwt from 'jsonwebtoken';
import pool from '../src/config/db.js';
import sellerRoutes from '../src/routes/sellerRoutes.js';

test('vendedor: autorização, isolamento, contadores e cadastro validado', async (t) => {
  const previous = process.env.JWT_SECRET;
  const secret = 'segredo-de-testes-do-modulo-vendedor';
  process.env.JWT_SECRET = secret;
  t.after(() => {
    if (previous === undefined) delete process.env.JWT_SECRET;
    else process.env.JWT_SECRET = previous;
  });
  const queries = [];
  const products = [{
    id: 42, title: 'Volante', price: '350.90', image_url: 'https://example.com/p.jpg', video_url: null,
    user_id: 7, category: 'Interior', model: 'Mustang', location: 'Palmas', status: 'ativo',
    requests_count: 3, pending_requests_count: '2', accepted_requests_count: '1', rejected_requests_count: '0',
  }];
  const execute = t.mock.method(pool, 'execute', async (sql, params) => {
    queries.push({ sql, params });
    if (sql.startsWith('SELECT role')) {
      return [params[0] === 7 || params[0] === 9 ? [{ role: 'vendedor' }] :
        params[0] === 8 ? [{ role: 'comprador' }] : []];
    }
    if (sql.startsWith('INSERT')) return [{ insertId: 43 }];
    assert.match(sql, /WHERE p.user_id = \?/);
    assert.match(sql, /LEFT JOIN/);
    assert.match(sql, /COUNT\(\*\)/);
    assert.match(sql, /SUM\(status = 'pendente'\)/);
    return [products.filter(product => product.user_id === params[0])];
  });
  t.mock.method(pool, 'query', async (sql, params) => {
    assert.match(sql, /SELECT id FROM users WHERE id = \?/);
    return [[7, 8, 9].includes(params[0]) ? [{ id: params[0] }] : []];
  });
  const app = express();
  app.use(express.json());
  app.use('/api/seller', sellerRoutes);
  const server = app.listen(0, '127.0.0.1');
  t.after(() => new Promise(resolve => server.close(resolve)));
  await once(server, 'listening');
  const url = `http://127.0.0.1:${server.address().port}/api/seller/products`;
  const token = id => jwt.sign({ role: 'vendedor' }, secret, { subject: String(id), expiresIn: '1h' });
  const send = (body, auth = token(7)) => fetch(url, {
    method: body === undefined ? 'GET' : 'POST',
    headers: { 'Content-Type': 'application/json', ...(auth ? { Authorization: `Bearer ${auth}` } : {}) },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
  });
  const product = { title: ' Nova peça ', price: '120.50', category: 'Interior', model: 'Mustang',
    image_url: 'https://example.com/nova.jpg', location: 'Palmas', user_id: 9 };
  assert.equal((await send(undefined, null)).status, 401);
  assert.equal((await send(product, 'invalido')).status, 401);
  assert.equal(queries.length, 0);
  for (const body of [undefined, product]) {
    assert.equal((await send(body, token(8))).status, 403, 'claim vendedor não pode substituir o papel no banco');
    assert.equal((await send(body, token(99))).status, 401);
  }
  const listed = await send();
  assert.equal(listed.status, 200);
  const [item] = await listed.json();
  assert.equal(item.requests_count, 3);
  assert.equal(item.pending_requests_count, 2);
  assert.equal(item.accepted_requests_count, 1);
  assert.equal(item.status, 'ativo');
  assert.deepEqual(await (await send(undefined, token(9))).json(), []);
  const created = await send(product);
  assert.equal(created.status, 201);
  const body = await created.json();
  assert.equal(body.id, 43);
  assert.equal(body.user_id, 7);
  assert.equal(body.title, 'Nova peça');
  assert.equal(body.price, '120.50');
  assert.equal(body.requests_count, 0);
  assert.equal(body.status, 'ativo');
  assert.match(queries.at(-1).sql, /VALUES \(\?, \?, \?, \?, \?, \?, \?, \?, TRUE\)/);
  assert.deepEqual(queries.at(-1).params,
    ['Nova peça', '120.50', 'Interior', 'Mustang', 'https://example.com/nova.jpg', null, 'Palmas', 7]);
  const video = await send({ ...product, image_url: null, video_url: 'https://example.com/video.mp4' });
  assert.equal(video.status, 201);
  assert.equal((await video.json()).video_url, 'https://example.com/video.mp4');

  for (const fields of [{ title: '' }, { category: '' }, { model: '' }, { price: 0 }, { price: -1 },
    { price: '1.234' }, { price: '100000000' }, { image_url: 'file:///tmp/media' },
    { image_url: null, video_url: null }, { image_url: 'https://user:pass@example.com/a.jpg' }]) {
    assert.equal((await send({ ...product, ...fields })).status, 400);
    assert.equal(queries.at(-1).sql.startsWith('SELECT role'), true, 'dados inválidos não devem chegar ao INSERT');
  }
  t.mock.method(console, 'error', () => {});
  execute.mock.mockImplementation(async sql => {
    if (sql.startsWith('SELECT role')) return [[{ role: 'vendedor' }]];
    throw new Error('Detalhes internos sensíveis');
  });
  for (const body of [undefined, product]) {
    const failure = await send(body);
    assert.equal(failure.status, 500);
    assert.doesNotMatch(await failure.text(), /sensíveis|internos/);
  }
});
