import { DatabaseSync } from 'node:sqlite';
import { readFile } from 'node:fs/promises';
import bcrypt from 'bcrypt';
import { demoImages, demoPostMedia } from './demoMedia.js';

export const demoAccounts = Object.freeze([
  { name: 'Ana Compradora', email: 'comprador@skippecas.local', role: 'comprador' },
  { name: 'Bruno Autopeças', email: 'vendedor@skippecas.local', role: 'vendedor' },
  { name: 'Oficina São José', email: 'oficina@skippecas.local', role: 'vendedor' },
]);
export const demoPassword = 'SkipPecas123!';

function translate(sql) {
  if (/information_schema\.TABLES/i.test(sql)) {
    return "SELECT name AS table_name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'";
  }
  return sql.replace(/\s+FOR UPDATE\b/gi, '')
    .replace(/\bprice\s*(>=|<=)\s*\?/gi, 'CAST(price AS REAL) $1 CAST(? AS REAL)')
    .replace(/CURRENT_TIMESTAMP\s*-\s*INTERVAL\s+15\s+MINUTE/gi, "datetime('now', '-15 minutes')")
    .replace(/CONCAT\('post:',\s*p\.id\)/gi, "('post:' || p.id)")
    .replace(/CONCAT\('product:',\s*pr\.id\)/gi, "('product:' || pr.id)")
    .replace(/\b(name|email) LIKE \? ESCAPE '!'/gi, "fold($1) LIKE fold(?) ESCAPE '!'")
    .replace(/ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID\(id\)/gi,
      'ON CONFLICT(user_id, blocked_user_id) DO NOTHING');
}

export async function createMemoryDatabase({ seed = true } = {}) {
  const database = new DatabaseSync(':memory:');
  try {
    database.exec(await readFile(new URL('./memory_schema.sql', import.meta.url), 'utf8'));
    database.function('fold', { deterministic: true }, value => String(value ?? '').normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '').toLowerCase());
    if (seed) {
      const hash = await bcrypt.hash(demoPassword, 12);
      const user = database.prepare('INSERT INTO users (name, email, password, role, bio) VALUES (?, ?, ?, ?, ?)');
      for (const item of demoAccounts) user.run(item.name, item.email, hash, item.role, 'Perfil de demonstração do SkipPeças.');
      // Conta local solicitada para testar o aplicativo; senha armazenada com bcrypt.
      user.run('Eric', 'ericdelucass@gmail.com', await bcrypt.hash('123', 12), 'comprador', 'Conta local de demonstração.');
      const names = ['Caio Motor', 'Davi Rodas', 'Eva Peças', 'Iara Auto', 'Léo Garage',
        'Bia Lima', 'Cris Melo', 'Duda Reis', 'Gui Alves', 'Nina Luz'];
      const testHash = await bcrypt.hash(demoPassword, 12);
      const post = database.prepare('INSERT INTO posts (user_id, description, image_url, created_at) VALUES (?, ?, ?, ?)');
      for (let i = 0; i < 10; i++) {
        const role = i < 5 ? 'vendedor' : 'comprador';
        const number = i % 5 + 1;
        const result = user.run(names[i], `${role}${number}@skippecas.local`, testHash, role, 'Conta de teste — histórico simulado.');
        const id = Number(result.lastInsertRowid);
        // Frequências distintas e estáveis; os posts são registros reais, com datas históricas.
        const history = Array(14).fill(0);
        for (let n = 0; n < 15 + i; n++) history[(n * (2 * i + 1) + i) % 14]++;
        for (let day = 0; day < 14; day++) {
          const count = history[day];
          const date = new Date(); date.setUTCDate(date.getUTCDate() - (13 - day));
          for (let n = 0; n < count; n++) {
            const media = demoPostMedia[(i + day + n) % demoPostMedia.length];
            post.run(id, `${names[i]} — ${media.caption} (Demonstração ${day + 1}.${n + 1})`,
              media.imageUrl, date.toISOString().slice(0, 19).replace('T', ' '));
          }
        }
      }
      const product = database.prepare(
        'INSERT INTO products (title, price, image_url, user_id, category, model, location) VALUES (?, ?, ?, ?, ?, ?, ?)');
      product.run('Volante esportivo', '150.00', demoImages.steeringWheel, 2, 'Interior', 'Gol', 'Palmas');
      product.run('Farol dianteiro', '320.90', demoImages.headlight, 3, 'Iluminação', 'Onix', 'São Paulo');
      product.run('Roda de liga leve', '580.00', demoImages.wheel, 2, 'Rodas', 'Corolla', 'Palmas');
      database.exec(`
        INSERT INTO posts (user_id, product_id, description, likes_count, comments_count) VALUES
          (2, 1, 'Volante disponível para seu projeto.', 12, 3),
          (3, 2, 'Farol revisado, pronto para instalar.', 8, 1),
          (2, 3, 'Roda de liga leve em ótimo estado.', 17, 4);
        INSERT INTO favorites (user_id, product_id) VALUES (1, 1);
        INSERT INTO user_follows (follower_id, followed_id) VALUES (1, 2), (3, 2);
        INSERT INTO product_requests (product_id, buyer_id) VALUES (1, 1);
        INSERT INTO blocked_users (user_id, blocked_user_id) VALUES (1, 3);
      `);
    }
  } catch (error) { database.close(); throw error; }

  // Uma conexão SQLite compartilhada: o mutex também protege consultas avulsas
  // para que outra requisição não leia/escreva dentro da transação de um usuário.
  let tail = Promise.resolve();
  let closed = false;
  async function acquire() {
    let unlock;
    const next = new Promise(resolve => { unlock = resolve; });
    const previous = tail;
    tail = previous.then(() => next);
    await previous;
    return unlock;
  }
  function run(sql, params = []) {
    if (closed) throw new Error('Banco de demonstração encerrado.');
    try {
      const statement = database.prepare(translate(sql));
      const values = params.map(value => typeof value === 'boolean' ? Number(value) : value);
      if (statement.columns().length) return [statement.all(...values).map(row => ({ ...row })), []];
      const result = statement.run(...values);
      return [{ affectedRows: Number(result.changes), insertId: Number(result.lastInsertRowid) }, []];
    } catch (error) {
      if (/UNIQUE constraint failed/i.test(error.message)) error.code = 'ER_DUP_ENTRY';
      throw error;
    }
  }
  async function execute(sql, params) {
    const unlock = await acquire();
    try { return run(sql, params); } finally { unlock(); }
  }
  return {
    query: execute, execute,
    async getConnection() {
      let unlock;
      let released = false;
      async function connectionExecute(sql, params) {
        if (released) throw new Error('Conexão já liberada.');
        return unlock ? run(sql, params) : execute(sql, params);
      }
      function finish(command) {
        if (!unlock) return;
        let finished = false;
        try { database.exec(command); finished = true; }
        finally {
          if (finished || command === 'ROLLBACK') { unlock(); unlock = undefined; }
        }
      }
      return {
        query: connectionExecute, execute: connectionExecute,
        async beginTransaction() {
          if (released || unlock) throw new Error('Estado de transação inválido.');
          unlock = await acquire();
          try { database.exec('BEGIN IMMEDIATE'); }
          catch (error) { unlock(); unlock = undefined; throw error; }
        },
        async commit() { finish('COMMIT'); },
        async rollback() { finish('ROLLBACK'); },
        release() { finish('ROLLBACK'); released = true; },
      };
    },
    async end() {
      const unlock = await acquire();
      try { if (!closed) { database.close(); closed = true; } } finally { unlock(); }
    },
  };
}
