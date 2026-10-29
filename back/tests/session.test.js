import assert from 'node:assert/strict';
import { once } from 'node:events';
import { test } from 'node:test';
import express from 'express';
import jwt from 'jsonwebtoken';
import pool from '../src/config/db.js';
import { requireAuth } from '../src/middleware/authMiddleware.js';

test('sessão: conta removida invalida JWT, falhas do banco fecham acesso e JWT inválido não consulta banco', async t => {
  const secret = process.env.JWT_SECRET;
  process.env.JWT_SECRET = 'session-test-secret';
  t.after(() => { if (secret === undefined) delete process.env.JWT_SECRET; else process.env.JWT_SECRET = secret; });
  let active = true; let unavailable = false; let queries = 0;
  t.mock.method(console, 'error', () => {});
  t.mock.method(pool, 'query', async (sql, params) => {
    queries++;
    assert.match(sql, /WHERE id = \? LIMIT 1/); assert.deepEqual(params, [7]);
    if (unavailable) throw new Error('sensitive details');
    return [active ? [{ id: 7 }] : []];
  });
  const app = express();
  app.get('/protected', requireAuth, (req, res) => res.json(req.auth));
  const server = app.listen(0, '127.0.0.1');
  t.after(() => new Promise(resolve => server.close(resolve)));
  await once(server, 'listening');
  const token = jwt.sign({}, process.env.JWT_SECRET, { subject: '7', expiresIn: '1h' });
  const send = (auth = token) => fetch(`http://127.0.0.1:${server.address().port}/protected`,
    { headers: { Authorization: `Bearer ${auth}` } });
  assert.equal((await send('invalid')).status, 401); assert.equal(queries, 0);
  assert.equal((await send()).status, 200);
  active = false;
  assert.equal((await send()).status, 401);
  unavailable = true;
  const failure = await send(); assert.equal(failure.status, 503);
  assert.doesNotMatch(await failure.text(), /sensitive/);
});
