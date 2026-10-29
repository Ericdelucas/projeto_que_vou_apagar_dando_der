import { createSellerProduct, listSellerProducts } from '../models/sellerModel.js';

function text(value, field, required = true) {
  if (!required && (value === undefined || value === null || value === '')) return null;
  if (typeof value !== 'string' || !value.trim() || value.trim().length > 255) {
    throw new Error(`Informe ${field} com até 255 caracteres.`);
  }
  return value.trim();
}

function mediaUrl(value, field) {
  if (value === undefined || value === null || value === '') return null;
  if (typeof value !== 'string' || value.length > 2048) throw new Error(`${field} inválida.`);
  try {
    const url = new URL(value.trim());
    if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password) throw new Error();
    return url.href;
  } catch { throw new Error(`Informe uma URL HTTP/HTTPS válida em ${field}.`); }
}

export async function getSellerProducts(req, res) {
  try {
    return res.status(200).json(await listSellerProducts(req.auth.userId));
  } catch (error) {
    console.error('Falha ao listar anúncios:', error.code || error.name);
    return res.status(500).json({ message: 'Não foi possível carregar seus anúncios.' });
  }
}

export async function postSellerProduct(req, res) {
  let product;
  try {
    const body = req.body ?? {};
    const price = typeof body.price === 'number' ? String(body.price) : body.price;
    if (typeof price !== 'string' || !/^\d{1,8}(\.\d{1,2})?$/.test(price) || Number(price) <= 0) {
      throw new Error('Informe um preço positivo, até 99999999.99, com até duas casas decimais.');
    }
    product = {
      title: text(body.title, 'título'), price,
      category: text(body.category, 'categoria'), model: text(body.model, 'modelo'),
      image_url: mediaUrl(body.image_url, 'image_url'),
      video_url: mediaUrl(body.video_url, 'video_url'),
      location: text(body.location, 'localização', false),
    };
    if (!product.image_url && !product.video_url) throw new Error('Informe image_url ou video_url.');
  } catch (error) {
    return res.status(400).json({ message: error.message });
  }
  try {
    const created = await createSellerProduct(req.auth.userId, product);
    return res.status(201).json(created);
  } catch (error) {
    console.error('Falha ao cadastrar anúncio:', error.code || error.name);
    return res.status(500).json({ message: 'Não foi possível cadastrar o anúncio.' });
  }
}
