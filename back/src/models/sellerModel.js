import pool from '../config/db.js';

export async function listSellerProducts(userId) {
  const [rows] = await pool.execute(`
    SELECT p.id, p.title, p.price, p.image_url, p.video_url, p.user_id,
           p.category, p.model, p.location,
           CASE WHEN p.is_active = TRUE THEN 'ativo' ELSE 'inativo' END AS status,
           COALESCE(r.total, 0) AS requests_count,
           COALESCE(r.pending, 0) AS pending_requests_count,
           COALESCE(r.accepted, 0) AS accepted_requests_count,
           COALESCE(r.rejected, 0) AS rejected_requests_count
    FROM products p
    LEFT JOIN (
      SELECT product_id, COUNT(*) AS total,
             SUM(status = 'pendente') AS pending,
             SUM(status = 'aceita') AS accepted,
             SUM(status = 'recusada') AS rejected
      FROM product_requests GROUP BY product_id
    ) r ON r.product_id = p.id
    WHERE p.user_id = ? ORDER BY p.id DESC
  `, [userId]);
  // SUM pode ser retornado como DECIMAL/string pelo mysql2.
  return rows.map(row => ({
    ...row,
    requests_count: Number(row.requests_count),
    pending_requests_count: Number(row.pending_requests_count),
    accepted_requests_count: Number(row.accepted_requests_count),
    rejected_requests_count: Number(row.rejected_requests_count),
  }));
}

export async function createSellerProduct(userId, product) {
  const { title, price, category, model, image_url, video_url, location } = product;
  const [result] = await pool.execute(
    `INSERT INTO products (title, price, category, model, image_url, video_url, location, user_id, is_active)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, TRUE)`,
    [title, price, category, model, image_url, video_url, location, userId],
  );
  return {
    id: result.insertId, ...product, price: Number(price).toFixed(2), user_id: userId, status: 'ativo',
    requests_count: 0, pending_requests_count: 0, accepted_requests_count: 0, rejected_requests_count: 0,
  };
}
