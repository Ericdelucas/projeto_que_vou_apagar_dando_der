import assert from 'node:assert/strict';
import { test } from 'node:test';
import { createDatabaseRouter } from '../src/config/databaseRouter.js';

test('roteamento: escolha única e concorrente, MySQL preferencial e fallback apenas de conectividade', async t => {
  await t.test('auto seleciona o mesmo SQLite uma vez para requisições concorrentes', async () => {
    let probes = 0; let fallbacks = 0; let notices = 0;
    const memory = { async execute() { return [['demo']]; }, async end() {} };
    const router = createDatabaseRouter({ mode: 'auto', mysqlPool: {
      async query() { probes++; throw Object.assign(new Error('offline'), { code: 'ECONNREFUSED' }); },
      async end() {},
    }, async createFallback() { fallbacks++; return memory; }, onFallback() { notices++; } });
    const results = await Promise.all([router.execute('SELECT'), router.execute('SELECT'), router.initialize()]);
    assert.deepEqual(results[0], [['demo']]); assert.equal(probes, 1); assert.equal(fallbacks, 1); assert.equal(notices, 1);
    assert.equal(router.status().engine, 'sqlite-memory'); assert.equal(router.status().reason, 'ECONNREFUSED');
    await router.end();
  });
  await t.test('MySQL ativo permanece selecionado e não troca de banco após operações', async () => {
    let online = true; let fallbacks = 0;
    const router = createDatabaseRouter({ mode: 'auto', mysqlPool: {
      async query() { return [[{ ok: 1 }]]; },
      async execute() {
        if (!online) throw Object.assign(new Error('offline'), { code: 'ECONNREFUSED' });
        return [['mysql']];
      }, async end() {},
    }, async createFallback() { fallbacks++; } });
    assert.deepEqual(await router.execute('query'), [['mysql']]);
    online = false;
    await assert.rejects(router.execute('query'), { code: 'ECONNREFUSED' });
    assert.equal(fallbacks, 0); assert.equal(router.status().engine, 'mysql'); await router.end();
  });
  await t.test('mysql estrito e erros de credencial/schema/SQL não ativam demonstração', async () => {
    for (const [mode, code] of [['mysql', 'ECONNREFUSED'], ['auto', 'ER_ACCESS_DENIED_ERROR'],
      ['auto', 'ER_BAD_DB_ERROR'], ['auto', 'ER_PARSE_ERROR']]) {
      let fallbacks = 0;
      const router = createDatabaseRouter({ mode, mysqlPool: {
        async query() { throw Object.assign(new Error('indisponível'), { code }); }, async end() {},
      }, async createFallback() { fallbacks++; } });
      await assert.rejects(router.initialize(), { code }); assert.equal(fallbacks, 0); await router.end();
    }
  });
  await t.test('memory explícito não tenta conectar ao MySQL', async () => {
    let probes = 0;
    const router = createDatabaseRouter({ mode: 'memory', mysqlPool: { async query() { probes++; }, async end() {} },
      async createFallback() { return { async query() { return [[]]; }, async end() {} }; } });
    await router.query('SELECT 1'); assert.equal(probes, 0); assert.equal(router.status().volatile, true);
    await router.end(); await assert.rejects(router.query('SELECT 1'), /encerrado/);
  });
});
