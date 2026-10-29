# Configurações, bloqueios e exclusão de conta

## Banco e execução

O módulo usa Express, MySQL e o middleware JWT existente. Para uma instalação nova, `back/src/config/database.sql` já inclui `blocked_users`.

Para um banco existente, aplique as migrações anteriores e depois `back/src/config/migrations/005_settings_and_blocks.sql`. Exemplo com o cliente MySQL, informando a senha interativamente:

```sh
mysql -u SEU_USUARIO -p < back/src/config/migrations/005_settings_and_blocks.sql
```

A migração usa o banco `skian_db`, seguindo os scripts existentes. Ajuste o `USE` se a instalação usar outro banco. A inicialização do servidor não aplica migrações automaticamente.

A tabela possui id, user_id, blocked_user_id e created_at; a combinação de origem/destino é única, não aceita autobloqueio e ambas as referências de usuário têm exclusão em cascata.

## Contratos HTTP

Todas as rotas recebem `Authorization: Bearer <token>`. A identidade vem do `sub` validado pelo middleware, nunca de `user_id` enviado no corpo. O middleware verifica a existência da conta em todas as rotas protegidas: excluir a conta invalida seus JWTs também nos outros módulos.

| Método | Caminho | Resultado |
|---|---|---|
| GET | `/api/settings/blocked` | 200 com lista de bloqueios do solicitante; lista vazia é `[]` |
| POST | `/api/settings/block` | Corpo `{"blocked_user_id": 8}`; 200 com o bloqueio, inclusive em repetição idempotente |
| DELETE | `/api/settings/block/{id}` | 204; `id` identifica a relação de bloqueio, não o usuário de destino |
| DELETE | `/api/settings/account` | 204 após commit da exclusão; sem corpo |

Cada bloqueio retorna:

```json
{
  "id": 31,
  "blocked_user_id": 8,
  "created_at": "2026-10-25T12:00:00.000Z",
  "user": { "id": 8, "name": "Ana", "role": "comprador", "avatar_url": null }
}
```

O formato exato da data segue a serialização do driver MySQL. Dados privados como e-mail e senha não são retornados. Entradas inválidas e autobloqueio retornam 400, autenticação inválida retorna 401, alvo ou bloqueio indisponível retorna 404 e falha interna retorna 500 com mensagem genérica. Remover um bloqueio de outra pessoa também retorna 404.

## Exclusão de conta

A exclusão é física e transacional. Primeiro bloqueia a linha da conta e depois remove bloqueios nos dois sentidos, seguidores/seguidos, favoritos próprios, solicitações próprias, posts próprios, produtos próprios e o usuário. As FKs dos produtos removem seus favoritos e solicitações, inclusive de terceiros; posts de terceiros vinculados ao produto permanecem com product_id nulo, conforme o schema existente.

Qualquer falha desfaz a transação inteira. Nenhum endpoint aceita escolher outra conta para excluir. No Android, a confirmação explica que a operação é irreversível. Somente após uma resposta de sucesso o app limpa o TokenStore e abre o login removendo o histórico de navegação autenticado.

## Android

- `SettingsApi.kt`: contratos Retrofit e modelos de bloqueio.
- `SettingsViewModel.kt`: estado observável, operações serializadas, tratamento de erros e sessão, requisições preservadas durante rotação e canceladas quando o ViewModel é descartado.
- `ConfiguracoesFragment.kt`: substitui o fragment Java, conecta Salvos aos favoritos, Bloqueados à nova lista, Loja a um diálogo informativo com acesso ao marketplace e Temas ao seletor persistente. A exclusão exige confirmação e desabilita ações enquanto aguarda o servidor.
- `BloqueadosFragment.kt` e `BlockedUsersAdapter.kt`: lista com avatar, nome, função, estado vazio, progresso, erro/repetição e confirmação para desbloquear.
- `BlockUserDialogFragment.kt`: busca pelo nome usando a API de usuários existente; permite selecionar e confirmar um bloqueio. Não exige que a pessoa conheça IDs internos.
- `ThemeSettings.kt`: preferência local de tema do sistema/claro/escuro, reaplicada ao iniciar a Activity.

O escopo de bloqueios deste módulo é cadastrar, consultar e remover a lista pessoal. Os endpoints públicos atuais de feed/marketplace não recebem a identidade do visitante; filtragem de conteúdo por bloqueio nesses endpoints não faz parte desta alteração.

## Validação

Backend, a partir de `back/`:

```sh
node --test tests/*.test.js
```

Android, a partir de `front/projeto_que_vou_apagar_dando_der/`:

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

Os testes do backend usam HTTP e JWT reais com persistência simulada: verificam autenticação, entradas inválidas, isolamento de origem, repetição de bloqueio, desbloqueio de terceiros, exclusão dos vínculos e rollback. Os testes Android usam MockWebServer para verificar URLs, métodos, headers, corpo, desserialização, respostas vazias e erros HTTP. Eles não substituem um teste de integração contra um MySQL real.

Validação manual: entrar, abrir Configurações, visitar Salvos/Loja, alternar o tema e reabrir o app; buscar e bloquear outro usuário; verificar a lista e desbloquear; cancelar a exclusão; confirmar a exclusão e verificar retorno ao login. Em banco de teste, conferir que a conta e seus vínculos desapareceram e que falhas de banco não deixam remoções parciais.
