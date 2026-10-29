import { randomUUID } from 'node:crypto';
import { mkdir, writeFile, unlink } from 'node:fs/promises';
import { createPost, findProductById, listPosts } from '../models/feedModel.js';

export async function getFeed(req, res) {
  try {
    return res.status(200).json(await listPosts());
  } catch (error) {
    console.error('Falha ao carregar feed:', error.code || error.name);
    return res.status(500).json({ message: 'Não foi possível carregar o feed.' });
  }
}

export async function getProduct(req, res) {
  const id = Number(req.params.id);
  if (!/^\d+$/.test(req.params.id) || !Number.isSafeInteger(id) || id < 1 || id > 2147483647) {
    return res.status(400).json({ message: 'ID de produto inválido.' });
  }

  try {
    const product = await findProductById(id);
    if (!product) {
      return res.status(404).json({ message: 'Produto não encontrado.' });
    }
    return res.status(200).json(product);
  } catch (error) {
    console.error('Falha ao carregar produto:', error.code || error.name);
    return res.status(500).json({ message: 'Não foi possível carregar o produto.' });
  }
}

export async function publishPost(req, res) {
  const body = req.body ?? {};
  const description = typeof body.description === 'string' ? body.description.trim() : '';
  const media = {};
  if (!description || description.length > 2000 || Object.keys(body).some(key => !['description', 'image_url', 'video_url'].includes(key))) {
    return res.status(400).json({ message: 'Informe uma descrição de até 2000 caracteres.' });
  }
  for (const key of ['image_url', 'video_url']) {
    const value = body[key];
    if (value == null || value === '') { media[key] = null; continue; }
    try {
      const url = new URL(value);
      if (typeof value !== 'string' || value.length > 2048 || !['http:', 'https:'].includes(url.protocol) || url.username || url.password) throw new Error();
      media[key] = value;
    } catch { return res.status(400).json({ message: 'Informe URLs de mídia HTTP ou HTTPS válidas.' }); }
  }
  try {
    return res.status(201).json(await createPost(req.auth.userId, { description, ...media }));
  } catch (error) {
    console.error('Falha ao publicar:', error.code || error.name);
    return res.status(500).json({ message: 'Não foi possível publicar.' });
  }
}

export async function publishPhoto(req, res) {
  const description = typeof req.query.description === 'string' ? req.query.description.trim() : '';
  if (description.length > 2000 || !Buffer.isBuffer(req.body) || req.body.length < 12) {
    return res.status(400).json({ message: 'Selecione uma foto válida e uma legenda de até 2000 caracteres.' });
  }
  const data = req.body;
  let extension;
  if (data[0] === 0xff && data[1] === 0xd8 && data[2] === 0xff) extension = 'jpg';
  else if (data.subarray(0, 8).equals(Buffer.from([137,80,78,71,13,10,26,10]))) extension = 'png';
  else if (data.toString('ascii', 0, 4) === 'RIFF' && data.toString('ascii', 8, 12) === 'WEBP') extension = 'webp';
  if (!extension || req.get('Content-Type')?.split(';')[0] !== `image/${extension === 'jpg' ? 'jpeg' : extension}`) {
    return res.status(400).json({ message: 'Envie uma foto JPEG, PNG ou WebP válida.' });
  }
  const directory = new URL('../../uploads/', import.meta.url);
  const name = `${randomUUID()}.${extension}`;
  const file = new URL(name, directory);
  try {
    await mkdir(directory, { recursive: true });
    await writeFile(file, data, { flag: 'wx' });
    const post = await createPost(req.auth.userId, { description: description || 'Foto', image_url: `/uploads/${name}`, video_url: null });
    return res.status(201).json(post);
  } catch (error) {
    await unlink(file).catch(() => {});
    console.error('Falha ao publicar foto:', error.code || error.name);
    return res.status(500).json({ message: 'Não foi possível publicar a foto.' });
  }
}
