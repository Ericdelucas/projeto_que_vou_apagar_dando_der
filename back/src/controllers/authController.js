import 'dotenv/config';
import bcrypt from 'bcrypt';
import jwt from 'jsonwebtoken';
import { createUser, findByEmail, touchLastActive } from '../models/userModel.js';

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function normalizeEmail(email) {
  return typeof email === 'string' ? email.trim().toLowerCase() : '';
}

export async function register(req, res) {
  const { name, password, role = 'comprador' } = req.body ?? {};
  const email = normalizeEmail(req.body?.email);
  const normalizedName = typeof name === 'string' ? name.trim() : '';

  // bcrypt considera até 72 bytes; rejeitamos senhas que seriam truncadas.
  if (
    !normalizedName || normalizedName.length > 255 ||
    !emailPattern.test(email) || email.length > 255 ||
    typeof password !== 'string' || password.length < 8 ||
    Buffer.byteLength(password, 'utf8') > 72 ||
    !['comprador', 'vendedor'].includes(role)
  ) {
    return res.status(400).json({
      message: 'Informe nome (até 255 caracteres), e-mail válido, senha com pelo menos 8 caracteres e até 72 bytes, e role comprador ou vendedor.',
    });
  }

  try {
    if (await findByEmail(email)) {
      return res.status(409).json({ message: 'E-mail já cadastrado.' });
    }

    const user = await createUser({ name: normalizedName, email, password, role });
    return res.status(201).json({ user });
  } catch (error) {
    // A restrição UNIQUE também protege registros concorrentes do mesmo e-mail.
    if (error.code === 'ER_DUP_ENTRY') {
      return res.status(409).json({ message: 'E-mail já cadastrado.' });
    }
    console.error('Falha ao registrar usuário:', error.code || error.name);
    return res.status(500).json({ message: 'Não foi possível registrar o usuário.' });
  }
}

export async function login(req, res) {
  const email = normalizeEmail(req.body?.email);
  const { password } = req.body ?? {};

  if (
    !emailPattern.test(email) || email.length > 255 ||
    typeof password !== 'string' || !password ||
    Buffer.byteLength(password, 'utf8') > 72
  ) {
    return res.status(400).json({ message: 'Informe e-mail e senha válidos.' });
  }

  if (!process.env.JWT_SECRET) {
    console.error('JWT_SECRET não configurado.');
    return res.status(500).json({ message: 'Serviço de autenticação indisponível.' });
  }

  try {
    const user = await findByEmail(email);
    // A mesma resposta é usada para usuário inexistente e senha incorreta.
    if (!user || !(await bcrypt.compare(password, user.password))) {
      return res.status(401).json({ message: 'E-mail ou senha inválidos.' });
    }

    await touchLastActive(user.id);
    const token = jwt.sign({ role: user.role }, process.env.JWT_SECRET, {
      algorithm: 'HS256',
      subject: String(user.id),
      expiresIn: '1h',
    });

    return res.status(200).json({
      token,
      user: { id: user.id, name: user.name, email: user.email, role: user.role },
    });
  } catch (error) {
    console.error('Falha ao autenticar usuário:', error.code || error.name);
    return res.status(500).json({ message: 'Não foi possível realizar o login.' });
  }
}
