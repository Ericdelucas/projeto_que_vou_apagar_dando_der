import express from 'express';
import cors from 'cors';
import { fileURLToPath } from 'node:url';
import { demoMedia } from './middleware/demoMedia.js';
import authRoutes from './routes/authRoutes.js';
import feedRoutes from './routes/feedRoutes.js';
import marketplaceRoutes from './routes/marketplaceRoutes.js';
import favoritesRoutes from './routes/favoritesRoutes.js';
import sellerRoutes from './routes/sellerRoutes.js';
import userRoutes from './routes/userRoutes.js';
import settingsRoutes from './routes/settingsRoutes.js';
import { health } from './controllers/healthController.js';

const app = express();
app.use(cors());
app.use(express.json());
app.use('/demo', express.static(fileURLToPath(new URL('../public/demo/', import.meta.url))));
app.use(demoMedia);
app.use('/uploads', express.static(fileURLToPath(new URL('../uploads/', import.meta.url)), {
  setHeaders(res) { res.set('X-Content-Type-Options', 'nosniff'); },
}));
app.use('/api/auth', authRoutes);
app.use('/api', feedRoutes);
app.use('/api/marketplace', marketplaceRoutes);
app.use('/api/favorites', favoritesRoutes);
app.use('/api/seller', sellerRoutes);
app.use('/api/users', userRoutes);
app.use('/api/settings', settingsRoutes);
app.get('/health', health);
app.use((error, req, res, next) => {
  if (error.type === 'entity.too.large') return res.status(413).json({ message: 'A foto deve ter no máximo 8 MB.' });
  return next(error);
});
export default app;
