import pool from '../config/db.js';

export async function searchUsers(query, filter) {
  // Escapa curingas: % e _ digitados pelo usuário são tratados como texto.
  const pattern = `%${query.replace(/[!%_]/g, '!$&')}%`;
  const [rows] = await pool.execute(`
    SELECT id, name, role, avatar_url, last_active_at,
           (SELECT GROUP_CONCAT(p.created_at) FROM posts p WHERE p.user_id = users.id) AS activity_dates,
           CASE WHEN last_active_at >= CURRENT_TIMESTAMP - INTERVAL 15 MINUTE
                THEN 'recente' ELSE 'inativo' END AS activity_status
    FROM users
    WHERE (name LIKE ? ESCAPE '!' OR email LIKE ? ESCAPE '!')
      ${filter ? 'AND role = ?' : ''}
    ORDER BY name, id LIMIT 100
  `, filter ? [pattern, pattern, filter] : [pattern, pattern]);
  const today = new Date(); today.setUTCHours(0, 0, 0, 0);
  return rows.map(({ activity_dates, ...user }) => {
    const activity = Array(14).fill(0);
    for (const stamp of String(activity_dates || '').split(',')) {
      const date = new Date(stamp.slice(0, 10) + 'T00:00:00Z');
      const index = 13 - Math.round((today - date) / 86400000);
      if (index >= 0 && index < 14) activity[index]++;
    }
    return { ...user, activity, activity_count: activity.reduce((sum, count) => sum + count, 0) };
  });
}

export async function getUserProfile(userId, viewerId) {
  const [rows] = await pool.execute(`
    SELECT u.id, u.name, u.email, u.role, u.avatar_url, u.bio, u.last_active_at,
           (SELECT COUNT(*) FROM posts WHERE user_id = u.id) AS posts_count,
           (SELECT COUNT(*) FROM user_follows WHERE followed_id = u.id) AS followers_count,
           (SELECT COUNT(*) FROM user_follows WHERE follower_id = u.id) AS following_count
    FROM users u WHERE u.id = ? LIMIT 1
  `, [userId]);
  if (!rows.length) return null;
  const user = rows[0];
  const [media] = await pool.execute(`
    SELECT CONCAT('post:', p.id) AS media_id, 'post' AS kind, p.id AS sort_id,
           p.id AS post_id, pr.id AS product_id,
           COALESCE(pr.title, p.description, 'Publicação') AS title,
           COALESCE(p.image_url, pr.image_url) AS image_url, COALESCE(NULLIF(p.video_url, ''), pr.video_url) AS video_url
    FROM posts p
    LEFT JOIN products pr ON pr.id = p.product_id AND pr.is_active = TRUE
    WHERE p.user_id = ?
    UNION ALL
    SELECT CONCAT('product:', pr.id), 'product', pr.id, NULL, pr.id,
           pr.title, pr.image_url, pr.video_url
    FROM products pr
    WHERE pr.user_id = ? AND pr.is_active = TRUE
      AND NOT EXISTS (SELECT 1 FROM posts p WHERE p.product_id = pr.id AND p.user_id = ?)
    ORDER BY sort_id DESC, kind
  `, [userId, userId, userId]);
  return {
    id: user.id, name: user.name, role: user.role, avatar_url: user.avatar_url,
    bio: user.bio, last_active_at: user.last_active_at,
    is_owner: userId === viewerId,
    ...(userId === viewerId ? { email: user.email } : {}),
    posts_count: Number(user.posts_count), followers_count: Number(user.followers_count),
    following_count: Number(user.following_count),
    media: media.map(({ sort_id, ...item }) => item),
  };
}

export async function updateUserProfile(userId, fields) {
  const keys = Object.keys(fields);
  // A whitelist também se aplica no model, antes de construir nomes de colunas.
  if (!keys.length || keys.some(key => !['name', 'email', 'avatar_url', 'bio'].includes(key))) {
    throw new Error('Campos de perfil inválidos.');
  }
  const [result] = await pool.execute(
    `UPDATE users SET ${keys.map(key => `${key} = ?`).join(', ')}, last_active_at = CURRENT_TIMESTAMP WHERE id = ?`,
    [...keys.map(key => fields[key]), userId],
  );
  if (result.affectedRows > 0) return true;
  // Uma atualização sem mudanças também é válida (dependendo das flags do driver).
  const [rows] = await pool.execute('SELECT id FROM users WHERE id = ? LIMIT 1', [userId]);
  return rows.length > 0;
}
