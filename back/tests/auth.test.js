import assert from 'node:assert/strict';
import { test } from 'node:test';
import bcrypt from 'bcrypt';
import jwt from 'jsonwebtoken';
import pool from '../src/config/db.js';
import { login, register } from '../src/controllers/authController.js';

test('registro e login com persistência simulada, bcrypt e JWT reais', async (t) => {
  const users = [];
  const queries = [];
  const secret = 'segredo-exclusivo-para-testes-de-autenticacao';
  t.mock.method(pool, 'execute', async (sql, params) => {
    queries.push({ sql, params });
    if (sql.startsWith('UPDATE')) return [{ affectedRows: 1 }];
    if (sql.startsWith('SELECT')) {
      return [users.filter(user => user.email === params[0])];
    }
    const [name, email, password, role] = params;
    const user = { id: users.length + 1, name, email, password, role };
    users.push(user);
    return [{ insertId: user.id }];
  });
  const originalSecret = process.env.JWT_SECRET;
  process.env.JWT_SECRET = secret;
  t.after(() => {
    if (originalSecret === undefined) delete process.env.JWT_SECRET;
    else process.env.JWT_SECRET = originalSecret;
  });

  async function invoke(handler, body) {
    const res = {
      statusCode: 200,
      status(code) { this.statusCode = code; return this; },
      json(data) { this.body = data; return this; },
    };
    await handler({ body }, res);
    return res;
  }

  const credentials = { name: ' Ana ', email: ' ANA@example.com ', password: 'senha-segura-123' };
  const created = await invoke(register, credentials);
  assert.equal(created.statusCode, 201);
  assert.deepEqual(created.body.user, {
    id: 1, name: 'Ana', email: 'ana@example.com', role: 'comprador',
  });
  assert.notEqual(users[0].password, credentials.password);
  assert.equal(await bcrypt.compare(credentials.password, users[0].password), true);
  assert.equal(queries[1].sql.includes('VALUES (?, ?, ?, ?)'), true);
  assert.equal((await invoke(register, credentials)).statusCode, 409);

  const authenticated = await invoke(login, credentials);
  assert.equal(authenticated.statusCode, 200);
  assert.equal('password' in authenticated.body.user, false);
  const claims = jwt.verify(authenticated.body.token, secret, { algorithms: ['HS256'] });
  assert.equal(claims.sub, '1');
  assert.equal(claims.role, 'comprador');
  assert.equal(claims.exp - claims.iat, 3600);

  const wrongPassword = await invoke(login, { ...credentials, password: 'senha-incorreta' });
  const missingUser = await invoke(login, { ...credentials, email: 'outro@example.com' });
  assert.equal(wrongPassword.statusCode, 401);
  assert.equal(missingUser.statusCode, 401);
  assert.deepEqual(wrongPassword.body, missingUser.body);
  assert.equal((await invoke(register, { ...credentials, role: 'admin' })).statusCode, 400);
  assert.equal((await invoke(register, { ...credentials, password: 'é'.repeat(37) })).statusCode, 400);
  assert.equal((await invoke(register, undefined)).statusCode, 400);
  assert.equal((await invoke(login, {})).statusCode, 400);

  // Simula a disputa por um e-mail após a consulta inicial.
  pool.execute.mock.mockImplementation(async (sql) => {
    if (sql.startsWith('SELECT')) return [[]];
    throw Object.assign(new Error('Duplicate entry'), { code: 'ER_DUP_ENTRY' });
  });
  assert.equal((await invoke(register, credentials)).statusCode, 409);
});
