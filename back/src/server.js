import 'dotenv/config';
import pool from './config/db.js';
import app from './app.js';

const port = process.env.PORT || 3000;
try {
  await pool.initialize();
  console.log(`Banco ativo: ${pool.status().engine}.`);
} catch (error) {
  console.error('Falha ao inicializar banco:', error.code || error.message);
}
const server = app.listen(port, error => {
  if (!error) {
    console.log(`SkipPeças API rodando na porta ${port}`);
    return;
  }
  console.error(error.code === 'EADDRINUSE'
    ? `A porta ${port} já está em uso. Confira http://localhost:${port}/health: o backend pode já estar ligado.`
    : `Não foi possível iniciar a API: ${error.code || error.message}`);
  process.exitCode = 1;
  pool.end().catch(closeError => {
    console.error('Falha ao encerrar banco:', closeError.code || closeError.name);
  });
});

for (const signal of ['SIGINT', 'SIGTERM']) {
  process.once(signal, () => server.close(() => {
    pool.end().catch(error => {
      console.error('Falha ao encerrar banco:', error.code || error.name);
      process.exitCode = 1;
    });
  }));
}
