import { listFilterOptions, listProducts } from '../models/marketplaceModel.js';

function parseFilters(query) {
  const filters = {};
  for (const key of ['model', 'category', 'location']) {
    const value = query[key];
    if (value === undefined) continue;
    if (typeof value !== 'string' || value.trim().length > 255) {
      throw new Error(`Filtro ${key} inválido.`);
    }
    if (value.trim()) filters[key] = value.trim();
  }

  // Mantém o valor decimal em string até o driver MySQL, sem arredondá-lo.
  for (const key of ['min_price', 'max_price']) {
    const value = query[key];
    if (value === undefined) continue;
    if (typeof value !== 'string' || !/^\d{1,8}(\.\d{1,2})?$/.test(value)) {
      throw new Error(`Filtro ${key} deve ser um preço entre 0 e 99999999.99, com até duas casas decimais.`);
    }
    filters[key] = value;
  }
  if (filters.min_price !== undefined && filters.max_price !== undefined &&
      Number(filters.min_price) > Number(filters.max_price)) {
    throw new Error('O preço mínimo não pode exceder o máximo.');
  }
  return filters;
}

export async function getMarketplace(req, res) {
  let filters;
  try {
    filters = parseFilters(req.query);
  } catch (error) {
    return res.status(400).json({ message: error.message });
  }

  try {
    return res.status(200).json(await listProducts(filters));
  } catch (error) {
    console.error('Falha ao carregar Marketplace:', error.code || error.name);
    return res.status(500).json({ message: 'Não foi possível carregar os anúncios.' });
  }
}

export async function getMarketplaceFilters(req, res) {
  try {
    return res.status(200).json(await listFilterOptions());
  } catch (error) {
    console.error('Falha ao carregar filtros:', error.code || error.name);
    return res.status(500).json({ message: 'Não foi possível carregar os filtros.' });
  }
}
