import pool from '../config/db.js';

export async function requireSeller(req, res, next) {
  try {
    // Consulta o papel atual no banco, sem confiar em role enviado no corpo ou token antigo.
    const [rows] = await pool.execute('SELECT role FROM users WHERE id = ? LIMIT 1', [req.auth.userId]);
    if (!rows.length) return res.status(401).json({ message: 'Sessão inválida.' });
    if (rows[0].role !== 'vendedor') {
      return res.status(403).json({ message: 'Esta área está disponível apenas para vendedores.' });
    }
    return next();
  } catch (error) {
    console.error('Falha ao verificar vendedor:', error.code || error.name);
    return res.status(500).json({ message: 'Não foi possível verificar o acesso.' });
  }
}
