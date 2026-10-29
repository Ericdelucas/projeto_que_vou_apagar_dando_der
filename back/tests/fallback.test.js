import 'dotenv/config';
import assert from 'node:assert/strict';
import { once } from 'node:events';
import net from 'node:net';
import jwt from 'jsonwebtoken';
import { test } from 'node:test';

test('APK sem MySQL: fallback automático e todos os módulos com dados/estado reais', async t => {
  const socket = net.createServer().listen(0, '127.0.0.1'); await once(socket, 'listening');
  const port = socket.address().port; await new Promise(resolve => socket.close(resolve));
  const keys = ['DB_MODE', 'DB_HOST', 'DB_PORT', 'JWT_SECRET'];
  const original = Object.fromEntries(keys.map(key => [key, process.env[key]]));
  Object.assign(process.env, { DB_MODE: 'auto', DB_HOST: '127.0.0.1', DB_PORT: String(port) });
  process.env.JWT_SECRET = 'segredo-original-mysql';
  const oldToken = jwt.sign({}, process.env.JWT_SECRET, { subject: '1', expiresIn: '1h' });
  t.after(() => { for (const key of keys) {
    if (original[key] === undefined) delete process.env[key]; else process.env[key] = original[key];
  } });
  const { default: app } = await import('../src/app.js');
  const { default: pool } = await import('../src/config/db.js');
  const server = app.listen(0, '127.0.0.1'); await once(server, 'listening');
  t.after(async () => { await new Promise(resolve => server.close(resolve)); await pool.end(); });
  const base = `http://127.0.0.1:${server.address().port}`;
  const send = (method, path, token, body) => fetch(base + path, {
    method, signal: AbortSignal.timeout(10000),
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
  });
  const json = async (response, code = 200) => {
    const text = await response.text(); assert.equal(response.status, code, text); return text ? JSON.parse(text) : null;
  };
  // Primeiro acesso protegido pode iniciar o fallback: o token antigo ainda deve ser recusado.
  assert.equal((await send('GET', '/api/users/me', oldToken)).status, 401);
  const [health, feed] = await Promise.all([json(await send('GET', '/health')), json(await send('GET', '/api/feed'))]);
  assert.equal(health.status, 'ok'); assert.equal(health.database.engine, 'sqlite-memory');
  assert.equal(health.database.reason, 'ECONNREFUSED'); assert.equal(health.database.volatile, true);
  assert.ok(Object.values(health.modules).every(module => module.status === 'ok'));
  assert.ok(process.env.JWT_SECRET.length >= 64);
  assert.notEqual(process.env.JWT_SECRET, 'segredo-original-mysql');
  assert.equal((await send('GET', '/api/users/me', oldToken)).status, 401);
  assert.equal(feed.length, 198); assert.ok(feed.every(post => post.user));
  const image = await fetch(feed.find(post => post.product)?.product.image_url);
  assert.equal(image.status, 200); assert.equal(image.headers.get('content-type'), 'image/png');
  assert.equal((await json(await send('GET', '/api/marketplace?min_price=50&max_price=200'))).length, 1);
  assert.equal((await json(await send('GET', '/api/marketplace?category=Ilumina%C3%A7%C3%A3o')))[0].id, 2);
  const filters = await json(await send('GET', '/api/marketplace/filters')); assert.equal(filters.models.length, 3);
  assert.equal((await json(await send('GET', '/api/products/1'))).price, '150.00');
  const login = async email => json(await send('POST', '/api/auth/login', null, { email, password: 'SkipPecas123!' }));
  const eric = await json(await send('POST', '/api/auth/login', null, { email: 'ericdelucass@gmail.com', password: '123' }));
  assert.equal(eric.user.email, 'ericdelucass@gmail.com');
  assert.ok(eric.token);
  const buyer = await login('comprador@skippecas.local'); const seller = await login('vendedor@skippecas.local');
  assert.equal((await send('POST', '/api/auth/login', null, { email: 'comprador@skippecas.local', password: 'incorreta' })).status, 401);
  assert.equal((await send('GET', '/api/favorites')).status, 401);
  const credentials = { name: 'Nova Pessoa', email: 'nova@example.com', password: 'senha-nova-123', role: 'comprador' };
  await json(await send('POST', '/api/auth/register', null, credentials), 201);
  assert.equal((await send('POST', '/api/auth/register', null, credentials)).status, 409);
  assert.ok((await json(await send('POST', '/api/auth/login', null, credentials))).token);
  const saved = await json(await send('GET', '/api/favorites', buyer.token)); assert.equal(saved[0].product_id, 1);
  const save = () => send('POST', '/api/favorites', buyer.token, { product_id: 2, is_favorite: true });
  await Promise.all([save().then(json), save().then(json)]);
  assert.equal((await json(await send('GET', '/api/favorites', buyer.token))).length, 2);
  await json(await send('POST', '/api/favorites', buyer.token, { product_id: 2, is_favorite: false }));
  const own = await json(await send('GET', '/api/users/me', buyer.token)); assert.equal(own.is_owner, true);
  const results = await json(await send('GET', '/api/users/search?q=sao&filter=vendedor', buyer.token));
  assert.equal(results[0].name, 'Oficina São José');
  assert.equal('email' in await json(await send('GET', '/api/users/profile/2', buyer.token)), false);
  const updated = await json(await send('PUT', '/api/users/profile', buyer.token, { name: 'Ana Atualizada', bio: 'Teste APK' }));
  assert.equal(updated.name, 'Ana Atualizada'); assert.equal(updated.bio, 'Teste APK');
  const products = await json(await send('GET', '/api/seller/products', seller.token));
  assert.equal(products.length, 2); assert.equal(products.find(p => p.id === 1).pending_requests_count, 1);
  assert.equal((await send('GET', '/api/seller/products', buyer.token)).status, 403);
  await json(await send('POST', '/api/seller/products', seller.token, {
    title: 'Peça nova', price: '25.50', category: 'Interior', model: 'Gol', location: 'Palmas',
    image_url: feed[0].product.image_url,
  }), 201);
  const blocked = await json(await send('GET', '/api/settings/blocked', buyer.token)); assert.equal(blocked.length, 1);
  assert.equal((await send('DELETE', `/api/settings/block/${blocked[0].id}`, seller.token)).status, 404);
  await json(await send('DELETE', `/api/settings/block/${blocked[0].id}`, buyer.token), 204);
  const block = () => send('POST', '/api/settings/block', buyer.token, { blocked_user_id: 2 });
  const [a, b] = await Promise.all([block().then(json), block().then(json)]); assert.equal(a.id, b.id);
  assert.equal((await json(await send('GET', '/api/settings/blocked', buyer.token))).length, 1);
  assert.equal((await send('POST', '/api/settings/block', buyer.token, { blocked_user_id: buyer.user.id })).status, 400);
  await json(await send('DELETE', '/api/settings/account', seller.token), 204);
  for (const path of ['/api/users/me', '/api/seller/products', '/api/settings/blocked', '/api/favorites'])
    assert.equal((await send('GET', path, seller.token)).status, 401);
  assert.deepEqual(await json(await send('GET', '/api/settings/blocked', buyer.token)), []);
  assert.deepEqual(await json(await send('GET', '/api/favorites', buyer.token)), []);
  assert.equal((await json(await send('GET', '/api/feed'))).length, feed.length - 2);
  assert.equal((await json(await send('GET', '/api/marketplace'))).length, 1);
  assert.equal((await json(await send('GET', '/health'))).database.engine, 'sqlite-memory');
});
