# Produção técnica e especificação — SkipPeças

## Apresentação e contextualização metodológica

O SkipPeças apoia a descoberta de peças automotivas e a publicação de anúncios por vendedores em um aplicativo Android conectado a uma API Node.js. O trabalho combina levantamento das funcionalidades do MVP, inspeção do código existente, modelagem reversa de navegação e dados e validação automatizada dos fluxos HTTP. A documentação aqui produzida descreve o sistema implementado e explicita metas e evidências ainda pendentes; não atribui entrevistas, aprovações de design ou ensaios que não ocorreram.

Os artefatos são a aplicação Android, API REST, scripts SQL, testes, infraestrutura Render, modelos editáveis e documentação de operação. Não fazem parte desta entrega checkout, pagamentos, chat ou aplicação SPA. O produto aproxima compradores e vendedores por descoberta, anúncios, perfis e favoritos; não conclui compra financeira.

## Modelagem do produto

A jornada começa no login/cadastro e leva ao feed. Marketplace permite filtrar peças e consultar detalhes; vendedores publicam anúncios; usuários guardam favoritos, consultam perfis e gerem conta. A referência visual verificável é o código dos layouts. Consulte [sitemap e wireframes por tela](INTERFACE.md); as imagens são esquemas de engenharia reversa e não substituem prints reais ou aprovação no Figma.

## Arquitetura de software

O backend isola entrada HTTP, autorização, validação e persistência; o Android separa Fragments, estado de apresentação e contratos Retrofit. Essa distribuição facilita testes da API sem abrir o servidor de produção e testes de rede Android sem dependência de uma nuvem pública. Veja [arquiteturas funcional, técnica e MVC](MODELOS.md).

| Elemento | Tecnologia / protocolo | Responsabilidade e configuração |
| --- | --- | --- |
| Cliente | Android 24+ / Kotlin e Java | Fragments, layouts, Navigation e ViewModels |
| Cliente HTTP | Retrofit / OkHttp / JSON | Base local http://localhost:3000/api/ via ADB; produção configurável por -PskippecasApiUrl |
| Entrada pública | Render / HTTPS 443 | TLS do serviço e encaminhamento para porta interna |
| API | Node 24 / Express | PORT da plataforma ou 10000; CORS; controllers e models |
| Banco | MySQL / mysql2 / TCP 3306 por padrão | Dados persistentes; DB_MODE=mysql em produção; TLS conforme provedor |
| Demonstração | node:sqlite em memória | Dados voláteis e JWT exclusivo por execução |
| Fotos | Arquivos back/uploads | Disco persistente necessário na Render |

## Engenharia de requisitos

Os requisitos a seguir consolidam comportamentos observáveis do MVP. Cada requisito tem regra de aceitação e referência de verificação. As metas de qualidade propostas não são apresentadas como medições realizadas. A homologação depende das evidências automatizadas e das verificações manuais listadas no roteiro.

### Requisitos funcionais

| ID | Comportamento | Regra / aceitação | Evidência em back/tests ou Android |
| --- | --- | --- | --- |
| RF01 | Cadastrar e autenticar | Nome/e-mail válidos; senha de cadastro 8 caracteres/até 72 bytes; bcrypt e JWT HS256 de 1h. | auth.test.js; session.test.js |
| RF02 | Consultar feed e detalhes | Feed público com autor e mídia; ID inválido 400 e produto ausente 404. | feed.test.js |
| RF03 | Filtrar marketplace | Combinar categoria, modelo, local e preços; retorno vazio é lista, não erro. | marketplace.test.js |
| RF04 | Gerir favoritos | JWT obrigatório; salvar/remover de modo idempotente e isolado por conta. | favorites.test.js |
| RF05 | Publicar e listar anúncios | Somente vendedor; preço e campos validados; listar apenas os próprios anúncios. | seller.test.js |
| RF06 | Buscar usuários e consultar perfis | Busca autenticada com filtro; perfil público não revela e-mail. | users.test.js |
| RF07 | Editar próprio perfil | Alterações apenas do titular; validar nome, bio, e-mail e URLs. | users.test.js |
| RF08 | Publicar texto/mídia/foto | Usuário autenticado; foto JPEG/PNG/WebP até 8 MB; publicação visível no feed e perfil. | publishing.test.js |
| RF09 | Gerir bloqueios | Proibir autobloqueio; operação idempotente; listar/remover apenas relações próprias. | settings.test.js |
| RF10 | Excluir conta | Transação remove relações, anúncios e conta; JWT antigo é recusado. | settings.test.js; session.test.js |
| RF11 | Configurar tema e recuperar acesso | Configuração de tema no Android e retorno ao login após sessão inválida; validar no dispositivo. | ThemeSettings.kt; SessionUi.kt; roteiro manual |
| RF12 | Informar saúde e permitir demo | Health verifica sete módulos; fallback somente para conectividade em auto; mysql estrito sem fallback. | health.test.js; databaseRouter.test.js; smoke.test.js |

### Requisitos não funcionais

| ID | Qualidade | Critério mensurável | Situação |
| --- | --- | --- | --- |
| RNF01 | Segurança de sessão | bcrypt, JWT HS256 expira em 1h, acesso protegido sem token 401, comprador em anúncio 403. | Testado backend |
| RNF02 | Segurança de entrada | Valores SQL parametrizados, corpo inválido 400, excesso 413, resposta de erro sem stack ou segredo. | Testado nos cenários cobertos; revisão contínua |
| RNF03 | Integridade | FKs e unicidade ativas; rollback sem perda parcial; idempotência de favoritos/bloqueios. | Testado SQLite e mocks; MySQL real pendente |
| RNF04 | Comunicação | Produção HTTPS; cliente connectTimeout=15s/readTimeout=90s/callTimeout=100s; CORS aberto. | Timeouts no código; HTTPS público pendente |
| RNF05 | Desempenho | Meta: p95 menor que 2s para consultas, 20 clientes concorrentes, base de 1000 anúncios; registrar hardware e resultado. | Meta proposta, carga não executada |
| RNF06 | Usabilidade e adaptação | Loading, erro, sucesso e vazio; validar 360dp, 600dp, 1240dp e paisagem sem corte/rolagem horizontal indevida. | Recursos implementados; ensaio visual pendente |
| RNF07 | Disponibilidade e persistência | Health 200 com módulos ok; meta 99,5% mensal após produção; MySQL e fotos sobrevivem a restart. | Meta proposta; medição e armazenamento pendentes |
| RNF08 | Reprodutibilidade | Node 24, lockfile, Gradle Wrapper, clone/setup e comandos automatizados; APK resolve MAIN/LAUNCHER. | Build/testes e manifestos devem constar no relatório de execução |

### Requisitos associados e rastreabilidade

| Função | Qualidade relacionada | Verificação |
| --- | --- | --- |
| RF01 | RNF01, RNF02, RNF04 | auth/session e login Android |
| RF02, RF03 | RNF02, RNF05, RNF06 | feed/marketplace, teste de carga pendente e telas |
| RF04 | RNF01, RNF03, RNF06 | favorites e estados visuais |
| RF05 | RNF01, RNF02, RNF03 | seller e autorização |
| RF06, RF07 | RNF01, RNF02, RNF06 | users e privacidade |
| RF08 | RNF01, RNF02, RNF07 | publishing; persistência de uploads pendente |
| RF09, RF10 | RNF01, RNF03 | settings/session; rollback e revogação |
| RF11 | RNF06, RNF08 | roteiro Android e teste de tema/sessão manual |
| RF12 | RNF07, RNF08 | health/databaseRouter/smoke; health público pendente |

### Projeto linear e marcos

Sequência de consolidação, sem inventar datas de execução ou branches que não existiram:

| Ordem | Entregável | Critério de conclusão | Evidência / situação |
| --- | --- | --- | --- |
| 1 | MVP Android/API | Módulos e contratos integrados | Código e documentação existentes |
| 2 | Banco e fallback | Schema, seeds, integridade e isolamento | SQL e testes existentes |
| 3 | Pré-deploy | Blueprint, health e smoke | render.yaml e testes |
| 4 | Especificação reversa | RF/RNF, modelos e wireframes | Documentos desta pasta |
| 5 | APK limpo | assembleDebug, unit tests e lint | Resultado em VALIDACAO_ANDROID.md |
| 6 | Homologação visual | Dispositivos, prints e fidelidade | Pendente |
| 7 | Produção persistente | MySQL, disco, HTTPS e testes públicos | Pendente |
| 8 | Ensaio de banca | Roteiro executado com evidências | Pendente |

## Modelagem de processos e fluxos

A cadeia de valor é cadastrar/autenticar, publicar, descobrir peças, consultar perfis, salvar interesse e gerir a conta. A modelagem diferencia regras de papel vendedor, sessão e validação de dados; erros interrompem a mutação sem simular sucesso. Consulte [macroprocesso, fluxograma e BPMN](MODELOS.md). Solicitação de produto é entidade do schema; seu ciclo de decisão não é apresentado como funcionalidade entregue pela API.

## Diagramas e modelagem de software

Os modelos comportamentais descrevem quem inicia ações, as transições de sessão e as chamadas internas de bloqueio. O DER e o [dicionário de dados](DICIONARIO_DADOS.md) derivam do schema SQL para preservar tipos, cardinalidades e restrições. O modelo de classes representa entidades e módulos reais, sem exigir herança inexistente no Node.js funcional. Diagramas completos e fontes CASE estão em [MODELOS.md](MODELOS.md).

### Casos de uso por tela

Pré-condição comum das telas protegidas: usuário com sessão válida. Alternativa comum: rede indisponível mostra erro e permite repetir; 401 limpa/renova o acesso via login. Não interpretar descrição de fluxo como prova de teste visual.

| Caso / tela | Ator e pré-condição | Fluxo principal | Alternativa | Exceção / resultado |
| --- | --- | --- | --- | --- |
| UC01 Login/cadastro | Visitante | Preencher, validar e autenticar → Home | Cadastrar conta nova | 400 dados inválidos; 401 credenciais; 409 duplicidade |
| UC02 Home | Usuário no app | Carregar feed e abrir produto/autor | Lista vazia | Erro de rede; detalhe inexistente |
| UC03 Marketplace | Usuário no app | Consultar e combinar filtros | Limpar filtros; nenhum resultado | Faixa inválida → 400 |
| UC04 Detalhes | Produto selecionado | Ver peça e alternar favorito | Retornar à lista | ID inválido 400; ausente 404; favorito exige sessão |
| UC05 Vender/novo anúncio | Vendedor autenticado | Preencher e salvar anúncio → lista | Cancelar formulário | Comprador 403; campo inválido 400 |
| UC06 Usuários | Usuário autenticado | Buscar, filtrar papel e abrir perfil | Busca sem resultado | 401 sessão inválida |
| UC07 Meu perfil/edição | Titular autenticado | Consultar e editar campos próprios | Cancelar alteração | E-mail duplicado; URL inválida |
| UC08 Perfil público | Usuário autenticado | Abrir por ID ou deep link e ver mídia | Nenhuma publicação | Usuário ausente 404; sem e-mail privado |
| UC09 Publicar foto | Titular autenticado | Selecionar imagem, prévia e publicar | Cancelar seleção | Formato inválido 400; limite 413 |
| UC10 Configurações | Usuário autenticado | Tema, favoritos e bloqueios | Manter preferências | Exclusão exige confirmação e pode falhar com rollback |
| UC11 Favoritos | Usuário autenticado | Listar, abrir produto e remover | Lista vazia | Token inválido 401 |
| UC12 Bloqueados | Usuário autenticado | Listar, bloquear e desbloquear | Repetição idempotente | Autobloqueio 400; relação alheia 404 |

## Auditoria e governança

A revisão técnica desta etapa cobre launcher, URL/porta, tratamento global de erros, rastreabilidade do schema e evidências de compilação/testes. Ela não atesta que cada integrante domina todo o código. Para encerrar o requisito de domínio, cada integrante deve explicar uma rotina, executar um teste de exceção e registrar revisão, responsável e data em evidência própria. Não foram criadas branches retroativas para simular desenvolvimento paralelo.

Checklist de revisão humana: explicar JWT e revogação; provar consultas parametrizadas; demonstrar rollback; distinguir SQLite demo de MySQL persistente; explicar estados de tela e cancelamento de chamadas; reproduzir build e ler os resultados do lint; conferir permissões/backup e tratamento de deep links.
