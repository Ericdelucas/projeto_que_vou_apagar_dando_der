import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { once } from 'node:events';
import net from 'node:net';
import { test } from 'node:test';
import { setTimeout as delay } from 'node:timers/promises';

async function unusedPort() {
  const server = net.createServer().listen(0, '127.0.0.1');
  await once(server, 'listening');
  const port = server.address().port;
  await new Promise(resolve => server.close(resolve));
  return port;
}

test('smoke: servidor real em produção com MySQL indisponível', { timeout: 30000 }, async t => {
  const port = await unusedPort();
  let dbPort = await unusedPort();
  while (dbPort === port) dbPort = await unusedPort();
  let output = '';
  const child = spawn(process.execPath, ['src/server.js'], {
    cwd: new URL('../', import.meta.url),
    env: { ...process.env, DOTENV_CONFIG_PATH: '/dev/null', NODE_ENV: 'production',
      PORT: String(port), DB_MODE: 'auto', DB_HOST: '127.0.0.1', DB_PORT: String(dbPort),
      JWT_SECRET: 'smoke-isolated-secret' },
    stdio: ['ignore', 'pipe', 'pipe'],
  });
  child.stdout.on('data', data => { output += data; });
  child.stderr.on('data', data => { output += data; });
  let spawnError;
  child.on('error', error => { spawnError = error; });
  t.after(async () => {
    if (child.exitCode === null && child.signalCode === null && !spawnError) {
      const closed = once(child, 'close');
      child.kill('SIGTERM');
      const timer = setTimeout(() => child.kill('SIGKILL'), 5000);
      try { await closed; } finally { clearTimeout(timer); }
    }
  });
  const base = `http://127.0.0.1:${port}`;
  let ready = false;
  for (let attempt = 0; attempt < 150; attempt++) {
    if (spawnError) throw spawnError;
    assert.equal(child.exitCode, null, output);
    try {
      ready = (await fetch(`${base}/health`, { signal: AbortSignal.timeout(500) })).ok;
    } catch { /* aguarda inicialização e seed */ }
    if (ready) break;
    await delay(100);
  }
  assert.ok(ready, `Servidor não ficou pronto:\n${output}`);
  assert.match(output, new RegExp(`rodando na porta ${port}`));
  assert.match(output, /SQLite em memória ativo/);
  const request = async (method, path, token, body, status = 200) => {
    const response = await fetch(base + path, {
      method, signal: AbortSignal.timeout(5000),
      headers: { Origin: 'https://android.skippecas.example', 'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}) },
      ...(body === undefined ? {} : { body: JSON.stringify(body) }),
    });
    const text = await response.text();
    assert.equal(response.status, status, `${method} ${path}: ${text}`);
    assert.equal(response.headers.get('access-control-allow-origin'), '*');
    t.diagnostic(`APROVADO ${method} ${path} → HTTP ${status}; CORS=*`);
    return text ? JSON.parse(text) : null;
  };
  const health = await request('GET', '/health');
  assert.equal(health.status, 'ok');
  assert.equal(health.database.engine, 'sqlite-memory');
  assert.equal(health.database.reason, 'ECONNREFUSED');
  assert.equal(health.database.volatile, true);
  assert.ok(Object.values(health.modules).every(module => module.status === 'ok'));
  const login = email => request('POST', '/api/auth/login', null, { email, password: 'SkipPecas123!' });
  const buyer = await login('comprador@skippecas.local');
  const seller = await login('vendedor@skippecas.local');
  assert.ok(buyer.token); assert.ok(seller.token);
  await request('POST', '/api/auth/login', null, { email: 'comprador@skippecas.local', password: 'errada' }, 401);
  for (const path of ['/api/feed', '/api/marketplace']) {
    assert.ok((await request('GET', path)).length > 0);
  }
  for (const [path, token] of [
    ['/api/favorites', buyer.token], ['/api/seller/products', seller.token],
    ['/api/users/search?q=sao&filter=vendedor', buyer.token], ['/api/settings/blocked', buyer.token],
  ]) {
    assert.ok((await request('GET', path, token)).length > 0);
    await request('GET', path, null, undefined, 401);
  }
  await request('GET', '/api/seller/products', buyer.token, undefined, 403);
  await request('OPTIONS', '/api/favorites', null, undefined, 204);
  const closed = once(child, 'close');
  child.kill('SIGTERM');
  const [code, signal] = await closed;
  assert.equal(code, 0, output); assert.equal(signal, null, output);
  t.diagnostic('APROVADO inicialização com fallback, porta dinâmica e encerramento SIGTERM sem crash');
});
