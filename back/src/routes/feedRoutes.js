import express, { Router } from 'express';
import { getFeed, getProduct, publishPost, publishPhoto } from '../controllers/feedController.js';

import { requireAuth } from '../middleware/authMiddleware.js';

const router = Router();
router.post('/posts/photo', requireAuth, express.raw({ type: ['image/jpeg', 'image/png', 'image/webp'], limit: '8mb' }), publishPhoto);
router.post('/posts', requireAuth, publishPost);

router.get('/feed', getFeed);
router.get('/products/:id', getProduct);

export default router;
