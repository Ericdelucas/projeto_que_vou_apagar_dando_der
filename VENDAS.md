# Gestão de anúncios do vendedor

## Banco

Para banco existente com as migrações anteriores aplicadas, execute uma vez em
`back/` (acrescente `-p` se houver senha):

```bash
mysql -h localhost -P 3306 -u root < src/config/migrations/003_seller_products.sql
```

Adiciona `products.video_url` e a tabela `product_requests`, com produto,
comprador, status e data. Para banco novo, execute somente o `database.sql`
completo. A coluna `is_active` da migração 001 deve existir nos bancos anteriores.

O status do anúncio deriva de `products.is_active`: `ativo` ou `inativo`.
Contadores são calculados a partir dos registros reais de `product_requests`,
com status `pendente`, `aceita` ou `recusada`; sem solicitações, retornam zero.
Esta etapa consulta os contadores e não cria endpoints de envio ou alteração
de solicitações.

## Endpoints

As duas rotas exigem `Authorization: Bearer <JWT>` e uma conta com
`users.role = 'vendedor'`. O papel é consultado no banco, não presumido a partir
do token. O ID do vendedor sempre vem do `sub` autenticado.

```http
GET /api/seller/products
```

Retorna somente os anúncios do vendedor, incluindo os inativos, em ordem
decrescente de ID:

```json
[
  {
    "id":42,"title":"Volante Mustang","price":"350.90","user_id":7,
    "image_url":"https://example.com/volante.jpg","video_url":null,
    "category":"Interior","model":"Mustang","location":"Palmas",
    "status":"ativo","requests_count":3,"pending_requests_count":2,
    "accepted_requests_count":1,"rejected_requests_count":0
  }
]
```

```http
POST /api/seller/products
Content-Type: application/json
Authorization: Bearer <JWT>

{
  "title":"Volante Mustang",
  "price":"350.90",
  "category":"Interior",
  "model":"Mustang",
  "image_url":"https://example.com/volante.jpg",
  "video_url":null,
  "location":"Palmas"
}
```

Título, categoria e modelo são obrigatórios, com até 255 caracteres. Preço deve
ser positivo, até `99999999.99`, com até duas casas decimais. É obrigatória uma
URL HTTP/HTTPS em `image_url` ou `video_url`; ambas podem ser preenchidas.
Localização é opcional. Preços são retornados como string decimal.

O POST retorna HTTP 201 com o anúncio criado, status ativo e contadores zerados.
Anúncios cadastrados aparecem na listagem do vendedor e no Marketplace. Não é
criado um post de Feed automaticamente.

Status de erro: 400 para dados inválidos, 401 para sessão inválida/expirada,
403 para comprador e 500 para falha interna. A configuração JWT ausente retorna
503 pelo middleware de autenticação.

Backend: `sellerController.js`, `sellerModel.js`, `sellerRoutes.js` e
`sellerMiddleware.js` nas respectivas pastas em `back/src/`, integrados em
`server.js`. As consultas usam parâmetros SQL.

## Android

Projeto: `front/projeto_que_vou_apagar_dando_der/`.
Arquivos em `app/src/main/java/com/example/app/`:

- `network/SellerApi.kt`: DTOs, GET e POST com Bearer JWT.
- `VenderFragment.kt`: substitui a classe Java e consome a listagem autenticada.
- `seller/SellerAdapter.kt`: tabela em RecyclerView, mantendo colunas na
  proporção 3:1, bordas e contador do layout existente. Exibe título, preço,
  status, total de solicitações e pendentes.
- `seller/NewProductDialogFragment.kt`: formulário aberto pelo botão “+”.
- `seller/NewProductViewModel.kt`: mantém a requisição de cadastro durante
  rotação e bloqueia reenvio enquanto salva.

O toque no contador mostra o resumo de pendentes, aceitas e recusadas. Os pills
de modelo/categoria filtram localmente os anúncios retornados. Após fechar o
formulário, a lista é atualizada; falha sem confirmação pede conferir a lista
antes de repetir o cadastro. Há estados de vazio, carregamento, erro, conta sem
permissão e sessão expirada.

A URL da imagem/vídeo precisa ser acessível ao Android. Para mídia local no
emulador, use `10.0.2.2`; demais hosts devem usar HTTPS na configuração atual.
O vídeo do produto também pode ser reproduzido em seus detalhes, com controles
nativos e encerramento ao sair da tela.

## Verificação

```bash
# back/
node --test tests/*.test.js

# pasta do projeto Android
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

Testes backend usam JWT real e MySQL simulado para verificar isolamento de
vendedores, rejeição de compradores, validação de campos/URLs, contadores,
cadastro, propriedade pelo token e falhas. Testes Retrofit verificam método,
caminho, Bearer, payload, status/contadores, lista vazia e HTTP 403.

Para validar visualmente, faça login com uma conta cadastrada com
`role: "vendedor"`, abra Vender, publique uma peça e confira a linha e o
Marketplace. Contas com papel comprador recebem a mensagem de acesso restrito.
