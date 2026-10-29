import { Router } from 'express';
import { requireAuth } from '../middleware/authMiddleware.js';
import { getFavorites, toggleFavorite } from '../controllers/favoritesController.js';

const router = Router();
router.use(requireAuth);
router.get('/', getFavorites);
router.post('/', toggleFavorite);
export default router;
