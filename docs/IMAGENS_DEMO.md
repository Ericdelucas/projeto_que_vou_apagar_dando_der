# Fotos de demonstração e ícone SkipPeças

Relaciona-se a RF02 (feed), RF03 (marketplace), RF12 (demonstração) e RNF06/RNF08 (usabilidade e APK reproduzível).

## Fotos

O catálogo em `back/src/config/demoMedia.js` contém URLs HTTPS diretas do Pexels, com ID fixo e largura de 960 pixels. O seed de `DB_MODE=memory` distribui cinco fotos entre as publicações e três fotos correspondentes entre os produtos. São imagens ilustrativas; não comprovam modelo, compatibilidade ou condição das peças de demonstração.

| Tema | Autor | Página da foto |
| --- | --- | --- |
| Volante e painel | Erik Mclean | [Pexels 10040636](https://www.pexels.com/photo/steering-wheel-and-car-dashboard-10040636/) |
| Farol e roda | Sofía Nuñez | [Pexels 19318130](https://www.pexels.com/photo/headlight-and-wheel-of-black-car-19318130/) |
| Roda e pneu | Julia Avamotive | [Pexels 8305224](https://www.pexels.com/photo/black-and-silver-car-wheel-8305224/) |
| Motor | Mike Bird | [Pexels 190574](https://www.pexels.com/photo/greyscale-photography-of-car-engine-190574/) |
| Carro esportivo | Fonte Pexels | [Pexels 12299323](https://www.pexels.com/photo/a-black-sports-car-parked-on-the-road-12299323/) |

Confira a [licença Pexels](https://www.pexels.com/license/). As cinco URLs retornaram HTTP 200 e `image/jpeg` na validação de 11/11/2026; as imagens baixadas foram inspecionadas visualmente. O serviço externo e a conexão do aparelho continuam necessários para carregar fotos que ainda não estejam no cache do Glide.

Os adapters do feed e marketplace já carregavam a URL da API com Glide. O volante do drawable permanece apenas durante o carregamento e em erros. No feed, `coverImageUrl` agora usa a foto do produto também quando a URL da publicação é vazia ou contém só espaços; uma foto própria da publicação continua tendo prioridade. Os testes verificam esse comportamento, a diversidade do seed e a preservação das URLs pelo backend, inclusive com `RENDER_EXTERNAL_URL` definido.

`back/public/demo/peca.png` foi mantido como arquivo local dos testes de upload. Ele deixou de ser usado no catálogo. As fixtures de integração MySQL e Android também usam foto remota de volante. Mocks de contratos HTTP continuam podendo usar `example.com`, pois não baixam imagens. As migrações SQL existentes definem o schema e não contêm seed de catálogo: não é necessária alteração de schema nem migração de dados reais.

Para aplicar o seed localmente, reinicie o backend em modo memory. Esse modo recria o banco e perde alterações anteriores da sessão. No serviço publicado, é preciso disponibilizar o backend atualizado para que o APK receba as novas URLs. Um novo APK sozinho não atualiza o banco da API na Render. Dados MySQL já existentes continuam usando as imagens cadastradas em seus registros.

## Ícone do aplicativo

O launcher usa um volante branco sobre o vermelho `#E51923` da marca. Os recursos incluem:

- WebP normal e circular em mdpi, hdpi, xhdpi, xxhdpi e xxxhdpi, para Android 7.
- Background e foreground vetoriais, usados pelos ícones adaptativos de Android 8 ou superior.
- Camada monocromática nos ícones adaptativos, para launchers que oferecem ícones temáticos.

O manifesto já aponta para `@mipmap/ic_launcher` e `@mipmap/ic_launcher_round`. A geração é reproduzível com `python3 scripts/gerar_icones.py` (requer Pillow). O foreground fica dentro da área segura de 66/108 do ícone adaptativo. O APK desta alteração é a versão 1.4, versionCode 5, mantendo o applicationId para permitir atualização de instalações com assinatura compatível.

Veja a [validação de launcher e instalação](VALIDACAO_ANDROID.md) e os [resultados desta execução](evidencias/APK_IMAGENS_2026-11-11.md).
