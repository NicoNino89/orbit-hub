package br.com.orbithub.empreendimentos.service;

import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.empreendimentos.domain.RevisaoDeCuradoria;
import br.com.orbithub.empreendimentos.domain.Startup;
import br.com.orbithub.empreendimentos.repository.RevisaoDeCuradoriaRepository;
import java.time.Clock;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 10:40
 * Cartão Jira: KAN-66
 * Título: Abertura das revisões da curadoria
 * Objetivo: Abrir a revisão quando a startup é enviada ou quando uma startup publicada muda nome, segmento ou estágio, e
 *           cancelar a revisão quando o envio é cancelado ou a alteração é desfeita.
 * Decisões: Separado do serviço da curadoria para o cadastro e as seções chamarem sem depender da fila. Alteração
 *           relevante abre a revisão sozinha, sem pedir ação do empreendedor; desfeita a alteração antes da decisão, a
 *           revisão é cancelada.
 */
@Component
public class RevisoesDaStartup {

    static final Set<RevisaoDeCuradoria.Situacao> ABERTAS = EnumSet.of(RevisaoDeCuradoria.Situacao.EM_ANALISE,
            RevisaoDeCuradoria.Situacao.AGUARDANDO_APROVACAO);

    private final RevisaoDeCuradoriaRepository revisoes;
    private final Clock relogio;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-66
     * Título: Criação
     * Objetivo: Receber o repositório e o relógio.
     * Decisões: Injeção por construtor.
     */
    RevisoesDaStartup(RevisaoDeCuradoriaRepository revisoes, Clock relogio) {
        this.revisoes = revisoes;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-66
     * Título: Abrir revisão
     * Objetivo: Pôr a startup na fila da curadoria.
     * Decisões: Recusa se já houver revisão aberta.
     */
    void abrir(Startup startup, RevisaoDeCuradoria.Tipo tipo, UUID por) {
        if (aberta(startup.getId()).isPresent()) {
            throw new RegraDeNegocioException("revisao_aberta", "A startup já está na fila da curadoria.");
        }
        revisoes.save(RevisaoDeCuradoria.nova(startup.getId(), tipo, por, relogio.instant()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-66
     * Título: Cancelar revisão aberta
     * Objetivo: Tirar da fila a revisão em análise da startup.
     * Decisões: Sem efeito se não houver revisão em análise.
     */
    void cancelar(UUID startupId) {
        aberta(startupId).filter(r -> r.getSituacao() == RevisaoDeCuradoria.Situacao.EM_ANALISE)
                .ifPresent(r -> r.cancelar(relogio.instant()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-66
     * Título: Conferir alteração relevante
     * Objetivo: Depois de uma edição, abrir a revisão da alteração (startup publicada com nome, segmento ou estágio novos)
     *           ou cancelá-la, se a edição voltou aos valores aprovados.
     * Decisões: Só mexe em revisões do tipo alteração.
     */
    public void conferirAlteracao(Startup startup, UUID por) {
        Optional<RevisaoDeCuradoria> aberta = aberta(startup.getId());
        if (startup.temAlteracaoRelevante()) {
            if (aberta.isEmpty()) {
                revisoes.save(RevisaoDeCuradoria.nova(startup.getId(), RevisaoDeCuradoria.Tipo.ALTERACAO, por, relogio.instant()));
            }
        } else {
            aberta.filter(r -> r.getTipo() == RevisaoDeCuradoria.Tipo.ALTERACAO
                            && r.getSituacao() == RevisaoDeCuradoria.Situacao.EM_ANALISE)
                    .ifPresent(r -> r.cancelar(relogio.instant()));
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-66
     * Título: Revisão aberta
     * Objetivo: Achar a revisão da startup que espera decisão.
     * Decisões: Inclui a espera da segunda aprovação.
     */
    public Optional<RevisaoDeCuradoria> aberta(UUID startupId) {
        return revisoes.findFirstByStartupIdAndSituacaoIn(startupId, ABERTAS);
    }
}
