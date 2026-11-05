# Contribuição e revisão — SkipPeças

Preserve o escopo Android + API REST. Antes de alterar comportamento, vincule a mudança a um RF/RNF de `docs/PRODUCAO_TECNICA.md` e atualize o teste/contrato correspondente.

Para novas funcionalidades, crie uma branch `feat/<descricao>`; para correções, `fix/<descricao>`. Faça commits pequenos e claros, abra revisão e descreva comportamento, validação e limitações. Essa é a regra daqui em diante; o histórico anterior não foi reescrito para simular branches.

Verificação de backend: `cd back && npm ci && npm test`. A integração Android descartável é `npm run test:android:memory`, com JAVA_HOME/SDK configurados. No projeto Android, execute `./gradlew assembleDebug testDebugUnitTest lintDebug --no-configuration-cache`.

Não versione `.env`, dependências, caches, APKs, senhas ou keystores. Registre resultados e responsáveis em `docs/evidencias/`; capturas não devem revelar credenciais ou dados pessoais. Mudanças no schema exigem migração para base existente e atualização do consolidado, DER e dicionário (`python3 scripts/gerar_modelos.py`).

Cada integrante deve revisar e explicar os trechos pelos quais responde. Um relatório gerado e testes automáticos não substituem essa revisão.
