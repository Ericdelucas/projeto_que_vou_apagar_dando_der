# Conferência dos 50 requisitos — SkipPeças

Data: 04/11/2026.

## Conclusão

**Não é possível considerar todos os requisitos preenchidos.** A implementação da API tem evidências relevantes; a documentação acadêmica de modelagem, publicação real e ensaio final permanecem incompletos ou sem comprovação.

Esta conferência compara o checklist anexado com os arquivos locais, o histórico Git disponível e os testes executados na etapa anterior. Não foram alterados código, configuração, commits ou requisitos nesta conferência. Não foram acessados documentos externos não fornecidos nem reexecutadas as suítes. “Não comprovado” significa ausência de evidência no material disponível, não afirma que o artefato inexiste fora do repositório. “Atendido” considera o escopo observado, não representa certificação de segurança ou garantia total.

## Matriz inicial (antes da complementação)

| Item | Situação | Evidência ou pendência |
| --- | --- | --- |
| #1 | Parcial | Há apresentação do MVP em README_FINAL_SKIPPECAS.md; falta capítulo formal de Produção Técnica com metodologia e artefatos. |
| #2 | Não comprovado | Não localizado texto introdutório específico de Modelagem do Produto. |
| #3 | Não comprovado | Não localizados sitemap publicado, link completo, prints e explicações por parte. |
| #4 | Não comprovado | Não localizados wireframes tela a tela, descrições e tabela de componentes. |
| #5 | Não comprovado | Não localizado link Figma, protótipo navegável nem prints dos fluxos. |
| #6 | Parcial | README_FINAL_SKIPPECAS.md descreve arquitetura e responsabilidades; falta apresentação formal da seção solicitada. |
| #7 | Parcial | Módulos funcionais descritos no relatório final; falta desenho esquemático. |
| #8 | Parcial | Há tabela de tecnologias e relatório de deploy; falta desenho técnico consolidado com protocolos, portas e infraestrutura. |
| #9 | Parcial | Camadas implementadas e hospedagem planejada; falta desenho MVC e publicação efetiva comprovada. |
| #10 | Não comprovado | Não localizada introdução formal de Engenharia de Requisitos e critérios contratuais de aceitação. |
| #11 | Parcial | Documentos por módulo descrevem comportamentos; falta tabela formal de RF com identificadores e regras. |
| #12 | Não comprovado | Não localizada tabela de RNF com métricas mensuráveis de desempenho, disponibilidade e interface. |
| #13 | Não comprovado | Não localizada matriz de rastreabilidade entre RF e RNF. |
| #14 | Não comprovado | Não localizada tabela de projeto linear com cronologia, marcos e entregáveis. |
| #15 | Não comprovado | Não localizada introdução de Modelagem de Processos e Fluxos. |
| #16 | Não comprovado | Não localizado mapa de macroprocesso e cadeia de valor com explicação. |
| #17 | Não comprovado | Não localizado fluxograma formal de decisões e término de fluxo. |
| #18 | Não comprovado | Não localizado BPMN com raias, eventos, gateways e tarefas. |
| #19 | Não comprovado | Não localizada introdução específica de Diagramas e Modelagem de Software. |
| #20 | Não comprovado | Não localizados desenho de casos de uso e especificações por tela, com alternativas e exceções. |
| #21 | Não comprovado | Não localizado diagrama de classes documentado com atributos, métodos e relações. |
| #22 | Não comprovado | Há schema SQL, mas não foi localizado DER conceitual/lógico com desenho e explicação. |
| #23 | Não comprovado | Não localizado diagrama de estados com transições e guardas. |
| #24 | Não comprovado | Não localizado diagrama de operação/chamadas de métodos de rotinas críticas. |
| #25 | Não comprovado | Não localizado DFD em níveis com entidades externas e depósitos. |
| #26 | Parcial | nav_graph.xml e Fragments definem navegação Android; falta diagrama explicado das transições por eventos. |
| #27 | Não comprovado | Não localizado projeto de ferramenta CASE e documentação associada. |
| #28 | Não comprovado | Não localizado diagrama de contexto de integrações e fronteiras. |
| #29 | Parcial / adequar | Há Navigation Component e deep link Android; não há sitemap para comparar. URLs web não são o mecanismo principal deste frontend. |
| #30 | Não comprovado | Há layouts Android, mas não há wireframes/Figma disponíveis para verificar fidelidade. |
| #31 | Parcial / adequar | Há recursos dimens.xml para larguras e orientação; faltam evidências de testes visuais. Critério desktop/web precisa ser alinhado ao escopo Android. |
| #32 | Parcial | Loading e erros aparecem no login e há estados nos módulos; não houve auditoria visual de todas as interações e telas vazias. |
| #33 | Parcial | Login valida e-mail/senha e há validações por formulário; máscaras e validação instantânea em todos os campos não foram comprovadas. |
| #34 | Atendido | API separada em routes, controllers, models e middleware; interface Android em projeto próprio. |
| #35 | Atendido | Suíte principal e smoke anterior validaram códigos 200/201/204/400/401/403/404/409/500/503 conforme cenários dos testes. |
| #36 | Parcial | Controllers usam mensagens genéricas, mas app.js só trata entity.too.large e repassa outros erros; falta padronizar erros globais, inclusive JSON inválido e 404, e testar ausência de detalhes internos. |
| #37 | Atendido | dotenv e process.env usados; .env ignorado e não rastreado; .env.example sem senha real; Blueprint solicita segredos fora do Git. |
| #38 | Não comprovado | Schema existe, porém sem DER formal disponível não é possível conferir igualdade de campos e restrições. |
| #39 | Atendido | Schema consolidado possui PKs, FKs, unicidade e cascatas; integridade também é exercitada no fallback. E2E MySQL atual permanece pendente. |
| #40 | Atendido | Models usam placeholders e parâmetros; SQL dinâmico de filtros usa nomes de colunas controlados pelo servidor. Não foi identificada interpolação direta de valores do usuário nos trechos examinados. |
| #41 | Atendido | Há schema consolidado, migrações e carga de demonstração em memoryDatabase.js/memory_schema.sql; teste de fallback executado. |
| #42 | Atendido | Git na raiz, sem .git internas; check-ignore confirmou dependências, .env e artefatos Android ignorados. |
| #43 | Parcial | Histórico tem mensagens identificáveis; só main e origin/main disponíveis. Não há evidência de branches por funcionalidade no histórico consultado. |
| #44 | Parcial | README principal tem caminho absoluto de uma máquina e instruções locais; README Android contém apenas título. Falta guia portátil de clone/setup e há conflito de portas 3000/10000. |
| #45 | Pendente | Blueprint preparado, mas não há URL pública de serviço implantado e validado. |
| #46 | Pendente | Sem serviço público validado não há evidência de SSL ativo nem redirecionamento obrigatório para HTTPS. |
| #47 | Não aplicável ao frontend atual | Frontend Android nativo, não SPA. Registrar justificativa e validar deep links; se a avaliação exigir SPA, é necessário alinhar o escopo com o responsável. |
| #48 | Parcial | 18 testes backend aprovados na execução anterior, com fluxo principal e exceções; E2E MySQL recusou conexão. Sem matriz formal não se comprova cobertura de todos os cenários; Android não foi reexecutado nesta auditoria. |
| #49 | Não comprovado | Há testes e esta conferência documental/técnica, mas isso não prova domínio de cada instrução pela equipe. Falta revisão documentada e validação pelos integrantes. |
| #50 | Pendente | Não localizado roteiro de ensaio executado em produção, gravação, ata ou checklist de demonstração. |

## Bloqueios práticos identificados

1. **Integração Android e porta local:** `ApiClient.kt` fixa `http://localhost:3000/api/`; README e ADB reverse usam 3000. `server.js` agora tem fallback 10000 e `.env.example` indica 10000. Um `.env` com PORT=3000 pode mascarar a divergência; sem ele, o roteiro local não conecta. Definir uma porta coerente no roteiro e configurar URL HTTPS de produção no build Android.
2. **Tratamento global de erros:** as respostas esperadas dos controllers são protegidas, mas o middleware final ainda repassa erros não tratados. Falta comprovar formato amigável e ausência de stack/detalhes em todas as entradas inválidas.
3. **Persistência de produção:** validar MySQL real e schema, atender TLS se exigido pelo provedor e configurar persistência de uploads antes de receber dados reais. O Blueprint não cria banco nem disco.
4. **Documentos e evidências:** entregar sitemap, wireframes, Figma, RF/RNF/rastreabilidade, processos e diagramas com imagens e textos exigidos. Código e schema não substituem esses artefatos formais.
5. **Adequação da rubrica:** URLs amigáveis, desktop e fallback SPA pressupõem web; o projeto atual é Android nativo. A justificativa de não aplicabilidade precisa ser aceita pelo avaliador, não presumida pela equipe.
6. **Validação de entrega:** completar E2E MySQL, testes visuais Android nos dispositivos previstos, URL pública, HTTPS e ensaio documentado.

## Documentos consultados

- `README.md`, `README_FINAL_SKIPPECAS.md` e documentação Markdown dos módulos.
- `RELATORIO_DEPLOY_RENDER.md`, `render.yaml`, `back/package.json`, `.gitignore` e histórico Git.
- API em `back/src/`, schema/migrações e testes em `back/tests/`.
- Projeto Android, `nav_graph.xml`, `ApiClient.kt`, layouts, recursos por largura, manifestos e testes existentes.

O relatório de deploy registra 18 testes aprovados e a falha de conexão do E2E MySQL. O relatório antigo de MVP menciona testes Android/MySQL de outra data; essa menção é evidência histórica, não execução atual nem validação do serviço em produção.

## Atualização após a complementação solicitada

A matriz inicial acima é o diagnóstico histórico; a tabela abaixo é a situação após a implementação desta etapa. Documentação produzida por engenharia reversa não equivale a aprovação anterior de design, teste visual ou homologação da equipe.

| Itens | Evolução e evidência atual | Limitação restante |
| --- | --- | --- |
| #1, #2, #6, #10, #15, #19 | Textos introdutórios e contexto em docs/PRODUCAO_TECNICA.md | Revisão editorial/aceitação acadêmica pela equipe |
| #3 | Sitemap completo Mermaid, SVG resumido e referência ao grafo Android | Prints reais por tela/trecho e eventual link publicado ainda pendentes |
| #4 | Wireframes SVG por tela/diálogo, links aos XMLs e tabela de componentes em docs/INTERFACE.md | São reconstruções esquemáticas posteriores ao código |
| #5 | APK navegável e roteiro dos fluxos | Figma, link compartilhável, alta fidelidade e prints continuam pendentes |
| #7, #8, #9 | Arquiteturas funcional/técnica/MVC e tabela de tecnologias | Hospedagem especificada, mas ainda não validada publicamente |
| #11, #12, #13, #14 | RF/RNF, critérios, rastreabilidade e sequência de marcos em docs/PRODUCAO_TECNICA.md | Metas de carga/disponibilidade declaradas como propostas; não medidas |
| #16, #17 | Macroprocesso e fluxograma completos em Mermaid, explicados | Aceitação dos modelos pela equipe |
| #18 | Modelo BPMN 2.0 com raias, gateway, eventos, DI e explicação | Importação/validação visual em modelador BPMN pelo avaliador pendente |
| #20 | Modelo de atores e tabela por tela com fluxos, alternativas e exceções | Homologação de todos os cenários manualmente |
| #21 | Modelo de entidades e módulos reais com atributos/métodos/associações | Sem herança artificial; atributos SQL completos no dicionário |
| #22, #38 | DER e dicionário gerados diretamente do schema consolidado | Banco MySQL implantado ainda deve ser comparado com esse schema |
| #23, #24, #25 | Estados de sessão, operação de bloqueio e DFD níveis 0/1 documentados | Não representam APIs futuras ausentes do código |
| #26 | Sitemap/fluxos, grafo real e casos por tela vinculam eventos a navegação | Prints e ensaio visual ainda necessários |
| #27 | Fontes Mermaid e BPMN versionadas, gerador Python de artefatos | Não equivale a projeto Figma; validar importação no modelador escolhido |
| #28 | Diagrama de contexto e fronteiras documentado | Integrações externas só confirmadas após deploy |
| #29 | Sitemap agora permite comparação com navegação Android | A rubrica de URLs web exige adaptação aprovada |
| #30, #31 | Inventário dos layouts e recursos por largura mantidos | Fidelidade Figma e testes visuais por dispositivo não comprovados |
| #32, #33 | Estados e validações existentes preservados e roteiro ampliado | Auditoria visual completa e máscaras aplicáveis ainda pendentes |
| #34, #35, #37, #39, #40, #41, #42 | Evidências técnicas anteriores preservadas | MySQL real e auditoria de segurança completa não substituídos por mocks |
| #36 | Middleware global JSON para 400/404/413/500 e teste de não exposição para JSON inválido, rota ausente e excesso | Revisão de todos os erros inesperados continua recomendada |
| #43 | Histórico preservado; nenhuma branch retroativa fictícia criada | Processo de branches por funcionalidade não pode ser comprovado retroativamente |
| #44 | README portátil com clone, setup, testes e porta USB explícita; README Android completo | Requer SDK/Java e dependências disponíveis na máquina de destino |
| #45, #46 | Blueprint e instruções mantidos; URL de API configurável no build Android | Serviço público, MySQL, fotos persistentes e HTTPS ainda pendentes |
| #47 | Não aplicável a Android nativo; launcher/deep link documentados | Exige aceitação da adequação de escopo pelo avaliador |
| #48 | Backend e build/testes Android reexecutados; integração Retrofit disponível | Ver resultado exato em docs/evidencias/EXECUCAO.md; MySQL real e testes visuais pendentes |
| #49 | Revisão desta etapa e checklist de domínio em docs/PRODUCAO_TECNICA.md | Cada integrante precisa executar e registrar sua própria revisão |
| #50 | Roteiro completo de ensaio e coleta de evidências em docs/VALIDACAO_ANDROID.md | Ensaio no dispositivo/produção não executado |

### Launcher e APK

O manifesto e o APK gerado confirmam MAIN/LAUNCHER e MainActivity. O nome foi corrigido para SkipPeças. O ADB não encontrou dispositivo conectado; instalação, desinstalação e botão Abrir não foram simulados. A atualização recomendada usa `adb install -r` para preservar dados; desinstalar só se necessário, após backup.
