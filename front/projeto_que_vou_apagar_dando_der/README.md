# SkipPeças — aplicativo Android

Abra esta pasta no Android Studio. O aplicativo usa Kotlin/Java, Fragments, Navigation Component e Retrofit. O launcher é `.MainActivity`, que abre o grafo na tela de login.

```bash
./gradlew clean assembleDebug testDebugUnitTest lintDebug
adb reverse tcp:3000 tcp:3000
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -W -n com.example.app/.MainActivity
```

Inicie o backend com `PORT=3000 DB_MODE=memory npm start` dentro de `back/`. Para produção, substitua a URL local por HTTPS no arquivo `network/ApiClient.kt` e compile novamente.

Documentação completa: [README raiz](../../README.md) e [validação Android](../../docs/VALIDACAO_ANDROID.md).
