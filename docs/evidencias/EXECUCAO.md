# Evidências da complementação

Data: 04/11/2026. Execução local pelo agente no repositório Engenharia. Os resultados abaixo descrevem comandos realmente executados; não constituem homologação humana ou deploy remoto.

## Backend

`cd back && npm test`: **19 testes aprovados, 0 falhas, 0 ignorados**, aproximadamente 9,2 segundos. Inclui autenticação, roteamento de banco, feed, marketplace, favoritos, usuário, vendedor, configurações, publicação, integridade SQLite, saúde e smoke do servidor real.

O novo `errors.test.js` verificou JSON malformado (400), rota inexistente (404) e corpo excessivo (413), com resposta JSON e ausência de stack, caminho de dependências ou segredo de entrada. A captura global também retorna mensagem genérica para falhas inesperadas (500); esse ramo permanece sujeito a revisão/expansão de testes.

O smoke verificou 200 nas oito rotas críticas, login inválido 401, acesso protegido sem token 401, vendedor com papel incorreto 403, preflight 204 e CORS aberto. A inicialização com MySQL indisponível selecionou SQLite em memória e o processo encerrou normalmente por SIGTERM.

## Android

O primeiro build offline não avançou porque faltava dependência do lint no cache. A execução com acesso às dependências prosseguiu. Foi necessário usar `JAVA_HOME=/opt/android-studio/jbr` porque Java não estava no PATH.

Ao ativar BuildConfig para URL configurável, o JDK detectou um Javadoc desconectado no arquivo gerado pelo AGP. A categoria `dangling-doc-comments` foi excluída do lint do compilador Java; os demais warnings continuam tratados como erro via `-Werror`. Não foi desativado o Android lint.

`./gradlew clean assembleDebug testDebugUnitTest lintDebug --no-configuration-cache` executou build limpo e gerou APK. Os testes passaram, mas o lint inicialmente falhou em dois domínios sem `includeSubdomains`; ambos foram corrigidos explicitamente para `false`.

`cd back && JAVA_HOME=/opt/android-studio/jbr npm run test:android:memory`: **BUILD SUCCESSFUL**, aproximadamente 20 segundos. A fixture abriu API Express real em porta livre, com SQLite descartável, e executou a suíte Retrofit: **24 testes, 0 falhas, 0 erros, 0 ignorados**. O teste de integração existente exerceu cadastro, login, anúncio, filtros, favoritos, perfil, bloqueio/desbloqueio, exclusão e revogação. O resumo por classe foi exportado dos XMLs do Gradle para [android-tests.json](android-tests.json).

Esse fluxo roda na JVM com Retrofit, não em emulador ou celular; não comprova renderização visual.

## Launcher e dispositivo

Manifesto de origem: MainActivity exportada, MAIN e LAUNCHER presentes. O pacote compilado foi inspecionado com `aapt dump badging`: etiqueta SkipPeças e Activity com.example.app.MainActivity.

`adb devices -l` retornou lista vazia. Portanto não foram executados uninstall/install, abertura da Activity, teste do botão Abrir, capturas ou teste de tamanhos em dispositivo. O aparelho não foi modificado.

## Pendências externas

- MySQL real: a tentativa da etapa anterior terminou com ECONNREFUSED em 127.0.0.1:3306; não há aprovação atual de E2E MySQL.
- Serviço Render, SSL público e persistência de fotos não foram provisionados/testados.
- Figma, aprovação visual, ensaio de produção e revisão individual da equipe não foram fabricados.
- Não houve novo commit ou push nesta complementação; os arquivos estão disponíveis para revisão no workspace.

## Validação final do APK e lint

Após a correção dos domínios, `JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug lintDebug --offline --no-configuration-cache` terminou com **BUILD SUCCESSFUL** em aproximadamente 29 segundos. Android lint: **0 erros e 142 avisos**. Os avisos incluem atualizações sugeridas de dependências, recursos/layouts, plurais e alocações em desenho; não foram ocultados por baseline ou desativação do lint. Devem ser triados antes da homologação final.

O APK final e seu hash estão registrados em [apk.json](apk.json); o launcher extraído está em [apk-launcher.txt](apk-launcher.txt). O APK permanece no diretório de build ignorado pelo Git.
