# Conexão do APK e revisão do login

O APK local usa `http://localhost:3000/api/`. No celular, localhost representa o aparelho; esse endereço não permite acesso pela internet à API do computador. Ele exige servidor local ligado e `adb reverse tcp:3000 tcp:3000`.

Para usar pela internet, é necessário ter o serviço Render publicado e saudável, banco configurado e a URL HTTPS real. O Blueprint no Git não publica o serviço sozinho.

Na versão debug, o login oferece **Configurar conexão**. Informe a URL real da API na Render; o app normaliza o sufixo `/api/`, salva o endereço e descarta a sessão anterior se o servidor mudar. A troca de servidor fica disponível apenas em debug; a versão release usa o endereço fornecido no build.

Para gerar um APK com o endereço correto já incluído:

```bash
cd front/projeto_que_vou_apagar_dando_der
./gradlew assembleDebug -PskippecasApiUrl=https://SEU-SERVICO.onrender.com/api/
```

Não use o domínio de exemplo literalmente. Falta receber e verificar a URL real do serviço do usuário para gerar e validar a versão destinada à internet.

O novo login usa vermelho/branco, elemento de roda existente no projeto, cabeçalho de marca, campos com contorno, botão de entrada destacado e textos próprios para login/cadastro. Erros de conexão permanecem visíveis e distinguem configuração local de indisponibilidade remota. Não há login fictício ou passagem sem autenticação.

A compilação e os testes validam código/contratos, mas aparência e conexão no aparelho exigem verificação no dispositivo. Nenhum dispositivo apareceu no ADB nesta etapa.

Validação desta revisão: build e Android lint passaram (0 erros, 144 avisos). A suíte Android com backend real em memória passou com 27 testes, 0 falhas e 0 ignorados; inclui 3 testes novos de normalização e restrição de URLs. A URL pública da Render ainda não foi fornecida, portanto a conexão pela internet não foi validada.
