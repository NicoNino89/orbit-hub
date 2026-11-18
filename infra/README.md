# Infraestrutura — Terraform (Azure)

Infraestrutura como código do Orbit Hub, com **Terraform** e o provider **`azurerm`**.

> Os arquivos Terraform serão criados na história KAN-55.

## Regras para a assinatura Azure for Students (crédito de US$ 100)

- Antes do primeiro `terraform apply`, configure um orçamento no Azure Cost Management com alertas em 50%, 80% e 100%.
- Use as camadas gratuitas ou de menor custo descritas em [`../docs/arquitetura.md`](../docs/arquitetura.md).
- Mantenha o servidor PostgreSQL parado quando não estiver em uso.
- Use somente regiões permitidas pela política da assinatura.
- Destrua os ambientes de demonstração quando não forem mais necessários (`terraform destroy`).

## Estrutura prevista

```
infra/
├── modules/          # módulos reutilizáveis (container app, banco, storage...)
└── envs/
    ├── homologacao/
    └── producao/
```

O estado do Terraform fica remoto, em uma Storage Account da Azure. Nunca versione arquivos `*.tfstate` nem `*.tfvars` com segredos.
