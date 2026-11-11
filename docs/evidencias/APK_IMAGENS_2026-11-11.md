# APK, launcher e imagens — 11/11/2026

Responsável pela execução: Codex, no ambiente local do projeto. Requisitos relacionados: RF02, RF03, RF12, RNF04, RNF06 e RNF08. As alterações locais que já existiam, incluindo foto de perfil, foram preservadas.

## Resultado

| Verificação executada | Resultado |
| --- | --- |
| Backend, `npm test` | 20 testes aprovados; 0 falhas; 0 ignorados. Inclui smoke do servidor real. |
| Android, build limpo + testes unitários + lint | `BUILD SUCCESSFUL`; 55 tarefas executadas. Na execução sem API de integração: 33 testes aprovados e 1 opt-in ignorado. |
| Android/Retrofit contra API real em memória | `npm run test:android:memory`: 34 testes aprovados; 0 falhas; 0 ignorados. O teste `retrofitCompletesTheProductJourneyAgainstRealBackend` foi executado. |
| Android lint | 0 erros e 140 avisos. Relatório em `app/build/reports/lint-results-debug.html`. |
| Fotos externas | Cinco URLs com HTTP 200 e `Content-Type: image/jpeg`; conteúdo baixado e inspecionado visualmente. |
| Manifesto do APK | Um launcher, `com.example.app.MainActivity`, exportado; nome SkipPeças; Internet declarada; ícones normal e circular referenciados. |
| Assinatura do APK | `apksigner verify --verbose`: `Verifies`, APK Signature Scheme v2 válido, um assinante. |
| Recursos legados do APK | Os dez WebP empacotados são idênticos aos novos ícones em res/mipmap-*. |
| Dispositivo | `adb devices -l`: lista vazia. Nenhum AVD configurado em `~/.android/avd`. Instalação, abertura e interface no aparelho não foram executadas. |

O lint ainda informa avisos de recursos não usados, textos, acessibilidade, layouts e dependências, entre outros. O resultado não equivale a uma validação visual completa. A integração MySQL isolada não foi executada; a integração real usada aqui foi SQLite em memória. Não houve deploy para a Render.

## Artefato

- APK: `front/projeto_que_vou_apagar_dando_der/app/build/outputs/apk/debug/app-debug.apk`.
- Versão: **1.4**, versionCode **5**, applicationId **com.example.app**, Android mínimo **7.0 / API 24**.
- API padrão: `https://skippecas-api.onrender.com/api/`.
- Tamanho: **8.688.042 bytes**.
- SHA-256: `eab96eff34f622eb32d4e55f0563de4651df978927f978abcf25617a862a4448`.
- É um APK debug assinado para teste; não foi versionado.

## Diagnóstico e alterações

O manifesto de origem já tinha `android:exported="true"` e um único filtro `MAIN` + `LAUNCHER` na MainActivity. O manifesto compilado também foi inspecionado com `aapt dump xmltree`. Nenhum filtro adicional foi necessário. O filtro `VIEW` de deep link foi preservado. Não foi identificada nesses arquivos uma causa para o relato de aplicativo ausente; sem inspecionar o aparelho e o APK anterior, a causa da instalação permanece indeterminada.

Os recursos padrão do Android foram substituídos pelo volante branco com fundo vermelho: dez ícones legados, foreground/background vetoriais e uso da camada monocromática já referenciada pelos adaptativos. O gerador reproduzível é `scripts/gerar_icones.py`.

O catálogo memory deixou de usar `/demo/peca.png`: há fotos HTTPS distintas de volante, farol, roda/pneu, motor e carro. O middleware preserva URLs externas. As fixtures de integração usam imagens reais, sem chamadas ao CDN nos testes automatizados. O seed mantém os mesmos IDs, preços, categorias e quantidades usados pelos demais testes.

FeedAdapter e MarketplaceAdapter já carregavam URLs válidas com Glide; os drawables eram placeholders ou imagens do próprio seed. Foi corrigida a escolha de foto no feed: uma URL da publicação vazia ou só com espaços passa a usar a foto do produto. O teste cobre também a prioridade da foto própria e posts sem mídia.

Os testes do backend verificam o catálogo retornado por feed, marketplace e detalhes, e a preservação do CDN quando `RENDER_EXTERNAL_URL` está definido. O teste Android de integração verifica que a URL cadastrada no anúncio chega intacta aos detalhes.

## Reprodução

Ambiente: Node.js **24.21.0**, npm **11.19.0**, Java do Android Studio em `/opt/android-studio/jbr`, SDK configurado no projeto. Na raiz:

```bash
cd back
npm test
```

Na pasta `front/projeto_que_vou_apagar_dando_der/`:

```bash
JAVA_HOME=/opt/android-studio/jbr bash ./gradlew clean :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --no-daemon --no-configuration-cache
```

Depois, em `back/`:

```bash
JAVA_HOME=/opt/android-studio/jbr npm run test:android:memory
```

O executor de integração não lê `.env`, cria API e banco descartáveis e encerra ambos no fim. Os resultados finais de JUnit ficam em `app/build/test-results/testDebugUnitTest/` (34 testes, zero falhas/erros/ignorados).

Inspeções adicionais, na pasta Android:

```bash
/home/eric/Android/Sdk/build-tools/36.0.0/aapt dump badging app/build/outputs/apk/debug/app-debug.apk
/home/eric/Android/Sdk/build-tools/36.0.0/aapt dump xmltree app/build/outputs/apk/debug/app-debug.apk AndroidManifest.xml
/opt/android-studio/jbr/bin/java -jar /home/eric/Android/Sdk/build-tools/36.0.0/lib/apksigner.jar verify --verbose app/build/outputs/apk/debug/app-debug.apk
```

## Aplicação no celular e no serviço

Instale/atualize usando o APK desta compilação e procure SkipPeças na gaveta. Com aparelho autorizado por USB, siga os comandos de instalação, resolução de Activity e abertura em [VALIDACAO_ANDROID.md](../VALIDACAO_ANDROID.md). A inspeção do APK não substitui essa etapa.

Para receber as novas fotos da API padrão, publique o backend atualizado na Render. No modo memory, o seed será recriado na inicialização; alterações da sessão anterior são voláteis. Em MySQL, registros existentes não são alterados automaticamente. As fotos são ilustrativas e dependem do CDN Pexels; [fontes e créditos](../IMAGENS_DEMO.md).
