import assert from 'node:assert/strict';
import { once } from 'node:events';
import { test } from 'node:test';
import express from 'express';
import pool from '../src/config/db.js';
import feedRoutes from '../src/routes/feedRoutes.js';

test('feed HTTP, produto vinculado, ausência de tag e falhas', async (t) => {
  const product = {
    id: 42, title: 'Volante Mustang', price: '350.90', image_url: 'https://example.com/volante.jpg',
    user_id: 7, category: 'Interior', model: 'Mustang', location: 'Palmas - TO',
  };
  const rows = [{
    id: 91, user_id: 7, user_name: 'Ana', product_id: 42,
    video_url: null, description: 'Peça em ótimo estado', likes_count: 12, comments_count: 3,
    linked_product_id: 42, title: product.title, price: product.price,
    image_url: product.image_url, seller_id: 7, category: product.category,
    model: product.model, location: product.location,
  }, {
    id: 90, user_id: 7, user_name: 'Ana', product_id: null, linked_product_id: null,
    video_url: 'https://example.com/video.mp4', description: 'Meu projeto',
    likes_count: 0, comments_count: 0,
  }];
  const statements = [];
  const execute = t.mock.method(pool, 'execute', async (sql, params) => {
    statements.push({ sql, params });
    return sql.includes('FROM posts') ? [rows] : [[product]];
  });
  const app = express();
  app.use('/api', feedRoutes);
  const server = app.listen(0, '127.0.0.1');
  t.after(() => new Promise(resolve => server.close(resolve)));
  await once(server, 'listening');
  const base = `http://127.0.0.1:${server.address().port}/api`;

  const response = await fetch(`${base}/feed`);
  assert.equal(response.status, 200);
  const posts = await response.json();
  assert.equal(posts.length, 2);
  assert.deepEqual(posts[0].product, product);
  assert.deepEqual(posts[0].user, { id: 7, name: 'Ana' });
  assert.equal(posts[0].id, 91);
  assert.equal(posts[0].product_id, 42);
  assert.equal(posts[0].likes_count, 12);
  assert.equal(posts[1].product, null);
  assert.match(statements[0].sql, /LEFT JOIN products/);
  assert.match(statements[0].sql, /ORDER BY p.id DESC/);

  const details = await fetch(`${base}/products/42`);
  assert.equal(details.status, 200);
  assert.deepEqual(await details.json(), product);
  assert.match(statements[1].sql, /WHERE id = \?/);
  assert.deepEqual(statements[1].params, [42]);
  const queryCount = statements.length;
  for (const invalidId of ['abc', '0', '-1', '1.5', '2147483648']) {
    assert.equal((await fetch(`${base}/products/${invalidId}`)).status, 400);
  }
  assert.equal(statements.length, queryCount);

  execute.mock.mockImplementation(async () => [[]]);
  assert.deepEqual(await (await fetch(`${base}/feed`)).json(), []);
  assert.equal((await fetch(`${base}/products/42`)).status, 404);

  t.mock.method(console, 'error', () => {});
  execute.mock.mockImplementation(async () => {
    throw Object.assign(new Error('Credenciais e detalhes internos'), { code: 'ECONNREFUSED' });
  });
  for (const path of ['/feed', '/products/42']) {
    const failure = await fetch(`${base}${path}`);
    assert.equal(failure.status, 500);
    assert.doesNotMatch(await failure.text(), /Credenciais|ECONNREFUSED/);
  }
});
