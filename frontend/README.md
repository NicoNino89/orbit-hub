# Front-end — Orbit Hub Web

SPA em **React 19 + TypeScript**, com build pelo **Vite** e estilos com **Tailwind CSS 4**.

## Comandos

| Comando | O que faz |
|---|---|
| `npm run dev` | Servidor de desenvolvimento; `/api` é repassado para `http://localhost:8080` |
| `npm run lint` | Lint com oxlint |
| `npm run typecheck` | Verificação de tipos do TypeScript |
| `npm test` | Testes com Vitest + React Testing Library |
| `npm run coverage` | Testes com relatório de cobertura |
| `npm run build` | Build de produção em `dist/` |

## Tokens visuais

As cores, fontes e raios do kit de interface ficam em [`src/styles/tokens.css`](src/styles/tokens.css), nos temas claro e escuro. O Tailwind lê esses tokens, então use `bg-primary`, `text-muted`, `border-line`, `font-display` etc. Nunca escreva uma cor direto no componente.

## Publicação

A mesma imagem roda no Docker Compose e na Azure (Container Apps). O `nginx.conf.template` serve o build, faz o fallback da SPA, aplica os cabeçalhos de segurança e repassa `/api` para o endereço em `ORBIT_API_UPSTREAM`:

- **Docker Compose:** `http://api:8080` (o padrão da imagem).
- **Azure:** o endereço HTTPS da API do ambiente, definido pelo Terraform.

Assim o navegador fala com uma origem só e a imagem não precisa ser refeita para cada ambiente.

## Áreas previstas

| Área | Padrão de interface |
|---|---|
| Descobrir | Catálogo editorial com virada de páginas (PoC em KAN-58) |
| Conexões | Painel de interesses e Matches |
| Perfil | Cadastro, tese de investimento e dados da startup |

As áreas Comunidade, Serviços e Aprender entram nas fases seguintes.
