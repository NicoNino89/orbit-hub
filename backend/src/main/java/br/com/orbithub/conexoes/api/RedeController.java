package br.com.orbithub.conexoes.api;

import br.com.orbithub.compartilhado.seguranca.Autenticado;
import br.com.orbithub.conexoes.service.MensagensDaConexao;
import br.com.orbithub.investidores.service.AtuacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 22:58
 * Cartão Jira: KAN-174, KAN-176
 * Título: Rede, mensagens e carteira
 * Objetivo: Expor a conversa da conexão, o contador de não lidas e a carteira do investidor (investimentos registrados e
 *           rodadas de que participa).
 * Decisões: A carteira mostra o valor só ao próprio investidor (ou à organização em nome da qual atua); participação em
 *           disputa ou não confirmada aparece marcada e não soma no total (a soma é feita na tela, só com CONFIRMADO).
 */
@RestController
@Tag(name = "Rede", description = "Mensagens da conexão e carteira")
class RedeController {

    private final MensagensDaConexao mensagens;
    private final AtuacaoService atuacao;
    private final JdbcTemplate jdbc;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:58
     * Cartão Jira: KAN-176
     * Título: Criação
     * Objetivo: Receber os serviços.
     * Decisões: Injeção por construtor.
     */
    RedeController(MensagensDaConexao mensagens, AtuacaoService atuacao, JdbcTemplate jdbc) {
        this.mensagens = mensagens;
        this.atuacao = atuacao;
        this.jdbc = jdbc;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:58
     * Cartão Jira: KAN-176
     * Título: Conversa
     * Objetivo: Ler a conversa da conexão.
     * Decisões: Marca a leitura.
     */
    @GetMapping("/api/v1/conexoes/{id}/mensagens")
    @Operation(summary = "Conversa da conexão")
    MensagensDaConexao.Conversa conversa(@PathVariable UUID id, @AuthenticationPrincipal Jwt token,
                                         @RequestHeader(name = AtuacaoService.CABECALHO, required = false) String organizacao) {
        return mensagens.conversa(Autenticado.id(token), organizacao, id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:58
     * Cartão Jira: KAN-176
     * Título: Enviar
     * Objetivo: Enviar uma mensagem na conexão.
     * Decisões: Só com a conexão aberta.
     */
    @PostMapping("/api/v1/conexoes/{id}/mensagens")
    @Operation(summary = "Envia mensagem na conexão")
    MensagensDaConexao.Conversa enviar(@PathVariable UUID id, @RequestBody Map<String, String> corpo, @AuthenticationPrincipal Jwt token,
                                       @RequestHeader(name = AtuacaoService.CABECALHO, required = false) String organizacao) {
        return mensagens.enviar(Autenticado.id(token), organizacao, id, corpo.get("texto"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:58
     * Cartão Jira: KAN-176
     * Título: Não lidas
     * Objetivo: Contar as mensagens não lidas.
     * Decisões: Para o contador do menu.
     */
    @GetMapping("/api/v1/mensagens/contagem")
    @Operation(summary = "Mensagens não lidas")
    Map<String, Integer> naoLidas(@AuthenticationPrincipal Jwt token) {
        return Map.of("naoLidas", mensagens.naoLidas(Autenticado.id(token)));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:58
     * Cartão Jira: KAN-174, KAN-64
     * Título: Carteira
     * Objetivo: Listar os investimentos registrados e as rodadas de que o investidor participa.
     * Decisões: Pela pessoa ou pela organização da atuação.
     */
    @GetMapping("/api/v1/investidores/carteira")
    @PreAuthorize("hasRole('INVESTIDOR')")
    @Operation(summary = "Carteira do investidor")
    @Transactional(readOnly = true)
    Carteira carteira(@AuthenticationPrincipal Jwt token, @RequestHeader(name = AtuacaoService.CABECALHO, required = false) String organizacao) {
        AtuacaoService.Atuacao a = atuacao.paraVer(Autenticado.id(token), organizacao);
        String filtro = a.organizacao() != null ? "p.organizacao_id = ?" : "p.pessoa_id = ? AND p.organizacao_id IS NULL";
        UUID chave = a.organizacao() != null ? a.organizacao() : a.pessoa();
        List<Investimento> investimentos = jdbc.query("SELECT s.id, s.nome_aprovado, r.data_fechamento, r.instrumento_utilizado, i.valor, i.situacao "
                + "FROM rodada_investimento i JOIN rodada_participante p ON p.id = i.participante_id JOIN rodada r ON r.id = i.rodada_id "
                + "JOIN startup s ON s.id = r.startup_id WHERE " + filtro + " ORDER BY r.data_fechamento DESC NULLS LAST", (rs, i) -> new Investimento(
                rs.getObject(1, UUID.class), rs.getString(2), rs.getDate(3) == null ? null : rs.getDate(3).toLocalDate(), rs.getString(4),
                rs.getBigDecimal(5), rs.getString(6)), chave);
        List<Participacao> rodadas = jdbc.query("SELECT s.id, s.nome_aprovado, r.situacao, r.prazo, p.intencao, p.entrou_em FROM rodada_participante p "
                + "JOIN rodada r ON r.id = p.rodada_id JOIN startup s ON s.id = r.startup_id WHERE " + filtro + " AND p.situacao <> 'SAIU' "
                + "AND r.situacao <> 'ENCERRADA' ORDER BY p.entrou_em DESC", (rs, i) -> new Participacao(rs.getObject(1, UUID.class), rs.getString(2),
                rs.getString(3), rs.getDate(4) == null ? null : rs.getDate(4).toLocalDate(), rs.getBigDecimal(5), rs.getTimestamp(6).toInstant()), chave);
        return new Carteira(investimentos, rodadas);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:58
     * Cartão Jira: KAN-174
     * Título: Dados da carteira
     * Objetivo: Tipar a carteira.
     * Decisões: Valores só para o próprio investidor.
     */
    record Investimento(UUID startupId, String startup, LocalDate data, String instrumento, BigDecimal valor, String situacao) {
    }

    record Participacao(UUID startupId, String startup, String situacao, LocalDate prazo, BigDecimal intencao, Instant entrouEm) {
    }

    record Carteira(List<Investimento> investimentos, List<Participacao> rodadas) {
    }
}
