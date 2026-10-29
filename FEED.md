# Feed e tag de produto — SkipPeças

## Backend

- `back/src/config/database.sql`: tabelas `products` e `posts`, com chaves
  estrangeiras para usuários e produtos. Produto removido deixa o post sem tag.
- `back/src/models/feedModel.js`: consulta os posts em ordem decrescente de ID,
  juntando autor e produto; consulta o produto individual por parâmetro SQL.
- `back/src/controllers/feedController.js`: respostas JSON e erros amigáveis.
- `back/src/routes/feedRoutes.js`: `GET /api/feed` e `GET /api/products/:id`.
- `back/src/server.js`: registro das rotas sob `/api`.

Exemplo de resposta de `GET /api/feed`:

```json
[
  {
    "id": 91,
    "user_id": 7,
    "user": { "id": 7, "name": "Ana" },
    "product_id": 42,
    "video_url": null,
    "description": "Peça em ótimo estado",
    "likes_count": 12,
    "comments_count": 3,
    "product": {
      "id": 42,
      "title": "Volante Mustang",
      "price": "350.90",
      "image_url": "https://seu-servidor.example/volante.jpg",
      "user_id": 7,
      "category": "Interior",
      "model": "Mustang",
      "location": "Palmas - TO"
    }
  }
]
```

`price` usa DECIMAL no MySQL e string no JSON para preservar precisão.
Posts sem produto têm `product_id: null` e `product: null`; feed vazio retorna
`[]`. Erros de banco retornam HTTP 500 sem detalhes internos. Os endpoints
de leitura são públicos nesta etapa.

`GET /api/products/42` retorna o objeto `product` acima; um ID inválido retorna
400, e produto inexistente retorna 404. Este endpoint permite abrir os detalhes
pelo ID da tag mesmo após recriar a tela Android.

### Preparar o banco

Com o MySQL iniciado, execute na pasta `back/` (inclua `-p` se houver senha):

```bash
mysql -h localhost -P 3306 -u root < src/config/database.sql
npm start
curl http://localhost:3000/api/feed
```

Não há dados fictícios inseridos automaticamente. Para experimentar com dados
locais, cadastre um vendedor por `/api/auth/register`, copie seu ID retornado e
execute este exemplo substituindo `7` pelo ID real e a URL por uma imagem válida:

```sql
USE skian_db;
START TRANSACTION;
INSERT INTO products (title, price, image_url, user_id, category, model, location)
VALUES ('Volante Mustang', 350.90, 'https://seu-servidor.example/volante.jpg',
        7, 'Interior', 'Mustang', 'Palmas - TO');
SET @product_id = LAST_INSERT_ID();
INSERT INTO posts (user_id, product_id, video_url, description, likes_count, comments_count)
VALUES (7, @product_id, NULL, 'Peça em ótimo estado', 12, 3);
COMMIT;
```

Para um post em vídeo, preencha `video_url` com uma URL de vídeo reproduzível,
por exemplo um MP4 acessível ao Android. A imagem do produto continua opcional.

## Android

Projeto: `front/projeto_que_vou_apagar_dando_der/`.
Arquivos Kotlin em `app/src/main/java/com/example/app/`:

- `network/FeedApi.kt`: `PostResponse`, `ProductTag`, autor e endpoints Retrofit.
- `network/ApiClient.kt`: compartilha o Retrofit entre autenticação e feed.
- `HomeFragment.kt`: consulta o feed ao criar a tela, mostrando progresso,
  estado vazio, erro e botão para tentar novamente. Substitui `HomeFragment.java`.
- `feed/FeedAdapter.kt`: RecyclerView com nomes, descrições, imagens via Glide,
  contadores, preço em reais e tag. Vídeo toca no card após clicar em reproduzir,
  com controles nativos. A reprodução para ao sair da tela ou reciclar o card.
- `ProductDetailsFragment.kt`: lê `product_id`, consulta o endpoint de produto
  e exibe imagem, título, preço, categoria, modelo e localização.

Layouts: `fragment_home.xml`, `item_post.xml` e `fragment_product_details.xml`.
O cabeçalho existente do Home foi mantido; o conteúdo estático virou uma lista.
`nav_graph.xml` declara a ação `action_home_to_product` e o argumento inteiro
obrigatório `product_id` da nova tela de detalhes.

O clique captura o produto do próprio card:

```kotlin
ui.buyButton.setOnClickListener { product?.let { onProductClick(it.id) } }
```

O Home passa o ID à navegação:

```kotlin
val args = Bundle().apply { putInt("product_id", productId) }
findNavController().navigate(R.id.action_home_to_product, args)
```

Posts sem produto ocultam a tag. As requisições são assíncronas e canceladas
ao destruir a view. Curtidas e comentários exibem os contadores do servidor;
esta etapa não implementa ações de curtir, comentar ou finalizar compra.

A URL base continua `http://10.0.2.2:3000/api/` para emulador. Use URLs de mídia
acessíveis pelo dispositivo: `localhost` dentro do Android aponta ao próprio
Android. O debug já permite HTTP para `10.0.2.2` e `localhost`; demais hosts de
mídia devem usar HTTPS. A configuração de release requer API HTTPS.

## Verificação

Backend, a partir de `back/`:

```bash
node --test tests/*.test.js
```

Os testes de feed usam um servidor Express real e pool MySQL simulado, cobrindo
associação do produto, post sem tag, lista vazia, consulta parametrizada, ID
inválido, produto removido e falhas internas. Não substituem a execução do SQL
e a integração com uma instância MySQL real.

Android, a partir da pasta do projeto:

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

`FeedApiTest` verifica método e URL do feed, desserialização, contadores, valores
nulos, lista vazia, 404 e consulta dos detalhes com ID de produto diferente do
ID do post.

Para conferir o fluxo visual, entre no app com o backend disponível, confirme
os dados de pelo menos dois posts com produtos distintos e toque em cada tag.
Os detalhes devem corresponder ao produto clicado. Teste também banco vazio,
servidor desligado, produto removido e mídia inválida.

Referências das bibliotecas: [RecyclerView](https://developer.android.com/develop/ui/views/layout/recyclerview)
e [Glide](https://bumptech.github.io/glide/doc/download-setup.html).
