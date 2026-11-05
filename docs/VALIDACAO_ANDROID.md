# Android — launcher, APK e roteiro de validação

## Launcher e identificação

Verificado em `app/src/main/AndroidManifest.xml`: `.MainActivity` é exportada e contém `android.intent.action.MAIN` e `android.intent.category.LAUNCHER`. Ela carrega `nav_graph.xml`, cuja entrada é `nav_login`. A etiqueta em `strings.xml` foi alinhada para **SkipPeças**; o applicationId permanece `com.example.app` para preservar compatibilidade com instalações anteriores.

Também foi inspecionado o APK gerado com `aapt dump badging`: `application-label:'SkipPeças'` e `launchable-activity: name='com.example.app.MainActivity'`. Isso prova a presença do launcher no pacote compilado, não a instalação em celular.

A combinação MAIN/LAUNCHER oferece a entrada na gaveta. Sua ausência não converte o aplicativo em serviço de segundo plano. Referência: [Android — tasks e launcher](https://developer.android.com/guide/components/activities/tasks-and-back-stack).

## Build limpo e instalação

Da raiz:

```bash
cd front/projeto_que_vou_apagar_dando_der
./gradlew clean assembleDebug testDebugUnitTest lintDebug --no-configuration-cache
```

Se Java não estiver no PATH, configure JAVA_HOME para o JDK compatível instalado. Neste computador foi usado `/opt/android-studio/jbr`. O APK fica em `app/build/outputs/apk/debug/app-debug.apk`. No Android Studio, Clean Project e geração de APK executam tarefas equivalentes; aguarde o resultado final, não apenas a existência do arquivo.

Atualize preservando dados, com celular autorizado e adb no PATH:

```bash
adb devices -l
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.LAUNCHER com.example.app
adb shell am start -W -n com.example.app/.MainActivity
```

Se houver erro de assinatura incompatível, faça backup dos dados necessários e só então considere desinstalar a versão antiga. A desinstalação apaga os dados locais. A instalação pelo gerenciador de arquivos pode exigir permissão para instalar aplicativos dessa origem. O botão Abrir depende do instalador; a evidência técnica é a Activity resolvida e iniciada com sucesso. Referência: [build Android por linha de comando](https://developer.android.com/build/building-cmdline).

Nenhum celular estava conectado no ADB durante esta execução. Não foram feitos uninstall, install, abertura, screenshots nem confirmação física do ícone. Não há AVD configurado no ambiente inspecionado. Essas etapas permanecem pendentes.

## API local e produção

Na raiz, terminal do servidor:

```bash
cd back
PORT=3000 DB_MODE=memory npm start
```

No terminal do Android:

```bash
adb reverse tcp:3000 tcp:3000
```

O backend tem fallback de porta 10000 para a Render; o roteiro USB define 3000 explicitamente. O APK padrão usa `https://skippecas-api.onrender.com/api/`. Para desenvolvimento USB, compile com `-PskippecasApiUrl=http://localhost:3000/api/`. Para compilar apontando à API publicada, substitua o domínio de exemplo pela URL real:

```bash
./gradlew assembleDebug -PskippecasApiUrl=https://SEU-SERVICO.onrender.com/api/
```

Essa propriedade gera BuildConfig.API_BASE_URL; exige HTTPS para endereços externos e sufixo `/api/`. Para distribuição, configure assinatura e build release; o APK debug é para teste, não representa publicação na Play Store. A comunicação HTTP local é permitida apenas pelo manifesto/configuração de debug.

## Ensaio proposto e coleta de evidências

Este é um roteiro a executar, não um ensaio concluído. Demonstre primeiro no modo memory e depois repita na URL de produção com MySQL persistente. Não use dados pessoais reais na banca.

| Etapa | Ação | Resultado esperado | Evidência a coletar |
| --- | --- | --- | --- |
| 1 | Instalar e abrir pela gaveta | Ícone SkipPeças e login | Print do ícone e login |
| 2 | Entrar com senha inválida e depois válida | Erro amigável; depois Home | Prints de ambos os estados |
| 3 | Abrir feed e detalhes | Conteúdo, autor e peça | Print da Home e detalhes |
| 4 | Filtrar marketplace e produzir busca vazia | Resultados coerentes e vazio explícito | Prints da seleção e vazio |
| 5 | Salvar/remover favorito | Estado e lista atualizados | Prints antes/depois |
| 6 | Entrar como vendedor e criar anúncio | Formulário validado; anúncio próprio | Print de validação e sucesso |
| 7 | Buscar vendedor e abrir perfil | Perfil correto, sem e-mail privado | Print da busca/perfil |
| 8 | Editar perfil e publicar foto | Preview e publicação no perfil/feed | Prints dos estados |
| 9 | Bloquear/desbloquear | Lista isolada e operação coerente | Prints da lista e alteração |
| 10 | Alterar tema, rotacionar e testar larguras | Conteúdo acessível e sem corte | Capturas 360dp/600dp/1240dp e paisagem |
| 11 | Desligar rede e tentar carregar | Erro, recuperação e loading sem travar | Gravação curta |
| 12 | Excluir conta descartável | Confirmação e sessão invalidada | Print sem dados sensíveis |
| 13 | Em produção, reiniciar serviço | Contas e fotos preservadas; HTTPS válido | Health e verificação de persistência |

Registrar responsável, dispositivo/API Android, data, versão/commit, resultado e caminho das imagens em `docs/evidencias/`. Não marcar sucesso sem executar. É necessária aceitação do avaliador para substituir critérios SPA/desktop pela navegação Android e dispositivos previstos.

## Resultado desta execução

Build limpo gerou o APK. Após corrigir dois erros de rede, build final e lint passaram: **0 erros e 142 avisos de lint**. Backend: **19 testes aprovados**. Android/Retrofit contra API real em memória: **24 aprovados, nenhum ignorado**. Veja [evidências](evidencias/EXECUCAO.md). A validação no aparelho continua pendente.
