import pool from '../config/db.js';

export async function listProducts(filters = {}) {
  const conditions = ['is_active = TRUE'];
  const values = [];

  // Somente nomes de colunas definidos pelo servidor entram no SQL.
  for (const field of ['model', 'category', 'location']) {
    if (filters[field] !== undefined) {
      conditions.push(`${field} = ?`);
      values.push(filters[field]);
    }
  }
  if (filters.min_price !== undefined) {
    conditions.push('price >= ?');
    values.push(filters.min_price);
  }
  if (filters.max_price !== undefined) {
    conditions.push('price <= ?');
    values.push(filters.max_price);
  }

  const [rows] = await pool.execute(
    `SELECT id, title, price, image_url, video_url, user_id, category, model, location
     FROM products WHERE ${conditions.join(' AND ')} ORDER BY id DESC`,
    values,
  );
  return rows;
}

export async function listFilterOptions() {
  // Opções independentes da seleção atual: o usuário pode trocar qualquer filtro.
  const [rows] = await pool.execute(
    'SELECT DISTINCT model, category, location FROM products WHERE is_active = TRUE',
  );
  const distinct = field => [...new Set(rows.map(row => row[field])
    .filter(value => typeof value === 'string' && value.trim()))]
    .sort((a, b) => a.localeCompare(b, 'pt-BR'));

  return { models: distinct('model'), categories: distinct('category'), locations: distinct('location') };
}
