import 'dotenv/config';
import jwt from 'jsonwebtoken';
import pool from '../config/db.js';

export async function requireAuth(req, res, next) {
  if (!process.env.JWT_SECRET) {
    return res.status(503).json({ message: 'Autenticação indisponível.' });
  }
  const match = /^Bearer ([^\s]+)$/i.exec(req.get('Authorization') || '');
  if (!match) return res.status(401).json({ message: 'Faça login para continuar.' });

  const verificationSecret = process.env.JWT_SECRET;
  let userId;
  try {
    const payload = jwt.verify(match[1], verificationSecret, { algorithms: ['HS256'] });
    userId = Number(payload?.sub);
    if (typeof payload?.sub !== 'string' || !/^\d+$/.test(payload.sub) ||
        !Number.isSafeInteger(userId) || userId < 1 || userId > 2147483647) {
      return res.status(401).json({ message: 'Sessão inválida.' });
    }
  } catch {
    return res.status(401).json({ message: 'Sessão inválida ou expirada. Entre novamente.' });
  }

  try {
    // Sem cache: excluir a conta invalida todos os seus JWTs em TODAS as rotas protegidas,
    // inclusive após reiniciar o servidor ou em outra instância da API.
    const [users] = await pool.query('SELECT id FROM users WHERE id = ? LIMIT 1', [userId]);
    if (process.env.JWT_SECRET !== verificationSecret) {
      return res.status(401).json({ message: 'Ambiente de dados alterado. Entre novamente.' });
    }
    if (!users.length) return res.status(401).json({ message: 'Conta não encontrada. Entre novamente.' });
  } catch (error) {
    console.error('Falha ao verificar sessão:', error.code || error.name);
    return res.status(503).json({ message: 'Não foi possível verificar a sessão. Tente novamente.' });
  }
  req.auth = { userId };
  return next();
}
