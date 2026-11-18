# ADR 0002 — oxlint como linter do front-end

- **Data:** 18/11/2026
- **Situação:** aceita
- **Histórias:** KAN-51, KAN-52

## Contexto

O KAN-51 cita o ESLint. O projeto do front-end foi gerado com o modelo atual do Vite, que já usa **TypeScript 7** (o compilador nativo) e o **oxlint**. O `typescript-eslint`, necessário para o ESLint entender TypeScript, ainda só aceita TypeScript abaixo da versão 6.1.

## Decisão

Usar o **oxlint** com os plugins `react`, `typescript` e `oxc`. Ele aplica as mesmas regras do ESLint que importam aqui (por exemplo `react/rules-of-hooks`) e roda em uma fração do tempo.

## Consequências

- O passo de lint do pipeline chama `npm run lint`, que executa o oxlint.
- Se o time precisar de uma regra que só exista no ESLint, a decisão é revista quando o `typescript-eslint` aceitar o TypeScript 7.
