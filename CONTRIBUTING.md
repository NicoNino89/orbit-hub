# Como contribuir com o Orbit Hub

## Fluxo de trabalho (GitHub Flow)

1. Escolha uma issue no [Jira](https://orbithub.atlassian.net/jira/software/projects/KAN) e mova para "Em andamento".
2. Atualize a `main` e crie uma branch a partir dela:
   ```bash
   git switch main
   git pull
   git switch -c feat/KAN-35-navegacao-horizontal
   ```
3. Faça commits pequenos e frequentes.
4. Abra um pull request para a `main` preenchendo o template.
5. Aguarde o pipeline e a revisão de pelo menos uma pessoa do time.
6. Depois de aprovado, faça o merge. A branch é apagada sozinha no GitHub.

A `main` deve estar sempre estável. Nada é enviado diretamente para ela.

## Proteção da `main`

A `main` é protegida no GitHub (o repositório é público justamente para isso: em repositório privado, a proteção exige o plano Pro):

- nada entra por push direto, só por pull request, e a regra vale também para administradores;
- o merge só é liberado com estes checks verdes e com a branch atualizada em relação à `main`:
  - `Back-end (build, testes e cobertura)`
  - `Front-end (lint, tipos, testes e build)`
  - `SonarCloud (quality gate)`
  - `Observabilidade (Prometheus e Grafana)`
- todas as conversas do pull request precisam estar resolvidas;
- a `main` não aceita push forçado nem pode ser apagada.

Enquanto o time tiver uma só conta no GitHub, a revisão é a aprovação do Product Owner registrada no próprio pull request ou no cartão do Jira: o GitHub não deixa o autor aprovar o próprio pull request, então a regra não exige aprovação formal. Quando entrar a segunda pessoa, passa a exigir uma aprovação.

## Nome das branches

```
<tipo>/KAN-<número>-<descricao-curta>
```

Exemplos:

- `feat/KAN-12-cadastro-usuario`
- `fix/KAN-45-validacao-interesse`
- `chore/KAN-51-pipeline-ci`

## Mensagens de commit (Conventional Commits)

```
<tipo>(<escopo opcional>): <descrição no imperativo> KAN-<número>
```

| Tipo | Quando usar |
|---|---|
| `feat` | Nova funcionalidade |
| `fix` | Correção de defeito |
| `docs` | Somente documentação |
| `test` | Criação ou ajuste de testes |
| `refactor` | Mudança de código sem alterar comportamento |
| `chore` | Configuração, dependências, tarefas de manutenção |
| `ci` | Pipelines e automações |
| `infra` | Terraform e infraestrutura |

Exemplos:

- `feat(identidade): emitir refresh token revogável KAN-57`
- `ci: adicionar build do back-end no GitHub Actions KAN-51`
- `infra: declarar Container Apps no Terraform KAN-55`

Citar a chave `KAN-<número>` permite ligar commits, branches e pull requests às issues do Jira.

## Escopos sugeridos (módulos)

`identidade`, `investidores`, `empreendimentos`, `catalogo`, `conexoes`, `recomendacao`, `publicidade`, `administracao`, `frontend`, `infra`.

## Definição de pronto

Uma história só está pronta quando:

- todos os critérios de aceite da issue foram atendidos;
- há testes automatizados cobrindo o comportamento novo;
- o pipeline está verde, incluindo cobertura e quality gate;
- o código foi revisado e aprovado em pull request;
- a documentação foi atualizada, quando necessário;
- nenhum segredo ou dado pessoal real foi adicionado ao repositório;
- os logs novos seguem as regras de [`docs/observabilidade.md`](docs/observabilidade.md): nenhum dado pessoal ou sensível.
