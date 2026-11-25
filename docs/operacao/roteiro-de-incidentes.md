# Roteiro de resposta a incidentes com dados pessoais (KAN-91)

> **Provisório.** Os prazos e o modelo de comunicação precisam ser validados pelo jurídico antes do lançamento. Revisão anual obrigatória (próxima: novembro de 2027).

## Quem faz o quê

| Papel | Responsabilidade |
|---|---|
| Qualquer pessoa da equipe | Registra a suspeita em **Backoffice → Painel de operação → Suspeita de incidente** assim que perceber o problema |
| Encarregado de dados | Conduz o incidente: avaliação, decisões, comunicações e encerramento (**Backoffice → Privacidade**) |
| Administrador (equipe técnica) | Recebe o aviso junto com o encarregado e executa a contenção técnica |
| Jurídico | Avalia o risco aos titulares e o texto das comunicações |

## Passos

1. **Registrar.** Título, o que aconteceu e, se souber, a data da detecção. O encarregado e os administradores são avisados na hora (sino e e-mail).
2. **Conter.** Revogar acessos, links ou credenciais expostos e preservar as evidências (logs, trilha de auditoria). Registrar a contenção no incidente.
3. **Avaliar.** Dados e titulares afetados e o risco: **baixo**, **médio** ou **alto** (risco ou dano relevante aos titulares).
4. **Comunicar.** Com risco alto, comunicar a ANPD e os titulares dentro de `lgpd.prazo_comunicacao_dias_uteis` dias úteis a partir da detecção (padrão: 3, conforme a regulamentação da ANPD; validar com o jurídico). Registrar cada comunicação no incidente. O painel destaca a comunicação atrasada.
5. **Encerrar.** Só com as **lições aprendidas** e as **ações preventivas**. Com risco alto, as duas comunicações precisam estar registradas.

Cada passo fica no histórico do incidente, com autor e data.

## Conteúdo mínimo da comunicação à ANPD

Natureza dos dados afetados, titulares envolvidos, medidas técnicas de segurança usadas, riscos, motivos de eventual demora e medidas adotadas para reverter ou mitigar os efeitos.
