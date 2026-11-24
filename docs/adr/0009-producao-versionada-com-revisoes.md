# ADR 0009 — Produção versionada, com aprovação e rollback por revisão

- **Data:** 24/11/2026
- **Situação:** aceita
- **Histórias:** KAN-54

## Contexto

Produção precisa de publicação controlada e reversível:

- aprovação manual;
- versão numerada;
- volta para a versão anterior sem recompilar nada;
- migrações do banco aplicadas pelo pipeline, com cuidado especial para as destrutivas.

Até aqui, os Container Apps rodavam em modo de revisão única (`Single`). Nesse modo, a revisão anterior é desligada a cada publicação e não pode receber tráfego de novo.

## Decisão

- **Modo de várias revisões** (`Multiple`) nos dois ambientes. Cada publicação cria uma revisão com o sufixo da versão e recebe 100% do tráfego. As antigas ficam desativadas: não gastam crédito e podem ser reativadas.
- **Rollback = reativar a revisão anterior** e mandar o tráfego para ela, pelo workflow *Rollback em produção* ou por `.github/scripts/revisoes.sh`. Se o smoke test falhar logo depois de uma publicação, essa volta é automática.
- **Versão**:
  - número semântico informado na publicação;
  - a mesma imagem testada no CI ganha a tag da versão;
  - tag git `vX.Y.Z` e release no GitHub, só depois do smoke test.
- **Aprovação** pelo environment `producao` do GitHub, com revisor obrigatório e só a `main`. As conferências (CI verde, imagens e migrações) rodam antes de pedir a aprovação, para não aprovar algo que já vai falhar.
- **Migrações**:
  - aplicadas pelo Flyway na subida da API;
  - nunca editadas depois de aplicadas, com um job de CI que recusa;
  - sempre compatíveis com a versão anterior;
  - as destrutivas exigem rótulo de revisão no PR e confirmação na publicação.
- O peso do tráfego sai do controle do Terraform (`ignore_changes`), como já acontecia com a imagem.

## Consequências

- Homologação também passou ao modo de várias revisões. Assim o rollback pode ser ensaiado lá antes de ser usado em produção.
- O rollback não desfaz o banco. Apagar ou renomear coluna leva duas versões (ver `docs/operacao/producao.md`).
- Um `apply` do Terraform que mude o template cria uma revisão nova sem receber tráfego. Depois de mudar a infraestrutura de um app, publicar a versão de novo.
- Produção continua com escala a zero, então as rotinas automáticas dependem da API estar acordada. Decisão pendente: 1 réplica mínima em produção.
