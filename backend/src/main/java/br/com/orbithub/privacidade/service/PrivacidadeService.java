package br.com.orbithub.privacidade.service;

import br.com.orbithub.administracao.domain.AcaoGrave;
import br.com.orbithub.administracao.service.ConflitosDeInteresse;
import br.com.orbithub.administracao.service.SegundaAprovacaoService;
import br.com.orbithub.administracao.service.TimesService;
import br.com.orbithub.auditoria.service.ContextoDeAuditoria;
import br.com.orbithub.compartilhado.erro.RecursoNaoEncontradoException;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.compartilhado.parametros.LeituraDeParametros;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.domain.Usuario;
import br.com.orbithub.identidade.repository.UsuarioRepository;
import br.com.orbithub.identidade.service.ConsultaDeContas;
import br.com.orbithub.identidade.service.ExclusaoDeConta;
import br.com.orbithub.notificacoes.domain.TipoDeNotificacao;
import br.com.orbithub.notificacoes.service.NotificacoesService;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 20:54
 * Cartão Jira: KAN-90, KAN-17
 * Título: Direitos do titular de dados
 * Objetivo: Receber os pedidos do titular (confirmação, acesso, correção, portabilidade, anonimização, eliminação,
 *           compartilhamentos e revogação de consentimento) com a identidade confirmada pela senha; entregar na hora a
 *           cópia dos dados (acesso e portabilidade); levar os pedidos ao encarregado, com prazo lgpd.prazo_resposta_dias;
 *           e excluir a conta fora do fluxo normal só com segunda aprovação.
 * Decisões: Todo pedido e toda resposta ficam em pedido_titular. A cópia dos dados é um JSON com o que a plataforma guarda
 *           da pessoa (sem dados de terceiros). O pedido novo vai ao encarregado por rodízio (equipe.distribuicao_privacidade).
 */
@Service
public class PrivacidadeService {

    static final String PRAZO = "lgpd.prazo_resposta_dias";
    static final String PEDIDO = "PEDIDO_TITULAR";

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-90
     * Título: Tipos de pedido
     * Objetivo: Enumerar os direitos que o titular pode exercer pelo canal de privacidade.
     * Decisões: Os nomes da LGPD, art. 18.
     */
    public enum Tipo { CONFIRMACAO, ACESSO, CORRECAO, PORTABILIDADE, ANONIMIZACAO, ELIMINACAO, COMPARTILHAMENTOS, REVOGACAO_CONSENTIMENTO }

    private final JdbcTemplate jdbc;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder senhas;
    private final ConsultaDeContas contas;
    private final NotificacoesService notificacoes;
    private final LeituraDeParametros parametros;
    private final TimesService times;
    private final SegundaAprovacaoService aprovacoes;
    private final ExclusaoDeConta exclusao;
    private final Clock relogio;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-90
     * Título: Criação
     * Objetivo: Receber os serviços envolvidos.
     * Decisões: Injeção por construtor.
     */
    @SuppressWarnings("java:S107")
    public PrivacidadeService(JdbcTemplate jdbc, UsuarioRepository usuarios, PasswordEncoder senhas, ConsultaDeContas contas, NotificacoesService notificacoes,
                              LeituraDeParametros parametros, TimesService times, SegundaAprovacaoService aprovacoes, ExclusaoDeConta exclusao,
                              Clock relogio) {
        this.jdbc = jdbc;
        this.usuarios = usuarios;
        this.senhas = senhas;
        this.contas = contas;
        this.notificacoes = notificacoes;
        this.parametros = parametros;
        this.times = times;
        this.aprovacoes = aprovacoes;
        this.exclusao = exclusao;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-90
     * Título: Fazer pedido
     * Objetivo: Registrar o pedido do titular depois de confirmar a identidade pela senha e avisar o encarregado.
     * Decisões: Um pedido aberto por tipo. Eliminação também pode ser feita direto em Conta (KAN-17).
     */
    @Transactional
    public Pedido pedir(UUID eu, Tipo tipo, String detalhes, String senha) {
        confirmarIdentidade(eu, senha);
        if (tipo == null) {
            throw new RegraDeNegocioException("tipo_invalido", "Escolha o que você quer pedir.");
        }
        Integer aberto = jdbc.queryForObject("SELECT count(*) FROM pedido_titular WHERE usuario_id = ? AND tipo = ? AND situacao IN ('ABERTO', 'EM_ANALISE')",
                Integer.class, eu, tipo.name());
        if (aberto != null && aberto > 0) {
            throw new RegraDeNegocioException("pedido_em_aberto", "Você já tem um pedido deste tipo em análise.");
        }
        Instant agora = relogio.instant();
        UUID id = UUID.randomUUID();
        UUID responsavel = times.distribuir(TimesService.Time.PRIVACIDADE, Set.of(Papel.ENCARREGADO_DADOS), PEDIDO, id,
                new ConflitosDeInteresse.Envolvidos(Set.of(), Set.of(eu))).orElse(null);
        jdbc.update("INSERT INTO pedido_titular (id, usuario_id, tipo, detalhes, situacao, prazo, responsavel_id, criado_em) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                id, eu, tipo.name(), detalhes == null || detalhes.isBlank() ? null : detalhes.strip().substring(0, Math.min(2000, detalhes.strip().length())),
                responsavel == null ? "ABERTO" : "EM_ANALISE", Timestamp.from(agora.plus(Duration.ofDays(parametros.inteiro(PRAZO)))), responsavel,
                Timestamp.from(agora));
        notificacoes.notificarTodos(responsavel != null ? List.of(responsavel) : contas.equipeAtivaComPapeis(Set.of(Papel.ENCARREGADO_DADOS)),
                TipoDeNotificacao.EQUIPE_ENCAMINHAMENTO, "Novo pedido do titular", "Pedido de " + tipo.name().toLowerCase().replace('_', ' ') + ".",
                "/admin/privacidade");
        return ler(id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-90
     * Título: Meus pedidos
     * Objetivo: Listar os pedidos do titular com a resposta.
     * Decisões: Mais recentes primeiro.
     */
    @Transactional(readOnly = true)
    public List<Pedido> meus(UUID eu) {
        return jdbc.queryForList("SELECT id FROM pedido_titular WHERE usuario_id = ? ORDER BY criado_em DESC", UUID.class, eu).stream().map(this::ler).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-90
     * Título: Cópia dos meus dados
     * Objetivo: Montar, com a identidade confirmada, a cópia dos dados pessoais que a plataforma guarda da pessoa.
     * Decisões: Acesso e portabilidade no fluxo normal (sem esperar o encarregado), registrados como pedido concluído.
     *           Formato JSON, legível por máquina.
     */
    @Transactional
    public Map<String, Object> meusDados(UUID eu, String senha) {
        confirmarIdentidade(eu, senha);
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("conta", jdbc.queryForList("SELECT nome, email, telefone, status, criado_em, ultimo_acesso_em FROM usuario WHERE id = ?", eu));
        dados.put("papeis", jdbc.queryForList("SELECT papel FROM usuario_papel WHERE usuario_id = ?", String.class, eu));
        dados.put("termos_aceitos", jdbc.queryForList("SELECT d.tipo, d.versao, a.aceito_em FROM aceite_documento a JOIN documento_legal d ON d.id = a.documento_id "
                + "WHERE a.usuario_id = ?", eu));
        dados.put("perfil_de_investimento", jdbc.queryForList("SELECT * FROM perfil_investimento WHERE pessoa_id = ?", eu));
        dados.put("organizacoes", jdbc.queryForList("SELECT g.nome, m.papel, m.entrou_em, m.saiu_em FROM organizacao_membro m JOIN organizacao g "
                + "ON g.id = m.organizacao_id WHERE m.usuario_id = ?", eu));
        dados.put("startups", jdbc.queryForList("SELECT s.nome, m.papel, m.entrou_em, m.saiu_em FROM startup_membro m JOIN startup s ON s.id = m.startup_id "
                + "WHERE m.usuario_id = ?", eu));
        dados.put("conexoes", jdbc.queryForList("SELECT s.nome AS startup, c.situacao, c.criada_em FROM conexao c JOIN startup s ON s.id = c.startup_id "
                + "WHERE c.pessoa_id = ?", eu));
        dados.put("reunioes", jdbc.queryForList("SELECT r.pauta, r.inicio, r.situacao FROM reuniao_participante p JOIN reuniao r ON r.id = p.reuniao_id "
                + "WHERE p.usuario_id = ?", eu));
        dados.put("favoritos", jdbc.queryForList("SELECT s.nome FROM catalogo_favorito f JOIN startup s ON s.id = f.startup_id WHERE f.usuario_id = ?",
                String.class, eu));
        dados.put("visualizacoes", jdbc.queryForList("SELECT s.nome, v.vista_em FROM catalogo_visualizacao v JOIN startup s ON s.id = v.startup_id "
                + "WHERE v.usuario_id = ?", eu));
        dados.put("chamados", jdbc.queryForList("SELECT categoria, descricao, situacao, criado_em FROM chamado WHERE usuario_id = ?", eu));
        dados.put("denuncias_feitas", jdbc.queryForList("SELECT categoria, descricao, criada_em FROM denuncia WHERE denunciante_id = ?", eu));
        dados.put("pedidos_de_privacidade", jdbc.queryForList("SELECT tipo, situacao, criado_em, respondido_em FROM pedido_titular WHERE usuario_id = ?", eu));
        Instant agora = relogio.instant();
        jdbc.update("INSERT INTO pedido_titular (id, usuario_id, tipo, situacao, prazo, resposta, respondido_em, criado_em) VALUES (?, ?, 'PORTABILIDADE', "
                + "'CONCLUIDO', ?, 'Cópia dos dados entregue pela própria conta.', ?, ?)", UUID.randomUUID(), eu, Timestamp.from(agora), Timestamp.from(agora),
                Timestamp.from(agora));
        dados.put("gerado_em", agora.toString());
        return dados;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-90
     * Título: Fila do encarregado
     * Objetivo: Listar os pedidos em aberto pelo prazo.
     * Decisões: Inclui os já atribuídos a outra pessoa do time.
     */
    @Transactional(readOnly = true)
    public List<Pedido> fila() {
        return jdbc.queryForList("SELECT id FROM pedido_titular WHERE situacao IN ('ABERTO', 'EM_ANALISE', 'AGUARDANDO_APROVACAO') ORDER BY prazo", UUID.class)
                .stream().map(this::ler).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-90
     * Título: Responder
     * Objetivo: Registrar a resposta (atendido ou recusado com o motivo) e avisar o titular.
     * Decisões: A resposta fica registrada e aparece para o titular.
     */
    @Transactional
    public Pedido responder(UUID eu, UUID id, String resposta, boolean recusar) {
        Map<String, Object> p = aberto(id);
        if (resposta == null || resposta.isBlank()) {
            throw new RegraDeNegocioException("resposta_obrigatoria", "Escreva a resposta ao titular.");
        }
        Instant agora = relogio.instant();
        jdbc.update("UPDATE pedido_titular SET situacao = ?, resposta = ?, respondido_por = ?, respondido_em = ?, responsavel_id = COALESCE(responsavel_id, ?) "
                + "WHERE id = ?", recusar ? "RECUSADO" : "CONCLUIDO", resposta.strip(), eu, Timestamp.from(agora), eu, id);
        notificacoes.notificar((UUID) p.get("usuario_id"), TipoDeNotificacao.SEGURANCA_CONTA, "Resposta ao seu pedido de privacidade",
                resposta.strip().length() > 200 ? resposta.strip().substring(0, 197) + "..." : resposta.strip(), "/conta");
        return ler(id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-90, KAN-81
     * Título: Propor exclusão fora do fluxo
     * Objetivo: Levar à segunda aprovação a exclusão da conta do titular feita pelo encarregado (fora do fluxo normal).
     * Decisões: O pedido fica aguardando a aprovação; aprovado, a conta é excluída e o pedido concluído.
     */
    @Transactional
    public Pedido proporExclusao(UUID eu, UUID id, String justificativa) {
        Map<String, Object> p = aberto(id);
        UUID titular = (UUID) p.get("usuario_id");
        aprovacoes.propor(AcaoGrave.DADOS_PESSOAIS_FORA_DO_FLUXO, PEDIDO, id.toString(), "Excluir a conta de " + contas.nome(titular).orElse("titular"),
                Map.of("acao", "EXCLUSAO", "usuarioId", titular.toString()), eu, justificativa);
        jdbc.update("UPDATE pedido_titular SET situacao = 'AGUARDANDO_APROVACAO', responsavel_id = COALESCE(responsavel_id, ?) WHERE id = ?", eu, id);
        return ler(id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-90, KAN-81
     * Título: Concluir exclusão aprovada
     * Objetivo: Excluir a conta depois da segunda aprovação e concluir o pedido.
     * Decisões: Chamado pelo executor da ação grave.
     */
    @Transactional
    public void concluirExclusao(UUID pedido, UUID aprovador) {
        Map<String, Object> p = jdbc.queryForMap("SELECT usuario_id FROM pedido_titular WHERE id = ?", pedido);
        exclusao.excluirPorDecisao((UUID) p.get("usuario_id"));
        jdbc.update("UPDATE pedido_titular SET situacao = 'CONCLUIDO', resposta = 'Conta excluída após segunda aprovação.', respondido_por = ?, "
                + "respondido_em = ? WHERE id = ?", aprovador, Timestamp.from(relogio.instant()), pedido);
        ContextoDeAuditoria.descrever("EXCLUSAO_FORA_DO_FLUXO", PEDIDO, pedido, "Conta excluída");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-90, KAN-81
     * Título: Exclusão não aprovada
     * Objetivo: Devolver o pedido à análise quando a exclusão é rejeitada ou cancelada.
     * Decisões: O encarregado responde de outro jeito.
     */
    @Transactional
    public void desistirDaExclusao(UUID pedido) {
        jdbc.update("UPDATE pedido_titular SET situacao = 'EM_ANALISE' WHERE id = ? AND situacao = 'AGUARDANDO_APROVACAO'", pedido);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-17, KAN-76, KAN-77, KAN-82, KAN-90, KAN-91, KAN-93, KAN-107, KAN-108
     * Título: confirmarIdentidade
     * Objetivo: Rotina auxiliar confirmarIdentidade.
     * Decisões: Ver a documentação da classe.
     */
    private void confirmarIdentidade(UUID eu, String senha) {
        Usuario u = usuarios.findById(eu).orElseThrow(() -> new RecursoNaoEncontradoException("Conta não encontrada."));
        if (senha == null || !senhas.matches(senha, u.getSenhaHash())) {
            throw new RegraDeNegocioException("senha_incorreta", "Confirme a sua identidade com a senha da conta.");
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-17, KAN-76, KAN-77, KAN-82, KAN-90, KAN-91, KAN-93, KAN-107, KAN-108
     * Título: aberto
     * Objetivo: Rotina auxiliar aberto.
     * Decisões: Ver a documentação da classe.
     */
    private Map<String, Object> aberto(UUID id) {
        List<Map<String, Object>> l = jdbc.queryForList("SELECT * FROM pedido_titular WHERE id = ?", id);
        if (l.isEmpty()) {
            throw new RecursoNaoEncontradoException("Pedido não encontrado.");
        }
        if (!Set.of("ABERTO", "EM_ANALISE").contains((String) l.get(0).get("situacao"))) {
            throw new RegraDeNegocioException("pedido_encerrado", "Este pedido já foi respondido ou aguarda aprovação.");
        }
        return l.get(0);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-17, KAN-76, KAN-77, KAN-82, KAN-90, KAN-91, KAN-93, KAN-107, KAN-108
     * Título: ler
     * Objetivo: Rotina auxiliar ler.
     * Decisões: Ver a documentação da classe.
     */
    private Pedido ler(UUID id) {
        Map<String, Object> p = jdbc.queryForMap("SELECT * FROM pedido_titular WHERE id = ?", id);
        UUID titular = (UUID) p.get("usuario_id");
        UUID resp = (UUID) p.get("responsavel_id");
        Instant prazo = ((Timestamp) p.get("prazo")).toInstant();
        return new Pedido(id, Tipo.valueOf((String) p.get("tipo")), contas.nome(titular).orElse(null),
                (String) p.get("detalhes"), (String) p.get("situacao"), prazo, prazo.isBefore(relogio.instant()) && Set.of("ABERTO", "EM_ANALISE")
                .contains((String) p.get("situacao")), resp == null ? null : contas.nome(resp).orElse(null), (String) p.get("resposta"),
                p.get("respondido_em") == null ? null : ((Timestamp) p.get("respondido_em")).toInstant(), ((Timestamp) p.get("criado_em")).toInstant());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-90
     * Título: Pedido do titular
     * Objetivo: Tipar o pedido para as telas.
     * Decisões: Vencido só enquanto aberto.
     */
    public record Pedido(UUID id, Tipo tipo, String titular, String detalhes, String situacao, Instant prazo, boolean vencido, String responsavel,
                         String resposta, Instant respondidoEm, Instant criadoEm) {
    }
}
