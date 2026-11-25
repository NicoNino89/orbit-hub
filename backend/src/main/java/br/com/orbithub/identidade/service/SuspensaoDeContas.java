package br.com.orbithub.identidade.service;

import br.com.orbithub.compartilhado.erro.RecursoNaoEncontradoException;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.identidade.domain.ContaSuspensa;
import br.com.orbithub.identidade.domain.Usuario;
import br.com.orbithub.identidade.repository.UsuarioRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 16:19
 * Cartão Jira: KAN-101, KAN-102
 * Título: Suspensão de contas
 * Objetivo: Suspender ou banir a conta de uma pessoa por decisão da moderação, encerrar as sessões dela e avisar os
 *           outros módulos, e devolver o acesso quando a sanção é revogada.
 * Decisões: Contas da equipe não são suspensas por aqui (o desligamento é outro fluxo). As sessões abertas são
 *           encerradas na hora; o login fica bloqueado até a data final.
 */
@Service
public class SuspensaoDeContas {

    private final UsuarioRepository usuarios;
    private final SessaoService sessoes;
    private final ApplicationEventPublisher eventos;
    private final Clock relogio;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-101
     * Título: Criação
     * Objetivo: Receber o repositório, as sessões, os eventos e o relógio.
     * Decisões: Injeção por construtor.
     */
    public SuspensaoDeContas(UsuarioRepository usuarios, SessaoService sessoes, ApplicationEventPublisher eventos, Clock relogio) {
        this.usuarios = usuarios;
        this.sessoes = sessoes;
        this.eventos = eventos;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-101
     * Título: Suspender
     * Objetivo: Impedir o acesso até a data informada (Usuario.BANIDA no banimento), encerrar as sessões e avisar os módulos.
     * Decisões: Os efeitos (Nível 2, interesses, rodadas, reuniões) rodam na mesma transação, pelos ouvintes do evento.
     */
    @Transactional
    public void suspender(UUID usuarioId, Instant ate) {
        Usuario u = conta(usuarioId);
        u.suspender(ate, relogio.instant());
        usuarios.save(u);
        sessoes.encerrarTodas(usuarioId, "SUSPENSAO");
        eventos.publishEvent(new ContaSuspensa(usuarioId, false));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-102
     * Título: Suspender só o Nível 2
     * Objetivo: Tirar todos os acessos confidenciais da pessoa, sem suspender a conta (medida preventiva de vazamento).
     * Decisões: A pessoa continua entrando e usando o catálogo.
     */
    @Transactional
    public void suspenderNivel2(UUID usuarioId) {
        conta(usuarioId);
        eventos.publishEvent(new ContaSuspensa(usuarioId, true));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86
     * Título: Devolver o acesso
     * Objetivo: Levantar a suspensão quando a sanção é revogada ou a medida preventiva termina sem sanção.
     * Decisões: Acessos confidenciais e conexões encerrados não voltam sozinhos: a startup libera de novo se quiser.
     */
    @Transactional
    public void levantar(UUID usuarioId) {
        Usuario u = conta(usuarioId);
        u.levantarSuspensao(relogio.instant());
        usuarios.save(u);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-101
     * Título: Conta da pessoa
     * Objetivo: Ler a conta pública que vai ser sancionada.
     * Decisões: 404 se não existir; conta da equipe é recusada.
     */
    private Usuario conta(UUID usuarioId) {
        Usuario u = usuarios.findById(usuarioId).orElseThrow(() -> new RecursoNaoEncontradoException("Conta não encontrada."));
        if (u.isInterna()) {
            throw new RegraDeNegocioException("conta_da_equipe", "Contas da equipe não recebem sanções de moderação.");
        }
        return u;
    }
}
