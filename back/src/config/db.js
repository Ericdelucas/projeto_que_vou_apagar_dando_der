import 'dotenv/config';
import mysql from 'mysql2/promise';
import { randomBytes } from 'node:crypto';
import { createDatabaseRouter } from './databaseRouter.js';

const mode = process.env.DB_MODE || (process.env.NODE_ENV === 'production' ? 'mysql' : 'auto');
const mysqlPool = mysql.createPool({
  host: process.env.DB_HOST || 'localhost',
  user: process.env.DB_USER || 'root',
  password: process.env.DB_PASSWORD ?? '',
  database: process.env.DB_NAME || 'skian_db',
  port: Number(process.env.DB_PORT || 3306),
  connectTimeout: 2000,
  waitForConnections: true,
  connectionLimit: 10,
  queueLimit: 0,
});

const pool = createDatabaseRouter({
  mode, mysqlPool,
  createFallback: async () => (await import('./memoryDatabase.js')).createMemoryDatabase(),
  onFallback(info) {
    if (info.fallback) console.warn('Aviso: MySQL offline. Ativando modo de fallback com dados em memória/mock');
    // IDs de demonstração não podem herdar sessões do MySQL ou de outro reinício.
    process.env.JWT_SECRET = randomBytes(48).toString('hex');
    console.warn('[demo] JWT_SECRET efêmero e exclusivo desta demonstração criado.');
    console.warn(`[demo] SQLite em memória ativo (${info.reason || 'DB_MODE=memory'}). Dados reiniciam ao encerrar o servidor.`);
    console.info('[demo] Login: comprador@skippecas.local ou vendedor@skippecas.local | Senha: SkipPecas123!');
  },
});

export const getDatabaseStatus = () => pool.status();
export default pool;
