# Modelagem de processos e software

Os modelos abaixo documentam o MVP existente. Relações representadas não adicionam checkout, pagamento, chat, notificações ou operações de aceitar/recusar solicitações que não tenham endpoint implementado. As imagens SVG são resumos de leitura; as fontes Mermaid abaixo são os diagramas completos.

## Sitemap Android

O ponto inicial é Login. A barra inferior organiza os módulos autenticados; detalhes recebem product_id e perfil público recebe userId. O diagrama resume também ações disparadas por botões, não apenas actions XML.

[Fonte CASE editável](modelos/sitemap.mmd) · [Visão resumida SVG](imagens/sitemap.svg)

```mermaid
flowchart TD
 Login --> Home
 Home --> Detalhes
 Home --> Marketplace
 Home --> Vender
 Home --> Usuarios
 Home --> Perfil
 Home --> Configuracoes
 Marketplace --> Detalhes
 Vender --> NovoAnuncio
 Vender --> PerfilPublico
 Usuarios --> PerfilPublico
 Perfil --> EditarPerfil
 Perfil --> PublicarFoto
 Configuracoes --> Favoritos
 Configuracoes --> Bloqueados
 Favoritos --> Detalhes
```

## Arquitetura funcional

Autenticação estabelece sessão; vendedor cria anúncios consultáveis no marketplace; publicações compõem o feed. Favoritos, perfis e bloqueios dependem da identidade autenticada.

[Fonte CASE editável](modelos/funcional.mmd) · [Visão resumida SVG](imagens/funcional.svg)

```mermaid
flowchart LR
 Auth[Autenticação] --> Sessao[Sessão JWT]
 Sessao --> Anuncios[Anúncios]
 Sessao --> Favoritos
 Sessao --> Perfis
 Sessao --> Bloqueios
 Anuncios --> Marketplace
 Anuncios --> Feed
 Perfis --> Publicacoes[Publicações]
 Publicacoes --> Feed
```

## Arquitetura técnica

Android troca JSON com Express via proxy HTTPS da Render. O banco é MySQL externo em produção; SQLite volátil serve apenas à demonstração. Fotos exigem armazenamento persistente. TLS do banco depende do provedor.

[Fonte CASE editável](modelos/tecnica.mmd) · [Visão resumida SVG](imagens/tecnica.svg)

```mermaid
flowchart LR
 Android[Android / Retrofit] -->|HTTPS 443| Proxy[Proxy Render / TLS]
 Proxy -->|PORT 10000| API[Node 24 / Express]
 API -->|mysql2 TCP 3306| DB[(MySQL persistente)]
 API --> Uploads[(back/uploads)]
 API -. apenas demo .-> Mem[(SQLite em memória)]
```

## Distribuição MVC

MVC é distribuído: View Android, controllers HTTP e models SQL no backend. ViewModels Android gerem estado de apresentação; não são os models de persistência Node.

[Fonte CASE editável](modelos/mvc.mmd) · [Visão resumida SVG](imagens/mvc.svg)

```mermaid
flowchart LR
 View[View: Fragments e layouts Android] --> Rede[Retrofit]
 Rede --> Routes[Routes e middleware Express]
 Routes --> Controller[Controllers: validação e HTTP]
 Controller --> Model[Models: SQL e transações]
 Model --> DB[(MySQL / SQLite demo)]
```

## Cadeia de valor

A cadeia de valor aproxima compradores e vendedores pela descoberta de peças e gestão de anúncios. Não há transação financeira implementada.

[Fonte CASE editável](modelos/macroprocesso.mmd) · [Visão resumida SVG](imagens/macroprocesso.svg)

```mermaid
flowchart LR
 A[Registrar e autenticar] --> B[Publicar anúncio ou foto]
 B --> C[Descobrir e filtrar peças]
 C --> D[Consultar detalhes e perfis]
 D --> E[Salvar favoritos]
 E --> F[Gerir conta e preferências]
```

## Fluxo de anúncio

Anunciar exige sessão e papel vendedor. Entradas inválidas retornam 400; ausência de sessão 401; papel insuficiente 403. Somente após persistência a API retorna 201.

[Fonte CASE editável](modelos/fluxograma.mmd) · [Visão resumida SVG](imagens/fluxograma.svg)

```mermaid
flowchart TD
 Inicio((Início)) --> Login[Login]
 Login --> Token{Token válido?}
 Token -->|Não| Erro[Exibir erro e pedir login]
 Token -->|Sim| Role{Vendedor?}
 Role -->|Não| Negar[403 sem permissão]
 Role -->|Sim| Form[Preencher anúncio]
 Form --> Valido{Dados válidos?}
 Valido -->|Não| Corrigir[400 corrigir campos]
 Corrigir --> Form
 Valido -->|Sim| Salvar[Persistir anúncio]
 Salvar --> Fim((201 / atualizar lista))
```

## Contexto do sistema

Comprador e vendedor são atores; Render e operador sustentam execução. MySQL e uploads são recursos externos ao aplicativo móvel. Serviços de pagamento não integram o escopo.

[Fonte CASE editável](modelos/contexto.mmd) · [Visão resumida SVG](imagens/contexto.svg)

```mermaid
flowchart LR
 Comprador -->|consulta e favoritos| Sistema[SkipPeças]
 Vendedor -->|anúncios e perfil| Sistema
 Sistema -->|dados relacionais| MySQL[(MySQL)]
 Sistema -->|fotos| Disco[(Uploads persistentes)]
 Operador -->|deploy e variáveis| Render[Render]
 Render --> Sistema
```

## Casos de uso

Atores podem compartilhar comportamentos sem herança de classes implementada. O catálogo de especificações por tela está em PRODUCAO_TECNICA.md.

[Fonte CASE editável](modelos/casos-uso.mmd) · [Visão resumida SVG](imagens/casos-uso.svg)

```mermaid
flowchart LR
 Visitante --> Consultar[Consultar feed e marketplace]
 Visitante --> Autenticar[Registrar / entrar]
 Comprador --> Consultar
 Comprador --> Favoritar
 Comprador --> Perfil[Gerir perfil e foto]
 Comprador --> Config[Gerir bloqueios / excluir conta]
 Vendedor --> Consultar
 Vendedor --> Anunciar[Publicar anúncio]
 Vendedor --> Perfil
```

## Estado de sessão

A máquina representa a sessão. Exclusão e expiração invalidam o acesso. O campo product_requests.status admite pendente/aceita/recusada no schema, mas não foi modelado como fluxo operacional entregue por ausência de API de decisão.

[Fonte CASE editável](modelos/estados.mmd) · [Visão resumida SVG](imagens/estados.svg)

```mermaid
stateDiagram-v2
 [*] --> SemSessao
 SemSessao --> Autenticando: enviar credenciais
 Autenticando --> Autenticado: 200 e JWT
 Autenticando --> SemSessao: 400/401/500 ou rede
 Autenticado --> SemSessao: expiração / 401
 Autenticado --> ContaExcluida: DELETE confirmado 204
 ContaExcluida --> SemSessao: token rejeitado
 SemSessao --> [*]
```

## Operação de bloqueio

Bloqueio valida a identidade, bloqueia linhas em ordem consistente e realiza INSERT idempotente dentro de transação. Em falha há rollback e release; nenhuma consulta recebe IDs concatenados.

[Fonte CASE editável](modelos/operacao.mmd) · [Visão resumida SVG](imagens/operacao.svg)

```mermaid
sequenceDiagram
 actor U as Usuário
 participant V as BloqueadosFragment
 participant A as SettingsApi
 participant C as settingsController
 participant M as settingsModel
 participant D as MySQL
 U->>V: confirmar bloqueio
 V->>A: block(token, id)
 A->>C: POST /api/settings/block
 C->>M: blockUser(userId, targetId)
 M->>D: BEGIN / SELECT FOR UPDATE
 M->>D: INSERT ON DUPLICATE / SELECT
 D-->>M: bloqueio
 M->>D: COMMIT
 M-->>C: publicBlock
 C-->>A: JSON
 A-->>V: atualizar lista
```

## DFD nível 0

O processo global recebe credenciais, filtros e alterações, e devolve conteúdo, sessão e feedback. O depósito agrega as persistências detalhadas no nível 1.

[Fonte CASE editável](modelos/dfd-nivel0.mmd) · [Visão resumida SVG](imagens/dfd-nivel0.svg)

```mermaid
flowchart LR
 U[Usuário] -->|credenciais e ações| P((0. SkipPeças))
 P -->|listas / erros / sessão| U
 P -->|consultas e escritas| D[(Dados persistentes)]
 D -->|registros| P
```

## DFD nível 1

A decomposição separa identidade, conteúdo, relações e arquivos. user_follows e product_requests existem no banco; isso não implica interface completa de gestão dessas relações.

[Fonte CASE editável](modelos/dfd-nivel1.mmd) · [Visão resumida SVG](imagens/dfd-nivel1.svg)

```mermaid
flowchart LR
 U[Usuário] --> A((1. Autenticar))
 A <--> D1[(D1 users)]
 U --> B((2. Consultar e publicar))
 B <--> D2[(D2 products / posts)]
 U --> C((3. Gerir relações))
 C <--> D3[(D3 favorites / blocked_users / user_follows)]
 U --> E((4. Publicar mídia))
 E --> D4[(D4 uploads)]
 E --> D2
```

## Classes e módulos do domínio

User, Product, Post e Favorite são conceitos e DTOs; a API usa módulos funcionais, não classes JavaScript com herança artificial. UserModel e SettingsModel representam exports reais. O dicionário de dados contém todos os atributos e tipos SQL.

[Fonte CASE editável](modelos/classes.mmd) · [Visão resumida SVG](imagens/classes.svg)

```mermaid
classDiagram
 class User {
 +int id
 +string name
 +string email
 +string role
 }
 class Product {
 +int id
 +decimal price
 +bool is_active
 }
 class Post {
 +int id
 +string description
 }
 class UserModel {
 +findByEmail(email)
 +createUser(data)
 }
 class SettingsModel {
 +blockUser(userId, targetId)
 +unblockUser(userId, blockId)
 +deleteAccount(userId)
 }
 class Favorite {
 +int id
 +int user_id
 +int product_id
 }
 User "1" --> "0..*" Product : anuncia
 User "1" --> "0..*" Post : publica
 Product "0..1" <-- "0..*" Post : referencia
 User "1" --> "0..*" Favorite
 Product "1" --> "0..*" Favorite
 UserModel ..> User
 SettingsModel ..> User
```

## Entidade relacionamento

O DER foi derivado do schema consolidado; FK product_id de posts é opcional, demais FKs são obrigatórias. user_follows possui PK composta. Consultar o dicionário para nulabilidade, CHECKs, índices e cascatas exatos.

[Fonte CASE editável](modelos/der.mmd) · [Visão resumida SVG](imagens/der.svg)

```mermaid
erDiagram
  users {
    INT id
    VARCHAR(255) name
    VARCHAR(255) email
    VARCHAR(255) password
    ENUM(comprador_vendedor) role
    TEXT avatar_url
    VARCHAR(500) bio
    TIMESTAMP last_active_at
    TIMESTAMP created_at
  }
  products {
    INT id
    VARCHAR(255) title
    DECIMAL(10_2) price
    TEXT image_url
    TEXT video_url
    INT user_id
    VARCHAR(255) category
    VARCHAR(255) model
    VARCHAR(255) location
    BOOLEAN is_active
  }
  users ||--o{ products : user_id
  posts {
    INT id
    INT user_id
    INT product_id
    TEXT video_url
    TEXT image_url
    TIMESTAMP created_at
    TEXT description
    INT likes_count
    INT comments_count
  }
  users ||--o{ posts : user_id
  products |o--o{ posts : product_id
  favorites {
    INT id
    INT user_id
    INT product_id
    TIMESTAMP created_at
  }
  users ||--o{ favorites : user_id
  products ||--o{ favorites : product_id
  product_requests {
    INT id
    INT product_id
    INT buyer_id
    ENUM(pendente_aceita_recusada) status
    TIMESTAMP created_at
  }
  products ||--o{ product_requests : product_id
  users ||--o{ product_requests : buyer_id
  user_follows {
    INT follower_id
    INT followed_id
    TIMESTAMP created_at
  }
  users ||--o{ user_follows : follower_id
  users ||--o{ user_follows : followed_id
  blocked_users {
    INT id
    INT user_id
    INT blocked_user_id
    TIMESTAMP created_at
  }
  users ||--o{ blocked_users : user_id
  users ||--o{ blocked_users : blocked_user_id
```

## BPMN: publicação de anúncio

[Modelo BPMN 2.0 importável](modelos/publicar-anuncio.bpmn). As raias distinguem Usuário e API. O evento inicial leva ao preenchimento, a API valida sessão/papel/dados, o gateway exclusivo decide validade e segue para persistir/sucesso ou rejeitar/fim de erro. O fluxo de erro exige nova tentativa do usuário. As conexões dentro do mesmo pool são fluxos de sequência; não são mensagens entre participantes.

## Ferramenta CASE e reprodução

Fontes `.mmd` são modelos textuais Mermaid versionados, editáveis em ferramentas compatíveis e renderizados nas páginas Markdown que suportam Mermaid. O `.bpmn` é XML BPMN 2.0 com DI para importação em modelador compatível. `python3 scripts/gerar_modelos.py` regenera resumos SVG, inventário de wireframes e dicionário diretamente das fontes locais, sem serviços externos. Os resumos SVG não substituem o conteúdo semântico dos modelos completos. Não foi alegado uso de Figma ou de licença de ferramenta comercial.