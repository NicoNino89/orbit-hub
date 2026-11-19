# PoC KAN-58: virada de páginas do catálogo

- **Data:** 18/11/2026
- **Resultado:** `react-pageflip` **reprovada**. A escolha é uma **implementação própria com CSS 3D**, já feita nesta PoC.
- **Habilita:** KAN-35, KAN-36 e KAN-40
- **Aprovação final:** depende do seu teste no computador, no tablet e no celular (roteiro no fim deste documento)

## 1. Avaliação da biblioteca candidata

| Critério do cartão | `react-pageflip` 2.0.3 (sobre `page-flip` 2.0.7) | Situação |
|---|---|---|
| Manutenção ativa | Última versão publicada em **15/05/2022**. Último commit em **26/08/2023**, com 54 issues abertas (e 48 no `page-flip`, sem commit desde 01/2024) | Reprovado |
| Compatível com o React adotado (19.3) | Não declara `peerDependencies` de React, então a compatibilidade com React 18 e 19 nunca foi garantida pelos autores | Reprovado |
| Segurança da cadeia de dependências | Depende de `page-flip` com a versão **`latest`**: qualquer publicação nova entraria no build sem revisão | Reprovado |
| Movimento reduzido | Sem suporte nativo a `prefers-reduced-motion` | Não atende |
| Acessibilidade | Desenha as páginas em canvas ou em HTML sem papéis ARIA nem anúncio da página atual | Não atende |

Com quatro critérios reprovados, a biblioteca não foi instalada.

## 2. Alternativa escolhida: implementação própria

Componente `Folheador` (`frontend/src/catalogo/Folheador.tsx`), sem dependência externa. É o mesmo efeito do kit de interface *Orbit Hub Interface*: cada folha gira em torno da lombada (`rotateY`) com `backface-visibility: hidden`.

| Critério do cartão | Como foi atendido | Teste automatizado |
|---|---|---|
| 3 startups fictícias com 3 páginas cada | NovaSafra, Lumen Saúde e Trama Logística. Cada uma tem capa, problema e solução, e rodada e critérios | `PocCatalogo.test.tsx` |
| Mouse | Setas laterais, pontos de página e arrastar com o mouse | `Folheador.test.tsx` |
| Teclado | ← →, Page Up/Down, Home e End com o foco no catálogo; o foco fica visível | `Folheador.test.tsx` |
| Deslizar no celular | Pointer Events com limiar de 48 px. O gesto só vale se for mais horizontal que vertical (`touch-action: pan-y`), então a rolagem da tela continua funcionando | `gesto.test.ts`, `Folheador.test.tsx` |
| Movimento reduzido | Com `prefers-reduced-motion: reduce`, a duração da virada vai a 0 e a página troca na hora | `Folheador.test.tsx` |
| Desempenho em celular de entrada | Só `transform` é animado, o que a GPU compõe sem refazer o layout. Só a folha atual e as duas vizinhas ficam visíveis; as outras recebem `hidden` e saem da renderização | `Folheador.test.tsx` (folhas distantes ocultas) |
| Acessibilidade (extra) | `role="region"` com `aria-roledescription="catálogo"`, anúncio *"Página 2 de 9: …"* em `aria-live` e folhas fora da tela com `inert`, sem foco nem leitura | `Folheador.test.tsx` |
| Regras da plataforma (extra) | O conteúdo mostra só os critérios da tese que combinaram, nunca percentual, e traz o aviso de que a plataforma não recomenda investimento | `PocCatalogo.test.tsx` |

### Layout por tamanho de tela

| Tela | Formato da página | Navegação |
|---|---|---|
| Computador e tablet na horizontal | 3:2 | Setas, teclado, pontos e arrastar |
| Celular (até 640 px) | 3:4, em pé | Deslizar e pontos. As setas saem para dar espaço |

## 3. Riscos que continuam abertos

- **Celular de entrada real:** a fluidez só se confirma no aparelho. Se travar, a próxima medida é trocar a virada 3D por um deslize 2D (`translateX`) só em telas pequenas, sem mudar o componente.
- **Edições com muitas páginas:** a PoC tem 9. Com dezenas de páginas, o KAN-35 deve carregar o conteúdo sob demanda. A estrutura de folhas vizinhas já está pronta para isso.

## 4. Roteiro de aprovação (teste humano)

Abra a página inicial e clique em **Ver a prova de conceito do catálogo**, ou acesse `#/poc-catalogo`.

1. **Computador:** passe pelas 9 páginas com as setas, depois com ← → e por fim arrastando com o mouse. Use Home e End. Clique num ponto do rodapé.
2. **Tablet:** deslize para os lados. Gire o aparelho e confira se o layout se ajusta.
3. **Celular:** deslize para os lados. Depois role a tela para cima e para baixo **sobre** o catálogo: a página **não** pode virar sem querer.
4. **Movimento reduzido:** no Windows, desligue *Configurações › Acessibilidade › Efeitos visuais › Efeitos de animação*. No Android, ative *Remover animações*. No iPhone, ative *Reduzir movimento*. Recarregue: as páginas devem trocar sem animação.
5. **Tema escuro:** com o sistema no modo escuro, as páginas e os textos devem continuar legíveis.

Aprovado ou reprovado, registre o resultado no KAN-58. Uma reprovação vira um Bug ligado ao cartão.
