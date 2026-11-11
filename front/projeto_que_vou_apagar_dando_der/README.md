# SkipPeças — aplicativo Android

Abra esta pasta no Android Studio. O aplicativo usa Kotlin/Java, Fragments, Navigation Component e Retrofit. O launcher é `.MainActivity`, que abre o grafo na tela de login.

```bash
./gradlew clean assembleDebug testDebugUnitTest lintDebug
adb reverse tcp:3000 tcp:3000
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -W -n com.example.app/.MainActivity
```

O APK padrão conecta a `https://skippecas-api.onrender.com/api/`. Para desenvolvimento USB, compile com `./gradlew assembleDebug -PskippecasApiUrl=http://localhost:3000/api/` e inicie o backend com `PORT=3000 DB_MODE=memory npm start` dentro de `back/`, mantendo o `adb reverse` acima. Para outra API publicada, informe `-PskippecasApiUrl=https://SEU-SERVICO/api/` na compilação.

A versão 1.4 inclui o ícone SkipPeças em versões legada, adaptativa e monocromática. As fotos de demonstração são definidas pelo backend; [fontes e funcionamento](../../docs/IMAGENS_DEMO.md). O [relatório do APK](../../docs/evidencias/APK_IMAGENS_2026-11-11.md) distingue a inspeção do pacote da instalação no aparelho.

Documentação completa: [README raiz](../../README.md) e [validação Android](../../docs/VALIDACAO_ANDROID.md).
