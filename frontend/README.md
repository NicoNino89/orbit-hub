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

- **Docker Compose:** `nginx.conf` serve o build e repassa `/api` para o container da API.
- **Azure Static Web Apps:** `public/staticwebapp.config.json` define o fallback da SPA e os cabeçalhos de segurança. O endereço da API entra no build pela variável `VITE_API_URL`.

## Áreas previstas

| Área | Padrão de interface |
|---|---|
| Descobrir | Catálogo editorial com virada de páginas (PoC em KAN-58) |
| Conexões | Painel de interesses e Matches |
| Perfil | Cadastro, tese de investimento e dados da startup |

As áreas Comunidade, Serviços e Aprender entram nas fases seguintes.
