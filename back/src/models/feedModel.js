import pool from '../config/db.js';

export async function listPosts() {
  // LEFT JOIN preserva posts sem produto; uma consulta evita buscas por post.
  const [rows] = await pool.execute(`
    SELECT p.id, p.user_id, p.product_id, p.video_url, p.image_url AS post_image_url, p.description,
           p.likes_count, p.comments_count, u.name AS user_name,
           pr.id AS linked_product_id, pr.title, pr.price, pr.image_url,
           pr.user_id AS seller_id, pr.category, pr.model, pr.location, pr.video_url AS product_video_url
    FROM posts p
    INNER JOIN users u ON u.id = p.user_id
    LEFT JOIN products pr ON pr.id = p.product_id
    ORDER BY p.id DESC
  `);

  return rows.map(row => ({
    id: row.id,
    user_id: row.user_id,
    user: { id: row.user_id, name: row.user_name },
    product_id: row.product_id,
    video_url: row.video_url,
    image_url: row.post_image_url,
    description: row.description,
    likes_count: row.likes_count,
    comments_count: row.comments_count,
    product: row.linked_product_id == null ? null : {
      id: row.linked_product_id,
      title: row.title,
      price: row.price,
      image_url: row.image_url,
      video_url: row.product_video_url,
      user_id: row.seller_id,
      category: row.category,
      model: row.model,
      location: row.location,
    },
  }));
}

// A tela de detalhes consulta pelo ID recebido da tag, sem depender do feed em memória.
export async function findProductById(id) {
  const [rows] = await pool.execute(
    `SELECT id, title, price, image_url, video_url, user_id, category, model, location
     FROM products WHERE id = ? LIMIT 1`,
    [id],
  );
  return rows[0] ?? null;
}

export async function createPost(userId, { description, image_url, video_url }) {
  const [result] = await pool.execute(
    'INSERT INTO posts (user_id, description, image_url, video_url) VALUES (?, ?, ?, ?)',
    [userId, description, image_url, video_url]);
  return { id: result.insertId, user_id: userId, description, image_url, video_url };
}
