# Publicação em produção e rollback (KAN-54)

Produção recebe só versões numeradas da `main`, depois da aprovação manual no environment **producao** do GitHub. Toda publicação pode ser desfeita pela revisão anterior do Azure Container Apps.

## Publicar uma versão

1. Na `main`, o CI do commit precisa estar verde. É ele que publica as imagens `orbit-hub-api:<commit>` e `orbit-hub-web:<commit>` no GHCR.
2. Se a versão traz mudança de infraestrutura, rodar antes o workflow **Infraestrutura** (`apply`, camada `producao`). Ele também pede aprovação.
3. Rodar o workflow **Publicação em produção** e informar:
   - **versão** no formato `X.Y.Z`. Use o [versionamento semântico](https://semver.org/lang/pt-BR/): correção sobe o último número, funcionalidade nova o do meio, quebra de compatibilidade o primeiro.
   - **commit**: o SHA completo. Vazio publica o último da `main`.
   - **confirmação de migração destrutiva**: só depois da revisão específica descrita abaixo.
4. O job **Conferir a versão** roda sem aprovação e verifica:
   - que a versão é nova;
   - que o commit está na `main` com o CI verde;
   - que as imagens existem;
   - as migrações desde a última versão.
5. O job **Publicar em produção** espera a aprovação de um revisor do environment `producao`. Aprovado, ele:
   - põe a tag `X.Y.Z` nas imagens que passaram no CI, sem recompilar;
   - cria as revisões `ca-orbithub-prd-api--vX-Y-Z` e `ca-orbithub-prd-web--vX-Y-Z` e manda 100% do tráfego para elas. O Flyway aplica as migrações quando a API nova sobe;
   - roda o smoke test (`.github/scripts/smoke-test.sh`, com `AMBIENTE=producao`);
   - **se o smoke test passar**: desativa as revisões antigas (desativada não gasta crédito) e cria a tag git `vX.Y.Z` e o release no GitHub, com as notas geradas dos PRs;
   - **se o smoke test falhar**: devolve o tráfego para as revisões que estavam no ar e termina com erro. Não sai tag nem release.

## Voltar uma versão (rollback)

Workflow **Rollback em produção**, também com aprovação:

- sem informar revisões, volta para a **anterior** à que está no ar (pela data de criação);
- para ir a uma revisão específica, informe os nomes (lista em `az containerapp revision list -g rg-orbithub-prd -n ca-orbithub-prd-api --all -o table`).

O workflow reativa a revisão, manda 100% do tráfego para ela e roda o smoke test com a versão dela. Depois desativa a que saiu.

O mesmo procedimento à mão, se o GitHub Actions estiver fora do ar:

```bash
bash .github/scripts/revisoes.sh fixar rg-orbithub-prd ca-orbithub-prd-api ca-orbithub-prd-api--v1-0-0
bash .github/scripts/revisoes.sh fixar rg-orbithub-prd ca-orbithub-prd-web ca-orbithub-prd-web--v1-0-0
```

O rollback **não desfaz migrações**. Por isso vale a regra abaixo.

### Ensaio

O workflow de rollback aceita `ambiente = homologacao`. Use para ensaiar o procedimento sem tocar em produção, depois de duas publicações em homologação.

## Migrações do Flyway

- Ficam versionadas em `backend/src/main/resources/db/migration` e são aplicadas pelo próprio pipeline, quando a API nova sobe.
- **Uma migração aplicada nunca muda.** O job *Migrações do Flyway* do CI recusa um PR que edita, apaga ou renomeia uma migração existente. Corrigir é sempre criar uma nova.
- **Compatível com a versão anterior.** A versão anterior precisa continuar funcionando com o banco já migrado, porque é para ela que o rollback volta. O Flyway da versão antiga ignora as migrações mais novas que encontra no banco. Na prática:
  - acrescentar tabela ou coluna anulável: pode;
  - apagar, renomear ou trocar o tipo de uma coluna: em duas versões. Primeiro o código para de usar; numa versão seguinte, a migração remove.
- **Migração destrutiva** é aquela com `DROP`, `TRUNCATE`, `DELETE FROM`, `RENAME` ou troca de tipo de coluna. Ela exige uma **revisão específica**:
  1. conferir que nenhuma versão no ar (nem a anterior) ainda usa o que vai ser apagado;
  2. conferir que há backup recente do banco (o PostgreSQL Flexible guarda 7 dias) e, se houver dado a preservar, uma cópia antes;
  3. registrar a revisão no PR com o rótulo `migracao-destrutiva-revisada` e rodar o job de novo;
  4. na publicação, marcar a confirmação de migração destrutiva.

## Configuração

- Environment `producao` no GitHub:
  - aprovação obrigatória de um revisor;
  - só a `main` pode publicar.
- Container Apps em modo de várias revisões (`modo_revisao = "Multiple"` no módulo `infra/modules/ambiente`). O peso do tráfego é do pipeline: o Terraform ignora mudanças nele, para um `apply` não desfazer um rollback.
- Réplicas mínimas: 0, com escala a zero para economizar crédito. Com 0, as rotinas automáticas (KAN-104) só rodam enquanto a API estiver acordada. Decidir se produção passa a 1 réplica.
