# Foto de perfil — diagnóstico e correção

## Causa confirmada no serviço público

Foi feito login de demonstração na API e enviado um corpo de imagem deliberadamente inválido para `POST https://skippecas-api.onrender.com/api/users/avatar`, sem substituir a foto de ninguém. O serviço retornou **404**, com `Rota não encontrada.`. Na versão local atual, a mesma entrada deve retornar 400 por imagem inválida: a rota está implementada, mas a versão pública observada ainda não a contém.

A consulta direta ao GitHub (`git ls-remote origin refs/heads/main`) confirmou `378b55f450d54d9f81ed757955c8292a2796f6f3`. Esse commit já contém `router.post('/avatar', ...)` em `back/src/routes/userRoutes.js`. Portanto, não basta instalar outro APK: é necessário publicar o backend que já está no GitHub.

## O que fazer agora na Render

1. Abra o serviço **skippecas-api**.
2. Em **Settings → Build**, confira o repositório do SkipPeças, **Branch: main**, **Root Directory: back**, **Build Command: npm install**.
3. Em **Settings → Deploy**, confira **Start Command: npm start**.
4. No topo direito, escolha **Manual Deploy → Deploy latest commit**. Se selecionar commit específico, escolha `378b55f` ou um posterior que contenha a rota de avatar.
5. Acompanhe **Deploys** e **Logs**. Aguarde o deploy NOVO terminar como **Live**; o selo Live de um deploy antigo não confirma esta atualização. Se falhar, a Render pode continuar atendendo pela versão antiga.
6. Não há nova variável obrigatória para o envio de avatar. Não troque DB_MODE para mysql sem preparar o banco.
7. Instale o APK 1.3 e entre novamente. Em **Perfil**, toque na própria foto ou em **Alterar foto da galeria**, selecione a imagem, confira a prévia e toque em **Salvar foto**.

O serviço estava em memória: reiniciar pode apagar cadastros e invalidar sessões. Arquivos enviados também precisam de disco persistente/armazenamento de objetos para sobreviver a reinícios; veja RELATORIO_DEPLOY_RENDER.md.

## Melhorias no aplicativo

- Acesso direto tocando no avatar e botão próprio no perfil do titular.
- Diálogo dedicado de foto: não exige preencher nome, e-mail ou bio.
- Prévia circular, seleção persistida na rotação e confirmação antes do upload.
- Botão Salvar foto só habilitado após selecionar imagem; bloqueio de duplicação durante envio.
- Atualização do perfil ao receber a resposta, sem segunda requisição para editar outros campos.
- Mensagens distintas para imagem inválida, arquivo grande, falha de rede, sessão expirada e rota ainda não publicada.
- O editor completo continua disponível para nome/e-mail/bio e seleção de imagem.

## Verificações

Build do APK 1.3 e lint executados. Teste de integração Retrofit foi ampliado para enviar uma imagem real, confirmar que o avatar aparece no GET do próprio perfil e que a outra conta não é alterada. A confirmação da implantação pública continua pendente até o redeploy. Não houve criação de conta ou upload válido no serviço público durante o diagnóstico.

Não foi feito deploy pela conta Render nem houve instalação/seleção de imagem em aparelho nesta revisão. Testes JVM e API local não substituem a verificação visual no celular.

Resultado final desta revisão: 33 testes Android aprovados, zero falhas e zero ignorados, incluindo upload de avatar contra API local real. Build do APK 1.3 aprovado; lint com 0 erros e 140 avisos.
