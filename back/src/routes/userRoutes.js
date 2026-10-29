import { Router } from 'express';
import { requireAuth } from '../middleware/authMiddleware.js';
import { search, profile, updateProfile } from '../controllers/userController.js';

const router = Router();
router.use(requireAuth);
router.get('/search', search);
router.get('/me', profile);
router.get('/profile/:userId', profile);
router.put('/profile', updateProfile);
export default router;
