# Busca de usuários e perfil

## Banco

Em banco existente com as migrações anteriores aplicadas, execute uma vez em
`back/` (inclua `-p` se houver senha):

```bash
mysql -h localhost -P 3306 -u root < src/config/migrations/004_user_profiles.sql
```

Adiciona `users.avatar_url`, `bio`, `last_active_at` e `user_follows`.
Para banco novo, execute somente `src/config/database.sql` completo.
Aplique a migração antes de iniciar a API: o login passa a atualizar a atividade.

## API

Todos os endpoints exigem `Authorization: Bearer <JWT>`.

```http
GET /api/users/search?q=ana&filter=vendedor
```

Busca parte do nome/e-mail, aceita `filter= todos | comprador | vendedor`
(sem espaços) ou filtro omitido. Retorna até 100 resultados ordenados por nome
e ID. `q` omitido/vazio lista os usuários da categoria. `%`, `_` e `!` digitados
são tratados como caracteres literais, com SQL parametrizado.

```json
[
  {
    "id":7,"name":"Ana","role":"vendedor","avatar_url":null,
    "last_active_at":"2026-10-25T12:00:00.000Z","activity_status":"recente"
  }
]
```

Atividade `recente` significa login ou atualização do perfil nos últimos 15
minutos; demais casos retornam `inativo`. O indicador não representa presença
online. A busca não retorna e-mails nem hashes de senha.

```http
GET /api/users/me
GET /api/users/profile/7
```

```json
{
  "id":7,"name":"Ana","email":"ana@example.com","role":"vendedor",
  "avatar_url":null,"bio":"Peças e customizações","last_active_at":null,
  "is_owner":true,"posts_count":2,"followers_count":3,"following_count":4,
  "media":[
    {
      "media_id":"post:1","kind":"post","post_id":1,"product_id":42,
      "title":"Volante Mustang","image_url":"https://example.com/p.jpg",
      "video_url":"https://example.com/p.mp4"
    }
  ]
}
```

`email` aparece somente no próprio perfil. As métricas contam registros reais
de `posts` e `user_follows`. A grade reúne posts e anúncios ativos independentes,
sem duplicar o anúncio já vinculado a um post do mesmo autor. `posts_count`
conta posts, não anúncios. IDs de mídia incluem o tipo para evitar colisões.
Não são criados seguidores ou publicações fictícios.

```http
PUT /api/users/profile
Content-Type: application/json

{"name":"Ana Silva","email":"ana@example.com","avatar_url":"https://example.com/avatar.jpg","bio":"Minha biografia"}
```

Aceita atualização parcial desses quatro campos. Avatar e biografia podem ser
limpos com `null` ou string vazia. Nome/e-mail têm limite de 255 caracteres,
biografia de 500 e avatar de 2048, com URL HTTP/HTTPS. `role`, `user_id`, senha
e demais campos são rejeitados. A edição usa exclusivamente o ID do JWT e
retorna o perfil atualizado. E-mail já cadastrado retorna 409; dados inválidos,
400; sessão inválida, 401; usuário inexistente, 404; falha interna, 500.

Backend: `userProfileModel.js`, `userController.js`, `userRoutes.js`, integrados
em `server.js`. `authController.js` e `userModel.js` registram o último login.

## Android

Projeto: `front/projeto_que_vou_apagar_dando_der/`.
Arquivos em `app/src/main/java/com/example/app/`:

- `network/UserApi.kt`: busca, próprio perfil, perfil por ID e edição.
- `UsersFragment.kt` e `users/UsersAdapter.kt`: barra editável, busca com espera
  de 350 ms, filtro de categoria, tabela com avatar/nome/categoria/atividade e
  abertura do perfil ao tocar na linha. Cancela e ignora respostas antigas.
- `PerfilFragment.kt` e `users/ProfileViewModel.kt`: avatar, nome, biografia,
  métricas, grade de três colunas e dados mantidos durante rotação.
- `users/EditProfileDialogFragment.kt`: formulário de edição do próprio perfil,
  PUT assíncrono, tratamento de e-mail duplicado e atualização imediata da tela.
- `users/ProfileMediaAdapter.kt` e `ProfileMediaDialogFragment.kt`: imagem/vídeo
  da grade; vídeos têm controles nativos e param ao sair. Imagens de anúncios
  abrem os detalhes do produto.

“Compartilhar” usa o compartilhamento nativo com `skippecas://users/{id}`;
Manifest e Navigation reconhecem esse link para abrir o perfil no app instalado.
O ícone de pessoas abre a busca e o menu abre Configurações. O botão de edição
é ocultado em perfis de terceiros. Sessões expiradas oferecem entrada no login.
Estados de vazio, carregamento, usuário inexistente e falha têm mensagens e
nova tentativa. As classes Java estáticas de Users/Perfil foram substituídas.

## Testes

```bash
# back/
node --test tests/*.test.js

# pasta do projeto Android
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

Os testes backend usam JWT real e pool MySQL simulado para cobrir autenticação,
escape da busca, categoria, validação de IDs/campos, privacidade do perfil,
métricas, mídia, edição própria, e-mail duplicado e falhas internas.
Os testes Retrofit verificam URLs, filtros, Bearer, métricas/mídias,
desserialização de perfil de terceiros, PUT, lista vazia e HTTP 409.

Para o teste visual com banco real: entre, busque por nome/e-mail, troque a
categoria, abra um resultado, confira a ausência de edição de terceiros,
edite o próprio perfil e abra uma mídia. Confira Compartilhar e o link recebido
com uma sessão autenticada no app.
