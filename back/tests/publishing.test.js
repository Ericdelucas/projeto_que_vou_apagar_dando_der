import assert from 'node:assert/strict';
import { readFile, unlink } from 'node:fs/promises';
import { once } from 'node:events';
import { test } from 'node:test';

process.env.DB_MODE = 'memory';
const { default: pool } = await import('../src/config/db.js');
const { default: app } = await import('../src/app.js');

test('dez contas reais, gráficos distintos e publicação autenticada no perfil e feed', async t => {
  await pool.initialize();
  const server = app.listen(0, '127.0.0.1');
  await once(server, 'listening');
  t.after(async () => { await new Promise(resolve => server.close(resolve)); await pool.end(); });
  const base = `http://127.0.0.1:${server.address().port}`;
  const send = async (path, token, body) => {
    const response = await fetch(base + path, {
      method: body === undefined ? 'GET' : 'POST',
      headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
      ...(body === undefined ? {} : { body: JSON.stringify(body) }),
    });
    return { status: response.status, body: await response.json() };
  };
  const accounts = [];
  for (const role of ['vendedor', 'comprador']) for (let n = 1; n <= 5; n++) {
    const result = await send('/api/auth/login', null, { email: `${role}${n}@skippecas.local`, password: 'SkipPecas123!' });
    assert.equal(result.status, 200); assert.equal(result.body.user.role, role);
    accounts.push(result.body);
  }
  const buyer = accounts[5];
  const search = await send('/api/users/search', buyer.token);
  assert.equal(search.status, 200);
  const users = search.body.filter(user => accounts.some(account => account.user.id === user.id));
  assert.equal(users.length, 10);
  assert.equal(new Set(users.map(user => JSON.stringify(user.activity))).size, 10);
  assert.equal(new Set(users.map(user => user.activity_count)).size, 10);
  assert.ok(users.every(user => user.activity.length === 14));
  const before = (await send('/api/users/me', buyer.token)).body;
  const description = 'Publicação nova de comprador: aparece no perfil e no home';
  assert.equal((await send('/api/posts', null, { description })).status, 401);
  for (const body of [{ description: '' }, { description: 'x'.repeat(2001) },
    { description, user_id: accounts[0].user.id }, { description, image_url: 'file:///tmp/test' },
    { description, video_url: 'javascript:alert(1)' }]) {
    assert.equal((await send('/api/posts', buyer.token, body)).status, 400);
  }
  const created = await send('/api/posts', buyer.token, { description, image_url: 'https://example.com/image.jpg' });
  assert.equal(created.status, 201); assert.equal(created.body.user_id, buyer.user.id);
  const profile = (await send('/api/users/me', buyer.token)).body;
  assert.equal(profile.posts_count, before.posts_count + 1);
  assert.equal(profile.media[0].post_id, created.body.id);
  assert.equal(profile.media[0].image_url, 'https://example.com/image.jpg');
  const feed = (await send('/api/feed')).body;
  assert.equal(feed[0].id, created.body.id); assert.equal(feed[0].description, description);
  assert.equal(feed[0].image_url, 'https://example.com/image.jpg'); assert.equal(feed[0].product, null);
  const updated = (await send('/api/users/search', buyer.token)).body.find(user => user.id === buyer.user.id);
  assert.equal(updated.activity_count, users.find(user => user.id === buyer.user.id).activity_count + 1);
  assert.equal((await send('/api/posts', accounts[0].token, { description: 'Vendedor também publica.' })).status, 201);
  const photo = await readFile(new URL('../public/demo/peca.png', import.meta.url));
  const upload = (bytes, token = buyer.token, type = 'image/png', caption = 'Foto da galeria') => fetch(base + '/api/posts/photo?description=' + encodeURIComponent(caption), {
    method: 'POST', headers: { 'Content-Type': type, ...(token ? { Authorization: `Bearer ${token}` } : {}) }, body: bytes,
  });
  assert.equal((await upload(photo, null)).status, 401);
  assert.equal((await upload(Buffer.from('arquivo falso com conteúdo inválido'))).status, 400);
  assert.equal((await upload(photo, buyer.token, 'image/jpeg')).status, 400);
  assert.equal((await upload(photo, buyer.token, 'image/png', 'x'.repeat(2001))).status, 400);
  assert.equal((await upload(Buffer.alloc(8 * 1024 * 1024 + 1))).status, 413);
  const uploaded = await upload(photo);
  assert.equal(uploaded.status, 201);
  const photoPost = await uploaded.json();
  const filename = new URL(photoPost.image_url).pathname.split('/').at(-1);
  t.after(() => unlink(new URL('../uploads/' + filename, import.meta.url)));
  const fileResponse = await fetch(photoPost.image_url);
  assert.equal(fileResponse.status, 200);
  assert.equal(fileResponse.headers.get('content-type'), 'image/png');
  assert.deepEqual(Buffer.from(await fileResponse.arrayBuffer()), photo);
  const photoFeed = (await send('/api/feed')).body;
  assert.equal(photoFeed[0].id, photoPost.id);
  assert.equal(photoFeed[0].image_url, photoPost.image_url);
  const photoProfile = (await send('/api/users/me', buyer.token)).body;
  assert.equal(photoProfile.media[0].post_id, photoPost.id);
  assert.equal(photoProfile.media[0].image_url, photoPost.image_url);

});
