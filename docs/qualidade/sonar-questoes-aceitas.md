# Questões do SonarCloud revisadas

Revisão de 23/11/2026, feita sobre as 22 questões da primeira análise da `main` (KAN-51). Cada questão foi corrigida ou aceita com justificativa. As aceitas ficam fora da análise em `sonar-project.properties` (`sonar.issue.ignore.multicriteria`), com o motivo ao lado.

## Corrigidas

| Regra | Onde | Correção |
|---|---|---|
| `githubactions:S7637` (14×) | todos os workflows | Actions fixadas pelo SHA do commit, com a versão em comentário. O Dependabot (`.github/dependabot.yml`) abre um PR por semana para atualizá-las. |
| `githubactions:S7631` | `cd-homologacao.yml` | A entrega já só rodava para CI disparado por `push`; agora também confere que o commit veio do próprio repositório (`head_repository`). Um fork nunca publica em homologação. |
| `githubactions:S8233` | `infra.yml` | `id-token: write` saiu do nível do workflow e foi para os dois jobs que fazem login na Azure. |
| `githubactions:S6505` e `docker:S6505` | `ci.yml` e `frontend/Dockerfile` | `npm ci --ignore-scripts`. Nenhuma dependência do front-end precisa de script de instalação. |

## Aceitas

| Regra | Onde | Por que é seguro aqui |
|---|---|---|
| `terraform:S8847` (2×) | Key Vaults | A proteção contra expurgo não pode ser desligada depois de ligada e impede apagar de vez o cofre e os segredos antes do fim da retenção. Os ambientes são de estudo e recriados com `terraform destroy`; os segredos são todos regeráveis (senhas aleatórias, connection string, token do GHCR). A exclusão temporária (*soft delete*) de 7 dias continua ligada. **Reavaliar antes de dados reais em produção.** |
| `terraform:S6378` | Storage Account | A identidade da própria conta de armazenamento só é usada para criptografia com chave gerenciada pelo cliente, que não usamos. O acesso aos arquivos é feito pela identidade gerenciada da API, sem chave de acesso (`shared_access_key_enabled = false`). |
| `typescript:S6845` | `Folheador.tsx` | O livro do catálogo é um componente operado pelo teclado (setas viram as páginas), com `role="region"` e rótulo. Ele precisa receber foco para isso funcionar (WCAG 2.1.1). Os botões de anterior e próxima continuam como alternativa. |

## Configuração da análise

- Os testes do back-end eram contados como código de produção, com 0% de cobertura, porque o SonarCloud deixou de considerar `sonar.sources`, `sonar.tests` e `sonar.test.inclusions`. Agora todos os testes saem da análise por `sonar.exclusions` (como já era com os do front-end), e a cobertura mede só o código de produção. O Sonar deixa de revisar a qualidade do código de teste; o oxlint e o compilador continuam olhando para ele.
- Os `tsconfig*.json` saíram da análise: têm comentários, e o analisador de JSON não os lê.
