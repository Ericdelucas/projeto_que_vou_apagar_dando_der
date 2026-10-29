import { listBlockedUsers, blockUser, unblockUser, deleteAccount } from '../models/settingsModel.js';

const validId = value => Number.isInteger(value) && value > 0 && value <= 2147483647;
function reportError(res, error, message) {
  if ([401, 404].includes(error.status)) return res.status(error.status).json({ message: error.message });
  console.error('Falha nas configurações:', error.code || error.name);
  return res.status(500).json({ message });
}

export async function getBlocked(req, res) {
  try { return res.json(await listBlockedUsers(req.auth.userId)); }
  catch (error) { return reportError(res, error, 'Não foi possível carregar os bloqueados.'); }
}

export async function createBlock(req, res) {
  const target = req.body?.blocked_user_id;
  if (!validId(target)) return res.status(400).json({ message: 'Informe blocked_user_id inteiro positivo.' });
  if (target === req.auth.userId) return res.status(400).json({ message: 'Você não pode bloquear a própria conta.' });
  try { return res.status(200).json(await blockUser(req.auth.userId, target)); }
  catch (error) { return reportError(res, error, 'Não foi possível bloquear o usuário.'); }
}

export async function removeBlock(req, res) {
  const id = Number(req.params.id);
  if (!/^\d+$/.test(req.params.id) || !validId(id)) {
    return res.status(400).json({ message: 'Identificador de bloqueio inválido.' });
  }
  try { await unblockUser(req.auth.userId, id); return res.status(204).end(); }
  catch (error) { return reportError(res, error, 'Não foi possível desbloquear o usuário.'); }
}

export async function removeAccount(req, res) {
  try { await deleteAccount(req.auth.userId); return res.status(204).end(); }
  catch (error) { return reportError(res, error, 'Não foi possível excluir a conta. Tente novamente.'); }
}
