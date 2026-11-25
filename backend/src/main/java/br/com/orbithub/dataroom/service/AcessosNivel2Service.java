package br.com.orbithub.dataroom.service;

import br.com.orbithub.administracao.domain.DocumentoLegal;
import br.com.orbithub.administracao.domain.TipoDocumento;
import br.com.orbithub.administracao.service.AceiteService;
import br.com.orbithub.administracao.service.DocumentoLegalService;
import br.com.orbithub.catalogo.service.ApresentacaoService;
import br.com.orbithub.compartilhado.erro.RecursoNaoEncontradoException;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.compartilhado.parametros.LeituraDeParametros;
import br.com.orbithub.conexoes.domain.Conexao;
import br.com.orbithub.conexoes.domain.ConexaoFinalizada;
import br.com.orbithub.conexoes.service.ConexoesService;
import br.com.orbithub.empreendimentos.domain.InvestidorBloqueado;
import br.com.orbithub.empreendimentos.domain.PapelNaStartup;
import br.com.orbithub.empreendimentos.domain.RegraDeConfidencialidade;
import br.com.orbithub.empreendimentos.domain.SituacaoDaStartup;
import br.com.orbithub.empreendimentos.domain.Startup;
import br.com.orbithub.empreendimentos.service.BloqueiosDaStartup;
import br.com.orbithub.empreendimentos.service.StartupsService;
import br.com.orbithub.identidade.service.ConsultaDeContas;
import br.com.orbithub.investidores.service.AtuacaoService;
import br.com.orbithub.investidores.service.OrganizacoesService;
import br.com.orbithub.notificacoes.domain.TipoDeNotificacao;
import br.com.orbithub.notificacoes.service.NotificacoesService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 12:04
 * Cartão Jira: KAN-30, KAN-31, KAN-32, KAN-33, KAN-63, KAN-105
 * Título: Acesso às informações confidenciais (Nível 2)
 * Objetivo: Deixar o fundador administrador liberar, renovar e revogar o Nível 2 para um investidor com conexão aberta,
 *           conferindo a regra de confidencialidade da startup; mostrar o Nível 2 ao investidor autorizado, com links
 *           temporários dos documentos; registrar todo acesso num histórico imutável; e encerrar o acesso sozinho no
 *           vencimento, no fim da conexão e no bloqueio.
 * Decisões: Liberação individual por conexão (pessoa ou organização), com validade nivel2.validade_dias por padrão (7 a
 *           365). Termo da plataforma: a pessoa (ou um administrador da organização) aceitou a versão vigente; versão nova
 *           exige novo aceite antes do próximo acesso. NDA bilateral: o NDA da startup confirmado pelas duas partes. O
 *           acesso é conferido de novo a cada leitura (prazo, conexão aberta, bloqueio, termo). Histórico só para os
 *           fundadores. Links valem dataroom.link_minutos e só para quem os gerou.
 */
@Service
public class AcessosNivel2Service {

    static final String VALIDADE = "nivel2.validade_dias";
    static final String AVISO = "nivel2.aviso_dias";
    static final String LINK = "dataroom.link_minutos";
    static final Set<Conexao.Situacao> ABERTAS = EnumSet.of(Conexao.Situacao.MATCH, Conexao.Situacao.EM_ANALISE, Conexao.Situacao.EM_NEGOCIACAO);
    static final SecureRandom ALEATORIO = new SecureRandom();

    private final JdbcTemplate jdbc;
    private final StartupsService startups;
    private final ConexoesService conexoes;
    private final BloqueiosDaStartup bloqueios;
    private final AtuacaoService atuacao;
    private final OrganizacoesService organizacoes;
    private final AceiteService aceites;
    private final DocumentoLegalService documentosLegais;
    private final DataRoomService dataRoom;
    private final ApresentacaoService apresentacao;
    private final NotificacoesService notificacoes;
    private final ConsultaDeContas contas;
    private final LeituraDeParametros parametros;
    private final Clock relogio;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30
     * Título: Criação
     * Objetivo: Receber os serviços dos módulos envolvidos.
     * Decisões: Injeção por construtor.
     */
    @SuppressWarnings("java:S107")
    public AcessosNivel2Service(JdbcTemplate jdbc, StartupsService startups, ConexoesService conexoes, BloqueiosDaStartup bloqueios,
                                AtuacaoService atuacao, OrganizacoesService organizacoes, AceiteService aceites,
                                DocumentoLegalService documentosLegais, DataRoomService dataRoom, ApresentacaoService apresentacao,
                                NotificacoesService notificacoes, ConsultaDeContas contas, LeituraDeParametros parametros, Clock relogio) {
        this.jdbc = jdbc;
        this.startups = startups;
        this.conexoes = conexoes;
        this.bloqueios = bloqueios;
        this.atuacao = atuacao;
        this.organizacoes = organizacoes;
        this.aceites = aceites;
        this.documentosLegais = documentosLegais;
        this.dataRoom = dataRoom;
        this.apresentacao = apresentacao;
        this.notificacoes = notificacoes;
        this.contas = contas;
        this.parametros = parametros;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30, KAN-31, KAN-63
     * Título: Painel de acessos da startup
     * Objetivo: Mostrar a regra de confidencialidade, se há NDA enviado e as conexões abertas com selo, requisitos
     *           cumpridos e o acesso ativo de cada uma.
     * Decisões: Fundadores (não colaboradores).
     */
    @Transactional(readOnly = true)
    public Painel painel(UUID eu, UUID startupId) {
        exigirFundador(eu, startupId);
        Startup s = startups.ler(startupId);
        List<Candidato> candidatos = conexoes.abertasDaStartup(startupId).stream().map(c -> {
            Optional<Acesso> ativo = ativoDaConexao(c.getId());
            return new Candidato(c.getId(), conexoes.investidor(c), c.getSituacao(), conexoes.investidorVerificado(c), termoAceito(c),
                    ndaConfirmado(c.getId(), Conexao.Lado.INVESTIDOR), ndaConfirmado(c.getId(), Conexao.Lado.STARTUP),
                    requisitoFalta(s, c), ativo.orElse(null));
        }).toList();
        return new Painel(s.getRegraConfidencialidade(), dataRoom.nda(startupId).isPresent(), parametros.inteiro(VALIDADE), candidatos);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-63
     * Título: Definir regra
     * Objetivo: Escolher entre o termo da plataforma e o NDA bilateral.
     * Decisões: Só o fundador administrador; não mexe nas liberações já feitas.
     */
    @Transactional
    public RegraDeConfidencialidade definirRegra(UUID eu, UUID startupId, RegraDeConfidencialidade regra) {
        startups.exigirAdministrador(eu, startupId);
        startups.ler(startupId).definirRegraConfidencialidade(regra, relogio.instant());
        return regra;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30
     * Título: Liberar
     * Objetivo: Liberar o Nível 2 para o investidor de uma conexão aberta, com validade.
     * Decisões: Só o fundador administrador; recusa sem o requisito da regra, com bloqueio ou com acesso já ativo.
     */
    @Transactional
    public Acesso liberar(UUID eu, UUID startupId, UUID conexaoId, Integer dias) {
        startups.exigirAdministrador(eu, startupId);
        Conexao c = conexaoDaStartup(startupId, conexaoId);
        if (!ABERTAS.contains(c.getSituacao())) {
            throw new RegraDeNegocioException("conexao_fechada", "Libere só para conexões em Match, em análise ou em negociação.");
        }
        if (bloqueios.bloqueia(startupId, c.getPessoaId(), c.getOrganizacaoId())) {
            throw new RegraDeNegocioException("investidor_bloqueado", "O investidor está bloqueado pela startup.");
        }
        String falta = requisitoFalta(startups.ler(startupId), c);
        if (falta != null) {
            throw new RegraDeNegocioException("requisito_pendente", falta);
        }
        if (ativoDaConexao(conexaoId).isPresent()) {
            throw new RegraDeNegocioException("acesso_ativo", "Este investidor já tem acesso. Renove, se quiser outro prazo.");
        }
        Instant agora = relogio.instant();
        UUID id = UUID.randomUUID();
        Instant ate = agora.plus(Duration.ofDays(dias(dias)));
        jdbc.update("INSERT INTO acesso_nivel2 (id, startup_id, conexao_id, investidor_chave, liberado_por, liberado_em, valido_ate) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)", id, startupId, conexaoId, c.getInvestidorChave(), eu, Timestamp.from(agora), Timestamp.from(ate));
        evento(startupId, id, c.getInvestidorChave(), "LIBERACAO", null, eu, "até " + ate);
        notificacoes.notificar(c.getPessoaId(), TipoDeNotificacao.NIVEL2_LIBERADO, "Informações confidenciais liberadas",
                "A " + startups.ler(startupId).getNome() + " liberou o acesso às informações confidenciais.", "/catalogo/startups/" + startupId);
        return acesso(id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30
     * Título: Renovar
     * Objetivo: Dar novo prazo a um acesso ativo.
     * Decisões: Conta a partir de agora; zera o aviso de vencimento.
     */
    @Transactional
    public Acesso renovar(UUID eu, UUID startupId, UUID acessoId, Integer dias) {
        startups.exigirAdministrador(eu, startupId);
        Acesso a = ativoDaStartup(startupId, acessoId);
        Instant ate = relogio.instant().plus(Duration.ofDays(dias(dias)));
        jdbc.update("UPDATE acesso_nivel2 SET valido_ate = ?, avisado_em = NULL WHERE id = ?", Timestamp.from(ate), acessoId);
        evento(startupId, acessoId, a.investidorChave(), "RENOVACAO", null, eu, "até " + ate);
        return acesso(acessoId);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-31
     * Título: Revogar
     * Objetivo: Tirar o acesso na hora.
     * Decisões: Só o fundador administrador; avisa o investidor.
     */
    @Transactional
    public void revogar(UUID eu, UUID startupId, UUID acessoId) {
        startups.exigirAdministrador(eu, startupId);
        Acesso a = ativoDaStartup(startupId, acessoId);
        encerrar(a, "REVOGADO", eu);
        notificacoes.notificar(conexoes.ler(a.conexaoId()).getPessoaId(), TipoDeNotificacao.NIVEL2_REVOGADO, "Acesso confidencial encerrado",
                "A " + startups.ler(startupId).getNome() + " encerrou o seu acesso às informações confidenciais.", "/conexoes");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-33
     * Título: Histórico de acessos
     * Objetivo: Listar liberações, renovações, revogações, encerramentos, visualizações e downloads.
     * Decisões: Só os fundadores; só leitura.
     */
    @Transactional(readOnly = true)
    public List<Evento> historico(UUID eu, UUID startupId) {
        exigirFundador(eu, startupId);
        return jdbc.query("SELECT e.tipo, e.investidor_chave, e.por, e.detalhe, e.em, d.nome FROM acesso_nivel2_evento e "
                + "LEFT JOIN dataroom_documento d ON d.id = e.documento_id WHERE e.startup_id = ? ORDER BY e.em DESC", (rs, i) -> new Evento(
                rs.getString(1), nomeDaChave(rs.getObject(2, UUID.class)),
                rs.getObject(3, UUID.class) == null ? null : contas.nome(rs.getObject(3, UUID.class)).orElse(null), rs.getString(4),
                rs.getTimestamp(5).toInstant(), rs.getString(6)), startupId);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-63
     * Título: Confirmar NDA
     * Objetivo: Registrar que uma das partes da conexão confirmou o NDA bilateral da startup.
     * Decisões: O lado sai de quem chama (fundador ou investidor). Exige a regra NDA e o NDA enviado.
     */
    @Transactional
    public void confirmarNda(UUID eu, String organizacao, UUID conexaoId) {
        Conexao c = conexoes.ler(conexaoId);
        Startup s = startups.ler(c.getStartupId());
        if (s.getRegraConfidencialidade() != RegraDeConfidencialidade.NDA_BILATERAL || dataRoom.nda(s.getId()).isEmpty()) {
            throw new RegraDeNegocioException("sem_nda", "Esta startup não usa NDA bilateral ou ainda não enviou o NDA.");
        }
        Conexao.Lado lado;
        if (startups.fundadores(s.getId(), false).contains(eu)) {
            lado = Conexao.Lado.STARTUP;
        } else if (chave(atuacao.paraAgir(eu, organizacao)).equals(c.getInvestidorChave())) {
            lado = Conexao.Lado.INVESTIDOR;
        } else {
            throw new RecursoNaoEncontradoException("Conexão não encontrada.");
        }
        int n = jdbc.update("INSERT INTO nda_confirmacao (conexao_id, lado, confirmado_por, confirmado_em) VALUES (?, ?, ?, ?) ON CONFLICT DO NOTHING",
                conexaoId, lado.name(), eu, Timestamp.from(relogio.instant()));
        if (n > 0) {
            evento(s.getId(), null, c.getInvestidorChave(), "NDA_CONFIRMADO", null, eu, lado.name());
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-63
     * Título: Aceitar o termo pela organização
     * Objetivo: Registrar o aceite da versão vigente do termo de confidencialidade em nome da organização.
     * Decisões: Só um administrador da organização.
     */
    @Transactional
    public void aceitarTermoDaOrganizacao(UUID eu, UUID organizacaoId) {
        if (!organizacoes.administradoresDe(organizacaoId).contains(eu)) {
            throw new AccessDeniedException("Só um administrador da organização aceita o termo em nome dela.");
        }
        DocumentoLegal termo = documentosLegais.vigente(TipoDocumento.TERMO_DE_CONFIDENCIALIDADE)
                .orElseThrow(() -> new RegraDeNegocioException("sem_termo", "Não há termo de confidencialidade vigente."));
        jdbc.update("INSERT INTO organizacao_aceite_termo (organizacao_id, documento_id, aceito_por, aceito_em) VALUES (?, ?, ?, ?) "
                + "ON CONFLICT DO NOTHING", organizacaoId, termo.getId(), eu, Timestamp.from(relogio.instant()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-32, KAN-63
     * Título: Situação para o investidor
     * Objetivo: Dizer ao investidor se tem o Nível 2 da startup, até quando, e o que falta (termo ou NDA).
     * Decisões: Sem conexão aberta, só diz que não tem acesso.
     */
    @Transactional(readOnly = true)
    public ParaInvestidor situacao(UUID eu, String organizacao, UUID startupId) {
        AtuacaoService.Atuacao a = atuacao.paraVer(eu, organizacao);
        Startup s = publicada(startupId, a);
        Optional<Conexao> c = conexoes.abertasDaStartup(startupId).stream().filter(x -> x.getInvestidorChave().equals(chave(a))).findFirst();
        Optional<Acesso> ativo = c.flatMap(x -> ativoDaConexao(x.getId())).filter(x -> x.validoAte().isAfter(relogio.instant()));
        boolean termo = c.map(this::termoAceito).orElse(false);
        return new ParaInvestidor(ativo.isPresent() && (s.getRegraConfidencialidade() != RegraDeConfidencialidade.TERMO_PLATAFORMA || termo),
                ativo.map(Acesso::validoAte).orElse(null), s.getRegraConfidencialidade(), termo, c.map(Conexao::getId).orElse(null),
                c.isPresent() && dataRoom.nda(startupId).isPresent(), c.map(x -> ndaConfirmado(x.getId(), Conexao.Lado.INVESTIDOR)).orElse(false));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-32, KAN-33
     * Título: Ver o Nível 2
     * Objetivo: Mostrar ao investidor autorizado as informações confidenciais e os documentos atuais, registrando a
     *           visualização.
     * Decisões: Confere tudo de novo a cada leitura.
     */
    @Transactional
    public Conteudo conteudo(UUID eu, String organizacao, UUID startupId) {
        Acesso a = exigirAcesso(eu, organizacao, startupId);
        evento(startupId, a.id(), a.investidorChave(), "VISUALIZACAO", null, eu, null);
        return new Conteudo(apresentacao.confidencial(startupId), dataRoom.atuais(startupId).stream()
                .map(d -> new DocumentoVisivel(d.id(), d.categoriaRotulo(), d.nome(), d.descricao(), d.tipo(), d.tamanho(), d.versao(), d.enviadoEm()))
                .toList(), a.validoAte());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-145
     * Título: Gerar link temporário
     * Objetivo: Criar um link de uso pessoal e curto para baixar um documento atual.
     * Decisões: Token aleatório de 32 bytes; só o hash fica no banco.
     */
    @Transactional
    public Link gerarLink(UUID eu, String organizacao, UUID startupId, UUID documentoId) {
        Acesso a = exigirAcesso(eu, organizacao, startupId);
        DataRoomService.Documento d = dataRoom.documento(startupId, documentoId);
        if (!d.atual() || DataRoomService.NDA.equals(d.categoria())) {
            throw new RecursoNaoEncontradoException("Documento não encontrado.");
        }
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expira = relogio.instant().plus(Duration.ofMinutes(parametros.inteiro(LINK)));
        jdbc.update("INSERT INTO dataroom_link (token_hash, documento_id, acesso_id, usuario_id, expira_em) VALUES (?, ?, ?, ?, ?)",
                hash(token), documentoId, a.id(), eu, Timestamp.from(expira));
        return new Link(token, expira);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-145, KAN-33
     * Título: Baixar pelo link
     * Objetivo: Entregar o arquivo do link válido de quem o gerou, registrando o download.
     * Decisões: O acesso é conferido de novo (pode ter sido revogado depois do link).
     */
    @Transactional
    public DataRoomService.Arquivo baixar(UUID eu, String organizacao, String token) {
        List<Map<String, Object>> linhas = jdbc.queryForList("SELECT l.documento_id, l.acesso_id, a.startup_id FROM dataroom_link l "
                + "JOIN acesso_nivel2 a ON a.id = l.acesso_id WHERE l.token_hash = ? AND l.usuario_id = ? AND l.expira_em > ?", hash(token), eu,
                Timestamp.from(relogio.instant()));
        if (linhas.isEmpty()) {
            throw new RecursoNaoEncontradoException("Link vencido ou inválido. Gere outro.");
        }
        UUID startupId = (UUID) linhas.get(0).get("startup_id");
        Acesso a = exigirAcesso(eu, organizacao, startupId);
        DataRoomService.Documento d = dataRoom.documento(startupId, (UUID) linhas.get(0).get("documento_id"));
        evento(startupId, a.id(), a.investidorChave(), "DOWNLOAD", d.id(), eu, null);
        return dataRoom.abrir(d);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-63
     * Título: Baixar o NDA
     * Objetivo: Entregar o NDA bilateral ao investidor com conexão aberta, para ler antes de confirmar.
     * Decisões: Não exige o Nível 2.
     */
    @Transactional(readOnly = true)
    public DataRoomService.Arquivo baixarNda(UUID eu, String organizacao, UUID startupId) {
        AtuacaoService.Atuacao a = atuacao.paraVer(eu, organizacao);
        publicada(startupId, a);
        if (conexoes.abertasDaStartup(startupId).stream().noneMatch(x -> x.getInvestidorChave().equals(chave(a)))) {
            throw new RecursoNaoEncontradoException("Documento não encontrado.");
        }
        return dataRoom.abrir(dataRoom.nda(startupId).orElseThrow(() -> new RecursoNaoEncontradoException("Documento não encontrado.")));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30, KAN-49
     * Título: Ao finalizar a conexão
     * Objetivo: Encerrar o acesso da conexão que chegou a um estado final.
     * Decisões: Mesma transação da mudança da conexão.
     */
    @EventListener
    public void aoFinalizarConexao(ConexaoFinalizada evento) {
        ativoDaConexao(evento.conexaoId()).ifPresent(a -> encerrar(a, "CONEXAO_FINAL", null));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30, KAN-67
     * Título: Ao bloquear
     * Objetivo: Encerrar na hora os acessos do investidor bloqueado.
     * Decisões: Pessoa bloqueada perde também os acessos que recebeu atuando por uma organização.
     */
    @EventListener
    public void aoBloquear(InvestidorBloqueado evento) {
        jdbc.query("SELECT a.id FROM acesso_nivel2 a JOIN conexao c ON c.id = a.conexao_id WHERE a.startup_id = ? AND a.encerrado_em IS NULL "
                        + "AND (a.investidor_chave = ? OR (? = 'PESSOA' AND c.pessoa_id = ?))", (rs, i) -> rs.getObject(1, UUID.class),
                evento.startupId(), evento.alvoId(), evento.alvoTipo(), evento.alvoId()).forEach(id -> encerrar(acesso(id), "BLOQUEIO", null));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-105
     * Título: Expirar vencidos (R3)
     * Objetivo: Encerrar os acessos com o prazo vencido e avisar as partes.
     * Decisões: Devolve quantos encerrou.
     */
    @Transactional
    public int expirarVencidos() {
        List<UUID> vencidos = jdbc.queryForList("SELECT id FROM acesso_nivel2 WHERE encerrado_em IS NULL AND valido_ate <= ?", UUID.class,
                Timestamp.from(relogio.instant()));
        for (UUID id : vencidos) {
            Acesso a = acesso(id);
            encerrar(a, "VENCIDO", null);
            notificacoes.notificar(conexoes.ler(a.conexaoId()).getPessoaId(), TipoDeNotificacao.NIVEL2_REVOGADO, "Acesso confidencial vencido",
                    "O prazo do seu acesso às informações confidenciais da " + startups.ler(a.startupId()).getNome() + " terminou.", "/conexoes");
        }
        return vencidos.size();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-105
     * Título: Avisar vencimento (R4)
     * Objetivo: Avisar investidor e fundadores administradores dos acessos que vencem em nivel2.aviso_dias, com opção de
     *           renovar.
     * Decisões: Um aviso por prazo (zerado na renovação).
     */
    @Transactional
    public int avisarVencimentos() {
        Instant agora = relogio.instant();
        List<UUID> perto = jdbc.queryForList("SELECT id FROM acesso_nivel2 WHERE encerrado_em IS NULL AND avisado_em IS NULL AND valido_ate <= ?",
                UUID.class, Timestamp.from(agora.plus(Duration.ofDays(parametros.inteiro(AVISO)))));
        for (UUID id : perto) {
            Acesso a = acesso(id);
            String nome = startups.ler(a.startupId()).getNome();
            notificacoes.notificar(conexoes.ler(a.conexaoId()).getPessoaId(), TipoDeNotificacao.NIVEL2_VENCENDO, "Acesso confidencial perto de vencer",
                    "O acesso às informações confidenciais da " + nome + " vence em breve. Fale com a startup se precisar de mais prazo.", "/conexoes");
            notificacoes.notificarTodos(startups.fundadores(a.startupId(), true), TipoDeNotificacao.NIVEL2_VENCENDO, "Acesso confidencial perto de vencer",
                    "Um acesso ao Nível 2 da " + nome + " vence em breve. Renove em Confidencial, se quiser.", "/empreendedor/startups/" + a.startupId());
            jdbc.update("UPDATE acesso_nivel2 SET avisado_em = ? WHERE id = ?", Timestamp.from(agora), id);
        }
        return perto.size();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-32
     * Título: Exigir acesso válido
     * Objetivo: Conferir que quem atua tem o Nível 2 válido da startup.
     * Decisões: Prazo, conexão aberta, sem bloqueio e, na regra do termo, o aceite da versão vigente.
     */
    private Acesso exigirAcesso(UUID eu, String organizacao, UUID startupId) {
        AtuacaoService.Atuacao a = atuacao.paraVer(eu, organizacao);
        Startup s = publicada(startupId, a);
        Conexao c = conexoes.abertasDaStartup(startupId).stream().filter(x -> x.getInvestidorChave().equals(chave(a))).findFirst()
                .orElseThrow(() -> new AccessDeniedException("Você não tem acesso às informações confidenciais desta startup."));
        Acesso acesso = ativoDaConexao(c.getId()).filter(x -> x.validoAte().isAfter(relogio.instant()))
                .orElseThrow(() -> new AccessDeniedException("Você não tem acesso às informações confidenciais desta startup."));
        if (s.getRegraConfidencialidade() == RegraDeConfidencialidade.TERMO_PLATAFORMA && !termoAceito(c)) {
            throw new AccessDeniedException("Aceite a versão atual do Termo de Confidencialidade para continuar.");
        }
        return acesso;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30, KAN-63
     * Título: Requisito que falta
     * Objetivo: Dizer o que falta para liberar conforme a regra da startup.
     * Decisões: Nulo quando nada falta.
     */
    private String requisitoFalta(Startup s, Conexao c) {
        if (s.getRegraConfidencialidade() == RegraDeConfidencialidade.TERMO_PLATAFORMA) {
            return termoAceito(c) ? null : "O investidor ainda não aceitou a versão atual do Termo de Confidencialidade.";
        }
        if (dataRoom.nda(s.getId()).isEmpty()) {
            return "Envie o NDA da startup no Data Room (categoria NDA).";
        }
        return ndaConfirmado(c.getId(), Conexao.Lado.INVESTIDOR) && ndaConfirmado(c.getId(), Conexao.Lado.STARTUP) ? null
                : "O NDA ainda não foi confirmado pelas duas partes.";
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-63
     * Título: Termo aceito
     * Objetivo: Dizer se o investidor da conexão aceitou a versão vigente do termo.
     * Decisões: Organização: aceite de um administrador dela; pessoa: o próprio aceite.
     */
    private boolean termoAceito(Conexao c) {
        if (c.getOrganizacaoId() == null) {
            return aceites.aceitouVigente(c.getPessoaId(), TipoDocumento.TERMO_DE_CONFIDENCIALIDADE);
        }
        return documentosLegais.vigente(TipoDocumento.TERMO_DE_CONFIDENCIALIDADE).map(d -> {
            Integer n = jdbc.queryForObject("SELECT count(*) FROM organizacao_aceite_termo WHERE organizacao_id = ? AND documento_id = ?",
                    Integer.class, c.getOrganizacaoId(), d.getId());
            return n != null && n > 0;
        }).orElse(true);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30
     * Título: ndaConfirmado
     * Objetivo: Rotina auxiliar ndaConfirmado.
     * Decisões: Ver a documentação da classe.
     */
    private boolean ndaConfirmado(UUID conexaoId, Conexao.Lado lado) {
        Integer n = jdbc.queryForObject("SELECT count(*) FROM nda_confirmacao WHERE conexao_id = ? AND lado = ?", Integer.class, conexaoId, lado.name());
        return n != null && n > 0;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30
     * Título: Encerrar acesso
     * Objetivo: Fechar o acesso com o motivo e registrar no histórico.
     * Decisões: Sem autor quando é automático.
     */
    private void encerrar(Acesso a, String motivo, UUID por) {
        jdbc.update("UPDATE acesso_nivel2 SET encerrado_em = ?, encerrado_motivo = ?, encerrado_por = ? WHERE id = ? AND encerrado_em IS NULL",
                Timestamp.from(relogio.instant()), motivo, por, a.id());
        evento(a.startupId(), a.id(), a.investidorChave(), "REVOGADO".equals(motivo) ? "REVOGACAO" : "ENCERRAMENTO", null, por, motivo);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30
     * Título: evento
     * Objetivo: Rotina auxiliar evento.
     * Decisões: Ver a documentação da classe.
     */
    private void evento(UUID startupId, UUID acessoId, UUID chave, String tipo, UUID documentoId, UUID por, String detalhe) {
        jdbc.update("INSERT INTO acesso_nivel2_evento (id, startup_id, acesso_id, investidor_chave, tipo, documento_id, por, detalhe, em) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", UUID.randomUUID(), startupId, acessoId, chave, tipo, documentoId, por, detalhe,
                Timestamp.from(relogio.instant()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30
     * Título: ativoDaConexao
     * Objetivo: Rotina auxiliar ativoDaConexao.
     * Decisões: Ver a documentação da classe.
     */
    private Optional<Acesso> ativoDaConexao(UUID conexaoId) {
        return jdbc.query(SELECT + " WHERE conexao_id = ? AND encerrado_em IS NULL", MAPA, conexaoId).stream().findFirst();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30
     * Título: ativoDaStartup
     * Objetivo: Rotina auxiliar ativoDaStartup.
     * Decisões: Ver a documentação da classe.
     */
    private Acesso ativoDaStartup(UUID startupId, UUID acessoId) {
        return jdbc.query(SELECT + " WHERE id = ? AND startup_id = ? AND encerrado_em IS NULL", MAPA, acessoId, startupId).stream().findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Acesso não encontrado ou já encerrado."));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30
     * Título: acesso
     * Objetivo: Rotina auxiliar acesso.
     * Decisões: Ver a documentação da classe.
     */
    private Acesso acesso(UUID id) {
        return jdbc.query(SELECT + " WHERE id = ?", MAPA, id).get(0);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30
     * Título: conexaoDaStartup
     * Objetivo: Rotina auxiliar conexaoDaStartup.
     * Decisões: Ver a documentação da classe.
     */
    private Conexao conexaoDaStartup(UUID startupId, UUID conexaoId) {
        Conexao c = conexoes.ler(conexaoId);
        if (!c.getStartupId().equals(startupId)) {
            throw new RecursoNaoEncontradoException("Conexão não encontrada.");
        }
        return c;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30
     * Título: publicada
     * Objetivo: Rotina auxiliar publicada.
     * Decisões: Ver a documentação da classe.
     */
    private Startup publicada(UUID startupId, AtuacaoService.Atuacao a) {
        Startup s = startups.ler(startupId);
        if (s.getSituacao() != SituacaoDaStartup.PUBLICADA || bloqueios.bloqueia(startupId, a.pessoa(), a.organizacao())) {
            throw new RecursoNaoEncontradoException("Startup não encontrada.");
        }
        return s;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30
     * Título: exigirFundador
     * Objetivo: Rotina auxiliar exigirFundador.
     * Decisões: Ver a documentação da classe.
     */
    private void exigirFundador(UUID eu, UUID startupId) {
        if (startups.papel(eu, startupId) == PapelNaStartup.COLABORADOR) {
            throw new AccessDeniedException("Só os fundadores veem os acessos às informações confidenciais.");
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30
     * Título: dias
     * Objetivo: Rotina auxiliar dias.
     * Decisões: Ver a documentação da classe.
     */
    private long dias(Integer pedido) {
        long d = pedido == null ? parametros.inteiro(VALIDADE) : pedido;
        if (d < 7 || d > 365) {
            throw new RegraDeNegocioException("prazo_invalido", "O prazo do acesso vai de 7 a 365 dias.");
        }
        return d;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30
     * Título: nomeDaChave
     * Objetivo: Rotina auxiliar nomeDaChave.
     * Decisões: Ver a documentação da classe.
     */
    private String nomeDaChave(UUID chave) {
        return organizacoes.nome(chave).or(() -> contas.nome(chave)).orElse(null);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30
     * Título: chave
     * Objetivo: Rotina auxiliar chave.
     * Decisões: Ver a documentação da classe.
     */
    private static UUID chave(AtuacaoService.Atuacao a) {
        return a.organizacao() != null ? a.organizacao() : a.pessoa();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30
     * Título: hash
     * Objetivo: Rotina auxiliar hash.
     * Decisões: Ver a documentação da classe.
     */
    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static final String SELECT = "SELECT id, startup_id, conexao_id, investidor_chave, liberado_em, valido_ate FROM acesso_nivel2";
    static final org.springframework.jdbc.core.RowMapper<Acesso> MAPA = (rs, i) -> new Acesso(rs.getObject(1, UUID.class),
            rs.getObject(2, UUID.class), rs.getObject(3, UUID.class), rs.getObject(4, UUID.class), rs.getTimestamp(5).toInstant(),
            rs.getTimestamp(6).toInstant());

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30
     * Título: Acesso
     * Objetivo: Rotina auxiliar Acesso.
     * Decisões: Ver a documentação da classe.
     */
    public record Acesso(UUID id, UUID startupId, UUID conexaoId, UUID investidorChave, Instant liberadoEm, Instant validoAte) {
    }

    public record Candidato(UUID conexaoId, String investidor, Conexao.Situacao situacao, boolean verificado, boolean termoAceito,
                            boolean ndaInvestidor, boolean ndaStartup, String falta, Acesso acesso) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30
     * Título: Painel
     * Objetivo: Rotina auxiliar Painel.
     * Decisões: Ver a documentação da classe.
     */
    public record Painel(RegraDeConfidencialidade regra, boolean ndaEnviado, long validadePadraoDias, List<Candidato> conexoes) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30
     * Título: Evento
     * Objetivo: Rotina auxiliar Evento.
     * Decisões: Ver a documentação da classe.
     */
    public record Evento(String tipo, String investidor, String por, String detalhe, Instant em, String documento) {
    }

    public record ParaInvestidor(boolean temAcesso, Instant validoAte, RegraDeConfidencialidade regra, boolean termoAceito, UUID conexaoId,
                                 boolean ndaDisponivel, boolean ndaConfirmado) {
    }

    public record DocumentoVisivel(UUID id, String categoria, String nome, String descricao, String tipo, long tamanho, int versao,
                                   Instant enviadoEm) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30
     * Título: Conteudo
     * Objetivo: Rotina auxiliar Conteudo.
     * Decisões: Ver a documentação da classe.
     */
    public record Conteudo(ApresentacaoService.Apresentacao informacoes, List<DocumentoVisivel> documentos, Instant validoAte) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30
     * Título: Link
     * Objetivo: Rotina auxiliar Link.
     * Decisões: Ver a documentação da classe.
     */
    public record Link(String token, Instant expiraEm) {
    }
}
