import { Router } from 'express';
import { getMarketplace, getMarketplaceFilters } from '../controllers/marketplaceController.js';

const router = Router();
router.get('/', getMarketplace);
router.get('/filters', getMarketplaceFilters);

export default router;
