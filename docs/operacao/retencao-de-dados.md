# Tabela de retenção de dados (KAN-93)

> **Provisório.** Esta tabela precisa entrar na **Política de Privacidade**. Depois da validação pelo jurídico, publique uma nova versão da política pela rota de publicação de documentos (administrador; ainda sem tela), o que pede novo aceite. Alterar qualquer prazo `retencao.*` ou `lgpd.retencao_legal_meses` passa pela segunda aprovação.

A rotina **R12** (diária, 3h) aplica os prazos e registra, em cada execução, só a quantidade de registros tratados por tipo, nunca os dados.

| Dado | Prazo | Parâmetro | O que acontece |
|---|---|---|---|
| Links de e-mail e sessões vencidos | 30 dias depois de vencer | `retencao.tokens_dias` | Exclusão |
| Cadastro não confirmado | 7 dias | `cadastro.prazo_confirmacao_dias` | Anonimização da conta |
| Histórico de visualizações do catálogo | 12 meses | `retencao.historico_visualizacoes_meses` | Exclusão |
| Chamados de atendimento fechados | 24 meses | `retencao.chamados_meses` | Texto e contato anonimizados (categoria, prazos e nota ficam para os indicadores) |
| Denúncias de casos decididos | 60 meses | `retencao.denuncias_meses` | Texto e evidências anonimizados (o caso e a sanção ficam no histórico) |
| Notificações | conforme `notificacoes.retencao_dias` | `notificacoes.retencao_dias` | Exclusão (rotina R19) |
| Documentos de verificação | enquanto a verificação vale, mais o prazo legal | `verificacao.*` | Exclusão (rotina R9) |
| Conta excluída (guarda legal) | 60 meses depois da exclusão | `lgpd.retencao_legal_meses` | A conta fica anonimizada; detalhes dos pedidos do titular e o histórico da conta são apagados ao fim do prazo |
| Trilha de auditoria | a definir pelo jurídico | — | Mantida |

Conta inativa: depois de `conta.inatividade_meses` (12) sem acesso, a pessoa é avisada; sem novo acesso em `conta.aviso_inatividade_dias` (30), a conta é desativada (rotina R11). Os dados seguem esta tabela.
