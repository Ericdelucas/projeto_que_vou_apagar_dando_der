# Marketplace

## Banco existente

Para um banco criado antes desta etapa, execute **uma vez** na pasta `back/`:

```bash
mysql -h localhost -P 3306 -u root < src/config/migrations/001_products_active.sql
```

Inclua `-p` se houver senha. A migração adiciona `products.is_active`, padrão
`TRUE`, e um índice. Em banco novo, use `src/config/database.sql`, que já contém
essa coluna; não execute também a migração. Não há atualização automática do
schema na inicialização do servidor.

Um anúncio é ativo quando `is_active = TRUE`. Os produtos anteriores passam a
ser ativos por padrão. Para retirar um anúncio da listagem, altere o campo para
`FALSE` no banco.

## API

```http
GET /api/marketplace?model=Mustang&category=Interior&location=Palmas%20-%20TO&min_price=100.00&max_price=500.00
```

Todos os parâmetros são opcionais e combinados com `AND`. Os filtros de texto
usam igualdade conforme a collation do banco; strings vazias não filtram.
Os limites de preço são inclusivos, aceitam zero e usam ponto decimal com até
duas casas. Preços inválidos, parâmetros repetidos e mínimo maior que máximo
retornam HTTP 400 antes de consultar o banco.

Resposta: lista de produtos ativos, ordenada por ID decrescente, com os campos
`id`, `title`, `price`, `image_url`, `user_id` (vendedor), `category`, `model` e
`location`. `price` permanece string decimal. Sem resultados: `[]`.
Falha interna: HTTP 500 com mensagem amigável.

```http
GET /api/marketplace/filters
```

```json
{
  "models": ["Fiesta", "Mustang"],
  "categories": ["Interior", "Rodas"],
  "locations": ["Palmas - TO", "São Paulo"]
}
```

As opções são distintas, ordenadas e derivadas de todos os anúncios ativos,
independentemente dos filtros selecionados. Nenhuma lista é fixa no Android.

Backend: `marketplaceModel.js`, `marketplaceController.js` e
`marketplaceRoutes.js` nas respectivas pastas em `back/src/`, integrados em
`server.js`. SQL parametrizado protege todos os valores de filtro.

## Android

Projeto em `front/projeto_que_vou_apagar_dando_der/`:

- `network/MarketplaceApi.kt`: listagem e opções de filtro no Retrofit.
- `MarketplaceFragment.kt`: substitui a classe Java; abre dropdowns nos pills
  existentes e consulta a API a cada mudança, combinando as seleções.
- `marketplace/MarketplaceAdapter.kt`: grid de duas colunas com imagem, título,
  preço em reais e navegação para os detalhes pelo ID do card clicado.
- `fragment_marketplace.xml`: preserva cabeçalho, cores e disposição dos filtros;
  substitui os cards fixos por RecyclerView.
- `item_marketplace_product.xml`: layout reutilizável de cada card.

Os arquivos Kotlin acima ficam em `app/src/main/java/com/example/app/`.

Cada dropdown oferece “Todos” para remover sua seleção. Os filtros permanecem
após rotação e retorno dos detalhes. Requisições anteriores são canceladas e
callbacks antigos ignorados; erro e lista vazia têm estados próprios. Há botão
para tentar novamente e tratamento separado para falha ao carregar as opções.

A faixa de preço está disponível na API e nos parâmetros opcionais do Retrofit;
a interface mantém os três filtros do desenho: modelo, categoria e local.
O texto “Categoria: Ano” é o rótulo original do filtro `category`; o schema
atual não tem um campo de ano separado.

## Verificar

```bash
# back/
node --test tests/*.test.js

# pasta do projeto Android
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

Os testes usam pool MySQL simulado no backend e MockWebServer no Android.
Cobrem parâmetros combinados, SQL parametrizado, faixa de preço, opções,
lista vazia, respostas de erro e serialização dos filtros no Retrofit.
Para testar visualmente, inicie MySQL/backend e abra o Marketplace no emulador;
selecione e remova cada filtro e confira a combinação no grid.
