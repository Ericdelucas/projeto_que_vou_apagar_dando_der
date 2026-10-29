import { listFavorites, setFavorite } from '../models/favoriteModel.js';

export async function toggleFavorite(req, res) {
  const { product_id: productId, is_favorite: desiredState } = req.body ?? {};
  if (!Number.isInteger(productId) || productId < 1 || productId > 2147483647 ||
      (desiredState !== undefined && typeof desiredState !== 'boolean')) {
    return res.status(400).json({ message: 'Informe product_id inteiro positivo e is_favorite booleano, quando presente.' });
  }
  try {
    return res.status(200).json(await setFavorite(req.auth.userId, productId, desiredState));
  } catch (error) {
    if ([401, 404].includes(error.status)) {
      return res.status(error.status).json({ message: error.message });
    }
    console.error('Falha ao alterar favorito:', error.code || error.name);
    return res.status(500).json({ message: 'Não foi possível atualizar o favorito.' });
  }
}

export async function getFavorites(req, res) {
  try {
    return res.status(200).json(await listFavorites(req.auth.userId));
  } catch (error) {
    console.error('Falha ao carregar favoritos:', error.code || error.name);
    return res.status(500).json({ message: 'Não foi possível carregar os salvos.' });
  }
}
