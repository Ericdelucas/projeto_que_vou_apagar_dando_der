import assert from 'node:assert/strict';
import { once } from 'node:events';
import { test } from 'node:test';
import app from '../src/app.js';

test('erros HTTP: JSON inválido, rota ausente e payload excessivo não expõem detalhes internos', async t => {
  const server = app.listen(0, '127.0.0.1');
  t.after(() => new Promise(resolve => server.close(resolve)));
  await once(server, 'listening');
  const base = `http://127.0.0.1:${server.address().port}`;
  const cases = [
    ['/api/auth/login', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: '{"password":"segredo-nao-expor"' }, 400, 'JSON inválido.'],
    ['/api/rota-inexistente', {}, 404, 'Rota não encontrada.'],
    ['/api/auth/login', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ data: 'x'.repeat(110000) }) }, 413, 'O conteúdo enviado excede o limite permitido.'],
  ];
  for (const [path, options, status, message] of cases) {
    const response = await fetch(base + path, { ...options, signal: AbortSignal.timeout(5000) });
    assert.equal(response.status, status);
    assert.match(response.headers.get('content-type'), /application\/json/);
    const body = await response.json();
    assert.deepEqual(body, { message });
    assert.doesNotMatch(JSON.stringify(body), /stack|SyntaxError|node_modules|segredo-nao-expor/);
  }
});
