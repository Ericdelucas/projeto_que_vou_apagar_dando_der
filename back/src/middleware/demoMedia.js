import { getDatabaseStatus } from '../config/db.js';

// Dados de demonstração guardam URLs relativas para funcionar no emulador,
// no adb reverse e em qualquer porta usada pela suíte de testes.
export function demoMedia(req, res, next) {
  const json = res.json.bind(res);
  res.json = body => {
    const demo = getDatabaseStatus().engine === 'sqlite-memory';
    const base = `${req.protocol}://${req.get('host')}`;
    function rewrite(value) {
      if (Array.isArray(value)) return value.map(rewrite);
      if (value && typeof value === 'object') {
        return Object.fromEntries(Object.entries(value).map(([key, item]) => [key,
          ['image_url', 'video_url', 'avatar_url'].includes(key) && typeof item === 'string' && (item.startsWith('/uploads/') || (demo && item.startsWith('/demo/')))
            ? base + item : rewrite(item),
        ]));
      }
      return value;
    }
    return json(rewrite(body));
  };
  next();
}
