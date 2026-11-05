# Correções de Home, Vender, perfil, fotos e tema

## Comportamento entregue

- Home: busca digitável e filtros combinados de modelo, categoria e peça com opção Todos, resultado vazio e seleção preservada. As opções vêm dos produtos reais associados às publicações. Publicações sem peça continuam aparecendo quando os filtros são limpos. Não existe campo de ano no schema; o rótulo Categoria: Ano foi substituído por Categoria.
- Vender: botões de filtro com área de toque adequada e opções de modelo, categoria e peça dos próprios anúncios. A lista de vendedores continua acessível aos compradores; filtros dos anúncios privados não são oferecidos quando a API retorna 403 para comprador. Isso preserva a autorização existente.
- Perfil: editor Material com campos delineados, espaçamento, botões coerentes com o vermelho da marca e seleção de foto da galeria com prévia circular. Não exige digitar URL. A imagem é redimensionada para até 1600px e enviada como JPEG ao salvar.
- Fotos: a API pública estava devolvendo http://skippecas-api.onrender.com/...; o Android bloqueia esse tráfego na distribuição normal. A nova versão normaliza mídia do próprio servidor para HTTPS, resolve URLs relativas e mantém a foto dos posts sem produto ao reciclar a lista. O backend também gera links usando RENDER_EXTERNAL_URL e considera o proxy da Render. Fotos de terceiros não são substituídas por conteúdo inventado; posts só de texto continuam sem foto. As sementes já tinham imagens de demonstração, que também passam a carregar.
- Tema: fundos, textos, cartões e divisores seguem atributos DayNight. O modo escuro usa superfícies #121212/#202020 e texto claro; o vermelho de marca é preservado. A seleção de tema continua salva nas preferências.

## Publicar o backend é necessário para foto de perfil

O APK usa a mesma URL HTTPS pública, sem configuração manual. A nova rota `POST /api/users/avatar` precisa chegar à Render antes de trocar a foto pela galeria no serviço público. Foi implementada e testada localmente, mas este trabalho não fez commit, push ou deploy remoto.

1. Faça commit e push das alterações do projeto para a branch main usada pela Render.
2. Em Render → skippecas-api → Manual Deploy → Deploy latest commit, publique o código atualizado; se o Auto-Deploy executar, aguarde seu término.
3. Mantenha Root Directory=back, Build Command=npm install, Start Command=npm start e Health Check Path=/health. Não há variável nova obrigatória: RENDER_EXTERNAL_URL já é fornecida pela plataforma.
4. Instale o APK versão 1.2 sobre a versão anterior. Abra Perfil → Editar perfil → Alterar foto da galeria → escolha imagem → Salvar.
5. Para testar filtros dos seus anúncios, entre com uma conta vendedor; a conta comprador não ganha permissão de venda por alterar o filtro.

O serviço ainda está em memória; o deploy reinicia os dados de demonstração e invalida suas sessões. Faça login novamente depois do deploy. Arquivos em uploads continuam exigindo armazenamento persistente e o banco deve ser MySQL para dados duráveis, conforme RELATORIO_DEPLOY_RENDER.md.

## Evidências e limites

- Backend: 20 testes, zero falhas; novo teste cobre upload autenticado, validação de formato, limite de tamanho, isolamento de titular e links HTTPS.
- Android: testes novos cobrem combinação/limpeza dos filtros, busca sem acentos, resolução de mídia e contrato HTTP de upload de avatar. Ver contagem final em evidencias/interface-tests.json.
- Build e lint são executados antes de entregar o APK. Lint pode manter avisos existentes; não são evidência de validação visual completa.
- API publicada: verificados HTTP 200 e Content-Type image/jpeg para foto já enviada (316904 bytes), e image/png para imagem de demonstração (1294 bytes), acessadas por HTTPS.
- ADB sem dispositivo: não foram executados cliques, seleção real da galeria ou screenshots em celular. A renderização final e o comportamento do seletor de imagens devem ser conferidos após instalação.

O upload do avatar e a edição dos demais campos são duas requisições: se a segunda falhar (por exemplo, e-mail duplicado), a foto já pode ter sido atualizada. A interface mantém a mensagem de falha para corrigir os campos e tentar novamente. O processo não promete atomicidade entre arquivo e todos os campos do perfil.

Resultado final: 20 testes backend e 32 testes Android aprovados, sem falhas ou testes ignorados. Build final aprovado; lint com 0 erros e 140 avisos. APK versão 1.2 / versionCode 3.
