package br.com.orbithub.conexoes.service;

import br.com.orbithub.administracao.domain.Lista;
import br.com.orbithub.administracao.service.ListasService;
import br.com.orbithub.compartilhado.erro.RecursoNaoEncontradoException;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.compartilhado.parametros.LeituraDeParametros;
import br.com.orbithub.conexoes.domain.Conexao;
import br.com.orbithub.conexoes.domain.ConexaoFinalizada;
import org.springframework.context.ApplicationEventPublisher;
import br.com.orbithub.conexoes.repository.ConexaoRepository;
import br.com.orbithub.empreendimentos.domain.PapelNaStartup;
import br.com.orbithub.empreendimentos.domain.SituacaoDaStartup;
import br.com.orbithub.empreendimentos.domain.Startup;
import br.com.orbithub.empreendimentos.service.BloqueiosDaStartup;
import br.com.orbithub.empreendimentos.service.StartupsService;
import br.com.orbithub.identidade.service.ConsultaDeContas;
import br.com.orbithub.investidores.domain.PerfilDeInvestimento;
import br.com.orbithub.investidores.service.AtuacaoService;
import br.com.orbithub.investidores.service.OrganizacoesService;
import br.com.orbithub.investidores.service.PerfilDeInvestimentoService;
import br.com.orbithub.notificacoes.domain.TipoDeNotificacao;
import br.com.orbithub.notificacoes.service.NotificacoesService;
import br.com.orbithub.verificacao.domain.Selo;
import br.com.orbithub.verificacao.domain.SujeitoDaVerificacao;
import br.com.orbithub.verificacao.service.VerificacoesService;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 11:25
 * Cartão Jira: KAN-45, KAN-46, KAN-47, KAN-48, KAN-49, KAN-67
 * Título: Conexões entre investidores e startups
 * Objetivo: Receber a manifestação de interesse, deixar o investidor retirar, o fundador administrador aceitar (Match) ou
 *           recusar, as partes avançarem e encerrarem, e montar os painéis dos dois lados com histórico e pendências.
 * Decisões: O investidor age como pessoa ou pela organização (AtuacaoService.paraAgir; observador não age). Uma conexão
 *           não final por investidor e startup; depois de um final negativo, nova tentativa só passados
 *           conexao.nova_tentativa_dias. Startup que bloqueou o investidor some (404). Startup que aceita só verificados
 *           recusa o não verificado com o caminho para se verificar. Aceitar e recusar: só o fundador administrador;
 *           avançar e encerrar: fundadores ou o investidor. O Match não libera o Nível 2 (S8). O motivo da recusa e do
 *           encerramento fica só com quem decidiu e com a plataforma; o investidor recusado recebe mensagem neutra.
 *           Contatos (nome e e-mail) aparecem para as duas partes a partir do Match.
 */
@Service
public class ConexoesService {

    static final String NOVA_TENTATIVA = "conexao.nova_tentativa_dias";
    static final Set<Conexao.Situacao> ATIVAS = EnumSet.of(Conexao.Situacao.PENDENTE, Conexao.Situacao.MATCH,
            Conexao.Situacao.EM_ANALISE, Conexao.Situacao.EM_NEGOCIACAO);
    static final Set<Conexao.Situacao> COM_CONTATO = EnumSet.of(Conexao.Situacao.MATCH, Conexao.Situacao.EM_ANALISE,
            Conexao.Situacao.EM_NEGOCIACAO, Conexao.Situacao.INVESTIU);

    private final ConexaoRepository conexoes;
    private final StartupsService startups;
    private final BloqueiosDaStartup bloqueios;
    private final AtuacaoService atuacao;
    private final VerificacoesService verificacoes;
    private final PerfilDeInvestimentoService perfis;
    private final OrganizacoesService organizacoes;
    private final ConsultaDeContas contas;
    private final ListasService listas;
    private final NotificacoesService notificacoes;
    private final LeituraDeParametros parametros;
    private final JdbcTemplate jdbc;
    private final Clock relogio;
    private final ApplicationEventPublisher eventos;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-45
     * Título: Criação do serviço
     * Objetivo: Receber os serviços dos módulos envolvidos.
     * Decisões: Injeção por construtor.
     */
    @SuppressWarnings("java:S107")
    public ConexoesService(ConexaoRepository conexoes, StartupsService startups, BloqueiosDaStartup bloqueios, AtuacaoService atuacao,
                           VerificacoesService verificacoes, PerfilDeInvestimentoService perfis, OrganizacoesService organizacoes,
                           ConsultaDeContas contas, ListasService listas, NotificacoesService notificacoes,
                           LeituraDeParametros parametros, JdbcTemplate jdbc, Clock relogio, ApplicationEventPublisher eventos) {
        this.eventos = eventos;
        this.conexoes = conexoes;
        this.startups = startups;
        this.bloqueios = bloqueios;
        this.atuacao = atuacao;
        this.verificacoes = verificacoes;
        this.perfis = perfis;
        this.organizacoes = organizacoes;
        this.contas = contas;
        this.listas = listas;
        this.notificacoes = notificacoes;
        this.parametros = parametros;
        this.jdbc = jdbc;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-45
     * Título: Situação para o investidor
     * Objetivo: Dizer, na apresentação da startup, se o investidor pode manifestar interesse, a conexão ativa e o motivo
     *           quando não pode.
     * Decisões: Startup que bloqueou responde 404, como no catálogo.
     */
    @Transactional(readOnly = true)
    public ParaInvestidor situacao(UUID eu, String organizacao, UUID startupId) {
        AtuacaoService.Atuacao a = atuacao.paraVer(eu, organizacao);
        Startup s = publicadaParaQuem(startupId, a);
        List<Conexao> todas = conexoes.findByStartupIdAndInvestidorChaveOrderByAtualizadaEmDesc(startupId, chave(a));
        Optional<Conexao> ativa = todas.stream().filter(c -> ATIVAS.contains(c.getSituacao())).findFirst();
        String impedimento = ativa.isPresent() ? "Já existe uma conexão com esta startup." : impedimento(s, a, todas);
        return new ParaInvestidor(ativa.map(c -> item(c, false)).orElse(null), impedimento == null, impedimento,
                s.isApenasVerificados() && !verificado(a));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-45, KAN-46
     * Título: Manifestar interesse
     * Objetivo: Criar a conexão pendente e avisar os fundadores administradores.
     * Decisões: Valor preliminar positivo; textos até 1.000 caracteres. O aviso de "sem compromisso" é da tela.
     */
    @Transactional
    public Conexao manifestar(UUID eu, String organizacao, UUID startupId, Conexao.Interesse interesse) {
        AtuacaoService.Atuacao a = atuacao.paraAgir(eu, organizacao);
        Startup s = publicadaParaQuem(startupId, a);
        List<Conexao> todas = conexoes.findByStartupIdAndInvestidorChaveOrderByAtualizadaEmDesc(startupId, chave(a));
        if (todas.stream().anyMatch(c -> ATIVAS.contains(c.getSituacao()))) {
            throw new RegraDeNegocioException("conexao_ativa", "Já existe uma conexão com esta startup.");
        }
        String impedimento = impedimento(s, a, todas);
        if (impedimento != null) {
            throw new RegraDeNegocioException("interesse_impedido", impedimento);
        }
        if (interesse.valor() != null && interesse.valor().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraDeNegocioException("valor_invalido", "O valor preliminar precisa ser maior que zero.");
        }
        Conexao c = conexoes.saveAndFlush(Conexao.nova(startupId, eu, a.organizacao(), new Conexao.Interesse(interesse.valor(),
                texto(interesse.condicoes()), interesse.pedido(), texto(interesse.mensagem())), relogio.instant()));
        historico(c, null, eu, Conexao.Lado.INVESTIDOR);
        notificacoes.notificarTodos(startups.fundadores(startupId, true), TipoDeNotificacao.INTERESSE_RECEBIDO,
                "Novo interesse na " + s.getNome(), nomeDoInvestidor(c) + " demonstrou interesse. Avalie e responda em Conexões.",
                "/empreendedor/startups/" + startupId);
        return c;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-45
     * Título: Retirar interesse
     * Objetivo: Deixar o investidor desistir enquanto a conexão está pendente.
     * Decisões: Avisa os fundadores administradores.
     */
    @Transactional
    public Conexao retirar(UUID eu, String organizacao, UUID conexaoId) {
        Conexao c = doInvestidor(eu, organizacao, conexaoId);
        mudar(c, Conexao.Situacao.RETIRADA, eu, Conexao.Lado.INVESTIDOR);
        notificacoes.notificarTodos(startups.fundadores(c.getStartupId(), true), TipoDeNotificacao.INTERESSE_RETIRADO,
                "Interesse retirado", nomeDoInvestidor(c) + " retirou a manifestação de interesse.", "/empreendedor/startups/" + c.getStartupId());
        return c;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-48
     * Título: Aceitar (Match)
     * Objetivo: Aceitar a manifestação e avisar o investidor do Match.
     * Decisões: Só o fundador administrador; startup que aceita só verificados exige o selo no momento do aceite.
     */
    @Transactional
    public Conexao aceitar(UUID eu, UUID conexaoId) {
        Conexao c = conexao(conexaoId);
        startups.exigirAdministrador(eu, c.getStartupId());
        Startup s = startups.ler(c.getStartupId());
        if (s.isApenasVerificados() && !verificado(c.getPessoaId(), c.getOrganizacaoId())) {
            throw new RegraDeNegocioException("investidor_nao_verificado", "A startup aceita só investidores verificados, e este ainda não é.");
        }
        mudar(c, Conexao.Situacao.MATCH, eu, Conexao.Lado.STARTUP);
        notificacoes.notificar(c.getPessoaId(), TipoDeNotificacao.MATCH, "Match com a " + s.getNome(),
                "A startup aceitou o seu interesse. Os contatos já aparecem em Conexões. O Match não é compromisso de investimento.",
                "/conexoes");
        return c;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-48
     * Título: Recusar
     * Objetivo: Recusar a manifestação com mensagem neutra ao investidor.
     * Decisões: Só o fundador administrador. Motivo opcional, da lista MOTIVO_RECUSA, só para a plataforma.
     */
    @Transactional
    public Conexao recusar(UUID eu, UUID conexaoId, String motivo) {
        Conexao c = conexao(conexaoId);
        startups.exigirAdministrador(eu, c.getStartupId());
        String codigo = motivo == null || motivo.isBlank() ? null : listas.exigirAtivo(Lista.MOTIVO_RECUSA, motivo).getCodigo();
        Conexao.Situacao anterior = c.recusar(codigo, relogio.instant());
        historico(c, anterior, eu, Conexao.Lado.STARTUP);
        notificacoes.notificar(c.getPessoaId(), TipoDeNotificacao.INTERESSE_RECUSADO, "Resposta ao seu interesse",
                "A startup não vai seguir com esta conexão agora. Continue explorando o catálogo.", "/catalogo");
        return c;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-49
     * Título: Avançar
     * Objetivo: Passar a conexão para em análise ou em negociação.
     * Decisões: Qualquer lado; "investiu" vem do registro do investimento (S9).
     */
    @Transactional
    public Conexao avancar(UUID eu, String organizacao, UUID conexaoId, Conexao.Situacao para) {
        if (para != Conexao.Situacao.EM_ANALISE && para != Conexao.Situacao.EM_NEGOCIACAO) {
            throw new RegraDeNegocioException("transicao_invalida", "Avance para em análise ou em negociação.");
        }
        Conexao c = conexao(conexaoId);
        mudar(c, para, eu, lado(eu, organizacao, c));
        return c;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-49
     * Título: Encerrar
     * Objetivo: Encerrar a conexão depois do Match, com motivo da lista.
     * Decisões: Qualquer lado. Avisa a outra parte sem o motivo.
     */
    @Transactional
    public Conexao encerrar(UUID eu, String organizacao, UUID conexaoId, String motivo) {
        Conexao c = conexao(conexaoId);
        Conexao.Lado lado = lado(eu, organizacao, c);
        String codigo = listas.exigirAtivo(Lista.MOTIVO_ENCERRAMENTO, motivo).getCodigo();
        Conexao.Situacao anterior = c.encerrar(codigo, lado, relogio.instant());
        historico(c, anterior, eu, lado);
        Startup s = startups.ler(c.getStartupId());
        if (lado == Conexao.Lado.STARTUP) {
            notificacoes.notificar(c.getPessoaId(), TipoDeNotificacao.PRAZO_CONEXAO, "Conexão encerrada",
                    "A " + s.getNome() + " encerrou a conexão.", "/conexoes");
        } else {
            notificacoes.notificarTodos(startups.fundadores(c.getStartupId(), true), TipoDeNotificacao.PRAZO_CONEXAO, "Conexão encerrada",
                    nomeDoInvestidor(c) + " encerrou a conexão.", "/empreendedor/startups/" + c.getStartupId());
        }
        return c;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-47, KAN-49
     * Título: Painel da startup
     * Objetivo: Listar as conexões da startup, com o resumo do investidor, pendências e contatos depois do Match.
     * Decisões: Fundadores (não colaboradores). Filtro opcional por situação.
     */
    @Transactional(readOnly = true)
    public List<Item> daStartup(UUID eu, UUID startupId, Conexao.Situacao filtro) {
        PapelNaStartup papel = startups.papel(eu, startupId);
        if (papel == PapelNaStartup.COLABORADOR) {
            throw new AccessDeniedException("Só os fundadores acompanham as conexões.");
        }
        return conexoes.findByStartupIdOrderByAtualizadaEmDesc(startupId).stream()
                .filter(c -> filtro == null || c.getSituacao() == filtro).map(c -> item(c, true)).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-49
     * Título: Painel do investidor
     * Objetivo: Listar as conexões da pessoa ou da organização da atuação.
     * Decisões: Na organização, todos os membros veem as conexões dela.
     */
    @Transactional(readOnly = true)
    public List<Item> doInvestidor(UUID eu, String organizacao) {
        AtuacaoService.Atuacao a = atuacao.paraVer(eu, organizacao);
        return conexoes.findByInvestidorChaveOrderByAtualizadaEmDesc(chave(a)).stream().map(c -> item(c, false)).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-49
     * Título: Histórico da conexão
     * Objetivo: Listar as mudanças de situação com quem fez e o lado.
     * Decisões: Só as partes da conexão veem.
     */
    @Transactional(readOnly = true)
    public List<Mudanca> historico(UUID eu, String organizacao, UUID conexaoId) {
        Conexao c = conexao(conexaoId);
        lado(eu, organizacao, c);
        return jdbc.query("SELECT de, para, por, lado, em FROM conexao_historico WHERE conexao_id = ? ORDER BY em", (rs, i) -> new Mudanca(
                rs.getString(1) == null ? null : Conexao.Situacao.valueOf(rs.getString(1)), Conexao.Situacao.valueOf(rs.getString(2)),
                rs.getObject(3, UUID.class) == null ? null : contas.nome(rs.getObject(3, UUID.class)).orElse(null),
                Conexao.Lado.valueOf(rs.getString(4)), rs.getTimestamp(5).toInstant()), conexaoId);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30
     * Título: Ler conexão
     * Objetivo: Devolver a conexão para outros módulos (Nível 2), sem conferir quem chama.
     * Decisões: 404 se não existir.
     */
    @Transactional(readOnly = true)
    public Conexao ler(UUID conexaoId) {
        return conexao(conexaoId);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30
     * Título: Conexões abertas da startup
     * Objetivo: Listar as conexões da startup em Match, em análise ou em negociação, que podem receber o Nível 2.
     * Decisões: Sem conferir quem chama; o Data Room confere.
     */
    @Transactional(readOnly = true)
    public List<Conexao> abertasDaStartup(UUID startupId) {
        return conexoes.findByStartupIdOrderByAtualizadaEmDesc(startupId).stream()
                .filter(c -> COM_CONTATO.contains(c.getSituacao()) && c.getSituacao() != Conexao.Situacao.INVESTIU).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30
     * Título: Nome do investidor da conexão
     * Objetivo: Escrever o investidor da conexão para outros módulos.
     * Decisões: "Organização (por Pessoa)" ou a pessoa.
     */
    public String investidor(Conexao c) {
        return nomeDoInvestidor(c);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30
     * Título: Investidor verificado da conexão
     * Objetivo: Dizer se o investidor da conexão tem o selo.
     * Decisões: Para o empreendedor ver antes de liberar.
     */
    public boolean investidorVerificado(Conexao c) {
        return verificado(c.getPessoaId(), c.getOrganizacaoId());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-105
     * Título: Expirar pela rotina
     * Objetivo: Passar a conexão pendente para expirada e avisar o investidor.
     * Decisões: Usado pela R1; sem autor.
     */
    @Transactional
    public void expirar(Conexao c) {
        mudar(c, Conexao.Situacao.EXPIRADA, null, Conexao.Lado.PLATAFORMA);
        notificacoes.notificar(c.getPessoaId(), TipoDeNotificacao.INTERESSE_EXPIRADO, "Interesse expirado",
                "A " + startups.ler(c.getStartupId()).getNome() + " não respondeu no prazo e a manifestação expirou.", "/conexoes");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-105
     * Título: Lembrar pela rotina
     * Objetivo: Lembrar os fundadores administradores da manifestação pendente.
     * Decisões: Usado pela R2.
     */
    @Transactional
    public void lembrar(Conexao c) {
        c.registrarLembrete();
        notificacoes.notificarTodos(startups.fundadores(c.getStartupId(), true), TipoDeNotificacao.PRAZO_CONEXAO,
                "Interesse aguardando resposta", nomeDoInvestidor(c) + " ainda aguarda a sua resposta na "
                        + startups.ler(c.getStartupId()).getNome() + ".", "/empreendedor/startups/" + c.getStartupId());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-45, KAN-67
     * Título: Impedimento para manifestar
     * Objetivo: Explicar por que o investidor ainda não pode manifestar interesse.
     * Decisões: Não verificado em startup que aceita só verificados; nova tentativa antes do prazo. Nulo = pode.
     */
    private String impedimento(Startup s, AtuacaoService.Atuacao a, List<Conexao> todas) {
        if (s.isApenasVerificados() && !verificado(a)) {
            return "Esta startup aceita interesse só de investidores verificados. Envie a sua verificação em Verificação.";
        }
        Optional<Conexao> ultima = todas.stream().filter(c -> c.getSituacao().negativa()).findFirst();
        Instant libera = ultima.map(c -> c.getAtualizadaEm().plus(Duration.ofDays(parametros.inteiro(NOVA_TENTATIVA)))).orElse(null);
        if (libera != null && libera.isAfter(relogio.instant())) {
            return "Você poderá demonstrar interesse de novo a partir de "
                    + java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy").format(libera.atZone(java.time.ZoneId.of("America/Sao_Paulo"))) + ".";
        }
        return null;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-67
     * Título: Startup publicada e sem bloqueio
     * Objetivo: Ler a startup publicada que não bloqueou quem está atuando.
     * Decisões: Bloqueio é silencioso: responde como se a startup não existisse.
     */
    private Startup publicadaParaQuem(UUID startupId, AtuacaoService.Atuacao a) {
        Startup s = startups.ler(startupId);
        if (s.getSituacao() != SituacaoDaStartup.PUBLICADA || bloqueios.bloqueia(startupId, a.pessoa(), a.organizacao())) {
            throw new RecursoNaoEncontradoException("Startup não encontrada.");
        }
        return s;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-49
     * Título: Lado de quem age
     * Objetivo: Dizer se quem chama é o investidor da conexão ou um fundador da startup.
     * Decisões: Quem não é parte recebe 404.
     */
    private Conexao.Lado lado(UUID eu, String organizacao, Conexao c) {
        if (startups.fundadores(c.getStartupId(), false).contains(eu)) {
            return Conexao.Lado.STARTUP;
        }
        AtuacaoService.Atuacao a = atuacao.paraAgir(eu, organizacao);
        if (chave(a).equals(c.getInvestidorChave())) {
            return Conexao.Lado.INVESTIDOR;
        }
        throw new RecursoNaoEncontradoException("Conexão não encontrada.");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-45
     * Título: Conexão do investidor
     * Objetivo: Ler a conexão exigindo que quem chama seja o investidor dela.
     * Decisões: 404 para os demais.
     */
    private Conexao doInvestidor(UUID eu, String organizacao, UUID conexaoId) {
        Conexao c = conexao(conexaoId);
        if (!chave(atuacao.paraAgir(eu, organizacao)).equals(c.getInvestidorChave())) {
            throw new RecursoNaoEncontradoException("Conexão não encontrada.");
        }
        return c;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-49
     * Título: Mudar e registrar
     * Objetivo: Aplicar a transição e gravar no histórico.
     * Decisões: Mesma transação.
     */
    private void mudar(Conexao c, Conexao.Situacao para, UUID por, Conexao.Lado lado) {
        Conexao.Situacao anterior = c.mudar(para, relogio.instant());
        historico(c, anterior, por, lado);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-49
     * Título: Gravar histórico
     * Objetivo: Registrar a mudança de situação.
     * Decisões: Autor nulo para a rotina. Estado final avisa os outros módulos (ConexaoFinalizada), para o Nível 2
     *           terminar na hora (KAN-30, 25/11/2026).
     */
    private void historico(Conexao c, Conexao.Situacao de, UUID por, Conexao.Lado lado) {
        jdbc.update("INSERT INTO conexao_historico (id, conexao_id, de, para, por, lado, em) VALUES (?, ?, ?, ?, ?, ?, ?)", UUID.randomUUID(),
                c.getId(), de == null ? null : de.name(), c.getSituacao().name(), por, lado.name(), Timestamp.from(relogio.instant()));
        if (c.getSituacao().finalizada()) {
            eventos.publishEvent(new ConexaoFinalizada(c.getId()));
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-47, KAN-49
     * Título: Montar item
     * Objetivo: Montar a conexão para o painel de um dos lados.
     * Decisões: Para a startup: resumo do investidor e motivo só se a startup decidiu. Para o investidor: nome da startup
     *           e motivo só se o investidor encerrou. Contatos a partir do Match.
     */
    private Item item(Conexao c, boolean paraStartup) {
        Startup s = startups.ler(c.getStartupId());
        boolean motivoMeu = c.getMotivo() != null && c.getMotivoLado() == (paraStartup ? Conexao.Lado.STARTUP : Conexao.Lado.INVESTIDOR);
        String motivo = motivoMeu ? listas.rotulo(c.getMotivoLado() == Conexao.Lado.STARTUP && c.getSituacao() == Conexao.Situacao.RECUSADA
                ? Lista.MOTIVO_RECUSA : Lista.MOTIVO_ENCERRAMENTO, c.getMotivo()) : null;
        List<Contato> contatos = COM_CONTATO.contains(c.getSituacao())
                ? (paraStartup ? List.of(contato(c.getPessoaId())) : startups.fundadores(c.getStartupId(), true).stream().map(this::contato).toList())
                : List.of();
        boolean aguardaMim = paraStartup ? c.getSituacao() == Conexao.Situacao.PENDENTE : c.getSituacao() == Conexao.Situacao.MATCH;
        return new Item(c.getId(), c.getStartupId(), s.getNomeAprovado() != null ? s.getNomeAprovado() : s.getNome(), c.getSituacao(),
                c.getValorPreliminar(), c.getCondicoes(), c.getPedido(), c.getMensagem(), c.getCriadaEm(), c.getAtualizadaEm(), aguardaMim,
                motivo, paraStartup ? resumo(c) : null, contatos, contas.nome(c.getPessoaId()).orElse(null));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-47, KAN-48
     * Título: Resumo do investidor
     * Objetivo: Montar o que o empreendedor vê do investidor: nome, organização, tipo, ticket, liderança e selo.
     * Decisões: Perfil da organização quando ela é a investidora.
     */
    private Resumo resumo(Conexao c) {
        Optional<PerfilDeInvestimento.Dados> perfil;
        try {
            perfil = perfis.ler(new AtuacaoService.Atuacao(c.getPessoaId(), c.getOrganizacaoId(), null)).perfil();
        } catch (RuntimeException e) {
            perfil = Optional.empty();
        }
        return new Resumo(contas.nome(c.getPessoaId()).orElse(null),
                c.getOrganizacaoId() == null ? null : organizacoes.nome(c.getOrganizacaoId()).orElse(null),
                perfil.map(p -> listas.rotulo(Lista.TIPO_INVESTIDOR, p.tipo())).orElse(null),
                perfil.map(PerfilDeInvestimento.Dados::ticketMinimo).orElse(null), perfil.map(PerfilDeInvestimento.Dados::ticketMaximo).orElse(null),
                perfil.map(p -> p.lideranca() == null ? null : p.lideranca().name()).orElse(null),
                verificado(c.getPessoaId(), c.getOrganizacaoId()) ? Selo.VERIFICADO
                        : c.getOrganizacaoId() != null ? verificacoes.selo(SujeitoDaVerificacao.ORGANIZACAO, c.getOrganizacaoId())
                        : verificacoes.selo(SujeitoDaVerificacao.PESSOA, c.getPessoaId()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-48
     * Título: Contato
     * Objetivo: Montar o nome e o e-mail de uma conta.
     * Decisões: Só depois do Match.
     */
    private Contato contato(UUID conta) {
        return new Contato(contas.nome(conta).orElse(null), contas.emailParaAviso(conta).orElse(null));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-45
     * Título: Nome do investidor
     * Objetivo: Escrever "Organização (por Pessoa)" ou o nome da pessoa.
     * Decisões: Para os avisos.
     */
    private String nomeDoInvestidor(Conexao c) {
        String pessoa = contas.nome(c.getPessoaId()).orElse("Um investidor");
        return c.getOrganizacaoId() == null ? pessoa : organizacoes.nome(c.getOrganizacaoId()).orElse("Uma organização") + " (por " + pessoa + ")";
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-45, KAN-62
     * Título: Investidor verificado
     * Objetivo: Dizer se a pessoa (ou a organização, quando age por ela) tem o selo.
     * Decisões: Mesma regra do catálogo.
     */
    private boolean verificado(AtuacaoService.Atuacao a) {
        return verificado(a.pessoa(), a.organizacao());
    }

    private boolean verificado(UUID pessoa, UUID organizacao) {
        Selo selo = organizacao != null ? verificacoes.selo(SujeitoDaVerificacao.ORGANIZACAO, organizacao)
                : verificacoes.selo(SujeitoDaVerificacao.PESSOA, pessoa);
        return selo == Selo.VERIFICADO;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-45
     * Título: Chave do investidor
     * Objetivo: Devolver a organização da atuação ou a pessoa.
     * Decisões: Pessoa e organização são investidores diferentes.
     */
    private static UUID chave(AtuacaoService.Atuacao a) {
        return a.organizacao() != null ? a.organizacao() : a.pessoa();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-45
     * Título: Texto opcional
     * Objetivo: Limpar o texto e conferir o limite de 1.000 caracteres.
     * Decisões: Vazio vira nulo.
     */
    private static String texto(String valor) {
        String t = valor == null || valor.isBlank() ? null : valor.strip();
        if (t != null && t.length() > 1000) {
            throw new RegraDeNegocioException("texto_longo", "Use até 1.000 caracteres.");
        }
        return t;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-49
     * Título: Conexão
     * Objetivo: Ler a conexão pelo identificador.
     * Decisões: 404 se não existir.
     */
    private Conexao conexao(UUID id) {
        return conexoes.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Conexão não encontrada."));
    }

    public record Resumo(String pessoa, String organizacao, String tipo, BigDecimal ticketMinimo, BigDecimal ticketMaximo,
                         String lideranca, Selo selo) {
    }

    public record Contato(String nome, String email) {
    }

    public record Item(UUID id, UUID startupId, String startup, Conexao.Situacao situacao, BigDecimal valorPreliminar, String condicoes,
                       Conexao.Pedido pedido, String mensagem, Instant criadaEm, Instant atualizadaEm, boolean aguardaVoce, String motivo,
                       Resumo investidor, List<Contato> contatos, String feitaPor) {
    }

    public record Mudanca(Conexao.Situacao de, Conexao.Situacao para, String por, Conexao.Lado lado, Instant em) {
    }

    public record ParaInvestidor(Item conexao, boolean podeManifestar, String impedimento, boolean precisaVerificar) {
    }
}
