# Login Android → SkipPeças API

Abra esta pasta no Android Studio e sincronize o Gradle. O projeto usa AGP 9,
com suporte integrado a Kotlin; as telas Java existentes continuam funcionando.

## Arquivos da implementação

- `app/build.gradle.kts`: Retrofit, conversor Gson e MockWebServer para testes.
- `app/src/main/AndroidManifest.xml`: permissão INTERNET.
- `app/src/debug/`: liberação de HTTP apenas para 10.0.2.2 e localhost no debug.
- `network/ApiClient.kt`: URL base e timeouts, sem logs de senha ou token.
- `network/AuthApi.kt`: contrato JSON e POST relativo `auth/login`.
- `auth/TokenStore.kt`: JWT em SharedPreferences privadas (`auth_session`).
- `LoginFragment.kt` e `fragment_login.xml`: formulário, validação, progresso,
  chamada assíncrona, Snackbar e navegação após o sucesso.
- `nav_graph.xml`: login inicial; sucesso remove o login da pilha e abre Home.
- `MainActivity.java`: esconde a navegação inferior enquanto estiver no login.
- Regras de backup: excluem `auth_session.xml` de backup e transferência.

Os caminhos Kotlin acima ficam em `app/src/main/java/com/example/app/`.

## Executar

1. Inicie o MySQL e crie a estrutura com `back/src/config/database.sql`.
2. Na pasta `back/`, execute `npm install` e `npm start`. Configure o `.env`
   com as credenciais do MySQL e o `JWT_SECRET` já criado.
3. Use um usuário existente ou crie um pela API:

```bash
curl -X POST http://localhost:3000/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"name":"Ana","email":"ana@example.com","password":"senha-teste-123","role":"comprador"}'
```

4. Execute a variante `debug` no emulador e entre com o mesmo e-mail e senha.
   Durante a requisição, os campos e o botão ficam desabilitados. Após o sucesso,
   o JWT é salvo e a tela Home/Feed existente abre.

## Contrato

```http
POST http://10.0.2.2:3000/api/auth/login
Content-Type: application/json

{"email":"ana@example.com","password":"senha-teste-123"}
```

```json
{
  "token": "<JWT>",
  "user": { "id": 1, "name": "Ana", "email": "ana@example.com", "role": "comprador" }
}
```

Credenciais rejeitadas (401), dados inválidos (400), falha de rede e falha do
servidor têm mensagens amigáveis. Nenhuma senha é salva pelo aplicativo.
O aplicativo solicita login ao abrir; não presume validade de um token antigo.

## Endereços e ambiente

`10.0.2.2` é o endereço da máquina hospedeira visto pelo emulador Android.
Para aparelho conectado por USB, execute `adb reverse tcp:3000 tcp:3000` e
altere `BASE_URL` em `ApiClient.kt` para `http://localhost:3000/api/`.
Mantenha a barra final da URL. Para release, configure uma API HTTPS.

Não é necessário alterar o backend: `express.json()` já recebe este payload,
a resposta já contém `token` e `user`, e CORS já está configurado. Acesso por
Retrofit em Android nativo não depende de CORS do navegador.

## Verificação

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

`AuthApiTest.kt` usa um servidor HTTP simulado para verificar método, caminho,
JSON enviado, leitura do token e resposta 401. Para validar a interface, teste
no emulador: campos vazios, senha incorreta, servidor desligado e login válido;
confira Home/Feed após o sucesso e que Voltar não reabre o login.
