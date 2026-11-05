# Conexão automática com a Render

O APK versão 1.1 já usa `https://skippecas-api.onrender.com/api/`. Não exige configurar endereço, cabo USB ou servidor no computador. O botão de configuração foi removido e endereços salvos nas versões anteriores não são reaproveitados.

O serviço foi consultado por HTTPS: health 200, login de comprador/vendedor de demonstração e rotas críticas serão registrados no resultado desta execução. O banco ativo é SQLite em memória; dados são apagados ao reiniciar. Configuração persistente e passos no painel estão no [relatório Render](../RELATORIO_DEPLOY_RENDER.md).

Instale `app/build/outputs/apk/debug/app-debug.apk` do projeto Android sobre a versão antiga. Este é um APK debug para validação; distribuição em loja exige assinatura release. Para testes locais de desenvolvimento, é possível compilar explicitamente com `-PskippecasApiUrl=http://localhost:3000/api/`; isso não é necessário para usar o APK fornecido.
