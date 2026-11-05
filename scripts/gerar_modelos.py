from pathlib import Path
import re,html,json,xml.etree.ElementTree as ET
D=Path('docs'); M=D/'modelos'; I=D/'imagens'
for p in [D,M,I,D/'evidencias']:p.mkdir(exist_ok=True,parents=True)
models={
'sitemap':('Sitemap Android','flowchart TD\n Login --> Home\n Home --> Detalhes\n Home --> Marketplace\n Home --> Vender\n Home --> Usuarios\n Home --> Perfil\n Home --> Configuracoes\n Marketplace --> Detalhes\n Vender --> NovoAnuncio\n Vender --> PerfilPublico\n Usuarios --> PerfilPublico\n Perfil --> EditarPerfil\n Perfil --> PublicarFoto\n Configuracoes --> Favoritos\n Configuracoes --> Bloqueados\n Favoritos --> Detalhes', ['Login / cadastro','Home / feed','Marketplace / detalhes','Vender / novo anúncio','Usuários / perfil público','Meu perfil / editar / foto','Configurações / favoritos / bloqueados']),
'funcional':('Arquitetura funcional','flowchart LR\n Auth[Autenticação] --> Sessao[Sessão JWT]\n Sessao --> Anuncios[Anúncios]\n Sessao --> Favoritos\n Sessao --> Perfis\n Sessao --> Bloqueios\n Anuncios --> Marketplace\n Anuncios --> Feed\n Perfis --> Publicacoes[Publicações]\n Publicacoes --> Feed',['Autenticação e sessão','Anúncios e marketplace','Feed e publicações','Favoritos','Usuários e perfis','Bloqueios e exclusão']),
'tecnica':('Arquitetura técnica','flowchart LR\n Android[Android / Retrofit] -->|HTTPS 443| Proxy[Proxy Render / TLS]\n Proxy -->|PORT 10000| API[Node 24 / Express]\n API -->|mysql2 TCP 3306| DB[(MySQL persistente)]\n API --> Uploads[(back/uploads)]\n API -. apenas demo .-> Mem[(SQLite em memória)]',['Android / Retrofit / JSON','HTTPS 443 / proxy Render','Express / PORT 10000','MySQL / TCP 3306','Uploads / disco necessário','SQLite em memória / demonstração']),
'mvc':('Distribuição MVC','flowchart LR\n View[View: Fragments e layouts Android] --> Rede[Retrofit]\n Rede --> Routes[Routes e middleware Express]\n Routes --> Controller[Controllers: validação e HTTP]\n Controller --> Model[Models: SQL e transações]\n Model --> DB[(MySQL / SQLite demo)]',['View / Android','Retrofit / HTTP JSON','Routes / middleware','Controller / validação','Model / persistência','Banco / MySQL']),
'macroprocesso':('Cadeia de valor','flowchart LR\n A[Registrar e autenticar] --> B[Publicar anúncio ou foto]\n B --> C[Descobrir e filtrar peças]\n C --> D[Consultar detalhes e perfis]\n D --> E[Salvar favoritos]\n E --> F[Gerir conta e preferências]',['Registrar e autenticar','Publicar anúncio ou foto','Descobrir e filtrar peças','Consultar detalhes / perfis','Salvar favoritos','Gerir conta / bloqueios']),
'fluxograma':('Fluxo de anúncio','flowchart TD\n Inicio((Início)) --> Login[Login]\n Login --> Token{Token válido?}\n Token -->|Não| Erro[Exibir erro e pedir login]\n Token -->|Sim| Role{Vendedor?}\n Role -->|Não| Negar[403 sem permissão]\n Role -->|Sim| Form[Preencher anúncio]\n Form --> Valido{Dados válidos?}\n Valido -->|Não| Corrigir[400 corrigir campos]\n Corrigir --> Form\n Valido -->|Sim| Salvar[Persistir anúncio]\n Salvar --> Fim((201 / atualizar lista))',['Login','Token válido? → não: 401','Vendedor? → não: 403','Preencher / validar → 400','Persistir com parâmetros','201 / atualizar lista']),
'contexto':('Contexto do sistema','flowchart LR\n Comprador -->|consulta e favoritos| Sistema[SkipPeças]\n Vendedor -->|anúncios e perfil| Sistema\n Sistema -->|dados relacionais| MySQL[(MySQL)]\n Sistema -->|fotos| Disco[(Uploads persistentes)]\n Operador -->|deploy e variáveis| Render[Render]\n Render --> Sistema',['Comprador','Vendedor','SkipPeças Android + API','MySQL externo','Disco de uploads','Operador / Render']),
'casos-uso':('Casos de uso','flowchart LR\n Visitante --> Consultar[Consultar feed e marketplace]\n Visitante --> Autenticar[Registrar / entrar]\n Comprador --> Consultar\n Comprador --> Favoritar\n Comprador --> Perfil[Gerir perfil e foto]\n Comprador --> Config[Gerir bloqueios / excluir conta]\n Vendedor --> Consultar\n Vendedor --> Anunciar[Publicar anúncio]\n Vendedor --> Perfil',['Visitante / consultar e registrar','Comprador / favoritos','Vendedor / publicar anúncio','Usuário / perfil e foto','Usuário / bloqueios','Usuário / excluir conta']),
'estados':('Estado de sessão','stateDiagram-v2\n [*] --> SemSessao\n SemSessao --> Autenticando: enviar credenciais\n Autenticando --> Autenticado: 200 e JWT\n Autenticando --> SemSessao: 400/401/500 ou rede\n Autenticado --> SemSessao: expiração / 401\n Autenticado --> ContaExcluida: DELETE confirmado 204\n ContaExcluida --> SemSessao: token rejeitado\n SemSessao --> [*]',['Sem sessão','Autenticando','200 / autenticado','Erro / sem sessão','Expirado / sem sessão','Conta excluída / JWT rejeitado']),
'operacao':('Operação de bloqueio','sequenceDiagram\n actor U as Usuário\n participant V as BloqueadosFragment\n participant A as SettingsApi\n participant C as settingsController\n participant M as settingsModel\n participant D as MySQL\n U->>V: confirmar bloqueio\n V->>A: block(token, id)\n A->>C: POST /api/settings/block\n C->>M: blockUser(userId, targetId)\n M->>D: BEGIN / SELECT FOR UPDATE\n M->>D: INSERT ON DUPLICATE / SELECT\n D-->>M: bloqueio\n M->>D: COMMIT\n M-->>C: publicBlock\n C-->>A: JSON\n A-->>V: atualizar lista',['Usuário / confirmar','Fragment / SettingsApi','Middleware / controller','blockUser / transação','MySQL / locks e INSERT','COMMIT / resposta / lista']),
'dfd-nivel0':('DFD nível 0','flowchart LR\n U[Usuário] -->|credenciais e ações| P((0. SkipPeças))\n P -->|listas / erros / sessão| U\n P -->|consultas e escritas| D[(Dados persistentes)]\n D -->|registros| P',['Usuário / entradas','0. SkipPeças / processamento','Dados / consultas e escritas','Usuário / saída JSON e interface']),
'dfd-nivel1':('DFD nível 1','flowchart LR\n U[Usuário] --> A((1. Autenticar))\n A <--> D1[(D1 users)]\n U --> B((2. Consultar e publicar))\n B <--> D2[(D2 products / posts)]\n U --> C((3. Gerir relações))\n C <--> D3[(D3 favorites / blocked_users / user_follows)]\n U --> E((4. Publicar mídia))\n E --> D4[(D4 uploads)]\n E --> D2',['1. Autenticar / users','2. Publicar e consultar / products posts','3. Relações / favorites bloqueios follows','4. Mídia / uploads e posts']),
'classes':('Classes e módulos do domínio','classDiagram\n class User {\n +int id\n +string name\n +string email\n +string role\n }\n class Product {\n +int id\n +decimal price\n +bool is_active\n }\n class Post {\n +int id\n +string description\n }\n class UserModel {\n +findByEmail(email)\n +createUser(data)\n }\n class SettingsModel {\n +blockUser(userId, targetId)\n +unblockUser(userId, blockId)\n +deleteAccount(userId)\n }\n class Favorite {\n +int id\n +int user_id\n +int product_id\n }\n User "1" --> "0..*" Product : anuncia\n User "1" --> "0..*" Post : publica\n Product "0..1" <-- "0..*" Post : referencia\n User "1" --> "0..*" Favorite\n Product "1" --> "0..*" Favorite\n UserModel ..> User\n SettingsModel ..> User',['User / id name email role','Product / id price is_active','Post / id description','Favorite / user_id product_id','UserModel / findByEmail createUser','SettingsModel / block unblock delete'])}

def svg(name,title,labels):
 w=850;h=85+len(labels)*85
 out=[f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}" role="img" aria-label="{html.escape(title)}">', '<rect width="100%" height="100%" fill="#f5f7fa"/>',f'<text x="30" y="38" font-family="sans-serif" font-size="24" fill="#12263a">{html.escape(title)}</text>']
 for i,label in enumerate(labels):
  y=65+i*85
  if i:out.append(f'<path d="M425 {y-25} V{y}" stroke="#506579" stroke-width="2"/><path d="M420 {y-8} L425 {y} L430 {y-8}" fill="none" stroke="#506579"/>')
  out.extend([f'<rect x="45" y="{y}" width="760" height="60" rx="8" fill="white" stroke="#a22b35"/>',f'<text x="65" y="{y+36}" font-family="sans-serif" font-size="18">{html.escape(label)}</text>'])
 out.append('</svg>'); (I/f'{name}.svg').write_text('\n'.join(out))
for name,(title,src,labels) in models.items():
 (M/f'{name}.mmd').write_text(src+'\n');svg(name,title,labels)
# DER e dicionário derivados diretamente do SQL versionado.
sql=Path('back/src/config/migrations/000_full_schema.sql').read_text()
tables=re.findall(r'CREATE TABLE IF NOT EXISTS (\w+) \((.*?)\n\);',sql,re.S)
der=['erDiagram'];dictionary=['# Dicionário de dados derivado do schema\n','Fonte: `back/src/config/migrations/000_full_schema.sql`. Regenerável com `python3 scripts/gerar_modelos.py`. Os trechos SQL preservam tipos, índices, restrições e ações referenciais exatamente como definidos.\n']
for table,body in tables:
 der.append(f'  {table} {{')
 for line in body.splitlines():
  match=re.match(r'\s*(\w+)\s+(INT|VARCHAR\(\d+\)|TEXT|TIMESTAMP|DECIMAL\(\d+,\s*\d+\)|BOOLEAN|ENUM\([^)]*\))',line,re.I)
  if match:
   col,typ=match.groups(); typ=typ.replace(' ','').replace(',','_').replace("'",'')
   der.append(f'    {typ} {col}')
 der.append('  }')
 dictionary += [f'## {table}\n', '```sql\nCREATE TABLE '+table+' ('+body+'\n);\n```\n']
 for col,target in re.findall(r'FOREIGN KEY \((\w+)\) REFERENCES (\w+)\(id\)',body):
  optional=bool(re.search(rf'^\s*{col} .*?\bNULL\b',body,re.M)) and not bool(re.search(rf'^\s*{col} .*?NOT NULL',body,re.M))
  der.append(f'  {target} '+('|o' if optional else '||')+f'--o{{ {table} : {col}')
(M/'der.mmd').write_text('\n'.join(der)+'\n');(D/'DICIONARIO_DADOS.md').write_text('\n'.join(dictionary));svg('der','Entidades e relações — visão resumida',[t for t,b in tables])
# Wireframes de engenharia reversa, um por layout de tela/dialog.
base=Path('front/projeto_que_vou_apagar_dando_der/app/src/main/res/layout')
wire=['# Interface, sitemap e wireframes\n','Estes wireframes são reconstruções esquemáticas da implementação atual, criadas em 04/11/2026. Não são screenshots, aprovação prévia de design ou arquivos Figma. O produto é Android nativo. A validação com o usuário e a comparação visual em dispositivo permanecem pendentes.\n','## Sitemap completo\n','[Fonte editável](modelos/sitemap.mmd) — [imagem resumida](imagens/sitemap.svg) — [grafo implementado](../front/projeto_que_vou_apagar_dando_der/app/src/main/res/navigation/nav_graph.xml).\n','```mermaid\n'+models['sitemap'][1]+'\n```\n','## Inventário de telas e componentes\n','| Tela / diálogo | Estrutura esquemática | Componentes essenciais |\n| --- | --- | --- |']
ns='{http://schemas.android.com/apk/res/android}'
for layout in sorted(base.glob('*.xml')):
 if not layout.stem.startswith(('fragment_','dialog_')):continue
 root=ET.parse(layout).getroot(); labels=[]
 for el in root.iter():
  id=el.get(ns+'id','').split('/')[-1];tag=el.tag.split('.')[-1]
  if id and tag not in ['ConstraintLayout','LinearLayout','FrameLayout','NestedScrollView']:
   labels.append(f'{tag}: {id}')
 if not labels:labels=['Blocos de interface / consultar XML']
 svg('wire-'+layout.stem,layout.stem,labels)
 wire.append(f'| [{layout.stem}](../{layout.as_posix()}) | [Imagem](imagens/wire-{layout.stem}.svg) | '+', '.join(labels[:6])+' |')
 wire.extend([])
wire+=['\n## Estados visuais e adaptação\n','Listas devem distinguir carregamento, sucesso, erro e vazio; ações mutantes devem desabilitar envio duplicado e exibir resultado. LoginFragment e ViewModels implementam estados assíncronos; revisar todas as telas durante o roteiro. Recursos values-w600dp, values-w1240dp e values-land adaptam dimensões, mas não comprovam testes de tablet ou desktop. Máscaras devem ser aplicadas apenas a campos que as exigem, sem inventar CPF, telefone ou checkout inexistentes no escopo.\n','## Prototipação navegável\n','A referência navegável disponível é o APK gerado a partir do código. Seguir Login → Home → Marketplace → Detalhes → Favoritos e Usuários → Perfil. Os SVGs acima descrevem blocos e não reproduzem fidelidade visual. **Pendente:** projeto Figma publicado, link compartilhável, prints reais e validação de fidelidade. Não há protótipo Figma criado nesta entrega.']
(D/'INTERFACE.md').write_text('\n'.join(wire))
modeldoc=['# Modelagem de processos e software\n','Os modelos abaixo documentam o MVP existente. Relações representadas não adicionam checkout, pagamento, chat, notificações ou operações de aceitar/recusar solicitações que não tenham endpoint implementado. As imagens SVG são resumos de leitura; as fontes Mermaid abaixo são os diagramas completos.\n']
explain={
'sitemap':'O ponto inicial é Login. A barra inferior organiza os módulos autenticados; detalhes recebem product_id e perfil público recebe userId. O diagrama resume também ações disparadas por botões, não apenas actions XML.',
'funcional':'Autenticação estabelece sessão; vendedor cria anúncios consultáveis no marketplace; publicações compõem o feed. Favoritos, perfis e bloqueios dependem da identidade autenticada.',
'tecnica':'Android troca JSON com Express via proxy HTTPS da Render. O banco é MySQL externo em produção; SQLite volátil serve apenas à demonstração. Fotos exigem armazenamento persistente. TLS do banco depende do provedor.',
'mvc':'MVC é distribuído: View Android, controllers HTTP e models SQL no backend. ViewModels Android gerem estado de apresentação; não são os models de persistência Node.',
'macroprocesso':'A cadeia de valor aproxima compradores e vendedores pela descoberta de peças e gestão de anúncios. Não há transação financeira implementada.',
'fluxograma':'Anunciar exige sessão e papel vendedor. Entradas inválidas retornam 400; ausência de sessão 401; papel insuficiente 403. Somente após persistência a API retorna 201.',
'contexto':'Comprador e vendedor são atores; Render e operador sustentam execução. MySQL e uploads são recursos externos ao aplicativo móvel. Serviços de pagamento não integram o escopo.',
'casos-uso':'Atores podem compartilhar comportamentos sem herança de classes implementada. O catálogo de especificações por tela está em PRODUCAO_TECNICA.md.',
'estados':'A máquina representa a sessão. Exclusão e expiração invalidam o acesso. O campo product_requests.status admite pendente/aceita/recusada no schema, mas não foi modelado como fluxo operacional entregue por ausência de API de decisão.',
'operacao':'Bloqueio valida a identidade, bloqueia linhas em ordem consistente e realiza INSERT idempotente dentro de transação. Em falha há rollback e release; nenhuma consulta recebe IDs concatenados.',
'dfd-nivel0':'O processo global recebe credenciais, filtros e alterações, e devolve conteúdo, sessão e feedback. O depósito agrega as persistências detalhadas no nível 1.',
'dfd-nivel1':'A decomposição separa identidade, conteúdo, relações e arquivos. user_follows e product_requests existem no banco; isso não implica interface completa de gestão dessas relações.',
'classes':'User, Product, Post e Favorite são conceitos e DTOs; a API usa módulos funcionais, não classes JavaScript com herança artificial. UserModel e SettingsModel representam exports reais. O dicionário de dados contém todos os atributos e tipos SQL.',
'der':'O DER foi derivado do schema consolidado; FK product_id de posts é opcional, demais FKs são obrigatórias. user_follows possui PK composta. Consultar o dicionário para nulabilidade, CHECKs, índices e cascatas exatos.'}
for name in list(models)+['der']:
 src=(M/f'{name}.mmd').read_text(); title=models[name][0] if name in models else 'Entidade relacionamento'
 modeldoc += [f'## {title}\n',explain[name]+'\n',f'[Fonte CASE editável](modelos/{name}.mmd) · [Visão resumida SVG](imagens/{name}.svg)\n','```mermaid\n'+src+'```\n']
modeldoc+=['## BPMN: publicação de anúncio\n','[Modelo BPMN 2.0 importável](modelos/publicar-anuncio.bpmn). As raias distinguem Usuário e API. O evento inicial leva ao preenchimento, a API valida sessão/papel/dados, o gateway exclusivo decide validade e segue para persistir/sucesso ou rejeitar/fim de erro. O fluxo de erro exige nova tentativa do usuário. As conexões dentro do mesmo pool são fluxos de sequência; não são mensagens entre participantes.\n','## Ferramenta CASE e reprodução\n','Fontes `.mmd` são modelos textuais Mermaid versionados, editáveis em ferramentas compatíveis e renderizados nas páginas Markdown que suportam Mermaid. O `.bpmn` é XML BPMN 2.0 com DI para importação em modelador compatível. `python3 scripts/gerar_modelos.py` regenera resumos SVG, inventário de wireframes e dicionário diretamente das fontes locais, sem serviços externos. Os resumos SVG não substituem o conteúdo semântico dos modelos completos. Não foi alegado uso de Figma ou de licença de ferramenta comercial.']
(D/'MODELOS.md').write_text('\n'.join(modeldoc))
