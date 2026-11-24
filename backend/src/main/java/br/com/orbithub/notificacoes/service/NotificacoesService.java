package br.com.orbithub.notificacoes.service;

import br.com.orbithub.compartilhado.erro.RecursoNaoEncontradoException;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.notificacoes.domain.Notificacao;
import br.com.orbithub.notificacoes.domain.TipoDeNotificacao;
import br.com.orbithub.notificacoes.domain.TipoDeNotificacao.Canal;
import br.com.orbithub.notificacoes.repository.NotificacaoRepository;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:36
 * Cartão Jira: KAN-146
 * Título: Central de notificações
 * Objetivo: Receber os avisos dos módulos e entregá-los pelo canal escolhido pela pessoa (plataforma, e-mail ou os
 *           dois), imediato ou no resumo diário, e deixar a pessoa ler, marcar e escolher as preferências.
 * Decisões: {@link #notificar} só grava: a notificação e, se houver e-mail, um item na fila. O envio é feito pela
 *           rotina R17, então a ação que gerou o aviso nunca espera nem falha por causa do e-mail. Aviso obrigatório
 *           ignora a preferência: vai pelos dois canais e nunca vai para o resumo. Chamar dentro da transação de quem
 *           gera o evento: se a ação for desfeita, o aviso também é.
 */
@Service
public class NotificacoesService {

    static final int TAMANHO_PAGINA = 20;

    private final NotificacaoRepository notificacoes;
    private final JdbcTemplate jdbc;
    private final Clock relogio;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:36
     * Cartão Jira: KAN-146
     * Título: Criação do serviço
     * Objetivo: Receber o repositório, o acesso ao banco (fila e preferências) e o relógio.
     * Decisões: Injeção por construtor.
     */
    public NotificacoesService(NotificacaoRepository notificacoes, JdbcTemplate jdbc, Clock relogio) {
        this.notificacoes = notificacoes;
        this.jdbc = jdbc;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:36
     * Cartão Jira: KAN-146
     * Título: Notificar uma pessoa
     * Objetivo: Registrar um aviso para a pessoa, pelo canal que ela escolheu.
     * Decisões: Sem preferência gravada, vale o canal padrão do tipo.
     */
    @Transactional
    public Notificacao notificar(UUID usuarioId, TipoDeNotificacao tipo, String titulo, String mensagem, String link) {
        Instant agora = relogio.instant();
        Canal canal = tipo.isObrigatoria() ? Canal.AMBOS : canal(usuarioId, tipo);
        Notificacao notificacao = notificacoes.saveAndFlush(Notificacao.nova(usuarioId, tipo, titulo, mensagem, link,
                canal.naPlataforma(), agora));
        if (canal.porEmail()) {
            boolean noResumo = !tipo.isObrigatoria() && resumoDiario(usuarioId);
            jdbc.update("""
                    INSERT INTO notificacao_email (id, notificacao_id, usuario_id, no_resumo, situacao, proxima_tentativa_em)
                    VALUES (?, ?, ?, ?, 'PENDENTE', ?)""", UUID.randomUUID(), notificacao.getId(), usuarioId, noResumo,
                    Timestamp.from(agora));
        }
        return notificacao;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:36
     * Cartão Jira: KAN-146
     * Título: Notificar várias pessoas
     * Objetivo: Registrar o mesmo aviso para várias pessoas (por exemplo, termos atualizados ou a equipe de aprovação).
     * Decisões: Uma notificação por pessoa, cada uma no canal dela.
     */
    @Transactional
    public int notificarTodos(Collection<UUID> usuarios, TipoDeNotificacao tipo, String titulo, String mensagem,
                              String link) {
        usuarios.forEach(u -> notificar(u, tipo, titulo, mensagem, link));
        return usuarios.size();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:36
     * Cartão Jira: KAN-146
     * Título: Avisos da pessoa
     * Objetivo: Listar os avisos do sino, paginados.
     * Decisões: Página de 20.
     */
    @Transactional(readOnly = true)
    public Page<Notificacao> listar(UUID usuarioId, int pagina) {
        return notificacoes.findByUsuarioIdAndNaPlataformaTrueOrderByCriadaEmDesc(usuarioId,
                PageRequest.of(Math.max(pagina, 0), TAMANHO_PAGINA));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:36
     * Cartão Jira: KAN-146
     * Título: Não lidos
     * Objetivo: Contar os avisos não lidos, para o contador do sino.
     * Decisões: Só os da plataforma.
     */
    @Transactional(readOnly = true)
    public long naoLidas(UUID usuarioId) {
        return notificacoes.countByUsuarioIdAndNaPlataformaTrueAndLidaEmIsNull(usuarioId);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:36
     * Cartão Jira: KAN-146
     * Título: Marcar como lida
     * Objetivo: Marcar um aviso da pessoa como lido.
     * Decisões: Aviso de outra pessoa: 404.
     */
    @Transactional
    public void marcarLida(UUID usuarioId, UUID notificacaoId) {
        notificacoes.findByIdAndUsuarioId(notificacaoId, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Notificação não encontrada."))
                .marcarLida(relogio.instant());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:36
     * Cartão Jira: KAN-146
     * Título: Marcar todas como lidas
     * Objetivo: Zerar o contador do sino.
     * Decisões: Devolve quantas foram marcadas.
     */
    @Transactional
    public int marcarTodasLidas(UUID usuarioId) {
        return notificacoes.marcarTodasLidas(usuarioId, relogio.instant());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:36
     * Cartão Jira: KAN-146
     * Título: Preferências da pessoa
     * Objetivo: Devolver o canal de cada tipo e se a pessoa quer o resumo diário.
     * Decisões: Obrigatórios aparecem sempre com os dois canais.
     */
    @Transactional(readOnly = true)
    public Preferencias preferencias(UUID usuarioId) {
        Map<TipoDeNotificacao, Canal> canais = new EnumMap<>(TipoDeNotificacao.class);
        Arrays.stream(TipoDeNotificacao.values())
                .forEach(t -> canais.put(t, t.isObrigatoria() ? Canal.AMBOS : canal(usuarioId, t)));
        return new Preferencias(canais, resumoDiario(usuarioId));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:36
     * Cartão Jira: KAN-146
     * Título: Salvar preferências
     * Objetivo: Gravar o canal escolhido para cada tipo e o resumo diário.
     * Decisões: Mudar um aviso obrigatório é recusado; os não informados mantêm o que estava.
     */
    @Transactional
    public Preferencias salvarPreferencias(UUID usuarioId, Map<TipoDeNotificacao, Canal> canais, boolean resumoDiario) {
        canais.forEach((tipo, canal) -> {
            if (tipo.isObrigatoria() && canal != Canal.AMBOS) {
                throw new RegraDeNegocioException("aviso_obrigatorio",
                        "\"" + tipo.getDescricao() + "\" é um aviso obrigatório e não pode ser desligado.");
            }
        });
        canais.forEach((tipo, canal) -> {
            if (!tipo.isObrigatoria()) {
                jdbc.update("""
                        INSERT INTO preferencia_notificacao (usuario_id, tipo, canal) VALUES (?, ?, ?)
                        ON CONFLICT (usuario_id, tipo) DO UPDATE SET canal = EXCLUDED.canal""",
                        usuarioId, tipo.name(), canal.name());
            }
        });
        jdbc.update("""
                INSERT INTO preferencia_resumo (usuario_id, resumo_diario) VALUES (?, ?)
                ON CONFLICT (usuario_id) DO UPDATE SET resumo_diario = EXCLUDED.resumo_diario""", usuarioId, resumoDiario);
        return preferencias(usuarioId);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:36
     * Cartão Jira: KAN-146
     * Título: Canal escolhido
     * Objetivo: Ler o canal que a pessoa escolheu para o tipo, ou o padrão.
     * Decisões: Uma consulta pela chave primária.
     */
    private Canal canal(UUID usuarioId, TipoDeNotificacao tipo) {
        List<String> escolhido = jdbc.queryForList(
                "SELECT canal FROM preferencia_notificacao WHERE usuario_id = ? AND tipo = ?", String.class,
                usuarioId, tipo.name());
        return escolhido.isEmpty() ? tipo.getPadrao() : Canal.valueOf(escolhido.get(0));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:36
     * Cartão Jira: KAN-146
     * Título: Quer resumo diário
     * Objetivo: Dizer se a pessoa prefere receber os e-mails não obrigatórios num resumo por dia.
     * Decisões: Padrão: avisos imediatos.
     */
    private boolean resumoDiario(UUID usuarioId) {
        List<Boolean> resumo = jdbc.queryForList(
                "SELECT resumo_diario FROM preferencia_resumo WHERE usuario_id = ?", Boolean.class, usuarioId);
        return !resumo.isEmpty() && Boolean.TRUE.equals(resumo.get(0));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:36
     * Cartão Jira: KAN-146
     * Título: Preferências
     * Objetivo: Levar o canal de cada tipo e o resumo diário.
     * Decisões: Mapa com todos os tipos.
     */
    public record Preferencias(Map<TipoDeNotificacao, Canal> canais, boolean resumoDiario) {
    }
}
