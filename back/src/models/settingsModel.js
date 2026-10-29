import pool from '../config/db.js';

const failure = (status, message) => Object.assign(new Error(message), { status });
const projection = `SELECT b.id, b.blocked_user_id, b.created_at,
                           u.name, u.role, u.avatar_url
                    FROM blocked_users b JOIN users u ON u.id = b.blocked_user_id`;
const publicBlock = ({ id, blocked_user_id, created_at, name, role, avatar_url }) => ({
  id, blocked_user_id, created_at,
  user: { id: blocked_user_id, name, role, avatar_url },
});

async function transaction(action) {
  const connection = await pool.getConnection();
  try {
    await connection.beginTransaction();
    const result = await action(connection);
    await connection.commit();
    return result;
  } catch (error) {
    await connection.rollback();
    throw error;
  } finally {
    connection.release();
  }
}

async function lockUser(connection, userId) {
  const [users] = await connection.execute('SELECT id FROM users WHERE id = ? FOR UPDATE', [userId]);
  if (!users.length) throw failure(401, 'Conta não encontrada. Entre novamente.');
}

export function listBlockedUsers(userId) {
  return transaction(async connection => {
    await lockUser(connection, userId);
    const [rows] = await connection.execute(
      `${projection} WHERE b.user_id = ? ORDER BY b.created_at DESC, b.id DESC`, [userId],
    );
    return rows.map(publicBlock);
  });
}

export function blockUser(userId, blockedUserId) {
  return transaction(async connection => {
    // Ordem fixa reduz deadlocks em bloqueios recíprocos simultâneos.
    const [users] = await connection.execute(
      'SELECT id FROM users WHERE id IN (?, ?) ORDER BY id FOR UPDATE',
      [userId, blockedUserId],
    );
    if (!users.some(user => user.id === userId)) throw failure(401, 'Conta não encontrada. Entre novamente.');
    if (!users.some(user => user.id === blockedUserId)) throw failure(404, 'Usuário não encontrado.');
    // Repetir o pedido mantém o mesmo bloqueio, sem alternar o estado.
    await connection.execute(`INSERT INTO blocked_users (user_id, blocked_user_id) VALUES (?, ?)
      ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id)`, [userId, blockedUserId]);
    const [rows] = await connection.execute(
      `${projection} WHERE b.user_id = ? AND b.blocked_user_id = ?`, [userId, blockedUserId],
    );
    return publicBlock(rows[0]);
  });
}

export function unblockUser(userId, blockId) {
  return transaction(async connection => {
    await lockUser(connection, userId);
    const [result] = await connection.execute(
      'DELETE FROM blocked_users WHERE id = ? AND user_id = ?', [blockId, userId],
    );
    if (!result.affectedRows) throw failure(404, 'Bloqueio não encontrado.');
  });
}

export function deleteAccount(userId) {
  return transaction(async connection => {
    await lockUser(connection, userId);
    // Remove explicitamente as relações do usuário antes das FKs sem cascade.
    await connection.execute('DELETE FROM blocked_users WHERE user_id = ? OR blocked_user_id = ?', [userId, userId]);
    await connection.execute('DELETE FROM user_follows WHERE follower_id = ? OR followed_id = ?', [userId, userId]);
    await connection.execute('DELETE FROM favorites WHERE user_id = ?', [userId]);
    await connection.execute('DELETE FROM product_requests WHERE buyer_id = ?', [userId]);
    await connection.execute('DELETE FROM posts WHERE user_id = ?', [userId]);
    // As FKs removem favoritos/solicitações desses produtos e desvinculam posts de terceiros.
    await connection.execute('DELETE FROM products WHERE user_id = ?', [userId]);
    await connection.execute('DELETE FROM users WHERE id = ?', [userId]);
  });
}
