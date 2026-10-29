const networkErrors = new Set([
  'ECONNREFUSED', 'ENOTFOUND', 'ETIMEDOUT', 'ECONNRESET', 'EHOSTUNREACH',
  'ENETUNREACH', 'EAI_AGAIN', 'PROTOCOL_CONNECTION_LOST',
]);

export function createDatabaseRouter({ mode, mysqlPool, createFallback, onFallback = () => {} }) {
  if (!['auto', 'mysql', 'memory'].includes(mode)) throw new Error('DB_MODE deve ser auto, mysql ou memory.');
  let backend;
  let initialization;
  let closed = false;
  let info = { engine: 'uninitialized', mode, fallback: false, volatile: false };

  async function initialize() {
    if (closed) throw new Error('Banco encerrado.');
    // Uma decisão por processo, inclusive quando várias requisições chegam juntas.
    initialization ??= (async () => {
      let reason;
      if (mode !== 'memory') {
        try {
          await mysqlPool.query('SELECT 1');
          backend = mysqlPool;
          info = { engine: 'mysql', mode, fallback: false, volatile: false };
          return backend;
        } catch (error) {
          if (mode !== 'auto' || !networkErrors.has(error.code)) throw error;
          reason = error.code;
        }
      }
      // Nunca mascara credenciais inválidas, schema ausente ou erros de SQL.
      await mysqlPool.end();
      backend = await createFallback();
      info = { engine: 'sqlite-memory', mode, fallback: mode === 'auto', volatile: true,
        ...(reason ? { reason } : {}) };
      onFallback(info);
      return backend;
    })();
    return initialization;
  }

  return {
    initialize,
    status: () => ({ ...info }),
    async query(...args) { return (await initialize()).query(...args); },
    async execute(...args) { return (await initialize()).execute(...args); },
    async getConnection() { return (await initialize()).getConnection(); },
    async end() {
      if (closed) return;
      if (initialization) { try { await initialization; } catch { /* encerra a pool mesmo após falha */ } }
      closed = true;
      await (backend ?? mysqlPool).end();
    },
  };
}
