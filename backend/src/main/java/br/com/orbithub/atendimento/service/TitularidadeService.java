package br.com.orbithub.atendimento.service;

import br.com.orbithub.administracao.domain.AcaoGrave;
import br.com.orbithub.administracao.domain.PropostaDeAprovacao;
import br.com.orbithub.administracao.service.SegundaAprovacaoService;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.empreendimentos.service.StartupsService;
import br.com.orbithub.identidade.service.ConsultaDeContas;
import br.com.orbithub.investidores.service.OrganizacoesService;
import br.com.orbithub.notificacoes.domain.TipoDeNotificacao;
import br.com.orbithub.notificacoes.service.NotificacoesService;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 20:54
 * Cartão Jira: KAN-77
 * Título: Cadastro assistido e transferência de titularidade
 * Objetivo: Deixar o atendimento N2 criar a organização convidada e convidar o responsável, e propor, com a comprovação
 *           documental, a transferência da administração de uma startup ou organização para outro membro, que só acontece
 *           com a segunda aprovação.
 * Decisões: A equipe nunca define senha, aceita termos nem preenche dados sensíveis em nome do usuário. O novo titular
 *           precisa ser membro ativo e é indicado pelo e-mail da conta. Tudo fica na trilha de auditoria (ações da equipe).
 */
@Service
public class TitularidadeService {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-77
     * Título: O que é transferido
     * Objetivo: Enumerar os cadastros com titularidade.
     * Decisões: Startup e organização investidora.
     */
    public enum Alvo { STARTUP, ORGANIZACAO }

    private final OrganizacoesService organizacoes;
    private final StartupsService startups;
    private final SegundaAprovacaoService aprovacoes;
    private final ConsultaDeContas contas;
    private final NotificacoesService notificacoes;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-77
     * Título: Criação
     * Objetivo: Receber os serviços envolvidos.
     * Decisões: Injeção por construtor.
     */
    public TitularidadeService(OrganizacoesService organizacoes, StartupsService startups, SegundaAprovacaoService aprovacoes, ConsultaDeContas contas,
                               NotificacoesService notificacoes) {
        this.organizacoes = organizacoes;
        this.startups = startups;
        this.aprovacoes = aprovacoes;
        this.contas = contas;
        this.notificacoes = notificacoes;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-77
     * Título: Convite institucional
     * Objetivo: Criar a organização convidada e mandar o convite ao responsável.
     * Decisões: Devolve o identificador da organização.
     */
    @Transactional
    public UUID convidarOrganizacao(UUID equipe, OrganizacoesService.Dados dados, String emailResponsavel) {
        return organizacoes.criarAssistida(equipe, dados, emailResponsavel).getOrganizacaoId();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-77, KAN-81
     * Título: Propor transferência
     * Objetivo: Levar à segunda aprovação a transferência da titularidade para um membro ativo, com a comprovação.
     * Decisões: A comprovação (documentos conferidos) é obrigatória e vai na justificativa.
     */
    @Transactional
    public PropostaDeAprovacao propor(UUID equipe, Alvo alvo, UUID alvoId, String emailNovoTitular, String comprovacao) {
        if (alvo == null || alvoId == null) {
            throw new RegraDeNegocioException("alvo_obrigatorio", "Indique a startup ou a organização.");
        }
        UUID novo = contas.contaAtivaDoEmail(emailNovoTitular == null ? "" : emailNovoTitular.strip().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new RegraDeNegocioException("titular_invalido", "Não há conta ativa com este e-mail."));
        String nome = alvo == Alvo.STARTUP ? startups.ler(alvoId).getNome() : organizacoes.nome(alvoId)
                .orElseThrow(() -> new RegraDeNegocioException("alvo_obrigatorio", "Organização não encontrada."));
        return aprovacoes.propor(AcaoGrave.TRANSFERENCIA_TITULARIDADE, alvo.name(), alvoId.toString(), "Transferir " + nome + " para "
                + contas.nome(novo).orElse(emailNovoTitular), Map.of("alvo", alvo.name(), "novoTitular", novo.toString()), equipe, comprovacao);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-77
     * Título: Concluir transferência
     * Objetivo: Aplicar a transferência aprovada e avisar o novo titular.
     * Decisões: Chamado pelo executor da ação grave.
     */
    @Transactional
    public void concluir(PropostaDeAprovacao proposta) {
        Alvo alvo = Alvo.valueOf(proposta.getDados().get("alvo"));
        UUID alvoId = UUID.fromString(proposta.getObjetoId());
        UUID novo = UUID.fromString(proposta.getDados().get("novoTitular"));
        if (alvo == Alvo.STARTUP) {
            startups.transferirTitularidade(alvoId, novo);
        } else {
            organizacoes.transferirAdministracao(alvoId, novo);
        }
        notificacoes.notificar(novo, alvo == Alvo.STARTUP ? TipoDeNotificacao.STARTUP : TipoDeNotificacao.ORGANIZACAO, "Você agora é o titular",
                "A administração foi transferida para você pelo atendimento, depois da análise e da segunda aprovação.", null);
    }
}
