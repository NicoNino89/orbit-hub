# Primeiro administrador da plataforma (KAN-75)

Só um administrador cadastra a equipe interna. O primeiro é criado na implantação, por este procedimento. Ele funciona igual no ambiente local, em homologação e em produção.

## Como funciona

Na subida, a API confere a variável `ORBIT_PRIMEIRO_ADMIN_EMAIL`:

- **vazia:** não faz nada;
- **preenchida e ainda não há administrador ativo:** cria a conta da equipe com o papel de administrador e emite um **convite**. A pessoa abre o link, escolhe a própria senha e, no primeiro login, configura o aplicativo autenticador (segundo fator). Ninguém define a senha por ela;
- **preenchida, mas já existe administrador ativo:** não faz nada. A variável pode ficar preenchida sem risco.

O convite vale por 14 dias (`convite.validade_dias`) e só pode ser usado uma vez. Se vencer, basta reiniciar a API: um convite novo é emitido e o antigo deixa de valer.

O e-mail precisa ser **só da equipe**. Uma conta de investidor ou empreendedor com o mesmo e-mail impede a criação, e o erro aparece no log.

## Onde chega o link

| Envio de e-mail (`ORBIT_EMAIL_TIPO`) | O link chega... |
|---|---|
| `smtp` (Mailpit no local, provedor real na nuvem) | por e-mail |
| `registro` (nuvem, enquanto não há provedor de e-mail) | **no log da API**, uma única vez, numa linha `Primeiro administrador: sem provedor de e-mail, abra este link...` |

O log da API só é lido por quem já administra a infraestrutura na Azure. Depois de usado, o link não vale mais.

## Passo a passo

### Ambiente local

1. No `.env`, fixe `ORBIT_JWT_CHAVE` (gere com `openssl rand -base64 32`). Sem ela, a API gera uma chave nova a cada início, e o segundo fator da equipe precisa ser configurado de novo depois de cada reinício.
2. Ainda no `.env`, preencha `ORBIT_PRIMEIRO_ADMIN_EMAIL=seu-email-da-equipe@...` e, se quiser, `ORBIT_PRIMEIRO_ADMIN_NOME`.
3. `docker compose up -d --build api`.
4. Abra o Mailpit (http://localhost:8025), clique no link do convite, escolha a senha e entre em http://localhost:5173/entrar.
5. Configure o aplicativo autenticador com a chave mostrada na tela e digite o código.

### Homologação e produção

1. Crie o segredo **`PRIMEIRO_ADMIN_EMAIL`** do repositório em https://github.com/NicoNino89/orbit-hub/settings/secrets/actions/new. É um segredo, e não uma variável, porque o repositório é público. Pelo site, e não por `gh secret set` sem `--body`, que grava o segredo vazio.
2. Rode o workflow **Infraestrutura** com `apply` no ambiente. A variável é marcada como sensível e não aparece no log do `plan`.
3. Publique ou reinicie a API e leia o log dela, procurando a linha `Primeiro administrador` (em produção, troque `hml` por `prd`):

   ```bash
   az containerapp logs show -g rg-orbithub-hml -n ca-orbithub-hml-api --type console --tail 300 | grep "Primeiro administrador"
   ```
4. Abra o link, escolha a senha, entre e configure o aplicativo autenticador.

## Se o administrador perder o celular

Outro administrador usa **Backoffice → Equipe → Redefinir segundo fator**. Se não houver outro administrador ativo, o caminho técnico é:

1. no banco, apagar o segredo: `UPDATE usuario SET totp_segredo = NULL, totp_pendente = NULL, totp_ultimo_passo = NULL WHERE email = '...';`
2. no próximo login, configurar o aplicativo de novo.

Registre esse uso no histórico do time, porque essa alteração direta não passa pela trilha de auditoria.

## Troca da chave da aplicação

O segredo do segundo fator fica cifrado no banco com uma chave derivada de `ORBIT_JWT_CHAVE`. Se essa chave for trocada (rotação no Key Vault), os segundos fatores deixam de ser lidos. Nesse caso, todos da equipe configuram o aplicativo de novo, com o procedimento acima.
