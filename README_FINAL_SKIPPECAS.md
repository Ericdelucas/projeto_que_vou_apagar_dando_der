# SkipPeças — relatório final do MVP

Relatório de implementação e validação em **25/10/2026**. Repositório: `/home/eric/Documentos/GitHub/Engenharia/`.

O SkipPeças reúne descoberta de peças automotivas, anúncios, favoritos, perfis e configurações em um aplicativo Android conectado a uma API REST. Este documento descreve os recursos implementados, os comandos para reproduzir o ambiente e o alcance dos testes executados.

## Testar o APK agora, sem MySQL

O backend oferece **fallback automático para SQLite em memória**, disponível no Node.js 24, sem instalar novas dependências. No modo `auto`, ao receber ECONNREFUSED ou outra falha de conectividade na primeira conexão MySQL, cria uma base de demonstração completa e continua atendendo os mesmos endpoints.

```sh
cd back
npm start
```

Para escolher explicitamente o modo:

```sh
DB_MODE=memory npm start  # Demonstração, sem tentar MySQL.
DB_MODE=auto npm start    # MySQL preferencial; fallback se estiver inacessível.
DB_MODE=mysql npm start   # MySQL obrigatório; nenhuma demonstração automática.
```

Sem DB_MODE explícito, o padrão é `auto`, inclusive com `NODE_ENV=production`, para permitir testes do APK sem MySQL. Use `DB_MODE=mysql` quando a persistência for obrigatória. Erros de credenciais, schema ou SQL não são tratados como indisponibilidade de rede e não ativam o fallback. O timeout inicial de conexão é de dois segundos.

### Logins de demonstração

| Perfil | E-mail | Senha |
|---|---|---|
| Eric (conta local) | `ericdelucass@gmail.com` | `123` |
| Comprador | `comprador@skippecas.local` | `SkipPecas123!` |
| Vendedor | `vendedor@skippecas.local` | `SkipPecas123!` |
| Outra loja | `oficina@skippecas.local` | `SkipPecas123!` |

A base inclui três anúncios, três publicações, favorito, bloqueio, seguidores e solicitação. As imagens de demonstração são servidas localmente pelo backend em `/demo/peca.png`, usando o endereço da requisição para funcionar no emulador e em portas de teste. O cadastro de novas contas também continua disponível pela API.

O JWT e as senhas continuam sendo validados de verdade. No modo SQLite, o servidor usa um segredo JWT efêmero e exclusivo daquela execução, mesmo que exista JWT_SECRET configurado para o MySQL. Isso impede que tokens de outro banco ou de uma demonstração anterior assumam usuários de IDs iguais. Dados alterados, contas cadastradas e sessões são descartados ao reiniciar. O arquivo .env permanece intacto. As credenciais do MySQL não são copiadas nem substituídas: estes são usuários independentes de demonstração.

`GET /health` identifica o modo em `database.engine` (`mysql` ou `sqlite-memory`), `database.mode`, `database.fallback`, `database.volatile` e, no fallback, `database.reason`. Com a base de demonstração pronta, responde 200 e todos os módulos ficam disponíveis.

A escolha do banco é feita uma vez por processo. O servidor não migra escritas automaticamente para o MySQL quando ele voltar nem troca de banco no meio de uma transação. Para voltar ao MySQL, reinicie o backend; para exigir persistência desde o início, use DB_MODE=mysql.

### Comportamento e validação do fallback

O SQLite mantém FKs, unicidade, CHECKs e transações reais. A camada adaptadora preserva o contrato mysql2 usado pelos models, traduzindo apenas as diferenças SQL necessárias. Um mutex impede que requisições concorrentes leiam ou alterem uma transação alheia. Filtros de preço continuam numéricos e a busca de usuários reconhece acentos. Rotas inválidas e credenciais incorretas continuam retornando erros apropriados; o modo demo não desativa autenticação ou permissões.

Validação do fallback: **16 testes backend aprovados**, **21 testes Android aprovados** (incluindo Retrofit contra a API real em SQLite) e regressão E2E com MySQL real aprovada. Os testes incluem inicialização automática com MySQL inacessível, todos os módulos por HTTP, concorrência, rollback, cascatas e isolamento de sessões. Os comandos de MySQL e o relatório de validação original abaixo continuam válidos para o modo persistente; não são pré-requisito para testar o APK no modo demo.

## 1. Arquitetura

| Camada | Tecnologias e responsabilidade |
|---|---|
| Android | Kotlin, Fragments, View Binding, Navigation Component, RecyclerView, LiveData/ViewModels e Material Components. MainActivity permanece em Java, interoperando com Kotlin. |
| Rede Android | Retrofit 2.11, Gson e OkHttp; contratos em `app/src/main/java/com/example/app/network/`. Chamadas de tela são assíncronas. |
| API | Node.js com módulos ES, Express e CORS; controllers validam entradas, models usam consultas parametrizadas e middleware protege rotas. |
| Autenticação | Senhas com bcrypt, JWT HS256 com expiração de uma hora; o servidor consulta a existência da conta em cada acesso protegido. |
| Persistência | MySQL com mysql2/promise; SQLite em memória como fallback de demonstração. FKs, índices e transações para bloqueios/exclusão em ambos os modos. |
| Imagens e vídeo | Glide para imagens e componentes Android para mídia, com URLs fornecidas pelos dados. |

Fluxo principal: **Fragment → Retrofit → rota Express → middleware/controller → model → MySQL → resposta JSON → estado da tela**.

`back/src/app.js` monta a aplicação sem abrir portas, permitindo que os mesmos endpoints sejam usados em testes. `back/src/server.js` carrega a configuração e inicia o servidor. A separação não muda os prefixos existentes.

## 2. Módulos implementados

| Módulo | Recursos disponíveis | Arquivos centrais |
|---|---|---|
| Autenticação | Registro na API, login no Android, validação de credenciais, hash bcrypt, JWT, persistência local e tratamento de sessão expirada. | `authController.js`, `authMiddleware.js`, `AuthApi.kt`, `LoginFragment.kt`, `TokenStore.kt` |
| Feed interativo | Consulta de posts, autor, conteúdo de mídia, produto associado, detalhes da peça e integração com favoritos. | `feedController.js`, `feedModel.js`, `FeedApi.kt`, `HomeFragment.kt`, `feed/FeedAdapter.kt` |
| Marketplace | Anúncios ativos e filtros combinados de modelo, categoria, localização e intervalo de preço; opções de filtros e detalhes por produto. | `marketplaceController.js`, `marketplaceModel.js`, `MarketplaceApi.kt`, `MarketplaceFragment.kt` |
| Favoritos/Salvos | Salvar/remover com estado explícito, listagem própria, navegação para a peça e sincronização com o feed. | `favoritesController.js`, `favoriteModel.js`, `FavoritesApi.kt`, `FavoritosFragment.kt`, `favorites/` |
| Vendas | Acesso de vendedor conferido no banco, cadastro de anúncio com validações, lista dos próprios produtos e contadores de solicitações por situação. | `sellerController.js`, `sellerModel.js`, `sellerMiddleware.js`, `SellerApi.kt`, `VenderFragment.kt`, `seller/` |
| Perfil/Busca | Busca textual/filtro por função, atividade, perfil próprio/público, edição dos campos permitidos, mídia, métricas e compartilhamento do perfil. | `userController.js`, `userProfileModel.js`, `UserApi.kt`, `UsersFragment.kt`, `PerfilFragment.kt`, `users/` |
| Configurações | Salvos, lista de bloqueados, estado informativo da Loja com acesso ao Marketplace, tema sistema/claro/escuro persistente e exclusão confirmada da conta. | `settingsController.js`, `settingsModel.js`, `SettingsApi.kt`, `ConfiguracoesFragment.kt`, `settings/` |
| Integridade/saúde | Saúde por módulo, detecção de schema incompleto, invalidação de sessões de contas removidas e transações com rollback. | `healthController.js`, `authMiddleware.js`, `settingsModel.js` |

### Escopo funcional do MVP

A lista de bloqueios permite cadastrar, consultar e remover relações pessoais. Os endpoints públicos de feed/marketplace continuam públicos; filtragem desses conteúdos por bloqueio não está implementada. O registro está disponível pela API e pela opção Cadastrar-se na tela de entrada Android, com nome, e-mail, senha e escolha de comprador ou vendedor. Novos cadastros exigem senha de pelo menos oito caracteres; a senha 123 pertence somente à conta local de demonstração solicitada. Contadores de likes/comentários e solicitações refletem os dados existentes: este relatório não afirma a existência de checkout, pagamento, chat, criação de comentários ou fluxo completo de pedidos. Os testes abaixo validam os recursos efetivamente descritos.

## 3. Contratos HTTP

Todas as rotas assinaladas como protegidas exigem `Authorization: Bearer <JWT>`.

| Módulo | Método e caminho | Acesso |
|---|---|---|
| Saúde | `GET /health` | Público |
| Autenticação | `POST /api/auth/register`, `POST /api/auth/login` | Público |
| Feed/produto | `GET /api/feed`, `GET /api/products/:id` | Público |
| Marketplace | `GET /api/marketplace`, `GET /api/marketplace/filters` | Público |
| Favoritos | `GET /api/favorites`, `POST /api/favorites` | Protegido |
| Vendas | `GET /api/seller/products`, `POST /api/seller/products` | Protegido, função vendedor |
| Usuários | `GET /api/users/search`, `GET /api/users/me`, `GET /api/users/profile/:userId`, `PUT /api/users/profile` | Protegido |
| Bloqueios | `GET /api/settings/blocked`, `POST /api/settings/block`, `DELETE /api/settings/block/:id` | Protegido |
| Conta | `DELETE /api/settings/account` | Protegido |

`GET /health` retorna `status`, `database` e os módulos `authentication`, `feed`, `marketplace`, `favorites`, `sales`, `users` e `settings`. Retorna HTTP 200 se o banco responde, as tabelas necessárias estão presentes e JWT_SECRET está configurado. Retorna 503 se o banco estiver indisponível, faltar tabela ou faltar a configuração JWT, indicando os módulos afetados. O health verifica conectividade e presença das tabelas; a validação comportamental é feita pelos testes.

### Bloqueios

- POST recebe `{"blocked_user_id": 8}`. A origem é sempre o usuário do JWT, nunca um campo enviado pelo cliente.
- A mesma combinação origem/destino não se duplica, inclusive sob concorrência. Autobloqueio e IDs inválidos retornam 400; usuário ausente retorna 404.
- GET retorna os bloqueios próprios com ID da relação, data e dados públicos do destinatário.
- DELETE recebe **o ID da relação de bloqueio**, não o ID do usuário. Relação de outra pessoa ou inexistente retorna 404; remoção bem-sucedida retorna 204.

### Exclusão de conta e invalidação do JWT

A exclusão é física e acontece em uma transação. O serviço bloqueia a linha do usuário e remove bloqueios nos dois sentidos, seguidores/seguidos, favoritos próprios, solicitações próprias, posts próprios, anúncios próprios e a conta. FKs removem também favoritos e solicitações associados aos anúncios excluídos. Posts de terceiros que apontavam para esses anúncios permanecem com a referência de produto nula, conforme o schema.

Se qualquer etapa falhar, o rollback restaura os dados. O corpo da requisição não pode selecionar outra conta. Depois do commit, o middleware rejeita todos os JWTs dessa conta com 401 em qualquer rota protegida, mesmo em outra instância/reinicialização da API: a invalidação depende da ausência no banco, não de uma lista em memória. Requisições já em execução antes do commit não são canceladas retroativamente.

No Android, `AlertDialog` exige confirmação. Após sucesso, o app limpa `auth_session` em SharedPreferences e navega ao login removendo o histórico autenticado. Uma falha de rede/servidor preserva a sessão para permitir nova tentativa.

## 4. Banco consolidado e migrações

O arquivo **`back/src/config/migrations/000_full_schema.sql`** permite iniciar um banco vazio com um comando. Contém sete tabelas: `users`, `products`, `posts`, `favorites`, `product_requests`, `user_follows` e `blocked_users`. As duas tabelas auxiliares são necessárias aos módulos de vendas e perfil.

`back/src/config/database.sql` permanece equivalente ao schema completo para compatibilidade com instruções anteriores. `005_settings_and_blocks.sql` contém a migração incremental de bloqueios; substitui o nome anterior `005_blocked_users.sql`, com estrutura equivalente e criação idempotente.

**Banco novo:** execute somente o schema completo. Não execute depois as migrações 001–004: elas alteram colunas já incluídas no consolidado.

**Banco existente:** mantenha seus dados e aplique apenas as migrações incrementais ainda não executadas. `CREATE TABLE IF NOT EXISTS` não atualiza uma tabela antiga com estrutura divergente.

## 5. Executar o backend e o MySQL

### Pré-requisitos

- Node.js 24 e npm, usados na validação.
- MySQL 8.4, usado na integração real; servidor em execução e cliente `mysql` disponível.
- Um usuário administrador para criar o schema; credenciais de aplicação com privilégios sobre `skian_db`.

A partir da raiz do repositório:

```sh
mysql -u root -p < back/src/config/migrations/000_full_schema.sql
```

Em instalações com autenticação administrativa por socket, use o cliente administrativo correspondente, por exemplo `sudo mysql < back/src/config/migrations/000_full_schema.sql`.

Opcionalmente crie um usuário exclusivo da aplicação no cliente MySQL, substituindo a senha de exemplo:

```sql
CREATE USER 'skippecas'@'localhost' IDENTIFIED BY 'SUBSTITUA_POR_UMA_SENHA_LOCAL';
GRANT SELECT, INSERT, UPDATE, DELETE ON skian_db.* TO 'skippecas'@'localhost';
```

Prepare a API:

```sh
cd back
npm ci
cp .env.example .env
```

Se já houver `.env`, preserve-o em vez de sobrescrever. Edite `DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD`, `DB_NAME`, `PORT` e `JWT_SECRET`. O arquivo de exemplo não contém credenciais reais. Gere um segredo, por exemplo, com `node -e "console.log(require('node:crypto').randomBytes(48).toString('hex'))"` e grave o resultado em `JWT_SECRET`.

```sh
npm start
# Alternativa para desenvolvimento:
# npm run dev
```

Em outro terminal:

```sh
curl --fail-with-body http://localhost:3000/health
```

Um banco novo começa vazio. Registre uma conta antes de entrar no Android:

```sh
curl --fail-with-body -X POST http://localhost:3000/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"name":"Vendedor de teste","email":"vendedor@example.com","password":"troque-esta-senha-123","role":"vendedor"}'
```

Entre no aplicativo com essas credenciais e cadastre peças em Vendas. A API de registro também aceita `role: comprador`. As publicações do feed são registros em `posts`; o cadastro de anúncio não afirma a criação automática de um post.

## 6. Abrir, compilar e instalar o Android

1. Abra no Android Studio a pasta **`front/projeto_que_vou_apagar_dando_der/`**, que contém `settings.gradle.kts`.
2. Use uma versão do Android Studio compatível com o AGP 9.4. O projeto usa Gradle Wrapper 9.6, daemon JDK 25, compileSdk/targetSdk 37 e minSdk 24. O arquivo `gradle-daemon-jvm.properties` declara o toolchain.
3. Instale o Android SDK Platform 37 e os build tools solicitados pelo Gradle; aceite as licenças pelo SDK Manager. Configure o SDK pela IDE ou `ANDROID_HOME`.
4. Sincronize o Gradle e inicie a API/MySQL antes de testar as telas conectadas.
5. A configuração atual usa `ApiClient.BASE_URL = http://localhost:3000/api/`. Para celular USB ou emulador conectado por ADB, execute `adb reverse tcp:3000 tcp:3000`. O build debug libera HTTP somente para `10.0.2.2` e `localhost`.
6. Mantenha o USB conectado e a API ativa; refaça o `adb reverse` após reconectar o aparelho. Para emulador sem reverse, altere a BASE_URL para `http://10.0.2.2:3000/api/`. Em publicação real, configure sua URL HTTPS.
7. Use **Run** para instalar e abrir o app ou gere o APK pelo Gradle.

```sh
cd front/projeto_que_vou_apagar_dando_der
./gradlew --warning-mode=fail :app:assembleDebug :app:testDebugUnitTest
```

APK gerado em:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Instalação opcional pelo terminal:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

O projeto trata warnings de Kotlin e Java como erros. O build de validação também usou `--warning-mode=fail` e `--rerun-tasks`, evitando depender apenas de tarefas previamente compiladas.

## 7. Testes e integridade end-to-end

### Backend: testes automatizados com persistência simulada

```sh
cd back
npm test
```

A suíte cobre autenticação real com bcrypt/JWT, contratos HTTP, filtros, isolamento de favoritos/vendas, perfis, configurações, health e invalidação de sessão. A suíte usa mocks de banco e não requer MySQL ativo.

### Backend: integração real com MySQL

Use um servidor MySQL de testes e um nome de banco **novo** com prefixo `skippecas_e2e_`. O executor recusa nomes fora desse padrão e bancos já existentes; remove o banco criado no encerramento.

```sh
cd back
SKIPPECAS_E2E=1 DB_HOST=127.0.0.1 DB_PORT=3306 DB_USER=root \
  DB_NAME=skippecas_e2e_backend JWT_SECRET=segredo-exclusivo-de-testes \
  npm run test:e2e
```

Configure `DB_PASSWORD` no ambiente local se necessário. O usuário de testes precisa criar/remover banco e criar trigger. O fluxo aplica o schema completo e a migração incremental repetidamente, registra contas, autentica, cadastra anúncios, consulta feed/marketplace/perfis, salva favoritos, bloqueia/desbloqueia e exclui conta. Uma trigger provoca falha na etapa final para conferir rollback real de todas as relações. Em seguida, o teste confirma a exclusão e a rejeição do JWT antigo em todos os módulos protegidos.

### Kotlin/Retrofit → Express → MySQL

O executor a seguir cria outro banco descartável, inicia a mesma aplicação Express em uma porta local livre e executa a suíte JVM Android. `SettingsBackendE2eTest` usa as interfaces Retrofit reais contra essa API; registra/loga usuários, cadastra anúncio, consulta/favorita, busca perfil, bloqueia/desbloqueia, exclui a conta e confere invalidação e cascata.

```sh
cd back
SKIPPECAS_E2E=1 DB_HOST=127.0.0.1 DB_PORT=3306 DB_USER=root \
  DB_NAME=skippecas_e2e_android JWT_SECRET=segredo-exclusivo-de-testes \
  npm run test:android:e2e
```

Configure também `JAVA_HOME` e `ANDROID_HOME` conforme sua instalação. O executor usa `--offline`, portanto as dependências Gradle precisam estar previamente sincronizadas. Fora desse executor, o teste de backend real é opt-in por `SKIPPECAS_E2E_URL`; sem a variável, apenas esse caso é ignorado. Os vinte testes locais restantes usam JUnit e MockWebServer.

### Resultado observado nesta entrega

- Suíte backend atual: 16 testes aprovados, incluindo roteamento, fallback SQLite, transações e os cenários originais.
- Um fluxo backend/HTTP com MySQL **8.4.11** real e descartável: aprovado, incluindo FKs, concorrência, rollback e revogação dos JWTs.
- APK debug: compilado com sucesso, com warnings Kotlin/Java tratados como erro e Gradle em modo de falha para warnings.
- Vinte testes locais Android/JUnit/MockWebServer mais um teste Kotlin/Retrofit contra Express/MySQL reais: **21 aprovados, zero falhas e zero ignorados** na execução E2E.

Os testes usam infraestrutura temporária em `/tmp`, sem instalar serviço MySQL no sistema nem modificar os dados do banco configurado no `.env` do projeto. O MySQL local configurado anteriormente retornou `ECONNREFUSED`: executar uma instância de teste não inicia esse serviço de desenvolvimento. A validação de UI por toque, rotação e retorno de tela em emulador/aparelho é uma etapa manual distinta; não foi substituída por uma alegação de instrumentação executada.

## 8. Roteiro de aceite visual

1. Entrar com comprador e vendedor; conferir erros para senha inválida e ausência de rede.
2. Consultar feed, abrir uma peça, salvar/remover e conferir a tela Salvos.
3. Combinar filtros no Marketplace e acessar os detalhes.
4. Como vendedor, cadastrar anúncio e conferir apenas os próprios produtos.
5. Buscar perfis, visualizar mídia e editar o próprio perfil.
6. Abrir Configurações → Salvos e Bloqueados; conferir lista vazia, busca, confirmação de bloqueio e desbloqueio.
7. Abrir Loja e conferir o estado informativo e acesso ao Marketplace; alternar tema e reabrir o aplicativo.
8. Cancelar a exclusão e conferir que nada foi alterado. Confirmar a exclusão de uma conta de testes e conferir retorno ao login sem histórico autenticado.
9. Reutilizar o JWT excluído em rota protegida e conferir 401; confirmar que os dados de outras contas continuam acessíveis.

## 9. Documentação complementar

- [Feed](FEED.md)
- [Marketplace](MARKETPLACE.md)
- [Favoritos](FAVORITES.md)
- [Vendas](VENDAS.md)
- [Usuários](USUARIOS.md)
- [Configurações](SETTINGS.md)
