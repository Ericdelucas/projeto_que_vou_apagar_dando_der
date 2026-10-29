import { getUserProfile, searchUsers, updateUserProfile } from '../models/userProfileModel.js';

export async function search(req, res) {
  const { q = '', filter = '' } = req.query;
  if (typeof q !== 'string' || q.trim().length > 255 || typeof filter !== 'string' ||
      !['', 'todos', 'comprador', 'vendedor'].includes(filter)) {
    return res.status(400).json({ message: 'Informe uma busca de até 255 caracteres e filtro todos, comprador ou vendedor.' });
  }
  try {
    return res.json(await searchUsers(q.trim(), ['', 'todos'].includes(filter) ? null : filter));
  } catch (error) {
    console.error('Falha ao buscar usuários:', error.code || error.name);
    return res.status(500).json({ message: 'Não foi possível buscar usuários.' });
  }
}

export async function profile(req, res) {
  const id = req.params.userId === undefined ? req.auth.userId : Number(req.params.userId);
  if (!Number.isInteger(id) || id < 1 || id > 2147483647 ||
      (req.params.userId !== undefined && !/^\d+$/.test(req.params.userId))) {
    return res.status(400).json({ message: 'ID de usuário inválido.' });
  }
  try {
    const user = await getUserProfile(id, req.auth.userId);
    if (!user) return res.status(404).json({ message: 'Usuário não encontrado.' });
    return res.json(user);
  } catch (error) {
    console.error('Falha ao carregar perfil:', error.code || error.name);
    return res.status(500).json({ message: 'Não foi possível carregar o perfil.' });
  }
}

function validateFields(body) {
  if (!body || typeof body !== 'object' || Array.isArray(body) || !Object.keys(body).length ||
      Object.keys(body).some(key => !['name', 'email', 'avatar_url', 'bio'].includes(key))) {
    throw new Error('Envie apenas name, email, avatar_url ou bio.');
  }
  const fields = {};
  if ('name' in body) {
    if (typeof body.name !== 'string' || !body.name.trim() || body.name.trim().length > 255) throw new Error('Nome inválido.');
    fields.name = body.name.trim();
  }
  if ('email' in body) {
    if (typeof body.email !== 'string' || body.email.trim().length > 255 ||
        !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(body.email.trim())) throw new Error('E-mail inválido.');
    fields.email = body.email.trim().toLowerCase();
  }
  if ('bio' in body) {
    if (body.bio !== null && (typeof body.bio !== 'string' || body.bio.trim().length > 500)) throw new Error('Biografia inválida.');
    fields.bio = body.bio?.trim() || null;
  }
  if ('avatar_url' in body) {
    if (body.avatar_url === null || body.avatar_url === '') fields.avatar_url = null;
    else {
      if (typeof body.avatar_url !== 'string' || body.avatar_url.length > 2048) throw new Error('URL do avatar inválida.');
      let url;
      try { url = new URL(body.avatar_url.trim()); } catch { throw new Error('URL do avatar inválida.'); }
      if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password) throw new Error('URL do avatar inválida.');
      fields.avatar_url = url.href;
    }
  }
  return fields;
}

export async function updateProfile(req, res) {
  let fields;
  try { fields = validateFields(req.body); }
  catch (error) { return res.status(400).json({ message: error.message }); }
  try {
    if (!(await updateUserProfile(req.auth.userId, fields))) {
      return res.status(404).json({ message: 'Usuário não encontrado.' });
    }
    const user = await getUserProfile(req.auth.userId, req.auth.userId);
    if (!user) return res.status(404).json({ message: 'Usuário não encontrado.' });
    return res.json(user);
  } catch (error) {
    if (error.code === 'ER_DUP_ENTRY') return res.status(409).json({ message: 'E-mail já cadastrado.' });
    console.error('Falha ao atualizar perfil:', error.code || error.name);
    return res.status(500).json({ message: 'Não foi possível atualizar o perfil.' });
  }
}
