import assert from 'node:assert/strict';
import { once } from 'node:events';
import { test } from 'node:test';
import express from 'express';
import pool from '../src/config/db.js';
import marketplaceRoutes from '../src/routes/marketplaceRoutes.js';

test('Marketplace: filtros combinados, preços, opções e erros HTTP', async (t) => {
  const product = {
    id: 42, title: 'Volante Mustang', price: '350.90', image_url: 'https://example.com/p.jpg',
    user_id: 7, model: 'Mustang', category: 'Interior', location: 'Palmas - TO',
  };
  const statements = [];
  const execute = t.mock.method(pool, 'execute', async (sql, params) => {
    statements.push({ sql, params });
    if (sql.includes('SELECT DISTINCT')) return [[
      { model: 'Mustang', category: 'Interior', location: 'Palmas - TO' },
      { model: 'Mustang', category: 'Rodas', location: 'São Paulo' },
      { model: 'Fiesta', category: 'Interior', location: null },
      { model: null, category: '', location: ' ' },
    ]];
    return [[product]];
  });
  const app = express();
  app.use('/api/marketplace', marketplaceRoutes);
  const server = app.listen(0, '127.0.0.1');
  t.after(() => new Promise(resolve => server.close(resolve)));
  await once(server, 'listening');
  const base = `http://127.0.0.1:${server.address().port}/api/marketplace`;

  const unfiltered = await fetch(base);
  assert.equal(unfiltered.status, 200);
  assert.deepEqual(await unfiltered.json(), [product]);
  assert.match(statements.at(-1).sql, /WHERE is_active = TRUE ORDER BY id DESC/);
  assert.deepEqual(statements.at(-1).params, []);

  const query = new URLSearchParams({
    model: ' Mustang ', category: 'Interior', location: 'Palmas - TO', min_price: '0', max_price: '500.00',
  });
  assert.equal((await fetch(`${base}?${query}`)).status, 200);
  assert.match(statements.at(-1).sql,
    /is_active = TRUE AND model = \? AND category = \? AND location = \? AND price >= \? AND price <= \?/);
  assert.deepEqual(statements.at(-1).params, ['Mustang', 'Interior', 'Palmas - TO', '0', '500.00']);
  const payload = "Mustang' OR 1=1 --";
  assert.equal((await fetch(`${base}?${new URLSearchParams({ model: payload })}`)).status, 200);
  assert.equal(statements.at(-1).sql.includes(payload), false);
  assert.deepEqual(statements.at(-1).params, [payload]);
  await fetch(`${base}?model=&category=%20%20&location=`);
  assert.deepEqual(statements.at(-1).params, []);

  const count = statements.length;
  for (const invalid of [
    'min_price=-1', 'max_price=abc', 'min_price=1.234', 'min_price=NaN',
    'min_price=1e2', 'max_price=100000000', 'min_price=',
    'min_price=50&max_price=10', 'model=Mustang&model=Fiesta',
    'min_price=0&min_price=1', `category=${'x'.repeat(256)}`,
  ]) {
    assert.equal((await fetch(`${base}?${invalid}`)).status, 400, invalid);
  }
  assert.equal(statements.length, count, 'validação deve ocorrer antes da consulta SQL');

  const filtersResponse = await fetch(`${base}/filters`);
  assert.equal(filtersResponse.status, 200);
  assert.deepEqual(await filtersResponse.json(), {
    models: ['Fiesta', 'Mustang'], categories: ['Interior', 'Rodas'], locations: ['Palmas - TO', 'São Paulo'],
  });
  assert.match(statements.at(-1).sql, /WHERE is_active = TRUE/);

  execute.mock.mockImplementation(async () => [[]]);
  assert.deepEqual(await (await fetch(base)).json(), []);
  assert.deepEqual(await (await fetch(`${base}/filters`)).json(), {
    models: [], categories: [], locations: [],
  });

  t.mock.method(console, 'error', () => {});
  execute.mock.mockImplementation(async () => { throw new Error('Detalhes internos do MySQL'); });
  for (const path of ['', '/filters']) {
    const response = await fetch(`${base}${path}`);
    assert.equal(response.status, 500);
    assert.doesNotMatch(await response.text(), /Detalhes internos|MySQL/);
  }
});
