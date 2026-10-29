import { Router } from 'express';
import { requireAuth } from '../middleware/authMiddleware.js';
import { requireSeller } from '../middleware/sellerMiddleware.js';
import { getSellerProducts, postSellerProduct } from '../controllers/sellerController.js';

const router = Router();
router.use(requireAuth, requireSeller);
router.get('/products', getSellerProducts);
router.post('/products', postSellerProduct);
export default router;
