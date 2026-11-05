# Validação da URL pública e APK automático

Data: 04/11/2026. URL confirmada pelas capturas e consultada por HTTPS: `https://skippecas-api.onrender.com`.

| Rota | Resultado |
| --- | --- |
| GET /health | 200, todos os módulos ok |
| POST /api/auth/login (comprador de demonstração) | 200, token presente |
| POST /api/auth/login (vendedor de demonstração) | 200, token presente |
| GET /api/feed | 200, lista |
| GET /api/marketplace | 200, lista |
| GET /api/favorites | 200 com sessão comprador |
| GET /api/seller/products | 200 com sessão vendedor |
| GET /api/users/search?q=sao | 200 com sessão comprador |
| GET /api/settings/blocked | 200 com sessão comprador |

Não foram criadas contas nem removidos registros. Os logins atualizam a atividade das contas de demonstração. Nenhum token ou segredo foi incluído nesta evidência.

Banco observado: sqlite-memory, mode=memory, volatile=true. Esta aprovação de HTTP não comprova persistência. A migração para MySQL e armazenamento de fotos está documentada no relatório Render.

Build/lint Android concluído com BUILD SUCCESSFUL. BuildConfig inspecionado: API_BASE_URL=https://skippecas-api.onrender.com/api/. APK inspecionado com aapt: versionCode=2, versionName=1.1. A configuração manual e o reaproveitamento de URL salva foram removidos. Preferências de versões anteriores são migradas automaticamente.

O teste do aparelho continua dependendo da instalação do APK novo; não houve instalação remota no dispositivo nesta etapa.

Regressão Android com API real descartável: 28 testes, 0 falhas, 0 erros e 0 ignorados. Inclui teste de regressão que exige a URL Render no build padrão.
