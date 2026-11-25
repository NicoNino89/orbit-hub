package br.com.orbithub.atendimento.service;

import br.com.orbithub.administracao.domain.ItemDeLista;
import br.com.orbithub.administracao.domain.Lista;
import br.com.orbithub.administracao.service.ListasService;
import br.com.orbithub.compartilhado.convites.TokensDeConvite;
import br.com.orbithub.compartilhado.email.EnvioDeEmail;
import br.com.orbithub.compartilhado.erro.RecursoNaoEncontradoException;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.compartilhado.parametros.LeituraDeParametros;
import br.com.orbithub.compartilhado.tempo.DiasUteis;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.service.ConsultaDeContas;
import br.com.orbithub.identidade.service.ContaProperties;
import br.com.orbithub.notificacoes.domain.TipoDeNotificacao;
import br.com.orbithub.notificacoes.service.NotificacoesService;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 16:19
 * Cartão Jira: KAN-88, KAN-89, KAN-103
 * Título: Central de atendimento
 * Objetivo: Publicar o catálogo de serviços e a central de ajuda; abrir chamados de quem tem conta (formulário) e de
 *           quem não tem (formulário público com confirmação por e-mail); conduzir o ciclo aberto → em atendimento →
 *           aguardando usuário → resolvido → fechado, com níveis N1, N2 e especialistas, prazos, respostas padrão,
 *           notas internas e avaliação; e mediar disputas entre usuários, ouvindo as duas partes.
 * Decisões: O nível inicial vem do serviço (acesso, titularidade, cadastro assistido e disputas vão direto ao N2). O
 *           prazo é o da primeira resposta no N1 e o do nível nos demais; horas úteis contadas em dias úteis de 24 h.
 *           A outra parte da disputa vê o chamado e responde, sem ver as notas internas. A plataforma media e registra
 *           e não arbitra: sem acordo, a participação na rodada fica "em disputa" e não conta como investimento. O
 *           atendimento não tem acesso ao Nível 2 (KAN-80).
 */
@Service
public class AtendimentoService {

    static final String SLA_N1 = "suporte.sla_primeira_resposta_horas";
    static final String SLA_N2 = "suporte.sla_n2_dias_uteis";
    static final String PRAZO_AGUARDANDO = "suporte.prazo_aguardando_dias";
    static final String SLA_DISPUTA = "disputa.sla_dias_uteis";
    private static final Set<String> FECHADOS = Set.of("FECHADO", "AGUARDANDO_CONFIRMACAO");

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-88
     * Título: Catálogo de serviços
     * Objetivo: Descrever cada serviço da plataforma: para quem, quem presta, o canal e o parâmetro do prazo.
     * Decisões: Os textos ficam aqui; os prazos vêm dos parâmetros mantidos pelo administrador (KAN-78).
     */
    public enum Servico {
        ONBOARDING("Onboarding e orientação de uso", "Todos", "Atendimento N1", "Central de ajuda e chamado", SLA_N1, "horas úteis", Nivel.N1),
        VERIFICACAO("Verificação de identidade e de empresa", "Investidores, organizações, fundadores e startups", "Time de verificação",
                "Fluxo de verificação", "verificacao.sla_dias_uteis", "dias úteis", Nivel.N1),
        CURADORIA("Curadoria da startup", "Empreendedores", "Time de curadoria", "Fluxo de publicação", "curadoria.sla_dias_uteis", "dias úteis", Nivel.N1),
        EDICOES("Inclusão em edições temáticas", "Empreendedores", "Curadoria", "Automático, com opção de não participar", null, "Por edição", Nivel.N1),
        CADASTRO_ASSISTIDO("Cadastro assistido de organização", "Fundos, redes e instituições", "Atendimento N2", "Convite institucional", SLA_N2,
                "dias úteis", Nivel.N2),
        ACESSO_E_TITULARIDADE("Recuperação de acesso e transferência de titularidade", "Todos", "Atendimento N2", "Chamado", SLA_N2, "dias úteis", Nivel.N2),
        DISPUTAS("Mediação de disputas", "Investidores e empreendedores", "Atendimento N2, com o jurídico", "Chamado", SLA_DISPUTA, "dias úteis", Nivel.N2),
        DENUNCIAS("Análise de denúncias", "Todos", "Moderação", "Botão Denunciar", null, "Por prioridade", Nivel.ESPECIALISTA),
        RECURSOS("Recursos", "Todos", "Mesmo time, outra pessoa", "Fluxo de recurso", "recurso.sla_dias_uteis", "dias úteis", Nivel.N1),
        LGPD("Direitos do titular (LGPD)", "Todos", "Encarregado de dados", "Canal de privacidade", "lgpd.prazo_resposta_dias", "dias", Nivel.ESPECIALISTA);

        private final String nome;
        private final String publico;
        private final String prestador;
        private final String canal;
        private final String parametro;
        private final String unidade;
        private final Nivel nivel;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: Servico
     * Objetivo: Rotina auxiliar Servico.
     * Decisões: Ver a documentação da classe.
     */
        Servico(String nome, String publico, String prestador, String canal, String parametro, String unidade, Nivel nivel) {
            this.nome = nome;
            this.publico = publico;
            this.prestador = prestador;
            this.canal = canal;
            this.parametro = parametro;
            this.unidade = unidade;
            this.nivel = nivel;
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-89
     * Título: Níveis de atendimento
     * Objetivo: Enumerar os níveis e os papéis que atendem cada um.
     * Decisões: O administrador vê todos; o N2 vê também o N1 (para assumir casos que o N1 não resolveu).
     */
    public enum Nivel {
        N1(EnumSet.of(Papel.SUPORTE_N1, Papel.SUPORTE_N2, Papel.ADMIN)),
        N2(EnumSet.of(Papel.SUPORTE_N2, Papel.ADMIN)),
        ESPECIALISTA(EnumSet.of(Papel.SUPORTE_N2, Papel.ADMIN, Papel.MODERADOR, Papel.ENCARREGADO_DADOS, Papel.JURIDICO));

        private final Set<Papel> papeis;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: Nivel
     * Objetivo: Rotina auxiliar Nivel.
     * Decisões: Ver a documentação da classe.
     */
        Nivel(Set<Papel> papeis) {
            this.papeis = papeis;
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 16:19
         * Cartão Jira: KAN-89
         * Título: Atende o nível
         * Objetivo: Dizer se algum dos papéis atende o nível.
         * Decisões: Basta um papel.
         */
        public boolean atende(Set<Papel> deQuem) {
            return deQuem.stream().anyMatch(papeis::contains);
        }
    }

    private final JdbcTemplate jdbc;
    private final ListasService listas;
    private final LeituraDeParametros parametros;
    private final NotificacoesService notificacoes;
    private final ConsultaDeContas contas;
    private final EnvioDeEmail email;
    private final ContaProperties conta;
    private final Clock relogio;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-89
     * Título: Criação
     * Objetivo: Receber os serviços envolvidos.
     * Decisões: Injeção por construtor.
     */
    @SuppressWarnings("java:S107")
    public AtendimentoService(JdbcTemplate jdbc, ListasService listas, LeituraDeParametros parametros, NotificacoesService notificacoes,
                              ConsultaDeContas contas, EnvioDeEmail email, ContaProperties conta, Clock relogio) {
        this.jdbc = jdbc;
        this.listas = listas;
        this.parametros = parametros;
        this.notificacoes = notificacoes;
        this.contas = contas;
        this.email = email;
        this.conta = conta;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-88
     * Título: Serviços
     * Objetivo: Devolver o catálogo de serviços com o prazo atual de cada um.
     * Decisões: Sem parâmetro, o prazo é o texto fixo (por edição, por prioridade).
     */
    @Transactional(readOnly = true)
    public List<ServicoDaPlataforma> servicos() {
        return Arrays.stream(Servico.values()).map(s -> new ServicoDaPlataforma(s, s.nome, s.publico, s.prestador, s.canal,
                s.parametro == null ? s.unidade : parametros.inteiro(s.parametro) + " " + s.unidade)).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-89
     * Título: Artigos da central de ajuda
     * Objetivo: Listar os artigos (só os publicados para o público; todos para a equipe).
     * Decisões: Ordem alfabética.
     */
    @Transactional(readOnly = true)
    public List<Artigo> artigos(boolean todos) {
        return jdbc.query("SELECT id, titulo, conteudo, publicado, atualizado_em FROM artigo_ajuda" + (todos ? "" : " WHERE publicado") + " ORDER BY titulo",
                (rs, i) -> new Artigo(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3), rs.getBoolean(4), rs.getTimestamp(5).toInstant()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-89
     * Título: Salvar artigo
     * Objetivo: Criar ou alterar um artigo da central de ajuda.
     * Decisões: Mantido pelo time de atendimento; título até 160 e texto até 8.000 caracteres.
     */
    @Transactional
    public Artigo salvarArtigo(UUID eu, UUID id, String titulo, String conteudo, Boolean publicado) {
        texto(titulo, 160, "titulo_invalido", "Dê um título com até 160 caracteres.");
        texto(conteudo, 8000, "conteudo_invalido", "Escreva o artigo com até 8.000 caracteres.");
        Instant agora = relogio.instant();
        UUID alvo = id == null ? UUID.randomUUID() : id;
        int n = id == null ? 0 : jdbc.update("UPDATE artigo_ajuda SET titulo = ?, conteudo = ?, publicado = ?, atualizado_por = ?, atualizado_em = ? WHERE id = ?",
                titulo.strip(), conteudo.strip(), Boolean.TRUE.equals(publicado), eu, Timestamp.from(agora), id);
        if (id != null && n == 0) {
            throw new RecursoNaoEncontradoException("Artigo não encontrado.");
        }
        if (id == null) {
            jdbc.update("INSERT INTO artigo_ajuda (id, titulo, conteudo, publicado, atualizado_por, atualizado_em) VALUES (?, ?, ?, ?, ?, ?)", alvo,
                    titulo.strip(), conteudo.strip(), Boolean.TRUE.equals(publicado), eu, Timestamp.from(agora));
        }
        return artigos(true).stream().filter(a -> a.id().equals(alvo)).findFirst().orElseThrow();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-89
     * Título: Respostas padrão
     * Objetivo: Listar as respostas padrão do atendimento.
     * Decisões: Ordem alfabética.
     */
    @Transactional(readOnly = true)
    public List<RespostaPadrao> respostas() {
        return jdbc.query("SELECT id, titulo, texto FROM resposta_padrao ORDER BY titulo",
                (rs, i) -> new RespostaPadrao(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3)));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-89
     * Título: Salvar resposta padrão
     * Objetivo: Criar ou alterar uma resposta padrão.
     * Decisões: Só o líder do time (N2 ou administrador), conferido no controlador.
     */
    @Transactional
    public RespostaPadrao salvarResposta(UUID eu, UUID id, String titulo, String texto) {
        texto(titulo, 160, "titulo_invalido", "Dê um título com até 160 caracteres.");
        texto(texto, 4000, "texto_invalido", "Escreva a resposta com até 4.000 caracteres.");
        Instant agora = relogio.instant();
        UUID alvo = id == null ? UUID.randomUUID() : id;
        if (id == null) {
            jdbc.update("INSERT INTO resposta_padrao (id, titulo, texto, atualizado_por, atualizado_em) VALUES (?, ?, ?, ?, ?)", alvo, titulo.strip(),
                    texto.strip(), eu, Timestamp.from(agora));
        } else if (jdbc.update("UPDATE resposta_padrao SET titulo = ?, texto = ?, atualizado_por = ?, atualizado_em = ? WHERE id = ?", titulo.strip(),
                texto.strip(), eu, Timestamp.from(agora), id) == 0) {
            throw new RecursoNaoEncontradoException("Resposta não encontrada.");
        }
        return new RespostaPadrao(alvo, titulo.strip(), texto.strip());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-89, KAN-103
     * Título: Abrir chamado
     * Objetivo: Registrar o chamado de quem tem conta, com categoria, serviço, descrição e anexos, no nível e com o prazo
     *           do serviço; em disputa, avisar a outra parte.
     * Decisões: Disputa exige a outra parte (uma conta ativa diferente) e vai ao N2 com o prazo de disputa. Sobre a
     *           participação na rodada, a outra parte vem do registro: o investidor, se quem abre é da startup; o fundador
     *           administrador que registrou o resultado, se quem abre é o investidor.
     */
    @Transactional
    public Chamado abrir(UUID eu, NovoChamado n) {
        if (n == null) {
            throw new RegraDeNegocioException("chamado_invalido", "Preencha o chamado.");
        }
        ItemDeLista categoria = listas.exigirAtivo(Lista.CATEGORIA_CHAMADO, n.categoria());
        boolean disputa = "DISPUTA".equals(categoria.getCodigo());
        Servico servico = disputa ? Servico.DISPUTAS : n.servico() == null ? Servico.ONBOARDING : n.servico();
        UUID outraParte = disputa && n.outraParteId() == null ? outraParteDoRegistro(eu, n.referenciaTipo(), n.referenciaId()) : n.outraParteId();
        if (disputa && (outraParte == null || outraParte.equals(eu)
                || (!contas.temPapelAtivo(outraParte, Papel.INVESTIDOR) && !contas.temPapelAtivo(outraParte, Papel.EMPREENDEDOR)))) {
            throw new RegraDeNegocioException("outra_parte_invalida", "Indique a outra parte da disputa.");
        }
        UUID id = inserir(eu, contas.emailParaAviso(eu).orElse(""), contas.nome(eu).orElse(""), categoria.getCodigo(), servico, n.descricao(),
                n.anexos(), disputa ? outraParte : null, n.referenciaTipo(), n.referenciaId(), null, "ABERTO");
        avisarEquipe(id, servico.nivel, "Novo chamado");
        if (disputa) {
            notificacoes.notificar(outraParte, TipoDeNotificacao.ATENDIMENTO, "Disputa aberta com você",
                    "Uma disputa foi aberta sobre um registro de que você participa. Conte a sua versão no chamado.", "/ajuda/chamados/" + id);
        }
        return ler(eu, Set.of(), id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-89
     * Título: Abrir chamado sem conta
     * Objetivo: Receber o chamado de quem não consegue entrar e mandar o link de confirmação ao e-mail informado.
     * Decisões: O chamado só entra na fila depois da confirmação (identidade mínima: posse do e-mail). A resposta é a
     *           mesma para qualquer e-mail, para não revelar quem tem conta.
     */
    @Transactional
    public void abrirPublico(NovoChamadoPublico n) {
        if (n == null || !TokensDeConvite.emailValido(n.email())) {
            throw new RegraDeNegocioException("email_invalido", "Informe um e-mail válido para receber a resposta.");
        }
        texto(n.nome(), 120, "nome_invalido", "Informe o seu nome.");
        ItemDeLista categoria = listas.exigirAtivo(Lista.CATEGORIA_CHAMADO, n.categoria());
        if ("DISPUTA".equals(categoria.getCodigo())) {
            throw new RegraDeNegocioException("categoria_invalida", "Disputas são abertas por quem está na plataforma.");
        }
        String token = TokensDeConvite.novoToken();
        Servico servico = "ACESSO".equals(categoria.getCodigo()) ? Servico.ACESSO_E_TITULARIDADE : Servico.ONBOARDING;
        inserir(null, n.email().strip().toLowerCase(), n.nome().strip(), categoria.getCodigo(), servico, n.descricao(), null, null, null, null,
                TokensDeConvite.hash(token), "AGUARDANDO_CONFIRMACAO");
        email.enviar(new EnvioDeEmail.MensagemEmail(n.email().strip(), "Confirme o seu chamado no Orbit Hub", "Olá, " + n.nome().strip()
                + ".\n\nRecebemos o seu pedido de ajuda. Para que ele entre na fila do atendimento, confirme o e-mail pelo link abaixo:\n\n"
                + conta.webUrl() + "/ajuda/confirmar?token=" + token + "\n\nSe você não pediu ajuda, ignore este e-mail.", "chamado_publico"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-89
     * Título: Confirmar chamado sem conta
     * Objetivo: Pôr na fila o chamado cujo e-mail foi confirmado e devolver o protocolo.
     * Decisões: O link vale uma vez; o prazo conta da confirmação.
     */
    @Transactional
    public UUID confirmarPublico(String token) {
        if (token == null || token.isBlank()) {
            throw new RegraDeNegocioException("link_invalido", "O link de confirmação é inválido.");
        }
        List<Map<String, Object>> l = jdbc.queryForList("SELECT id, servico FROM chamado WHERE token_confirmacao_hash = ? AND situacao = 'AGUARDANDO_CONFIRMACAO'",
                TokensDeConvite.hash(token));
        if (l.isEmpty()) {
            throw new RegraDeNegocioException("link_invalido", "O link de confirmação é inválido ou já foi usado.");
        }
        UUID id = (UUID) l.get(0).get("id");
        Servico servico = Servico.valueOf((String) l.get(0).get("servico"));
        Instant agora = relogio.instant();
        jdbc.update("UPDATE chamado SET situacao = 'ABERTO', token_confirmacao_hash = NULL, criado_em = ?, atualizado_em = ?, prazo_primeira_resposta = ? "
                + "WHERE id = ?", Timestamp.from(agora), Timestamp.from(agora), Timestamp.from(prazo(servico, servico.nivel, agora)), id);
        avisarEquipe(id, servico.nivel, "Novo chamado (sem conta)");
        return id;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-89, KAN-103
     * Título: Meus chamados
     * Objetivo: Listar os chamados abertos pela pessoa e as disputas em que ela é a outra parte.
     * Decisões: Mais recentes primeiro.
     */
    @Transactional(readOnly = true)
    public List<Resumo> meus(UUID eu) {
        return jdbc.query("SELECT id, categoria, servico, situacao, nivel, criado_em, atualizado_em, outra_parte_id = ? FROM chamado WHERE (usuario_id = ? "
                + "OR outra_parte_id = ?) AND situacao <> 'AGUARDANDO_CONFIRMACAO' ORDER BY criado_em DESC", (rs, i) -> resumo(rs), eu, eu, eu);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-89
     * Título: Fila do atendimento
     * Objetivo: Listar os chamados em aberto dos níveis que a pessoa atende, pelo prazo.
     * Decisões: Fechados e resolvidos ficam de fora.
     */
    @Transactional(readOnly = true)
    public List<ItemDaFila> fila(Set<Papel> papeis) {
        Instant agora = relogio.instant();
        return jdbc.query("SELECT id, categoria, servico, situacao, nivel, prioridade, prazo_primeira_resposta, responsavel_id, nome_contato, criado_em, "
                + "primeira_resposta_em FROM chamado WHERE situacao IN ('ABERTO', 'EM_ATENDIMENTO', 'AGUARDANDO_USUARIO') "
                + "ORDER BY CASE prioridade WHEN 'ALTA' THEN 0 ELSE 1 END, prazo_primeira_resposta", (rs, i) -> {
                    UUID resp = rs.getObject(8, UUID.class);
                    Instant prazo = rs.getTimestamp(7).toInstant();
                    return new ItemDaFila(rs.getObject(1, UUID.class), listas.rotulo(Lista.CATEGORIA_CHAMADO, rs.getString(2)), Servico.valueOf(rs.getString(3)),
                            rs.getString(4), Nivel.valueOf(rs.getString(5)), rs.getString(6), prazo,
                            prazo.isBefore(agora) && (rs.getTimestamp(11) == null || !Nivel.N1.name().equals(rs.getString(5))),
                            resp == null ? null : contas.nome(resp).orElse(null), rs.getString(9), rs.getTimestamp(10).toInstant());
                }).stream().filter(c -> c.nivel().atende(papeis)).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-89, KAN-103
     * Título: Ler chamado
     * Objetivo: Mostrar o chamado com as mensagens a quem abriu, à outra parte da disputa ou à equipe do nível.
     * Decisões: Notas internas só para a equipe.
     */
    @Transactional(readOnly = true)
    public Chamado ler(UUID eu, Set<Papel> papeis, UUID id) {
        Map<String, Object> c = linha(id);
        boolean equipe = Nivel.valueOf((String) c.get("nivel")).atende(papeis) || papeis.contains(Papel.ADMIN);
        boolean autor = eu.equals(c.get("usuario_id"));
        boolean outraParte = eu.equals(c.get("outra_parte_id"));
        if (!equipe && !autor && !outraParte) {
            throw new AccessDeniedException("Chamado de outra pessoa");
        }
        List<Mensagem> mensagens = jdbc.query("SELECT autor_id, equipe, interna, texto, em FROM chamado_mensagem WHERE chamado_id = ?"
                + (equipe ? "" : " AND NOT interna") + " ORDER BY em", (rs, i) -> {
                    UUID quem = rs.getObject(1, UUID.class);
                    boolean daEquipe = rs.getBoolean(2);
                    String nome = daEquipe ? (equipe && quem != null ? contas.nome(quem).orElse("Atendimento") : "Atendimento Orbit Hub")
                            : quem == null ? (String) c.get("nome_contato") : contas.nome(quem).orElse(null);
                    return new Mensagem(nome, daEquipe, rs.getBoolean(3), rs.getString(4), rs.getTimestamp(5).toInstant(), eu.equals(quem));
                }, id);
        UUID resp = (UUID) c.get("responsavel_id");
        UUID parte = (UUID) c.get("outra_parte_id");
        return new Chamado(id, listas.rotulo(Lista.CATEGORIA_CHAMADO, (String) c.get("categoria")), (String) c.get("categoria"),
                Servico.valueOf((String) c.get("servico")), (String) c.get("descricao"), (String) c.get("situacao"), Nivel.valueOf((String) c.get("nivel")),
                (String) c.get("prioridade"), instante(c.get("prazo_primeira_resposta")), (String) c.get("nome_contato"),
                equipe ? (String) c.get("email_contato") : null, resp == null ? null : contas.nome(resp).orElse(null),
                parte == null ? null : contas.nome(parte).orElse(null), outraParte, (String) c.get("referencia_tipo"), (UUID) c.get("referencia_id"),
                (String) c.get("resultado_disputa"), (Integer) c.get("nota"), (String) c.get("comentario_avaliacao"),
                equipe ? (String) c.get("escalonamento") : null, mensagens, instante(c.get("criado_em")));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-89, KAN-103
     * Título: Responder como usuário
     * Objetivo: Acrescentar a mensagem de quem abriu (ou da outra parte da disputa) e devolver o chamado ao atendimento.
     * Decisões: Chamado fechado não recebe mensagens; abra outro.
     */
    @Transactional
    public Chamado responder(UUID eu, UUID id, String texto) {
        Map<String, Object> c = linha(id);
        if (!eu.equals(c.get("usuario_id")) && !eu.equals(c.get("outra_parte_id"))) {
            throw new AccessDeniedException("Chamado de outra pessoa");
        }
        aberto(c);
        mensagem(id, eu, false, false, texto);
        jdbc.update("UPDATE chamado SET situacao = CASE WHEN situacao IN ('AGUARDANDO_USUARIO', 'RESOLVIDO') THEN 'EM_ATENDIMENTO' ELSE situacao END, "
                + "aguardando_desde = NULL, atualizado_em = ? WHERE id = ?", Timestamp.from(relogio.instant()), id);
        UUID resp = (UUID) c.get("responsavel_id");
        if (resp != null) {
            notificacoes.notificar(resp, TipoDeNotificacao.EQUIPE_ENCAMINHAMENTO, "Resposta no chamado", "O usuário respondeu ao chamado.",
                    "/admin/atendimento/" + id);
        }
        return ler(eu, Set.of(), id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-89
     * Título: Avaliar o atendimento
     * Objetivo: Registrar a nota (1 a 5) e o comentário de quem abriu e fechar o chamado resolvido.
     * Decisões: Uma avaliação por chamado.
     */
    @Transactional
    public Chamado avaliar(UUID eu, UUID id, Integer nota, String comentario) {
        Map<String, Object> c = linha(id);
        if (!eu.equals(c.get("usuario_id"))) {
            throw new AccessDeniedException("Chamado de outra pessoa");
        }
        if (!Set.of("RESOLVIDO", "FECHADO").contains(c.get("situacao")) || c.get("nota") != null) {
            throw new RegraDeNegocioException("avaliacao_indisponivel", "Avalie o atendimento depois que o chamado for resolvido, uma vez.");
        }
        if (nota == null || nota < 1 || nota > 5) {
            throw new RegraDeNegocioException("nota_invalida", "Dê uma nota de 1 a 5.");
        }
        jdbc.update("UPDATE chamado SET nota = ?, comentario_avaliacao = ?, situacao = 'FECHADO', atualizado_em = ? WHERE id = ?", nota,
                comentario == null || comentario.isBlank() ? null : comentario.strip().substring(0, Math.min(1000, comentario.strip().length())),
                Timestamp.from(relogio.instant()), id);
        return ler(eu, Set.of(), id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-89
     * Título: Assumir chamado
     * Objetivo: Pôr o chamado em atendimento com a pessoa da equipe.
     * Decisões: Outra pessoa do nível pode assumir no lugar.
     */
    @Transactional
    public Chamado assumir(UUID eu, Set<Papel> papeis, UUID id) {
        Map<String, Object> c = daEquipe(papeis, id);
        aberto(c);
        jdbc.update("UPDATE chamado SET responsavel_id = ?, situacao = CASE WHEN situacao = 'ABERTO' THEN 'EM_ATENDIMENTO' ELSE situacao END, "
                + "atualizado_em = ? WHERE id = ?", eu, Timestamp.from(relogio.instant()), id);
        return ler(eu, papeis, id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-89
     * Título: Responder como equipe
     * Objetivo: Acrescentar a resposta (ou nota interna), marcar a primeira resposta e, se pedido, esperar o usuário.
     * Decisões: Nota interna não muda a situação nem avisa o usuário. Chamado sem conta recebe a resposta por e-mail.
     */
    @Transactional
    public Chamado responderEquipe(UUID eu, Set<Papel> papeis, UUID id, String texto, boolean interna, boolean aguardarUsuario) {
        Map<String, Object> c = daEquipe(papeis, id);
        aberto(c);
        mensagem(id, eu, true, interna, texto);
        if (!interna) {
            Instant agora = relogio.instant();
            jdbc.update("UPDATE chamado SET primeira_resposta_em = COALESCE(primeira_resposta_em, ?), responsavel_id = COALESCE(responsavel_id, ?), "
                    + "situacao = ?, aguardando_desde = ?, atualizado_em = ? WHERE id = ?", Timestamp.from(agora), eu,
                    aguardarUsuario ? "AGUARDANDO_USUARIO" : "EM_ATENDIMENTO", aguardarUsuario ? Timestamp.from(agora) : null, Timestamp.from(agora), id);
            avisarUsuarios(c, "Resposta do atendimento", texto.strip());
        }
        return ler(eu, papeis, id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-89
     * Título: Escalar
     * Objetivo: Passar o chamado ao N2 ou a um especialista, registrando o motivo no chamado.
     * Decisões: Só para um nível acima; o prazo é recalculado e o responsável sai. Devolve o chamado como ficou, mesmo
     *           que quem escalou não atenda o novo nível.
     */
    @Transactional
    public Chamado escalar(UUID eu, Set<Papel> papeis, UUID id, Nivel para, String motivo) {
        Map<String, Object> c = daEquipe(papeis, id);
        aberto(c);
        Nivel atual = Nivel.valueOf((String) c.get("nivel"));
        if (para == null || para.ordinal() <= atual.ordinal()) {
            throw new RegraDeNegocioException("nivel_invalido", "Escale para um nível acima do atual.");
        }
        texto(motivo, 500, "motivo_obrigatorio", "Explique por que o chamado está sendo escalado.");
        Instant agora = relogio.instant();
        String historico = (c.get("escalonamento") == null ? "" : c.get("escalonamento") + " | ") + atual + " → " + para + ": " + motivo.strip();
        jdbc.update("UPDATE chamado SET nivel = ?, responsavel_id = NULL, situacao = 'ABERTO', prazo_primeira_resposta = ?, escalonamento = ?, atualizado_em = ? "
                + "WHERE id = ?", para.name(), Timestamp.from(prazo(Servico.valueOf((String) c.get("servico")), para, agora)),
                historico.length() > 500 ? historico.substring(historico.length() - 500) : historico, Timestamp.from(agora), id);
        mensagem(id, eu, true, true, "Escalado para " + para + ": " + motivo.strip());
        avisarEquipe(id, para, "Chamado escalado");
        return ler(eu, EnumSet.of(Papel.ADMIN), id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-89, KAN-103
     * Título: Resolver
     * Objetivo: Marcar o chamado como resolvido com a explicação e, em disputa, o resultado (acordo ou em disputa).
     * Decisões: Disputa sem acordo sobre participação na rodada marca a participação "em disputa", que não conta como
     *           investimento concretizado. Com acordo, as partes corrigem o registro (por exemplo, o investidor confirma).
     */
    @Transactional
    public Chamado resolver(UUID eu, Set<Papel> papeis, UUID id, String texto, String resultadoDisputa) {
        Map<String, Object> c = daEquipe(papeis, id);
        aberto(c);
        texto(texto, 4000, "texto_invalido", "Explique a solução ao usuário.");
        boolean disputa = "DISPUTA".equals(c.get("categoria"));
        if (disputa && (resultadoDisputa == null || !Set.of("ACORDO", "EM_DISPUTA").contains(resultadoDisputa))) {
            throw new RegraDeNegocioException("resultado_invalido", "Informe se houve acordo entre as partes ou se o registro fica em disputa.");
        }
        Instant agora = relogio.instant();
        mensagem(id, eu, true, false, texto);
        jdbc.update("UPDATE chamado SET situacao = 'RESOLVIDO', resultado_disputa = ?, resolvido_em = ?, primeira_resposta_em = COALESCE(primeira_resposta_em, ?), "
                + "responsavel_id = COALESCE(responsavel_id, ?), atualizado_em = ? WHERE id = ?", disputa ? resultadoDisputa : null, Timestamp.from(agora),
                Timestamp.from(agora), eu, Timestamp.from(agora), id);
        if (disputa && "EM_DISPUTA".equals(resultadoDisputa) && "RODADA_INVESTIMENTO".equals(c.get("referencia_tipo")) && c.get("referencia_id") != null) {
            jdbc.update("UPDATE rodada_investimento SET situacao = 'EM_DISPUTA' WHERE id = ?", c.get("referencia_id"));
        }
        avisarUsuarios(c, "Chamado resolvido", texto.strip());
        return ler(eu, papeis, id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-89
     * Título: Fechar chamados parados
     * Objetivo: Fechar os chamados que esperam o usuário (ou foram resolvidos) há mais de suporte.prazo_aguardando_dias.
     * Decisões: Rodado pela R20; o usuário é avisado.
     */
    @Transactional
    public int fecharParados() {
        Instant limite = relogio.instant().minus(Duration.ofDays(parametros.inteiro(PRAZO_AGUARDANDO)));
        List<Map<String, Object>> parados = jdbc.queryForList("SELECT * FROM chamado WHERE (situacao = 'AGUARDANDO_USUARIO' AND aguardando_desde < ?) "
                + "OR (situacao = 'RESOLVIDO' AND resolvido_em < ?)", Timestamp.from(limite), Timestamp.from(limite));
        for (Map<String, Object> c : parados) {
            jdbc.update("UPDATE chamado SET situacao = 'FECHADO', atualizado_em = ? WHERE id = ?", Timestamp.from(relogio.instant()), c.get("id"));
            if ("AGUARDANDO_USUARIO".equals(c.get("situacao"))) {
                avisarUsuarios(c, "Chamado fechado", "Fechamos o chamado porque não recebemos resposta. Se precisar, abra outro.");
            }
        }
        return parados.size();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-88, KAN-89
     * Título: Indicadores do atendimento
     * Objetivo: Medir, por serviço, o volume, o cumprimento do prazo e a satisfação média.
     * Decisões: Cumprido = primeira resposta (N1) ou resolução (demais) dentro do prazo.
     */
    @Transactional(readOnly = true)
    public List<Indicador> indicadores() {
        return jdbc.query("SELECT servico, count(*), count(*) FILTER (WHERE COALESCE(CASE WHEN nivel = 'N1' THEN primeira_resposta_em ELSE resolvido_em END, "
                + "now()) <= prazo_primeira_resposta), avg(nota), count(nota) FROM chamado WHERE situacao <> 'AGUARDANDO_CONFIRMACAO' GROUP BY servico ORDER BY servico",
                (rs, i) -> new Indicador(Servico.valueOf(rs.getString(1)), rs.getInt(2), rs.getInt(3), rs.getObject(4) == null ? null : rs.getDouble(4), rs.getInt(5)));
    }

    private UUID outraParteDoRegistro(UUID eu, String tipo, UUID id) {
        if (!"RODADA_INVESTIMENTO".equals(tipo) || id == null) {
            return null;
        }
        List<Map<String, Object>> l = jdbc.queryForList("SELECT p.pessoa_id, r.startup_id, r.criada_por FROM rodada_investimento i JOIN rodada_participante p "
                + "ON p.id = i.participante_id JOIN rodada r ON r.id = i.rodada_id WHERE i.id = ?", id);
        if (l.isEmpty()) {
            return null;
        }
        UUID investidor = (UUID) l.get(0).get("pessoa_id");
        if (!eu.equals(investidor)) {
            Integer fundador = jdbc.queryForObject("SELECT count(*) FROM startup_membro WHERE startup_id = ? AND usuario_id = ? AND saiu_em IS NULL "
                    + "AND papel IN ('FUNDADOR_ADMINISTRADOR', 'FUNDADOR')", Integer.class, l.get(0).get("startup_id"), eu);
            return fundador != null && fundador > 0 ? investidor : null;
        }
        return jdbc.queryForList("SELECT usuario_id FROM startup_membro WHERE startup_id = ? AND papel = 'FUNDADOR_ADMINISTRADOR' AND saiu_em IS NULL "
                + "ORDER BY entrou_em", UUID.class, l.get(0).get("startup_id")).stream().findFirst().orElse(null);
    }

    @SuppressWarnings("java:S107")
    private UUID inserir(UUID usuario, String emailContato, String nome, String categoria, Servico servico, String descricao, String anexos, UUID outraParte,
                         String referenciaTipo, UUID referenciaId, String tokenHash, String situacao) {
        texto(descricao, 4000, "descricao_invalida", "Descreva o que aconteceu com até 4.000 caracteres.");
        Instant agora = relogio.instant();
        UUID id = UUID.randomUUID();
        String texto = descricao.strip() + (anexos == null || anexos.isBlank() ? "" : "\n\nAnexos: " + anexos.strip());
        jdbc.update("INSERT INTO chamado (id, usuario_id, email_contato, nome_contato, categoria, servico, descricao, prioridade, nivel, situacao, "
                + "prazo_primeira_resposta, outra_parte_id, referencia_tipo, referencia_id, token_confirmacao_hash, criado_em, atualizado_em) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", id, usuario, emailContato, nome, categoria, servico.name(),
                texto.length() > 4000 ? texto.substring(0, 4000) : texto, Set.of("ACESSO", "DISPUTA").contains(categoria) ? "ALTA" : "NORMAL", servico.nivel.name(),
                situacao, Timestamp.from(prazo(servico, servico.nivel, agora)), outraParte, referenciaTipo == null || referenciaTipo.isBlank() ? null
                        : referenciaTipo.strip(), referenciaId, tokenHash, Timestamp.from(agora), Timestamp.from(agora));
        return id;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: prazo
     * Objetivo: Rotina auxiliar prazo.
     * Decisões: Ver a documentação da classe.
     */
    private Instant prazo(Servico servico, Nivel nivel, Instant agora) {
        if (servico == Servico.DISPUTAS) {
            return DiasUteis.somar(agora, parametros.inteiro(SLA_DISPUTA));
        }
        if (nivel == Nivel.N1) {
            long horas = parametros.inteiro(SLA_N1);
            return horas < 24 ? agora.plus(Duration.ofHours(horas)) : DiasUteis.somar(agora, (horas + 23) / 24);
        }
        return DiasUteis.somar(agora, parametros.inteiro(SLA_N2));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: mensagem
     * Objetivo: Rotina auxiliar mensagem.
     * Decisões: Ver a documentação da classe.
     */
    private void mensagem(UUID chamado, UUID autor, boolean equipe, boolean interna, String texto) {
        texto(texto, 4000, "mensagem_invalida", "Escreva a mensagem com até 4.000 caracteres.");
        jdbc.update("INSERT INTO chamado_mensagem (id, chamado_id, autor_id, equipe, interna, texto, em) VALUES (?, ?, ?, ?, ?, ?, ?)", UUID.randomUUID(),
                chamado, autor, equipe, interna, texto.strip(), Timestamp.from(relogio.instant()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: avisarUsuarios
     * Objetivo: Rotina auxiliar avisarUsuarios.
     * Decisões: Ver a documentação da classe.
     */
    private void avisarUsuarios(Map<String, Object> c, String titulo, String texto) {
        UUID id = (UUID) c.get("id");
        UUID usuario = (UUID) c.get("usuario_id");
        if (usuario != null) {
            notificacoes.notificar(usuario, TipoDeNotificacao.ATENDIMENTO, titulo, resumir(texto), "/ajuda/chamados/" + id);
        } else {
            email.enviar(new EnvioDeEmail.MensagemEmail((String) c.get("email_contato"), titulo + " — Orbit Hub", "Olá, " + c.get("nome_contato") + ".\n\n"
                    + texto + "\n\nResponda pelo formulário da central de ajuda informando o protocolo " + id.toString().substring(0, 8) + ".", "chamado_resposta"));
        }
        UUID parte = (UUID) c.get("outra_parte_id");
        if (parte != null) {
            notificacoes.notificar(parte, TipoDeNotificacao.ATENDIMENTO, titulo, resumir(texto), "/ajuda/chamados/" + id);
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: avisarEquipe
     * Objetivo: Rotina auxiliar avisarEquipe.
     * Decisões: Ver a documentação da classe.
     */
    private void avisarEquipe(UUID id, Nivel nivel, String titulo) {
        Set<Papel> papeis = nivel == Nivel.N1 ? Set.of(Papel.SUPORTE_N1) : nivel == Nivel.N2 ? Set.of(Papel.SUPORTE_N2) : Set.of(Papel.SUPORTE_N2, Papel.ADMIN);
        notificacoes.notificarTodos(contas.equipeAtivaComPapeis(papeis), TipoDeNotificacao.EQUIPE_ENCAMINHAMENTO, titulo, "Chamado no nível " + nivel + ".",
                "/admin/atendimento/" + id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: daEquipe
     * Objetivo: Rotina auxiliar daEquipe.
     * Decisões: Ver a documentação da classe.
     */
    private Map<String, Object> daEquipe(Set<Papel> papeis, UUID id) {
        Map<String, Object> c = linha(id);
        if (!Nivel.valueOf((String) c.get("nivel")).atende(papeis) && !papeis.contains(Papel.ADMIN)) {
            throw new AccessDeniedException("Nível de atendimento de outro time");
        }
        return c;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: aberto
     * Objetivo: Rotina auxiliar aberto.
     * Decisões: Ver a documentação da classe.
     */
    private static void aberto(Map<String, Object> c) {
        if (FECHADOS.contains(c.get("situacao"))) {
            throw new RegraDeNegocioException("chamado_fechado", "Este chamado está fechado. Se precisar, abra outro.");
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: linha
     * Objetivo: Rotina auxiliar linha.
     * Decisões: Ver a documentação da classe.
     */
    private Map<String, Object> linha(UUID id) {
        List<Map<String, Object>> l = jdbc.queryForList("SELECT * FROM chamado WHERE id = ?", id);
        if (l.isEmpty()) {
            throw new RecursoNaoEncontradoException("Chamado não encontrado.");
        }
        return l.get(0);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: resumo
     * Objetivo: Rotina auxiliar resumo.
     * Decisões: Ver a documentação da classe.
     */
    private Resumo resumo(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Resumo(rs.getObject(1, UUID.class), listas.rotulo(Lista.CATEGORIA_CHAMADO, rs.getString(2)), Servico.valueOf(rs.getString(3)),
                rs.getString(4), Nivel.valueOf(rs.getString(5)), rs.getTimestamp(6).toInstant(), rs.getTimestamp(7).toInstant(), rs.getBoolean(8));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: texto
     * Objetivo: Rotina auxiliar texto.
     * Decisões: Ver a documentação da classe.
     */
    private static void texto(String valor, int max, String codigo, String mensagem) {
        if (valor == null || valor.isBlank() || valor.strip().length() > max) {
            throw new RegraDeNegocioException(codigo, mensagem);
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: resumir
     * Objetivo: Rotina auxiliar resumir.
     * Decisões: Ver a documentação da classe.
     */
    private static String resumir(String texto) {
        return texto.length() > 200 ? texto.substring(0, 197) + "..." : texto;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: instante
     * Objetivo: Rotina auxiliar instante.
     * Decisões: Ver a documentação da classe.
     */
    private static Instant instante(Object o) {
        return o instanceof Timestamp t ? t.toInstant() : o instanceof java.time.OffsetDateTime d ? d.toInstant() : (Instant) o;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-88, KAN-89, KAN-103
     * Título: Dados do atendimento
     * Objetivo: Tipar os pedidos e as respostas da central de atendimento.
     * Decisões: O e-mail de contato só aparece para a equipe.
     */
    public record ServicoDaPlataforma(Servico codigo, String nome, String publico, String prestador, String canal, String prazo) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: Artigo
     * Objetivo: Rotina auxiliar Artigo.
     * Decisões: Ver a documentação da classe.
     */
    public record Artigo(UUID id, String titulo, String conteudo, boolean publicado, Instant atualizadoEm) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: RespostaPadrao
     * Objetivo: Rotina auxiliar RespostaPadrao.
     * Decisões: Ver a documentação da classe.
     */
    public record RespostaPadrao(UUID id, String titulo, String texto) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: NovoChamado
     * Objetivo: Rotina auxiliar NovoChamado.
     * Decisões: Ver a documentação da classe.
     */
    public record NovoChamado(String categoria, Servico servico, String descricao, String anexos, UUID outraParteId, String referenciaTipo, UUID referenciaId) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: NovoChamadoPublico
     * Objetivo: Rotina auxiliar NovoChamadoPublico.
     * Decisões: Ver a documentação da classe.
     */
    public record NovoChamadoPublico(String nome, String email, String categoria, String descricao) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: Resumo
     * Objetivo: Rotina auxiliar Resumo.
     * Decisões: Ver a documentação da classe.
     */
    public record Resumo(UUID id, String categoria, Servico servico, String situacao, Nivel nivel, Instant criadoEm, Instant atualizadoEm, boolean souOutraParte) {
    }

    public record ItemDaFila(UUID id, String categoria, Servico servico, String situacao, Nivel nivel, String prioridade, Instant prazo, boolean vencido,
                             String responsavel, String contato, Instant criadoEm) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: Mensagem
     * Objetivo: Rotina auxiliar Mensagem.
     * Decisões: Ver a documentação da classe.
     */
    public record Mensagem(String autor, boolean equipe, boolean interna, String texto, Instant em, boolean minha) {
    }

    public record Chamado(UUID id, String categoria, String categoriaCodigo, Servico servico, String descricao, String situacao, Nivel nivel, String prioridade,
                          Instant prazo, String contato, String emailContato, String responsavel, String outraParte, boolean souOutraParte,
                          String referenciaTipo, UUID referenciaId, String resultadoDisputa, Integer nota, String comentarioAvaliacao,
                          String escalonamento, List<Mensagem> mensagens, Instant criadoEm) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: Indicador
     * Objetivo: Rotina auxiliar Indicador.
     * Decisões: Ver a documentação da classe.
     */
    public record Indicador(Servico servico, int chamados, int noPrazo, Double satisfacao, int avaliacoes) {
    }
}
