# SkipPeças — relatório de pré-deploy e guia Render

**Foto de perfil ainda falhando?** Foi confirmado HTTP 404 na rota pública de avatar; o commit que contém a rota já está no GitHub, mas precisa ser implantado. Veja [passo a passo da correção e APK 1.3](docs/FOTO_PERFIL.md).

**Atualização de interface 1.2:** filtros, tema escuro e mídia HTTPS corrigidos. A troca de foto da galeria depende de publicar a nova rota de backend; veja [passos exatos](docs/CORRECOES_INTERFACE.md). As instruções da versão 1.1 abaixo são históricas.

## Atualização: APK conectado automaticamente à Render

O serviço foi verificado em `https://skippecas-api.onrender.com/health`: HTTP 200, todos os módulos saudáveis, banco SQLite em memória (`DB_MODE=memory`, `volatile=true`). O APK versão 1.1 (versionCode 2) agora inclui `https://skippecas-api.onrender.com/api/` como URL padrão. O botão de configuração manual foi removido; preferências antigas de endereço são descartadas na atualização. Instale o APK novo sobre o antigo.

Para resolver a conexão atual, **nenhuma mudança na Render é necessária**: o serviço já responde. As instruções anteriores sobre URL ainda não fornecida ou configuração dentro do aplicativo foram superadas por esta atualização. O histórico abaixo registra as etapas anteriores.

### O que conferir no painel da Render

Em **skippecas-api → Settings**, mantenha:

| Campo | Valor |
| --- | --- |
| Branch | `main` |
| Root Directory | `back` |
| Build Command | `npm install` |
| Start Command | `npm start` |
| Health Check Path | `/health` |
| Render Subdomain | Enabled |

O prefixo `back/ $` exibido pelo painel representa o diretório/prompt, não faz parte do comando. Não coloque `/api` no Health Check Path; `/api/` pertence à URL base do aplicativo.

Em **Environment**, confira `NODE_ENV=production`, `PORT=10000`, `NODE_VERSION=24.21.0`. O serviço está comprovadamente em `DB_MODE=memory`; pode mantê-lo para demonstração sem MySQL. Não é necessário criar variável de frontend, configurar CORS no painel ou trocar os comandos para conectar este APK.

### Para cadastros não desaparecerem

O banco atual é temporário. Antes de usar contas reais, disponibilize um MySQL persistente, aplique o schema e vá a **Environment → Edit/Add Environment Variable**:

| Chave | O que colocar |
| --- | --- |
| `DB_MODE` | `mysql`, somente depois de o banco estar pronto |
| `DB_HOST` | Host real fornecido pelo provedor MySQL; não localhost |
| `DB_PORT` | Porta fornecida pelo provedor, normalmente 3306 |
| `DB_NAME` | Nome do banco com o schema SkipPeças aplicado |
| `DB_USER` | Usuário desse banco |
| `DB_PASSWORD` | Senha desse usuário, apenas no painel |
| `JWT_SECRET` | Segredo aleatório longo e estável, apenas no painel |

Se precisar gerar JWT_SECRET, execute `node -e "console.log(require('node:crypto').randomBytes(48).toString('hex'))"` localmente e copie o resultado para o campo. Não use o texto do comando como valor. Salve as variáveis e use **Manual Deploy → Deploy latest commit** se o painel não iniciar o deploy. O cliente atual ainda exige ajuste de TLS se o provedor MySQL obrigar certificados/opções específicas.

Após migrar, `/health` deve indicar `engine=mysql`, `volatile=false` e todos os módulos em `ok`. Não altere para mysql sem preencher as credenciais e aplicar o schema: isso deixará a API indisponível. Fotos também exigem armazenamento persistente, conforme a seção específica deste relatório.

### Inicialização do plano Free

O plano Free pode suspender o serviço após inatividade e levar cerca de um minuto para acordar. O cliente passou a aguardar até 100 segundos por chamada, sem repetir automaticamente cadastros. Isso não garante disponibilidade contínua. Fonte: [limites do plano Free](https://render.com/docs/free).

### Hook exposto na captura

A captura mostrou o Deploy Hook. Em **Settings → Deploy → Deploy Hook → Regenerate hook**, gere outro para invalidar o endereço exposto. Não compartilhe o novo hook nem o use como URL do APK.


Data: 04/11/2026. Ambiente local: Node.js 24.21.0.

## Resultado da validação

A suíte `cd back && npm test` terminou com **18 testes aprovados, zero falhas**, em aproximadamente 8,7 segundos. Inclui autenticação, feed, marketplace, favoritos, vendedor, usuários, configurações, saúde, isolamento de sessões, transações e fallback.

O novo `back/tests/smoke.test.js` inicia um subprocesso real de `src/server.js`, com `NODE_ENV=production`, porta HTTP dinâmica e MySQL deliberadamente indisponível. Não usa o `.env` local e encerra o processo após o teste. Execute isoladamente com `cd back && npm run test:smoke`.

| Requisição | Resultado observado |
| --- | --- |
| GET /health | 200; todos os módulos ok; sqlite-memory; volatile=true; reason=ECONNREFUSED |
| POST /api/auth/login | 200 para comprador e vendedor; tokens presentes |
| POST /api/auth/login com senha inválida | 401 |
| GET /api/feed | 200; lista preenchida |
| GET /api/marketplace | 200; lista preenchida |
| GET /api/favorites | 200 autenticado; 401 sem token |
| GET /api/seller/products | 200 vendedor; 401 sem token; 403 comprador |
| GET /api/users/search?q=sao&filter=vendedor | 200 autenticado; resultado preenchido; 401 sem token |
| GET /api/settings/blocked | 200 autenticado; lista preenchida; 401 sem token |
| OPTIONS /api/favorites | 204 |

Todas as respostas acima tiveram `Access-Control-Allow-Origin: *`. A inicialização com fallback e o encerramento por SIGTERM passaram sem crash. Os logs por rota são exibidos pelo runner no terminal.

A suíte separada `npm run test:e2e` foi tentada com `DB_MODE=mysql`, banco isolado `skippecas_e2e_predeploy_20261104` e localhost:3306. **Não aprovada neste ambiente:** ECONNREFUSED, pois não há MySQL acessível nessa porta. Nenhum banco foi criado por essa tentativa. Não há garantia de funcionamento integral em produção sem validar o MySQL e o serviço publicado. Testes do APK/dispositivo e deploy remoto não foram executados.

## Configuração entregue

- `render.yaml` na raiz substitui o antigo `back/render.yaml`, evitando duas fontes de configuração.
- Web Service `skippecas-api`, runtime Node, `rootDir: back`, build `npm install`, start `npm start` e health check `/health`.
- `server.js` usa `process.env.PORT || 10000`; Express escuta sem restringir o host a localhost.
- CORS aberto permanece em `back/src/app.js` com `app.use(cors())`.
- `back/package.json` mantém `start: node src/server.js`; todas as dependências de execução foram encontradas por `npm ls --omit=dev` em `back/`.
- `NODE_VERSION=24.21.0` fixa a versão usada nos testes, necessária também ao fallback com `node:sqlite`.
- `NODE_ENV=production`, `PORT=10000`, `DB_MODE=mysql` e segredo JWT gerado pela Render. Credenciais MySQL são solicitadas no painel, sem valores secretos no repositório.
- Nenhuma subpasta `.git` encontrada. O `.gitignore` já cobre `back/node_modules/`, `back/.env`, build Android, `.gradle/` e `local.properties`; nenhuma mudança adicional foi necessária.

## Preparar o banco persistente

1. Disponibilize um MySQL acessível pelo serviço Render e configure as permissões/regras de rede do provedor. O Blueprint não provisiona MySQL.
2. Para um banco novo, aplique `back/src/config/migrations/000_full_schema.sql` com um usuário administrador. O arquivo cria e seleciona `skian_db`; se o provedor atribuir outro nome, adapte essas referências antes de executar. Não execute as migrações 001–004 sobre o schema consolidado.
3. Para banco existente, confira as migrações já aplicadas e os campos exigidos pela versão atual, inclusive `006_posts_media_activity.sql`. Faça backup antes de migrar.
4. Use um usuário de aplicação com acesso ao banco e configure `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` e `DB_PASSWORD` na Render.
5. Se o provedor exigir TLS, configure o cliente `mysql2` em `back/src/config/db.js` conforme os certificados/opções do provedor antes de publicar: esta versão não oferece variáveis de TLS para MySQL.

Para repetir o E2E, use uma instância de testes MySQL e credenciais com permissão para criar/remover banco e criar trigger. Exemplo, após definir DB_USER e DB_PASSWORD no ambiente:

```bash
cd back
DOTENV_CONFIG_PATH=/dev/null SKIPPECAS_E2E=1 DB_MODE=mysql DB_HOST=127.0.0.1 DB_PORT=3306 DB_NAME=skippecas_e2e_validacao JWT_SECRET=segredo-exclusivo-de-teste npm run test:e2e
```

A suíte se recusa a sobrescrever um banco já existente e remove somente o banco isolado que criou. Use credenciais exclusivas de testes.

## Publicar na Render

1. Envie o commit para a branch `main` do remoto já configurado:

```bash
git push -u origin main
```

2. No painel Render, crie um Blueprint, conecte o repositório GitHub e selecione a branch `main` e o arquivo `render.yaml` da raiz.
3. Revise o plano e as configurações antes de criar os recursos. Preencha os campos MySQL marcados com `sync: false`. O segredo JWT é gerado automaticamente; não o substitua por um valor público ou de demonstração.
4. Confirme `rootDir: back`, build `npm install`, start `npm start` e health check `/health`. Os comandos são executados dentro de `back/`.
5. Faça o deploy e confira os logs: em produção persistente deve aparecer `Banco ativo: mysql.`. Um erro de banco deixa `/health` em 503 e impede que o serviço seja considerado saudável; não esconda a falha trocando para demonstração.
6. Use a URL HTTPS efetivamente atribuída pelo painel. Valide `/health`: HTTP 200, `status=ok`, `database.engine=mysql`, `volatile=false` e todos os módulos em `ok`.
7. Cadastre contas próprias e valide login, cadastro de anúncio, feed, favoritos, busca e bloqueios pela URL pública. Configure essa URL base no aplicativo Android e gere uma nova versão do aplicativo.

A Render documenta [Blueprints e variáveis](https://render.com/docs/blueprint-spec), [monorepos](https://render.com/docs/monorepo-support), [versão do Node](https://render.com/docs/node-version) e [health checks](https://render.com/docs/health-checks).

## Persistência dos uploads

A aplicação grava fotos em `back/uploads/`. Antes de receber uploads reais, configure um disco persistente montado em `/opt/render/project/src/back/uploads` no serviço Node com `rootDir: back`, ou implemente armazenamento de objetos e adapte a aplicação. O Blueprint atual não cria disco nem armazenamento de objetos. Sem isso, arquivos locais enviados podem desaparecer em reinícios e novos deploys. Um disco exige plano compatível e limita o serviço a uma instância. Veja a [documentação de discos da Render](https://render.com/docs/disks).

## Demonstração sem MySQL

Para uma instância destinada explicitamente a demonstração, altere `DB_MODE` para `memory`; para testar queda de conexão e fallback, use `auto`. No fallback, dados e contas são temporários, o JWT é recriado e as sessões deixam de valer ao reiniciar. Não use esse modo para persistir contas reais. O Blueprint mantém `mysql` por esse motivo.

## Critério de aprovação

**Aprovado:** suíte principal local e smoke HTTP com fallback, CORS, autorização, porta dinâmica e encerramento limpo.

**Pendente antes de liberar produção:** E2E com MySQL disponível, schema aplicado, eventual TLS conforme o provedor, armazenamento persistente de fotos, deploy na conta Render e validação pública/Android. A execução local aprovada não comprova essas etapas externas.

O commit reúne a infraestrutura, o teste integrado e este relatório. O push e a criação de recursos na Render não foram executados.

## Atualização da preparação Android e documentação

A especificação ampliada está em [docs/PRODUCAO_TECNICA.md](docs/PRODUCAO_TECNICA.md). O roteiro USB define `PORT=3000` explicitamente; o fallback de produção continua 10000. O launcher foi confirmado no manifesto e no APK. A URL externa agora é configurável por `-PskippecasApiUrl=https://SEU-SERVICO.onrender.com/api/`, sem editar Kotlin. Consulte [validação Android](docs/VALIDACAO_ANDROID.md) e [evidência desta execução](docs/evidencias/EXECUCAO.md) para resultados atuais; os números anteriores descrevem a etapa anterior.
