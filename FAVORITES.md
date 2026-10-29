# Favoritos / Salvos

## Banco

`back/src/config/database.sql` inclui `favorites` com `id`, `user_id`,
`product_id`, `created_at`, chave única por usuário/produto e chaves estrangeiras
com exclusão em cascata.

Para adicionar a tabela a um banco existente, execute na pasta `back/`:

```bash
mysql -h localhost -P 3306 -u root < src/config/migrations/002_favorites.sql
```

Inclua `-p` se houver senha. O campo `products.is_active` da etapa anterior deve
existir; se necessário, aplique antes `001_products_active.sql` uma única vez.
Em banco novo, execute somente o `database.sql` completo.

## API autenticada

As duas rotas exigem `Authorization: Bearer <JWT>`. O middleware verifica a
assinatura HS256, a expiração e o ID de usuário no `sub`. O usuário sempre vem
do token, nunca de um `user_id` enviado pelo cliente.

```http
POST /api/favorites
Authorization: Bearer <JWT>
Content-Type: application/json

{"product_id":42}
```

Sem `is_favorite`, alterna o estado atual. Resposta HTTP 200:

```json
{"product_id":42,"is_favorite":true}
```

Também aceita o estado desejado, usado pelo Android para tornar repetições da
mesma requisição idempotentes:

```json
{"product_id":42,"is_favorite":false}
```

Só anúncios ativos podem ser adicionados. A remoção de salvos de anúncios
desativados continua permitida. A transação bloqueia a linha do usuário antes
de verificar/alterar o favorito, evitando alternâncias concorrentes incorretas.
Falhas executam rollback e a conexão é liberada.

```http
GET /api/favorites
Authorization: Bearer <JWT>
```

```json
[
  {
    "id":3,
    "product_id":42,
    "created_at":"2026-10-25T12:00:00.000Z",
    "product":{
      "id":42,"title":"Volante Mustang","price":"350.90",
      "image_url":"https://example.com/p.jpg","user_id":9,
      "category":"Interior","model":"Mustang","location":"Palmas"
    },
    "author":{"id":9,"name":"Vendedor"}
  }
]
```

A lista contém somente os salvos do usuário autenticado, ordenados do mais
recente ao mais antigo. Inclui produtos desativados para permitir sua remoção;
produtos excluídos são removidos dos favoritos pela chave estrangeira.
Lista vazia retorna `[]`.

Status: 400 para corpo inválido, 401 para sessão inválida/expirada, 404 para
produto indisponível ao salvar, 500 para falha interna e 503 se JWT_SECRET
não estiver configurado.

Código backend:

- `src/middleware/authMiddleware.js`
- `src/models/favoriteModel.js`
- `src/controllers/favoritesController.js`
- `src/routes/favoritesRoutes.js`, integrado em `src/server.js`

## Android

Projeto: `front/projeto_que_vou_apagar_dando_der/`.
Arquivos em `app/src/main/java/com/example/app/`:

- `network/FavoritesApi.kt`: DTOs, GET e POST com cabeçalho Bearer explícito.
- `favorites/FavoritesViewModel.kt`: estado da sessão compartilhado pela Activity,
  consulta inicial, envio do estado desejado, reconciliação após falha de rede
  e tratamento de expiração. O cache é limpo quando o token muda.
- `favorites/FavoritesUi.kt`: observação com ciclo de vida da view, botão de
  coração e Snackbar com nova tentativa ou entrada no login.
- `favorites/FavoritesAdapter.kt`: lista com imagem, título, preço, autor e remoção.
- `FavoritosFragment.kt`: substitui a classe Java e consome a lista dinâmica.

O coração foi integrado nos cards do Feed e Marketplace e na tela de detalhes.
“Salvar” muda para “Salvo” após confirmação da API; cliques ficam bloqueados
durante carregamento/alteração. O item removido desaparece de Salvos sem precisar
reabrir a tela. Os estados são compartilhados, inclusive para produtos repetidos
em vários posts.

A tela de Salvos preserva o cabeçalho e a disposição em linhas do layout local,
com RecyclerView no lugar dos produtos estáticos. O acesso existente por
Configurações → Salvos continua funcionando. Clicar no item abre seus detalhes.
Há estados de carregamento, vazio, erro e sessão expirada.

## Verificação

```bash
# back/
node --test tests/*.test.js

# pasta do projeto Android
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

Os testes HTTP backend usam JWT real e pool/transações MySQL simulados. Cobrem
rejeição de tokens, isolamento entre contas, alternância, estados explícitos
idempotentes, listagem com autor, validação, produto indisponível, rollback e
liberação de conexão. Os testes Android verificam cabeçalho Bearer, payload de
salvar/remover, desserialização da lista, lista vazia e HTTP 401.

Para a validação visual, use MySQL/backend disponíveis: faça login, salve uma
peça no Marketplace, confira o coração no Feed e em detalhes, abra Salvos e
remova o item. Reinicie o app para confirmar a persistência e teste uma segunda
conta para conferir o isolamento. Teste também servidor desligado e JWT expirado.
