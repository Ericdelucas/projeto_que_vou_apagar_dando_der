import { Router } from 'express';
import { requireAuth } from '../middleware/authMiddleware.js';
import { getBlocked, createBlock, removeBlock, removeAccount } from '../controllers/settingsController.js';

const router = Router();
router.use(requireAuth);
router.get('/blocked', getBlocked);
router.post('/block', createBlock);
router.delete('/block/:id', removeBlock);
router.delete('/account', removeAccount);
export default router;
