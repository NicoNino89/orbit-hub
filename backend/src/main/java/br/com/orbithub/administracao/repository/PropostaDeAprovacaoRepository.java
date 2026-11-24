package br.com.orbithub.administracao.repository;

import br.com.orbithub.administracao.domain.AcaoGrave;
import br.com.orbithub.administracao.domain.PropostaDeAprovacao;
import br.com.orbithub.administracao.domain.PropostaDeAprovacao.Situacao;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:52
 * Cartão Jira: KAN-81
 * Título: Repositório das propostas de aprovação
 * Objetivo: Gravar e consultar as propostas de ações graves.
 * Decisões: Spring Data; as consultas de fila ordenam da mais antiga para a mais nova.
 */
public interface PropostaDeAprovacaoRepository extends JpaRepository<PropostaDeAprovacao, UUID> {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Propostas numa situação
     * Objetivo: Listar as propostas de uma situação.
     * Decisões: Pendentes da mais antiga para a mais nova (quem espera mais aparece primeiro).
     */
    List<PropostaDeAprovacao> findBySituacaoOrderByPropostoEmAsc(Situacao situacao);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Decididas recentemente
     * Objetivo: Listar as últimas propostas decididas, para o histórico da tela.
     * Decisões: Limitadas às 50 mais recentes.
     */
    List<PropostaDeAprovacao> findTop50BySituacaoNotOrderByDecididoEmDesc(Situacao situacao);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Já existe pendente
     * Objetivo: Dizer se já há proposta pendente da mesma ação sobre o mesmo objeto.
     * Decisões: O banco também garante, por índice único parcial.
     */
    boolean existsByTipoAndObjetoTipoAndObjetoIdAndSituacao(AcaoGrave tipo, String objetoTipo, String objetoId,
                                                            Situacao situacao);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Pendentes vencidas sem alerta
     * Objetivo: Listar as propostas paradas há mais do que o prazo e ainda não alertadas.
     * Decisões: Usado pela rotina de alerta (KAN-104); "limite" é agora menos o prazo.
     */
    @Query("""
            select p from PropostaDeAprovacao p
            where p.situacao = br.com.orbithub.administracao.domain.PropostaDeAprovacao.Situacao.PENDENTE
            and p.alertadaEm is null and p.propostoEm <= :limite
            order by p.propostoEm""")
    List<PropostaDeAprovacao> pendentesVencidasSemAlerta(@Param("limite") Instant limite);
}
