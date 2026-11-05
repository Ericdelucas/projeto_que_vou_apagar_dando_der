import { spawn } from 'node:child_process';
import { once } from 'node:events';

// Fixture descartável: não lê .env e nunca acessa MySQL ou dados persistentes.
process.env.DOTENV_CONFIG_PATH = '/dev/null';
process.env.DB_MODE = 'memory';
const { default: app } = await import('../../src/app.js');
const { default: pool } = await import('../../src/config/db.js');
let server;
try {
  await pool.initialize();
  server = app.listen(0, '127.0.0.1');
  await once(server, 'listening');
  const child = spawn('./gradlew', ['--offline', '--no-configuration-cache',
    'testDebugUnitTest', '--rerun-tasks'], {
    cwd: new URL('../../../front/projeto_que_vou_apagar_dando_der/', import.meta.url),
    env: { ...process.env, SKIPPECAS_E2E_URL: `http://127.0.0.1:${server.address().port}/api/` },
    stdio: 'inherit',
  });
  const timer = setTimeout(() => child.kill('SIGTERM'), 180000);
  try {
    const [code] = await once(child, 'exit');
    process.exitCode = code ?? 1;
  } finally { clearTimeout(timer); }
} finally {
  if (server) await new Promise(resolve => server.close(resolve));
  await pool.end();
}
