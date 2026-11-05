import express, { Router } from 'express';
import { requireAuth } from '../middleware/authMiddleware.js';
import { search, profile, updateProfile, uploadAvatar } from '../controllers/userController.js';

const router = Router();
router.use(requireAuth);
router.get('/search', search);
router.get('/me', profile);
router.get('/profile/:userId', profile);
router.put('/profile', updateProfile);
router.post('/avatar', express.raw({ type: ['image/jpeg', 'image/png', 'image/webp'], limit: '8mb' }), uploadAvatar);
export default router;
