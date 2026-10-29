import pool from '../config/db.js';

export async function setFavorite(userId, productId, desiredState) {
  const connection = await pool.getConnection();
  try {
    await connection.beginTransaction();
    // Serializa alterações deste usuário, inclusive quando ainda não existe favorito.
    const [users] = await connection.execute('SELECT id FROM users WHERE id = ? FOR UPDATE', [userId]);
    if (!users.length) throw Object.assign(new Error('Usuário não encontrado.'), { status: 401 });

    const [favorites] = await connection.execute(
      'SELECT id FROM favorites WHERE user_id = ? AND product_id = ?', [userId, productId],
    );
    const exists = favorites.length > 0;
    const isFavorite = desiredState ?? !exists;

    if (isFavorite && !exists) {
      const [products] = await connection.execute(
        'SELECT id FROM products WHERE id = ? AND is_active = TRUE FOR UPDATE', [productId],
      );
      if (!products.length) throw Object.assign(new Error('Produto indisponível.'), { status: 404 });
      await connection.execute('INSERT INTO favorites (user_id, product_id) VALUES (?, ?)', [userId, productId]);
    } else if (!isFavorite && exists) {
      // Permite remover um favorito mesmo se o anúncio tiver sido desativado.
      await connection.execute('DELETE FROM favorites WHERE user_id = ? AND product_id = ?', [userId, productId]);
    }

    await connection.commit();
    return { product_id: productId, is_favorite: isFavorite };
  } catch (error) {
    await connection.rollback();
    throw error;
  } finally {
    connection.release();
  }
}

export async function listFavorites(userId) {
  const [rows] = await pool.execute(`
    SELECT f.id AS favorite_id, f.product_id, f.created_at,
           p.title, p.price, p.image_url, p.video_url, p.user_id, p.category, p.model, p.location,
           u.name AS author_name
    FROM favorites f
    INNER JOIN products p ON p.id = f.product_id
    INNER JOIN users u ON u.id = p.user_id
    WHERE f.user_id = ?
    ORDER BY f.created_at DESC, f.id DESC
  `, [userId]);

  return rows.map(row => ({
    id: row.favorite_id,
    product_id: row.product_id,
    created_at: row.created_at,
    product: {
      id: row.product_id, title: row.title, price: row.price, image_url: row.image_url,
      video_url: row.video_url,
      user_id: row.user_id, category: row.category, model: row.model, location: row.location,
    },
    author: { id: row.user_id, name: row.author_name },
  }));
}
