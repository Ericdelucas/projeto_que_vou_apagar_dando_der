import pool, { getDatabaseStatus } from '../config/db.js';

export const moduleTables = Object.freeze({
  authentication: ['users'],
  feed: ['users', 'posts', 'products'],
  marketplace: ['products', 'users'],
  favorites: ['favorites', 'products', 'users'],
  sales: ['products', 'product_requests', 'users'],
  users: ['users', 'user_follows', 'posts', 'products'],
  settings: ['users', 'blocked_users', 'favorites', 'posts', 'products', 'product_requests', 'user_follows'],
});

export async function health(req, res) {
  try {
    const [rows] = await pool.query(`SELECT TABLE_NAME AS table_name FROM information_schema.TABLES
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE'`);
    const tables = new Set(rows.map(row => row.table_name));
    const modules = Object.fromEntries(Object.entries(moduleTables).map(([name, required]) => {
      const missingTables = required.filter(table => !tables.has(table));
      const needsJwt = ['authentication', 'favorites', 'sales', 'users', 'settings'].includes(name);
      const missingConfiguration = needsJwt && !process.env.JWT_SECRET ? ['JWT_SECRET'] : [];
      return [name, {
        status: missingTables.length || missingConfiguration.length ? 'degraded' : 'ok',
        missing_tables: missingTables, missing_configuration: missingConfiguration,
      }];
    }));
    const ready = Object.values(modules).every(module => module.status === 'ok');
    return res.status(ready ? 200 : 503).json({
      status: ready ? 'ok' : 'degraded', app: 'SkipPeças API',
      database: { status: 'ok', ...getDatabaseStatus() }, modules,
    });
  } catch (error) {
    console.error('Falha no health check:', error.code || error.name);
    return res.status(503).json({
      status: 'degraded', app: 'SkipPeças API', database: { status: 'unavailable', ...getDatabaseStatus() },
      modules: Object.fromEntries(Object.keys(moduleTables).map(name => [name, { status: 'unavailable' }])),
    });
  }
}
