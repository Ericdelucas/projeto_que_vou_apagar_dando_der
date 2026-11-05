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
// Render termina TLS no proxy imediatamente à frente do Express.
if (process.env.RENDER === 'true') app.set('trust proxy', 1);
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
app.use((req, res) => res.status(404).json({ message: 'Rota não encontrada.' }));
app.use((error, req, res, next) => {
  if (res.headersSent) return next(error);
  if (error.type === 'entity.too.large') return res.status(413).json({ message: 'O conteúdo enviado excede o limite permitido.' });
  if (error.type === 'entity.parse.failed') return res.status(400).json({ message: 'JSON inválido.' });
  console.error('Falha não tratada na API:', error.code || error.name);
  return res.status(500).json({ message: 'Não foi possível processar a solicitação.' });
});
export default app;
