# SkipPeças — Android e API Node.js

**APK atual (1.4): conexão automática com `https://skippecas-api.onrender.com/api/`, sem configuração no aplicativo.** Para desenvolvimento local por USB, compile explicitamente com `-PskippecasApiUrl=http://localhost:3000/api/`. O roteiro USB abaixo só se aplica a esse build local. Veja [configuração Render e situação do banco](RELATORIO_DEPLOY_RENDER.md).

O ícone do aplicativo agora usa o volante do SkipPeças. O modo memory traz fotos automotivas reais no feed e marketplace; a API publicada precisa receber o backend atualizado para servir esse catálogo. Veja [imagens e créditos](docs/IMAGENS_DEMO.md) e [validação do APK 1.4](docs/evidencias/APK_IMAGENS_2026-11-11.md).

Aplicativo Android nativo de descoberta de peças, anúncios, favoritos e perfis. Não é uma SPA web.

## Instalação do zero

Requisitos: Git, Node.js 24, npm, Android Studio com SDK 37, JDK compatível com o Gradle do projeto e platform-tools (adb) no PATH. Use o Gradle Wrapper incluído.

```bash
git clone https://github.com/Ericdelucas/projeto_que_vou_apagar_dando_der.git Engenharia
cd Engenharia
cd back
npm ci
cp .env.example .env
```

O exemplo usa porta 10000. Para o Android por USB, o roteiro abaixo define explicitamente PORT=3000, que corresponde à URL local do aplicativo. Em produção use a URL HTTPS do serviço, não localhost.

```bash
cd back
npm test
npm run test:smoke
```

Se já estiver em `back/`, não repita `cd back`. Na raiz, abra `front/projeto_que_vou_apagar_dando_der/` no Android Studio ou execute:

```bash
cd front/projeto_que_vou_apagar_dando_der
./gradlew clean assembleDebug testDebugUnitTest lintDebug
```

- [Produção técnica e especificação](docs/PRODUCAO_TECNICA.md)
- [Modelos e diagramas](docs/MODELOS.md)
- [Modelagem visual](docs/INTERFACE.md)
- [Roteiro de instalação e demonstração](docs/VALIDACAO_ANDROID.md)
- [Deploy Render](RELATORIO_DEPLOY_RENDER.md)

## Ligar o backend por USB

Com o celular conectado por USB e a depuração USB autorizada, **copie somente o bloco abaixo e cole no terminal**. As dependências já devem estar instaladas e o Node.js deve ser versão 24 ou superior.

```bash
cd back &&
adb reverse tcp:3000 tcp:3000 &&
PORT=3000 DB_MODE=memory npm start
```

Esse bloco conecta o celular ao computador pela porta 3000 e inicia o backend sem precisar de MySQL. Use novamente quando precisar ligar o projeto. Se desconectar o celular, pare o backend com Ctrl+C, reconecte o USB e execute o bloco novamente.

**Deixe o terminal aberto e ocupado pelo servidor enquanto usa o aplicativo.** Para desligar, pressione Ctrl+C.

Quando aparecer “Banco ativo: sqlite-memory.” e “SkipPeças API rodando na porta 3000”, o servidor iniciou. **Essas frases são mensagens do programa: não copie nem digite no terminal.**

Para conferir a API, abra [a página de saúde](http://localhost:3000/health) **no navegador do computador**. Não digite esse endereço como comando no terminal. A resposta deve conter `"status":"ok"`.

Entre no aplicativo com **ericdelucass@gmail.com**, senha **123**. Também é possível usar **Cadastrar-se** para criar uma conta com senha de pelo menos oito caracteres.

Os arquivos de fotos enviados ficam em `back/uploads/`. O banco deste modo fica em memória: novos cadastros e alterações são apagados ao encerrar o backend. A conta local acima é recriada ao iniciar.

Se aparecer “unauthorized”, desbloqueie o celular e aceite a autorização de depuração USB. Se aparecer “EADDRINUSE”, a porta 3000 já está ocupada; confira a página de saúde antes de abrir outro servidor.

Configuração de MySQL e documentação técnica: [relatório do projeto](README_FINAL_SKIPPECAS.md).


## Contas adicionais para testar a lista e os perfis

Todas usam a senha **SkipPecas123!** e são criadas no modo `memory`, com senhas protegidas por bcrypt. O histórico inicial é simulado com publicações reais de teste distribuídas nos últimos 14 dias. Cada conta começa com uma frequência e um total diferentes.

| Nome | Categoria | E-mail | Publicações em 14 dias |
|---|---|---|---|
| Caio Motor | Vendedor | vendedor1@skippecas.local | 15 |
| Davi Rodas | Vendedor | vendedor2@skippecas.local | 16 |
| Eva Peças | Vendedor | vendedor3@skippecas.local | 17 |
| Iara Auto | Vendedor | vendedor4@skippecas.local | 18 |
| Léo Garage | Vendedor | vendedor5@skippecas.local | 19 |
| Bia Lima | Comprador | comprador1@skippecas.local | 20 |
| Cris Melo | Comprador | comprador2@skippecas.local | 21 |
| Duda Reis | Comprador | comprador3@skippecas.local | 22 |
| Gui Alves | Comprador | comprador4@skippecas.local | 23 |
| Nina Luz | Comprador | comprador5@skippecas.local | 24 |

A lista mostra nome, categoria e gráfico vermelho da frequência diária de publicações. O número abaixo da linha é o total dos últimos 14 dias, calculado pela API; publicar aumenta a atividade do dia.

No próprio perfil, toque no botão **+** no canto inferior direito. Toque em **Escolher foto da galeria**, selecione uma imagem, confira a prévia e escreva uma legenda opcional. Toque em **Publicar**: a foto é enviada ao backend e aparece no perfil e na Home. A imagem é redimensionada no celular para até 1600 pixels; o upload aceita até 8 MB. Os posts e contas criados durante o uso são perdidos ao reiniciar o modo em memória.

Bancos MySQL existentes precisam receber a migração `back/src/config/migrations/006_posts_media_activity.sql` uma vez antes de usar esta versão. Bancos novos já incluem os campos no schema consolidado. As dez contas acima são exclusivas da demonstração em memória.

Na aba **Vender** da barra inferior, a lista de vendedores aparece acima dos seus anúncios. Toque em um nome para abrir o perfil ou em **Ver todos os vendedores** para pesquisar a lista completa.
