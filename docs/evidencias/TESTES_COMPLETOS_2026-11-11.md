# Validação completa local — 11/11/2026

Executada por Codex no estado local do projeto, sem alterar código da aplicação nesta rodada. Ambiente: Node.js 24.21.0, npm 11.19.0, JDK do Android Studio em `/opt/android-studio/jbr`. Referências: RF01–RF12 e RNF01–RNF04/RNF08 nos cenários cobertos pelos testes existentes. Resultados estruturados e contagens por classe: [JSON desta execução](TESTES_COMPLETOS_2026-11-11.json).

## Resultados dos testes

| Execução | Aprovados | Falhas/erros | Ignorados | Resultado |
| --- | ---: | ---: | ---: | --- |
| Backend: `npm test` | 20 | 0 | 0 | Aprovado, incluindo smoke e integrações em memória; 8,33 s. |
| Android: `:app:test` | 33 | 0 | 1 | O teste opt-in de API real depende do executor de integração. |
| Android + API real: `npm run test:android:memory` | 34 | 0 | 0 | Aprovado, incluindo o teste antes ignorado; Gradle concluiu em 17 s. |

A execução de integração reexecuta a suíte debug com a API descartável ativa. Os 33 testes locais não devem ser somados aos 34 como casos distintos. O relatório JUnit final confirma `tests=34`, `failures=0`, `errors=0`, `skipped=0`. O caso `retrofitCompletesTheProductJourneyAgainstRealBackend` foi executado com sucesso.

O backend cobriu cadastro/login com bcrypt e JWT, autorização, sessão, feed e detalhes, filtros de marketplace, favoritos, vendas, busca/perfis, uploads de avatar e publicações, bloqueios, exclusão transacional, rollback, integridade SQLite, erros HTTP e saúde dos módulos. O smoke iniciou um processo real do servidor em `NODE_ENV=production`, provocou indisponibilidade de MySQL para testar o fallback em memória, verificou respostas HTTP/CORS e confirmou encerramento por SIGTERM sem crash. Isso não representa uma execução contra o serviço publicado.

O Android cobriu contratos Retrofit/JSON, tokens, parâmetros e filtros, preços, URLs de mídia, respostas vazias e de erro, upload binário e escolha da foto do feed. A integração criou contas e anúncio em uma API real com SQLite em memória e percorreu avatar, marketplace, detalhes, favoritos, perfil, bloqueio/desbloqueio, exclusão de conta e invalidação de sessão.

## Compilação, recursos e dependências

| Verificação | Resultado |
| --- | --- |
| Build limpo debug, testes e lint | `BUILD SUCCESSFUL in 1m 24s`; 55 tarefas executadas. |
| Build release e lint release | `BUILD SUCCESSFUL in 1m 54s`; 49 tarefas executadas, 1 atualizada. |
| Recursos, layouts, navegação e ViewBinding | Compilaram nas variantes debug e release. |
| Classes duplicadas e metadados de dependências Android | Verificações Gradle concluídas nas duas variantes. |
| Dependências npm: `npm ls --all --json` | Exit code 0, sem problemas; 161 entradas na árvore e 95 combinações pacote/versão distintas. |
| Lint debug | 0 erros/fatais; **140 avisos**. |
| Lint release | 0 erros/fatais; **132 avisos**. |

A listagem `:app:tasks --all` oferece `testDebugUnitTest`, mas não `testReleaseUnitTest`. Por isso, a tarefa agregada `:app:test` executou os testes debug. A release foi verificada com compilação completa e lint, sem modificar a configuração de testes do projeto.

As duas variantes apresentam avisos como recursos não utilizados (55), textos fixos em layouts (14), sugestões de KTX (10), overdraw (9) e preenchimento automático (9). Não são erros bloqueantes, mas a validação não significa lint sem avisos.

Relatórios gerados pelo Gradle:

- [Testes debug, incluindo integração final](../../front/projeto_que_vou_apagar_dando_der/app/build/reports/tests/testDebugUnitTest/index.html).
- [Lint debug](../../front/projeto_que_vou_apagar_dando_der/app/build/reports/lint-results-debug.html).
- [Lint release](../../front/projeto_que_vou_apagar_dando_der/app/build/reports/lint-results-release.html).

## Integridade do APK

O [APK debug 1.4](../../front/projeto_que_vou_apagar_dando_der/app/build/outputs/apk/debug/app-debug.apk) foi regenerado pelo build limpo. `aapt dump badging` e o manifesto compilado confirmaram:

- applicationId `com.example.app`, versionName `1.4`, versionCode `5`.
- Nome **SkipPeças**, Android mínimo API 24 e target API 37.
- Uma única entrada `MAIN`/`LAUNCHER` para `com.example.app.MainActivity`, exportada.
- Permissão de Internet e referências aos ícones normal e circular.

`apksigner verify --verbose` retornou `Verifies`, com assinatura APK v2 válida e um assinante. A verificação CRC do ZIP não encontrou corrupção nos APKs debug e release; ambos contêm manifesto, DEX e tabela de recursos.

| Artefato | Tamanho | SHA-256 |
| --- | ---: | --- |
| `app-debug.apk` | 8.688.042 bytes | `eab96eff34f622eb32d4e55f0563de4651df978927f978abcf25617a862a4448` |
| `app-release-unsigned.apk` | 6.904.734 bytes | `e29349714fba77cf7165b576cb78dcbe567afe253885c42b0876b6422597338e` |

A release gerada está sem assinatura, conforme a configuração atual. O artefato assinado para instalação e teste desta rodada é o APK debug. Nenhum APK foi adicionado ao controle de versão.

## Comandos executados

Em `back/`:

```bash
npm test
npm ls --all --json
```

Em `front/projeto_que_vou_apagar_dando_der/`:

```bash
JAVA_HOME=/opt/android-studio/jbr bash ./gradlew clean :app:assembleDebug :app:test :app:lintDebug --no-daemon --no-configuration-cache
JAVA_HOME=/opt/android-studio/jbr bash ./gradlew :app:tasks --all --offline --no-daemon --no-configuration-cache
JAVA_HOME=/opt/android-studio/jbr bash ./gradlew :app:assembleRelease :app:lintRelease --offline --no-daemon --no-configuration-cache
```

Por último, em `back/`, sem executar outro Gradle simultaneamente:

```bash
JAVA_HOME=/opt/android-studio/jbr npm run test:android:memory
```

O runner de memória ignora `.env`, usa uma porta local temporária, inicia banco e API descartáveis e encerra ambos ao final. A inspeção adicional usou `aapt`, `apksigner`, CRC ZIP e SHA-256. Logs brutos desta rodada ficam em `/tmp/skippecas-validacao-completa/`; os dados consolidados estão no JSON versionável vinculado acima.

## Limites

Nenhuma regressão foi detectada nos cenários executados. Não foram executados os runners de MySQL real (`test:e2e` e `test:android:e2e`), que exigem banco isolado com configuração explícita. O comportamento MySQL simulado nos testes não substitui essa integração.

`adb devices -l` retornou lista vazia e não há AVD configurado. Os testes instrumentados, a instalação/abertura no aparelho e a navegação visual ficaram pendentes. Compilação, ViewBinding e lint não comprovam ausência de toda referência nula em tempo de execução ou de todo problema visual.

O serviço publicado na Render não foi validado nem atualizado nesta rodada. Os resultados de integração acima usam a API local em memória. Não houve mudança de código, deploy ou commit para obter os resultados.
