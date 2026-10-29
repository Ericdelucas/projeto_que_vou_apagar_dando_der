import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { spawn } from 'node:child_process';
import { once } from 'node:events';
import mysql from 'mysql2/promise';
import app from '../../src/app.js';
import pool from '../../src/config/db.js';

// Executa o cliente Retrofit real em um banco novo e isolado. Nunca usa skian_db.
const database = process.env.DB_NAME || '';
assert.equal(process.env.SKIPPECAS_E2E, '1');
assert.match(database, /^skippecas_e2e_[a-z0-9_]+$/);
const admin = await mysql.createConnection({ host: process.env.DB_HOST || '127.0.0.1',
  port: Number(process.env.DB_PORT || 3306), user: process.env.DB_USER || 'root',
  password: process.env.DB_PASSWORD ?? '', multipleStatements: true });
let server; let created = false;
try {
  const [exists] = await admin.execute('SELECT SCHEMA_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = ?', [database]);
  assert.equal(exists.length, 0, 'Recusado: banco E2E já existe');
  const schema = (await readFile(new URL('../../src/config/migrations/000_full_schema.sql', import.meta.url), 'utf8')).replaceAll('skian_db', database);
  await admin.query(schema); created = true;
  server = app.listen(0, '127.0.0.1'); await once(server, 'listening');
  const android = new URL('../../../front/projeto_que_vou_apagar_dando_der/', import.meta.url);
  const child = spawn('./gradlew', ['--offline', '--no-daemon', '--no-configuration-cache', '--warning-mode=fail',
    ':app:testDebugUnitTest', '--rerun-tasks'], {
    cwd: android,
    env: { ...process.env, SKIPPECAS_E2E_URL: `http://127.0.0.1:${server.address().port}/api/` }, stdio: 'inherit',
  });
  const [code] = await once(child, 'exit'); process.exitCode = code || 0;
} finally {
  if (server) await new Promise(resolve => server.close(resolve));
  await pool.end();
  if (created) await admin.query(`DROP DATABASE \`${database}\``);
  await admin.end();
}
